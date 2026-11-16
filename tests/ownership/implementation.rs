// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
//! The ownership fixture's imports implemented only against the generated trait: no raw pointer,
//! length or `from_raw_parts` here beyond the one pass-through pointer the descriptor leaves raw.
//! `define_implementations!` defines the `extern "C"` entry points a C caller links against.
#[allow(dead_code, non_snake_case, unused_imports)]
mod seam {
    include!(concat!(env!("SEAM_GENERATED"), "/seam.rs"));
}
use std::sync::atomic::{AtomicBool, AtomicI64, Ordering};

static LIVE: AtomicI64 = AtomicI64::new(0);
static FAIL: AtomicBool = AtomicBool::new(false);
const ABSENT: u64 = 1 << 32;

/// Counts itself live until dropped, which the destroy entry points do through `Box`.
struct Live;
impl Drop for Live {
    fn drop(&mut self) {
        LIVE.fetch_sub(1, Ordering::SeqCst);
    }
}
pub struct Ticket {
    _live: Live,
}
pub struct Parsed {
    value: u64,
    _live: Live,
}
fn live() -> Option<Live> {
    if FAIL.swap(false, Ordering::SeqCst) {
        return None;
    }
    LIVE.fetch_add(1, Ordering::SeqCst);
    Some(Live)
}
fn sum(bytes: &[u8]) -> u64 {
    bytes.iter().map(|&b| u64::from(b)).sum()
}

struct Demo;
impl seam::OwnershipDemoImplementation for Demo {
    type Parsed = Parsed;
    type Ticket = Ticket;
    fn checksum(bytes: &[u8]) -> u64 {
        sum(bytes)
    }
    fn decode(data: &[u8], text: Option<&mut [u8]>) -> u64 {
        let Some(text) = text else { return data.len() as u64 | ABSENT };
        for (out, byte) in text.iter_mut().zip(data) {
            *out = byte.wrapping_add(1);
        }
        data.len() as u64
    }
    fn increment(bytes: &mut [u8]) {
        for byte in bytes {
            *byte = byte.wrapping_add(1);
        }
    }
    fn tally(bytes: Option<&[u8]>) -> u64 {
        bytes.map_or(ABSENT, sum)
    }
    fn transfer(input: &[u8], output: &mut [u8], copied: *mut u64) -> u64 {
        let fits = input.len().min(output.len());
        output[..fits].copy_from_slice(&input[..fits]);
        if !copied.is_null() {
            // SAFETY: a non-null `copied` is writable, as the descriptor declares.
            unsafe { copied.write(fits as u64) };
        }
        sum(&input[..fits])
    }
    fn analyze(request: &[u8], scale: u8) -> Option<Box<Parsed>> {
        Some(Box::new(Parsed { value: sum(request) * u64::from(scale), _live: live()? }))
    }
    fn parse(source: Option<&[u8]>) -> Option<Box<Parsed>> {
        Some(Box::new(Parsed { value: sum(source?), _live: live()? }))
    }
    fn release(parsed: Option<Box<Parsed>>) {
        drop(parsed);
    }
    fn parsedValue(parsed: &Parsed) -> u64 {
        parsed.value
    }
    fn ticketCreate() -> Option<Box<Ticket>> {
        Some(Box::new(Ticket { _live: live()? }))
    }
    fn ticketDestroy(ticket: Box<Ticket>) {
        drop(ticket);
    }
    fn failNext() {
        FAIL.store(true, Ordering::SeqCst);
    }
    fn liveCount() -> i64 {
        LIVE.load(Ordering::SeqCst)
    }
}

seam::define_implementations!(Demo, seam);
// The same entry points also satisfy the ABI pins.
seam::assert_implementations!(seam);
