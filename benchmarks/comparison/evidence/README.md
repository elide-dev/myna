# Native Image C call inspection

On the pinned Elide GraalVM 25.5.5-elide.24 macOS ARM64 build, generated and handwritten normal-transition scalar loops each contain 72 instructions and match after normalizing relocation addresses. Their leaf stubs each contain eight matching instructions, including the same native target lookup and call. This establishes no extra generated-path instructions for this workload; it does not prove parity for every ABI shape or portable absolute timing.

`arm64-inspection.json` records the input build, sources, binary hashes and inspection command. `normal-a-arm64.asm` and `normal-b-arm64.asm` retain the normal-transition loop disassembly. The inspected classes are from the recorded pre-release build; the later version bump changes the generated fingerprint constant, not these call declarations.

Build with `-H:-DeleteLocalSymbols` to retain method names for `llvm-objdump --disassemble`. Native Image's `-g` is unsupported on macOS. Preserve sidecars before stripping shipping binaries. The unstripped inspection executable is a separate artifact from the saved timing runs.

## Native leaf floor

`leaf-floor-linux-arm64.asm` and `leaf-floor-macos-arm64.asm` retain the ordinary identity/recurrence leaf stubs, native direct/indirect caller variants, and SVM loops from the floor experiments. Each matching JSON file records the build, input/binary hashes, full assembly hash and selected instruction counts. The full Linux assembly is also retained in the linked workflow artifact in the baseline JSON.

The inlineable `example.leaf.LongLeaf.invoke` has no standalone adapter stub in either image. The ordinary generated leaf retains its frame/address-load/call/return stub. Java batches inline these ordinary stubs into bounded uninterruptible callers; the enclosing measurement loop remains interruptible. The retained SVM loops include native indirect calls and safepoint slow paths; elimination of the adapter does not mean the native call itself is eliminated.

The original normal-transition comparison above and these leaf-floor experiments use different builds and workloads. Do not equate their instruction counts or mix their wall timings. Extract fresh evidence with `benchmarks/comparison/inspect-floor.py` when updating a floor baseline.

## Binding ABI 2 default calls

`default-leaf-linux-arm64.asm` and `default-leaf-macos-arm64.asm` retain the new default's call sites and matched native/handwritten baselines. Matching JSON records preserve source and binary hashes. Generated `Identity.leaf`, `Generated.leaf`, and their typed pointer invoke methods have no standalone adapter stub. Handwritten `Floor.identity` / `Floor.leaf` retain their ordinary native stubs. Runtime address initialization remains separate from steady-state calls.

The floor workflow checks this structural property with `inspect-floor.py` as well as collecting timings. The generated default and cached-pointer integration test also requires observed collections during concurrent allocation/GC. These checks preserve the compiler's own uninterruptible validation; they do not prove arbitrary consumer implementations satisfy the leaf contract.
