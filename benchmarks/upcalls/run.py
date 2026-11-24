# Copyright the Myna contributors.
# SPDX-License-Identifier: Apache-2.0 OR MIT
"""Build the SVM executable or collect callback wall-time samples."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import platform
import shutil
import subprocess
import sys
import uuid

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "tests"))
sys.path.insert(0, str(ROOT / "benchmarks/native"))
from support import TARGET, compile_native, native_image, run
from walltime import report
from truffle_integration import TRUFFLE_JAR, truffle_jars

SOURCE = Path(__file__).resolve().parent
WORK = ROOT / "build/upcalls"
LIB = WORK / ("libbench.dylib" if sys.platform == "darwin" else "libbench.so")
APP = WORK / "measure"
CASES = [f"{kind}-{thread}" for kind in ("control", "static", "dynamic", "bound")
         for thread in ("caller", "worker")]
CASES += [f"{kind}-hook-{thread}" for kind in ("static", "dynamic", "bound")
          for thread in ("caller", "worker")]
CASES += ["isolate-caller", "static-setup", "dynamic-setup", "bound-setup"]
CASES += [f"{target}-{scope}-{thread}" for target in ("guest", "guest-bound")
          for scope in ("each", "batch") for thread in ("caller", "worker")]
CASES += [f"reducer-{kind}-{thread}" for kind in ("bound", "cell") for thread in ("caller", "worker")]
NFI_CASES = ["reducer-nfi-caller", "reducer-nfi-worker"]


def file_hash(path):
    with path.open("rb") as source:
        return hashlib.file_digest(source, "sha256").hexdigest()


def build_hashes(jars):
    gen = WORK / "generated"
    return {
        "binaries": {p.name: file_hash(p) for p in (APP, LIB)},
        "sources": {p.name: file_hash(p) for p in SOURCE.iterdir() if p.suffix in (".java", ".c", ".seam")},
        "nfi_shim_sha256": file_hash(ROOT / "benchmarks/native/nfi_memfd.c"),
        "dependencies": {name: file_hash(path) for name, path in {"myna-truffle": TRUFFLE_JAR, **jars}.items()},
        "generated": {p.name: file_hash(p) for p in sorted(gen.glob("*.java"))},
    }


def current_build():
    metadata = json.loads((WORK / "build.json").read_text())
    if metadata.get("cases") != CASES + NFI_CASES or any(
        metadata.get(key) != value for key, value in build_hashes(truffle_jars()).items()
    ):
        raise ValueError("Stale upcall inputs or executable. Run mise run bench:upcalls-build.")
    return metadata


def build():
    gen = WORK / "generated"
    WORK.mkdir(parents=True, exist_ok=True)
    shutil.rmtree(gen, ignore_errors=True)
    run("java", "-cp", ROOT / ".dev/jvm/classes/main/java", "dev.elide.myna.cli.Main",
        "generate", SOURCE / "upcalls.seam", "--out", gen, "--target", TARGET)
    run("cc", "-O3", "-shared", "-fPIC", "-pthread", "-I", gen, SOURCE / "kernel.c", "-o", LIB)
    nfi_shim = ROOT / "benchmarks/native/nfi_memfd.c"
    run("cc", "-O3", "-fPIC", "-c", nfi_shim, "-o", WORK / "nfi.o")
    classes = WORK / "classes"
    shutil.rmtree(classes, ignore_errors=True)
    jars = truffle_jars()
    modules = ("truffle-api", "truffle-runtime", "truffle-compiler", "polyglot", "collections", "nativeimage", "word", "jniutils", "nativebridge", "truffle-nfi", "truffle-nfi-libffi")
    compile_native(classes, gen, *sorted(gen.glob("*.java")), *sorted(SOURCE.glob("*.java")), classpath=[TRUFFLE_JAR, *jars.values()],
                   options=("-processorpath", str(jars["truffle-dsl-processor"])))
    # The DSL processor emits the interop library exports and their call-site cache classes.
    nfi_classes = sorted(p.stem for p in (classes / "bench/upcalls").glob("NfiExecutableGen*.class"))
    init = ["Guest$CountingRoot", "Guest$MixNode", "HookLanguage", "HookLanguageProvider",
            "ReducerBench$ReducerRoot", "ReducerBench$NfiExecutable", "ReducerBench$NfiExecutable$Execute",
            "ReducerCellChecks$CheckedRoot",
            *nfi_classes]
    native_image("-O3", "--macro:truffle-svm", "--module-path", os.pathsep.join(str(jars[m]) for m in modules),
                 "--initialize-at-build-time=" + ",".join("bench.upcalls." + name for name in init), "-H:+UnlockExperimentalVMOptions", "-H:-DeleteLocalSymbols",
                 f'-H:NativeLinkerOption={WORK / "nfi.o"}',
                 "-H:-UnlockExperimentalVMOptions", "--enable-native-access=ALL-UNNAMED", "-cp", os.pathsep.join(map(str, (classes, TRUFFLE_JAR))),
                 "--features=bench.upcalls.UpcallsForeignFeature", "bench.upcalls.Measure",
                 "-o", APP, timeout=900)

    run(APP, LIB, "reducer-cell-checks", "1")

    metadata = {
        "revision": run("git", "rev-parse", "HEAD").stdout.strip(),
        "dirty": run("git", "status", "--porcelain").stdout.strip(),
        "machine": platform.platform(),
        "build_flags": {"native_image": "-O3 --macro:truffle-svm -H:-DeleteLocalSymbols", "kernel": "-O3 -shared -fPIC -pthread"},
        "compiler": run("cc", "--version").stdout.strip(),
        "graalvm": run(Path(os.environ["GRAALVM_HOME"]) / "bin/native-image", "--version").stdout.strip(),
        **build_hashes(jars),
        "cases": CASES + NFI_CASES,
    }
    (WORK / "build.json").write_text(json.dumps(metadata, indent=2))


def measure(forks, include_nfi):
    if include_nfi and os.getenv("CODSPEED_PROFILE_FOLDER"):
        raise ValueError("Run cross-library comparisons outside CodSpeed")
    cases = CASES + NFI_CASES if include_nfi else CASES
    output = WORK / "results"
    output.mkdir(exist_ok=True)
    metadata = current_build()
    if sys.platform == "darwin":
        metadata["cpu"] = run("sysctl", "-n", "machdep.cpu.brand_string").stdout.strip()
    else:
        metadata["cpu"] = run("lscpu").stdout.strip()
    if hasattr(os, "sched_getaffinity"):
        metadata["cpu_affinity"] = sorted(os.sched_getaffinity(0))
    metadata.update(cases=cases, forks=forks, campaign=uuid.uuid4().hex)
    (output / "metadata.json").unlink(missing_ok=True)
    for fork in range(forks):
        order = cases if fork % 2 == 0 else reversed(cases)
        for case in order:
            count = 262144 if case.startswith(("guest-", "reducer-")) else 256 if case.endswith("setup") else 16384
            proc = subprocess.Popen([APP, LIB, case, str(count)], stdout=subprocess.PIPE, text=True)
            stdout, _ = proc.communicate()
            samples = list(map(int, stdout.splitlines()))
            if proc.returncode or len(samples) != 12 or min(samples) <= 0:
                raise RuntimeError(f"invalid measurement for {case}")
            raw = dict(case=case, campaign=metadata["campaign"], iterations=count, warmup_iters=count * 3,
                       samples_ns=samples, pid=proc.pid)
            (output / f"{case}-{fork}.json").write_text(json.dumps(raw, indent=2))
            if folder := os.getenv("CODSPEED_PROFILE_FOLDER"):
                payload = report(raw)
                payload["benchmarks"][0]["uri"] = "benchmarks/upcalls/Measure.java::" + case
                results = Path(folder) / "results"
                results.mkdir(parents=True, exist_ok=True)
                (results / f"{proc.pid}.json").write_text(json.dumps(payload))
    (output / "metadata.json").write_text(json.dumps(metadata, indent=2))


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("command", choices=("build", "run"))
    parser.add_argument("--forks", type=int, default=1)
    parser.add_argument("--include-nfi", action="store_true", help="include the uninstrumented NFI closure comparison")
    args = parser.parse_args()
    if args.forks < 1:
        parser.error("--forks must be positive")
    if args.command == "build":
        build()
    else:
        measure(args.forks, args.include_nfi)
