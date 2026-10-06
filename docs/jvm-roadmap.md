# Optional adapters: FFM and Static JNI

Substrate VM remains the primary target; direct C API speed and build-time specialization take priority. JVM compatibility must not add overhead to that path. See the [Native Image performance design](performance.md).

The canonical ABI and contracts should remain shared between Native Image and JVM consumers. Runtime adapters are explicit lowerings with separate concrete signatures and symbol identities; neither Java FFM types nor JNI handles become substitutes for ABI types.

## FFM

The initial backend already emits Java 25 `FunctionDescriptor`s, struct layouts/offsets, downcall-handle factories accepting a `SymbolLookup`, typed `invokeExact` methods with per-symbol constant handles, and a generated hosted Feature registering Native Image downcalls. Extend this optional adapter as follows:

1. Add a library binding object with explicit lookup/library lifetime beyond the current loader-scoped typed calls.
2. Model pointer/length relationships, ownership and memory lifetime, then generate `MemorySegment` marshalling and scoped cleanup.
3. Add callback signatures, upcall-stub ownership, thread constraints, and exception-to-status adaptation.
4. Add by-value aggregate lowering with target-specific ABI conformance tests.
5. Extend the existing Native Image hosted registration to known static callback targets and policy-checked critical downcalls.

Ordinary JVM downcalls should use the raw C ABI. A JVM calling a Native Image export still needs that export's explicit isolate-thread argument; FFM does not erase isolate setup requirements. Scope and arena choices must follow declared lifetime semantics, not guessed Rust lifetimes.

See the [JDK FFM upcall guide](https://docs.oracle.com/en/java/javase/25/core/upcalls-passing-java-code-function-pointer-foreign-function.html) and [Native Image FFM registration requirements](https://docs.oracle.com/en/graalvm/jdk/25/docs/reference-manual/native-image/native-code-interoperability/ffm-api/).

## Static JNI

Add a distinct JNI lowering rather than labeling a raw C function as a JNI method. The concrete JNI ABI needs `JNIEnv*`, `jclass` for static Java methods or `jobject` for instance methods, JNI scalar/reference carriers, and a symbol/registration strategy. Keep all of these in an adapter model so C/Rust bindings and the LLVM applicator see the actual lowered signature.

The standard statically linked JNI lifecycle uses `JNI_OnLoad_<library>` and optionally `JNI_OnUnload_<library>` for a library combined with the VM. `RegisterNatives` can bind Java declarations to native function pointers. Static Java native methods and libraries statically linked into a JVM are separate concepts; the chosen Elide/JVM packaging model determines which lifecycle hooks the backend needs. See the [JNI invocation specification](https://docs.oracle.com/en/java/javase/26/docs/specs/jni/invocation.html#support-for-statically-linked-libraries).

First support scalar methods and explicit off-heap buffers. Then model local/global reference lifetime, class-loader identity, native-thread attachment, JNI array/string acquisition and release, pending exceptions, and unload behavior. A JNI wrapper should delegate to a raw C seam implementation wherever that preserves the intended API.

Compile the JNI wrappers with LLVM ThinLTO and apply contracts to those real objects using the existing applicator. Optimizer facts must be scoped to the raw implementation or to the complete adapter, as appropriate. A read-only raw function does not imply its JNI wrapper is globally read-only: reference management, attachment, exception translation, and VM transitions have their own effects. HotSpot's JIT-generated Java methods do not become LLVM LTO input through this integration.

## Acceptance tests

Use one scalar-plus-buffer operation across Rust, Native Image C API, JVM FFM, and JNI. Test successful calls, invalid inputs, ownership transfer/cleanup, callbacks from native threads, exception conversion, and repeated load/unload where supported. Compare actual lowered signatures and layouts; inspect the annotated wrapper/caller bitcode and verify the intended optimization occurs.

The current `Function.abiParameters()` is the shared normalization point for the Native Image thread argument. Generalize this into an explicit adapter/lowering object as JNI is added, so each backend continues to project a precomputed ABI rather than independently inventing implicit parameters.
