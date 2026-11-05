"""Standalone comparisons; no CodSpeed instrumentation or cross-machine ratios."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import platform
import random
import statistics
import shutil
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "tests"))
from support import TARGET, native_image, run
SOURCE = Path(__file__).resolve().parent
WORK = ROOT / "build/comparison"
COMMANDS = []

def execute(*args):
    COMMANDS.append([str(arg) for arg in args])
    return run(*args).stdout

def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def build():
    WORK.mkdir(parents=True, exist_ok=True)
    generated = WORK / "generated"
    generator_classes = WORK / "generator-classes"
    shutil.rmtree(generator_classes, ignore_errors=True)
    generator_classes.mkdir()
    generator_sources = sorted((ROOT / "src/main/java").rglob("*.java"))
    execute("javac", "-d", generator_classes, *generator_sources)
    shutil.rmtree(generated, ignore_errors=True)
    execute("java", "-cp", generator_classes, "dev.elide.myna.cli.Main",
            "generate", SOURCE / "boundary.seam", "--out", generated, "--target", TARGET)
    home = Path(os.environ["GRAALVM_HOME"])
    cpu_flags = ["-march=armv8-a"] if sys.platform == "linux" and platform.machine() == "aarch64" else []
    flags = ["-O3", *cpu_flags, "-fPIC", "-I", home / "include", "-I", home / "include" / ("darwin" if sys.platform == "darwin" else "linux")]
    execute(os.environ["CC"], *flags, "-c", SOURCE / "kernel.c", "-o", WORK / "kernel.o")
    library = WORK / ("libboundary.dylib" if sys.platform == "darwin" else "libboundary.so")
    execute(os.environ["CC"], "-shared", WORK / "kernel.o", "-o", library)
    classes = WORK / "classes"
    shutil.rmtree(classes, ignore_errors=True)
    classes.mkdir()
    sdk = [home / "lib/svm" / name for name in ("svm-shared.jar", "svm-guest-staging.jar", "svm-guest.jar")]
    execute("javac", "-cp", os.pathsep.join(map(str, sdk)), "-d", classes, *sorted(SOURCE.glob("*.java")), *sorted(generated.glob("*.java")))
    shutil.copytree(generated / "META-INF", classes / "META-INF", dirs_exist_ok=True)
    # Record actual builder commands including its driver flags in build metadata.
    native_args = ["-O3", "-march=compatibility", "-cp", str(classes), "-H:+UnlockExperimentalVMOptions",
                   f"-H:NativeLinkerOption={WORK / 'kernel.o'}", "-H:-UnlockExperimentalVMOptions",
                   "bench.Native", "-o", str(WORK / "native")]
    result = native_image(*native_args)
    (WORK / "native-build.log").write_text(result.stdout + result.stderr)
    COMMANDS.append([str(home / "bin/native-image"), "-J--enable-native-access=ALL-UNNAMED",
                     "-H:+UnlockExperimentalVMOptions", "-H:-CheckToolchain",
                     f"-H:CCompilerPath={os.environ['CC']}", "-H:-UnlockExperimentalVMOptions", *native_args])
    ffm_args = ["-O3", "-march=compatibility", "--enable-native-access=ALL-UNNAMED", "-cp", str(classes),
                "--initialize-at-run-time=bench.Jvm", "--initialize-at-build-time=bench.Jvm$Unbound", "--features=bench.GeneratedForeignFeature",
                "bench.Jvm", "-o", str(WORK / "ffm-aot")]
    result = native_image(*ffm_args)
    (WORK / "ffm-build.log").write_text(result.stdout + result.stderr)
    COMMANDS.append([str(home / "bin/native-image"), "-J--enable-native-access=ALL-UNNAMED",
                     "-H:+UnlockExperimentalVMOptions", "-H:-CheckToolchain",
                     f"-H:CCompilerPath={os.environ['CC']}", "-H:-UnlockExperimentalVMOptions", *ffm_args])
    sources = [*SOURCE.glob("*"), *generator_sources, ROOT / "tests/support.py", ROOT / "mise.lock", ROOT / "tools/graalvm.json"]
    binaries = [WORK / "native", WORK / "ffm-aot", library, *classes.rglob("*.class")]
    versions = {}
    for tool, command in {
        "java": ("java", "-version"), "clang": (os.environ["CC"], "--version"),
        "native-image": (home / "bin/native-image", "--version"),
        "toolchain": ("elide-toolchain", "version")}.items():
        result = run(*command)
        versions[tool] = result.stdout + result.stderr
    metadata = {
        "schema": 1, "workload": "scalar-recurrence-v2", "target": TARGET,
        "execution_profile": {"native_image_cpu": "compatibility", "c_cpu_flags": cpu_flags},
        "revision": execute("git", "rev-parse", "HEAD").strip(),
        "dirty": execute("git", "status", "--porcelain"),
        "sources": {str(p.relative_to(ROOT)): digest(p) for p in sources if p.is_file()},
        "binaries": {str(p.relative_to(ROOT)): digest(p) for p in binaries},
        "versions": versions,
        "graalvm": json.loads((ROOT / "tools/graalvm.json").read_text()),
        "build_commands": COMMANDS, "library": str(library),
    }
    (WORK / "build.json").write_text(json.dumps(metadata, indent=2) + "\n")

def linux_cpu_identity(cpuinfo):
    # ARM exposes different keys from x86. Retain all distinct core identities
    # on heterogeneous hosts, excluding frequency/load and processor indices.
    keys = ("vendor_id", "model name", "cpu family", "model", "stepping", "microcode", "flags",
            "CPU implementer", "CPU architecture", "CPU variant", "CPU part", "CPU revision", "Features")
    identities = set()
    for block in cpuinfo.strip().split("\n\n"):
        fields = dict(line.split(":", 1) for line in block.splitlines() if ":" in line)
        fields = {k.strip(): v.strip() for k, v in fields.items()}
        identity = {key: fields[key] for key in keys if fields.get(key)}
        if not (identity.get("model name") or
                (identity.get("CPU implementer") and identity.get("CPU part"))):
            continue
        identities.add(json.dumps(identity, sort_keys=True))
    if not identities:
        raise RuntimeError("Cannot establish CPU identity for repeatable measurements")
    return "\n".join(sorted(identities))

def cpu_identity():
    if sys.platform == "darwin":
        return run("sysctl", "-n", "machdep.cpu.brand_string").stdout.strip()
    return linux_cpu_identity(Path("/proc/cpuinfo").read_text())

def measure(args):
    cpu = cpu_identity()  # Fail before a long measurement if provenance is missing.
    metadata = json.loads((WORK / "build.json").read_text())
    for name, expected in metadata["sources"].items():
        if digest(ROOT / name) != expected:
            raise RuntimeError(f"Source changed since build: {name}")
    for name, expected in metadata["binaries"].items():
        if digest(ROOT / name) != expected:
            raise RuntimeError(f"Binary changed since build: {name}")
    cases = {"native": ["control", "generated-normal", "manual-normal", "generated-leaf", "manual-leaf"],
             "jvm": ["control", "generated-ffm", "manual-ffm", "manual-bound-ffm", "jni"],
             "ffm-aot": ["control", "generated-ffm", "manual-ffm", "manual-bound-ffm"]}
    schedule = [(mode, name) for mode, names in cases.items() for name in names]
    random.Random(args.seed).shuffle(schedule)
    samples = []
    logs = args.output.parent / (args.output.stem + "-logs")
    logs.mkdir(parents=True, exist_ok=True)
    for fork in range(args.forks):
        # Rotate orders across forks; each case gets its own fresh process.
        order = schedule[fork % len(schedule):] + schedule[:fork % len(schedule)]
        for mode, name in order:
            samples_file = logs / f"{fork}-{mode}-{name}.jsonl"
            samples_file.unlink(missing_ok=True)
            result_option = f"-Dmyna.bench.results={samples_file.resolve()}"
            if mode == "native":
                command = [WORK / "native", result_option, name, args.iterations]
            else:
                options = [result_option, f"-Dmyna.bench.library={WORK / Path(metadata['library']).name}"]
                if mode == "jvm":
                    command = ["java", "--enable-native-access=ALL-UNNAMED", "-Xbatch",
                               "-XX:+UseJVMCICompiler", "-XX:-TieredCompilation", "-XX:+PrintCompilation",
                               *options, "-cp", WORK / "classes", "bench.Jvm", name, args.iterations]
                else:
                    command = [WORK / "ffm-aot", *options, name, args.iterations]
            print(f"{fork + 1}/{args.forks}: {mode}/{name}", flush=True)
            result = run(*command)
            (logs / f"{fork}-{mode}-{name}.log").write_text(result.stdout + result.stderr)
            parsed = [json.loads(line) for line in samples_file.read_text().splitlines()]
            if len(parsed) != 10:
                raise RuntimeError("Missing measurement samples")
            for sample in parsed:
                if sample["ns"] <= 0 or sample["iterations"] != args.iterations:
                    raise RuntimeError("Invalid measurement")
            evidence = []
            if mode == "jvm":
                method = {"control": "bench.Measure::reference", "generated-ffm": "bench.Jvm::lambda$main$0",
                          "manual-ffm": "bench.Jvm::lambda$main$1", "manual-bound-ffm": "bench.Jvm::lambda$main$2", "jni": "bench.Jvm::lambda$main$3"}[name]
                evidence = [line for line in result.stdout.splitlines() if method in line
                            and " b " in line and line.split()[0].isdigit()
                            and int(line.split()[0]) <= parsed[0]["warmup_uptime_ms"]]
                if not evidence:
                    raise RuntimeError(f"No synchronous Graal compilation before measurement: {method}")
            samples.append({"compilation_evidence": evidence,"mode": mode, "case": name, "fork": fork,
                            "command": list(map(str, command)), "samples": parsed})
    result = {"build": metadata, "host": {"platform": platform.platform(), "machine": platform.machine(),
              "cpu": cpu},
              "seed": args.seed, "forks": args.forks, "iterations": args.iterations,
              "smoke": args.forks < 5 or args.iterations < 1_000_000, "results": samples}
    args.output.write_text(json.dumps(result, indent=2) + "\n")
    report(args.output)

def report(path):
    data = json.loads(path.read_text())
    groups = {}
    for result in data["results"]:
        med = statistics.median(s["ns"] / s["iterations"] for s in result["samples"])
        groups.setdefault((result["mode"], result["case"]), []).append(med)
    lines = ["# Scalar call boundary comparison", "", f"Revision: `{data['build']['revision']}`.",
             f"Fresh processes per case: {data['forks']}. Smoke run: {data['smoke']}.", "",
             "Each row summarizes process medians. Modes and transition policies are separate comparisons.",
             "Control is shown without subtraction. This is a dependent scalar latency workload.", "",
             "| Mode | Case | Median ns/op | Min–max process median | Bootstrap 95% CI |", "|---|---|---:|---:|---:|"]
    for (mode, case), values in sorted(groups.items(), key=lambda item: ({"native": 0, "ffm-aot": 1, "jvm": 2}[item[0][0]], item[0][1])):
        rng = random.Random(data["seed"])
        boot = sorted(statistics.median(rng.choices(values, k=len(values))) for _ in range(10000))
        ci = f"{boot[249]:.3f}–{boot[9749]:.3f}" if len(values) >= 5 else "insufficient forks"
        lines.append(f"| {mode} | {case} | {statistics.median(values):.3f} | {min(values):.3f}–{max(values):.3f} | {ci} |")
    path.with_suffix(".md").write_text("\n".join(lines) + "\n")

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("command", choices=["build", "run", "report"])
    parser.add_argument("--output", type=Path, default=WORK / "results.json")
    parser.add_argument("--forks", type=int, default=5)
    parser.add_argument("--iterations", type=int, default=1_000_000)
    parser.add_argument("--seed", type=int, default=1729)
    args = parser.parse_args()
    if args.forks < 1 or args.iterations < 1:
        parser.error("forks and iterations must be positive")
    if args.command == "build": build()
    elif args.command == "run": measure(args)
    else: report(args.output)
