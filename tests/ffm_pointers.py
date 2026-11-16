# Copyright the Myna contributors.
# SPDX-License-Identifier: Apache-2.0 OR MIT
"""Shared fixture for typed struct pointers in Java FFM on HotSpot and Native Image."""
import sys
import shutil
from support import BINARY, ROOT, TARGET, compile_native, run


def build_pointers_fixture():
    work = ROOT / "build/ffm-pointers-test"
    if work.exists():
        shutil.rmtree(work)
    work.mkdir(parents=True)
    generated = work / "generated"
    run(BINARY, "generate", ROOT / "tests/pointers/pointers.seam", "--out", generated, "--target", TARGET)
    library = work / ("libpointers.dylib" if sys.platform == "darwin" else "libpointers.so")
    run("cc", "-std=c11", "-Wall", "-Werror", "-fPIC", "-I", generated,
        "-dynamiclib" if sys.platform == "darwin" else "-shared",
        ROOT / "tests/pointers/native.c", "-o", library)
    classes = work / "classes"
    compile_native(classes, generated, generated / "PointersFFM.java",
                   generated / "PointersForeignFeature.java", ROOT / "tests/pointers/PointersMain.java")
    return work, generated, classes, library
