# Performance maintenance and boundary comparisons

Myna maintains two separate benchmark systems. CodSpeed tracks regressions in
Myna's own workloads; comparison measurements evaluate equivalent interop paths
under explicit runtime and ownership contracts. Neither system treats results
from different hardware, execution tiers, or instrumentation modes as directly
comparable.

SVM is the performance acceptance target. Generated direct C calls should meet
or beat equivalent handwritten Native Image `@CFunction` calls with the same
transition contract, native target, compiler settings and workload. Evaluate
normal transitions and transition-free leaves independently. AOT FFM is an
additional SVM adapter; improving it does not substitute for direct C API parity.
JVM FFM and JNI measurements are secondary diagnostics.

Do not declare parity from overlapping noisy intervals alone. Retain repeated
controlled measurements and inspect generated call sites for extra instructions,
indirection, allocations or transitions. Investigate a reproducible regression
against the handwritten C API before accepting a performance change. When both
paths compile to the same call sequence, parity is the expected result; beating
the baseline requires a real optimization available under the same contract.

## Toolchain and execution modes

Pin Elide 1.6.2, the native toolchain 2026.11.1, and GraalVM
25.5.5-elide.24. The latter is an explicit external distribution: Elide 1.6.2's
embedded Native Image compiler reports 25.5.5-elide.21. Retain archive checksums,
compiler versions, edition, target, JVM flags and native link flags with results.
Rust and LLVM must pass bitcode compatibility tests, beyond a version-major check.

Exercise the generated Native Image C API, FFM AOT registration, JVM FFM with
Graal JIT compilation, and a compiled Truffle guest consumer where applicable.
Warmup must establish the intended compilation tier before measurement; a JIT
failure must fail the experiment rather than silently become an interpreter result.

## Continuous Myna benchmarks

Build on PR #29's CodSpeed JMH wall-time generator benchmarks, retaining its pinned
JMH fork and workloads. Add native simulation regions for Myna's boundary and
value operations, using pinned CodSpeed instrumentation hooks. Keep setup,
compilation, warmup, loading, allocation of reusable buffers, and isolate creation
outside the measured regions unless a case explicitly measures those costs.
An instrumentation-required invocation must fail outside the CodSpeed runner.

Keep wall-time and simulation series distinct. Simulation instruction counts are
useful regression signals; they are not elapsed nanoseconds. Wall-time regressions
need repeated samples on a controlled runner, with noise visible. CI should always
check workload results and optimizer invariants even where timing is advisory.

## Comparison matrix

The primary Native Image baseline is handwritten C API glue with the same target
function and transition contract. Compare generated and handwritten paths for:

- No-argument and scalar calls, pointer/length buffers, and callbacks.
- Normal transitions separately from explicitly contracted bounded native leaves.
- Generated FFM versus manually prepared FFM handles, both on JVM and in AOT
  images with the necessary downcall registrations.
- JNI as a JVM baseline with the same primitive workload and no extra conversion.
- Original versus Myna-annotated LLVM objects, linked with ThinLTO; retain the
  optimized IR and disassembly to prove which loads/checks/calls disappear.

BoltFFI is a separately pinned ecosystem comparison when it can expose the same
Java/JVM primitive operation. Do not compare a boxed, locking, allocating or
serialization-heavy path with a raw scalar path and attribute the difference to
call overhead. Record any unavoidable wrapper differences.

Native Image machine code is opaque to LLVM. ThinLTO may optimize participating
C/Rust callers using approved contracts; it cannot inline arbitrary Java machine
code as though it were LLVM IR. A successful executable alone is insufficient
proof of optimization: check optimized IR and machine instructions.

## Rerunnable measurement contract

Check in workloads, build/run/report commands, correctness checks and result
schemas. A full run uses multiple fresh processes, warmup and measurement samples,
balanced case ordering, input-dependent results and observable checksums to resist
dead-code elimination. Report the loop/control baseline rather than blindly
subtracting it from every timing.

Retain raw per-process samples, medians and variability, confidence intervals,
source revision and dirty state, source and binary hashes, input sizes, seed,
operating system, CPU, runtime/edition, all tool versions and exact commands.
A report can be regenerated from saved data without rebuilding or remeasuring.
Comparisons reject mismatched workload schemas or execution profiles.

Keep at least one checked-in baseline with its metadata. Continuous measurements
retain artifacts and provide an explicit baseline update procedure. A smoke run
checks correctness and the pipeline; it must not be presented as a stable timing
baseline. No universal optimum is inferred from a single machine or one ratio.

## Correctness and coverage gates

Measure production JVM, Rust and native C/C++ coverage separately. Tests themselves
must not inflate production coverage. Native-only Java readers require real SVM
execution; their absence from JVM coverage is not evidence they are untested.
Raise gates toward near-complete meaningful coverage as gaps are closed, documenting
any intrinsically platform-specific or unreachable paths.

Run reproducible property tests and bounded fuzz campaigns with checked-in seed
corpora and retained failures. Use ASan/UBSan for native producer, parser adaptation
and LLVM input handling. Use TSan for supported concurrent ownership/read scenarios,
with independent allocators where required. A sanitizer lane must prove its runtime
is active using a separate expected-failure canary. Do not claim that sanitizer
coverage of native libraries instruments the Native Image managed heap.

## Implemented commands

Run `mise install && mise run setup` first. Setup verifies the published GraalVM
archive against `tools/graalvm.json`; the native bundle is locked in `mise.lock`.
`mise run build:llvm` builds with the pinned compiler and external Native Image.
The direct driver avoids Elide 1.6.2's embedded `.21` image compiler.

```sh
mise run test:jvm             # production JVM: >=99% lines, >=93% branches
mise run test:rust-coverage   # production Rust: >=97.5% lines
mise run test:llvm-coverage   # actual object applicator execution, LCOV report
mise run test:cpp             # C++ simdjson adaptation under ASan/UBSan
mise run test:asan            # canary + deterministic libFuzzer campaign + C++ LCOV
mise run test:tsan            # race canary + immutable sharing/independent allocators
mise run bench:native-build   # own values workloads, normal execution correctness
mise run bench:comparison-build
mise run bench:comparison     # five fresh processes/case, 10 warmup + 10 measured samples
mise exec -- scripts/python.sh benchmarks/comparison/run.py report --output build/comparison/results.json
```

The checked-in comparison currently measures dependent scalar call latency:
generated/manual Native Image normal and leaf calls, generated/manual FFM on JVM
and Native Image, JNI on JVM, and a control loop. Each matched pair calls the same
native function. Normal and leaf declarations have separate symbols containing
the same arithmetic. JNI delegates to the normal function. No allocation or
string conversion is part of this workload. Buffer/callback correctness and
ThinLTO optimization are checked by integration tests; their timings are future
extensions of this comparison matrix. BoltFFI is not required for this primitive
baseline and is not represented by invented estimates.

JVM runs request synchronous Graal compilation with tiering disabled. They fail
unless compilation logs establish that the measured loop compiled before warmup
ended. Generated AOT FFM now initializes unbound downcall adapters at image build time,
while addresses bind at runtime. This lets Graal compile the MethodHandle adapter
chain without putting native addresses in the image heap. The handwritten FFM
baseline uses the same technique; a separate `manual-bound-ffm` case retains the
runtime-created bound-handle cost for comparison. The public SymbolLookup factory
remains available for callers that need independently bound handles.

Raw JSON, per-process JSONL, compilation logs, hashes and reports live under
`build/comparison/`. Preserve that directory when recording a baseline. The runner
rejects changed sources or binaries after its build. A smoke run uses one process
and 10,000 iterations and is explicitly marked as unsuitable for stable timing
claims. Medians, process ranges and bootstrap intervals describe the observed
runs, not universal hardware limits. Compare results only for matching workload,
compiler/runtime, transition policy and hardware profiles.

CodSpeed's generator JMH wall series uses the PR #29 harness. Separate native
wall and simulation jobs exercise inline, owned and borrowed value construction.
These use its pinned instrument-hooks submodule and fail if instrumentation was
requested but is absent. Native wall-time runs retain 30 raw measured rounds after
10 warmup rounds (100,000 operations each), and emit the pinned integration's wall-time
result schema. Hooks alone provide profiles, not wall-time result statistics. Cross-library comparison runs do not use those jobs.

## Sanitizer and coverage boundaries

The sanitizer wrappers and Linux amd64 instrumented-library add-ons come from
`elide-dev/toolchain` 2026.11.1. ASan and TSan each must detect a deliberate bug in
a separate process before a clean workload can pass. Fuzzer seed is 1729; seed
corpora are checked in under `tests/fuzz/corpus`. Extend a campaign with
`mise exec -- scripts/python.sh tests/sanitizers.py asan --runs 1000000`; replay
failures with `build/sanitizers/asan/fuzz-values path/to/crash-file`.

On macOS, LLVM 23.1.4's `ld64.lld` plus ASan fails a minimal C++ exception-catching
probe. Sanitizer builds therefore retain pinned Clang/compiler-rt and use
`/usr/bin/ld`; normal ThinLTO builds continue using LLD. Apple SDK headers are
selected explicitly. Native Image's Apple-only compiler-banner check is disabled;
real C API, FFM AOT, Rust and C++ integration builds validate compatibility.

Elide Graal's Native Image managed heap is not claimed as ASan/TSan instrumented.
The sanitizers exercise native allocation/ownership code. Native-only Java readers
are checked in SVM integration tests and excluded solely from the JVM denominator.
The production coverage selector fails if a production source disappears from the
report; test classes cannot inflate the percentage. Rust coverage runs the actual
integration-test binary after cleaning profiles: combining the unused unit-test
binary causes LLVM coverage to misattribute the unmangled C bridge's counters.
Remaining defensive limits include >4GiB text/region counts and impossible Serde
protocol states; these are not removed merely to raise the percentage.

Compare two full saved runs with
`mise exec -- scripts/python.sh benchmarks/comparison/compare.py baseline.json candidate.json`.
It rejects smoke runs and mismatched workload sources, hardware, runtime, compiler,
iteration counts or case sets. Changes are advisory percentages; timing thresholds
belong on controlled runners, not shared CI hosts.

ThinLTO tests retain before/after optimized C and Rust IR, final linker IR,
disassembly and artifact/tool hashes under `build/reports/thinlto/<target>/`.
These tests prove load elimination across the opaque call without pretending that
LLVM can inline Java Native Image machine code.


The `Boundary comparisons` workflow runs the standalone harness without CodSpeed
instrumentation or performance-series uploads. It compiles the generator from
current Java sources with pinned `javac`. Build/run/report commands remain usable
locally. Historical local before/after data is checked in under
`benchmarks/comparison/baselines`; it is advisory because this shared workstation
experienced substantial concurrent load.

ARM binaries are built on hosted ARM builders and measured on `ubuntu-24.04-arm`
by default. Set the repository variable `MYNA_ARM_BENCH_RUNNER` to a compatible,
controlled ARM runner label for repeatable timing gates. Shared-host wall-time
results remain advisory. CodSpeed's older `codspeed-macro` ARMv8.0 CPU cannot run
the pinned compiler/runtime libraries: both native and JNI benchmarks failed with
illegal instructions there. Compiling workload sources with `-march=armv8-a`
does not lower the instruction requirements of prebuilt runtime libraries. The
replacement runner must support ARMv8.2 and SVE for the current linked artifacts.

C/C++ benchmark sources target ARMv8-A and Native Image uses
`-march=compatibility`; execution-profile and hardware metadata prevent comparing
incompatible profiles. Build and measurement provenance remain separate. The
pinned CodSpeed JMH fork's x86-only native-library target is overridden by the
checked-in Gradle initialization script when building ARM benchmark artifacts.

See `benchmarks/comparison/evidence/` for the scalar direct-call instruction
comparison and [operations](operations.md) for symbol-sidecar publication.
