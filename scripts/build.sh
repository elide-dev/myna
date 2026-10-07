#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
elide build :jar :feature-jar :native
mkdir -p build/dist
cp .dev/artifacts/native-image/myna build/dist/myna
cp .dev/artifacts/jar/myna/myna.jar build/dist/myna.jar
cp .dev/artifacts/jar/myna-feature/myna-feature.jar build/dist/myna-feature.jar
if [ "${1:-}" = "--llvm" ]; then
  LLVM_CONFIG=${LLVM_CONFIG:-llvm-config}
  cmake -S llvm -B build/llvm -DLLVM_DIR="$("$LLVM_CONFIG" --cmakedir)" -DCMAKE_BUILD_TYPE=Release
  cmake --build build/llvm --parallel 4
  cp build/llvm/myna-llvm build/dist/myna-llvm
fi
