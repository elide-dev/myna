#![cfg_attr(not(feature = "std"), no_std)]
#[unsafe(no_mangle)]
pub unsafe extern "C" fn seam_inspect(value: *const i64) -> i64 {
    unsafe { *value }
}
