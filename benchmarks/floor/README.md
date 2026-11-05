# Native leaf floor

Run `mise run bench:floor-build`, then `mise run bench:floor`. The dedicated Native call floor workflow runs the same experiment on compatible ARM hardware. Results, complete disassembly, input/binary hashes, commands, toolchain identities, and per-process samples are retained under `build/floor/` and in CI artifacts. Use the same machine and build for comparisons; absolute times from the existing Linux scalar baseline are not interchangeable with local macOS results.

Each runtime executes the **same separately compiled kernel object**, without LTO. The scalar native function is noinline, and each result is checked against both the implementation's reference loop and an independent Python affine-exponentiation oracle. Recurrence cases include the scalar arithmetic dependency chain; identity cases return their input and expose call/loop costs without that arithmetic. One operation is one call, including the callee and loop. Controls are not subtracted. Five fresh processes per case, ten retained samples per process, ten warmups, and seeded randomized/rotated case order match the comparison harness.

- `native/direct`: ordinary C caller to the linked native leaf; the practical call floor for this workload.
- `native/indirect`: runtime-loaded function pointer, separating indirection from SVM costs.
- `svm/generated-leaf` and `svm/manual-leaf`: binding ABI 2 default generated leaves and handwritten Native Image no-transition imports, respectively.
- `svm/inline-pointer` and `svm/identity-inline-pointer`: the custom [typed leaf-pointer example](../../examples/leaf-pointer/README.md), resolved at runtime through Myna-generated bindings.
- `native/identity-*-unrolled4`: explicit four-call loop unrolling, to distinguish call overhead from differences in compiler loop policy.
- `svm/generated-normal`: retained transition reference.
- `batch-N`: one crossing into a fixed native batch of N scalar calls. The native caller invokes exactly the same batch function as SVM. Every scalar call remains; this is not an algebraically fused recurrence.
- `svm/java-batch-N`: a fixed Java batch annotated with Graal's internal `@Uninterruptible`. It receives a pre-resolved generated pointer and only calls short native leaves on primitive carriers, has no allocation or callbacks, and contains at most 256 iterations. The outer measurement loop remains interruptible. This is an opt-in compiler experiment, not a change to generated bindings or their dependencies.

The pinned Graal compiler rejects `@AlwaysInline` on a no-transition native method because its implicit uninterruptible annotation disallows inlining into interruptible callers. Explicit `@Uninterruptible(mayBeInlined=true)` on that native declaration is also rejected. Those checks must remain enabled. Bounded uninterruptible callers allow investigation of safe stub inlining without modifying the compiler or disabling checks.

The Graal SDK annotation and symbol jars are used only for javac; it must not be copied into the application or placed on the image-build classpath. Graal resolves its own hosted annotations. Any downstream use must pin and test the Graal version and enforce the **whole batch's** bounded latency, including all native callees. An ordinary nonblocking API name alone is not enough to establish that contract.

The default is 16,777,216 calls per sample to reduce timer and CPU-ramp effects. The build also runs allocation/GC stress and requires observed collections; the calling loop remains active until the collector thread finishes. No safety checks are disabled.
