# Copyright the Myna contributors.
# SPDX-License-Identifier: Apache-2.0 OR MIT
"""Compile binding ABI 2 Java sources and retain their Native Image configuration.

The pinned Graal SDK jars are compile-only; they must not be bundled into the app.
"""
import argparse
import os
from pathlib import Path
import shutil
import subprocess


def compile_bindings(output, generated, sources, classpath=()):
    home = Path(os.environ['GRAALVM_HOME'])
    sdk = [home / 'lib/svm' / name for name in ('svm-shared.jar', 'svm-guest-staging.jar', 'svm-guest.jar')]
    for jar in sdk:
        if not jar.is_file():
            raise RuntimeError('Myna binding ABI 2 requires the pinned Elide Graal SDK: missing ' + str(jar))
    output = Path(output)
    output.mkdir(parents=True, exist_ok=True)
    subprocess.run(['javac', '-cp', os.pathsep.join(map(str, [*sdk, *classpath])), '-d', str(output),
                    *map(str, sources)], check=True)
    for directory in generated:
        metadata = Path(directory) / 'META-INF'
        if metadata.is_dir():
            shutil.copytree(metadata, output / 'META-INF', dirs_exist_ok=True)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--out', required=True, type=Path)
    parser.add_argument('--generated', required=True, action='append', type=Path)
    parser.add_argument('--classpath', default='')
    parser.add_argument('sources', nargs='*', type=Path, help='Additional application sources; all generated Java files are included')
    args = parser.parse_args()
    sources = [p for directory in args.generated for p in sorted(directory.glob('*.java'))] + args.sources
    compile_bindings(args.out, args.generated, sources, filter(None, args.classpath.split(os.pathsep)))
