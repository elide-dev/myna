# Copyright the Myna contributors.
# SPDX-License-Identifier: Apache-2.0 OR MIT
"""Build and compare Myna span callbacks with TruffleData's published native tape traversal."""
import argparse
import json
import os
from pathlib import Path
import platform
import random
import shutil
import statistics
import subprocess
import sys
import tempfile

SOURCE = Path(__file__).resolve().parent
ROOT = SOURCE.parents[1]
sys.path.insert(0, str(ROOT / "tests"))
from support import TARGET, compile_native, native_image, run, sha256_file
from truffle_integration import TRUFFLE_JAR, truffle_jars

WORK = ROOT / "build/upcall-records"
DEPS = WORK / "deps"
APP = WORK / "measure-records"
SUFFIX = ".dylib" if sys.platform == "darwin" else ".so"
RANGES = WORK / ("libranges" + SUFFIX)
VERSION = "0.14.0"
SHA = {
    "core": "0d38f26a6ef1808f00b33b90993d514855bfca5425bf4ba4ce314a98a89dba80",
    "tape": "092b771f8e5897b308d21f863a5c26be2ac95145909ab4a5d9db42de16ec5f04",
    "json-bindings": "c50f640782d9657384e537c53d03c29dd46d7ba1cecb133556a6275167afa822",
    "json": "03841816cd0b2fa654fb7f83a2ba9fedb4532479bedec389bcec2994faf8c9dc",
}
NATIVES = {
    ("Linux", "x86_64"): ("linux-x86_64", "3477403004849b81ec47be0de816b8c5b1e107d3f9f9744faef55e132bbce878",
                          "01e8e56e6d0a01d0428289db4cbc4f34f516ad72c0ae0d4301e4e5d6992a326e"),
    ("Linux", "aarch64"): ("linux-aarch64", "43a775e209cb27b13826e56b6d22392ee79810bd9d5e9d3799f373aa4ff22d7b",
                          "bf566f05265e54ef6795cbc32cbbccdec263aba70d96cae5ddf2183a6146ef37"),
    ("Darwin", "arm64"): ("macos-aarch64", "db1b7dfebd5850f51728ff78c1eae025b9f3d7b4017e5c953893a974c215a73d",
                          "8d4069b85a2f8a441d9416fb69090fd9a28c0c654c0a551d07439ec22b5d2d71"),
}
MODES = ("whole", "pull", "bound", "push")


def artifact(module, extension, checksum):
    name = "truffledata-" + module
    filename = name + "-" + VERSION + extension
    path = DEPS / filename
    if path.is_file() and sha256_file(path) == checksum:
        return path
    with tempfile.TemporaryDirectory(dir=DEPS) as staging:
        candidate = Path(staging) / filename
        run("curl", "-fLsS", "--retry", "3", f"https://maven.elide.dev/dev/elide/{name}/{VERSION}/{filename}",
            "-o", candidate)
        if not candidate.is_file() or sha256_file(candidate) != checksum:
            raise RuntimeError("TruffleData checksum mismatch for " + filename)
        candidate.replace(path)
    return path


def fixture_hashes():
    return {p.name: sha256_file(p) for p in sorted(SOURCE.iterdir()) if p.suffix in (".java", ".seam", ".c", ".py")}


def build():
    DEPS.mkdir(parents=True, exist_ok=True)
    host = (platform.system(), platform.machine())
    if host not in NATIVES:
        raise RuntimeError(f"TruffleData 0.14.0 has no published JSON binary for {host}")
    classifier, sha, archive_sha = NATIVES[host]
    downstream = [artifact(n, ".jar", h) for n, h in SHA.items()]
    parser = artifact("json", "-" + classifier + SUFFIX, sha)
    # The public shared parser exports its JSON API. Its tape UTF-8 helper is in the archive.
    archive = artifact("json", "-" + classifier + ".a", archive_sha)
    shutil.copyfile(parser, DEPS / ("libtruffledata_json" + SUFFIX))
    jars = truffle_jars()
    generation = WORK / "generated"
    classes = WORK / "classes"
    for d in (generation, classes):
        if d.exists():
            shutil.rmtree(d)
    run("java", "-cp", ROOT / ".dev/jvm/classes/main/java", "dev.elide.myna.cli.Main",
        "generate", SOURCE / "records.seam", "--out", generation, "--target", TARGET)
    run("cc", "-O3", "-shared", "-fPIC", "-I", generation, SOURCE / "ranges.c", "-o", RANGES)
    compile_native(classes, generation, *sorted(generation.glob("*.java")), *sorted(SOURCE.glob("*.java")),
                   classpath=[TRUFFLE_JAR, *downstream, *jars.values()],
                   options=("-processorpath", str(jars["truffle-dsl-processor"])))
    modules = ("truffle-api", "truffle-runtime", "truffle-compiler", "polyglot", "collections",
               "nativeimage", "word", "jniutils", "nativebridge")
    image_flags = ["-O3", "--macro:truffle-svm", "--module-path", os.pathsep.join(str(jars[m]) for m in modules),
                   "--initialize-at-build-time=bench.records,dev.elide.truffledata",
                   "--enable-native-access=ALL-UNNAMED", "-H:+UnlockExperimentalVMOptions",
                   "-H:-DeleteLocalSymbols", "-H:NativeLinkerOption=-L" + str(DEPS),
                   "-H:NativeLinkerOption=-ltruffledata_json", "-H:NativeLinkerOption=" + str(archive),
                   "-H:NativeLinkerOption=-Wl,-rpath," + ("@executable_path/deps" if sys.platform == "darwin" else "$ORIGIN/deps"),
                   "-H:-UnlockExperimentalVMOptions", "-cp", os.pathsep.join(map(str, [classes, TRUFFLE_JAR, *downstream])),
                   "--features=bench.records.RecordsForeignFeature", "bench.records.RecordBench", "-o", str(APP)]
    native_image(*image_flags, timeout=900)
    for mode in MODES:
        for count in (32, 35):
            run(APP, RANGES, mode, str(count), "32")
    no_inline = run(APP, "-Dpolyglot.compiler.Inlining=false", RANGES, "push", "35", "32", ok=False)
    if not no_inline.returncode or "spans outside compiled upcall cell" not in no_inline.stderr:
        raise RuntimeError("uninlined guest must fail the push compilation check: " + no_inline.stderr)
    metadata = dict(revision=run("git", "rev-parse", "HEAD").stdout.strip(), sources=fixture_hashes(),
                    truffledata=VERSION, native_image=image_flags, target=TARGET,
                    cc=run("cc", "--version").stdout, kernel_flags=["-O3", "-shared", "-fPIC"],
                    graal=run(Path(os.environ["GRAALVM_HOME"]) / "bin/native-image", "--version").stdout,
                    inputs={p.name: sha256_file(p) for p in [*downstream, parser, archive, TRUFFLE_JAR, *jars.values()]},
                    generated={p.name: sha256_file(p) for p in generation.glob("*.java")},
                    binaries={p.name: sha256_file(p) for p in (APP, RANGES, DEPS / ("libtruffledata_json" + SUFFIX))})
    (WORK / "build.json").write_text(json.dumps(metadata, indent=2) + "\n")


def sample(mode, rows, width, timeout=180):
    with subprocess.Popen([APP, RANGES, mode, str(rows), str(width)],
                          stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True) as process:
        try:
            stdout, stderr = process.communicate(timeout=timeout)
        except subprocess.TimeoutExpired:
            process.kill()
            process.communicate()
            raise
    if process.returncode:
        raise RuntimeError(f"{mode} exited with {process.returncode}: {stderr}")
    ns = [int(line) for line in stdout.splitlines()]
    verification = next((line for line in stderr.splitlines() if line.startswith("verified ")), "")
    if len(ns) != 12 or min(ns) <= 0 or not verification.startswith(f"verified {mode} rows={rows} compiled={rows} "):
        raise RuntimeError(f"{mode} failed verification: {stderr}")
    if mode == "push" and not verification.endswith(f" inlined_spans={(rows + width - 1) // width}"):
        raise RuntimeError(f"push failed to verify inlined spans: {verification}")
    return dict(mode=mode, pid=process.pid, ns=ns, verification=verification)


def measure(args):
    if os.getenv("CODSPEED_PROFILE_FOLDER"):
        raise RuntimeError("Run the cross-library comparison outside CodSpeed")
    metadata = json.loads((WORK / "build.json").read_text())
    if metadata["sources"] != fixture_hashes() or any(
        metadata["binaries"][p.name] != sha256_file(p) for p in (APP, RANGES, DEPS / ("libtruffledata_json" + SUFFIX))
    ):
        raise RuntimeError("Changed benchmark inputs, rebuild before measuring")
    if args.cpu is not None:
        os.sched_setaffinity(0, {args.cpu})
    data = dict(build=metadata, platform=platform.platform(),
                hardware=run("sysctl", "-n", "machdep.cpu.brand_string").stdout.strip()
                if sys.platform == "darwin" else run("lscpu").stdout.strip(),
                affinity=sorted(os.sched_getaffinity(0)) if hasattr(os, "sched_getaffinity") else None,
                rows=args.rows, width=args.width, rounds=12, warmup=3, samples=[])
    order = list(MODES)
    rng = random.Random(5521)
    for block in range(args.forks):
        if block % len(order) == 0:
            rng.shuffle(order)
        for mode in order[block % len(order):] + order[:block % len(order)]:
            data["samples"].append(dict(block=block, **sample(mode, args.rows, args.width)))
    data["ns_per_record"] = {mode: statistics.median(statistics.median(s["ns"]) / args.rows
                             for s in data["samples"] if s["mode"] == mode) for mode in MODES}
    out = WORK / f"records-{args.rows}-{args.width}.json"
    out.write_text(json.dumps(data, indent=2) + "\n")
    print(out, data["ns_per_record"])


if __name__ == "__main__":
    cli = argparse.ArgumentParser()
    cli.add_argument("command", choices=("build", "run"))
    cli.add_argument("--forks", type=int, default=5)
    cli.add_argument("--rows", type=int, default=32771)
    cli.add_argument("--width", type=int, default=32)
    cli.add_argument("--cpu", type=int)
    args = cli.parse_args()
    if min(args.forks, args.rows, args.width) < 1:
        cli.error("positive forks, rows and width required")
    if args.cpu is not None:
        if args.command != "run" or not hasattr(os, "sched_setaffinity"):
            cli.error("--cpu is supported for measurements on Linux")
        if args.cpu not in os.sched_getaffinity(0):
            cli.error("--cpu must be in the current process's CPU affinity mask")
    if args.command == "build":
        build()
    else:
        measure(args)
