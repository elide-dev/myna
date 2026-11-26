# Copyright the Myna contributors.
# SPDX-License-Identifier: Apache-2.0 OR MIT
"""Repeat the uninstrumented Linux x86-64 stock GraalVM versus Elide scalar upcall comparison."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import platform
import random
import shutil
import statistics
import subprocess
import tempfile

from run import current_build

SOURCE = Path(__file__).resolve().parent
ROOT = SOURCE.parents[1]
FORK = ROOT / "build/upcalls"
BASELINE = SOURCE / "baselines/stock-25i5-scalar-upcalls-linux-x86_64.json"
KINDS = ("bound", "cell", "nfi")


def sha(path):
    with path.open("rb") as stream:
        return hashlib.file_digest(stream, "sha256").hexdigest()


def download(spec, dest):
    if not dest.exists():
        partial = dest.with_name(dest.name + ".partial")
        subprocess.run(["curl", "--fail", "--location", "--retry", "3", "--output", partial, spec["url"]], check=True)
        if sha(partial) != spec["sha256"]:
            raise ValueError(f"Checksum mismatch for {partial}")
        partial.replace(dest)
    if sha(dest) != spec["sha256"]:
        raise ValueError(f"Checksum mismatch for {dest}")


def verify_inputs(baseline):
    metadata = current_build()
    # This stock release recipe reproduces a specific set of guest, adapter and generated inputs.
    for name, digest in baseline["source_sha256"].items():
        if sha(ROOT / name) != digest:
            raise ValueError(f"{name} differs from the stock comparison at {baseline['revision']}")
    return metadata


def build(directory, baseline):
    jdk = directory / "jdk"
    jars = directory / "jars"
    jars.mkdir(exist_ok=True)
    archive = directory / "stock-jdk.tar.gz"
    download(baseline["stock"]["runtime_archive"], archive)
    if not (jdk / "bin/native-image").is_file():
        with tempfile.TemporaryDirectory(dir=directory) as temporary:
            subprocess.run(["tar", "-xzf", archive, "--strip-components=1", "-C", temporary], check=True)
            Path(temporary).replace(jdk)
    for name, spec in baseline["stock"]["jars"].items():
        download(spec, jars / f"{name}.jar")
    clang = (ROOT / ".dev/toolchains/native/bin/clang").resolve(strict=True)
    with tempfile.TemporaryDirectory(dir=directory) as temporary:
        classes = Path(temporary)
        generated = FORK / "generated"
        shutil.copytree(generated / "META-INF", classes / "META-INF")
        shutil.copytree(ROOT / "src/truffle/resources", classes, dirs_exist_ok=True)
        sources = [*sorted((ROOT / "src/truffle/java").rglob("*.java")), *sorted(SOURCE.glob("*.java")),
                   *sorted(generated.glob("*.java"))]
        sdk = [jdk / "lib/svm" / name for name in ("svm-shared.jar", "svm-guest-staging.jar", "svm-guest.jar")]
        classpath = os.pathsep.join(map(str, [*sdk, *sorted(jars.glob("*.jar"))]))
        javac = [jdk / "bin/javac", "-processorpath", jars / "truffle-dsl-processor.jar", "-cp", classpath,
                 "-d", classes, *sources]
        paths = dict(stock_jdk=jdk, stock_jars=jars, classes=classes, work=FORK, comparison=directory, clang=clang)
        image = [arg.format(**paths) for arg in baseline["stock"]["native_image_argv"]]
        for cmd in (javac, image):
            subprocess.run(cmd, cwd=directory, check=True)
        return {"javac": list(map(str, javac)), "native_image": image}


def measure(directory, baseline, cpu, blocks, build_commands):
    # Hold all six positions equally often in each six-block epoch.
    rng = random.Random(25153201)
    count = baseline["workload"]["events_per_round"]
    expected = baseline["workload"]["verified_each_measured_round"]["guest_and_native_result"]
    library = FORK / "libbench.so"
    apps = {"stock": directory / "measure", "elide": FORK / "measure"}
    cases = [(runtime, kind) for runtime in apps for kind in KINDS]
    os.sched_setaffinity(0, {cpu})
    metadata = verify_inputs(baseline)
    results = {"machine": platform.platform(), "lscpu": subprocess.check_output(["lscpu"], text=True), "cpu": cpu,
               "source_sha256": {p: sha(ROOT / p) for p in baseline["source_sha256"]},
               "kernel_sha256": sha(library), "image_sha256": {r: sha(app) for r, app in apps.items()},
               "elide_build": metadata, "stock_build": build_commands, "blocks": []}
    for epoch in range(blocks // 6):
        keys = cases.copy()
        rng.shuffle(keys)
        offsets = list(range(6))
        rng.shuffle(offsets)
        for offset in offsets:
            block = {}
            for index in (0, 1, 5, 2, 4, 3):
                runtime, kind = keys[(index + offset) % 6]
                case = f"reducer-{kind}-worker"
                cmd = [apps[runtime], library, case, str(count)]
                proc = subprocess.run(cmd, capture_output=True, text=True, check=True, timeout=120)
                samples = [int(s) for s in proc.stdout.splitlines()]
                verdict = f"verified {case} calls={count} compiled={count} checksum={expected} timed_rounds=12"
                if len(samples) != 12 or min(samples) <= 0 or verdict not in proc.stderr.splitlines():
                    raise RuntimeError(f"Unexpected sample from {case}: {proc.stderr}")
                block[f"{runtime}-{kind}"] = samples
            results["blocks"].append(block)
    results["median_ns_per_callback"] = {
        key: statistics.median(statistics.median(b[key]) / count for b in results["blocks"])
        for key in results["blocks"][0]}
    output = directory / "paired.json"
    output.write_text(json.dumps(results, indent=2) + "\n")
    print(json.dumps(results["median_ns_per_callback"], indent=2), output)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--out", type=Path, default=ROOT / "build/upcalls-stock")
    parser.add_argument("--cpu", type=int, required=True)
    parser.add_argument("--blocks", type=int, default=12)
    args = parser.parse_args()
    if (platform.system(), platform.machine()) != ("Linux", "x86_64"):
        parser.error("the pinned upstream distribution targets Linux x86-64")
    if args.blocks < 6 or args.blocks % 6:
        parser.error("the six-treatment rotation needs a positive multiple of six blocks")
    if os.getenv("CODSPEED_PROFILE_FOLDER"):
        parser.error("use an uninstrumented process for cross-library comparisons")
    if not (FORK / "build.json").is_file():
        parser.error("run mise run bench:upcalls-build to build the .32 image and native kernel first")
    out = args.out.resolve()
    out.mkdir(parents=True, exist_ok=True)
    baseline = json.loads(BASELINE.read_text())
    verify_inputs(baseline)
    measure(out, baseline, args.cpu, args.blocks, build(out, baseline))
