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
// Two constructors of one resource, each reading a call-scoped buffer during the call only: the
// resource holds its own copy of what it computed. parse fails on an absent source, and either
// fails after ticket_fail_next. release accepts null as a no-op.
unsafe fn parsed_new(value: u64) -> *mut u64 {
    if FAIL.swap(false, Ordering::SeqCst) { return core::ptr::null_mut(); }
    let p = unsafe { malloc(8) } as *mut u64;
    if !p.is_null() { unsafe { p.write(value); } LIVE.fetch_add(1, Ordering::SeqCst); }
    p
}
#[unsafe(no_mangle)] pub unsafe extern "C" fn parsed_parse(source: *const u8, length: u64) -> *mut u64 {
    if source.is_null() { return core::ptr::null_mut(); }
    let sum = (0..length as usize).map(|i| u64::from(unsafe { source.add(i).read() })).sum();
    unsafe { parsed_new(sum) }
}
#[unsafe(no_mangle)] pub unsafe extern "C" fn parsed_analyze(request: *const u8, length: u32, scale: u8) -> *mut u64 {
    let sum: u64 = (0..length as usize).map(|i| u64::from(unsafe { request.add(i).read() })).sum();
    unsafe { parsed_new(sum * u64::from(scale)) }
}
#[unsafe(no_mangle)] pub unsafe extern "C" fn parsed_release(p: *mut u64) {
    if p.is_null() { return; }
    unsafe { free(p as *mut u8); }
    LIVE.fetch_sub(1, Ordering::SeqCst);
}
#[unsafe(no_mangle)] pub unsafe extern "C" fn parsed_value(p: *const u64) -> u64 { unsafe { p.read() } }
// Status-plus-out-pointer constructors. A negative width fails with status 3 and a message in the
// error buffer when there is one; ticket_fail_next makes the next one fail with status 4. A failed
// call writes junk to the out slot, which callers must ignore.
unsafe fn surface_new(width: i64, out: *mut *mut i64, error: *mut u8, capacity: usize) -> i32 {
    let fail = |message: &[u8], status: i32| {
        if !out.is_null() { unsafe { out.write(0xdead as *mut i64); } }
        if !error.is_null() {
            for (i, &b) in message.iter().take(capacity).enumerate() { unsafe { error.add(i).write(b); } }
        }
        status
    };
    if FAIL.swap(false, Ordering::SeqCst) { return fail(b"failed", 4); }
    if width < 0 { return fail(b"negative width", 3); }
    let p = unsafe { malloc(8) } as *mut i64;
    unsafe { p.write(width); }
    LIVE.fetch_add(1, Ordering::SeqCst);
    unsafe { out.write(p); }
    0
}
#[unsafe(no_mangle)] pub unsafe extern "C" fn surface_open(width: i32, out: *mut *mut i64, error: *mut u8, capacity: usize) -> i32 {
    unsafe { surface_new(i64::from(width), out, error, capacity) }
}
#[unsafe(no_mangle)] pub unsafe extern "C" fn surface_from(source: *const u8, length: u64, out: *mut *mut i64, error: *mut u8, capacity: usize) -> i32 {
    let width = (0..length as usize).map(|i| i64::from(unsafe { source.add(i).read() })).sum();
    unsafe { surface_new(width, out, error, capacity) }
}
#[unsafe(no_mangle)] pub unsafe extern "C" fn surface_close(p: *mut i64) {
    unsafe { free(p as *mut u8); }
    LIVE.fetch_sub(1, Ordering::SeqCst);
}
#[unsafe(no_mangle)] pub unsafe extern "C" fn surface_width(p: *const i64) -> i64 { unsafe { p.read() } }
