# Copyright the Myna contributors.
# SPDX-License-Identifier: Apache-2.0 OR MIT
"""Build the SVM executable or collect callback wall-time samples."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import platform
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "tests"))
sys.path.insert(0, str(ROOT / "benchmarks/native"))
from support import TARGET, compile_native, native_image, run
from walltime import report
from truffle_integration import truffle_jars

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


def source_hashes():
    return {p.name: hashlib.sha256(p.read_bytes()).hexdigest() for p in SOURCE.iterdir()
            if p.suffix in (".java", ".c", ".seam")}


def build():
    gen = WORK / "generated"
    WORK.mkdir(parents=True, exist_ok=True)
    run("java", "-cp", ROOT / ".dev/jvm/classes/main/java", "dev.elide.myna.cli.Main",
        "generate", SOURCE / "upcalls.seam", "--out", gen, "--target", TARGET)
    run("cc", "-O3", "-shared", "-fPIC", "-pthread", "-I", gen, SOURCE / "kernel.c", "-o", LIB)
    classes = WORK / "classes"
    jars = truffle_jars()
    modules = ("truffle-api", "truffle-runtime", "truffle-compiler", "polyglot", "collections", "nativeimage", "word", "jniutils", "nativebridge", "truffle-nfi", "truffle-nfi-libffi")
    compile_native(classes, gen, *sorted(gen.glob("*.java")), *sorted(SOURCE.glob("*.java")), classpath=jars.values(),
                   options=("-processorpath", str(jars["truffle-dsl-processor"])))
    native_image("-O3", "--macro:truffle-svm", "--module-path", os.pathsep.join(str(jars[m]) for m in modules),
                 "--initialize-at-build-time=bench.upcalls.Guest$CountingRoot,bench.upcalls.Guest$MixNode,bench.upcalls.HookLanguage,bench.upcalls.HookLanguageProvider", "-H:+UnlockExperimentalVMOptions", "-H:-DeleteLocalSymbols",
                 "-H:-UnlockExperimentalVMOptions", "--enable-native-access=ALL-UNNAMED", "-cp", classes,
                 "--features=bench.upcalls.UpcallsForeignFeature", "bench.upcalls.Measure",
                 "-o", APP, timeout=900)

    metadata = {
        "revision": run("git", "rev-parse", "HEAD").stdout.strip(),
        "machine": platform.platform(),
        "build_flags": {"native_image": "-O3 --macro:truffle-svm -H:-DeleteLocalSymbols", "kernel": "-O3 -shared -fPIC -pthread"},
        "compiler": run("cc", "--version").stdout.strip(),
        "graalvm": run(Path(os.environ["GRAALVM_HOME"]) / "bin/native-image", "--version").stdout.strip(),
        "binaries": {p.name: hashlib.sha256(p.read_bytes()).hexdigest() for p in (APP, LIB)},
        "sources": source_hashes(),
        "cases": CASES,
    }
    (WORK / "build.json").write_text(json.dumps(metadata, indent=2))


def measure(forks):
    output = WORK / "results"
    output.mkdir(exist_ok=True)
    metadata = json.loads((WORK / "build.json").read_text())
    if metadata.get("cases") != CASES or metadata["sources"] != source_hashes():
        raise RuntimeError("upcall workloads have changed; rebuild the Native Image before measuring")
    if sys.platform == "darwin":
        metadata["cpu"] = run("sysctl", "-n", "machdep.cpu.brand_string").stdout.strip()
    else:
        metadata["cpu"] = run("lscpu").stdout.strip()
    if hasattr(os, "sched_getaffinity"):
        metadata["cpu_affinity"] = sorted(os.sched_getaffinity(0))
    (output / "metadata.json").write_text(json.dumps(metadata, indent=2))
    for fork in range(forks):
        for case in CASES:
            count = 262144 if case.startswith("guest-") else 256 if case.endswith("setup") else 16384
            proc = subprocess.Popen([APP, LIB, case, str(count)], stdout=subprocess.PIPE, text=True)
            stdout, _ = proc.communicate()
            samples = list(map(int, stdout.splitlines()))
            if proc.returncode or len(samples) != 12 or min(samples) <= 0:
                raise RuntimeError(f"invalid measurement for {case}")
            raw = dict(case=case, iterations=count, warmup_iters=count * 3,
                       samples_ns=samples, pid=proc.pid)
            (output / f"{case}-{fork}.json").write_text(json.dumps(raw, indent=2))
            if folder := os.getenv("CODSPEED_PROFILE_FOLDER"):
                payload = report(raw)
                payload["benchmarks"][0]["uri"] = "benchmarks/upcalls/Measure.java::" + case
                results = Path(folder) / "results"
                results.mkdir(parents=True, exist_ok=True)
                (results / f"{proc.pid}.json").write_text(json.dumps(payload))


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("command", choices=("build", "run"))
    parser.add_argument("--forks", type=int, default=1)
    args = parser.parse_args()
    if args.forks < 1:
        parser.error("--forks must be positive")
    if args.command == "build":
        build()
    else:
        measure(args.forks)
