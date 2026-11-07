# Calling Myna bindings from Truffle guest code

Elide's guest languages (JavaScript, Python and other Truffle languages) run in the same Native Image as Myna's bindings. Their hot code is compiled at run time by Truffle partial evaluation, and that code can call Myna bindings directly. This page records what each binding kind costs from there and what it requires of the Elide GraalVM fork. It needs no generator changes: the current emitted code is already the right shape.

Measured from force-compiled Truffle roots on Linux amd64 (unclemax, `taskset`), calling `labs` (C interface) or `abs` (FFM) through bindings written exactly as Myna emits them:

| Binding kind | Cost per call | Fork requirement |
| --- | --- | --- |
| Native Image leaf: `@InvokeCFunctionPointer(NO_TRANSITION)` with `@AlwaysInline` and `@Uninterruptible(mayBeInlined = true)` | **0.86 ns**, inlined | any |
| Native Image transitioning function pointer (`native_leaf=false@explicit_contract`) | **6.2 ns**, calls the AOT stub | 25.5.5-elide.26 ([graal#196](https://github.com/elide-dev/graal/pull/196)); older: image build fails |
| Native Image `@CFunction` | **6.5 ns**, calls the AOT stub | 25.5.5-elide.26 ([graal#196](https://github.com/elide-dev/graal/pull/196)); older: image build fails |
| FFM downcall (`static final MethodHandle`, registered by `MynaForeignFeature`) | **11.2 ns** | 25.5.5-elide.26 ([graal#189](https://github.com/elide-dev/graal/pull/189)); older: ~575 ns, or the image does not build |
| FFM critical downcall (`Linker.Option.critical(false)`, leaves only) | **9.2 ns** | 25.5.5-elide.26 ([graal#189](https://github.com/elide-dev/graal/pull/189)) |

## Why each kind behaves this way

- **Leaves.** A no-transition call has no frame anchor and no safepoint, so Truffle may inline it into compiled guest code, just as the AOT compiler does. Keep `@AlwaysInline` on the generated leaf interfaces. Native Image only accepts `@Uninterruptible(mayBeInlined = true)` with a custom reason because the method is always inlined; compiled on its own, it fails the uninterruptible-annotation check.
- **Transitioning calls.** The Java-to-native thread-state transition can only exist in AOT code: its lowering is hosted-only, and deoptimizing while blocked in native code would need the deoptimization stub to do the transition. Before graal#196 a Truffle call path that reached such a binding was a blocklist violation, which failed the image build. Since then, Truffle-compiled code calls the binding's AOT stub, which does the transition.
- **FFM.** graal#189 creates every registered downcall's handle at image build time, generates an entry method per handle that calls the `DowncallStub` directly, and links constant handles to it during partial evaluation. Myna's FFM output already uses build-time-initialized `static final` handles and registers every descriptor (plus the `critical` variant for leaves), which is exactly what the fast path needs. It is active in images with runtime compilation.

Prefer the direct Native Image backend for guest-facing hot calls: it is the fastest path (and the leaf default is cheaper than any FFM call). FFM remains useful where a binding must also run on HotSpot.

## Known issues

- **Compiling in isolates** (Native Image's default with `--macro:truffle`) can abort while installing code for typed Truffle entries ([graal#194](https://github.com/elide-dev/graal/issues/194); fix in progress). It is unrelated to Myna's bindings. Until it is fixed, images that hit it can build with `-H:+UnlockExperimentalVMOptions -H:-SupportCompileInIsolates`.
- **Dynamic calls through Truffle NFI** (for example GraalPy `ctypes`) don't go through Myna. 25.5.5-elide.26 makes them about 45% faster for primitive arguments and 3.8 times faster for strings ([graal#190](https://github.com/elide-dev/graal/pull/190)), but they remain about 24 ns per call. Use Myna bindings for hot paths.

## ThinLTO

With 25.5.5-elide.26 and toolchain 2026.11.5, Native Image's own static C libraries (`libjvm`, `libsvm_container`, `libchelper`, `libzstd`) are fat LTO objects for both glibc and musl ([graal#193](https://github.com/elide-dev/graal/pull/193)). A fully static musl image linked with `-flto=thin` takes them, musl `libc.a` and Myna's annotated Rust and C bitcode into one ThinLTO link. glibc's own archives are machine code only, so on glibc the LTO stops at libc. See [the ThinLTO integration](thinlto.md).
