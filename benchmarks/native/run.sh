#!/bin/sh
# Copyright the Myna contributors.
# SPDX-License-Identifier: Apache-2.0 OR MIT
set -eu
for workload in inline owned borrowed; do
  build/bench-native/values "$workload" --require-instrumentation
done
