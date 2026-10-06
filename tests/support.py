"""Standard-library test runner with JUnit XML, also used for native-binary tests."""
import os
from pathlib import Path
import platform
import subprocess
import time
import unittest
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
BINARY = Path(os.environ.get("SVMGEN_BIN", ROOT / "build/dist/svmgen")).resolve()
TARGET = {("Darwin", "arm64"): "aarch64-apple-darwin", ("Darwin", "x86_64"): "x86_64-apple-darwin",
          ("Linux", "aarch64"): "aarch64-unknown-linux-gnu", ("Linux", "x86_64"): "x86_64-unknown-linux-gnu"}.get((platform.system(), platform.machine()))

def run(*args, cwd=ROOT, ok=True):
    result = subprocess.run([str(a) for a in args], cwd=cwd, text=True, capture_output=True, timeout=300)
    if ok and result.returncode:
        raise AssertionError(f"Command failed ({result.returncode}): {args}\n{result.stdout}\n{result.stderr}")
    return result

class XmlResult(unittest.TextTestResult):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.cases = []
    def startTest(self, test):
        super().startTest(test)
        self.started = time.monotonic()
    def stopTest(self, test):
        case = ET.Element("testcase", name=test._testMethodName, classname=type(test).__name__, time=f"{time.monotonic()-self.started:.3f}")
        for label, results in [("failure", self.failures), ("error", self.errors), ("skipped", self.skipped)]:
            for item, message in results:
                if item is test:
                    ET.SubElement(case, label).text = message
        self.cases.append(case)
        super().stopTest(test)

def main(module, report):
    suite = unittest.defaultTestLoader.loadTestsFromModule(module)
    result = unittest.TextTestRunner(verbosity=2, resultclass=XmlResult).run(suite)
    root = ET.Element("testsuite", name=report, tests=str(result.testsRun), failures=str(len(result.failures)), errors=str(len(result.errors)), skipped=str(len(result.skipped)))
    root.extend(result.cases)
    path = ROOT / "build/reports" / f"{report}.xml"
    path.parent.mkdir(parents=True, exist_ok=True)
    ET.ElementTree(root).write(path, encoding="utf-8", xml_declaration=True)
    raise SystemExit(not result.wasSuccessful())
