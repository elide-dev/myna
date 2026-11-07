// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
use crate::*;

#[derive(Clone, Copy)]
struct Frame {
    index: usize,
    values: u32,
}

/// Direct producer for typed values and borrowed text regions. No Serde value
/// tree is required. Registered slices must outlive the builder and its views.
/// Errors poison the current document until `clear`; partial views never escape.
///
/// ```compile_fail
/// use myna_values::{Builder, Bump};
/// let arena = Bump::new();
/// let mut builder = Builder::new_in(&arena);
/// let text = String::from("borrowed");
/// let region = builder.region(text.as_bytes()).unwrap();
/// drop(text);
/// builder.string_ref(region, 0, 8).unwrap();
/// ```
pub struct Builder<'a, A: Allocator + Clone> {
    doc: Document<A>,
    regions: Vec<Region, A>,
    stack: Vec<Frame, A>,
    roots: usize,
    v4: bool,
    directory_threshold: u32,
    directory: Vec<DirectoryEntry, A>,
    borrowed: PhantomData<&'a [u8]>,
}
impl<'a, A: Allocator + Clone> Builder<'a, A> {
    pub fn new_in(allocator: A) -> Self {
        Self::with_limits_in(allocator, Limits::default())
    }
    pub fn with_limits_in(allocator: A, limits: Limits) -> Self {
        Self::with_options_in(allocator, limits, Options::default())
    }
    pub fn with_options_in(allocator: A, limits: Limits, options: Options) -> Self {
        Self {
            doc: Document::with_options_in(allocator.clone(), limits, options),
            regions: Vec::new_in(allocator.clone()),
            stack: Vec::new_in(allocator.clone()),
            directory: Vec::new_in(allocator),
            v4: false,
            directory_threshold: 8,
            roots: 0,
            borrowed: PhantomData,
        }
    }
    /// Enable ABI 4 before writing. Zero threshold disables object directories.
    pub fn enable_v4(&mut self, threshold: u32, metadata: bool) -> Result<(), Error> {
        self.run(|s| {
            if !s.doc.nodes.is_empty() {
                return Err(Error::Protocol("enable ABI 4 before writing"));
            }
            s.v4 = true;
            s.directory_threshold = threshold;
            s.doc.has_metadata = metadata;
            Ok(())
        })
    }
    /// Set an opaque format-specific metadata ID. Zero means unspecified.
    pub fn metadata(&mut self, node: usize, value: u32) -> Result<(), Error> {
        self.run(|s| {
            let slot = s.doc.metadata.get_mut(node).ok_or(Error::RegionBounds)?;
            *slot = value;
            Ok(())
        })
    }
    /// Append an explicit alias of a completed earlier subtree. Never expands it.
    pub fn reference_node(&mut self, target: usize) -> Result<(), Error> {
        self.run(|s| {
            if !s.v4
                || target >= s.doc.nodes.len()
                || s.doc.nodes[target].subtree_end(target) > s.doc.nodes.len() as u64
                || s.stack.iter().any(|f| f.index == target)
            {
                return Err(Error::Protocol(
                    "reference requires completed earlier value",
                ));
            }
            s.slot(REF)?;
            s.doc.push(REF, 0, target as u64)?;
            Ok(())
        })
    }
    pub fn clear(&mut self) {
        self.doc.clear();
        self.regions.clear();
        self.stack.clear();
        self.directory.clear();
        self.roots = 0;
    }
    fn run<T>(&mut self, f: impl FnOnce(&mut Self) -> Result<T, Error>) -> Result<T, Error> {
        if self.doc.failed {
            return Err(Error::Protocol(
                "builder is poisoned; clear it before reuse",
            ));
        }
        let result = f(self);
        if result.is_err() {
            self.doc.failed = true;
        }
        result
    }
    /// Register immutable storage without copying. Region 0 is internal owned
    /// text; returned IDs start at 1 and are invalidated by `clear`.
    pub fn region(&mut self, bytes: &'a [u8]) -> Result<u32, Error> {
        self.run(|s| {
            let id = u32::try_from(s.regions.len() + 1).map_err(|_| Error::Limit)?;
            s.regions.try_reserve(1).map_err(|_| Error::Allocation)?;
            s.regions.push(Region {
                data: bytes.as_ptr(),
                len: bytes.len() as u64,
            });
            Ok(id)
        })
    }
    fn slot(&mut self, kind: u32) -> Result<(), Error> {
        if let Some(frame) = self.stack.last_mut() {
            if self.doc.nodes[frame.index].kind() == OBJECT
                && frame.values % 2 == 0
                && kind != STRING
            {
                return Err(Error::KeyType);
            }
            frame.values = frame.values.checked_add(1).ok_or(Error::Limit)?;
        } else {
            if self.roots != 0 {
                return Err(Error::Protocol("multiple roots"));
            }
            self.roots = 1;
        }
        Ok(())
    }
    /// Append a Serde value at the current position. Borrowed regions and exact
    /// numbers use the explicit methods below, keeping custom Serde conventions out of the ABI.
    pub fn value<T: Serialize + ?Sized>(&mut self, value: &T) -> Result<(), Error> {
        self.run(|s| {
            let start = s.doc.nodes.len();
            value.serialize(ser::Serializer {
                doc: &mut s.doc,
                depth: s.stack.len(),
            })?;
            let node = s
                .doc
                .nodes
                .get(start)
                .ok_or(Error::Protocol("empty serialization"))?;
            if node.subtree_end(start) != s.doc.nodes.len() as u64 {
                return Err(Error::Protocol("incomplete value"));
            }
            s.slot(node.kind())
        })
    }
    pub fn begin_array(&mut self) -> Result<(), Error> {
        self.begin(ARRAY)
    }
    pub fn begin_object(&mut self) -> Result<(), Error> {
        self.begin(OBJECT)
    }
    fn begin(&mut self, kind: u32) -> Result<(), Error> {
        self.run(|s| {
            if s.stack.len() >= s.doc.limits.max_depth {
                return Err(Error::Limit);
            }
            s.slot(kind)?;
            let index = s.doc.push(kind, 0, 0)?;
            s.stack.try_reserve(1).map_err(|_| Error::Allocation)?;
            s.stack.push(Frame { index, values: 0 });
            Ok(())
        })
    }
    pub fn end(&mut self) -> Result<(), Error> {
        self.run(|s| {
            let frame = s.stack.pop().ok_or(Error::Protocol("no open container"))?;
            let object = s.doc.nodes[frame.index].kind() == OBJECT;
            if object && frame.values % 2 != 0 {
                return Err(Error::Protocol("missing object value"));
            }
            s.doc.finish(
                frame.index,
                if object {
                    frame.values / 2
                } else {
                    frame.values
                },
            );
            if s.v4
                && object
                && s.directory_threshold != 0
                && frame.values / 2 >= s.directory_threshold
            {
                let start = s.directory.len();
                s.directory
                    .try_reserve((frame.values / 2) as usize)
                    .map_err(|_| Error::Allocation)?;
                let mut key = frame.index + 1;
                while key < s.doc.nodes.len() {
                    let view = View {
                        nodes: &s.doc.nodes,
                        bytes: &s.doc.bytes,
                        regions: &s.regions,
                        flags: 0,
                        v4: None,
                    };
                    let text = view.string(key).ok_or(Error::KeyType)?;
                    let hash = hash_utf8(text.as_str().as_bytes());
                    let key_offset = u32::try_from(key - frame.index).map_err(|_| Error::Limit)?;
                    s.directory.push(DirectoryEntry { hash, key_offset });
                    key = s.doc.nodes[key + 1].subtree_end(key + 1) as usize;
                }
                s.directory[start..].sort_unstable();
                s.doc.nodes[frame.index].payload = start as u64 + 1;
            }
            Ok(())
        })
    }
    pub fn exact_number(&mut self, token: &str) -> Result<(), Error> {
        self.run(|s| {
            json_number::Number::new(token).map_err(|_| Error::InvalidNumber)?;
            s.slot(DECIMAL)?;
            let start = s.doc.nodes.len();
            s.doc.string(token)?;
            s.doc.nodes[start].tag = (s.doc.nodes[start].tag & !KIND_MASK) | DECIMAL;
            Ok(())
        })
    }
    /// Borrow text at any length, including <=16 bytes. UTF-8 is validated;
    /// the resulting node retains a region reference instead of inlining.
    pub fn string_ref(&mut self, region: u32, offset: u32, len: u32) -> Result<(), Error> {
        self.reference(STRING, region, offset, len)
    }
    pub fn number_ref(&mut self, region: u32, offset: u32, len: u32) -> Result<(), Error> {
        self.reference(DECIMAL, region, offset, len)
    }
    fn reference(&mut self, kind: u32, region: u32, offset: u32, len: u32) -> Result<(), Error> {
        self.run(|s| {
            let r = region
                .checked_sub(1)
                .and_then(|i| s.regions.get(i as usize))
                .ok_or(Error::RegionBounds)?;
            if u64::from(offset) + u64::from(len) > r.len {
                return Err(Error::RegionBounds);
            }
            // SAFETY: registered slices are live for 'a; extent was checked.
            let bytes =
                unsafe { std::slice::from_raw_parts(r.data.add(offset as usize), len as usize) };
            if kind == DECIMAL {
                json_number::Number::new(bytes).map_err(|_| Error::InvalidNumber)?;
            } else {
                text::Utf8::from_bytes(bytes)?;
            }
            s.slot(kind)?;
            s.doc.charge_utf8(len as usize)?;
            s.doc.push(
                kind | REGION_TEXT,
                len,
                (u64::from(region) << 32) | u64::from(offset),
            )?;
            Ok(())
        })
    }
    pub fn finish(&self) -> Result<View<'_>, Error> {
        if self.doc.failed || self.roots != 1 || !self.stack.is_empty() {
            return Err(Error::Protocol("incomplete or failed builder"));
        }
        Ok(View {
            nodes: &self.doc.nodes,
            bytes: &self.doc.bytes,
            regions: &self.regions,
            v4: self.v4.then_some((&self.directory, &self.doc.metadata)),
            flags: if self.doc.options.allow_non_finite {
                ALLOW_NON_FINITE
            } else {
                0
            },
        })
    }
}
