# Copyright the Myna contributors.
# SPDX-License-Identifier: Apache-2.0 OR MIT
"""Check maintained source notices; --fix adds notices without changing code.

Generated golden seams and archived benchmark outputs retain their original bytes.
Vendored sources retain upstream notices and licensing.
"""
import argparse
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[1]
COPYRIGHT = 'Copyright the Myna contributors.'
LICENSE = 'SPDX-License-Identifier: Apache-2.0 OR MIT'
CODE = {'.java', '.rs', '.c', '.h', '.hpp', '.cpp', '.inc', '.pkl', '.gradle'}
HASH = {'.py', '.sh', '.cmake'}
EXCLUDED = ('third-party/', 'tests/golden/', 'benchmarks/comparison/baselines/')


def sources():
    names = subprocess.check_output(['git', 'ls-files', '-z'], cwd=ROOT).decode().split('\0')
    for name in filter(None, names):
        path = Path(name)
        if name.startswith(EXCLUDED):
            continue
        if path.suffix in CODE:
            yield path, '//'
        elif path.suffix in HASH or path.name in {'CMakeLists.txt', 'Dockerfile', 'native-cc', 'native-cxx'}:
            yield path, '#'


def check(fix=False):
    errors = []
    count = 0
    for path, comment in sources():
        source = ROOT / path
        text = source.read_text()
        if LICENSE in text[:1024] and 'Copyright' in text[:1024]:
            count += 1
            continue
        if fix:
            # Preserve executable shebangs as the first line and Python module docstrings.
            header = f'{comment} {COPYRIGHT}\n{comment} {LICENSE}\n'
            if text.startswith('#!'):
                first, rest = text.split('\n', 1)
                text = first + '\n' + header + rest
            else:
                text = header + text
            source.write_text(text)
            count += 1
        else:
            errors.append(str(path))
    if errors:
        raise SystemExit('Missing source notices:\n' + '\n'.join(errors))
    print(f'Copyright and dual-license notices verified for {count} maintained source files')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--fix', action='store_true')
    check(parser.parse_args().fix)
