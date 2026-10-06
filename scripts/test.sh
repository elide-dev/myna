#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
mvn -B verify
scripts/build.sh --llvm
python3 tests/e2e.py
python3 tests/llvm_integration.py
python3 tests/native_image_integration.py
