# Native Image C call inspection

On the pinned Elide GraalVM 25.5.5-elide.24 macOS ARM64 build, generated and handwritten normal-transition scalar loops each contain 72 instructions and match after normalizing relocation addresses. Their leaf stubs each contain eight matching instructions, including the same native target lookup and call. This establishes no extra generated-path instructions for this workload; it does not prove parity for every ABI shape or portable absolute timing.

`arm64-inspection.json` records the input build, sources, binary hashes and inspection command. `normal-a-arm64.asm` and `normal-b-arm64.asm` retain the normal-transition loop disassembly. The inspected classes are from the recorded pre-release build; the later version bump changes the generated fingerprint constant, not these call declarations.

Build with `-H:-DeleteLocalSymbols` to retain method names for `llvm-objdump --disassemble`. Native Image's `-g` is unsupported on macOS. Preserve sidecars before stripping shipping binaries. The unstripped inspection executable is a separate artifact from the saved timing runs.
