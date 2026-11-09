// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
//! Arbitrary node, region and byte tables through `myna_tape_text_v1`, checked
//! against the safe reference model shared with `tests/tape.rs`.
#![no_main]
#[path = "../../tests/support/tape_model.rs"]
#[allow(dead_code)]
mod tape_model;

libfuzzer_sys::fuzz_target!(|data: &[u8]| {
    tape_model::Tape::from_bytes(data).check();
});
