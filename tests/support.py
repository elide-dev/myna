"""Standard-library test runner with JUnit XML, also used for native-binary tests."""
import os
from pathlib import Path
import platform
import subprocess
import time
import unittest
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
BINARY = Path(os.environ.get("MYNA_BIN", ROOT / "build/dist/myna")).resolve()
TARGET = {("Darwin", "arm64"): "aarch64-apple-darwin", ("Darwin", "x86_64"): "x86_64-apple-darwin",
          ("Linux", "aarch64"): "aarch64-unknown-linux-gnu", ("Linux", "x86_64"): "x86_64-unknown-linux-gnu"}.get((platform.system(), platform.machine()))

def run(*args, cwd=ROOT, ok=True):
    if str(args[0]) in ("cc", "c++"):
        args = (os.environ["CC" if str(args[0]) == "cc" else "CXX"], *args[1:])
    if str(args[0]) == "rustc":
        args = (*args, "-C", f"linker={os.environ['CC']}")
    if platform.system() == "Darwin" and Path(args[0]).name in ("clang", "clang++"):
        args = (*args, "-isysroot", os.environ["SDKROOT"])
    result = subprocess.run([str(a) for a in args], cwd=cwd, env=dict(os.environ), text=True, capture_output=True, timeout=300)
    if ok and result.returncode:
        raise AssertionError(f"Command failed ({result.returncode}): {args}\n{result.stdout}\n{result.stderr}")
    return result

def compile_native(classes, generated, *sources, classpath=()):
    import importlib.util
    spec = importlib.util.spec_from_file_location("myna_compile", ROOT / "scripts/compile-native-bindings.py")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    module.compile_bindings(classes, [generated], sources, classpath)


def native_image(*args):
    # Keep the direct driver's argument dump inside ignored build output.
    work = ROOT / "build"
    work.mkdir(exist_ok=True)
    return run(Path(os.environ["GRAALVM_HOME"]) / "bin/native-image",
               "-J--enable-native-access=ALL-UNNAMED",
               "-H:+UnlockExperimentalVMOptions", "-H:-CheckToolchain", f"-H:CCompilerPath={os.environ['CC']}",
               "-H:-UnlockExperimentalVMOptions", *args, cwd=work)


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
