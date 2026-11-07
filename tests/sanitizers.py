# Copyright the Myna contributors.
# SPDX-License-Identifier: Apache-2.0 OR MIT
"""Exercise Elide's sanitizer runtimes, then run bounded, replayable native fuzzing."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import platform
import shutil
import subprocess
import sys
import time
from support import ROOT, run

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("mode", choices=["asan", "tsan"])
def positive(value):
    number = int(value)
    if number <= 0:
        raise argparse.ArgumentTypeError("must be positive")
    return number
campaign = parser.add_mutually_exclusive_group()
campaign.add_argument("--runs", type=positive, help="number of fuzz inputs (default: 20000)")
campaign.add_argument("--duration", type=positive, help="fuzz for this many seconds, then exit normally")
parser.add_argument("--rss-limit-mb", type=positive, default=512)
parser.add_argument("--malloc-limit-mb", type=positive, default=64)
parser.add_argument("--timeout", type=positive, default=5, help="maximum seconds per fuzz input")
parser.add_argument("--campaign-timeout", type=positive, help="watchdog deadline (default: at least 300 seconds, or duration + 30)")
args = parser.parse_args()
if args.duration is not None and args.mode != "asan":
    parser.error("--duration is only supported for the ASan fuzz campaign")
if args.campaign_timeout is None:
    args.campaign_timeout = max(300, (args.duration or 0) + 30)
if args.duration is not None and args.campaign_timeout <= args.duration:
    parser.error("--campaign-timeout must exceed --duration to allow clean sanitizer shutdown")
if args.runs is None and args.duration is None:
    args.runs = 20000
triple = {("Darwin", "arm64"): "arm64-apple-darwin", ("Linux", "x86_64"): "x86_64-unknown-linux-gnu",
          ("Linux", "aarch64"): "aarch64-unknown-linux-gnu"}[(platform.system(), platform.machine())]
home = Path(os.environ["ELIDE_TOOLCHAIN_HOME"])
if triple == "x86_64-unknown-linux-gnu":
    # Checksummed, version-matched instrumented libc++ and dependency overlay.
    print(run(home / "bin/elide-toolchain", "addon", "install", f"sanitizer-{args.mode}").stdout, flush=True)
compiler = home / "bin" / f"{triple}-{args.mode}-clang++"
flags = ["-std=c++17", "-O1", "-g", "-fno-omit-frame-pointer", "-pthread", "-I", ROOT / "include"]
if args.mode == "asan":
    flags += ["-fsanitize=undefined", "-fno-sanitize-recover=all"]
# CI must fail on the first report, even if an inherited environment disabled it.
options = {"ASAN_OPTIONS": "detect_leaks=1:halt_on_error=1:abort_on_error=1:quarantine_size_mb=64",
           "LSAN_OPTIONS": "exitcode=23:report_objects=1",
           "UBSAN_OPTIONS": "halt_on_error=1:print_stacktrace=1",
           "TSAN_OPTIONS": "halt_on_error=1:exitcode=66"}
os.environ.update(options)
if sys.platform == "darwin":
    flags += ["-isysroot", os.environ["SDKROOT"], "-fuse-ld=/usr/bin/ld"]
work = ROOT / "build/sanitizers" / args.mode
work.mkdir(parents=True, exist_ok=True)
(ROOT / "build/reports").mkdir(parents=True, exist_ok=True)
source = ROOT / "tests/fuzz"
def bounded_run(command, rss_limit_mb, report, timeout_seconds, log_name):
    """Enforce RSS externally; compiler-rt's RSS limit is ineffective on macOS."""
    started = time.monotonic()
    peak_kb = 0
    reason = None
    log_path = work / log_name
    report_path = work / report
    def snapshot(status, returncode):
        pending = report_path.with_suffix(".json.tmp")
        pending.write_text(json.dumps({
            "status": status, "log": str(log_path.relative_to(ROOT)),
            "rss_limit_mb": rss_limit_mb, "observed_peak_kb": peak_kb,
            "process_timeout_seconds": timeout_seconds,
            "elapsed_seconds": time.monotonic() - started,
            "returncode": returncode, "limit_failure": reason,
        }, indent=2) + "\n")
        pending.replace(report_path)
    # Output is durable while the campaign is running, including if the outer
    # runtime terminates unexpectedly. No subprocess pipe can fill between polls.
    with log_path.open("w") as log:
        with subprocess.Popen(list(map(str, command)), cwd=ROOT, env=dict(os.environ),
                              stdout=log, stderr=subprocess.STDOUT, text=True) as process:
            snapshot("running", None)
            last_snapshot = started
            try:
                while process.poll() is None:
                    usage = run("ps", "-o", "rss=", "-p", str(process.pid), ok=False).stdout.strip()
                    peak_kb = max(peak_kb, int(usage) if usage else 0)
                    if peak_kb > rss_limit_mb * 1024:
                        reason = f"RSS limit exhausted: {peak_kb} KiB > {rss_limit_mb} MiB"
                    elif time.monotonic() - started > timeout_seconds:
                        reason = f"campaign exceeded {timeout_seconds} second process limit"
                    if reason:
                        process.kill()
                        break
                    if time.monotonic() - last_snapshot >= 1:
                        snapshot("running", None)
                        last_snapshot = time.monotonic()
                    time.sleep(0.1)
            except BaseException as failure:
                reason = f"runner interrupted: {type(failure).__name__}: {failure}"
                raise
            finally:
                if process.poll() is None:
                    process.kill()
                process.wait()
                if reason:
                    log.write(f"\nMyna runner: {reason}\n")
                log.flush()
                snapshot("passed" if process.returncode == 0 and reason is None else "failed", process.returncode)
    return subprocess.CompletedProcess(command, process.returncode, "", log_path.read_text())

def canary(name, define, expected):
    binary = work / name
    run(compiler, *flags, *([f"-D{define}"] if define else []), source / "canary.cpp", "-o", binary)
    if define == "MYNA_RSS_CANARY":
        result = bounded_run([binary], 32, "rss-canary.json", 30, f"{name}.log")
    else:
        result = run(binary, ok=False)
        (work / f"{name}.log").write_text(result.stdout + result.stderr)
    if result.returncode == 0 or expected not in result.stderr:
        raise AssertionError(f"Sanitizer canary did not detect {expected}: {result.stderr}")

if args.mode == "tsan":
    canary("canary", "MYNA_RACE_CANARY", "ThreadSanitizer: data race")
else:
    canary("canary", None, "AddressSanitizer: heap-buffer-overflow")
    canary("leak-canary", "MYNA_LEAK_CANARY", "LeakSanitizer: detected memory leaks")
    canary("undefined-canary", "MYNA_UB_CANARY", "runtime error: signed integer overflow")
    canary("rss-canary", "MYNA_RSS_CANARY", "Myna runner: RSS limit exhausted")
resource = Path(run(compiler, "--print-resource-dir").stdout.strip())
runtime_files = [path for path in (resource / "lib").rglob("libclang_rt.*") if path.is_file()
                 and any(f".{name}" in path.name for name in ("asan", "lsan", "tsan", "ubsan", "fuzzer"))]
def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()
(work / "runtime.json").write_text(json.dumps({
    "target": triple, "toolchain": run(home / "bin/elide-toolchain", "version").stdout.strip(),
    "compiler": run(compiler, "--version").stdout, "flags": list(map(str, flags)),
    "options": options, "resource_dir": str(resource),
    "rss_watchdog": "host ps RSS sampled every 100ms; expected-failure canary required",
    "rss_limit_mb": args.rss_limit_mb,
    "fuzz_runs": args.runs,
    "fuzz_duration_seconds": args.duration,
    "campaign_timeout_seconds": args.campaign_timeout,
    "outer_runtime_timeout": os.environ.get("MYNA_PYTHON_TIMEOUT", "Elide default"),
    "runtime_inventory_sha256": {str(path.relative_to(home)): digest(path) for path in sorted(runtime_files)},
    "source_sha256": {str(path.relative_to(ROOT)): digest(path) for path in sorted(source.glob("*.cpp"))},
}, indent=2) + "\n")
(work / "link-command.log").write_text(run(compiler, "-###", *flags, source / "canary.cpp", "-o", work / "canary").stderr)
if args.mode == "tsan":
    run(compiler, *flags, source / "threads.cpp", "-o", work / "threads")
    result = run(work / "threads", ok=False)
    (work / "threads.log").write_text(result.stdout + result.stderr)
    if result.returncode:
        raise AssertionError(f"TSan workload failed: {result.stderr}")
else:
    run(compiler, *flags, "-fsanitize=fuzzer", "-fprofile-instr-generate", "-fcoverage-mapping", source / "values.cpp", "-o", work / "fuzz-values")
    corpus = work / "corpus"
    # A seed and retained command are reproducible only from a known starting corpus.
    shutil.rmtree(corpus, ignore_errors=True)
    corpus.mkdir()
    for seed in (source / "corpus").iterdir():
        (corpus / seed.name).write_bytes(seed.read_bytes())
    stop = f"-max_total_time={args.duration}" if args.duration is not None else f"-runs={args.runs}"
    command = [work / "fuzz-values", stop, "-seed=1729", "-max_len=1024",
               # Pinned libFuzzer's detached RSS watcher leaks its thread state on macOS.
               # The external watchdog enforces the cap; the RSS canary proves it works.
               "-rss_limit_mb=0", f"-malloc_limit_mb={args.malloc_limit_mb}",
               f"-timeout={args.timeout}", "-detect_leaks=1",
               f"-artifact_prefix={work}/", corpus]
    # macOS libc++abi leaks a demangler output buffer when libFuzzer prints newly
    # covered template functions. Disable that progress listing, not symbolized
    # sanitizer reports or leak detection. Expected-failure canaries still run.
    if sys.platform == "darwin":
        command.append("-print_new_funcs=0")
    os.environ["LLVM_PROFILE_FILE"] = str(work / "values.profraw")
    (work / "values.profraw").unlink(missing_ok=True)
    (work / "command.json").write_text(json.dumps(list(map(str, command)), indent=2))
    result = bounded_run(command, args.rss_limit_mb, "fuzz-resources.json", args.campaign_timeout, "fuzz.log")
    if result.returncode:
        raise AssertionError(f"ASan/UBSan/LSan fuzz workload failed: {result.stderr}")
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
print(f"{args.mode}: {'race canary' if args.mode == 'tsan' else 'overflow, UB, leak and RSS canaries'} and workload passed")
