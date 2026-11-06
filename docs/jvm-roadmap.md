# Optional adapters: FFM and Static JNI

Substrate VM remains the primary target. JVM support uses Java 25 FFM (Panama); static JNI targets Substrate VM. These adapters do not change the direct C API or its default native-leaf policy. See the [Native Image performance design](performance.md).

The canonical seam remains producer-neutral. FFM descriptors project the C ABI; `JniAbi` separately computes the concrete JNI carriers, implicit arguments, descriptors and mangled symbols. JNI handles never replace pointer types in the canonical seam. Generator 0.9.0 fingerprints include explicit static FFM callback policies.

## FFM

Generated FFM APIs provide:

- Existing loader-scoped typed calls and `SymbolLookup` method-handle factories.
- `new ApiFFM.LibraryBinding(lookup)` or `new ApiFFM.LibraryBinding(path, arena)` for independent library bindings. The caller owns the arena. Missing symbols fail when called, so an import library need not provide Native Image exports. Closing the library arena invalidates its symbol segments.
- For each `borrow` declaration, a typed overload taking a native `MemorySegment` and deriving its byte length. It rejects heap memory, closed segments, wrong-thread access, incompatible read-only memory and lengths outside the ABI carrier. Empty buffers use non-null temporary storage for the declared non-null C contract. The call does not retain the buffer.
- For each resource, `ApiFFM.ResourceName.create(binding, factory, ...)`. The consumer factory receives the address, the declared destructor and any required thread. It assumes sole ownership on successful non-null return; failed adoption destroys the allocation. Null native creation bypasses the factory. The consumer must guard borrows, arrange exactly-once destruction, and keep the library arena alive through destruction. This API does not impose a cleaner or GC mechanism.
- By-value imports using the JDK's target-specific C ABI lowering, including caller-provided allocation for record results. Native Image exports retain their explicit reference lowering and isolate argument.
- Scoped callback factories and hosted registration for dynamic and declared static targets.
- `function(lookup, true)` for an explicitly requested critical downcall. It rejects functions outside the native-leaf contract. `false` selects the ordinary transition. Critical calls do not permit heap segments. Both configurations are registered for AOT use; this does not imply a measured performance advantage.
- `new ApiFFM.CriticalLibraryBinding(lookup)` (also `Path, Arena`) exposes typed calls only for native leaves. Its unbound critical adapters initialize at image build time, avoiding the generic runtime-bound method-handle path in SVM. Addresses retain the lookup's runtime scope. Use this form for repeated critical calls in AOT applications; the method-handle factories remain useful for dynamic invocation.

```java
try (var library = Arena.ofConfined(); var memory = Arena.ofConfined()) {
    var api = new NativeApiFFM.LibraryBinding(Path.of("libownership.so"), library);
    var bytes = memory.allocateFrom(ValueLayout.JAVA_BYTE, (byte) 1, (byte) 2);
    long sum = api.checksum(bytes); // declared borrow derives length=2
}
```

Each callback signature emits `NameCallback`, `Name_CALLBACK_DESCRIPTOR` and `NameUpcalls`. `NameUpcalls.create(callback, sentinel, arena)` catches Java exceptions and returns the supplied sentinel. Void callbacks omit the sentinel and discard exceptions; callers needing diagnostics must catch and report inside their callback. Pointer/record sentinels must remain valid and satisfy the callback ABI. The arena owns the stub and retains its target. Native code must stop using the stub before that arena closes. Use a shared arena when native threads may invoke the callback; confined arenas impose the owning-thread restriction.

A known static callback target can declare an integer status policy:

```text
callback Reducer return=i64 ffm_java=fixture.CallbackOps.reduce ffm_sentinel=-1
  param left type=i64
  param right type=i64
end
```

This emits `ReducerUpcalls.create(arena)` and registers its static exception-translating method handle with `RuntimeForeignAccess.registerForDirectUpcall`. Static targets currently require `i32` or `i64` returns. Adapter method handles initialize at image build time; library addresses and upcall segments are created at runtime. No process address enters the image heap.

An ordinary JVM downcall uses the raw C ABI. Calling a Native Image export still requires that export's isolate-thread argument. FFM does not create or erase isolate setup requirements. See the [JDK FFM upcall guide](https://docs.oracle.com/en/java/javase/25/core/upcalls-passing-java-code-function-pointer-foreign-function.html) and [Native Image FFM registration requirements](https://docs.oracle.com/en/graalvm/jdk/25/docs/reference-manual/native-image/native-code-interoperability/ffm-api/).

## Static JNI for Substrate VM

`myna generate INPUT.seam --out generated --jni` additionally emits:

- `ApiJNI.java`: static native declarations, runtime library loading, and checked direct-buffer overloads.
- `seam-jni.c`: real JNI wrappers delegating to raw C implementations, plus `JNI_OnLoad_<module>_jni`.
- `seam-jni.h` and `seam-jni.rs`: the actual JNI signatures, including `JNIEnv*`, `jclass`, JNI primitive carriers and local object handles.
- `seam-jni.ll`: raw seam contracts and separate JNI wrapper declarations. Raw optimizer effects are not copied onto JNI wrappers.
- `lib<module>_jni.a.symbols`: the generated JNI symbol manifest required by the pinned SVM linker.
- Native Image properties selecting optional static JNI linkage and runtime initialization of the JNI class.

Compile the wrapper with the pinned LLVM toolchain and JNI headers from `$GRAALVM_HOME/include` and its platform subdirectory. Apply `seam-jni.ll` to the wrapper bitcode before ThinLTO, combine it with the raw implementation into `lib<module>_jni.a` using `$AR` (the pinned `llvm-ar`), and place its generated `.symbols` file beside it. Supply the directory through `-H:CLibraryPath=...` and retain generated `META-INF` resources when compiling Java. The `test_static_jni_archive` integration test provides an executable build recipe and retains the annotated bitcode, IR and executable disassembly.

Java `long` carries explicitly unsafe off-heap addresses. A by-value record argument uses an address to its record storage; a record result uses an additional trailing destination address. C performs the actual by-value call. Address callers must preserve allocation ownership, alignment, extent and lifetime. Narrow unsigned values preserve their bit patterns in signed JNI Java carriers. JNI `boolean` uses `jboolean`/`u8`, not Rust `bool`.

Declared byte-buffer borrows additionally accept `ByteBuffer`. These overloads require direct buffers, use only the remaining range, preserve the original position, and reject read-only memory for writable borrows. They check the backing segment's liveness and thread accessibility before JNI entry. These checks do not acquire or pin its arena: the caller must prevent concurrent or callback-driven arena closure until the call returns. Use FFM segment borrows when scope acquisition is required. JNI object references are local to the call; wrappers retain no global references. Generated error paths preserve pending exceptions and release temporary local references. No JNI array or string acquisition occurs: arbitrary objects, arrays, strings and persistent JNI references are outside this adapter's exposed type system. Native-thread callbacks use the scoped FFM stubs above, including attachment and exception handling supplied by the runtime.

The selected lifecycle is a library statically linked into SVM, discovered through a literal runtime `System.loadLibrary` and `JNI_OnLoad_<library>`. There is no unload-time global state or generated unload hook. Dynamic JNI loading/class-loader unloading on HotSpot is not part of this adapter; use Panama there. See the [JNI static-library lifecycle](https://docs.oracle.com/en/java/javase/25/docs/specs/jni/invocation.html#support-for-statically-linked-libraries).

## Acceptance and evidence

`mise run test` exercises the shared scalar-plus-buffer operations through Rust, the Native Image C API, JVM FFM and static JNI. Fixtures cover invalid inputs, null allocation, failed ownership adoption and cleanup (including suppressed cleanup errors), native-thread callbacks, exception-to-status conversion, arena closure and repeated FFM library lifetimes. JVM and SVM execute the FFM narrow-carrier and by-value record fixture; SVM also executes the JNI lowering against the same Rust functions. Critical calls additionally exercise both record ABI classes, heap rejection, closed and wrong-thread scopes, and concurrent allocation with observed collections. Callback fixtures execute unsigned returns, floating-point, void and pointer success/failure policies. CI is configured for host ABI checks on macOS ARM64 and Linux x86-64; other supported triples require execution on their own hardware before claiming conformance there.

Run `mise run lint` and `python3 scripts/check-hygiene.py` alongside the full test gate. Native Image machine code is not LLVM IR. Retained JNI wrapper bitcode and disassembly describe different compilation stages. The [comparison harness](benchmarking.md) separates normal, native-leaf, typed critical, runtime-bound critical and static JNI scalar paths. Production generator coverage is not generated-adapter runtime coverage, and scalar timing does not establish buffer, callback or resource performance.

Remaining acceptance work includes differential ABI fuzzing across generated signatures and target hardware, callback record returns and explicit confined-resource runtime fixtures, sanitizer execution of generated JNI adapters, and matched buffer/record/callback/resource performance workloads. Static JNI remains a compatibility adapter with normal JNI transitions; a native-leaf declaration does not turn a JNI wrapper that calls `JNIEnv` into a critical native method.
