"""Exercise Elide's sanitizer runtimes, then run bounded, replayable native fuzzing."""
import argparse
import json
import os
from pathlib import Path
import platform
import sys
from support import ROOT, run

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("mode", choices=["asan", "tsan"])
parser.add_argument("--runs", type=int, default=20000)
args = parser.parse_args()
triple = {("Darwin", "arm64"): "arm64-apple-darwin", ("Linux", "x86_64"): "x86_64-unknown-linux-gnu",
          ("Linux", "aarch64"): "aarch64-unknown-linux-gnu"}[(platform.system(), platform.machine())]
home = Path(os.environ["ELIDE_TOOLCHAIN_HOME"])
if triple == "x86_64-unknown-linux-gnu":
    # Checksummed, version-matched instrumented libc++ and dependency overlay.
    print(run(home / "bin/elide-toolchain", "addon", "install", f"sanitizer-{args.mode}").stdout, flush=True)
compiler = home / "bin" / f"{triple}-{args.mode}-clang++"
flags = ["-std=c++17", "-O1", "-g", "-fno-omit-frame-pointer", "-pthread", "-I", ROOT / "include"]
if sys.platform == "darwin":
    flags += ["-isysroot", os.environ["SDKROOT"], "-fuse-ld=/usr/bin/ld"]
work = ROOT / "build/sanitizers" / args.mode
work.mkdir(parents=True, exist_ok=True)
source = ROOT / "tests/fuzz"
canary = work / "canary"
run(compiler, *flags, *(["-DMYNA_RACE_CANARY"] if args.mode == "tsan" else []), source / "canary.cpp", "-o", canary)
result = run(canary, ok=False)
(work / "canary.log").write_text(result.stdout + result.stderr)
expected = "ThreadSanitizer: data race" if args.mode == "tsan" else "AddressSanitizer: heap-buffer-overflow"
if result.returncode == 0 or expected not in result.stderr:
    raise AssertionError(f"Sanitizer canary did not detect {expected}: {result.stderr}")
if args.mode == "tsan":
    run(compiler, *flags, source / "threads.cpp", "-o", work / "threads")
    run(work / "threads")
else:
    run(compiler, *flags, "-fsanitize=fuzzer,undefined", "-fprofile-instr-generate", "-fcoverage-mapping", source / "values.cpp", "-o", work / "fuzz-values")
    corpus = work / "corpus"
    corpus.mkdir(exist_ok=True)
    for seed in (source / "corpus").iterdir():
        (corpus / seed.name).write_bytes(seed.read_bytes())
    command = [work / "fuzz-values", f"-runs={args.runs}", "-seed=1729", "-max_len=1024",
               f"-artifact_prefix={work}/", corpus]
    os.environ["LLVM_PROFILE_FILE"] = str(work / "values.profraw")
    result = run(*command)
    (work / "fuzz.log").write_text(result.stdout + result.stderr)
    (work / "command.json").write_text(json.dumps(list(map(str, command)), indent=2))
    run(home / "bin/llvm-profdata", "merge", "-sparse", work / "values.profraw", "-o", work / "values.profdata")
    coverage = run(home / "bin/llvm-cov", "export", work / "fuzz-values", "-format=lcov",
                   f"-instr-profile={work / 'values.profdata'}", ROOT / "include/myna/values.hpp", ROOT / "include/myna/values.h")
    (ROOT / "build/reports/cpp.lcov").write_text(coverage.stdout)
    summary = run(home / "bin/llvm-cov", "report", work / "fuzz-values",
                  f"-instr-profile={work / 'values.profdata'}", ROOT / "include/myna/values.hpp", ROOT / "include/myna/values.h")
    print(summary.stdout)
    details = json.loads(run(home / "bin/llvm-cov", "export", work / "fuzz-values",
                            f"-instr-profile={work / 'values.profdata'}",
                            ROOT / "include/myna/values.hpp", ROOT / "include/myna/values.h").stdout)
    lines = details["data"][0]["totals"]["lines"]
    if lines["percent"] < 98:
        raise AssertionError(f"C++ production line coverage below 98%: {lines}")
print(f"{args.mode}: canary and workload passed")
