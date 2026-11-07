// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
//! Validated byte-sourced strings and caller-buffer Unicode conversion.
use crate::Error;
use serde::{Serialize, Serializer};

/// A validated UTF-8 borrow. Serialize this wrapper to represent bytes as a
/// string; ordinary Serde byte arrays retain their array semantics.
#[derive(Clone, Copy, Debug)]
pub struct Utf8<'a>(&'a str);
impl<'a> Utf8<'a> {
    pub fn from_bytes(bytes: &'a [u8]) -> Result<Self, Error> {
        if bytes.len() < 64 {
            return std::str::from_utf8(bytes)
                .map(Self)
                .map_err(|_| Error::InvalidUtf8);
        }
        if !simdutf::validate_utf8(bytes) {
            return Err(Error::InvalidUtf8);
        }
        // SAFETY: simdutf validated exactly this immutable slice.
        Ok(Self(unsafe { std::str::from_utf8_unchecked(bytes) }))
    }
    pub fn as_str(self) -> &'a str {
        self.0
    }
}
impl Serialize for Utf8<'_> {
    fn serialize<S: Serializer>(&self, serializer: S) -> Result<S::Ok, S::Error> {
        serializer.serialize_str(self.0)
    }
}

#[derive(Clone, Copy, Debug)]
enum Storage<'a> {
    Inline([u8; 16], usize),
    Borrowed(&'a str),
}
/// Allocation-free text access for either inline or externally stored text.
#[derive(Clone, Copy, Debug)]
pub struct StringValue<'a>(Storage<'a>);
impl<'a> StringValue<'a> {
    pub(crate) fn inline(bytes: [u8; 16], len: usize) -> Self {
        Self(Storage::Inline(bytes, len))
    }
    pub(crate) fn borrowed(value: &'a str) -> Self {
        Self(Storage::Borrowed(value))
    }
    pub fn as_str(&self) -> &str {
        match &self.0 {
            Storage::Inline(bytes, len) => {
                std::str::from_utf8(&bytes[..*len]).expect("serializer emits UTF-8")
            }
            Storage::Borrowed(value) => value,
        }
    }
}

/// Convert valid Rust text into a caller-owned UTF-16 buffer. No allocation.
/// Short input uses Rust's local iterator; bulk conversion uses simdutf.
pub fn to_utf16(value: &str, output: &mut [u16]) -> Result<usize, Error> {
    if value.len() < 64 {
        // A UTF-8 byte per output unit is a conservative capacity bound. Count
        // only when the caller offers less, avoiding a second pass otherwise.
        if output.len() < value.len() && output.len() < value.encode_utf16().count() {
            return Err(Error::OutputTooSmall);
        }
        let mut written = 0;
        for unit in value.encode_utf16() {
            output[written] = unit;
            written += 1;
        }
        return Ok(written);
    }
    if output.len() < value.len()
        && output.len() < simdutf::utf16_length_from_utf8(value.as_bytes())
    {
        return Err(Error::OutputTooSmall);
    }
    // SAFETY: Rust str is valid UTF-8, output capacity is sufficient by an upper bound or simdutf measurement,
    // and the two safe borrows are live, properly aligned, and disjoint.
    Ok(unsafe {
        simdutf::convert_valid_utf8_to_utf16(value.as_ptr(), value.len(), output.as_mut_ptr())
    })
}

/// SVM bridge: validate and transcode into pinned Java char[] storage. Returns
/// UTF-16 units written, -1 for invalid UTF-8, or -2 for invalid extents.
///
/// # Safety
/// Input/output must be live, nonoverlapping allocations for the supplied
/// extents; output is writable and u16-aligned. Neither pointer is retained.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn myna_utf8_to_utf16_v2(
    input: *const u8,
    len: u64,
    output: *mut u16,
    capacity: u64,
) -> i64 {
    if len == 0 {
        return 0;
    }
    if input.is_null()
        || output.is_null()
        || !(output as usize).is_multiple_of(2)
        || len > isize::MAX as u64
        || capacity > (isize::MAX / 2) as u64
        || capacity < len
    {
        return -2;
    }
    // SAFETY: caller guarantees allocations; the byte length is representable.
    // One UTF-16 unit per input byte is a sufficient upper bound even on errors.
    let written = unsafe { simdutf::convert_utf8_to_utf16(input, len as usize, output) };
    if written == 0 { -1 } else { written as i64 }
}

/// One entry per tape node. Encoding 0 means no text, 1 Latin-1 bytes, 2 UTF-16LE.
/// Offset is in output bytes; length is code units (bytes for Latin-1).
#[repr(C)]
#[derive(Clone, Copy, Debug, Default)]
pub struct TextEntry {
    pub offset: u64,
    pub length: u64,
    pub encoding: u32,
    pub reserved: u32,
}

unsafe fn tape_text<'a>(
    view: &crate::RawView,
    node: &crate::Node,
    inline: &'a mut [u8; 16],
) -> Result<&'a str, i64> {
    let bytes = if node.is_inline_text() {
        *inline = node.inline_bytes();
        &inline[..node.len as usize]
    } else {
        if node.tag != (node.kind() | crate::REGION_TEXT) {
            return Err(-2);
        }
        let region = node.payload >> 32;
        let (data, len) = if region == 0 {
            (view.bytes, view.byte_count)
        } else {
            if region > view.region_count {
                return Err(-2);
            }
            // SAFETY: caller supplies a live region table; index was checked.
            let r = unsafe { &*view.regions.add(region as usize - 1) };
            (r.data, r.len)
        };
        let offset = node.payload & u32::MAX as u64;
        if len > isize::MAX as u64
            || offset > len
            || u64::from(node.len) > len - offset
            || (node.len > 0 && data.is_null())
        {
            return Err(-2);
        }
        if node.len == 0 {
            &[]
        } else {
            // SAFETY: caller guarantees live immutable allocations, checked subextent.
            unsafe { std::slice::from_raw_parts(data.add(offset as usize), node.len as usize) }
        }
    };
    Utf8::from_bytes(bytes).map(Utf8::as_str).map_err(|_| -1)
}

/// Batch all STRING and DECIMAL nodes, including keys, without expanding REF nodes.
/// Returns output bytes required/written, -1 for malformed UTF-8, -2 for invalid
/// descriptor/extents, -3 for insufficient output/table capacity. A sizing call
/// supplies null output/table and zero capacities. No output is changed on errors.
///
/// # Safety
/// Descriptor, nodes, region table and referenced bytes must describe live,
/// immutable allocations for this call. Nonempty pointers must be aligned for
/// their types. Output and table must be writable for their supplied capacities
/// and disjoint from one another and all input allocations. No pointers are retained.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn myna_tape_text_v1(
    descriptor: *const crate::RawView,
    output: *mut u8,
    capacity: u64,
    table: *mut TextEntry,
    table_capacity: u64,
) -> i64 {
    if descriptor.is_null() || !(descriptor as usize).is_multiple_of(8) {
        return -2;
    }
    // SAFETY: caller guarantees descriptor validity; alignment checked.
    let view = unsafe { &*descriptor };
    if !matches!(view.version, crate::ABI_VERSION | crate::ABI_VERSION_V4)
        || view.flags & !crate::ALLOW_NON_FINITE != 0
        || view.node_count == 0
        || view.node_count > (isize::MAX as u64 / 24)
        || view.nodes.is_null()
        || !(view.nodes as usize).is_multiple_of(8)
        || view.byte_count > isize::MAX as u64
        || (view.byte_count > 0 && view.bytes.is_null())
        || view.region_count > u32::MAX as u64
        || (view.region_count > 0
            && (view.regions.is_null() || !(view.regions as usize).is_multiple_of(8)))
        || capacity > isize::MAX as u64
        || table_capacity > isize::MAX as u64 / 24
    {
        return -2;
    }
    // SAFETY: caller supplies valid node allocation; representable size/alignment checked.
    let nodes = unsafe { std::slice::from_raw_parts(view.nodes, view.node_count as usize) };
    let mut required = 0_u64;
    for node in nodes {
        if !matches!(node.kind(), crate::STRING | crate::DECIMAL) {
            continue;
        }
        let mut inline = [0; 16];
        // SAFETY: inherited allocation contracts; scratch remains live.
        let text = match unsafe { tape_text(view, node, &mut inline) } {
            Ok(t) => t,
            Err(e) => return e,
        };
        let latin = text.chars().all(|c| u32::from(c) <= 255);
        let size = if latin {
            text.chars().count() as u64
        } else {
            text.encode_utf16().count() as u64 * 2
        };
        required = match required
            .checked_add(size)
            .filter(|n| *n <= isize::MAX as u64)
        {
            Some(n) => n,
            None => return -2,
        };
    }
    if output.is_null() && table.is_null() && capacity == 0 && table_capacity == 0 {
        return required as i64;
    }
    if (required > 0 && output.is_null()) || table.is_null() || !(table as usize).is_multiple_of(8)
    {
        return -2;
    }
    if capacity < required || table_capacity < view.node_count {
        return -3;
    }
    let mut offset = 0_usize;
    for (index, node) in nodes.iter().enumerate() {
        let mut entry = TextEntry::default();
        if matches!(node.kind(), crate::STRING | crate::DECIMAL) {
            let mut inline = [0; 16];
            // SAFETY: immutable inputs were validated in the first pass.
            let text = match unsafe { tape_text(view, node, &mut inline) } {
                Ok(t) => t,
                Err(e) => return e,
            };
            let latin = text.chars().all(|c| u32::from(c) <= 255);
            entry.offset = offset as u64;
            entry.encoding = if latin { 1 } else { 2 };
            if latin {
                for c in text.chars() {
                    // SAFETY: first pass checked total output capacity; outputs are disjoint.
                    unsafe {
                        output.add(offset).write(c as u8);
                    }
                    offset += 1;
                    entry.length += 1;
                }
            } else {
                for c in text.encode_utf16() {
                    // SAFETY: byte writes require no u16 alignment; full capacity checked.
                    unsafe {
                        std::ptr::copy_nonoverlapping(
                            c.to_le_bytes().as_ptr(),
                            output.add(offset),
                            2,
                        );
                    }
                    offset += 2;
                    entry.length += 1;
                }
            }
        }
        // SAFETY: table is aligned and has one writable entry per node.
        unsafe {
            table.add(index).write(entry);
        }
    }
    required as i64
}
