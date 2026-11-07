# Copyright the Myna contributors.
# SPDX-License-Identifier: Apache-2.0 OR MIT
"""Regression checks for source selection and non-destructive notice insertion."""
import contextlib
import importlib.util
import io
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

spec = importlib.util.spec_from_file_location('notices', Path(__file__).parents[1] / 'scripts/source-notices.py')
notices = importlib.util.module_from_spec(spec)
spec.loader.exec_module(notices)


class SourceNoticesTests(unittest.TestCase):
    def test_fix_preserves_shebang_and_is_idempotent(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            source = root / 'run.py'
            original = '#!/usr/bin/env python3\n"""Module documentation."""\nprint(42)\n'
            source.write_text(original)
            with patch.object(notices, 'ROOT', root), patch.object(notices, 'sources', return_value=[(Path('run.py'), '#')]), contextlib.redirect_stdout(io.StringIO()):
                with self.assertRaisesRegex(SystemExit, 'run.py'):
                    notices.check()
                notices.check(fix=True)
                fixed = source.read_text()
                self.assertTrue(fixed.startswith('#!/usr/bin/env python3\n# Copyright'))
                self.assertEqual(fixed.replace('# ' + notices.COPYRIGHT + '\n# ' + notices.LICENSE + '\n', ''), original)
                notices.check(fix=True)
                self.assertEqual(source.read_text(), fixed)
                notices.check()

    def test_selects_maintained_sources_and_preserves_archives(self):
        names = b'src/Main.java\0scripts/native-cc\0llvm/CMakeLists.txt\0third-party/upstream/a.py\0tests/golden/Myna.java\0benchmarks/comparison/baselines/a.py\0README.md\0'
        with patch.object(notices.subprocess, 'check_output', return_value=names):
            self.assertEqual(list(notices.sources()), [(Path('src/Main.java'), '//'), (Path('scripts/native-cc'), '#'), (Path('llvm/CMakeLists.txt'), '#')])

    def test_requires_both_notices(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            source = root / 'a.c'
            with patch.object(notices, 'ROOT', root), patch.object(notices, 'sources', return_value=[(Path('a.c'), '//')]):
                for text in ['// Copyright the Myna contributors.\n', '// ' + notices.LICENSE + '\n']:
                    source.write_text(text)
                    with self.assertRaisesRegex(SystemExit, 'a.c'):
                        notices.check()


if __name__ == '__main__':
    unittest.main()
