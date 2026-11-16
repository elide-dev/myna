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
    compile_native(classes, generated, generated / "CellApi.java", generated / "CellApiTruffle.java", generated / "CellApiOwnership.java",
                   generated / "CellApiDirectives.java", FIXTURE / "Directives.java", FIXTURE / "CellChecks.java", FIXTURE / "CellsMain.java", classpath=classpath,
                   options=("--module-path", os.pathsep.join(map(str, jars.values()))))
    classes_ffm = work / "classes-ffm"
    compile_native(classes_ffm, generated_ffm, generated_ffm / "CellApiFFM.java", generated_ffm / "CellApiTruffle.java",
                   generated_ffm / "CellApiForeignFeature.java", FIXTURE / "CellChecks.java", FIXTURE / "FfmCellsMain.java",
                   classpath=classpath)
    # Truffle cells over FFM need no C context: nothing compiles CellApiDirectives.
    assert not list(classes_ffm.rglob("*Directives.class")), list(classes_ffm.rglob("*Directives.class"))
    return work, classes, classes_ffm, library, jars


class TruffleIntegrationTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.work, cls.classes, cls.classes_ffm, cls.library, cls.jars = build_truffle_fixture()

    def test_ffm_cells_on_hotspot(self):
        java = Path(os.environ["GRAALVM_HOME"]) / "bin/java"
        classpath = os.pathsep.join(map(str, [self.classes_ffm, TRUFFLE_JAR, *self.jars.values()]))
        # Assertions on for the generated bindings only (Truffle's own interop assertions reject the
        # host nulls the checks pass): a non-cell wrapper asserts the caller's align contract.
        result = run(java, "-ea:fixture.cells...", "--enable-native-access=ALL-UNNAMED", "-cp", classpath, "fixture.cells.FfmCellsMain", self.library)
        self.assertIn("wrapper assertions enabled: fired", result.stdout)
        self.assertIn("guest constants read and named", result.stdout)
        self.assertIn("Truffle FFM cells passed", result.stdout)
        # Write-back of a copied buffer after its arena closed: refused, nothing written.
        self.assertRegex(result.stdout, r"copy-back after close refused: confined, (shared|shared arenas unsupported)")

    def test_ffm_cells_in_native_image(self):
        """FFM cells in an image: the same checks, including the scopes of lent segments, and what
        each pattern of lent segments allocates from runtime-compiled code."""
        app = self.work / "ffm-cells-app"
        # -O3, as the cell benchmark builds: at -O2 the host inliner does not inline the static FFM
        # entry into a cell's boundary method, so even invoke allocates the address's segment.
        build = native_image("-O3", "--macro:truffle-svm", "--enable-native-access=ALL-UNNAMED",
                             # The probe is the consumer's own node class.
                             "--initialize-at-build-time=fixture.cells.FfmCellsMain$Repeat,fixture.cells.CellChecks$GuestConstants",
                             "--module-path", self.module_path(), "-cp", os.pathsep.join(map(str, [self.classes_ffm, TRUFFLE_JAR])),
                             "-H:+PrintAnalysisCallTree", f"-H:Path={self.work}",
                             "--features=fixture.cells.CellApiForeignFeature", "fixture.cells.FfmCellsMain", "-o", app)
        self.assertIn("ffm-cells-app", build.stdout)
        self.assert_wrapper_assertions_compiled_out(self.work / "reports")
        result = run(app, self.library)
        self.assertRegex(result.stdout, r"guest constants folded and allocated \d+ bytes over 4096 compiled iterations")
        self.assertIn("wrapper assertions disabled: silent", result.stdout)
        self.assertIn("Truffle FFM cells passed", result.stdout)
        # Write-back of a copied buffer after its arena closed: refused, nothing written.
        self.assertRegex(result.stdout, r"copy-back after close refused: confined, (shared|shared arenas unsupported)")

    def assert_wrapper_assertions_compiled_out(self, reports):
        """An image built without assertions has no assertion code in a non-cell wrapper.

        The call tree lists what each method reaches; the wrapper that asserts a borrow's align
        contract reaches `AssertionError` only if the `assert` statement survived into the image.
        The report is kept in the build directory as evidence."""
        trees = sorted(reports.glob("call_tree_ffm-cells-app_*.txt"))
        self.assertTrue(trees, f"no call tree report in {reports}")
        lines = trees[-1].read_text().splitlines()
        wrapper = "CellApiFFM$LibraryBinding.addressOfBorrow("
        starts = [i for i, line in enumerate(lines) if wrapper in line]
        self.assertTrue(starts, "the asserting wrapper is not in the image's call tree")
        for start in starts:
            depth = len(lines[start]) - len(lines[start].lstrip())
            for line in lines[start + 1:]:
                if len(line) - len(line.lstrip()) <= depth:
                    break
                self.assertNotIn("AssertionError", line, "assertion code reachable from the wrapper")
        evidence = self.work / "wrapper-assertions-call-tree.txt"
        evidence.write_text("\n".join(lines[starts[0]:starts[0] + 60]) + "\n")

    def module_path(self):
        return os.pathsep.join(str(self.jars[name]) for name in
                               ("truffle-api", "truffle-runtime", "truffle-compiler", "polyglot", "collections",
                                "nativeimage", "word", "jniutils", "nativebridge"))

    def test_native_image_cells_run_from_compiled_code(self):
        app = self.work / "cells-app"
        module_path = self.module_path()
        # Direct stub calls (elide-dev/graal#196) and isolate-compiled code installation (#197) both
        # need the pinned 25.5.5-elide.26 fork; the fixture uses the default direct transitions.
        build = native_image("-O2", "--macro:truffle-svm", "--enable-native-access=ALL-UNNAMED",
                             # The generated properties cover the cells; the probes are the consumer's own node classes.
                             "--initialize-at-build-time=fixture.cells.CellsMain$Probe,fixture.cells.CellsMain$TypedAdd,fixture.cells.CellsMain$Repeat,fixture.cells.CellsMain$Creates,fixture.cells.CellChecks$GuestConstants",
                             "-H:+UnlockExperimentalVMOptions", "-H:+PrintRuntimeCompileMethods", "-H:-UnlockExperimentalVMOptions",
                             "--module-path", module_path, "-cp", os.pathsep.join(map(str, [self.classes, TRUFFLE_JAR])),
                             f"-Dmyna.cells.libraryPath={self.work}",
                             "fixture.cells.CellsMain", "-o", app)
        self.assert_cells_are_runtime_compilable(build.stdout)
        result = run(app)
        # Reads of constants.Status.ok compared with a cell result: compiled, folded to a compilation
        # constant, no deoptimization and no allocation; lowByte's u8 255 equals constants.Octet.top
        # (CellChecks.guestConstants throws otherwise).
        self.assertRegex(result.stdout, r"guest constants folded and allocated \d+ bytes over 4096 compiled iterations")
        self.assertIn("Truffle cells compiled and passed", result.stdout)
        # Write-back of a copied buffer after its arena closed: refused, nothing written.
        self.assertRegex(result.stdout, r"copy-back after close refused: confined, (shared|shared arenas unsupported)")

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
            "Borrow.lendString", "Borrow.lendBuffer", "Borrow.lendPushed", "Borrow.release", "GuestText.push",
            "BytesCoercion.pushGuestText", "BytesCoercion.learn", "BytesCoercion.learnPinThreshold",
            "BytesCoercion.emptyAddress", "AlignedBytesCoercion.execute", "AlignedBytesCoercion.slow",
            "OverAlignedBytesCoercion.execute", "OverAlignedBytesCoercion.slow",
            "NativeCellRootNode.execute", "HeldBytesCoercion.execute", "Borrow.lendHeld", "Borrow.lendHeldText",
            # Guest-visible constants: the exported cached read (the specialization body is trivially
            # inlined into it at analysis); names are a switch over constants.
            "NativeConstantsGen$InteropLibraryExports$Cached.readMember")]
        expected.append("fixture.cells.CellApiTruffle$StatusNames.name")
        missing = [method for method in expected if method not in compilable]
        self.assertEqual([], missing, "excluded from runtime compilation")


if __name__ == "__main__":
    main(sys.modules[__name__], "truffle-integration")
