#!/bin/sh
# Copyright the Myna contributors.
# SPDX-License-Identifier: Apache-2.0 OR MIT
set -eu
cd "$(dirname "$0")/.."
elide build :values-jar
cargo package --locked -p myna-values --allow-dirty
scripts/python.sh scripts/package-values.py
