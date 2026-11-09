#!/bin/sh
# Copyright the Myna contributors.
# SPDX-License-Identifier: Apache-2.0 OR MIT
# Run myna-values' integration tests under Miri on the pinned nightly, checking
# the crate's unsafe code (builder byte appends, borrowed regions, raw-view
# readers) for undefined behavior. simdutf is foreign C++ and cannot be
# interpreted: tests that must reach it are ignored under cfg(miri), and the
# arbitrary-tape harness keeps text below simdutf's 64-byte threshold. Isolation
# is disabled only so proptest can resolve its working directory.
set -eu
cd "$(dirname "$0")/.."
toolchain=${MYNA_RUST_NIGHTLY:?run through mise so the pinned nightly is set}
rustup toolchain install "$toolchain" --profile minimal --component miri,rust-src --no-self-update
export RUSTUP_TOOLCHAIN="$toolchain"
cargo miri setup
MIRIFLAGS="-Zmiri-disable-isolation" PROPTEST_CASES=8 \
  cargo miri test -p myna-values --locked --test values --test tape
