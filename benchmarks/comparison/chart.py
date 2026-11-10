# Copyright the Myna contributors.
# SPDX-License-Identifier: Apache-2.0 OR MIT
"""Render measured SVM scalar latency for matched generated/handwritten pairs from one host."""
import argparse
import datetime
import json
from pathlib import Path
import sys

sys.path.insert(0, str(Path(__file__).resolve().parent))
import brand

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('results', type=Path)
parser.add_argument('--host', required=True, help='host label, such as "Threadripper PRO 9965WX · Linux x86-64"')
parser.add_argument('--date', required=True, type=datetime.date.fromisoformat)
parser.add_argument('--output', type=Path, default=brand.ROOT / 'docs/charts/svm-boundaries')
args = parser.parse_args()
data = json.loads(args.results.read_text())
if data['smoke'] or data['forks'] < 5 or not data['host']['cpu'].strip():
    raise SystemExit('A full measured baseline with CPU provenance is required')
groups = brand.process_medians(data['results'])
brand.fonts()
# Each pair calls the same native function under the same transition contract.
pairs = [
    ('Direct C · normal transition', '@CFunction', 'native', 'generated-normal', 'manual-normal'),
    ('Direct C · native leaf', '@CFunction(NO_TRANSITION)', 'native', 'generated-leaf', 'manual-leaf'),
    ('Static JNI', 'checks pending exceptions', 'native', 'generated-static-jni', 'manual-static-jni'),
    ('AOT FFM', 'build-time downcall adapters', 'ffm-aot', 'generated-ffm', 'manual-ffm'),
    ('AOT FFM · native leaf', 'critical downcall, unbound', 'ffm-aot', 'generated-static-critical-ffm',
     'manual-unbound-critical-ffm'),
]
chart_groups = [{'title': title, 'subtitle': subtitle, 'rows': [
    {'color': brand.MYNA, 'values': groups[(mode, generated)], 'emphasis': True},
    {'color': brand.BASELINE, 'values': groups[(mode, manual)]}]}
    for title, subtitle, mode, generated, manual in pairs]
limit = max(max(r['values']) for g in chart_groups for r in g['rows'])
step = next(s for s in (1, 2, 2.5, 5, 10, 20, 50) if limit * 1.18 / s <= 8)
xmax = step * (int(limit * 1.18 / step) + 1)
revision = data['build'].get('verified_source_commit', data['build']['revision'])[:7]
chart = brand.RangeChart(
    eyebrow='Myna · Native Image boundaries',
    title='Generated vs. handwritten bindings',
    subtitle='Dependent scalar recurrence · same native function per pair · lower is better',
    groups=chart_groups, xmax=xmax, xticks=[i * step for i in range(int(round(xmax / step)) + 1)],
    legend=[('Myna generated', brand.MYNA), ('Handwritten', brand.BASELINE)], salt='myna-svm-boundaries',
    pitch=24, label_width=2.95, compact=True,
    notes=[f"{data['forks']} fresh processes per case, 10 warmup + 10 measured samples of {data['iterations']:,} calls. "
           'Bars and dots: median of process medians; whiskers: their min–max.',
           'Includes callee and loop; no control subtraction. Compare within a pair: transition contracts differ across pairs.',
           'Native leaf and critical FFM calls must be bounded and nonblocking, and never re-enter Java.'],
    provenance=f"{args.host} · source {revision} · measured {args.date:%B} {args.date.day}, {args.date.year}")
chart.render(args.output)
brand.manifest(args.output, [args.results], __file__)
