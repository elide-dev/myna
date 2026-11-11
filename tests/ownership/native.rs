#![no_std]
// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
use core::sync::atomic::{AtomicBool, AtomicI64, Ordering};
static LIVE: AtomicI64 = AtomicI64::new(0);
static FAIL: AtomicBool = AtomicBool::new(false);
unsafe extern "C" { fn malloc(size: usize) -> *mut u8; fn free(ptr: *mut u8); }
#[unsafe(no_mangle)] pub unsafe extern "C" fn ticket_create() -> *mut u8 {
    if FAIL.swap(false, Ordering::SeqCst) { return core::ptr::null_mut(); }
    let p = unsafe { malloc(1) };
    if !p.is_null() { LIVE.fetch_add(1, Ordering::SeqCst); }
    p
}
#[unsafe(no_mangle)] pub unsafe extern "C" fn ticket_destroy(p: *mut u8) {
    unsafe { free(p); }
    LIVE.fetch_sub(1, Ordering::SeqCst);
}
#[unsafe(no_mangle)] pub extern "C" fn ticket_live() -> i64 { LIVE.load(Ordering::SeqCst) }
#[unsafe(no_mangle)] pub extern "C" fn ticket_fail_next() { FAIL.store(true, Ordering::SeqCst); }
#[unsafe(no_mangle)] pub unsafe extern "C" fn buffer_checksum(p: *const u8, count: u64) -> u64 {
    unsafe { core::slice::from_raw_parts(p, count as usize) }.iter().map(|&v| v as u64).sum()
}
#[unsafe(no_mangle)] pub unsafe extern "C" fn buffer_increment(p: *mut u8, count: u64) {
    for v in unsafe { core::slice::from_raw_parts_mut(p, count as usize) } { *v = v.wrapping_add(1); }
}
// Two borrowed buffers with lengths of their own, beside a pointer that is not a buffer: copies
// what fits, stores how many bytes that was through `copied` when it is non-null, and returns
// their sum. Raw accesses throughout: nothing here assumes the buffers are disjoint.
#[unsafe(no_mangle)] pub unsafe extern "C" fn buffer_transfer(input: *const u8, count: u64, output: *mut u8, capacity: u32, copied: *mut u64) -> u64 {
    let fits = core::cmp::min(count, capacity as u64) as usize;
    let mut sum = 0u64;
    for i in 0..fits {
        let value = unsafe { input.add(i).read() };
        unsafe { output.add(i).write(value); }
        sum += value as u64;
    }
    if !copied.is_null() { unsafe { copied.write(fits as u64); } }
    sum
}
// An optional output buffer: `text` may be null with capacity zero. Writes each input byte plus
// one into what fits of a buffer that is present and returns the bytes the whole output needs,
// with ABSENT set when there is no buffer. A buffer that is present never has the null address,
// even when it is empty; null with a capacity breaks the contract and returns u64::MAX.
const ABSENT: u64 = 1 << 32;
#[unsafe(no_mangle)] pub unsafe extern "C" fn buffer_decode(data: *const u8, length: u64, text: *mut u8, capacity: u32) -> u64 {
    if text.is_null() { return if capacity == 0 { length | ABSENT } else { u64::MAX }; }
    let fits = core::cmp::min(length, capacity as u64) as usize;
    for i in 0..fits { unsafe { text.add(i).write(data.add(i).read().wrapping_add(1)); } }
    length
}
// One optional input buffer: the sum of its bytes, or ABSENT for the null pointer with count zero.
#[unsafe(no_mangle)] pub unsafe extern "C" fn buffer_tally(bytes: *const u8, count: u64) -> u64 {
    if bytes.is_null() { return if count == 0 { ABSENT } else { u64::MAX }; }
    (0..count as usize).map(|i| u64::from(unsafe { bytes.add(i).read() })).sum()
}
