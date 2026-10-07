#![cfg_attr(not(feature = "std"), no_std)]
#[allow(dead_code, non_snake_case)]
mod seam { include!(concat!(env!("SEAM_GENERATED"), "/seam.rs")); }
seam::assert_implementations!(seam);
seam::native_image_shims!(seam);
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
#[unsafe(no_mangle)]
pub unsafe extern "C" fn seam_for_each(visitor: seam::Visitor, isolate: *mut core::ffi::c_void, count: i64) -> i32 {
    let Some(visit) = visitor else { return -1 };
    (0..count).map(|i| unsafe { visit(isolate, i) }).sum()
}
unsafe extern "C" fn reduce(left: i64, right: i64) -> i64 {
    left * 10 + right
}
#[unsafe(no_mangle)]
pub extern "C" fn seam_reducer() -> seam::Reducer {
    Some(reduce)
}
#[unsafe(no_mangle)]
pub extern "C" fn seam_swap_pair(value: seam::Pair) -> seam::Pair {
    seam::Pair { left: value.right as i64, right: value.left as i32, seamPadding0: [0; 4] }
}
#[unsafe(no_mangle)]
pub extern "C" fn seam_rotate(value: seam::Triple, times: i32) -> seam::Triple {
    let mut v = [value.a, value.b, value.c];
    v.rotate_left(times.rem_euclid(3) as usize);
    seam::Triple { a: v[0], b: v[1], c: v[2] }
}
