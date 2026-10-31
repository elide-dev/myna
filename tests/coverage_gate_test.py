"""Regression tests for the coverage gate used with Elide's JaCoCo reports."""
from pathlib import Path
import sys
import tempfile
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "scripts"))
from check_coverage import check_coverage


class CoverageGateTests(unittest.TestCase):
    def check(self, xml):
        with tempfile.TemporaryDirectory() as directory:
            report = Path(directory) / "coverage.xml"
            report.write_text(xml)
            return check_coverage(report)

    def test_accepts_exact_thresholds(self):
        self.check('<report><counter type="LINE" missed="15" covered="85"/>'
                   '<counter type="BRANCH" missed="30" covered="70"/></report>')

    def test_rejects_line_shortfall(self):
        with self.assertRaisesRegex(ValueError, "LINE"):
            self.check('<report><counter type="LINE" missed="16" covered="84"/>'
                       '<counter type="BRANCH" missed="0" covered="100"/></report>')

    def test_rejects_branch_shortfall(self):
        with self.assertRaisesRegex(ValueError, "BRANCH"):
            self.check('<report><counter type="LINE" missed="0" covered="100"/>'
                       '<counter type="BRANCH" missed="31" covered="69"/></report>')

    def test_rejects_missing_report(self):
        with self.assertRaises((OSError, ValueError)):
            check_coverage(Path("/nonexistent/myna-coverage.xml"))

    def test_rejects_missing_or_nested_only_counters(self):
        for xml in ['<report/>', '<report><package><counter type="LINE" missed="0" covered="1"/>'
                    '<counter type="BRANCH" missed="0" covered="1"/></package></report>']:
            with self.subTest(xml=xml), self.assertRaises(ValueError):
                self.check(xml)

    def test_rejects_zero_or_negative_totals(self):
        for covered, missed in [(0, 0), (-1, 1), (100, -1)]:
            with self.subTest(covered=covered, missed=missed), self.assertRaises(ValueError):
                self.check(f'<report><counter type="LINE" missed="{missed}" covered="{covered}"/>'
                           '<counter type="BRANCH" missed="0" covered="1"/></report>')

    def test_rejects_duplicate_counters(self):
        with self.assertRaises(ValueError):
            self.check('<report><counter type="LINE" missed="0" covered="1"/>'
                       '<counter type="LINE" missed="0" covered="1"/>'
                       '<counter type="BRANCH" missed="0" covered="1"/></report>')

    def test_rejects_malformed_report(self):
        for xml in ['not XML', '<other/>', '<report><counter type="LINE" missed="x" covered="1"/></report>']:
            with self.subTest(xml=xml), self.assertRaises(ValueError):
                self.check(xml)


if __name__ == "__main__":
    unittest.main()
