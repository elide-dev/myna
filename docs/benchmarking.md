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

Pin Elide 1.6.2, the native toolchain 2026.11.6, and GraalVM
25.5.5-elide.28. The latter is an explicit external distribution: Elide 1.6.2's
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

The checked-in comparison measures dependent scalar call latency: generated/manual
Native Image normal and leaf calls, ordinary and critical FFM on JVM and SVM,
explicit FFM library bindings, generated/manual static JNI on SVM, JNI on JVM,
and a control loop. Each matched pair calls the same native function. Normal and
leaf declarations have separate symbols containing the same arithmetic. Static
JNI wrappers both check pending exceptions and delegate to the normal function.
Typed critical bindings use build-time unbound adapters; separately reported
runtime-bound critical factories compare against equivalent manual bound handles.
Buffer/callback correctness and LTO optimization are checked by integration tests;
buffer, record, callback and resource timings remain gaps. BoltFFI is not required
for this primitive baseline and is not represented by invented estimates.

Every sample records current-thread Java heap allocation bytes using an active
allocation canary. The four direct SVM generated/manual normal/leaf cases enforce
exactly zero measured bytes after warmup. Other adapters report allocation rather
than assuming it is absent. Counters run outside the timed interval. This measures
the invoking thread's Java heap, not native malloc traffic or other threads.
The report includes the maximum sample B/op as well as the median, so rare
allocations are visible. Named loop classes make the retained SVM assembly easy
to map back to cases. The expanded workload is `scalar-recurrence-v3`; historical
v2 results cannot be used as a same-workload regression baseline.

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

CodSpeed's generator JMH wall series uses the PR #29 harness. Each case runs two forks with 15 one-second warmup and five measured iterations: shorter warmup left Graal JIT convergence inside the measured rounds and produced 10-17% swings between identical commits. The workflow shards the JMH cases across four jobs, which CodSpeed merges into one report. Separate native
wall and simulation jobs exercise inline, owned and borrowed value construction.
These use its pinned instrument-hooks submodule and fail if instrumentation was
requested but is absent. Native wall-time runs retain 30 raw measured rounds after
10 warmup rounds (100,000 operations each), and emit the pinned integration's wall-time
result schema. Hooks alone provide profiles, not wall-time result statistics. Cross-library comparison runs do not use those jobs.

## Sanitizer and coverage boundaries

The sanitizer wrappers and Linux amd64 instrumented-library add-ons come from
`elide-dev/toolchain` 2026.11.6. ASan and TSan each must detect a deliberate bug in
a separate process before a clean workload can pass. The ASan lane also requires
undefined-behavior and leak canaries and enforces fail-fast UBSan and LSan without
leak suppressions. UBSan includes `-fsanitize=integer,implicit-conversion`, with
truncation and unsigned-overflow canaries; `tests/fuzz/ubsan.ignorelist` exempts
only unsigned overflow in `myna_value_hash`, whose FNV-1a multiplication wraps by
definition. A separate RSS canary verifies the runner's process-memory
watchdog. The value fuzzer feeds input bytes into strings, number tokens and
borrowed regions; the C++ producer treats UTF-8 and number lexing as caller
contracts, so it must publish such bytes exactly. Fuzzer seed is 1729; seed
corpora are checked in under `tests/fuzz/corpus`. CI fuzzes for ten minutes with
`--duration 600`, then exits normally so leak checks and coverage reports run.
Local `mise run test:asan` retains its 20,000-input development workload.
Extend a campaign with
`MYNA_PYTHON_TIMEOUT=35m mise exec -- scripts/python.sh tests/sanitizers.py asan --runs 1000000 --campaign-timeout 1800`; replay
failures with `build/sanitizers/asan/fuzz-values path/to/crash-file`.

Fuzzing bounds input length (1KiB), individual allocations (64MiB), each input's
execution (5 seconds), total campaign execution (300 seconds by default;
duration-based campaigns get at least 30 seconds of shutdown grace; use
`--campaign-timeout 1800` for the million-input campaign above), and observed
process RSS (512MiB). The RSS canary retains an independent 30-second limit.
`MYNA_PYTHON_TIMEOUT` sets Elide's outer runtime cap when supplied; the long
command allows 35 minutes around the fuzz process's 30-minute cap. Without it,
the wrapper preserves Elide's default. Process logs stream to disk, with periodic
resource snapshots and final status retained even for watchdog failures.
Longer campaigns run separately from CI; for example, an eight-hour run is
`MYNA_PYTHON_TIMEOUT=9h mise exec -- scripts/python.sh tests/sanitizers.py asan --duration 28800`.
Duration and input-count limits are mutually exclusive, and the watchdog must
exceed a requested duration so a successful campaign can finish its leak checks.

The Rust reader (`myna_tape_text_v1`) and UTF-8 bridge take untrusted node,
region and byte tables. `crates/myna-values/tests/support/tape_model.rs` decodes
arbitrary bytes into such tables and checks every result against a safe reference
model; `cargo test` runs it as proptests. `mise run test:rust-fuzz` runs the same
harness under cargo-fuzz with libFuzzer and ASan (`scripts/rust-fuzz.sh [seconds]`,
60 per target by default, five minutes in CI) after an expected-failure ASan canary,
and `mise run test:miri` runs the value tests under Miri. Both use the project's
pinned nightly Rust from `mise.toml`; their binaries never enter Myna's LLVM
bitcode. `mise run test` runs both lanes: the first run adds the `miri` and
`rust-src` components and builds cargo-fuzz into `build/tools`, and together they
take a few minutes. Set `MYNA_SKIP_MIRI_FUZZ=1` to skip them locally; CI always
runs them. Miri cannot interpret simdutf's C++,
so tests that must reach it are ignored under `cfg(miri)`. Replay a Rust crash with
`mise exec -- build/tools/cargo-fuzz/bin/cargo-fuzz fuzz run --fuzz-dir crates/myna-values/fuzz TARGET FILE`.
The RSS watcher polls
the process; it is a test termination limit, not an exact allocator budget.
The value workload separately enforces actual live PMR allocation budgets and
injects failure after successful allocations, checks poisoned-builder recovery,
and requires balanced release. Rust tests fail each allocation in document and
builder construction in turn, then verify recovery and exactly balanced release.
Runtime flags, compiler-rt hashes, source hashes,
exact commands and RSS observations are retained under `build/sanitizers/`.
Pinned libFuzzer's own RSS watcher leaks its detached thread state on macOS;
the harness disables that watcher and supplies an external one, preserving exit
leak detection. Merely accepting an RSS option does not establish enforcement:
the pinned macOS runtime's `hard_rss_limit_mb` failed the allocation canary.

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

### Paired performance gates

Comparisons between runs stay advisory, but within one run, every generated binding must keep
pace with its matched handwritten or direct call. Run `benchmarks/gate.py` on the controlled x86-64
benchmark host after each floor, comparison and cells run, before results are checked in:
`python3 benchmarks/gate.py comparison build/comparison/results.json` (or `floor`, `cells`). Each
pair calls the same native function under the same transition contract, in fresh processes
interleaved on one host, so host speed and drift cancel out. A pair fails when the generated case's
median of process medians exceeds its reference's by more than 2% (`--tolerance`), and the cases
also separate: with each side's fastest and slowest process set aside, every remaining generated
process is slower. A single outlier process can neither fail the gate nor hide a gap. The hosted
CI benchmark workflows stay advisory and do not run the gate.

Each reference has the same structure as the generated code it gates. A generated library
binding is an object that owns its library's symbol segments, so its reference is a handwritten
object holding the segment in a final field (`manual-instance-library-ffm`,
`manual-instance-critical-ffm`); reaching an instance costs one dependent load more than a static
handle. The generated static entries are gated against static handwritten handles.

A pair with a known, explained gap carries its own allowance and reason in `gate.py`. The
allowance keeps that gap from growing; lower it when the gap is fixed. Only the two create-cell
pairs in the cells gate carry one: a create cell allocates an owner and the runtime's struct that
the direct call does not.

The gate exists because the generated typed critical AOT FFM call was 15% slower than the
handwritten one, and nothing failed. The generated library binding held each symbol address in an
`Optional`, and in Native Image every call paid for an extra dependent load and a type check. The
same investigation found the static AOT FFM entry 3.3% behind: every call checked two
run-time-initialized classes and dispatched on the lookup segment's scope. The generator now
initializes the descriptor-only outer class at build time and passes the `loaderLookup` address as
`MemorySegment.ofAddress`, whose global scope is known when compiling. The static entry now beats
the handwritten unbound handle (4.82 against 5.56 ns/call on Linux x86-64). Static entries for
native leaves are critical downcalls (0.964 against 1.392 ns/call for a handwritten critical
handle), and FFM Truffle cells over leaf imports, which call them, dropped from 5.83 to 1.60
ns/call, level with native cells.

ThinLTO and full LTO tests retain before/after optimized C and Rust IR, final
linker IR, disassembly and artifact/tool hashes under
`build/reports/thinlto/<target>/` and `build/reports/fulllto/<target>/`.
These tests prove load elimination across the opaque call without pretending that
LLVM can inline Java Native Image machine code.


The `Boundary comparisons` workflow runs the standalone harness without CodSpeed
instrumentation or performance-series uploads. It compiles the generator from
current Java sources with pinned `javac`. Build/run/report commands remain usable
locally. Historical local before/after data is checked in under
`benchmarks/comparison/baselines`; it is advisory because this shared workstation
experienced substantial concurrent load.

The [adapter scalar baseline](../benchmarks/comparison/baselines/adapters-macos-arm64.md)
records five fresh processes for direct, normal/critical FFM, explicit library
bindings and static JNI, including Java heap allocation counts. Matching
[assembly evidence](../benchmarks/comparison/evidence/README.md) records which
generated and handwritten paths are instruction-equivalent. These scalar results
do not establish buffer, record, callback or resource-lifecycle performance.

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


## Performance charts

The README charts come from one November 10, 2026 run on a Linux x86-64 workstation; [the evidence record](../benchmarks/comparison/evidence/readme-2026-11-10/README.md) lists their inputs, provenance and the commands for all three. The earlier v0.7.2 hosted ARM comparison, `benchmarks/comparison/baselines/hosted-arm64-0.7.2.json`, stays checked in with its raw process samples and source hashes verified against release commit `7401c446ef9ad80d7bd14a9bf92f360a04956b80`. Source/build identities are retained rather than rewritten to pretend the data came from a later commit.

```sh
python3 -m venv build/charts-venv
build/charts-venv/bin/pip install --require-hashes -r benchmarks/comparison/requirements.txt
build/charts-venv/bin/python benchmarks/comparison/chart.py benchmarks/comparison/baselines/scalar-2026-11-10-linux-x86_64.json \
  --host 'Threadripper PRO 9965WX · Linux x86-64' --date 2026-11-10
python3 scripts/check-hygiene.py
```

The plot separates normal transitions, native leaves, and AOT FFM. Bars show medians of process medians on a shared linear axis starting at zero; whiskers show process min–max. The runtime-bound FFM diagnostic and JVM comparisons remain in the full results table rather than the README chart. Shared-host measurements remain advisory. SVG and PNG exports plus a manifest of input/output hashes live under `docs/charts/`; changing a baseline, chart script, or dependency lock requires regenerating them. CI verifies the manifest and renders fresh artifacts without inventing a performance threshold.

To update the baseline, run the complete comparison workflow, verify workload correctness and hardware/toolchain provenance, retain its JSON, and regenerate its table and charts in the same PR. `compare.py` rejects mismatched profiles and incomplete CPU identities. Refresh `requirements.txt` from `requirements.in` with `uv pip compile --generate-hashes --output-file benchmarks/comparison/requirements.txt benchmarks/comparison/requirements.in` when upgrading plotting dependencies.

## Truffle cell costs

`mise run bench:cells-build` and `mise run bench:cells` measure generated Truffle cells from
runtime-compiled code on a Native Image: direct Java calls to the Native Image and AOT FFM bindings
against the same cells through consumer glue and through the generic interop wrapper, Truffle NFI
as the dynamic-FFI baseline, string borrows, and native, pinned and copied byte borrows by size.
Each case is a fresh process whose single compiled root must observe compiled code and print its
compilation trace; samples carry the current-thread allocation count. The
[harness contract](../benchmarks/cells/README.md) and the checked-in advisory
[Linux amd64 baseline](../benchmarks/cells/baselines/wsl2-linux-amd64-2026-11-07.md) describe the
cases; [Truffle native cells](truffle-cells.md#measurement) summarizes the results. Runtime-compiled
code is not in the image disassembly, so the inlining evidence is parity with the direct call plus
zero allocation, not assembly.

## Native call floor

Run `mise run bench:floor-build` and `mise run bench:floor` for matched native/SVM identity and arithmetic workloads. The [harness contract](../benchmarks/floor/README.md) specifies the exact calls, controls, unrolling, bounded batches, independent checksum oracle, and concurrent-GC test. The dedicated Native call floor workflow retains build commands, source/binary hashes, CPU identity, full assembly, sample logs and summaries. It is also manually dispatchable; the same controlled ARM runner variable applies.

The checked-in Linux and Mac default-leaf baselines are separate experiments, not cross-host speedup comparisons. Use five or more processes and the default 16,777,216 calls/sample when refreshing them. Keep the slower cases, including nonmonotonic batch sizes, in the full report.

The README's leaf chart compares binding ABI 2 defaults with handwritten SVM leaves and native function-pointer callers explicitly unrolled four times. Custom pointers and explicitly unrolled SVM variants remain in the full table. Regenerate it with:

```sh
build/charts-venv/bin/python benchmarks/comparison/abi2-chart.py
python3 scripts/check-hygiene.py
```

`benchmarks/comparison/inspect-floor.py ARTIFACT_DIRECTORY OUTPUT_PREFIX` extracts the ordinary leaf stubs, native baselines and SVM loops from the full workflow assembly, preserving hashes and build metadata. It fails if a standalone custom pointer adapter remains or a binding ABI 2 build retains generated leaf wrappers/invoke stubs. Historical prototype data and its `floor-chart.py` renderer remain separate. All chart renderers are covered by input/output manifests and rerendered in the hygiene workflow. Do not silently replace an identity result with an arithmetic result or subtract independently timed controls to claim pure crossing cost.
