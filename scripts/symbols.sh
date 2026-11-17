#!/bin/sh
# Copyright the Myna contributors.
# SPDX-License-Identifier: Apache-2.0 OR MIT
# Extract symbols before stripping; keep the original build ID/UUID for lookup.
set -eu
binary=${1:?Usage: symbols.sh BINARY}
name=$(basename "$binary")
output=$(dirname "$binary")/symbols
mkdir -p "$output"
llvm=${LLVM_BIN:-${ELIDE_TOOLCHAIN_HOME:?Pinned LLVM toolchain required}/bin}
case "$(uname -s)" in
  Darwin)
    original=$("$llvm/llvm-dwarfdump" --uuid "$binary" | awk '{print $2}')
    test -n "$original"
    "$llvm/dsymutil" --flat --out="$output/$name.dsym" "$binary"
    "$llvm/llvm-strip" --discard-all "$binary"
    companion=$("$llvm/llvm-dwarfdump" --uuid "$output/$name.dsym" | awk '{print $2}')
    stripped=$("$llvm/llvm-dwarfdump" --uuid "$binary" | awk '{print $2}')
    test -n "$original"
    test "$original" = "$companion"
    test "$original" = "$stripped"
    echo "$name UUID: $original"
    ;;
  Linux)
    original=$("$llvm/llvm-readelf" --notes "$binary" | sed -n 's/.*Build ID: //p')
    test -n "$original"
    # Compress DWARF (ELF SHF_COMPRESSED, zlib) as the sidecar is written: the
    # 0.19.0 applicator's sidecar was 175 MB raw and 66 MB compressed. zlib, not
    # zstd, so older gdb, lldb and elfutils debuginfod clients can still read it.
    # The debuglink below hashes these final bytes, so its CRC matches.
    "$llvm/llvm-objcopy" --only-keep-debug --compress-debug-sections=zlib "$binary" "$output/$name.debug"
    case "$("$llvm/llvm-readelf" --sections "$output/$name.debug")" in
      *".debug_info"*) ;;
      *) echo "Missing DWARF in $name sidecar" >&2; exit 1 ;;
    esac
    "$llvm/llvm-strip" --strip-unneeded "$binary"
    "$llvm/llvm-objcopy" --add-gnu-debuglink="$output/$name.debug" "$binary"
    companion=$("$llvm/llvm-readelf" --notes "$output/$name.debug" | sed -n 's/.*Build ID: //p')
    stripped=$("$llvm/llvm-readelf" --notes "$binary" | sed -n 's/.*Build ID: //p')
    test -n "$original"
    test "$original" = "$companion"
    test "$original" = "$stripped"
    echo "$name build ID: $original"
    ;;
  *) echo "Unsupported symbol platform" >&2; exit 1 ;;
esac
