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
scripts/test-jvm.sh
scripts/build.sh --llvm
scripts/python.sh tests/e2e.py
mise run test:llvm-coverage
scripts/python.sh tests/native_image_integration.py
