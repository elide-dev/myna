# Copyright the Myna contributors.
# SPDX-License-Identifier: Apache-2.0 OR MIT
"""Skip heavy CI only when the complete Git diff contains Markdown files alone."""
import os
import subprocess


def requires_full(paths):
    return not paths or any(not path.lower().endswith('.md') for path in paths)


def main():
    base, head = os.environ.get('DIFF_BASE', ''), os.environ.get('DIFF_HEAD', '')
    full = True
    if base and head and set(base) != {'0'}:
        result = subprocess.run(['git', 'diff', '--name-only', '--no-renames', '-z', base, head], capture_output=True, check=True)
        full = requires_full([path for path in result.stdout.decode().split('\0') if path])
    with open(os.environ['GITHUB_OUTPUT'], 'a') as output:
        output.write(f'full={str(full).lower()}\n')
    print('Full validation required' if full else 'Markdown-only change: hygiene and Gate remain active')


if __name__ == '__main__':
    main()
