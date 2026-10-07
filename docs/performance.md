# Native Image first: build-time specialization

Substrate VM is the primary execution target. The generator should resolve ABI layouts, symbol identities, Java targets, exception adapters, and supported call policies before image compilation. FFM and JNI are optional projections of that model; the direct Native Image path does not depend on their invocation machinery.

## Selected execution paths

| Edge | Preferred lowering | Optimization boundary |
| --- | --- | --- |
| Native Image → known Rust symbol | Generated `@CFunction` with primitive/word carriers and direct linker symbol | Graal optimizes the Java caller; LLVM optimizes the Rust implementation. |
| Native Image → proven short native leaf | The same binding with `Transition.NO_TRANSITION` | Removes the VM transition when the explicit leaf contract permits it. |
| Rust → known Native Image target | Generated `@CEntryPoint` invoking a fixed Java method | Graal sees the adapter and Java target together; LLVM sees contracts on the real Rust callsite. |
| Native Image → runtime-selected function through FFM | Typed `invokeExact`, constant descriptors, generated hosted downcall registration | Image builder generates ABI stubs ahead of time; addresses bind at runtime. |
| Future known FFM callback | Register the specific static Java target for a specialized direct upcall | Prefer target-specific generated code over descriptor-only generic dispatch. |
| Future JNI compatibility | Explicit JNI adapter delegating to the raw seam | Adapter effects and JNI lifecycle remain separate from implementation contracts. |

No libffi-based dispatcher is needed for the fixed C API path. Build-time knowledge removes runtime ABI discovery, reflection, argument arrays, boxing, and symbol lookup from that path. Generated entry methods use direct static Java calls, allowing Graal to optimize and inline eligible Java adapter code. Necessary isolate entry, exception conversion, and GC transitions remain explicit.

## Explicit leaf calls

The implemented DSL opt-in is:

```text
import inspect symbol=seam_inspect return=i64 error=no_failure native_leaf=true@explicit_contract
  param value type=ptr<i64> nullable=false ownership=borrowed access=read
end
```

`native_leaf` promises that **every execution is short and bounded, cannot block, cannot call back into Java, and obeys the Native Image no-transition restrictions**. It applies to native imports only. The generator emits `@CFunction(value = "seam_inspect", transition = CFunction.Transition.NO_TRANSITION)`. The owned example performs a single off-heap integer read and exercises this path in the real Native Image/Rust round-trip test.

Ordinary imports retain the default transition. Neither `nounwind`, `willreturn`, nor read-only pointer access implies a leaf call. Unapproved leaf facts fail strict validation; relaxed generation preserves provenance but does not elide the transition. Leaf policy participates in the ABI fingerprint and JSON, but is not emitted as an LLVM attribute. It does not imply LLVM `nounwind` or `nofree`.

The same model can later drive FFM `critical` options, but that mapping is not enabled in this version. Heap access, pointer escape, safepoint behavior, and callbacks need separate policy checks. Exports retain normal isolate entry; removing the entry prologue merely because a caller is known would not establish valid thread state.

See the [Native Image transition contract](https://www.graalvm.org/sdk/javadoc/org/graalvm/nativeimage/c/function/CFunction.Transition.html). Long-running no-transition calls can delay safepoints and GC, so leaf eligibility is an implementation obligation, not a name-based heuristic.

## Real cross-language optimization

The implemented LLVM applicator rewrites matching definitions, declarations, and direct calls in compiler-produced objects before ThinLTO indexing. It regenerates summaries and hashes, so the optimizer sees the contracts in the actual modules it consumes. The tests demonstrate an optimization across an opaque foreign call in both Rust and C callers. See [ThinLTO integration](thinlto.md).

The default Native Image backend emits machine code. Adding attributes to LLVM callers cannot let LLVM inline that machine-code body, and the hosted Feature does not inject these facts into Graal compiler graphs. Full body-level Java/Rust LTO would require a compatible Native Image LLVM pipeline producing genuine implementation bitcode, preserving VM state/GC semantics, and accepting the contracts before optimization. This is a distinct compiler integration, not a wrapper trick. Native Image's LLVM backend currently does not support FFM.

Generate LLVM-importable wrappers only when they perform useful work: ABI adaptation, constant-argument specialization, buffer validation, status conversion, or batching. Apply contracts to the concrete lowered signatures and complete effects of those wrappers. A wrapper that merely forwards the same call usually adds no information beyond direct callsite annotations. Moving loops or batches to one side of the seam is the route to eliminating repeated crossings when individual entry/exit transitions cannot be removed.

## Build-time FFM support

The optional generated `MynaForeignFeature.java` registers all downcall descriptors with `RuntimeForeignAccess` during hosted setup. Compile it alongside `MynaFFM.java`, then enable it with:

```text
--features=dev.elide.myna.generated.MynaForeignFeature
--enable-native-access=ALL-UNNAMED
```

Generated typed methods use per-symbol lazy, static-final handles and `invokeExact`. Load the native library with `System.load` in the binding's class loader before the first typed invocation. Handle holders are explicitly initialized at runtime: a build-host address must never be frozen into an executable. The existing factories accepting `SymbolLookup` remain available for explicit library lifetime management.

The integration suite compiles and runs this path as a Native Image executable calling Rust. Full callback/lifetime modeling is still future work. Graal's [FFM documentation](https://www.graalvm.org/latest/reference-manual/native-image/native-code-interoperability/ffm-api/) explains AOT registration and specialization for known upcall targets.

## Ownership and off-heap access

The [ownership API](ownership.md) keeps Native Image cleanup policy in consumer code. Each resource type supplies one reusable native destructor callback. Off-heap buffer calls use consumer acquire/release hooks and a direct C API call without generated per-call allocation, copies, or monitors. Rust lifetime markers have no runtime bookkeeping. These are structural properties, not latency benchmark results; consumer GC registration and retention costs remain part of the integration.

## Performance acceptance

The current evidence is executable ABI correctness and inspected LLVM optimization results, not measured minimum latency. Future performance gates should compare direct C/Rust baselines, normal and leaf `@CFunction`, `@CEntryPoint`, and AOT FFM on each supported architecture. Measure steady-state cycles per crossing, allocation, generated assembly, batch scaling, and safepoint latency under concurrent GC. Keep startup/library binding separate from repeated invocation, consume results to prevent elimination, and retain disassembly with benchmark reports. JVM benchmarks are secondary compatibility checks.
