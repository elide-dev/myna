# README call-floor rerun · November 7, 2026

Both hosts run the complete uninstrumented `benchmarks/floor/run.py` workload: five fresh processes per case, ten retained samples per process, seeded case order, and 16,777,216 calls per sample. The oracle validates results; stress checks require observed GC. Each host compares generated SVM leaves, handwritten SVM leaves, and C callers against the same separately compiled kernel object without LTO. Normal-transition measurements remain separate from leaf comparisons.

- [Linux samples, compiler settings, source/binary hashes, and hardware](../../baselines/default-leaf-2026-11-07-linux-arm64.json) · [report](../../baselines/default-leaf-2026-11-07-linux-arm64.md) · [successful workflow and complete artifacts](https://github.com/elide-dev/myna/actions/runs/42391576810)
- [Mac samples, compiler settings, source/binary hashes, and hardware](../../baselines/default-leaf-2026-11-07-macos-arm64.json) · [report](../../baselines/default-leaf-2026-11-07-macos-arm64.md)
- [Linux selected instructions](linux-arm64.asm) · [inspection metadata](linux-arm64.json)
- [Mac selected instructions](macos-arm64.asm) · [inspection metadata](macos-arm64.json)

Full Linux disassemblies are retained in the linked workflow artifact; full Mac disassemblies remain in `build/floor/native.asm` and `build/floor/svm.asm` from the measured build. Compressed local copies of both platforms are retained under `build/readme-full-disassembly/`. Their SHA-256 digests match `full_disassembly_sha256` in the inspection metadata. These are machine instructions, not LLVM IR. Inspection confirms that generated ABI 2 leaf adapters have no standalone invoke stub; the handwritten leaf stub and caller loops are retained for comparison.

The runs use identical hashes for all 40 measured source inputs. The Mac build records revision `83229a2` with an in-progress README/workflow working tree; its complete measured-source hashes are retained. Linux builds revision `2812eaf`. Neither result is relabeled as a release artifact or given release provenance.

The chart displays the median of per-process medians and process min–max ranges. Myna and native C pointer ranges overlap on Mac, so this run does not establish a performance win over native C there. The Linux generated-call range is above the native pointer floor. Shared-host and local timing variations do not establish improvements over earlier runs. Native pointer callers shown in the chart are explicitly unrolled four times; no control is subtracted.

Rerun with `mise run bench:floor-build` and `mise run bench:floor`, then inspect instructions with `python3 benchmarks/comparison/inspect-floor.py build/floor build/floor/callsite-evidence`. Regenerate the chart using the hash-locked plotting dependencies and `python3 benchmarks/comparison/abi2-chart.py`.
