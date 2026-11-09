// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
//! Expected-failure canary: a one-byte heap overread in instrumented Rust code.
//! `scripts/rust-fuzz.sh` requires ASan to report it before any campaign runs,
//! proving the targets are built with `-Zsanitizer=address`, not just sancov.
#![no_main]

libfuzzer_sys::fuzz_target!(|data: &[u8]| {
    if data.first() == Some(&b'!') {
        let heap = data.to_vec().into_boxed_slice();
        // SAFETY: deliberately not safe; reads one byte past the allocation.
        let past = unsafe { heap.as_ptr().add(heap.len()).read_volatile() };
        std::hint::black_box(past);
    }
});
