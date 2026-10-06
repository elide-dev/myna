#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
elide build :jar :feature-jar :native
mkdir -p build/dist
cp .dev/artifacts/native-image/svmgen build/dist/svmgen
cp .dev/artifacts/jar/svmgen/svmgen.jar build/dist/svmgen.jar
cp .dev/artifacts/jar/svmgen-feature/svmgen-feature.jar build/dist/svmgen-feature.jar
if [ "${1:-}" = "--llvm" ]; then
  LLVM_CONFIG=${LLVM_CONFIG:-llvm-config}
  cmake -S llvm -B build/llvm -DLLVM_DIR="$("$LLVM_CONFIG" --cmakedir)" -DCMAKE_BUILD_TYPE=Release
  cmake --build build/llvm --parallel 4
  cp build/llvm/svmgen-llvm build/dist/svmgen-llvm
fi
