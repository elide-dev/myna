# Copyright the Myna contributors.
# SPDX-License-Identifier: Apache-2.0 OR MIT
"""Build the signed i64 upcall seam and C worker for native integration or measurement."""
import os
import shutil
import sys
from support import ROOT, TARGET, compile_native, native_image, run
from truffle_integration import TRUFFLE_JAR

LIB_NAME = "libbench.dylib" if sys.platform == "darwin" else "libbench.so"


def build_upcall_fixture(work, generator, jars, *, benchmark):
    source = ROOT / "benchmarks/upcalls"
    gen = work / "generated"
    work.mkdir(parents=True, exist_ok=True)
    shutil.rmtree(gen, ignore_errors=True)
    run(*generator, "generate", source / "upcalls.seam", "--out", gen, "--target", TARGET)
    library = work / LIB_NAME
    run("cc", "-O3", "-shared", "-fPIC", "-pthread", "-I", gen, source / "kernel.c", "-o", library)
    classes = work / "classes"
    shutil.rmtree(classes, ignore_errors=True)
    sources = sorted(source.glob("*.java"))
    if not benchmark:
        sources.append(ROOT / "tests/truffle/UpcallCellMain.java")
    compile_native(classes, gen, *sorted(gen.glob("*.java")), *sources, classpath=[TRUFFLE_JAR, *jars.values()],
                   options=("-processorpath", str(jars["truffle-dsl-processor"])))
    modules = ["truffle-api", "truffle-runtime", "truffle-compiler", "polyglot", "collections", "nativeimage",
               "word", "jniutils", "nativebridge"]
    init = ["Guest$CountingRoot", "Guest$MixNode", "HookLanguage", "HookLanguageProvider",
            "ReducerBench$ReducerRoot", "ReducerCellChecks$CheckedRoot"]
    options = []
    if benchmark:
        modules += ["truffle-nfi", "truffle-nfi-libffi"]
        run("cc", "-O3", "-fPIC", "-c", ROOT / "benchmarks/native/nfi_memfd.c", "-o", work / "nfi.o")
        options = ["-H:+UnlockExperimentalVMOptions", "-H:-DeleteLocalSymbols",
                   f'-H:NativeLinkerOption={work / "nfi.o"}', "-H:-UnlockExperimentalVMOptions"]
        init += ["ReducerBench$NfiExecutable", "ReducerBench$NfiExecutable$Execute"]
        init += sorted(p.stem for p in (classes / "bench/upcalls").glob("NfiExecutableGen*.class"))
    app = work / ("measure" if benchmark else "check-upcall-cell")
    main = "bench.upcalls.Measure" if benchmark else "bench.upcalls.UpcallCellMain"
    native_image("-O3" if benchmark else "-O1", "--macro:truffle-svm", "--module-path",
                 os.pathsep.join(str(jars[m]) for m in modules),
                 "--initialize-at-build-time=" + ",".join("bench.upcalls." + name for name in init),
                 *options, "--enable-native-access=ALL-UNNAMED", "-cp", os.pathsep.join(map(str, [classes, TRUFFLE_JAR])),
                 "--features=bench.upcalls.UpcallsForeignFeature", main, "-o", app, timeout=900)
    return app, library
