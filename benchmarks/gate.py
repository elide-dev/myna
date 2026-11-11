# Copyright the Myna contributors.
# SPDX-License-Identifier: Apache-2.0 OR MIT
"""Fail when a generated binding is slower than its matched handwritten or direct call.

Each pair calls the same native function under the same transition contract, in fresh processes
interleaved on one host, so the ratio of their medians is far steadier than either time. A pair
fails only when both hold:

- the median of the generated case's process medians exceeds the reference's by more than the
  pair's allowance, and
- the two cases separate consistently: with each side's single fastest and slowest process set
  aside, every remaining generated process is slower than every remaining reference process.

One outlier process can therefore neither fail the gate nor hide a gap, and shared-host noise,
which widens process ranges until they overlap, does not fail it either.

A pair's allowance is the default tolerance unless it records a known, explained gap; such an
allowance stops that gap from growing and should shrink when the gap is fixed.
"""
import argparse
import json
from pathlib import Path
import statistics
import sys

# (mode, generated case, reference case, allowance or None for the default, reason).
# Mode is None for suites whose results carry no mode.
PAIRS = {
    'comparison': [
        ('native', 'generated-normal', 'manual-normal', None, ''),
        ('native', 'generated-leaf', 'manual-leaf', None, ''),
        ('native', 'generated-static-jni', 'manual-static-jni', None, ''),
        ('ffm-aot', 'generated-ffm', 'manual-ffm', None, ''),
        # A library binding is an object that owns its library's segments, so its matched
        # handwritten form holds them in instance fields too. Myna's answer to a static handwritten
        # handle is the static entry, gated below against the static handle.
        ('ffm-aot', 'generated-library-ffm', 'manual-instance-library-ffm', None, ''),
        ('ffm-aot', 'generated-critical-ffm', 'manual-bound-critical-ffm', None, ''),
        ('ffm-aot', 'generated-typed-critical-ffm', 'manual-instance-critical-ffm', None, ''),
        ('ffm-aot', 'generated-static-critical-ffm', 'manual-unbound-critical-ffm', None, ''),
    ],
    'floor': [
        ('svm', 'identity-generated', 'identity-manual', None, ''),
        ('svm', 'generated-leaf', 'manual-leaf', None, ''),
    ],
    'cells': [
        (None, 'typed-leaf', 'direct-leaf', None, ''),
        (None, 'typed-step', 'direct-step', None, ''),
        (None, 'typed-ffm-leaf', 'direct-ffm-leaf', None, ''),
        (None, 'typed-ffm-step', 'direct-ffm-step', None, ''),
        # FFM cells lending native memory against the static FFM entry given the same segments: a
        # global one (nothing to check), a confined and a shared one (the scope is checked on both
        # sides), two global and two shared buffers, and a global buffer beside a heap array
        # copied for the call through the same borrow.
        (None, 'typed-ffm-sum-native-256', 'direct-ffm-sum-256', None, ''),
        (None, 'typed-ffm-sum-confined-256', 'direct-ffm-sum-confined-256', None, ''),
        (None, 'typed-ffm-sum-shared-256', 'direct-ffm-sum-shared-256', None, ''),
        (None, 'typed-ffm-slab-native', 'direct-ffm-slab', None, ''),
        (None, 'typed-ffm-slab-shared', 'direct-ffm-slab-shared', None, ''),
        (None, 'typed-ffm-slab-mixed', 'direct-ffm-slab-mixed', None, ''),
        # A create cell against the same create and destroy called directly, over the same native
        # memory. The direct pair writes the caller's own struct; the cell also allocates its owner
        # and the runtime's struct (calloc and free), checks and copies the guest's buffer, and, for
        # the held borrow, keeps the borrow on the owner. Measured on the controlled x86-64 host at
        # e9f004a (before rebasing onto #116, which changed only FFM cells): +92.3% and +113.2%;
        # the allowances leave about 5% of headroom over that.
        (None, 'glue-create-scan-native-256', 'direct-create-scan-256', 1.0,
         'the cell allocates an owner and a runtime struct the direct call does not'),
        (None, 'glue-create-adopt-native-256', 'direct-create-adopt-256', 1.25,
         'as scan, and the owner keeps the held borrow until its release'),
    ],
}


def process_medians(results):
    groups = {}
    for r in results:
        groups.setdefault((r.get('mode'), r['case']), []).append(
            statistics.median(s['ns'] / s['iterations'] for s in r['samples']))
    return groups


def check(data, suite, tolerance):
    groups = process_medians(data['results'])
    failures = []
    for mode, generated, reference, allowance, reason in PAIRS[suite]:
        limit = tolerance if allowance is None else allowance
        label = f"{mode + '/' if mode else ''}{generated} vs {reference}"
        if (mode, generated) not in groups or (mode, reference) not in groups:
            print(f'FAIL {label}: missing from results')
            failures.append(label)
            continue
        g, r = groups[(mode, generated)], groups[(mode, reference)]
        excess = statistics.median(g) / statistics.median(r) - 1
        separated = sorted(g)[1] > sorted(r)[-2]
        failed = excess > limit and separated
        print(f"{'FAIL' if failed else 'ok':4} {label}: {statistics.median(g):.3f} "
              f'({min(g):.3f}-{max(g):.3f}) vs {statistics.median(r):.3f} ({min(r):.3f}-{max(r):.3f}) '
              f"ns/call; {excess:+.1%}, limit {limit:+.1%}, {'separated' if separated else 'overlapping'}"
              + (f'; {reason}' if reason else ''))
        if failed:
            failures.append(label)
    return failures


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('suite', choices=sorted(PAIRS))
    parser.add_argument('results', type=Path)
    parser.add_argument('--tolerance', type=float, default=0.02,
                        help='default allowed excess of the generated median, as a fraction (0.02)')
    args = parser.parse_args()
    data = json.loads(args.results.read_text())
    if data.get('smoke') or data.get('forks', 0) < 5:
        raise SystemExit('Performance gates need a full run of at least five processes per case')
    failures = check(data, args.suite, args.tolerance)
    if failures:
        sys.exit('Generated bindings are slower than their matched references: ' + ', '.join(failures))


if __name__ == '__main__':
    main()
