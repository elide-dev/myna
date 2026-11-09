# Custom runtime-selected leaf pointer

`LongLeaf.java` supplies a primitive-only function-pointer adapter for the pinned Elide GraalVM 25.5.5-elide.27 compiler. Myna's existing `java_type` callback mapping generates the resolver's typed return value; no generator extensions or relaxed validation are needed.

Generate `leaf-pointer.seam`, compile `LongLeaf.java` with the generated Native Image binding, and implement `myna_leaf_address` to return a valid `int64_t (*)(int64_t)` native target. Resolve **at runtime**, once per library lifetime, and invoke the returned `LongLeaf` directly. Do not capture Word values in Java lambdas, use generic Java functional interfaces for per-call dispatch, or freeze runtime addresses into the image heap.

The floor benchmark compiles and executes this exact example, checks its results independently, and runs allocation/GC stress with the inlineable pointer and bounded Java batches. It retains default generated ABI 2 leaves and handwritten `@CFunction(NO_TRANSITION)` comparisons and native direct/indirect callers. Run `mise run bench:floor-build` and `mise run bench:floor`.

The target's complete execution must be short, bounded, nonblocking, and unable to call back into Java. The adapter carries only primitive values: this example does **not** establish that arbitrary managed-reference, record, callback, or ownership adapters can safely inline. The target library and address must remain alive throughout every call. Resolution failure must be handled before invoking a pointer.

The annotations are internal Graal API. Fixed symbols now receive this optimization automatically in binding ABI 2; this example is for custom runtime-selected targets. Compile against `$GRAALVM_HOME/lib/svm/svm-shared.jar`; do not bundle that jar or add it to the Native Image application classpath. Pin and retest the compiler before upgrading. For generated binding compilation and explicit transition opt-outs, see [ABI 2 migration](../../docs/binding-abi-2.md).
