"""Build own-workload CodSpeed hooks from the PR #29 pinned submodule."""
import os
from pathlib import Path
import sys
sys.path.insert(0, str(Path(__file__).resolve().parents[2] / "tests"))
from support import ROOT, run
hooks = ROOT / "third-party/codspeed-jvm/jmh-fork/jmh-core/native-instrument-hooks/instrument-hooks"
if not (hooks / "dist/core.c").exists():
    raise RuntimeError("Run git submodule update --init --recursive")
work = ROOT / "build/bench-native"
work.mkdir(parents=True, exist_ok=True)
run(os.environ["CC"], "-O2", "-I", hooks / "includes", "-c", hooks / "dist/core.c", "-o", work / "hooks.o")
run(os.environ["CXX"], "-std=c++17", "-O3", "-g", "-I", ROOT / "include", "-I", hooks / "includes",
    ROOT / "benchmarks/native/values.cpp", work / "hooks.o", "-o", work / "values")
for name in ("inline", "owned", "borrowed"):
    print(run(work / "values", name).stdout, end="")
if run(work / "values", "inline", "--require-instrumentation", ok=False).returncode != 3:
    raise AssertionError("Instrumentation guard must reject ordinary executions")
