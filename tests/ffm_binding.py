"""Shared fixture for JVM and SVM library lifetime conformance."""
import sys
from support import BINARY, ROOT, TARGET, compile_native, run


def build_binding_fixture():
    work = ROOT / "build/ffm-binding-test"
    work.mkdir(parents=True, exist_ok=True)
    generated = work / "generated"
    run(BINARY, "generate", ROOT / "examples/ownership.seam", "--out", generated, "--target", TARGET)
    obj = work / "ownership.o"
    run("rustc", "--edition=2024", "--crate-type=lib", "--emit=obj", "-Cpanic=abort",
        "-Copt-level=2", "-Crelocation-model=pic", ROOT / "tests/ownership/native.rs", "-o", obj)
    library = work / ("libownership.dylib" if sys.platform == "darwin" else "libownership.so")
    run("cc", "-dynamiclib" if sys.platform == "darwin" else "-shared", obj, "-o", library)
    classes = work / "classes"
    compile_native(classes, generated, generated / "NativeApiFFM.java",
                   generated / "NativeApiForeignFeature.java", ROOT / "tests/ownership/FfmBindingMain.java")
    return work, classes, library
