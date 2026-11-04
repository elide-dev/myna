"""Matched SVM/native leaf floors and bounded batch scaling, without instrumentation."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import platform
import random
import shutil
import statistics
import sys

ROOT = Path(__file__).resolve().parents[2]
SOURCE = Path(__file__).resolve().parent
WORK = ROOT / 'build/floor'
sys.path.insert(0, str(ROOT / 'benchmarks/comparison'))
import run as comparison
from support import run, native_image, TARGET

def digest(p):
    return hashlib.sha256(p.read_bytes()).hexdigest()

def build():
    WORK.mkdir(parents=True, exist_ok=True)
    commands=[]
    def execute(*args):
        commands.append(list(map(str,args)))
        return run(*args)
    home=Path(os.environ['GRAALVM_HOME'])
    shared=home/'lib/svm/svm-shared.jar'
    if not shared.is_file():
        raise RuntimeError('The pinned Elide GraalVM shared annotation jar is required for this experiment')
    classes=WORK/'classes'; generated=WORK/'generated'; genclasses=WORK/'generator-classes'
    for p in (classes, generated, genclasses):
        shutil.rmtree(p,ignore_errors=True);p.mkdir()
    generator_sources=sorted((ROOT/'src/main/java').rglob('*.java'))
    execute('javac','-d',genclasses,*generator_sources)
    for seam in (ROOT/'benchmarks/comparison/boundary.seam', SOURCE/'batch.seam',SOURCE/'identity.seam'):
        destination=generated/seam.stem
        execute('java','-cp',genclasses,'dev.elide.myna.cli.Main','generate',seam,'--out',destination,'--target',TARGET)
    cpu_flags=['-march=armv8-a'] if sys.platform=='linux' and platform.machine()=='aarch64' else []
    flags=['-O3',*cpu_flags,'-fPIC','-I',home/'include','-I',home/'include'/('darwin' if sys.platform=='darwin' else 'linux')]
    cc=os.environ['CC']
    for source, output in ((ROOT/'benchmarks/comparison/kernel.c','kernel.o'),(SOURCE/'batch.c','batch.o'),(SOURCE/'native.c','caller.o')):
        execute(cc,*flags,'-c',source,'-o',WORK/output)
    execute(cc,WORK/'caller.o',WORK/'kernel.o',WORK/'batch.o','-o',WORK/'native')
    # Generator names its direct binding according to java_class, not the filename suffix.
    java_sources=[SOURCE/'Floor.java',SOURCE/'FloorMeasure.java',ROOT/'benchmarks/comparison/Measure.java',generated/'boundary/Generated.java',generated/'batch/Batch.java',generated/'identity/Identity.java']
    execute('javac','-cp',shared,'-d',classes,*java_sources)
    args=['-O3','-march=compatibility','--initialize-at-build-time=bench.Generated','-cp',str(classes),
          '-H:+UnlockExperimentalVMOptions','-H:-DeleteLocalSymbols',
          f'-H:NativeLinkerOption={WORK / "kernel.o"}',f'-H:NativeLinkerOption={WORK / "batch.o"}',
          '-H:-UnlockExperimentalVMOptions','bench.Floor','-o',str(WORK/'svm')]
    result=native_image(*args)
    (WORK/'build.log').write_text(result.stdout+result.stderr)
    commands.append([str(home/'bin/native-image'),'-J--enable-native-access=ALL-UNNAMED',
                     '-H:+UnlockExperimentalVMOptions','-H:-CheckToolchain',f'-H:CCompilerPath={cc}',
                     '-H:-UnlockExperimentalVMOptions',*args])
    for binary in ('native','svm'):
        disassembly=execute('llvm-objdump','--disassemble',WORK/binary)
        (WORK/f'{binary}.asm').write_text(disassembly.stdout)
    files=[*SOURCE.glob('*'),ROOT/'benchmarks/comparison/kernel.c',ROOT/'benchmarks/comparison/Measure.java',
           ROOT/'benchmarks/comparison/boundary.seam',ROOT/'benchmarks/comparison/run.py',ROOT/'tests/support.py',
           ROOT/'mise.lock',ROOT/'tools/graalvm.json',*generator_sources]
    versions={name:execute(*command).stdout+'' for name,command in {
        'clang':(cc,'--version'),'graalvm':(home/'bin/native-image','--version')}.items()}
    metadata={'workload':'leaf-floor-v1','revision':execute('git','rev-parse','HEAD').stdout.strip(),
              'dirty':execute('git','status','--porcelain').stdout,'target':TARGET,'commands':commands,'versions':versions,
              'sources':{str(p.relative_to(ROOT)):digest(p) for p in files if p.is_file()},
              'binaries':{str((WORK/p).relative_to(ROOT)):digest(WORK/p) for p in ('native','svm','kernel.o','batch.o','caller.o')},
              'annotation_jar_sha256':digest(shared),'graalvm':json.loads((ROOT/'tools/graalvm.json').read_text()),
              'execution_profile':{'native_image_cpu':'compatibility','c_cpu_flags':cpu_flags,'lto':False}}
    (WORK/'build.json').write_text(json.dumps(metadata,indent=2)+'\n')

def measure(args):
    metadata=json.loads((WORK/'build.json').read_text())
    for group in ('sources','binaries'):
        for name,expected in metadata[group].items():
            if digest(ROOT/name)!=expected: raise RuntimeError('Changed since build: '+name)
    batches=['batch-'+str(n) for n in (4,16,64,256)]
    cases={'native':['control','direct','indirect','identity-direct','identity-indirect',*batches],
           'svm':['control','generated-normal','generated-leaf','manual-leaf','inline-pointer','identity-generated','identity-manual','identity-inline-pointer',*batches,*['java-'+b for b in batches]]}
    results=[];schedule=[(mode,name) for mode,names in cases.items() for name in names]
    random.Random(args.seed).shuffle(schedule)
    logs=args.output.parent/(args.output.stem+'-logs');logs.mkdir(parents=True,exist_ok=True)
    for fork in range(args.forks):
        for mode,name in schedule[fork%len(schedule):]+schedule[:fork%len(schedule)]:
            path=(logs/f'{fork}-{mode}-{name}.jsonl').resolve();path.unlink(missing_ok=True)
            command=([WORK/'svm',f'-Dmyna.bench.results={path}',name,args.iterations] if mode=='svm'
                     else [WORK/'native',name,args.iterations,path])
            print(f'{fork+1}/{args.forks}: {mode}/{name}',flush=True)
            result=run(*command)
            (logs/f'{fork}-{mode}-{name}.log').write_text(result.stdout+result.stderr)
            samples=[json.loads(line) for line in path.read_text().splitlines()]
            if len(samples)!=10 or any(s['ns']<=0 or s['iterations']!=args.iterations or s['case']!=name for s in samples):
                raise RuntimeError('Invalid samples')
            # Verify independently of the implementation/reference loops, using affine exponentiation modulo 2^64.
            mul,add,a,b,n=1,0,1664525,1013904223,args.iterations
            while n:
                if n&1: mul,add=(a*mul)%(1<<64),(a*add+b)%(1<<64)
                a,b,n=(a*a)%(1<<64),(a*b+b)%(1<<64),n//2
            for s in samples:
                expected=(123456789+s['sample']) if name.startswith('identity-') else (mul*(123456789+s['sample'])+add)%(1<<64)
                if s['checksum']%(1<<64)!=expected: raise RuntimeError('Independent checksum mismatch')
            results.append({'mode':mode,'case':name,'fork':fork,'batch':int(name.split('-')[-1]) if 'batch-' in name else 1,
                            'command':list(map(str,command)),'samples':samples})
    data={'build':metadata,'host':{'platform':platform.platform(),'machine':platform.machine(),'cpu':comparison.cpu_identity()},
          'forks':args.forks,'iterations':args.iterations,'seed':args.seed,
          'smoke':args.forks<5 or args.iterations<1_000_000,'results':results}
    args.output.write_text(json.dumps(data,indent=2)+'\n');report(args.output)

def report(path):
    data=json.loads(path.read_text());groups={}
    for r in data['results']:
        groups.setdefault((r['mode'],r['case'],r['batch']),[]).append(statistics.median(s['ns']/s['iterations'] for s in r['samples']))
    lines=['# Native call floor and bounded batching','',f"Source: `{data['build']['revision']}`; {data['forks']} processes/case; smoke: {data['smoke']}.",'',
           'One operation is one scalar call (identity or recurrence, as labeled). Batches retain each noinline native scalar call; no control subtraction.',
           'Native and SVM execute identical linked kernel objects. Java batches use an experimental internal Graal annotation and fixed bounds.', '',
           '| Runtime | Case | Batch | Median ns/op | Process min–max | ns/boundary including work |',
           '|---|---|---:|---:|---:|---:|']
    for (mode,name,batch),values in sorted(groups.items()):
        median=statistics.median(values)
        lines.append(f'| {mode} | {name} | {batch} | {median:.3f} | {min(values):.3f}–{max(values):.3f} | {median*batch:.3f} |')
    path.with_suffix('.md').write_text('\n'.join(lines)+'\n')

if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('command',choices=['build','run','report'])
    parser.add_argument('--output',type=Path,default=WORK/'results.json')
    parser.add_argument('--forks',type=int,default=5)
    parser.add_argument('--iterations',type=int,default=16777216)
    parser.add_argument('--seed',type=int,default=1729)
    args=parser.parse_args()
    if args.forks<1 or args.iterations<=0 or args.iterations%256: parser.error('Positive forks and iterations divisible by 256 required')
    if args.command=='build': build()
    elif args.command=='run': measure(args)
    else: report(args.output)
