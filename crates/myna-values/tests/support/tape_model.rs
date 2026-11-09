// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
//! Arbitrary, possibly malformed value tapes for `myna_tape_text_v1`.
//!
//! Shared by the proptest suite (`tests/tape.rs`) and the libFuzzer target
//! (`fuzz/fuzz_targets/tape_text.rs`). Input bytes choose node, region and byte
//! tables, descriptor fields and output capacities. Every pointer still names a
//! live allocation of at least its declared extent, because that is the caller
//! contract; everything inside those allocations (tags, lengths, region IDs,
//! offsets, text bytes) is untrusted. A safe reference model computes the
//! documented result, and the native entry point must agree exactly, never
//! write on errors and never touch memory outside the declared extents.

use myna_values::text::{TextEntry, myna_tape_text_v1};
use myna_values::{
    ABI_VERSION, ABI_VERSION_V4, ALLOW_NON_FINITE, DECIMAL, KIND_MASK, Node, REGION_TEXT, RawView,
    Region, STRING,
};

/// Text that Miri can interpret: simdutf validation (>= 64 bytes) is foreign code.
pub const MAX_TEXT: usize = if cfg!(miri) { 63 } else { 512 };
const MAX_NODES: usize = if cfg!(miri) { 12 } else { 64 };
const MAX_REGIONS: usize = 4;

struct Input<'a>(&'a [u8]);
impl Input<'_> {
    fn byte(&mut self) -> u8 {
        let (first, rest) = self.0.split_first().unwrap_or((&0, &[]));
        self.0 = rest;
        *first
    }
    fn u32(&mut self) -> u32 {
        u32::from_le_bytes([self.byte(), self.byte(), self.byte(), self.byte()])
    }
    fn u64(&mut self) -> u64 {
        u64::from(self.u32()) | (u64::from(self.u32()) << 32)
    }
    fn bytes(&mut self, len: usize) -> Vec<u8> {
        (0..len).map(|_| self.byte()).collect()
    }
    /// Mostly valid Latin-1 and wider text, with invalid sequences mixed in.
    /// Truncation can also split a multibyte sequence at the end.
    fn text(&mut self, len: usize) -> Vec<u8> {
        const VALID: [&[u8]; 7] = [
            b"a",
            b"\0",
            "é".as_bytes(),
            "ÿ".as_bytes(),
            "Ā".as_bytes(),
            "🦀".as_bytes(),
            b"-1.5e+3",
        ];
        const INVALID: [&[u8]; 5] = [
            b"\xff",
            b"\xc0\x80",
            b"\xed\xa0\x80",
            b"\xf4\x90\x80\x80",
            b"\xe2\x82",
        ];
        let mut out = Vec::with_capacity(len);
        while out.len() < len {
            let choice = self.byte();
            if choice < 248 {
                out.extend_from_slice(VALID[usize::from(choice) % VALID.len()]);
            } else if choice < 254 {
                out.extend_from_slice(INVALID[usize::from(choice) % INVALID.len()]);
            } else {
                out.push(self.byte());
            }
        }
        out.truncate(len);
        out
    }
    /// Values clustered at the boundaries the reader must check.
    fn extent(&mut self, limit: u64) -> u64 {
        match self.byte() % 32 {
            0 | 1 => 0,
            2 | 3 => limit,
            4 => limit.saturating_add(1),
            5 => limit.saturating_sub(1),
            6 => u64::from(u32::MAX),
            7 => u64::from(self.u32()),
            _ => u64::from(self.byte()) % (limit + 1),
        }
    }
}

/// Owned storage plus a descriptor whose pointers borrow it.
pub struct Tape {
    nodes: Vec<Node>,
    bytes: Vec<u8>,
    region_storage: Vec<Vec<u8>>,
    regions: Vec<Region>,
    view: RawView,
    capacity_slack: i64,
    table_slack: i64,
}

impl Tape {
    pub fn from_bytes(data: &[u8]) -> Self {
        let mut input = Input(data);
        let header = input.byte();
        let bytes_len = usize::from(input.byte()) % (MAX_TEXT + 1);
        let bytes = input.text(bytes_len);
        let region_count = usize::from(input.byte()) % (MAX_REGIONS + 1);
        let mut region_storage = Vec::new();
        let mut regions = Vec::new();
        for _ in 0..region_count {
            let len = usize::from(input.byte()) % (MAX_TEXT + 1);
            let storage = input.text(len);
            // Declared length never exceeds the live allocation (caller contract).
            let declared = match input.byte() % 4 {
                0 => storage.len().saturating_sub(1),
                _ => storage.len(),
            };
            regions.push(Region {
                data: if storage.is_empty() && input.byte() & 1 == 0 {
                    std::ptr::null()
                } else {
                    storage.as_ptr()
                },
                len: declared as u64,
            });
            region_storage.push(storage);
        }
        // A null region pointer with a nonzero declared length must be rejected,
        // not read; the reader checks before dereferencing.
        if header & 0x50 == 0x50
            && let Some(region) = regions.first_mut()
        {
            region.data = std::ptr::null();
            region.len = region.len.max(1);
        }
        let shape = input.byte();
        let node_count = 1 + usize::from(input.byte()) % if shape & 1 == 0 { 4 } else { MAX_NODES };
        let mut nodes = Vec::with_capacity(node_count);
        for _ in 0..node_count {
            let kind = match input.byte() % 8 {
                0 | 1 => STRING,
                2 => DECIMAL,
                3 | 4 => u32::from(input.byte()) % 10,
                5 => u32::from(input.byte()) & KIND_MASK,
                _ if shape & 2 == 0 => STRING,
                _ => input.u32(),
            };
            let tag = match input.byte() % 8 {
                0..=2 => kind & KIND_MASK,
                3..=6 => (kind & KIND_MASK) | REGION_TEXT,
                _ => kind,
            };
            let (len, payload, next) = if tag & REGION_TEXT == 0 && input.byte() & 1 == 0 {
                // Inline candidate: raw words, length around the 16-byte limit.
                let words = if input.byte() < 32 {
                    input.bytes(16)
                } else {
                    input.text(16)
                };
                (
                    input.extent(16) as u32,
                    u64::from_le_bytes(words[..8].try_into().unwrap()),
                    u64::from_le_bytes(words[8..].try_into().unwrap()),
                )
            } else {
                let region = match input.byte() % 8 {
                    0 | 1 => 0,
                    2 | 3 => regions.len() as u64,
                    4 => regions.len() as u64 + 1,
                    _ => input.extent(regions.len() as u64),
                } & u64::from(u32::MAX);
                let size = match region {
                    0 => bytes.len() as u64,
                    r if r as usize <= regions.len() => regions[r as usize - 1].len,
                    _ => 0,
                };
                let offset = input.extent(size) & u64::from(u32::MAX);
                let len = input.extent(size.saturating_sub(offset)) as u32;
                (len, (region << 32) | offset, input.u64())
            };
            nodes.push(Node {
                tag,
                len,
                payload,
                next,
            });
        }
        let mut view = RawView {
            version: if header & 1 == 0 {
                ABI_VERSION
            } else {
                ABI_VERSION_V4
            },
            flags: if header & 2 == 0 { 0 } else { ALLOW_NON_FINITE },
            nodes: nodes.as_ptr(),
            node_count: nodes.len() as u64,
            bytes: if bytes.is_empty() {
                std::ptr::null()
            } else {
                bytes.as_ptr()
            },
            byte_count: bytes.len() as u64,
            regions: if regions.is_empty() {
                std::ptr::null()
            } else {
                regions.as_ptr()
            },
            region_count: regions.len() as u64,
        };
        // Header faults that must be rejected before any table is read.
        if header & 0xa0 == 0xa0 {
            match input.byte() % 8 {
                0 => view.version = u32::from(input.byte()),
                1 => view.flags = input.u32(),
                2 => view.node_count = 0,
                3 => view.byte_count = view.byte_count.saturating_sub(1),
                4 => view.region_count = view.region_count.saturating_sub(1),
                5 => view.node_count = view.node_count.saturating_sub(1).max(1),
                6 => view.nodes = view.nodes.wrapping_byte_add(4),
                _ => view.regions = view.regions.wrapping_byte_add(4),
            }
        }
        let capacity_slack = i64::from(input.byte() as i8) % 3;
        let table_slack = i64::from(input.byte() as i8) % 2;
        Self {
            nodes,
            bytes,
            region_storage,
            regions,
            view,
            capacity_slack,
            table_slack,
        }
    }

    fn header_valid(&self) -> bool {
        let v = &self.view;
        matches!(v.version, ABI_VERSION | ABI_VERSION_V4)
            && v.flags & !ALLOW_NON_FINITE == 0
            && v.node_count > 0
            && (v.nodes as usize).is_multiple_of(8)
            && (v.region_count == 0 || (v.regions as usize).is_multiple_of(8))
    }

    /// Safe reference: the bytes a node denotes, or the documented error code.
    fn text(&self, node: &Node) -> Result<String, i64> {
        let raw: Vec<u8> = if (node.tag == STRING || node.tag == DECIMAL) && node.len <= 16 {
            let mut words = node.payload.to_le_bytes().to_vec();
            words.extend_from_slice(&node.next.to_le_bytes());
            words.truncate(node.len as usize);
            words
        } else {
            if node.tag != (node.tag & KIND_MASK) | REGION_TEXT {
                return Err(-2);
            }
            let region = node.payload >> 32;
            let offset = node.payload & u64::from(u32::MAX);
            let (null, data): (bool, &[u8]) = if region == 0 {
                (
                    self.view.bytes.is_null(),
                    &self.bytes[..self.view.byte_count as usize],
                )
            } else if region > self.view.region_count {
                return Err(-2);
            } else {
                let r = &self.regions[region as usize - 1];
                let storage = &self.region_storage[region as usize - 1];
                (
                    r.data.is_null(),
                    &storage[..storage.len().min(r.len as usize)],
                )
            };
            let declared = if region == 0 {
                self.view.byte_count
            } else {
                self.regions[region as usize - 1].len
            };
            if offset > declared || u64::from(node.len) > declared - offset {
                return Err(-2);
            }
            if node.len > 0 && null {
                return Err(-2);
            }
            if node.len == 0 {
                return Ok(String::new());
            }
            data[offset as usize..][..node.len as usize].to_vec()
        };
        String::from_utf8(raw).map_err(|_| -1)
    }

    /// Expected output bytes and table, or the error code.
    fn reference(&self) -> Result<(Vec<u8>, Vec<TextEntry>), i64> {
        if !self.header_valid() {
            return Err(-2);
        }
        let mut output = Vec::new();
        let mut table = Vec::new();
        for node in &self.nodes[..self.view.node_count as usize] {
            let mut entry = TextEntry::default();
            if matches!(node.tag & KIND_MASK, STRING | DECIMAL) {
                let text = self.text(node)?;
                entry.offset = output.len() as u64;
                if text.chars().all(|c| u32::from(c) <= 255) {
                    entry.encoding = 1;
                    output.extend(text.chars().map(|c| c as u8));
                    entry.length = text.chars().count() as u64;
                } else {
                    entry.encoding = 2;
                    for unit in text.encode_utf16() {
                        output.extend_from_slice(&unit.to_le_bytes());
                        entry.length += 1;
                    }
                }
            }
            table.push(entry);
        }
        Ok((output, table))
    }

    /// Run sizing, exact, short-output and short-table calls against the model.
    pub fn check(&self) {
        const SENTINEL: u8 = 0xa5;
        let expected = self.reference();
        // SAFETY: the descriptor borrows live storage owned by `self`; every
        // nonempty pointer names an allocation of at least its declared extent.
        let size = unsafe {
            myna_tape_text_v1(&self.view, std::ptr::null_mut(), 0, std::ptr::null_mut(), 0)
        };
        let (output, table) = match expected {
            Err(code) => {
                assert_eq!(size, code, "sizing call");
                let mut out = vec![SENTINEL; 64];
                let mut entries = vec![
                    TextEntry {
                        offset: u64::MAX,
                        ..TextEntry::default()
                    };
                    self.nodes.len()
                ];
                // SAFETY: owned, disjoint output buffers of the declared sizes.
                let written = unsafe {
                    myna_tape_text_v1(
                        &self.view,
                        out.as_mut_ptr(),
                        out.len() as u64,
                        entries.as_mut_ptr(),
                        entries.len() as u64,
                    )
                };
                assert_eq!(written, code, "write call");
                assert!(
                    out.iter().all(|b| *b == SENTINEL),
                    "output changed on error"
                );
                assert!(
                    entries.iter().all(|e| e.offset == u64::MAX),
                    "table changed on error"
                );
                return;
            }
            Ok(result) => result,
        };
        assert_eq!(size, output.len() as i64, "sizing call");
        let nodes = self.view.node_count as usize;
        let capacity = (output.len() as i64 + self.capacity_slack).max(0) as usize;
        let table_capacity = (nodes as i64 + self.table_slack - 1).max(0) as usize;
        let mut out = vec![SENTINEL; capacity + 8];
        let mut entries = vec![
            TextEntry {
                offset: u64::MAX,
                ..TextEntry::default()
            };
            table_capacity + 1
        ];
        // SAFETY: owned, disjoint buffers at least as large as the capacities passed.
        let written = unsafe {
            myna_tape_text_v1(
                &self.view,
                out.as_mut_ptr(),
                capacity as u64,
                entries.as_mut_ptr(),
                table_capacity as u64,
            )
        };
        if capacity < output.len() || table_capacity < nodes {
            assert_eq!(written, -3, "insufficient capacity");
            assert!(
                out.iter().all(|b| *b == SENTINEL),
                "output changed on error"
            );
            assert!(entries.iter().all(|e| e.offset == u64::MAX));
            return;
        }
        assert_eq!(written, output.len() as i64);
        assert_eq!(&out[..output.len()], &output[..]);
        assert!(
            out[output.len()..].iter().all(|b| *b == SENTINEL),
            "wrote past result"
        );
        for (index, (actual, expected)) in entries.iter().zip(&table).enumerate() {
            assert_eq!(
                (
                    actual.offset,
                    actual.length,
                    actual.encoding,
                    actual.reserved
                ),
                (expected.offset, expected.length, expected.encoding, 0),
                "table entry {index}"
            );
        }
        assert!(
            entries[nodes..].iter().all(|e| e.offset == u64::MAX),
            "wrote past table"
        );
    }
}

/// `myna_utf8_to_utf16_v2` over arbitrary bytes must agree with Rust's decoder.
pub fn check_utf16(data: &[u8]) {
    use myna_values::text::myna_utf8_to_utf16_v2;
    let mut output = vec![0_u16; data.len() + 1];
    // SAFETY: live, disjoint input and u16-aligned output of at least `len` units.
    let written = unsafe {
        myna_utf8_to_utf16_v2(
            data.as_ptr(),
            data.len() as u64,
            output.as_mut_ptr(),
            data.len() as u64,
        )
    };
    match std::str::from_utf8(data) {
        Ok(text) => {
            let units: Vec<u16> = text.encode_utf16().collect();
            assert_eq!(written, units.len() as i64);
            assert_eq!(&output[..units.len()], &units[..]);
        }
        Err(_) => assert_eq!(written, -1),
    }
}
