"""Retain call-site evidence from a floor workflow's full disassembly artifact."""
import argparse
import hashlib
import json
from pathlib import Path
import re

parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('artifacts',type=Path)
parser.add_argument('output',type=Path)
args=parser.parse_args()
metadata=json.loads((args.artifacts/'build.json').read_text())
retained=[];summary={}
for runtime in ('native','svm'):
    path=args.artifacts/(runtime+'.asm');text=path.read_text()
    functions=re.findall(r'^([0-9a-f]+ <([^>]+)>:\n.*?)(?=\n\n|\Z)',text,re.M|re.S)
    selected=[]
    for block,name in functions:
        symbol=name.lstrip('_')
        wanted=(runtime=='native' and (symbol.startswith(('identity_','myna_bench_identity','direct','indirect')))) or (
            runtime=='svm' and ('Identity_leaf_' in symbol or 'Generated_leaf_' in symbol
                               or 'Floor_leaf_' in symbol or 'Floor_identity_' in symbol or 'Floor_javaBatch' in symbol or ('Floor$$' in symbol and '_run_' in symbol)))
        if wanted:
            instructions=[l for l in block.splitlines()[1:] if re.match(r'\s*[0-9a-f]+:',l)]
            selected.append({'symbol':name,'instructions':len(instructions)})
            retained.extend([f'# {runtime}: {name}',block,''])
    if not selected: raise SystemExit('Missing floor symbols in '+str(path))
    if runtime=='svm' and any('LongLeaf_invoke_' in name for _,name in functions):
        raise SystemExit('Inlineable leaf adapter unexpectedly retains a standalone invoke stub')
    if runtime=='svm' and metadata.get('binding_abi', 1)>=2:
        forbidden=('Identity_leaf_', 'Generated_leaf_', 'leaf_Leaf_invoke_')
        if any(any(part in name for part in forbidden) for _,name in functions):
            raise SystemExit('Binding ABI 2 unexpectedly retains a standalone generated leaf adapter')
    summary[runtime]={'full_disassembly_sha256':hashlib.sha256(path.read_bytes()).hexdigest(),'functions':selected}
args.output.parent.mkdir(parents=True,exist_ok=True)
args.output.with_suffix('.asm').write_text('\n'.join(retained)+'\n')
summary['build']=metadata
summary['retained_disassembly_sha256']=hashlib.sha256(args.output.with_suffix('.asm').read_bytes()).hexdigest()
args.output.with_suffix('.json').write_text(json.dumps(summary,indent=2)+'\n')
print('Retained native baselines, handwritten leaf stubs and SVM loops; inlineable adapters have no standalone stub')
