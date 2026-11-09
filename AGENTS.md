# Working on Myna

SVM is the primary runtime and performance target. Generated direct C bindings should meet or beat equivalent handwritten Native Image `@CFunction` calls. Binding ABI 2 defaults imports to short, bounded, nonblocking, non-reentrant leaves; require native_leaf=false@explicit_contract for other calls. Compare normal transitions and default native leaves separately, against the same native function and build settings. JVM measurements are secondary. Inspect optimized instructions as well as repeated timings; noisy intervals do not establish a performance win.

Use the pinned tools through `mise`: Elide 1.6.2, elide-dev/toolchain 2026.11.5, and the checksum-verified elide-dev/graal 25.5.5-elide.28 distribution. Native builds use the external GraalVM driver. Keep Rust bitcode compatible with the pinned LLVM reader and linker. Run Elide builds/tests sequentially within one checkout because they share incremental output directories.

Keep AOT FFM ABI adapters initialized at image build time and native symbol addresses initialized at runtime. Never embed process addresses in the image heap. Preserve the explicit SymbolLookup factory and test real Native Image execution for carrier, record, pointer and ownership changes.

`myna-truffle` (src/truffle) uses only the public Truffle API plus the GraalVM SDK, built with the Truffle DSL processor; generated cells must stay DSL-free and must never change a binding's leaf status or embed addresses. Everything on a cell's compiled path must survive partial evaluation on Substrate: cell and runtime classes initialize at build time (generated `native-image.properties`), native memory access goes through `Memory` (word operations in an image, never `MemorySegment` accessors), and `BigInteger`, string building and other slow paths sit behind `@TruffleBoundary`. `tests/truffle_integration.py` proves cells run from runtime-compiled code; `benchmarks/cells` measures them against direct calls and NFI, with a checked-in advisory baseline. Cells over transitioning imports call the AOT stub directly (`truffle_transitions=direct`, the default), which needs the pinned fork's elide-dev/graal#196; `boundary` remains for consumers on older forks.

Native value ABI v3 is producer-neutral. Preserve allocator ownership, borrowed-region lifetimes, validated UTF-8, exact number tokens and poisoned-builder recovery contracts. Do not add implicit JSON parsing or object materialization to direct traversal. C++ simdjson integration belongs in downstream adapters.

Use `mise run test` and `mise run lint` for the full gates; use focused tasks while developing. Coverage denominators contain production code. Native-only Java readers need SVM integration tests. Sanitizer workloads must run their expected-failure canaries. Retain ThinLTO IR and disassembly evidence; Native Image machine code is not LLVM IR.

CodSpeed tracks Myna's own simulation and wall-time workloads. Keep cross-library comparisons uninstrumented and rerunnable, with raw samples, source/binary hashes, compiler settings and hardware metadata. See `docs/benchmarking.md` for commands and limits.

Extract matching symbol sidecars before stripping release executables. macOS Native Image currently supplies method symbols, not source-level DWARF; do not describe its dSYM as complete source debugging. Keep symbols tied to the executable UUID/build ID.

Regenerate golden seam outputs together when changing the generator version: versioned canonical descriptors affect their fingerprints. Preserve unrelated work in downstream checkouts, including TruffleData.

Myna is dual-licensed under Apache-2.0 OR MIT. Preserve both license texts in packaged artifacts and keep crate copies synchronized. Run `python3 scripts/check-hygiene.py` for licensing, local documentation links, and performance-chart manifests. Regenerate charts from retained raw measurements with the hash-locked plotting dependencies; do not hand-edit plotted values.

Keep CodeQL disabled unless explicitly requested. Scorecard reports are evidence to review, not a reason to broaden credentials or weaken repository rules. Changes to rulesets, CODEOWNERS, release provenance, or symbol publication belong in reviewed PRs. Attest only artifacts actually produced by the identified build workflow; do not retroactively claim CI provenance for historical local builds. Preserve published artifact bytes, checksums, and version tags.
