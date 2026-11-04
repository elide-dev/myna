#!/bin/sh
set -eu
for workload in inline owned borrowed; do
  build/bench-native/values "$workload" --require-instrumentation
done
