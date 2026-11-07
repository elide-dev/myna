# Copyright the Myna contributors.
# SPDX-License-Identifier: Apache-2.0 OR MIT
"""Retain uninstrumented SVM text crossover samples with reproducibility metadata."""
import argparse
import hashlib
import json
import os
import platform
from pathlib import Path
import subprocess
import re
import datetime

ROOT = Path(__file__).resolve().parents[2]

def capture(*args):
    return subprocess.check_output(args, cwd=ROOT, text=True).strip()

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--forks', type=int, default=7)
    parser.add_argument('--iterations', type=int, default=100000)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    if args.forks < 1 or args.iterations < 1:
        parser.error('positive forks and iterations required')
    binary = ROOT / 'build/values-test/values-app'
    samples = []
    for fork in range(args.forks):
        text = capture(str(binary), 'strings', str(args.iterations))
        for fixture, size, reader, ns in re.findall(r'materialize (ASCII|Unicode|Escaped) (\d+) bytes (Java|SIMD reader): ([\d.]+) ns/string', text):
            samples.append(dict(fork=fork, fixture=fixture, utf8_bytes=int(size), reader=reader, ns=float(ns)))
    if len(samples) != args.forks * 48:
        raise RuntimeError('Missing benchmark cases')
    paths = [binary, ROOT / 'tests/values/ValuesMain.java', ROOT / 'tests/values/src/lib.rs',
             ROOT / 'target/release/libmyna_values_fixture.a',
             *sorted((ROOT / 'crates/myna-values/src').glob('*.rs')),
             *sorted((ROOT / 'src/values/java').rglob('*.java'))]
    result = dict(timestamp=datetime.datetime.now(datetime.timezone.utc).isoformat(),
                  platform=platform.platform(), machine=platform.machine(),
                  cpu=capture('sysctl', '-n', 'machdep.cpu.brand_string') if platform.system() == 'Darwin' else Path('/proc/cpuinfo').read_text(),
                  toolchain=capture('elide-toolchain', 'version'), java=capture('java', '--version'),
                  clang=capture(os.environ['CC'], '--version'), rust=capture('rustc', '-vV'),
                  build_environment={name: os.environ.get(name, '') for name in ('CC', 'CXX', 'CFLAGS', 'CXXFLAGS', 'RUSTFLAGS', 'SDKROOT')},
                  commit=capture('git', 'rev-parse', 'HEAD'), dirty=bool(capture('git', 'status', '--porcelain')),
                  native_image_flags=['-O2'], iterations=args.iterations,
                  sha256={str(p.relative_to(ROOT)): hashlib.sha256(p.read_bytes()).hexdigest() for p in paths},
                  note='Normal native transition; same Rust storage, callback, reusable reader and volatile sink. Strings already decoded; escaped fixture contains decoded JSON escape characters. Includes descriptor rebinding and callback overhead. No cross-machine absolute comparison.',
                  samples=samples)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(result, indent=2) + '\n')
    print(args.output)

if __name__ == '__main__':
    main()
