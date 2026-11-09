#!/bin/sh
# Copyright the Myna contributors.
# SPDX-License-Identifier: Apache-2.0 OR MIT
set -eu
cd "$(dirname "$0")/.."
mise run check:style
mise run check:java
mise run check:native
python3 -m unittest discover -s tests -p test_build_policy.py
mise run test:rust
mise run test:cpp
mise run test:rust-coverage
mise run test:asan
mise run test:tsan
# Both lanes install the pinned nightly (MYNA_RUST_NIGHTLY) on first use.
if [ "${MYNA_SKIP_NIGHTLY:-0}" != 1 ]; then
  mise run test:miri
  mise run test:rust-fuzz
else
  echo "MYNA_SKIP_NIGHTLY=1: skipping Miri and Rust fuzzing (CI still runs them)" >&2
fi
scripts/test-jvm.sh
scripts/build.sh --llvm
scripts/python.sh tests/e2e.py
mise run test:llvm-coverage
scripts/python.sh tests/native_image_integration.py
scripts/python.sh tests/truffle_integration.py
