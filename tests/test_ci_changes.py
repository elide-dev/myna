# Copyright the Myna contributors.
# SPDX-License-Identifier: Apache-2.0 OR MIT
"""Keep documentation skips conservative across mixed changes and renames."""
import importlib.util
from pathlib import Path
import unittest

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


if __name__ == '__main__':
    unittest.main()
