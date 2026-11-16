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
    // A nullable buffer is an Option of the slice. None reaches native code as the null pointer
    // with length zero, which the fixture reports as ABSENT; a slice that is present, empty or
    // not, never does. No pointer passes through, so both wrappers are safe.
    const ABSENT: u64 = 1 << 32;
    let mut text = [0u8; 2];
    assert_eq!(decode(&bytes, Some(&mut text)), 3);
    assert_eq!(text, [3, 4]);
    assert_eq!(decode(&bytes, None), 3 | ABSENT);
    assert_eq!(decode(&bytes, Some(&mut [])), 3);
    assert_eq!(decode(&[], None), ABSENT);
    let optional: Option<&mut [u8]> = if bytes[0] == 2 { Some(&mut text[..1]) } else { None };
    assert_eq!(decode(&[9], optional), 1);
    assert_eq!(text, [10, 4]);
    assert_eq!(tally(Some(&bytes)), 5);
    assert_eq!(tally(Some(&[])), 0);
    assert_eq!(tally(None), ABSENT);
    // Two constructors of one resource, each named after its import, each lending its buffer for
    // the call only: the resource keeps what it computed, not the buffer.
    let mut source = [1u8, 2, 3];
    let parsed = Parsed::parse(Some(&source)).unwrap();
    source[0] = 100;
    assert_eq!(unsafe { seam::parsedValue(parsed.borrow().as_ptr()) }, 6);
    assert!(Parsed::parse(None).is_none());
    let empty = Parsed::parse(Some(&[])).unwrap();
    assert_eq!(unsafe { seam::parsedValue(empty.borrow().as_ptr()) }, 0);
    let analyzed = Parsed::analyze(&source, 2).unwrap();
    assert_eq!(unsafe { seam::parsedValue(analyzed.borrow().as_ptr()) }, 210);
    assert_eq!(unsafe { seam::liveCount() }, 3);
    unsafe { seam::failNext(); }
    assert!(Parsed::analyze(&source, 1).is_none());
    drop((parsed, empty));
    std::thread::spawn(move || drop(analyzed)).join().unwrap();
    // The release accepts null as a no-op; no owner ever passes it one.
    unsafe { seam::release(core::ptr::null_mut()); }
    assert_eq!(unsafe { seam::liveCount() }, 0);
    // Status plus out-pointer: Ok adopts the handle; Err carries the status, with the message in
    // the buffer the caller lent and nothing created.
    let mut error = [0u8; 32];
    let surface = Surface::surfaceOpen(7, Some(&mut error)).unwrap();
    assert_eq!(unsafe { seam::surfaceWidth(surface.borrow().as_ptr()) }, 7);
    let from = Surface::surfaceFrom(&[1, 2, 3], None).unwrap();
    assert_eq!(unsafe { seam::surfaceWidth(from.borrow().as_ptr()) }, 6);
    assert_eq!(Surface::surfaceOpen(-1, Some(&mut error)).err(), Some(seam::SurfaceStatus::NEGATIVE));
    assert_eq!(&error[..14], b"negative width");
    unsafe { seam::failNext(); }
    assert_eq!(Surface::surfaceFrom(&[1], None).err(), Some(seam::SurfaceStatus::FAILED));
    assert_eq!(unsafe { seam::liveCount() }, 2);
    drop((surface, from));
    assert_eq!(unsafe { seam::liveCount() }, 0);
    assert!(std::panic::catch_unwind(|| {
        let _owner = Ticket::create().unwrap();
        panic!("release during unwinding");
    }).is_err());
    assert_eq!(unsafe { seam::liveCount() }, 0);
    println!("Rust ownership passed");
}
