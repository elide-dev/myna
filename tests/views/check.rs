// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
// Compiles the generated views seam under #![deny(warnings)], with debug assertions, and checks
// the counted slices: their elements, index count, and the null rule.
#![deny(warnings)]
#[allow(dead_code, non_snake_case)]
mod seam {
    include!(concat!(env!("SEAM_GENERATED"), "/seam.rs"));
}
use seam::{Function, Program};

static FUNCTIONS: [Function; 2] = [
    Function {
        id: 10,
        arity: 1,
        seamPadding0: [0; 2],
        entry: 0x1000,
    },
    Function {
        id: 11,
        arity: 2,
        seamPadding0: [0; 2],
        entry: 0x2000,
    },
];
static TEXT: [u16; 3] = [b'h' as u16, b'i' as u16, 0xffff];

fn program() -> Program {
    Program {
        functions: FUNCTIONS.as_ptr().cast_mut(),
        functionCount: 2,
        text: TEXT.as_ptr().cast_mut(),
        textLength: 3,
        version: 1,
    }
}

fn empty() -> Program {
    Program {
        functions: core::ptr::null_mut(),
        functionCount: 0,
        text: core::ptr::null_mut(),
        textLength: 0,
        version: 0,
    }
}

#[unsafe(no_mangle)]
pub extern "C" fn views_program() -> *mut Program {
    core::ptr::null_mut()
}
#[unsafe(no_mangle)]
pub extern "C" fn views_empty() -> *mut Program {
    core::ptr::null_mut()
}
seam::assert_implementations!(seam);

#[test]
fn views_have_the_counted_elements() {
    let p = program();
    // SAFETY: the record's pointers address static arrays of the counted lengths.
    let (functions, text) = unsafe { (p.functions(), p.text()) };
    assert_eq!(functions.len(), 2);
    assert_eq!(functions[1].id, 11);
    assert_eq!(functions[1].entry, 0x2000);
    assert_eq!(text, &[b'h' as u16, b'i' as u16, 0xffff]);
}

#[test]
#[should_panic]
fn index_count_panics() {
    let p = program();
    let index = p.functionCount as usize;
    // SAFETY: as above; the index is what is out of bounds.
    let _ = unsafe { p.functions() }[index].id;
}

#[test]
#[should_panic]
fn scalar_index_count_panics() {
    let p = program();
    let index = p.textLength as usize;
    // SAFETY: as above; the index is what is out of bounds.
    let _ = unsafe { p.text() }[index];
}

#[test]
fn null_with_zero_count_is_empty() {
    let p = empty();
    // SAFETY: null pointers with zero counts obey the contract.
    assert!(unsafe { p.functions() }.is_empty());
    assert!(unsafe { p.text() }.is_empty());
}

#[test]
#[should_panic(expected = "Program.text: null with a nonzero count")]
fn null_with_a_count_panics_in_debug() {
    let mut p = empty();
    p.textLength = 1;
    // SAFETY: deliberately breaks the contract; the debug assertion stops it before any read.
    let _ = unsafe { p.text() };
}
