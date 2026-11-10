# Descriptor format, version 1

The frontend uses UTF-8 lines. Whitespace separates tokens; `#` starts a comment. Every option is `key=value`. Unknown directives/options, duplicate options, and unterminated blocks fail with a line number. There are no implicit includes, substitutions, or executable expressions.

A module starts with:

```text
module moduleName abi=1 target=aarch64-apple-darwin
```

Optional `java_package=dev.example.io` and `java_class=IoNatives` name the generated Java: the Native Image class becomes `IoNatives`, the FFM class `IoNativesFFM`, and the Feature `IoNativesForeignFeature`, each named `<class>.java`. Defaults are `dev.elide.myna.generated` and `MynaNative`/`MynaFFM`/`MynaForeignFeature`. Naming is part of the canonical JSON and fingerprint only when set.

Supported targets: `aarch64-apple-darwin`, `x86_64-apple-darwin`, `aarch64-unknown-linux-gnu`, `x86_64-unknown-linux-gnu`. Their layouts are modeled explicitly as 64-bit little-endian targets; no global assumption about other ABIs is made. The CLI's `--target` overrides the descriptor target before validation/fingerprinting.

The canonical descriptor records the target's resolved ABI class (`"targetAbi": {"dataModel":"LP64","byteOrder":"little"}`, with `,"charUnsigned":true` on AAPCS64 Linux), not its triple. Struct sizes, alignments and offsets, scalar widths and Java carriers follow from the class alone, so targets in one class produce the same fingerprint and byte-identical `seam.json`, `seam.abi`, `seam.h`, `seam.rs`, Java and Native Image metadata. Only the LLVM contracts (`seam.ll`, `seam-jni.ll`) name the triple: sub-word argument extension and by-value lowering are target conventions that the native compiler and the Java linker each apply for the platform they run on, and `myna apply` still rejects contracts for a different target. The C `char` signedness is part of the ABI class because the Truffle cells observe it: x86-64 and Apple arm64 (`signed` `char`) share one LP64 little-endian class, and `aarch64-unknown-linux-gnu` (AAPCS64 Linux, `unsigned` `char`) is its own class. `myna validate INPUT.seam --targets TRIPLE,...` validates every listed target and fails unless they share one fingerprint.

## Types and layouts

Scalars are `void`, `bool`, `char`, `i8/u8`, `i16/u16`, `i32/u32`, `i64/u64`, `usize/isize`, `f32/f64`. `char` is C `char` (Rust `core::ffi::c_char`, signed or unsigned by target). `usize/isize` are pointer-sized (`size_t`/`ptrdiff_t`, Java `long`, `ptr<usize>` → `WordPointer`). `ptr<T>` supports scalar, opaque, record, and nested-pointer pointees.

`isolate_thread` is a parameter type for the Native Image isolate thread passed through native code (Java `IsolateThread`, Rust/C `void *`); every export's prepended isolate parameter has this type.

## Callbacks

```text
callback Visitor return=i32
  param isolate type=isolate_thread
  param value type=i64
end
export visit return=i32 java=fixture.Ops.visit isolate=thread error=abort callback=Visitor
  param value type=i64
end
import forEach return=i32 error=no_failure
  param visitor type=fn<Visitor>
  param isolate type=isolate_thread
end
```

`fn<Name>` is a C function pointer to a declared callback: a nested `CFunctionPointer` interface with `@InvokeCFunctionPointer invoke(...)` in Java, `Option<unsafe extern "C" fn(...)>` in Rust, and a `typedef` in C. An export with `callback=Name` must match the signature (including its isolate parameter); the generated `<Class>.Literals` holds its `CEntryPointLiteral`. `CEntryPointLiteral.create` only runs during the image build, so initialize `<Class>$Literals` at build time. Callback parameters carry no contracts, and unsigned sub-32-bit callback parameters are rejected.

Sub-32-bit values (`bool`, `i8/u8`, `i16/u16`) follow each target's C extension rule, as clang and rustc emit it: parameters are `signext`/`zeroext` on x86-64 and Apple arm64 and unextended on AAPCS64 Linux. Returns carry no extension contract because the producers disagree on x86-64 Linux. `myna apply` verifies extension against the compiler's and never adds it. Native Image extends a narrow Java value by its signed Java type, so unsigned imports go through a `<name>Native` `int` binding behind a typed wrapper, and exports return an explicitly extended `int`. `void` cannot be a parameter/field. Unsigned Java carriers preserve raw bits in the same-width signed primitive.

```text
opaque Buffer
struct View size=24 align=8
  field data type=ptr<u8> offset=0
  field length type=u64 offset=8
  field flags type=u32 offset=16
end
```

Fields are scalar/pointer values in ascending, non-overlapping, naturally aligned offsets. All padding and final size are explicit. C/Rust emit compile-time size/alignment/offset assertions; FFM uses explicit padding layouts. Rust records derive `Clone, Copy`; the Native Image class gets a nested accessor class per record (`SIZE`, `ALIGNMENT`, typed get/set at each offset) for records in `StackValue` or unmanaged memory. Arrays, nested by-value fields and custom packing are not implemented.

### Records by value

A declared `struct` may be a function parameter or return type (`param p type=Pt`, `return=Pt`). C, Rust and FFM use the real by-value C ABI. Native Image's C interface carries only primitives and words, so that boundary uses a reference form, `<symbol>_myna_ref`, taking `const T *` for each record and returning a record through a trailing `T *result`:

- Imports: the native side implements the by-value symbol; the implementing Rust crate invokes `native_image_shims!(path::to::seam)` beside it to define the reference forms, which Java's `@CFunction`s bind. ThinLTO inlines each shim into its implementation. `seam.h` declares the reference form (`<symbol>_myna_ref`) beside each by-value import's prototype; a C provider linked through `c_library`/`c_library_static` must also define it — the C counterpart of `native_image_shims!` — e.g. `void pt_flip_myna_ref(struct Pt const * p, int32_t k, struct Pt * result) { *result = pt_flip(*p, k); }`.
- Exports: Native Image exports only the reference form, and the Java target takes `PointerBase` records plus `result`. `seam.rs` adds an `#[inline]` by-value wrapper and `seam.h` a `static inline` one, so Rust and C call by value.
- `seam.ll` declares only reference forms, with `nonnull`, `captures(none)`, `readonly`/`writeonly`, `align` and `dereferenceable` from the record layout; by-value lowering itself (registers, `byval`, `sret`) is target-specific and not described.

Records cannot cross callbacks, and `result` is reserved as a parameter name when a function returns a record. Macros expanded outside the generated module take its path: `assert_implementations!(seam)`, `native_image_shims!(seam)`. Symbol/type/parameter names must be portable identifiers and avoid reserved words.

## Functions

```text
import inspect symbol=seam_inspect return=i64 error=no_failure cc=c
  param value type=ptr<i64> nullable=false ownership=borrowed access=read
end
export sum symbol=seam_sum return=i64 java=fixture.BufferOps.sum isolate=thread error=integer_sentinel sentinel=-1
  param data type=ptr<u8> nullable=false ownership=borrowed access=read
  param length type=i64
end
```

`import` means Java calls an external native implementation. For imports, `seam.rs` also defines `assert_implementations!()`: invoked where the Rust implementations are in scope under their symbol names, it fails compilation if any implementation's signature differs from the descriptor's lowered ABI (including `*const`/`*mut` from `access`). `export` means Native Image exposes a Java method. Rust declarations may call either symbol; downstream implementations define imports separately. Logical names and linker symbols are independent; `symbol` defaults to the logical name. Symbols and logical names are unique within a module.

Binding ABI 2 imports default to inlineable no-transition leaf calls and require `error=no_failure`, describing an ABI where language exceptions do not cross the boundary. This does **not** infer `nounwind` or mean an operation cannot return an application error code.

Exports require `isolate=thread` and a public static `java=qualified.Class.method`. An `isolate_thread` pointer is prepended to the normalized ABI and all backend signatures. The implementation method receives only the declared parameters. The caller creates/attaches an isolate before invoking an export. Pointer carriers are typed where Native Image has one (`ptr<u8>`/`ptr<i8>` → `CCharPointer`, `ptr<ptr<u8>>` → `CCharPointerPointer`, `ptr<void>` → `VoidPointer`, `ptr<i32>` → `CIntPointer`, …) and `org.graalvm.word.PointerBase` otherwise. `include=qualified.Predicate` sets `@CEntryPoint(include = …)`, a `BooleanSupplier` that decides per image whether the export is built. Read-only pointer parameters of exports carry `@CConst`, which reaches the C headers Native Image writes. `seam.rs` also defines `<Name>Fn` function-pointer types for every function, for callers that resolve symbols dynamically.

Supported export error conventions:

- `integer_sentinel` catches `Throwable` and returns the specified signed i32/i64 `sentinel`.
- `null_sentinel` catches `Throwable` and returns a null pointer.
- `abort` uses Native Image's default fatal exception handler.

The sentinel must be reserved by the application's logical API. None of these schemes automatically supplies stronger optimizer facts.

## Pointer metadata and contracts

Pointer parameter metadata defaults to `nullable=true`, `ownership=unspecified`, `access=unspecified`.

Ownership: `borrowed`, `owned_by_caller`, `owned_by_callee`, `transferred_to_callee`, `unspecified`. Transfer-to-caller on parameters is rejected. Access: `none`, `read`, `write`, `read_write`, `unspecified`. Read access produces C const/Rust const pointers; it alone does not emit LLVM `readonly`.

Optimizer facts have mandatory provenance:

```text
param data type=ptr<u8> nullable=false ownership=borrowed access=read nonnull=true@explicit_contract nocapture=true@explicit_contract readonly=true@explicit_contract align=8@explicit_contract dereferenceable=16@explicit_contract
```

Parameter facts: `nonnull`, `nocapture`, `readonly`, `writeonly`, `noalias`, `align`, `dereferenceable`. Function facts: `nounwind`, `noreturn`, `willreturn`, `nofree`. Booleans use `true`/`false`; alignments and byte counts are positive decimal integers. LLVM 23 renders `nocapture` as `captures(none)`.

Provenance: `explicit_contract`, `compiler_proven`, `abi_derived`, `language_derived`, `heuristic`. Version 1 emits only explicit/proven enabled facts. Others fail strict validation or are omitted under `--relaxed`, while remaining visible in JSON. `noalias` with unapproved provenance always fails. Contradictions fail in either mode.

Facts describe the entire exported/native operation, including runtime transitions and error paths. A read-only Java body is not evidence that its Native Image entry wrapper is globally read-only. There is no automatic inference of `noalias`, `memory(none)`, `nosync`, or `speculatable`.

## Determinism

Declarations and fact keys are sorted, parameter/field order is preserved, and output includes no timestamps or input paths. `seam.abi` is the SHA-256 of canonical `seam.json` including target ABI class, generator/schema version, ABI shape, Java adaptation, and fact provenance. It intentionally changes when contracts change, even if the C signature remains identical. Fingerprints detect drift; they do not prove semantic correctness or authenticate a provider binary.

## Native Image leaf-call policy

Imports default to the binding ABI 2 leaf contract: short, bounded, nonblocking execution with no Java reentry. Use `native_leaf=false@explicit_contract` to opt out and retain a normal `@CFunction` transition. Explicit `native_leaf=true@explicit_contract` is accepted but redundant. Exports retain normal isolate entry; `noreturn` imports must opt out. The effective policy is recorded in schema 3 JSON as `nativeCallPolicy`, with a separate `bindingAbiVersion: 2`; the module's `abi` remains the consumer's own protocol version. Leaf policy changes fingerprints but emits no LLVM attributes. See [migration](binding-abi-2.md) and [performance](performance.md).

## Resource lifecycles and call borrows

`resource NAME representation=native threading=confined|shared create=FUNCTION destroy=FUNCTION` declares a unique native resource lifecycle. `NAME` must already be an opaque type. `borrow FUNCTION PARAMETER length=PARAMETER lifetime=call` declares a complete non-escaping byte-buffer contract. These opt into generated ownership APIs and stronger validation; metadata-only `ownership=` on a pointer does not do so by itself. See [ownership.md](ownership.md) for exact obligations, supported signatures, and the consumer-implemented Native Image API.

## Native Image C context, library linkage, and handwritten callbacks

Module options `c_context=qualified.DirectivesClass` and `c_library=library_name` annotate the generated Native Image class with `@CContext` and `@CLibrary`. They can be used independently. `c_library_static=true` requests static linkage and requires `c_library`; its default is false. These options require at least one native import.

```text
module bemo target=aarch64-apple-darwin java_package=my.bemo java_class=BemoImports c_context=my.bemo.BemoDirectives c_library=bemo c_library_static=true
callback Completion return=i64 java_type=my.bemo.HandwrittenCompletion
  param thread type=isolate_thread
  param status type=i64
end
import registerCompletion return=void error=no_failure
  param callback type=fn<Completion>
end
```

The consumer supplies the public, accessible `CContext.Directives` implementation on the image builder's classpath. It can provide headers, macros, library search paths, and other build settings. `c_library` is the library name without the platform prefix/extension, as expected by Native Image. Annotations are on the class, so they also cover private native methods generated for small-integer lowering. See [CContext](https://www.graalvm.org/sdk/javadoc/org/graalvm/nativeimage/c/CContext.html) and [CLibrary](https://www.graalvm.org/sdk/javadoc/org/graalvm/nativeimage/c/function/CLibrary.html).

`java_type` on a callback references an existing fully qualified Java function-pointer interface. The generator uses that type in Native Image parameters, returns, and explicitly requested entry-point literals, and **does not emit a replacement interface**. The handwritten interface must extend `CFunctionPointer` and supply its own `@InvokeCFunctionPointer` method with the declared ABI. The descriptor remains the signature source for C, Rust, FFM, and LLVM; keeping the handwritten Java signature consistent is the consumer's obligation, since descriptor validation does not load consumer classes.

Omit `callback=` on an export if its entry-point literal should also remain handwritten. Existing callback declarations without `java_type` continue generating nested interfaces. Callback retention and lifetime management remain the consumer's responsibility.

Both linkage configuration and handwritten type mappings are serialized and fingerprinted; target overrides preserve them. The executable [Bemo integration fixture](../tests/bemo/bemo.seam) links a static Rust archive using only the generated annotations and consumer directives, then calls back through a handwritten interface and literal.

## Truffle cells

`truffle_cells=true` on the module generates `<NativeClass>Truffle.java`: one Truffle cell per import, calling the module's existing bindings with language-neutral coercions from `myna-truffle`. `truffle_backend=native|ffm`, `truffle_coercion=strict|wrap` and `truffle_transitions=direct|boundary` refine it and require the option. Per import, `truffle_cell=false@explicit_contract` opts out (required for by-value records and callback parameters) and `truffle_raw_pointers=true@explicit_contract` accepts guest numbers as pointers. Per byte-pointer parameter, `truffle_buffer=true@explicit_contract` lends raw bytes (no terminator, heap arrays pinned when large) where the parameter would otherwise be treated as a C string; it is a provider fact, not an LLVM attribute. The configuration is serialized under `"truffle"` and fingerprinted; descriptors without it keep their fingerprints. See [Truffle native cells](truffle-cells.md).

## FFM callback targets and static JNI

Callbacks may additionally declare `ffm_java=qualified.Class.method ffm_sentinel=INTEGER`. Both fields are required together, and the signature must return `i32` or `i64`. The generated FFM upcall invokes the public static target with FFM carriers, catches Java exceptions and returns the reserved sentinel. The hosted Feature registers this static target for AOT upcalls. These fields are independent of the SVM `java_type` interface mapping and are included in the ABI fingerprint.

Callbacks without a static FFM target still provide scoped factories accepting a Java callback and an explicit failure sentinel. Stub lifetime and thread access follow the supplied arena. Imports that call back must opt out of native leaves. See the [adapter guide](jvm-roadmap.md) for callback ownership and library lifetimes.

The CLI's optional `--jni` flag projects imports into static SVM JNI methods. It does not change the canonical C signature or introduce JNI handles into seam types. Declared `borrow` relationships additionally generate direct `ByteBuffer` overloads; raw pointers remain explicit unsafe `long` addresses. The adapter emits its own concrete JNI signatures and library lifecycle metadata.
