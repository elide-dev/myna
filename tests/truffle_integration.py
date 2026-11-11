# Copyright the Myna contributors.
# SPDX-License-Identifier: Apache-2.0 OR MIT
"""Generated Truffle cells: FFM cells on HotSpot, and Native Image cells from runtime-compiled code.

The Truffle jars come from the pinned Elide GraalVM Maven repository (tools/truffle.json), reusing
Elide's dependency cache when its copies match the pinned checksums.
"""
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import unittest
from support import BINARY, ROOT, TARGET, compile_native, main, native_image, run

FIXTURE = ROOT / "tests/truffle"
TRUFFLE_JAR = ROOT / ".dev/artifacts/jar/myna-truffle/myna-truffle.jar"


def truffle_jars():
    """Pinned Truffle API, runtime, compiler and SDK jars, verified by checksum."""
    spec = json.loads((ROOT / "tools/truffle.json").read_text())
    repository = spec["repository"].format(version=spec["version"])
    cache = ROOT / "build/downloads/truffle"
    cache.mkdir(parents=True, exist_ok=True)
    jars = {}
    for name, artifact in spec["artifacts"].items():
        candidates = [ROOT / ".dev/dependencies/m2" / artifact["path"], cache / Path(artifact["path"]).name]
        chosen = next((c for c in candidates if c.is_file() and hashlib.sha256(c.read_bytes()).hexdigest() == artifact["sha256"]), None)
        if chosen is None:
            chosen = candidates[-1]
            subprocess.run(["curl", "--http1.1", "--fail", "--location", "--retry", "3", "--silent", "--show-error",
                            "--output", str(chosen), f"{repository}/{artifact['path']}"], check=True)
            if hashlib.sha256(chosen.read_bytes()).hexdigest() != artifact["sha256"]:
                raise RuntimeError(f"checksum mismatch for {name}")
        jars[name] = chosen
    return jars


def build_truffle_fixture():
    work = ROOT / "build/truffle-test"
    shutil.rmtree(work, ignore_errors=True)
    work.mkdir(parents=True)
    if not TRUFFLE_JAR.is_file():
        raise RuntimeError("myna-truffle.jar is missing; run `elide build :truffle-jar`")
    generated = work / "generated"
    run(BINARY, "generate", FIXTURE / "cells.seam", "--out", generated, "--target", TARGET)
    ffm_seam = work / "cells-ffm.seam"
    ffm_seam.write_text((FIXTURE / "cells.seam").read_text().replace("truffle_cells=true", "truffle_cells=true truffle_backend=ffm"))
    generated_ffm = work / "generated-ffm"
    run(BINARY, "generate", ffm_seam, "--out", generated_ffm, "--target", TARGET)
    run("cc", "-O2", "-fPIC", "-c", FIXTURE / "native.c", "-o", work / "native.o")
    run("ar", "rcs", work / "libcellsfixture.a", work / "native.o")
    library = work / ("libcellsfixture.dylib" if sys.platform == "darwin" else "libcellsfixture.so")
    run("cc", "-dynamiclib" if sys.platform == "darwin" else "-shared", work / "native.o", "-o", library)
    jars = truffle_jars()
    classpath = [TRUFFLE_JAR, *jars.values()]
    classes = work / "classes"
    compile_native(classes, generated, generated / "CellApi.java", generated / "CellApiTruffle.java",
                   FIXTURE / "Directives.java", FIXTURE / "CellChecks.java", FIXTURE / "CellsMain.java", classpath=classpath,
                   options=("--module-path", os.pathsep.join(map(str, jars.values()))))
    classes_ffm = work / "classes-ffm"
    compile_native(classes_ffm, generated_ffm, generated_ffm / "CellApiFFM.java", generated_ffm / "CellApiTruffle.java",
                   FIXTURE / "CellChecks.java", FIXTURE / "FfmCellsMain.java", classpath=classpath)
    return work, classes, classes_ffm, library, jars


class TruffleIntegrationTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.work, cls.classes, cls.classes_ffm, cls.library, cls.jars = build_truffle_fixture()

    def test_ffm_cells_on_hotspot(self):
        java = Path(os.environ["GRAALVM_HOME"]) / "bin/java"
        classpath = os.pathsep.join(map(str, [self.classes_ffm, TRUFFLE_JAR, *self.jars.values()]))
        result = run(java, "--enable-native-access=ALL-UNNAMED", "-cp", classpath, "fixture.cells.FfmCellsMain", self.library)
        self.assertIn("Truffle FFM cells passed", result.stdout)

    def test_native_image_cells_run_from_compiled_code(self):
        app = self.work / "cells-app"
        module_path = os.pathsep.join(str(self.jars[name]) for name in
                                      ("truffle-api", "truffle-runtime", "truffle-compiler", "polyglot", "collections",
                                       "nativeimage", "word", "jniutils", "nativebridge"))
        # Direct stub calls (elide-dev/graal#196) and isolate-compiled code installation (#197) both
        # need the pinned 25.5.5-elide.26 fork; the fixture uses the default direct transitions.
        build = native_image("-O2", "--macro:truffle-svm", "--enable-native-access=ALL-UNNAMED",
                             # The generated properties cover the cells; the probes are the consumer's own node classes.
                             "--initialize-at-build-time=fixture.cells.CellsMain$Probe,fixture.cells.CellsMain$TypedAdd,fixture.cells.CellsMain$Repeat",
                             "-H:+UnlockExperimentalVMOptions", "-H:+PrintRuntimeCompileMethods", "-H:-UnlockExperimentalVMOptions",
                             "--module-path", module_path, "-cp", os.pathsep.join(map(str, [self.classes, TRUFFLE_JAR])),
                             f"-Dmyna.cells.libraryPath={self.work}", "fixture.cells.CellsMain", "-o", app)
        self.assert_cells_are_runtime_compilable(build.stdout)
        result = run(app)
        self.assertIn("Truffle cells compiled and passed", result.stdout)

    def assert_cells_are_runtime_compilable(self, log):
        """Every method on a cell's compiled path must be available to partial evaluation.

        Native Image leaves a method out of runtime compilation, and compiled code then calls it as
        an opaque function, when it needs a class-initialization check: a static call or field read
        of a class that initializes at run time. One such access in a coercion keeps its result (a
        borrow) from being scalar-replaced; one in a cell body keeps the whole cell, argument array
        and boxed carriers included, out of the compiled caller.
        """
        compilable = {line.split("%%")[0] for line in log.splitlines() if "%%R(" in line}
        self.assertGreater(len(compilable), 100, "no runtime-compiled method listing in the build output")
        # The listing discriminates: this method reads a run-time-initialized holder.
        self.assertNotIn("dev.elide.myna.truffle.Borrow.pinThreshold", compilable)
        cells = [line.split()[1] for line in (FIXTURE / "cells.seam").read_text().splitlines() if line.startswith("import ")]
        self.assertGreater(len(cells), 10)
        expected = [f"fixture.cells.CellApiTruffle${name}_Cell.execute" for name in cells]
        expected += [f"dev.elide.myna.truffle.{method}" for method in (
            "BytesCoercion.execute", "BytesCoercion.slow", "IntegerCoercion.execute", "PointerCoercion.execute",
            "BooleanCoercion.execute", "FloatingCoercion.execute", "Borrow.lendArray", "Borrow.lendCopy",
            "Borrow.lendString", "Borrow.lendBuffer", "Borrow.lendPushed", "Borrow.release",
            "NativeCellRootNode.execute")]
        missing = [method for method in expected if method not in compilable]
        self.assertEqual([], missing, "excluded from runtime compilation")


if __name__ == "__main__":
    main(sys.modules[__name__], "truffle-integration")
