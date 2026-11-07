//! JSON-shaped, native-endian value tapes for synchronous Rust / SVM calls.
//!
//! Both backing buffers use the supplied [`Allocator`]. The default is a borrowed
//! [`Bump`]; retain a [`Document`] to reuse its capacity across encodes.
//!
//! ```
//! use myna_values::{Bump, Document};
//! let arena = Bump::new();
//! let mut document = Document::new(&arena);
//! let view = document.encode(&vec![1_u64, 2, 3])?;
//! assert_eq!(view.nodes().len(), 4);
//! # Ok::<(), myna_values::Error>(())
//! ```
//!
//! ```compile_fail
//! use myna_values::{Bump, Document};
//! let arena = Bump::new();
//! let mut document = Document::new(&arena);
//! let view = document.encode(&42).unwrap();
//! document.clear();
//! println!("{:?}", view.nodes());
//! ```
//!
//! A view borrows its document, preventing safe mutation or destruction during
//! use. Native consumers must not retain pointers past that borrow or mutate the
//! storage. This is an in-process ABI, not a portable serialization format.

mod builder;
mod ser;
pub mod text;
pub use builder::Builder;

use allocator_api2::vec::Vec;
pub use allocator_api2::{self, alloc::Allocator};
pub use bumpalo::Bump;
use serde::Serialize;
use std::{fmt, marker::PhantomData};

#[cfg(not(target_pointer_width = "64"))]
compile_error!("Myna value ABI v3 requires a 64-bit target");

pub const ABI_VERSION: u32 = 3;
pub const ABI_VERSION_V4: u32 = 4;
/// Descriptor capability for IEEE-754 NaN and infinities; absent by default.
pub const ALLOW_NON_FINITE: u32 = 1;
pub const NULL: u32 = 0;
pub const BOOL: u32 = 1;
pub const I64: u32 = 2;
pub const U64: u32 = 3;
pub const F64: u32 = 4;
pub const STRING: u32 = 5;
pub const ARRAY: u32 = 6;
pub const OBJECT: u32 = 7;
pub const DECIMAL: u32 = 8;
pub const REF: u32 = 9;
pub const REGION_TEXT: u32 = 0x100;
pub const KIND_MASK: u32 = 0xff;
pub const INLINE_STRING_BYTES: usize = 16;

/// A 24-byte node. Use `subtree_end(index)` to skip a value; the raw `next`
/// field is UTF-8 payload for inline strings and a subtree index otherwise.
///
/// Text without REGION_TEXT packs up to 16 UTF-8 bytes into `payload` and
/// `next` in little-endian byte order; its subtree end is implicit. REGION_TEXT
/// uses the high 32 payload bits as region ID and low 32 as byte offset, with
/// `len` as byte length. Both strings and decimal tokens use this layout. Containers
/// use `len` as child/entry count; their first child follows the container node.
/// Objects alternate string-key and value subtrees. Scalars store raw bits in
/// `payload`. Unused fields are zero. Integer fields use native endianness.
#[repr(C)]
#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub struct Node {
    pub tag: u32,
    pub len: u32,
    pub payload: u64,
    pub next: u64,
}

impl Node {
    pub fn kind(&self) -> u32 {
        self.tag & KIND_MASK
    }
    pub fn is_inline_string(&self) -> bool {
        self.tag == STRING && self.len <= INLINE_STRING_BYTES as u32
    }
    pub fn is_inline_text(&self) -> bool {
        (self.tag == STRING || self.tag == DECIMAL) && self.len <= INLINE_STRING_BYTES as u32
    }
    pub fn subtree_end(&self, index: usize) -> u64 {
        if self.is_inline_text() {
            index as u64 + 1
        } else {
            self.next
        }
    }
    fn set_inline(&mut self, value: &[u8]) {
        let mut bytes = [0_u8; INLINE_STRING_BYTES];
        bytes[..value.len()].copy_from_slice(value);
        self.payload = u64::from_le_bytes(bytes[..8].try_into().unwrap());
        self.next = u64::from_le_bytes(bytes[8..].try_into().unwrap());
    }
    fn inline_bytes(&self) -> [u8; INLINE_STRING_BYTES] {
        let mut bytes = [0; INLINE_STRING_BYTES];
        bytes[..8].copy_from_slice(&self.payload.to_le_bytes());
        bytes[8..].copy_from_slice(&self.next.to_le_bytes());
        bytes
    }
}

/// An immutable borrowed region. Owners guarantee allocation validity and lifetime.
#[repr(C)]
#[derive(Clone, Copy, Debug)]
pub struct Region {
    pub data: *const u8,
    pub len: u64,
}

/// The 56-byte descriptor on Myna's supported 64-bit targets.
///
/// Pointers are read-only, valid only while the originating view is live, and
/// may be dangling for empty byte storage (never dereference at length zero).
#[repr(C)]
#[derive(Clone, Copy, Debug)]
pub struct RawView {
    pub version: u32,
    pub flags: u32,
    pub nodes: *const Node,
    pub node_count: u64,
    pub bytes: *const u8,
    pub byte_count: u64,
    pub regions: *const Region,
    pub region_count: u64,
}

/// ABI 4 object directory entry, sorted by unsigned hash then relative key offset.
#[repr(C)]
#[derive(Clone, Copy, Debug, PartialEq, Eq, PartialOrd, Ord)]
pub struct DirectoryEntry {
    pub hash: u32,
    pub key_offset: u32,
}

/// ABI 4 preserves the 56-byte v3 prefix and adds two optional borrowed arrays.
#[repr(C)]
#[derive(Clone, Copy, Debug)]
pub struct RawViewV4 {
    pub base: RawView,
    pub directory: *const DirectoryEntry,
    pub directory_count: u64,
    pub metadata: *const u32,
    pub metadata_count: u64,
}

/// FNV-1a over decoded UTF-8 bytes, with wrapping 32-bit multiplication.
pub fn hash_utf8(bytes: &[u8]) -> u32 {
    bytes.iter().fold(0x811c9dc5, |hash, b| {
        (hash ^ u32::from(*b)).wrapping_mul(0x01000193)
    })
}

#[derive(Debug, thiserror::Error)]
pub enum Error {
    #[error("invalid JSON number token")]
    InvalidNumber,
    #[error("invalid region or byte extent")]
    RegionBounds,
    #[error("invalid UTF-8 input")]
    InvalidUtf8,
    #[error("UTF-16 output buffer is too small")]
    OutputTooSmall,
    #[error("value storage allocation failed")]
    Allocation,
    #[error("value exceeds the configured size or depth limit")]
    Limit,
    #[error("non-finite floats require Options::allow_non_finite")]
    NonFinite,
    #[error("integer is outside the supported i64/u64 range")]
    IntegerRange,
    #[error("object keys must serialize as strings")]
    KeyType,
    #[error("invalid Serde container protocol: {0}")]
    Protocol(&'static str),
    #[error("{0}")]
    Custom(String),
}
impl serde::ser::Error for Error {
    fn custom<T: fmt::Display>(msg: T) -> Self {
        Self::Custom(msg.to_string())
    }
}

/// Bounds apply to each encode, including object-key nodes and UTF-8 key bytes.
#[derive(Clone, Copy, Debug)]
pub struct Limits {
    pub max_depth: usize,
    pub max_nodes: usize,
    pub max_bytes: usize,
}

/// Optional value semantics. Defaults preserve strict, JSON-shaped values.
#[derive(Clone, Copy, Debug, Default)]
pub struct Options {
    pub allow_non_finite: bool,
}
impl Default for Limits {
    fn default() -> Self {
        Self {
            max_depth: 128,
            max_nodes: u32::MAX as usize,
            max_bytes: u32::MAX as usize,
        }
    }
}

/// Reusable storage. `A` may be any allocator-api2 0.2 allocator (including a
/// reference to a pool, region, or global allocator). Both vectors use `A`.
/// Custom allocators must obey the allocator-api2 safety contract.
pub struct Document<A: Allocator + Clone> {
    nodes: Vec<Node, A>,
    metadata: Vec<u32, A>,
    has_metadata: bool,
    bytes: Vec<u8, A>,
    limits: Limits,
    options: Options,
    complete: bool,
    failed: bool,
    utf8_len: usize,
}
impl<'a> Document<&'a Bump> {
    pub fn new(arena: &'a Bump) -> Self {
        Self::new_in(arena)
    }
}
impl<A: Allocator + Clone> Document<A> {
    pub fn new_in(allocator: A) -> Self {
        Self::with_limits_in(allocator, Limits::default())
    }
    pub fn with_limits_in(allocator: A, limits: Limits) -> Self {
        Self::with_options_in(allocator, limits, Options::default())
    }
    /// Enable explicit descriptor capabilities without changing the ABI layout.
    pub fn with_options_in(allocator: A, limits: Limits, options: Options) -> Self {
        Self {
            nodes: Vec::new_in(allocator.clone()),
            metadata: Vec::new_in(allocator.clone()),
            has_metadata: false,
            bytes: Vec::new_in(allocator),
            limits,
            options,
            complete: false,
            failed: false,
            utf8_len: 0,
        }
    }
    /// Clears the previous value, retaining allocated capacity. On error the
    /// document is empty; partial encodes are never exposed through `view()`.
    pub fn encode<T: Serialize + ?Sized>(&mut self, value: &T) -> Result<View<'_>, Error> {
        self.clear();
        if let Err(error) = value.serialize(ser::Serializer {
            doc: self,
            depth: 0,
        }) {
            self.clear();
            return Err(error);
        }
        if self.failed
            || self
                .nodes
                .first()
                .is_none_or(|node| node.subtree_end(0) != self.nodes.len() as u64)
        {
            self.clear();
            return Err(Error::Protocol("expected exactly one root value"));
        }
        self.complete = true;
        Ok(self.view().expect("completed serialization"))
    }
    pub fn clear(&mut self) {
        self.complete = false;
        self.failed = false;
        self.utf8_len = 0;
        self.nodes.clear();
        self.metadata.clear();
        self.bytes.clear();
    }
    pub fn view(&self) -> Option<View<'_>> {
        self.complete.then(|| View {
            nodes: &self.nodes,
            bytes: &self.bytes,
            regions: &[],
            v4: None,
            flags: if self.options.allow_non_finite {
                ALLOW_NON_FINITE
            } else {
                0
            },
        })
    }
    pub fn capacities(&self) -> (usize, usize) {
        (self.nodes.capacity(), self.bytes.capacity())
    }

    fn push(&mut self, tag: u32, len: u32, payload: u64) -> Result<usize, Error> {
        if self.nodes.len() >= self.limits.max_nodes {
            return Err(Error::Limit);
        }
        self.nodes.try_reserve(1).map_err(|_| Error::Allocation)?;
        if self.has_metadata {
            self.metadata
                .try_reserve(1)
                .map_err(|_| Error::Allocation)?;
            self.metadata.push(0);
        }
        let index = self.nodes.len();
        self.nodes.push(Node {
            tag,
            len,
            payload,
            next: index as u64 + 1,
        });
        Ok(index)
    }
    fn charge_utf8(&mut self, additional: usize) -> Result<(), Error> {
        let end = self.utf8_len.checked_add(additional).ok_or(Error::Limit)?;
        if end > self.limits.max_bytes {
            return Err(Error::Limit);
        }
        self.utf8_len = end;
        Ok(())
    }
    fn string(&mut self, value: &str) -> Result<(), Error> {
        let len = u32::try_from(value.len()).map_err(|_| Error::Limit)?;
        self.charge_utf8(value.len())?;
        if value.len() <= INLINE_STRING_BYTES {
            let index = self.push(STRING, len, 0)?;
            self.nodes[index].set_inline(value.as_bytes());
        } else {
            if self.bytes.len() > u32::MAX as usize {
                return Err(Error::Limit);
            }
            self.bytes
                .try_reserve(value.len())
                .map_err(|_| Error::Allocation)?;
            self.push(STRING | REGION_TEXT, len, self.bytes.len() as u64)?;
            self.append_reserved_bytes(value.as_bytes());
        }
        Ok(())
    }
    // allocator-api2 0.2 extends slices through an element-wise Clone iterator.
    // Use one bulk copy after fallible reservation, avoiding per-byte growth checks.
    fn append_reserved_bytes(&mut self, bytes: &[u8]) {
        let old_len = self.bytes.len();
        assert!(bytes.len() <= self.bytes.capacity() - old_len);
        // SAFETY: callers reserved sufficient capacity. The destination is in
        // uninitialized spare capacity, disjoint from the immutable input slice.
        // u8 has alignment 1 and every copied byte is initialized. No operation
        // can panic between the copy and publishing the initialized length.
        unsafe {
            std::ptr::copy_nonoverlapping(
                bytes.as_ptr(),
                self.bytes.as_mut_ptr().add(old_len),
                bytes.len(),
            );
            self.bytes.set_len(old_len + bytes.len());
        }
    }
    fn finish(&mut self, index: usize, count: u32) {
        let end = self.nodes.len() as u64;
        self.nodes[index].len = count;
        self.nodes[index].next = end;
    }
}

/// Immutable borrowed storage. Accessing the underlying ABI requires the native
/// consumer to honor this lifetime; exporting a raw pointer cannot enforce it.
#[derive(Clone, Copy)]
pub struct View<'a> {
    nodes: &'a [Node],
    bytes: &'a [u8],
    regions: &'a [Region],
    flags: u32,
    v4: Option<(&'a [DirectoryEntry], &'a [u32])>,
}
impl<'a> View<'a> {
    pub fn version(self) -> u32 {
        if self.v4.is_some() {
            ABI_VERSION_V4
        } else {
            ABI_VERSION
        }
    }
    pub fn directory(self) -> &'a [DirectoryEntry] {
        self.v4.map_or(&[], |v| v.0)
    }
    pub fn metadata(self) -> &'a [u32] {
        self.v4.map_or(&[], |v| v.1)
    }
    pub fn reference_target(self, index: usize) -> Option<usize> {
        let n = self.nodes.get(index)?;
        (n.kind() == REF && self.v4.is_some()).then_some(n.payload as usize)
    }
    pub fn flags(self) -> u32 {
        self.flags
    }
    pub fn nodes(self) -> &'a [Node] {
        self.nodes
    }
    pub fn bytes(self) -> &'a [u8] {
        self.bytes
    }
    /// Borrow long text or copy at most 16 bytes into a stack-sized wrapper.
    pub fn string(self, index: usize) -> Option<text::StringValue<'a>> {
        let node = self.nodes.get(index)?;
        if node.kind() != STRING {
            return None;
        }
        Some(self.text_value(node))
    }
    /// Exact decimal token, without conversion to binary floating point.
    pub fn number_text(self, index: usize) -> Option<text::StringValue<'a>> {
        let node = self.nodes.get(index)?;
        if node.kind() != DECIMAL {
            return None;
        }
        Some(self.text_value(node))
    }
    fn text_value(self, node: &Node) -> text::StringValue<'a> {
        if node.is_inline_text() {
            return text::StringValue::inline(node.inline_bytes(), node.len as usize);
        }
        let region = (node.payload >> 32) as usize;
        let offset = (node.payload & u32::MAX as u64) as usize;
        let bytes = if region == 0 {
            self.bytes
        } else {
            let r = &self.regions[region - 1];
            // SAFETY: Builder binds each registered immutable region to its lifetime.
            unsafe { std::slice::from_raw_parts(r.data, r.len as usize) }
        };
        let bytes = &bytes[offset..offset + node.len as usize];
        // SAFETY: successful Serde/builder operations validate all string and number text.
        text::StringValue::borrowed(unsafe { std::str::from_utf8_unchecked(bytes) })
    }
    pub fn raw(self) -> BorrowedRawView<'a> {
        BorrowedRawView {
            raw: RawViewV4 {
                base: RawView {
                    version: self.version(),
                    flags: self.flags,
                    nodes: self.nodes.as_ptr(),
                    node_count: self.nodes.len() as u64,
                    bytes: self.bytes.as_ptr(),
                    byte_count: self.bytes.len() as u64,
                    regions: self.regions.as_ptr(),
                    region_count: self.regions.len() as u64,
                },
                directory: self.directory().as_ptr(),
                directory_count: self.directory().len() as u64,
                metadata: self.metadata().as_ptr(),
                metadata_count: self.metadata().len() as u64,
            },
            lifetime: PhantomData,
        }
    }
}
/// Owns the small ABI descriptor, while borrowing the document's buffers.
/// Keep this value alive for the entire native call. No allocation occurs.
pub struct BorrowedRawView<'a> {
    raw: RawViewV4,
    lifetime: PhantomData<&'a ()>,
}
impl BorrowedRawView<'_> {
    pub fn as_v4_ptr(&self) -> Option<*const RawViewV4> {
        (self.raw.base.version == ABI_VERSION_V4).then_some(&self.raw)
    }
    pub fn as_ptr(&self) -> *const RawView {
        &self.raw.base
    }
}
