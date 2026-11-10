# README benchmark rerun · November 10, 2026

All three README charts come from one host, **unclemax**: Linux x86-64, AMD Ryzen Threadripper PRO 9965WX, `performance` CPU governor ([environment](unclemax-environment.json)). The runs used an isolated clone at `/tmp/myna-pr-rebased.nnaqk7/source`, checked out at revision `0e81866` (tree `32db8a8e5605`; the pull request's commits were later reworded and squashed, so the tree, not the commit, is the stable identity); no existing checkout on the host was modified, and `git status --porcelain` was empty after the last run. The host's mise (2026.8.14) is older than the project's minimum, so the runs used a separately downloaded mise 2026.11.5 binary, verified against its published SHA-256 checksum, with the user's global mise configuration replaced by an empty file. `mise install --locked` and `mise run setup` installed Elide 1.6.2, toolchain 2026.11.6 (clang 23.1.4) and checksum-verified GraalVM 25.5.5-elide.28.

The three workloads ran sequentially, with no other significant load observed on the host:

```sh
mise run bench:floor-build && python3 benchmarks/comparison/inspect-floor.py build/floor build/floor/callsite-evidence
mise run bench:floor
mise run bench:comparison-build && mise run bench:comparison
mise run build:jvm && mise run bench:cells-build && mise run bench:cells
python3 benchmarks/gate.py floor build/floor/results.json   # likewise comparison, cells
```

Every [paired performance gate](../../../../docs/benchmarking.md#paired-performance-gates) passed. After the measurements, `mise run test:e2e`, `test:native-image` and `test:truffle` passed in the same checkout.

## Chart inputs

- **Native call floor** (`docs/charts/default-leaf.svg`): [raw samples and build metadata](../../baselines/default-leaf-2026-11-10-linux-x86_64.json) · [report](../../baselines/default-leaf-2026-11-10-linux-x86_64.md) · [selected instructions](linux-x86_64.asm) · [inspection metadata](linux-x86_64.json). Five processes per case, ten warmup and ten retained samples of 16,777,216 calls; the oracle validated every checksum, and the GC stress check passed.
- **Scalar boundary comparison** (`docs/charts/svm-boundaries.svg`): [raw samples](../../baselines/scalar-2026-11-10-linux-x86_64.json) · [report](../../baselines/scalar-2026-11-10-linux-x86_64.md). Workload `scalar-recurrence-v3`, five processes per case, 1,000,000 calls per sample. The four direct SVM normal/leaf cases enforced zero measured Java heap bytes.
- **Truffle native cells** (`docs/charts/truffle-cells.svg`): [raw samples](../../../cells/baselines/unclemax-linux-x86_64-2026-11-10.json) · [report](../../../cells/baselines/unclemax-linux-x86_64-2026-11-10.md). Every measured sample ran in runtime-compiled code.

All three build records verify against `0e81866`'s sources, and each records an empty dirty state. The first floor measurement ran while another workload loaded the host (process ranges several times wider than usual), so `mise run bench:floor` was repeated on the same build once the one-minute load average fell below 1; the checked-in floor samples are that repeat.

## Machine code

`inspect-floor.py` passed: the binding ABI 2 generated leaf has no standalone invoke stub, and handwritten leaf stubs and caller loops are retained for comparison. Apart from relocated addresses and one immediate constant that differs between image builds, the 2,235 selected instructions match the [November 7 Linux x86-64 record](../readme-2026-11-07/linux-x86_64.asm). The full disassemblies stay in the host's isolated `build/`; their SHA-256 digests are recorded in the inspection metadata. These are machine instructions, not LLVM IR.

## Before: release 0.16.0

The same host measured release 0.16.0 (`c8802c5`) earlier the same day: [floor](../../baselines/default-leaf-0.16.0-linux-x86_64.json), [scalar comparison](../../baselines/scalar-0.16.0-linux-x86_64.json) and [cells](../../../cells/baselines/unclemax-linux-x86_64-0.16.0.json), with [reports](../../baselines/scalar-0.16.0-linux-x86_64.md). Two AOT FFM paths trailed their handwritten pairs there, and the disassembly of that image showed why:

- **Typed critical library binding, 1.601 against 1.393 ns/call.** `CriticalLibraryBinding` held each symbol address in an `Optional<MemorySegment>`. Every call loaded the `Optional`, loaded its value, null-checked it and checked its class before the downcall. Library bindings now hold a nullable `MemorySegment` and throw the same `UnsatisfiedLinkError` for a missing symbol.
- **Static FFM entry, 5.738 against 5.552 ns/call.** Each call checked two run-time-initialized classes inside the loop: the outer bindings class and the address holder. It also dispatched on the lookup segment's scope before the downcall. The descriptor-only outer class now initializes at build time. Each static entry keeps its `loaderLookup` address as a `long` and passes `MemorySegment.ofAddress`, whose global scope is known when compiling, so the scope dispatch and liveness check fold away. The generated loop shrank from 279 to 86 instructions; the handwritten loop has 267.

Static entries for binding ABI 2 leaves are now critical downcalls, the FFM counterpart of the direct Native Image leaf. Truffle FFM cells over leaf imports call those entries.

## Reading the charts

Bars and dots show the median of five per-process medians; whiskers span the minimum and maximum of those medians. They are descriptive ranges, not confidence intervals. Timings include the callee and loop, with no control subtraction. Compare implementations within one chart and one pair; these numbers are not comparable with other hosts or architectures.

- The floor chart's native C pointer series is unrolled four times, so its loop structure differs from the SVM series.
- The scalar comparison's critical pair compares Myna's static leaf entry with a handwritten critical handle over a static `loaderLookup` segment. Library bindings are compared in the report and gates with handwritten objects that hold the same segments in final fields; reaching an instance costs one dependent load more than a static.
- The ratio shown in a chart's header appears only when the two process ranges do not overlap.

## Regenerating the charts

The charts use the light theme and typefaces of elide.dev (Instrument Sans and JetBrains Mono). `benchmarks/comparison/brand.py` downloads the fonts from pinned upstream commits and checks them against `benchmarks/comparison/fonts.json`. SVG text is written as glyph outlines, so renderers that cannot load web fonts still draw the brand typefaces. Install the hash-locked plotting dependencies from `benchmarks/comparison/requirements.txt`, then run:

```sh
host='Threadripper PRO 9965WX · Linux x86-64'
python3 benchmarks/comparison/abi2-chart.py benchmarks/comparison/baselines/default-leaf-2026-11-10-linux-x86_64.json --host "$host" --date 2026-11-10
python3 benchmarks/comparison/chart.py benchmarks/comparison/baselines/scalar-2026-11-10-linux-x86_64.json --host "$host" --date 2026-11-10
python3 benchmarks/cells/chart.py benchmarks/cells/baselines/unclemax-linux-x86_64-2026-11-10.json --host "$host" --date 2026-11-10
```

Each script rewrites its `docs/charts/*.manifest.json`, which records the hashes of its inputs, scripts, fonts manifest, dependency lock and outputs; `scripts/check-hygiene.py` rejects stale charts. Renders are byte-reproducible on a given platform and font cache. None of these results is given release provenance.
