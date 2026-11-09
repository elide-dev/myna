#!/bin/sh
# Copyright the Myna contributors.
# SPDX-License-Identifier: Apache-2.0 OR MIT
# Fuzz the Rust value-tape reader (myna_tape_text_v1) and UTF-8 bridge with
# cargo-fuzz: libFuzzer plus AddressSanitizer (-Zsanitizer) on the project's
# pinned nightly Rust. These binaries never enter Myna's LLVM/ThinLTO bitcode.
# An expected-failure canary must be reported by ASan before any campaign counts.
#   scripts/rust-fuzz.sh [seconds per target, default 60]
set -eu
cd "$(dirname "$0")/.."
seconds=${1:-60}
fuzz=crates/myna-values/fuzz
tools=build/tools/cargo-fuzz
work=build/rust-fuzz
# Build cargo-fuzz for this host with its own lockfile.
if [ "$("$tools/bin/cargo-fuzz" --version 2>/dev/null || true)" != "cargo-fuzz 0.13.1" ]; then
  cargo install cargo-fuzz --version 0.13.1 --locked --root "$tools"
fi
cargo fetch --locked --manifest-path "$fuzz/Cargo.toml"
rm -rf "$work"
mkdir -p "$work"
fuzz_run() {
  "$tools/bin/cargo-fuzz" fuzz run --fuzz-dir "$fuzz" "$@"
}

printf '!' > "$work/asan-canary-input"
if fuzz_run asan_canary "$work/asan-canary-input" -- -runs=1 > "$work/asan-canary.log" 2>&1; then
  cat "$work/asan-canary.log"
  echo "Rust ASan canary was not detected" >&2
  exit 1
fi
if ! grep -q "AddressSanitizer: heap-buffer-overflow" "$work/asan-canary.log"; then
  cat "$work/asan-canary.log"
  echo "Rust ASan canary failed without an AddressSanitizer report" >&2
  exit 1
fi

for target in tape_text utf8_to_utf16; do
  mkdir -p "$work/$target/corpus"
  if ! fuzz_run "$target" "$work/$target/corpus" -- -seed=1729 -max_len=4096 \
      -max_total_time="$seconds" -timeout=10 -rss_limit_mb=2048 -malloc_limit_mb=256 \
      -artifact_prefix="$work/$target/" > "$work/$target.log" 2>&1; then
    tail -n 80 "$work/$target.log"
    echo "Rust fuzz target $target failed; replay with $tools/bin/cargo-fuzz fuzz run --fuzz-dir $fuzz $target <artifact>" >&2
    exit 1
  fi
  tail -n 1 "$work/$target.log"
done
echo "rust-fuzz: ASan canary and $seconds-second campaigns per target passed"
