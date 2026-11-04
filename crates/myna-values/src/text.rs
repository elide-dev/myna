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
