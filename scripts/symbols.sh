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
    "$llvm/dsymutil" --flat --out="$output/$name.dsym" "$binary"
    "$llvm/llvm-strip" --discard-all "$binary"
    "$llvm/llvm-dwarfdump" --uuid "$binary" "$output/$name.dsym"
    ;;
  Linux)
    "$llvm/llvm-objcopy" --only-keep-debug "$binary" "$output/$name.debug"
    "$llvm/llvm-strip" --strip-unneeded "$binary"
    "$llvm/llvm-objcopy" --add-gnu-debuglink="$output/$name.debug" "$binary"
    "$llvm/llvm-readelf" --notes "$binary" "$output/$name.debug"
    ;;
  *) echo "Unsupported symbol platform" >&2; exit 1 ;;
esac
