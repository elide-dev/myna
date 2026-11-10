// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
#[allow(dead_code, non_snake_case)] mod seam { include!(concat!(env!("SEAM_GENERATED"), "/seam.rs")); }
fn main() {
    use seam::ownership::*;
    assert_eq!(unsafe { seam::liveCount() }, 0);
    let owner = Ticket::create().unwrap();
    let borrow = owner.borrow();
    assert!(!borrow.as_ptr().is_null());
    let raw = owner.into_raw();
    assert_eq!(unsafe { seam::liveCount() }, 1);
    let owner = unsafe { Ticket::from_raw(raw) }.unwrap();
    std::thread::spawn(move || drop(owner)).join().unwrap();
    assert_eq!(unsafe { seam::liveCount() }, 0);
    unsafe { seam::failNext(); }
    assert!(Ticket::create().is_none());
    assert!(unsafe { Ticket::from_raw(core::ptr::null_mut()) }.is_none());
    let mut bytes = [1, 2, 255];
    assert_eq!(checksum(&bytes), 258);
    increment(&mut bytes);
    assert_eq!(bytes, [2, 3, 0]);
    assert_eq!(checksum(&[]), 0);
    increment(&mut []);
    // Two slices and a pointer the wrapper cannot vouch for, which makes it an unsafe fn.
    let mut output = [0u8; 2];
    let mut copied = u64::MAX;
    assert_eq!(unsafe { transfer(&bytes, &mut output, &mut copied) }, 5);
    assert_eq!((output, copied), ([2, 3], 2));
    assert_eq!(unsafe { transfer(&[], &mut output, core::ptr::null_mut()) }, 0);
    assert_eq!(unsafe { transfer(&bytes, &mut [], &mut copied) }, 0);
    assert_eq!((output, copied), ([2, 3], 0));
    assert!(std::panic::catch_unwind(|| {
        let _owner = Ticket::create().unwrap();
        panic!("release during unwinding");
    }).is_err());
    assert_eq!(unsafe { seam::liveCount() }, 0);
    println!("Rust ownership passed");
}
