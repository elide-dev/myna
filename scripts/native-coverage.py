"""Measure actual LLVM applicator execution, independently of JVM/Rust coverage."""
import json
import os
from pathlib import Path
import sys
sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "tests"))
from support import ROOT, run
work = ROOT / "build/llvm-coverage"
work.mkdir(parents=True, exist_ok=True)
llvm = Path(os.environ["LLVM_BIN"])
run("cmake", "--fresh", "-S", ROOT / "llvm", "-B", work, f"-DLLVM_DIR={run(llvm / 'llvm-config', '--cmakedir').stdout.strip()}",
    "-DCMAKE_BUILD_TYPE=Debug", "-DMYNA_COVERAGE=ON",
    f"-DCMAKE_TOOLCHAIN_FILE={os.environ['CMAKE_TOOLCHAIN_FILE']}")
run("cmake", "--build", work, "--parallel", 4)
for profile in work.glob("*.profraw"):
    profile.unlink()
os.environ["MYNA_LLVM"] = str(work / "myna-llvm")
os.environ["LLVM_PROFILE_FILE"] = str(work / "%p.profraw")
run(work / "myna-llvm-unit")
import llvm_integration
from support import main
try:
    main(llvm_integration, "llvm-integration")
except SystemExit as result:
    if result.code:
        raise
profiles = list(work.glob("*.profraw"))
if not profiles:
    raise AssertionError("No native coverage profiles were produced")
run(llvm / "llvm-profdata", "merge", "-sparse", *profiles, "-o", work / "merged.profdata")
args = [work / "myna-llvm", f"-object={work / 'myna-llvm-unit'}",
        f"-instr-profile={work / 'merged.profdata'}", ROOT / "llvm/main.cpp"]
result = run(llvm / "llvm-cov", "export", "-format=lcov", *args)
(ROOT / "build/reports/llvm.lcov").write_text(result.stdout)
print(run(llvm / "llvm-cov", "report", *args).stdout)

coverage = json.loads(run(llvm / "llvm-cov", "export", *args).stdout)
lines = coverage["data"][0]["totals"]["lines"]
if lines["percent"] < 98:
    raise AssertionError(f"LLVM production line coverage below 98%: {lines}")
