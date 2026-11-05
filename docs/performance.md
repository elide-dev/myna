# Native Image first: build-time specialization

Substrate VM is the primary execution target. The generator should resolve ABI layouts, symbol identities, Java targets, exception adapters, and supported call policies before image compilation. FFM and JNI are optional projections of that model; the direct Native Image path does not depend on their invocation machinery.

## Selected execution paths

| Edge | Preferred lowering | Optimization boundary |
| --- | --- | --- |
| Native Image → known Rust symbol | Generated inlineable leaf pointer with primitive/word carriers and linker symbol | Graal optimizes the Java caller; LLVM optimizes the Rust implementation. |
| Native Image → proven short native leaf | Default binding ABI 2 leaf | Removes the adapter stub and VM transition under the default leaf contract. |
| Rust → known Native Image target | Generated `@CEntryPoint` invoking a fixed Java method | Graal sees the adapter and Java target together; LLVM sees contracts on the real Rust callsite. |
| Native Image → runtime-selected function through FFM | Typed `invokeExact`, build-time unbound adapters, runtime symbol addresses, hosted downcall registration | Image builder generates ABI stubs ahead of time; addresses bind at runtime. |
| Future known FFM callback | Register the specific static Java target for a specialized direct upcall | Prefer target-specific generated code over descriptor-only generic dispatch. |
| Future JNI compatibility | Explicit JNI adapter delegating to the raw seam | Adapter effects and JNI lifecycle remain separate from implementation contracts. |

No libffi-based dispatcher is needed for the fixed C API path. Build-time knowledge removes runtime ABI discovery, reflection, argument arrays, boxing, and symbol lookup from that path. Generated entry methods use direct static Java calls, allowing Graal to optimize and inline eligible Java adapter code. Necessary isolate entry, exception conversion, and GC transitions remain explicit.

## Default leaf calls (binding ABI 2)

Myna 0.8.0 makes short native leaves the default for every import:

```text
import inspect symbol=seam_inspect return=i64 error=no_failure
  param value type=ptr<i64> nullable=false ownership=borrowed access=read
end
```

Every execution must be **short and bounded, nonblocking, and unable to reenter Java**. Consumers must explicitly opt out for allocating, blocking, callback-capable, or potentially long-running operations:

```text
import compress symbol=compress return=i32 error=no_failure native_leaf=false@explicit_contract
end
```

The default generated method calls a typed `@InvokeCFunctionPointer(NO_TRANSITION)` interface with the pinned Graal compiler's `@AlwaysInline` and `@Uninterruptible(mayBeInlined=true)` annotations. Graal can inline the adapter into an interruptible caller while retaining the caller's safepoints. Explicit opt-outs use ordinary transitioning `@CFunction` methods. Exports retain normal isolate entry. No compiler safety checks are disabled.

Each leaf has a symbolic linker reference. Ordinary calls use Graal's relocated-symbol intrinsic directly, avoiding runtime holder-initialization guards in the hot path. A separate lazy runtime holder serves the cached pointer accessor. Generated `META-INF/native-image` configuration initializes symbolic metadata at image build time and resolves addresses at runtime. It must accompany the compiled classes. Only reached imports require their symbols; static libraries work through the existing `c_context` / `c_library` configuration. Linked libraries must remain available throughout calls. The generated `nameLeafPointer()` accessor exposes the cached typed pointer for specialized callers: resolve it before entering an uninterruptible region, because first-use initialization itself is interruptible.

This backend deliberately uses internal APIs of the pinned **elide-dev/graal 25.5.5-elide.24** compiler. Compile against its `svm-shared.jar`, `svm-guest-staging.jar`, and `svm-guest.jar` without bundling them or putting them on the Native Image application classpath. [The compiler helper](../scripts/compile-native-bindings.py) handles the compile-only classpath and copies generated metadata. Retest compiler upgrades. See [ABI 2 migration](binding-abi-2.md) for complete commands.

Leaf policy participates in canonical JSON and the ABI fingerprint. It never implies LLVM `nounwind`, `nofree`, or other effects. Unapproved explicit leaf facts fail strict validation; relaxed generation retains their provenance and conservatively uses a normal transition. FFM does not yet map this contract to `critical` downcalls.

## Call-floor experiments

The [floor harness](../benchmarks/floor/README.md) compares identical linked kernel objects through default generated leaves, handwritten `@CFunction` leaves, custom inlineable pointers, native direct/indirect calls, explicit caller unrolling, and fixed C/Java batches. It separates identity-call cost from arithmetic dependency-chain latency. Java batches receive an already-resolved typed pointer, contain at most 256 calls, and must satisfy the bounded-latency contract as a whole; larger batches are not uniformly faster. Concurrent allocation/GC checks exercise both generated leaves and specialized callers.

The [custom pointer example](../examples/leaf-pointer/README.md) remains useful for runtime-selected targets. Fixed symbols need no handwritten pointer interface or resolver with binding ABI 2.

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

The retained evidence includes executable ABI correctness, inspected LLVM/native code, and measured scalar call floors on Linux ARM64 and macOS ARM64. It does not establish a universal minimum for every ABI or workload. Performance gates should compare direct C/Rust baselines, normal and leaf `@CFunction`, `@CEntryPoint`, and AOT FFM on each supported architecture. Measure steady-state cycles per crossing, allocation, generated assembly, batch scaling, and safepoint latency under concurrent GC. Keep startup/library binding separate from repeated invocation, consume results to prevent elimination, and retain disassembly with benchmark reports. JVM benchmarks are secondary compatibility checks.

See [repeatable benchmarks and comparisons](benchmarking.md) for the measurement strategy.

Generated FFM convenience calls split the unbound MethodHandle adapter from its
runtime symbol binding. The generated Native Image feature initializes the adapter
at build time and the address holder at runtime. This allows AOT specialization of
the adapter while keeping native library loading and address ownership at runtime.
The explicit `SymbolLookup` factory retains its original independently bound-handle
semantics; benchmarks distinguish that path from the optimized convenience call.
