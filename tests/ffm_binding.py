# Copyright the Myna contributors.
# SPDX-License-Identifier: Apache-2.0 OR MIT
"""Shared fixture for JVM and SVM library lifetime conformance."""
import sys
import shutil
from support import BINARY, ROOT, TARGET, compile_native, run


def build_binding_fixture():
    work = ROOT / "build/ffm-binding-test"
    work.mkdir(parents=True, exist_ok=True)
    generated = work / "generated"
    run(BINARY, "generate", ROOT / "examples/ownership.seam", "--out", generated, "--target", TARGET)
    confined_source = work / "confined.seam"
    confined_source.write_text((ROOT / "examples/ownership.seam").read_text()
        .replace("threading=shared", "threading=confined")
        .replace("java_class=NativeApi", "java_class=ConfinedApi"))
    confined = work / "confined"
    run(BINARY, "generate", confined_source, "--out", confined, "--target", TARGET)
    obj = work / "ownership.o"
    run("rustc", "--edition=2024", "--crate-type=lib", "--emit=obj", "-Cpanic=abort",
        "-Copt-level=2", "-Crelocation-model=pic", ROOT / "tests/ownership/native.rs", "-o", obj)
    library = work / ("libownership.dylib" if sys.platform == "darwin" else "libownership.so")
    run("cc", "-dynamiclib" if sys.platform == "darwin" else "-shared", obj, "-o", library)
    classes = work / "classes"
    compile_native(classes, generated, generated / "NativeApiFFM.java",
                   generated / "NativeApiForeignFeature.java", confined / "ConfinedApiFFM.java",
                   confined / "ConfinedApiForeignFeature.java", ROOT / "tests/ownership/ConfinedOwner.java",
                   ROOT / "tests/ownership/FfmBindingMain.java")
    shutil.copytree(confined / "META-INF", classes / "META-INF", dirs_exist_ok=True)
    return work, classes, library
