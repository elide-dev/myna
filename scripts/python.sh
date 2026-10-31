#!/usr/bin/env bash
# Elide 1.6.1 does not forward host env into Python, even with --allow-env.
# Keep this tool-only allowlist until elide-dev/runtime#2690 is implemented.
set -euo pipefail
cd "$(dirname "$0")/.."
args=()
for key in PATH HOME USER TMPDIR RUSTUP_HOME CARGO_HOME LLVM_BIN LLVM_CONFIG MYNA_BIN SDKROOT; do
  value="${!key-}"
  if [[ -n "$value" ]]; then
    args+=(--env "$key" "$value")
  fi
done
exec elide python scripts/run_python.py "${args[@]}" -- "$@"
