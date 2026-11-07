#!/bin/sh
# Called by mise after loading tools; tolerate a not-yet-installed tool during
# initial setup, without falling back to an unrelated system compiler.
myna_user_cc=${CC-}
myna_user_cxx=${CXX-}
myna_toolchain_install=$(mise where github:elide-dev/toolchain 2>/dev/null || true)
if [ -x "$myna_toolchain_install/elide-toolchain/bin/elide-toolchain" ]; then
  ELIDE_TOOLCHAIN_HOME="$myna_toolchain_install/elide-toolchain"
  LLVM_BIN="$ELIDE_TOOLCHAIN_HOME/bin"
  LLVM_CONFIG="$LLVM_BIN/llvm-config"
  export ELIDE_TOOLCHAIN_HOME LLVM_BIN LLVM_CONFIG
  case "$(uname -s)-$(uname -m)" in
    Darwin-arm64)
      myna_target=arm64-apple-darwin
      SDKROOT=$(xcrun --show-sdk-path)
      export SDKROOT
      ;;
    Linux-x86_64) myna_target=x86_64-unknown-linux-gnu ;;
    Linux-aarch64) myna_target=aarch64-unknown-linux-gnu ;;
    *) echo "Unsupported Myna native host" >&2; return 1 ;;
  esac
  eval "$("$LLVM_BIN/elide-toolchain" env --target "$myna_target")"
  CC=${myna_user_cc:-"$MYNA_ROOT/scripts/native-cc"}
  CXX=${myna_user_cxx:-"$MYNA_ROOT/scripts/native-cxx"}
  export CC CXX
  if [ "$myna_target" = arm64-apple-darwin ]; then
    CARGO_TARGET_AARCH64_APPLE_DARWIN_LINKER="$CC"
    export CARGO_TARGET_AARCH64_APPLE_DARWIN_LINKER
  fi
  CXXSTDLIB=c++
  export CXXSTDLIB
fi

GRAALVM_HOME="$MYNA_ROOT/.dev/toolchains/graalvm"
JAVA_HOME="$GRAALVM_HOME"
export GRAALVM_HOME JAVA_HOME
PATH="$GRAALVM_HOME/bin:$PATH"
export PATH
