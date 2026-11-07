# Copyright the Myna contributors.
# SPDX-License-Identifier: Apache-2.0 OR MIT
"""Keep documentation skips conservative across mixed changes and renames."""
import importlib.util
from pathlib import Path
import unittest
import os
import tempfile
from types import SimpleNamespace
from unittest.mock import patch

spec = importlib.util.spec_from_file_location('ci_changes', Path(__file__).parents[1] / 'scripts/ci-changes.py')
changes = importlib.util.module_from_spec(spec)
spec.loader.exec_module(changes)


class ChangeClassificationTest(unittest.TestCase):
    def test_markdown_only(self):
        self.assertFalse(changes.requires_full(['README.md', 'docs/values.md']))

    def test_code_config_and_images_require_validation(self):
        for path in ['src/Main.java', '.github/workflows/on.pr.yml', 'docs/charts/default-leaf.svg', 'mise.lock', 'Dockerfile']:
            with self.subTest(path=path):
                self.assertTrue(changes.requires_full(['README.md', path]))

    def test_no_diff_fails_closed(self):
        self.assertTrue(changes.requires_full([]))

    def test_rename_out_of_markdown_requires_validation(self):
        self.assertTrue(changes.requires_full(['docs/sample.md', 'scripts/sample.py']))

    def test_manual_and_first_push_use_full_validation(self):
        for base in ['', '0' * 40]:
            with tempfile.TemporaryDirectory() as directory:
                output = Path(directory) / 'output'
                with patch.dict(os.environ, {'DIFF_BASE': base, 'DIFF_HEAD': 'head', 'GITHUB_OUTPUT': str(output)}), patch.object(changes.subprocess, 'run') as run:
                    changes.main()
                    run.assert_not_called()
                self.assertEqual(output.read_text(), 'full=true\n')

    def test_git_diff_emits_skip_only_for_markdown(self):
        with tempfile.TemporaryDirectory() as directory:
            output = Path(directory) / 'output'
            with patch.dict(os.environ, {'DIFF_BASE': 'base', 'DIFF_HEAD': 'head', 'GITHUB_OUTPUT': str(output)}), patch.object(changes.subprocess, 'run', return_value=SimpleNamespace(stdout=b'README.md\0docs/ci.md\0')) as run:
                changes.main()
                self.assertIn('--no-renames', run.call_args.args[0])
                self.assertTrue(run.call_args.kwargs['check'])
            self.assertEqual(output.read_text(), 'full=false\n')


if __name__ == '__main__':
    unittest.main()
