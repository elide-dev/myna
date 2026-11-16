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
    assert!(std::panic::catch_unwind(|| {
        let _owner = Ticket::create().unwrap();
        panic!("release during unwinding");
    }).is_err());
    assert_eq!(unsafe { seam::liveCount() }, 0);
    batches();
    println!("Rust ownership passed");
}

// The fixture's batch digest: the count, then each element's length and bytes, or NULL_ELEMENT
// for an absent one; the empty batch, passed as null arrays, folds in ABSENT instead.
const FNV_OFFSET: u64 = 0xcbf2_9ce4_8422_2325;
const NULL_ELEMENT: u64 = u64::MAX - 1;
fn mix(hash: u64, value: u64) -> u64 { (hash ^ value).wrapping_mul(0x100_0000_01b3) }
fn fold(hash: u64, bytes: &[u8]) -> u64 {
    bytes.iter().fold(mix(hash, bytes.len() as u64), |hash, &byte| mix(hash, u64::from(byte)))
}
fn fold_batch(hash: u64, buffers: &[Option<&[u8]>]) -> u64 {
    if buffers.is_empty() { return mix(hash, 1 << 32); }
    buffers.iter().fold(mix(hash, buffers.len() as u64), |hash, buffer| match buffer {
        Some(bytes) => fold(hash, bytes),
        None => mix(hash, NULL_ELEMENT),
    })
}

fn batches() {
    use seam::ownership::*;
    // Three buffers of different sizes, one of them empty: the native side folds exactly them.
    let (a, b): ([u8; 3], [u8; 1]) = ([1, 2, 3], [255]);
    let three: [&[u8]; 3] = [&a, &[], &b];
    assert_eq!(digest(&three), fold_batch(FNV_OFFSET, &[Some(&a), Some(&[]), Some(&b)]));
    // The empty batch passes null arrays with count zero.
    assert_eq!(digest(&[]), fold_batch(FNV_OFFSET, &[]));
    // More than eight buffers take the heap path.
    let many: Vec<Vec<u8>> = (0..20u8).map(|i| (0..i).collect()).collect();
    let slices: Vec<&[u8]> = many.iter().map(Vec::as_slice).collect();
    let expected: Vec<Option<&[u8]>> = slices.iter().copied().map(Some).collect();
    assert_eq!(digest(&slices), fold_batch(FNV_OFFSET, &expected));
    // Two batches beside a single buffer; an absent element of deps is a null with length zero.
    let key = [7u8, 8];
    let deps: [Option<&[u8]>; 3] = [Some(&b), None, Some(&[])];
    let expected = fold_batch(fold_batch(fold(FNV_OFFSET, &key), &[Some(&a), Some(&b)]), &deps);
    assert_eq!(merge(&key, &[&a, &b], &deps), expected);
    let expected = fold_batch(fold_batch(fold(FNV_OFFSET, &key), &[]), &[]);
    assert_eq!(merge(&key, &[], &[]), expected);
    // The implementation side's view of the raw arrays.
    let pointers = [a.as_ptr().cast_mut(), core::ptr::null_mut(), b.as_ptr().cast_mut()];
    let lengths = [3u32, 0, 1];
    let view = unsafe { seam::BufferBatch::from_raw(pointers.as_ptr(), lengths.as_ptr(), 3u64) };
    assert_eq!(view.len(), 3);
    assert_eq!(view.get(2), Some(&b[..]));
    assert_eq!(view.get(3), None);
    assert_eq!(view.collect::<Vec<_>>(), [&a[..], &[], &b[..]]);
    let view = unsafe { seam::OptionalBufferBatch::from_raw(pointers.as_ptr(), lengths.as_ptr(), 3u32) };
    assert_eq!(view.collect::<Vec<_>>(), [Some(&a[..]), None, Some(&b[..])]);
    let mut view = unsafe { seam::BufferBatch::<u64>::from_raw(core::ptr::null(), core::ptr::null(), 0i32) };
    assert_eq!((view.len(), view.next()), (0, None));
}
