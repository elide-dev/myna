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
