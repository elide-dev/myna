#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
scripts/test-jvm.sh
scripts/build.sh --llvm
scripts/python.sh tests/e2e.py
scripts/python.sh tests/llvm_integration.py
scripts/python.sh tests/native_image_integration.py
