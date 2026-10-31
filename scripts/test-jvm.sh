#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
# Never let an old report satisfy the coverage gate after a failed test run.
rm -rf .dev/reports/tests/jvm .dev/reports/coverage/jvm
elide test --coverage
elide python tests/coverage_gate_test.py
elide python scripts/check_coverage.py
