// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
//! Arbitrary bytes through the SVM UTF-8 bridge, checked against Rust's decoder.
#![no_main]
#[path = "../../tests/support/tape_model.rs"]
#[allow(dead_code)]
mod tape_model;

libfuzzer_sys::fuzz_target!(|data: &[u8]| {
    tape_model::check_utf16(data);
});
