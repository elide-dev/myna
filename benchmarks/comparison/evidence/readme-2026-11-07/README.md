# README call-floor rerun · November 7, 2026

Each host runs the complete uninstrumented `benchmarks/floor/run.py` workload: five fresh processes per case, ten retained samples per process, seeded case order, and 16,777,216 calls per sample. The oracle validates results; stress checks require observed GC. Within each host, generated SVM leaves, handwritten SVM leaves, and C callers use the same separately compiled kernel object without LTO. Normal-transition measurements remain separate from leaf comparisons.

## Chart inputs

- **unclemax:** Linux x86-64, AMD Ryzen Threadripper PRO 9965WX. [Raw samples and build metadata](../../baselines/default-leaf-2026-11-07-linux-x86_64.json) · [report](../../baselines/default-leaf-2026-11-07-linux-x86_64.md) · [selected instructions](linux-x86_64.asm) · [inspection metadata](linux-x86_64.json) · [environment](unclemax-environment.json)
- **Mac:** Apple M5 Max. [Raw samples and build metadata](../../baselines/default-leaf-2026-11-07-macos-arm64.json) · [report](../../baselines/default-leaf-2026-11-07-macos-arm64.md) · [selected instructions](macos-arm64.asm) · [inspection metadata](macos-arm64.json)

Both runs use identical hashes for all 40 measured source inputs at revision `89bcf2a`. The Mac checkout was clean during build. Unclemax used an isolated checkout at `/tmp/myna-readme-2026-11-07.GY7woO/source`; no existing project checkout was modified. Setup used mise 2026.10.25, toolchain 2026.11.5, and checksum-verified GraalVM 25.5.5-elide.24. Both Native Image compiler-fallback symlinks were synchronized to the pinned toolchain before these builds.

The raw result files retain hardware identity, commands, compiler versions, source/binary hashes, and all samples. Full Mac disassemblies remain in `build/floor/`; full unclemax disassemblies remain in its isolated `build/floor/`. Compressed copies are retained locally in `build/readme-full-disassembly/`. Their decompressed SHA-256 digests match `full_disassembly_sha256` in the inspection metadata. These are machine instructions, not LLVM IR. The selected-instruction files normalize trailing blank lines and have their own recorded hashes. Inspection confirms no standalone generated ABI 2 leaf invoke stub; handwritten leaf stubs and caller loops remain available for comparison.

## Reading the chart

The chart displays the median of per-process medians and the complete process min–max ranges. Myna and native C pointer ranges overlap on both hosts; these runs do not establish a performance win over native C. Unclemax's native-pointer range includes a slower process; no samples were dropped. Native pointer callers shown in the chart are explicitly unrolled four times. No control is subtracted. Compare implementations within each host; different CPU architectures and local/hosted timings cannot establish cross-machine performance improvements.

## Additional hosted baseline

The earlier Linux ARM64 rerun is preserved separately: [raw samples](../../baselines/default-leaf-2026-11-07-linux-arm64.json), [report](../../baselines/default-leaf-2026-11-07-linux-arm64.md), [selected instructions](linux-arm64.asm), [inspection metadata](linux-arm64.json), and [successful workflow with complete artifacts](https://github.com/elide-dev/myna/actions/runs/42391576810). The initial Mac rerun is retained in commit `89bcf2a`; the chart uses the subsequent rebuild after synchronizing the compiler-fallback symlink. No benchmark result is relabeled as a release artifact or given release provenance.

Rerun with `mise exec -- python3 scripts/setup-graalvm.py`, `mise run bench:floor-build`, and `mise run bench:floor`. Inspect instructions with `python3 benchmarks/comparison/inspect-floor.py build/floor build/floor/callsite-evidence`. Regenerate the chart using the hash-locked plotting dependencies and `python3 benchmarks/comparison/abi2-chart.py`.
