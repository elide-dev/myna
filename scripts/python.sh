#!/usr/bin/env bash
# Elide does not forward host env into Python, even with --allow-env.
# Keep this tool-only allowlist until elide-dev/runtime#2690 is implemented.
set -euo pipefail
cd "$(dirname "$0")/.."
args=()
for key in PATH HOME USER TMPDIR RUSTUP_HOME CARGO_HOME LLVM_BIN LLVM_CONFIG MYNA_BIN MYNA_LLVM LLVM_PROFILE_FILE MYNA_PYTHON_TIMEOUT SDKROOT GRAALVM_HOME JAVA_HOME ELIDE_TOOLCHAIN_HOME CC CXX AR RANLIB CMAKE_TOOLCHAIN_FILE CXXSTDLIB CARGO_TARGET_AARCH64_APPLE_DARWIN_LINKER CARGO_TARGET_X86_64_UNKNOWN_LINUX_GNU_LINKER CARGO_TARGET_AARCH64_UNKNOWN_LINUX_GNU_LINKER; do
  value="${!key-}"
  if [[ -n "$value" ]]; then
    args+=(--env "$key" "$value")
  fi
done
if [[ -n "${MYNA_PYTHON_TIMEOUT-}" ]]; then
  exec elide python --timeout "$MYNA_PYTHON_TIMEOUT" scripts/run_python.py "${args[@]}" -- "$@"
fi
exec elide python scripts/run_python.py "${args[@]}" -- "$@"
