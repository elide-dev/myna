#!/bin/sh
# Copyright the Myna contributors.
# SPDX-License-Identifier: Apache-2.0 OR MIT
set -eu
cd "$(dirname "$0")/.."
elide build :jar :feature-jar :values-jar :truffle-jar
mkdir -p build/dist
# Native Image emits DWARF on Linux; macOS preserves method names in a dSYM.
case "$(uname -s)" in
  Linux) native_debug=-g; native_id=-H:NativeLinkerOption=-Wl,--build-id=sha1 ;;
  *) native_debug=; native_id= ;;
esac
python3 scripts/native-image-build.py \
  -J--enable-native-access=ALL-UNNAMED -O2 ${native_debug:+"$native_debug"} --initialize-at-run-time \
  -H:+UnlockExperimentalVMOptions -H:-DeleteLocalSymbols -H:-StripDebugInfo -H:-CheckToolchain "-H:CCompilerPath=${CC:?Pinned compiler required}" \
  ${native_id:+"$native_id"} -H:-UnlockExperimentalVMOptions \
  -jar .dev/artifacts/jar/myna/myna.jar -o build/dist/myna
scripts/symbols.sh build/dist/myna
cp scripts/compile-native-bindings.py build/dist/compile-native-bindings.py
python3 scripts/reproducible_zip.py .dev/artifacts/jar/myna/myna.jar build/dist/myna.jar
python3 scripts/reproducible_zip.py .dev/artifacts/jar/myna-feature/myna-feature.jar build/dist/myna-feature.jar
python3 scripts/reproducible_zip.py .dev/artifacts/jar/myna-values/myna-values.jar build/dist/myna-values.jar
python3 scripts/reproducible_zip.py .dev/artifacts/jar/myna-truffle/myna-truffle.jar build/dist/myna-truffle.jar
if [ "${1:-}" = "--llvm" ]; then
  LLVM_CONFIG=${LLVM_CONFIG:-llvm-config}
  cmake --fresh -S llvm -B build/llvm -DLLVM_DIR="$("$LLVM_CONFIG" --cmakedir)" -DCMAKE_BUILD_TYPE=Release -DCMAKE_CXX_FLAGS_RELEASE="-O3 -DNDEBUG -g" \
    -DCMAKE_EXPORT_COMPILE_COMMANDS=ON -DCMAKE_TOOLCHAIN_FILE="$PWD/tools/native.cmake"
  cmake --build build/llvm --parallel 4
  cp build/llvm/myna-llvm build/dist/myna-llvm
  scripts/symbols.sh build/dist/myna-llvm
fi
