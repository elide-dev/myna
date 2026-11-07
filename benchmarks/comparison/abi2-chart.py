# Copyright the Myna contributors.
# SPDX-License-Identifier: Apache-2.0 OR MIT
"""Render matched identity-call floors; never combine timings across hosts."""
import argparse
import hashlib
import json
from pathlib import Path
import statistics

import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt

ROOT=Path(__file__).resolve().parents[2]
parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--linux',type=Path,default=ROOT/'benchmarks/comparison/baselines/default-leaf-linux-arm64.json')
parser.add_argument('--macos',type=Path,default=ROOT/'benchmarks/comparison/baselines/default-leaf-macos-arm64.json')
parser.add_argument('--output',type=Path,default=ROOT/'docs/charts/default-leaf')
args=parser.parse_args()
inputs=[args.linux,args.macos]
datasets=[json.loads(p.read_text()) for p in inputs]
for d in datasets:
    if d['build'].get('binding_abi') != 2:
        raise SystemExit('Binding ABI 2 measurements required')
    if d['smoke'] or d['forks']<5 or not d['host']['cpu'].strip():
        raise SystemExit('Full measured baselines with CPU identity required')
cases=[('svm','identity-generated','Myna · default leaf','#69dfad'),
       ('svm','identity-manual','Handwritten SVM leaf','#91b9ff'),
       ('native','identity-indirect-unrolled4','Native C pointer¹','#a6afbd')]
groups=[]
for d in datasets:
    g={}
    for r in d['results']:
        g.setdefault((r['mode'],r['case']),[]).append(statistics.median(s['ns']/s['iterations'] for s in r['samples']))
    groups.append(g)
background='#101820'
foreground='#edf3f8'
muted='#a8b8c8'
plt.rcParams.update({'font.family':'DejaVu Sans','font.size':11,'svg.hashsalt':'myna-leaf-floor','svg.fonttype':'none'})
fig,axes=plt.subplots(1,2,figsize=(12,5.6),facecolor=background)
fig.subplots_adjust(left=.25,right=.95,top=.68,bottom=.29,wspace=.30)
limit=max(max(g[(mode,case)]) for g in groups for mode,case,_,_ in cases)*1.30
for ax,g,title in zip(axes,groups,('Linux ARM64','Apple M5 Max')):
    ax.set_facecolor(background)
    for y,(mode,case,label,color) in enumerate(cases):
        values=g[(mode,case)]; median=statistics.median(values)
        ax.barh(y,limit,height=.38,color='#1e2b38',zorder=1)
        ax.barh(y,median,height=.38,color=color,alpha=.28,zorder=2)
        ax.errorbar(median,y,xerr=[[median-min(values)],[max(values)-median]],
                    fmt='o',color=color,markersize=7,ecolor=color,capsize=4,linewidth=1.5,zorder=4)
        ax.text(limit,y-.32,f'{median:.3f} ns',ha='right',va='center',fontsize=11,
                fontweight='bold',color=color)
    ax.set_ylim(2.6,-.65)
    ax.set_xlim(0,limit)
    ax.set_title(title,loc='left',fontweight='bold',fontsize=14,color=foreground,pad=20)
    ax.set_yticks([])
    ax.spines[:].set_visible(False)
    ax.tick_params(axis='x',length=0,pad=10,labelsize=9,colors=muted)
    ax.xaxis.set_major_locator(plt.MaxNLocator(4))
    ax.grid(axis='x',color='#2b3a48',linewidth=.6,zorder=0)
for y,(_,_,label,color) in enumerate(cases):
    axes[0].text(-.06,y,label,transform=axes[0].get_yaxis_transform(),
                 ha='right',va='center',fontsize=11,color=color,fontweight='bold' if y==0 else 'normal')
fig.text(.055,.91,'Native calls, measured.',fontsize=26,fontweight='bold',color=foreground)
fig.text(.055,.835,'Identity function + caller loop · nanoseconds per call · lower is better',fontsize=11,color=muted)
fig.text(.055,.145,'Dots: median of process medians   /   Whiskers: process min–max   /   5 fresh processes per case',
         fontsize=9,color=foreground)
fig.text(.055,.09,'Compare within each host. No control subtraction. ¹ C pointer caller unrolled ×4.',fontsize=9,color=muted)
fig.text(.055,.035,'Binding ABI 2 · bounded, nonblocking, no Java reentry   /   Sources: '
         + ' / '.join(d['build'].get('verified_source_commit',d['build']['revision'])[:7] for d in datasets),
         fontsize=8,color=muted)
args.output.parent.mkdir(parents=True,exist_ok=True)
fig.savefig(args.output.with_suffix('.svg'),metadata={'Date':None})
fig.savefig(args.output.with_suffix('.png'),dpi=160,metadata={'Software':'Myna abi2-chart.py'})
plt.close(fig)
svg=args.output.with_suffix('.svg');svg.write_text('\n'.join(line.rstrip() for line in svg.read_text().splitlines())+'\n')
files=[*inputs,Path(__file__),Path(__file__).with_name('requirements.txt'),svg,args.output.with_suffix('.png')]
manifest={str(p.resolve().relative_to(ROOT)):hashlib.sha256(p.read_bytes()).hexdigest() for p in files}
args.output.with_suffix('.manifest.json').write_text(json.dumps(manifest,indent=2)+'\n')
