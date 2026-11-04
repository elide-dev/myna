#!/bin/sh
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
    "$llvm/dsymutil" --flat --out="$output/$name.dsym" "$binary"
    "$llvm/llvm-strip" --discard-all "$binary"
    companion=$("$llvm/llvm-dwarfdump" --uuid "$output/$name.dsym" | awk '{print $2}')
    stripped=$("$llvm/llvm-dwarfdump" --uuid "$binary" | awk '{print $2}')
    test -n "$original" && test "$original" = "$companion" && test "$original" = "$stripped"
    echo "$name UUID: $original"
    ;;
  Linux)
    original=$("$llvm/llvm-readelf" --notes "$binary" | sed -n 's/.*Build ID: //p')
    "$llvm/llvm-objcopy" --only-keep-debug "$binary" "$output/$name.debug"
    case "$("$llvm/llvm-readelf" --sections "$output/$name.debug")" in
      *".debug_info"*) ;;
      *) echo "Missing DWARF in $name sidecar" >&2; exit 1 ;;
    esac
    "$llvm/llvm-strip" --strip-unneeded "$binary"
    "$llvm/llvm-objcopy" --add-gnu-debuglink="$output/$name.debug" "$binary"
    companion=$("$llvm/llvm-readelf" --notes "$output/$name.debug" | sed -n 's/.*Build ID: //p')
    stripped=$("$llvm/llvm-readelf" --notes "$binary" | sed -n 's/.*Build ID: //p')
    test -n "$original" && test "$original" = "$companion" && test "$original" = "$stripped"
    echo "$name build ID: $original"
    ;;
  *) echo "Unsupported symbol platform" >&2; exit 1 ;;
esac
