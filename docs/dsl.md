# Descriptor format, version 1

The frontend uses UTF-8 lines. Whitespace separates tokens; `#` starts a comment. Every option is `key=value`. Unknown directives/options, duplicate options, and unterminated blocks fail with a line number. There are no implicit includes, substitutions, or executable expressions.

A module starts with:

```text
module moduleName abi=1 target=aarch64-apple-darwin
```

Supported targets: `aarch64-apple-darwin`, `x86_64-apple-darwin`, `aarch64-unknown-linux-gnu`, `x86_64-unknown-linux-gnu`. Their layouts are modeled explicitly as 64-bit little-endian targets; no global assumption about other ABIs is made. The CLI's `--target` overrides the descriptor target before validation/fingerprinting.

## Types and layouts

Scalars are `void`, `i8/u8`, `i16/u16`, `i32/u32`, `i64/u64`, `f32/f64`. `ptr<T>` supports scalar, opaque, record, and nested-pointer pointees. Function scalars are limited to 32/64 bits plus floats and void returns. `void` cannot be a parameter/field. Unsigned Java carriers preserve raw bits in the same-width signed primitive.

```text
opaque Buffer
struct View size=24 align=8
  field data type=ptr<u8> offset=0
  field length type=u64 offset=8
  field flags type=u32 offset=16
end
```

Fields are scalar/pointer values in ascending, non-overlapping, naturally aligned offsets. All padding and final size are explicit. C/Rust emit compile-time size/alignment/offset assertions; FFM uses explicit padding layouts. By-value records/arrays and custom packing are not implemented. Symbol/type/parameter names must be portable identifiers and avoid reserved words.

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

`import` means Java calls an external native implementation. `export` means Native Image exposes a Java method. Rust declarations may call either symbol; downstream implementations define imports separately. Logical names and linker symbols are independent; `symbol` defaults to the logical name. Symbols and logical names are unique within a module.

Imports default to the normal `@CFunction` transition and require `error=no_failure`, describing an ABI where language exceptions do not cross the boundary. This does **not** infer `nounwind` or mean an operation cannot return an application error code.

Exports require `isolate=thread` and a public static `java=qualified.Class.method`. An `isolate_thread` pointer is prepended to the normalized ABI and all backend signatures. The implementation method receives only the declared parameters. The caller creates/attaches an isolate before invoking an export. Pointer carriers in the implementation are `org.graalvm.word.PointerBase`.

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

Declarations and fact keys are sorted, parameter/field order is preserved, and output includes no timestamps or input paths. `seam.abi` is the SHA-256 of canonical `seam.json` including target, generator/schema version, ABI shape, Java adaptation, and fact provenance. It intentionally changes when contracts change, even if the C signature remains identical. Fingerprints detect drift; they do not prove semantic correctness or authenticate a provider binary.

## Native Image leaf-call policy

`native_leaf=true@explicit_contract` on an import selects `CFunction.Transition.NO_TRANSITION`. It promises short, bounded execution with no blocking or Java callbacks and compliance with the Native Image no-transition restrictions. Exports and `noreturn` functions cannot opt in. Default imports retain normal transitions. This runtime contract is recorded in JSON/fingerprints, never emitted as an LLVM attribute, and never inferred from `nounwind` or `willreturn`. See [the performance design](performance.md) for obligations and optimization boundaries.
