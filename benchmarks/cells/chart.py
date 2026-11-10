# Copyright the Myna contributors.
# SPDX-License-Identifier: Apache-2.0 OR MIT
"""Render Truffle cell call costs against the direct-call floor and NFI, from one host."""
import argparse
import datetime
import json
from pathlib import Path
import statistics
import sys

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / 'comparison'))
import brand

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('results', type=Path)
parser.add_argument('--host', required=True, help='host label, such as "Threadripper PRO 9965WX · Linux x86-64"')
parser.add_argument('--date', required=True, type=datetime.date.fromisoformat)
parser.add_argument('--output', type=Path, default=brand.ROOT / 'docs/charts/truffle-cells')
args = parser.parse_args()
data = json.loads(args.results.read_text())
if data['smoke'] or data['forks'] < 5 or not data['host']['cpu'].strip():
    raise SystemExit('A full measured baseline with CPU provenance is required')
if any(not s['compiled'] for r in data['results'] for s in r['samples']):
    raise SystemExit('Every measured sample must run in runtime-compiled code')
groups = brand.process_medians(data['results'], key=lambda r: r['case'])
brand.fonts()
cell = lambda case, label, sublabel: {'label': label, 'sublabel': sublabel, 'color': brand.MYNA,
                                      'values': groups[case], 'emphasis': True}
floor = lambda case: {'label': 'Direct binding call', 'sublabel': 'plain Java, the floor', 'color': brand.REFERENCE,
                      'values': groups[case]}
chart_groups = [
    {'title': 'Leaf import', 'subtitle': 'binding ABI 2 default', 'rows': [
        cell('typed-leaf', 'Myna cell', 'typed glue · executeLong'),
        cell('interop-leaf', 'Myna cell via interop', 'InteropLibrary · executable'),
        cell('typed-ffm-leaf', 'Myna FFM cell', 'FFM backend · critical downcall'),
        floor('direct-leaf'),
        {'label': 'Truffle NFI', 'sublabel': 'libffi backend · same symbol', 'color': brand.BASELINE,
         'values': groups['nfi-leaf']}]},
    {'title': 'Transitioning import', 'subtitle': 'native_leaf=false', 'rows': [
        cell('typed-step', 'Myna cell', 'direct AOT stub'),
        floor('direct-step'),
        {'label': 'Handwritten boundary', 'sublabel': '@TruffleBoundary', 'color': brand.BASELINE,
         'values': groups['hand-step']}]},
]
hero = None
if max(groups['typed-leaf']) < min(groups['nfi-leaf']):  # Claim a ratio only when process ranges separate.
    hero = (f"{statistics.median(groups['nfi-leaf']) / statistics.median(groups['typed-leaf']):.0f}×",
            'less time per leaf call than Truffle NFI')
limit = max(max(r['values']) for g in chart_groups for r in g['rows'])
step = next(s for s in (1, 2, 2.5, 5, 10, 20, 50) if limit * 1.1 / s <= 8)
xmax = step * (int(limit * 1.1 / step) + 1)
revision = data['build'].get('verified_source_commit', data['build']['revision'])[:7]
chart = brand.RangeChart(
    eyebrow='Myna · Truffle native cells',
    title='Guest calls into C, at the direct-call cost',
    subtitle='Runtime-compiled Truffle root · dependent scalar recurrence · lower is better',
    groups=chart_groups, xmax=xmax, xticks=[i * step for i in range(int(round(xmax / step)) + 1)],
    hero=hero, salt='myna-truffle-cells', pitch=36,
    notes=[f"{data['forks']} fresh processes per case, 10 warmup + 10 measured samples of {data['iterations']:,} calls, "
           'every sample in compiled code. Bars and dots: median of process medians; whiskers: their min–max.',
           'Includes callee and loop; no control subtraction. All rows call the same native functions on one Native Image.'],
    provenance=f"{args.host} · source {revision} · measured {args.date:%B} {args.date.day}, {args.date.year}")
chart.render(args.output)
brand.manifest(args.output, [args.results], __file__)
