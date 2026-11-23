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

SOURCE = Path(__file__).resolve().parent
WORK = ROOT / "build/upcalls"
LIB = WORK / ("libbench.dylib" if sys.platform == "darwin" else "libbench.so")
APP = WORK / "measure"
CASES = [f"{kind}-{thread}" for kind in ("control", "static", "dynamic")
         for thread in ("caller", "worker")] + ["isolate-caller", "static-setup", "dynamic-setup"]


def build():
    gen = WORK / "generated"
    WORK.mkdir(parents=True, exist_ok=True)
    run("java", "-cp", ROOT / ".dev/jvm/classes/main/java", "dev.elide.myna.cli.Main",
        "generate", SOURCE / "upcalls.seam", "--out", gen, "--target", TARGET)
    run("cc", "-O3", "-shared", "-fPIC", "-pthread", "-I", gen, SOURCE / "kernel.c", "-o", LIB)
    classes = WORK / "classes"
    compile_native(classes, gen, *sorted(gen.glob("*.java")), SOURCE / "Measure.java")
    native_image("-O3", "-H:+UnlockExperimentalVMOptions", "-H:-DeleteLocalSymbols",
                 "-H:-UnlockExperimentalVMOptions", "--enable-native-access=ALL-UNNAMED", "-cp", classes,
                 "--features=bench.upcalls.UpcallsForeignFeature", "bench.upcalls.Measure",
                 "-o", APP, timeout=900)


def measure(forks):
    output = WORK / "results"
    output.mkdir(exist_ok=True)
    metadata = {
        "revision": run("git", "rev-parse", "HEAD").stdout.strip(),
        "machine": platform.platform(),
        "cpu": platform.processor(),
        "compiler": run("cc", "--version").stdout.strip(),
        "graalvm": run(Path(os.environ["GRAALVM_HOME"]) / "bin/native-image", "--version").stdout.strip(),
        "binaries": {p.name: hashlib.sha256(p.read_bytes()).hexdigest() for p in (APP, LIB)},
        "sources": {p.name: hashlib.sha256(p.read_bytes()).hexdigest() for p in SOURCE.iterdir()
                    if p.is_file()},
    }
    (output / "metadata.json").write_text(json.dumps(metadata, indent=2))
    for fork in range(forks):
        for case in CASES:
            count = 256 if case.endswith("setup") else 16384
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
