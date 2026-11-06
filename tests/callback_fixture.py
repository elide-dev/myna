"""One callback fixture for JVM FFM and SVM FFM/static JNI."""
import os
import shutil
import sys
from pathlib import Path
from support import BINARY, ROOT, TARGET, compile_native, run


def build_callback_fixture():
    work = ROOT / "build/callback-test"
    work.mkdir(parents=True, exist_ok=True)
    fixture = ROOT / "tests/callbacks"
    generated = work / "generated"
    run(BINARY, "generate", fixture / "callbacks.seam", "--jni", "--out", generated, "--target", TARGET)
    obj = work / "native.o"
    run("cc", "-O2", "-fPIC", "-I", generated, "-c", fixture / "native.c", "-o", obj)
    library = work / ("libcallbacks.dylib" if sys.platform == "darwin" else "libcallbacks.so")
    run("cc", "-dynamiclib" if sys.platform == "darwin" else "-shared", obj, "-pthread", "-o", library)
    home = Path(os.environ["GRAALVM_HOME"])
    run("cc", "-O2", "-fPIC", "-I", home / "include", "-I",
        home / "include" / ("darwin" if sys.platform == "darwin" else "linux"),
        "-c", generated / "seam-jni.c", "-o", work / "jni.o")
    run("ar", "rcs", work / "libcallbacks_jni.a", work / "jni.o", obj)
    shutil.copyfile(generated / "libcallbacks_jni.a.symbols", work / "libcallbacks_jni.a.symbols")
    classes = work / "classes"
    compile_native(classes, generated, generated / "CallbackApiFFM.java", generated / "CallbackApiJNI.java",
                   generated / "CallbackApiForeignFeature.java", *sorted(fixture.glob("*.java")))
    return work, classes, library
