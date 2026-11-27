# Copyright the Myna contributors.
# SPDX-License-Identifier: Apache-2.0 OR MIT
"""Build the SVM executable or collect callback wall-time samples."""
import argparse
import json
import os
from pathlib import Path
import platform
import subprocess
import sys
import uuid

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "tests"))
sys.path.insert(0, str(ROOT / "benchmarks/native"))
from support import run, sha256_file
from upcall_fixture import LIB_NAME, build_upcall_fixture
from walltime import report
from truffle_integration import TRUFFLE_JAR, truffle_jars

SOURCE = Path(__file__).resolve().parent
WORK = ROOT / "build/upcalls"
LIB = WORK / LIB_NAME
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


def build_hashes(jars):
    gen = WORK / "generated"
    return {
        "binaries": {p.name: sha256_file(p) for p in (APP, LIB)},
        "sources": {p.name: sha256_file(p) for p in SOURCE.iterdir() if p.is_file() and p.suffix in (".py", ".java", ".c", ".seam")},
        "fixture_sha256": sha256_file(ROOT / "tests/upcall_fixture.py"),
        "nfi_shim_sha256": sha256_file(ROOT / "benchmarks/native/nfi_memfd.c"),
        "dependencies": {name: sha256_file(path) for name, path in {"myna-truffle": TRUFFLE_JAR, **jars}.items()},
        "generated": {p.name: sha256_file(p) for p in sorted(gen.glob("*.java"))},
    }


def current_build():
    metadata = json.loads((WORK / "build.json").read_text())
    if metadata.get("cases") != CASES + NFI_CASES or any(
        metadata.get(key) != value for key, value in build_hashes(truffle_jars()).items()
    ):
        raise ValueError("Stale upcall inputs or executable. Run mise run bench:upcalls-build.")
    return metadata


def build():
    jars = truffle_jars()
    generator = ["java", "-cp", ROOT / ".dev/jvm/classes/main/java", "dev.elide.myna.cli.Main"]
    app, library = build_upcall_fixture(WORK, generator, jars, benchmark=True)
    run(app, library, "reducer-cell-checks", "1")

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
