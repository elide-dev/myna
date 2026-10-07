#![no_std]
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
