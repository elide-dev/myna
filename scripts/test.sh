#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
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
