#![cfg_attr(not(feature = "std"), no_std)]
#[unsafe(no_mangle)]
pub unsafe extern "C" fn seam_inspect(value: *const i64) -> i64 {
    unsafe { *value }
}
#[unsafe(no_mangle)]
pub extern "C" fn seam_widen(signed_byte: i8, unsigned_byte: u8, signed_short: i16, unsigned_short: u16, flag: bool) -> i32 {
    signed_byte as i32 + unsigned_byte as i32 + signed_short as i32 + unsigned_short as i32 + if flag { 1000 } else { 0 }
}
#[unsafe(no_mangle)]
pub extern "C" fn seam_low_unsigned(value: i32) -> u8 {
    value as u8
}
#[unsafe(no_mangle)]
pub extern "C" fn seam_low_signed(value: i32) -> i8 {
    value as i8
}
