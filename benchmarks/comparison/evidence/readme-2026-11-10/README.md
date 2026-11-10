# README benchmark rerun · November 10, 2026

All three README charts come from one host, **unclemax**: Linux x86-64, AMD Ryzen Threadripper PRO 9965WX, `performance` CPU governor ([environment](unclemax-environment.json)). The runs used an isolated clone at `/tmp/myna-readme-2026-11-10.aHcRCQ/source`, checked out at revision `c8802c5` (release 0.16.0); no existing checkout on the host was modified, and `git status --porcelain` was empty after the last run. The host's mise (2026.8.14) is older than the project's minimum, so the runs used a separately downloaded mise 2026.11.5 binary, verified against its published SHA-256 checksum, with the user's global mise configuration replaced by an empty file. `mise install --locked` and `mise run setup` installed Elide 1.6.2, toolchain 2026.11.6 (clang 23.1.4) and checksum-verified GraalVM 25.5.5-elide.28.

The three workloads ran sequentially, with no other significant load observed on the host:

```sh
mise run bench:floor-build && mise run bench:floor
mise run bench:comparison-build && mise run bench:comparison
mise run build:jvm && mise run bench:cells-build && mise run bench:cells
```

## Chart inputs

- **Native call floor** (`docs/charts/default-leaf.svg`): [raw samples and build metadata](../../baselines/default-leaf-2026-11-10-linux-x86_64.json) · [report](../../baselines/default-leaf-2026-11-10-linux-x86_64.md) · [selected instructions](linux-x86_64.asm) · [inspection metadata](linux-x86_64.json). Five processes per case, ten warmup and ten retained samples of 16,777,216 calls; the oracle validated every checksum, and the GC stress check passed.
- **Scalar boundary comparison** (`docs/charts/svm-boundaries.svg`): [raw samples](../../baselines/scalar-2026-11-10-linux-x86_64.json) · [report](../../baselines/scalar-2026-11-10-linux-x86_64.md). Workload `scalar-recurrence-v3`, five processes per case, 1,000,000 calls per sample. The four direct SVM normal/leaf cases enforced zero measured Java heap bytes.
- **Truffle native cells** (`docs/charts/truffle-cells.svg`): [raw samples](../../../cells/baselines/unclemax-linux-x86_64-2026-11-10.json) · [report](../../../cells/baselines/unclemax-linux-x86_64-2026-11-10.md). Every measured sample ran in runtime-compiled code.

All three build records verify byte-for-byte against `c8802c5`'s sources, with one exception: the scalar comparison's source manifest also hashes `chart.py` and `abi2-chart.py`, which were rewritten after the run to produce these charts. Neither file takes part in building or measuring.

## Machine code

`python3 benchmarks/comparison/inspect-floor.py build/floor build/floor/callsite-evidence` passed on the host: the binding ABI 2 generated leaf has no standalone invoke stub, and handwritten leaf stubs and caller loops are retained for comparison. Apart from relocated addresses, the selected instructions match the [November 7 Linux x86-64 record](../readme-2026-11-07/linux-x86_64.asm). The full disassemblies stay in the host's isolated `build/floor/`; their SHA-256 digests are recorded in the inspection metadata. These are machine instructions, not LLVM IR.

## Reading the charts

Bars and dots show the median of five per-process medians; whiskers span the minimum and maximum of those medians. They are descriptive ranges, not confidence intervals. Timings include the callee and the loop, with no control subtracted. Compare implementations within one chart and one pair; these numbers are not comparable with other hosts or architectures.

- The floor chart's native C pointer series is unrolled four times, so its loop structure differs from the SVM series.
- In the scalar comparison, generated native leaves (0.844 ns/call) beat handwritten `@CFunction(NO_TRANSITION)` (1.545 ns/call); normal transitions and static JNI are at parity. Generated static AOT FFM (5.738 ns/call) is 3.3% slower than handwritten (5.552); the ranges overlap only because of one handwritten process at 5.948. The machine code shows two class-initialization checks inside the generated loop, for run-time-initialized holder classes. Generated typed critical AOT FFM (1.601 ns/call, range 1.595–1.646) is slower than the handwritten unbound critical handle (1.393, range 1.393–1.397). The chart shows both gaps as measured.
- The critical gap's cause is in the generated `CriticalLibraryBinding`: it held the symbol address in an `Optional<MemorySegment>`, so every call loaded the `Optional`, loaded its value, null-checked it and checked its class before the downcall. The generator now emits a nullable `MemorySegment` field. A rerun on the same host, at `c8802c5` with only that generator change applied (not a commit, so not a baseline), measured 1.414 ns/call (1.412–1.416) against 1.394 (1.393–1.397), and its disassembly has no class check left in the loop. The generated non-critical library binding was unchanged (14.251 against 14.071 ns/call).
- A second rerun added two static-entry changes: the descriptor-only outer FFM class initializes at build time, and each static entry keeps its `loaderLookup` address as a `long` and passes `MemorySegment.ofAddress`. The global scope of that segment is known when compiling, so the scope dispatch and liveness check fold away, and one run-time class-initialization check remains per call. The generated method shrank from 279 to 86 instructions; the handwritten one has 267. Static AOT FFM measured 4.866 ns/call (4.811–4.992) against 5.575 (5.553–5.957) handwritten. Like the first rerun, this was an uncommitted change on `c8802c5` and is not a baseline.
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
