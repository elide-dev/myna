// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
//! Property tests over arbitrary, malformed tapes read by `myna_tape_text_v1`.
//! The same harness backs `fuzz/fuzz_targets/tape_text.rs`; failures found by
//! either can be replayed here by adding the input bytes to `regressions`.
#[path = "support/tape_model.rs"]
mod tape_model;

use proptest::prelude::*;
use tape_model::{Tape, check_utf16};

fn config() -> ProptestConfig {
    ProptestConfig {
        // Miri interprets every instruction; keep its run short but nonzero.
        cases: if cfg!(miri) { 24 } else { 4096 },
        failure_persistence: None,
        ..ProptestConfig::default()
    }
}

proptest! {
    #![proptest_config(config())]
    #[test]
    fn arbitrary_tapes_match_reference(data in proptest::collection::vec(any::<u8>(), 0..1024)) {
        Tape::from_bytes(&data).check();
    }
}

proptest! {
    #![proptest_config(config())]
    #[cfg_attr(miri, ignore = "simdutf transcoding is foreign C++ code")]
    #[test]
    fn arbitrary_bytes_transcode_like_rust(data in proptest::collection::vec(any::<u8>(), 0..512)) {
        check_utf16(&data);
    }
}

/// Deterministic shapes that random bytes reach rarely, plus fuzz regressions.
#[test]
fn regressions() {
    let cases: &[&[u8]] = &[
        &[],
        &[0],
        // Header faults: version, flags, empty, short counts, misaligned tables.
        &[
            0xa0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        ],
        &[
            0xa0, 4, 1, 1, 2, 0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 6,
        ],
        &[
            0xa0, 4, 1, 1, 2, 0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 7,
        ],
        // Null first region with nonzero declared length.
        &[0x50, 0, 1, 4, 1, 2, 3, 4, 1, 0, 0, 2, 0, 1, 1],
        // Owned bytes holding invalid UTF-8, read through region 0.
        &[0, 8, 200, 0xff, 200, 0xc0, 0, 0, 1, 1, 0, 1, 0, 0],
    ];
    for case in cases {
        Tape::from_bytes(case).check();
    }
    for bytes in [
        &b""[..],
        b"\xff",
        b"\xed\xa0\x80",
        "é🦀".as_bytes(),
        &[b'a'; 100],
    ] {
        if !cfg!(miri) {
            check_utf16(bytes);
        }
    }
}
