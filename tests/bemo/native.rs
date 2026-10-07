#![no_std]
#[unsafe(no_mangle)]
pub unsafe extern "C" fn bemo_invoke(
    callback: unsafe extern "C" fn(*mut core::ffi::c_void, i64) -> i64,
    thread: *mut core::ffi::c_void,
    value: i64,
) -> i64 {
    unsafe { callback(thread, value) }
}
