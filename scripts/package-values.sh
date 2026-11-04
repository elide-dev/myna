#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
elide build :values-jar
cargo package --locked -p myna-values --allow-dirty
scripts/python.sh scripts/package-values.py
