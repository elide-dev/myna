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
cases=[('native','identity-indirect-unrolled4','Native C pointer · unrolled ×4','#94a3b8'),
       ('svm','identity-manual','Handwritten SVM leaf','#94a3b8'),
       ('svm','identity-generated','Myna default · ABI 2','#166534')]
groups=[]
for d in datasets:
    g={}
    for r in d['results']:
        g.setdefault((r['mode'],r['case']),[]).append(statistics.median(s['ns']/s['iterations'] for s in r['samples']))
    groups.append(g)
plt.rcParams.update({'font.family':'DejaVu Sans','font.size':11,'svg.hashsalt':'myna-leaf-floor','svg.fonttype':'none'})
fig,axes=plt.subplots(2,1,figsize=(11,7.2),sharex=True,facecolor='white')
fig.subplots_adjust(left=.30,right=.94,top=.79,bottom=.22,hspace=.55)
limit=max(max(g[(mode,case)]) for g in groups for mode,case,_,_ in cases)*1.17
for ax,g,title in zip(axes,groups,('Linux ARM64 · hosted runner','macOS ARM64 · Apple M5 Max')):
    for y,(mode,case,label,color) in enumerate(cases):
        values=g[(mode,case)]; median=statistics.median(values)
        ax.barh(y,median,height=.52,color=color,zorder=3)
        ax.errorbar(median,y,xerr=[[median-min(values)],[max(values)-median]],fmt='none',ecolor='#243746',capsize=3,linewidth=1,zorder=4)
        ax.text(max(values)+limit*.018,y,f'{median:.3f} ns',va='center',fontsize=11,
                fontweight='bold' if case=='identity-generated' else 'normal',color='#172b3a')
    ax.set_yticks(range(len(cases)),[c[2] for c in cases],color='#172b3a')
    ax.invert_yaxis();ax.set_xlim(0,limit)
    ax.set_title(title,loc='left',fontweight='bold',fontsize=12,color='#172b3a',pad=13)
    ax.grid(axis='x',color='#e7edf1',linewidth=.8,zorder=0)
    ax.spines[:].set_visible(False)
    ax.tick_params(axis='both',length=0,pad=8,labelsize=10,colors='#526578')
axes[-1].set_xlabel('Nanoseconds per identity call · lower is better',fontsize=10,color='#526578',labelpad=12)
fig.text(.055,.93,'Native leaf call overhead',fontsize=24,fontweight='bold',color='#172b3a')
fig.text(.055,.875,'Fast by default. Generated calls eliminate the extra SVM leaf stub.',fontsize=12,color='#526578')
fig.text(.055,.10,'5 fresh processes/case · Median latency; whiskers show process min–max\n'
         'Primitive identity function + caller loop; no control subtraction. Compare within each host.',
         fontsize=9,color='#526578',linespacing=1.6)
fig.text(.055,.035,'Binding ABI 2 · Short, bounded, nonblocking calls with no Java reentry.\n'
         + 'Sources: '+ ' / '.join(d['build'].get('verified_source_commit',d['build']['revision'])[:7] for d in datasets),
         fontsize=8,color='#526578',linespacing=1.6)
args.output.parent.mkdir(parents=True,exist_ok=True)
fig.savefig(args.output.with_suffix('.svg'),metadata={'Date':None})
fig.savefig(args.output.with_suffix('.png'),dpi=160,metadata={'Software':'Myna abi2-chart.py'})
plt.close(fig)
svg=args.output.with_suffix('.svg');svg.write_text('\n'.join(line.rstrip() for line in svg.read_text().splitlines())+'\n')
files=[*inputs,Path(__file__),Path(__file__).with_name('requirements.txt'),svg,args.output.with_suffix('.png')]
manifest={str(p.resolve().relative_to(ROOT)):hashlib.sha256(p.read_bytes()).hexdigest() for p in files}
args.output.with_suffix('.manifest.json').write_text(json.dumps(manifest,indent=2)+'\n')
