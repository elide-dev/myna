# Copyright the Myna contributors.
# SPDX-License-Identifier: Apache-2.0 OR MIT
"""Truffle cell call costs from runtime-compiled code on a Native Image, without instrumentation.

Every case is one process running one Truffle root that contains the whole measured loop; the root
compiles immediately in the foreground and each measured sample must observe compiled code. Cases
compare the generated cells (language glue and the generic interop wrapper) with plain Java calls
to the same bindings, Truffle NFI as the dynamic-FFI baseline, and borrow strategies by size.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import platform
import random
import re
import shutil
import statistics
import sys

ROOT = Path(__file__).resolve().parents[2]
SOURCE = Path(__file__).resolve().parent
WORK = ROOT / 'build/cells'
TRUFFLE_JAR = ROOT / '.dev/artifacts/jar/myna-truffle/myna-truffle.jar'
sys.path.insert(0, str(ROOT / 'benchmarks/comparison'))
sys.path.insert(0, str(ROOT / 'tests'))
import run as comparison
from support import run, native_image, TARGET
from truffle_integration import truffle_jars

WORKLOAD = 'truffle-cells-v1'
SIZES = (16, 256, 1024, 4096, 65536)
SCALAR = ['control', 'direct-leaf', 'direct-step', 'direct-ffm-leaf', 'direct-ffm-step', 'glue-leaf', 'glue-step',
          'glue-ffm-leaf', 'glue-ffm-step', 'typed-leaf', 'typed-step', 'typed-ffm-leaf', 'typed-ffm-step', 'interop-leaf', 'interop-step', 'nfi-leaf', 'direct-text', 'glue-text', 'nfi-text',
          'hand-step', 'glue-boundary-step']
# A ten-argument entry (two byte buffers, two opaque pointers, integers), as a leaf and a transition.
SLAB = ['direct-slab', 'invoke-slab', 'invoke-slab-step', 'glue-slab-native', 'glue-slab-heap', 'glue-slab-bytes-heap', 'interop-slab-native', 'direct-slab-step', 'hand-slab-step',
        'glue-slab-step-native', 'glue-boundary-slab-step-native', 'interop-slab-step-native']
SLAB_DELTA = 256 + 7 + 7 + 3 - 1 + 7 + 11 + 2
# Pinning one heap array by hand (behind a boundary, and visible to partial evaluation) against the
# cell, for a leaf and across a transition.
PIN_SIZE = 256
PINS = [f'{kind}-{PIN_SIZE}' for kind in ('glue-sum-mixed', 'hand-sum-pin', 'inline-sum-pin', 'direct-sum-step', 'glue-sum-step-native',
                                          'glue-sum-step-pin', 'hand-sum-step-pin', 'inline-sum-step-pin')]
BORROWS = [f'{kind}-{size}' for size in SIZES for kind in ('direct-sum', 'glue-sum-native', 'glue-sum-pin', 'glue-sum-copy')]
CASES = SCALAR + SLAB + BORROWS + PINS
MODULES = ('truffle-api', 'truffle-runtime', 'truffle-compiler', 'polyglot', 'collections', 'nativeimage', 'word',
           'jniutils', 'nativebridge', 'truffle-nfi', 'truffle-nfi-libffi')

def digest(p):
    return hashlib.sha256(p.read_bytes()).hexdigest()

def iterations_for(name, iterations):
    """Borrow cases scale the call count with the buffer so every sample sums a similar byte volume."""
    if '-sum-' not in name:
        return iterations
    size = int(name.rsplit('-', 1)[1])
    return max(65536, iterations * 64 // max(size, 64)) // 256 * 256

def expected(name, count, seed):
    if '-sum-' in name:
        size = int(name.rsplit('-', 1)[1])
        return (seed + count * sum((i * 31 + 7) & 0xff for i in range(size))) % (1 << 64)
    if '-slab' in name:
        return (seed + count * SLAB_DELTA) % (1 << 64)
    if name.endswith('-text'):
        return (seed + count * len('hello world')) % (1 << 64)
    mul, add, a, b, n = 1, 0, 1664525, 1013904223, count
    while n:
        if n & 1: mul, add = (a * mul) % (1 << 64), (a * add + b) % (1 << 64)
        a, b, n = (a * a) % (1 << 64), (a * b + b) % (1 << 64), n // 2
    return (mul * seed + add) % (1 << 64)

def build():
    WORK.mkdir(parents=True, exist_ok=True)
    commands = []
    def execute(*args):
        commands.append(list(map(str, args)))
        return run(*args)
    if not TRUFFLE_JAR.is_file():
        raise RuntimeError('myna-truffle.jar is missing; run `mise run build:jvm`')
    home = Path(os.environ['GRAALVM_HOME'])
    jars = truffle_jars()
    classes = WORK / 'classes'; generated = WORK / 'generated'; genclasses = WORK / 'generator-classes'
    for p in (classes, generated, genclasses):
        shutil.rmtree(p, ignore_errors=True); p.mkdir()
    generator_sources = sorted((ROOT / 'src/main/java').rglob('*.java'))
    execute('javac', '-d', genclasses, *generator_sources)
    # The same seam three times in one image: Native Image cells (Cells), FFM cells (FfmCells) and
    # Native Image cells with transitions behind a boundary (BoundaryCells).
    native_seam = SOURCE / 'cells.seam'
    ffm_seam = WORK / 'cells-ffm.seam'
    ffm_seam.write_text(native_seam.read_text().replace('java_class=Cells truffle_cells=true',
                                                        'java_class=FfmCells truffle_cells=true truffle_backend=ffm'))
    boundary_seam = WORK / 'cells-boundary.seam'
    boundary_seam.write_text(native_seam.read_text().replace('java_class=Cells truffle_cells=true',
                                                             'java_class=BoundaryCells truffle_cells=true truffle_transitions=boundary'))
    for seam, name in ((native_seam, 'native'), (ffm_seam, 'ffm'), (boundary_seam, 'boundary')):
        execute('java', '-cp', genclasses, 'dev.elide.myna.cli.Main', 'generate', seam, '--out', generated / name, '--target', TARGET)
    cpu_flags = ['-march=armv8-a'] if sys.platform == 'linux' and platform.machine() == 'aarch64' else []
    flags = ['-O3', *cpu_flags, '-fPIC', '-I', home / 'include', '-I', home / 'include' / ('darwin' if sys.platform == 'darwin' else 'linux')]
    cc = os.environ['CC']
    execute(cc, *flags, '-c', ROOT / 'benchmarks/comparison/kernel.c', '-o', WORK / 'kernel.o')
    execute(cc, *flags, '-c', SOURCE / 'native.c', '-o', WORK / 'cells.o')
    library = WORK / ('libcellsbench.dylib' if sys.platform == 'darwin' else 'libcellsbench.so')
    execute(cc, '-dynamiclib' if sys.platform == 'darwin' else '-shared', WORK / 'kernel.o', WORK / 'cells.o', '-o', library)
    java_sources = [SOURCE / 'CellBench.java', SOURCE / 'Bodies.java', SOURCE / 'BenchLanguage.java',
                    *(generated / 'native' / n for n in ('Cells.java', 'CellsTruffle.java')),
                    *(generated / 'boundary' / n for n in ('BoundaryCells.java', 'BoundaryCellsTruffle.java')),
                    *(generated / 'ffm' / n for n in ('FfmCells.java', 'FfmCellsFFM.java', 'FfmCellsTruffle.java', 'FfmCellsForeignFeature.java'))]
    sdk = [home / 'lib/svm' / name for name in ('svm-shared.jar', 'svm-guest-staging.jar', 'svm-guest.jar')]
    # The benchmark language (NFI loader) is the one DSL-processed source; generated cells stay DSL-free.
    execute('javac', '-processorpath', jars['truffle-dsl-processor'],
            '-cp', os.pathsep.join(map(str, [*sdk, TRUFFLE_JAR, *jars.values()])), '-d', classes, *java_sources)
    for name in ('native', 'ffm', 'boundary'):
        shutil.copytree(generated / name / 'META-INF', classes / 'META-INF', dirs_exist_ok=True)
    module_path = os.pathsep.join(str(jars[name]) for name in MODULES)
    args = ['-O3', '-march=compatibility', '--macro:truffle-svm', '--enable-native-access=ALL-UNNAMED',
            '--module-path', module_path, '-cp', os.pathsep.join(map(str, [classes, TRUFFLE_JAR])),
            '--features=bench.FfmCellsForeignFeature',
            # Consumer node classes must initialize at build time, or partial evaluation stops at them.
            '--initialize-at-build-time=bench.nodes',
            '-H:+UnlockExperimentalVMOptions', '-H:-DeleteLocalSymbols',
            f'-H:NativeLinkerOption={WORK / "kernel.o"}', f'-H:NativeLinkerOption={WORK / "cells.o"}',
            '-H:-UnlockExperimentalVMOptions', 'bench.CellBench', '-o', str(WORK / 'svm')]
    # One -O3 truffle-svm image with every benchmark cell: slower than the test images, and
    # over five minutes on the shared ARM runners.
    result = native_image(*args, timeout=1200)
    (WORK / 'build.log').write_text(result.stdout + result.stderr)
    commands.append([str(home / 'bin/native-image'), '-J--enable-native-access=ALL-UNNAMED',
                     '-H:+UnlockExperimentalVMOptions', '-H:-CheckToolchain', f'-H:CCompilerPath={cc}',
                     '-H:-UnlockExperimentalVMOptions', *args])
    files = [*sorted(SOURCE.glob('*')), ROOT / 'benchmarks/comparison/kernel.c', ROOT / 'tests/support.py',
             ROOT / 'tests/truffle_integration.py', ROOT / 'tools/graalvm.json', ROOT / 'tools/truffle.json', *generator_sources]
    versions = {name: execute(*command).stdout for name, command in {
        'clang': (cc, '--version'), 'graalvm': (home / 'bin/native-image', '--version')}.items()}
    metadata = {'workload': WORKLOAD, 'binding_abi': 2, 'revision': execute('git', 'rev-parse', 'HEAD').stdout.strip(),
                'dirty': execute('git', 'status', '--porcelain').stdout, 'target': TARGET, 'commands': commands, 'versions': versions,
                'cases': CASES, 'sizes': list(SIZES), 'allocation_scope': 'current-thread-java-heap',
                'sources': {str(p.relative_to(ROOT)): digest(p) for p in files if p.is_file()},
                'binaries': {str((WORK / p).relative_to(ROOT)): digest(WORK / p) for p in ('svm', 'kernel.o', 'cells.o', library.name)},
                'jars': {'myna-truffle': digest(TRUFFLE_JAR), **{name: digest(path) for name, path in jars.items()}},
                'graalvm': json.loads((ROOT / 'tools/graalvm.json').read_text()),
                'execution_profile': {'native_image_cpu': 'compatibility', 'c_cpu_flags': cpu_flags, 'lto': False,
                                      'truffle': {'CompileImmediately': True, 'BackgroundCompilation': False, 'MultiTier': False}}}
    (WORK / 'build.json').write_text(json.dumps(metadata, indent=2) + '\n')

def measure(args):
    cpu = comparison.cpu_identity()
    metadata = json.loads((WORK / 'build.json').read_text())
    for group in ('sources', 'binaries'):
        for name, saved in metadata[group].items():
            if digest(ROOT / name) != saved: raise RuntimeError('Changed since build: ' + name)
    library = next(name for name in metadata['binaries'] if 'libcellsbench' in name)
    schedule = [name for name in CASES if re.search(args.cases, name)]
    if not schedule: raise RuntimeError('No case matches ' + args.cases)
    random.Random(args.seed).shuffle(schedule)
    results = []
    logs = args.output.parent / (args.output.stem + '-logs'); logs.mkdir(parents=True, exist_ok=True)
    for fork in range(args.forks):
        for name in schedule[fork % len(schedule):] + schedule[:fork % len(schedule)]:
            path = (logs / f'{fork}-{name}.jsonl').resolve(); path.unlink(missing_ok=True)
            count = iterations_for(name, args.iterations)
            options = [f'-Dmyna.bench.results={path}', f'-Dmyna.bench.library={ROOT / library}']
            if '-pin-' in name or '-mixed-' in name: options.append('-Dmyna.truffle.pinThreshold=1')
            if '-copy-' in name: options.append('-Dmyna.truffle.pinThreshold=2147483647')
            command = [WORK / 'svm', *options, name, count]
            print(f'{fork + 1}/{args.forks}: {name}', flush=True)
            result = run(*command)
            (logs / f'{fork}-{name}.log').write_text(result.stdout + result.stderr)
            samples = [json.loads(line) for line in path.read_text().splitlines()]
            if len(samples) != 10 or any(s['ns'] <= 0 or s['iterations'] != count or s['case'] != name
                                         or not s['compiled'] or s['allocated_bytes'] < 0 or s['allocation_canary_bytes'] < 4096
                                         for s in samples):
                raise RuntimeError('Invalid samples for ' + name)
            for s in samples:
                if s['checksum'] % (1 << 64) != expected(name, count, 123456789 + s['sample']):
                    raise RuntimeError('Independent checksum mismatch for ' + name)
            # The compilation trace must show the measured root compiled before the first sample.
            if 'opt done' not in result.stdout + result.stderr:
                raise RuntimeError('No Truffle compilation trace for ' + name)
            results.append({'case': name, 'fork': fork, 'iterations': count, 'command': list(map(str, command)), 'samples': samples})
    data = {'build': metadata, 'host': {'platform': platform.platform(), 'machine': platform.machine(), 'cpu': cpu},
            'forks': args.forks, 'iterations': args.iterations, 'seed': args.seed,
            'cases': args.cases,
            'smoke': args.forks < 5 or args.iterations < 1_000_000 or len(schedule) < len(CASES), 'results': results}
    args.output.write_text(json.dumps(data, indent=2) + '\n'); report(args.output)

def report(path):
    data = json.loads(path.read_text()); groups = {}; allocations = {}
    for r in data['results']:
        groups.setdefault(r['case'], []).append(statistics.median(s['ns'] / s['iterations'] for s in r['samples']))
        allocations.setdefault(r['case'], []).append(max(s['allocated_bytes'] / s['iterations'] for s in r['samples']))
    lines = ['# Truffle cell call costs', '', f"Source: `{data['build']['revision']}`; {data['forks']} processes/case; smoke: {data['smoke']}.", '',
             'One operation is one call from a runtime-compiled Truffle root on a Native Image; the loop and the callee are included and the control is not subtracted.',
             'Scalar cases run the dependent recurrence; sum cases lend a pattern buffer of the stated size on every call; text cases lend an 11-byte string.',
             'Allocated bytes are the maximum sample of the current Java thread, so a single allocating call shows up.', '',
             '| Case | Median ns/op | Process min–max | Bootstrap 95% CI | Java heap B/op (max sample) |', '|---|---:|---:|---:|---:|']
    for name in CASES:
        if name not in groups: continue
        values = groups[name]
        rng = random.Random(data['seed'])
        boot = sorted(statistics.median(rng.choices(values, k=len(values))) for _ in range(10000))
        ci = f'{boot[249]:.3f}–{boot[9749]:.3f}' if len(values) >= 5 else 'insufficient forks'
        lines.append(f'| {name} | {statistics.median(values):.3f} | {min(values):.3f}–{max(values):.3f} | {ci} | {max(allocations[name]):.6g} |')
    path.with_suffix('.md').write_text('\n'.join(lines) + '\n')

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('command', choices=['build', 'run', 'report'])
    parser.add_argument('--output', type=Path, default=WORK / 'results.json')
    parser.add_argument('--forks', type=int, default=5)
    parser.add_argument('--iterations', type=int, default=16777216)
    parser.add_argument('--seed', type=int, default=1729)
    parser.add_argument('--cases', default='', help='regular expression selecting cases; a partial run is a smoke run')
    args = parser.parse_args()
    if args.forks < 1 or args.iterations < 256:
        parser.error('forks must be positive and iterations at least 256')
    if args.command == 'build': build()
    elif args.command == 'run': measure(args)
    else: report(args.output)
