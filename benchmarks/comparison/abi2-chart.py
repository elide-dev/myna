# Copyright the Myna contributors.
# SPDX-License-Identifier: Apache-2.0 OR MIT
"""Render matched identity-call floors from one host; never combine timings across hosts."""
import argparse
import datetime
import json
from pathlib import Path
import statistics
import sys

sys.path.insert(0, str(Path(__file__).resolve().parent))
import brand

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('results', type=Path)
parser.add_argument('--host', required=True, help='host label, such as "Threadripper PRO 9965WX · Linux x86-64"')
parser.add_argument('--date', required=True, type=datetime.date.fromisoformat)
parser.add_argument('--output', type=Path, default=brand.ROOT / 'docs/charts/default-leaf')
args = parser.parse_args()
data = json.loads(args.results.read_text())
if data['build'].get('binding_abi') != 2:
    raise SystemExit('Binding ABI 2 measurements required')
if data['smoke'] or data['forks'] < 5 or not data['host']['cpu'].strip():
    raise SystemExit('Full measured baselines with CPU identity required')
groups = brand.process_medians(data['results'])
generated, manual = groups[('svm', 'identity-generated')], groups[('svm', 'identity-manual')]
brand.fonts()
rows = [
    {'label': 'Myna generated binding', 'sublabel': 'binding ABI 2 default leaf', 'color': brand.MYNA,
     'values': generated, 'emphasis': True},
    {'label': 'Handwritten @CFunction', 'sublabel': 'transition = NO_TRANSITION', 'color': brand.BASELINE,
     'values': manual},
    {'label': 'Native C, function pointer', 'sublabel': 'reference floor · unrolled ×4', 'color': brand.REFERENCE,
     'values': groups[('native', 'identity-indirect-unrolled4')]},
]
hero = None
if max(generated) < min(manual):  # Claim a ratio only when process ranges separate.
    hero = (f"{statistics.median(manual) / statistics.median(generated):.2f}×",
            'less time per call than handwritten @CFunction')
limit = max(max(r['values']) for r in rows)
step = .25 if limit < 1.5 else .5
xmax = step * (int(limit * 1.22 / step) + 1)
revision = data['build'].get('verified_source_commit', data['build']['revision'])[:7]
chart = brand.RangeChart(
    eyebrow='Myna · native call floor',
    title='Direct C calls from Native Image',
    subtitle='Identity function and caller loop · time per call · lower is better',
    groups=[{'title': args.host, 'subtitle': 'one image build · one native kernel object', 'rows': rows}],
    xmax=xmax, xticks=[i * step for i in range(int(round(xmax / step)) + 1)],
    hero=hero, salt='myna-default-leaf',
    notes=[f"{data['forks']} fresh processes per case, 10 warmup + 10 measured samples of "
           f"{data['iterations']:,} calls. Bars and dots: median of process medians; whiskers: their min–max.",
           'Includes callee and loop; no control subtraction. Leaf calls are bounded, nonblocking and '
           'never re-enter Java.'],
    provenance=f"{data['host']['machine']} · source {revision} · measured {args.date:%B} {args.date.day}, {args.date.year}")
chart.render(args.output)
brand.manifest(args.output, [args.results], __file__)
