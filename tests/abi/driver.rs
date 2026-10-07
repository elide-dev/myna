mod bindings { include!(concat!(env!("SEAM_GENERATED"), "/seam.rs")); }
use core::ffi::c_void;
unsafe extern "C" {
    fn graal_create_isolate(params: *mut c_void, isolate: *mut *mut c_void, thread: *mut *mut c_void) -> i32;
    fn graal_tear_down_isolate(thread: *mut c_void) -> i32;
}
fn main() {
    unsafe {
        let mut isolate = core::ptr::null_mut();
        let mut thread = core::ptr::null_mut();
        assert_eq!(graal_create_isolate(core::ptr::null_mut(), &mut isolate, &mut thread), 0);
        let bytes = [1_u8, 2, 255, 4];
        assert_eq!(bindings::sum(thread, bytes.as_ptr(), bytes.len() as i64), 262);
        assert_eq!(bindings::sum(thread, bytes.as_ptr(), 0), 0);
        assert_eq!(bindings::sum(thread, bytes.as_ptr(), -1), -1);
        assert_eq!(bindings::classify(thread, 12345, false), 127, "small-value probe mask");
        assert_eq!(bindings::classify(thread, -300, false), -1);
        assert_eq!(bindings::classify(thread, 300, false), 1);
        assert_eq!(bindings::classify(thread, 0, true), -128);
        assert!(bindings::isHigh(thread, 200));
        assert!(!bindings::isHigh(thread, 5));
        assert_eq!(bindings::echo(thread, 200), 200);
        assert_eq!(graal_tear_down_isolate(thread), 0);
    }
    println!("Rust -> Native Image -> Rust ABI round trip passed");
}
