# Descriptor format, version 1

The frontend uses UTF-8 lines. Whitespace separates tokens; `#` starts a comment. Every option is `key=value`. Unknown directives/options, duplicate options, and unterminated blocks fail with a line number. There are no implicit includes, substitutions, or executable expressions.

A module starts with:

```text
module moduleName abi=1 target=aarch64-apple-darwin
```

`abi=N` is the consumer's own protocol version (a positive integer, 1 when omitted), recorded in the canonical
JSON. When the module line names it, three outputs also emit it, typed `i32`:
`#define <MODULE>_ABI_VERSION INT32_C(N)` in `seam.h`, `pub const ABI_VERSION: i32 = N;` in `seam.rs` and
`public static final int ABI_VERSION = N;` in `<Class>Constants`, which is generated for it even when the
module declares no other constants. A provider can return it and a consumer check it from generated code. The
Truffle `constants()` object, the Native Image `@CConstant` accessors and `seam.ll` do not carry it. Only the
version is part of the canonical JSON and fingerprint, not whether `abi=` was written: `abi=1` and an omitted
`abi=` share a fingerprint but differ in these outputs (and in whether `<Class>Constants` exists). With `abi=`
declared, a `const ABI_VERSION`, a function or an enum or flag set named `ABI_VERSION`, and any other
generated C name `<MODULE>_ABI_VERSION` are rejected.

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

Fields are scalar/pointer values in ascending, non-overlapping, naturally aligned offsets. All padding and final size are explicit. C/Rust emit compile-time size/alignment/offset assertions; FFM uses explicit padding layouts. Rust records derive `Clone, Copy`; the Native Image class gets a `@CStruct` interface per record (`SIZE` and `ALIGNMENT` constants, `@CField` get/set per field, `addressOf<Field>()`), used with `StackValue.get(R.class)` and `SizeOf.get(R.class)` for records in the stack or unmanaged memory. Inline arrays, nested by-value fields and custom packing are not implemented; a pointer field can name its element count (below).

FFM declares `<Struct>_LAYOUT` and a `<Struct>_<field>_OFFSET` per field. A struct that some `ptr<Struct>` targets (an import's or a callback's parameter or return, a struct field, or the out slot of a struct [resource](ownership.md#status-plus-out-pointer)) also gets `<Struct>_POINTER`, which is `ValueLayout.ADDRESS.withTargetLayout(<Struct>_LAYOUT)`. Those imports, callbacks and fields use it. An import's result, a callback's argument, the address an out-pointer constructor hands its factory and `record.get(<Struct>_POINTER, <Owner>_<field>_OFFSET)` are therefore segments of the struct's size in the global scope, with no `reinterpret`. Truffle FFM cells keep their own `NativePointer` handling.

**A null pointer is a sized segment too**, at address 0. It equals `MemorySegment.NULL`, but its `byteSize()` is the struct's, so a read through it is not bounds-checked: it is an unchecked native read of address 0, which crashes the process rather than throwing. The same holds for a dangling pointer to freed memory. Test `address() == 0` (or `equals(MemorySegment.NULL)`) before any read, not `byteSize() == 0`. Keeping the pointee alive is the caller's contract, as it is for the record.

A pointer misaligned for the struct is an `IllegalArgumentException` when it is read or returned, after the native call has completed. This includes sentinel and tagged values such as `(T *)-1`, so declare such a field or return as `ptr<void>`, or as an integer, and compare the raw value. Pointers to opaque types, scalars and pointers stay zero-length. So does a counted view's pointer, which its accessors size by the count, and so does a pointer array's element (below).

A struct passed only by value gets no `<Struct>_POINTER`, so its module initializes as before. A layout is complete when it is created and cannot contain itself. A pointer field whose target can reach the field's own struct through pointer fields therefore targets a padding layout of the target's size and alignment. Such a target is in the same cycle: a self-reference such as `next: ptr<Node>`, or every edge of a mutual cycle. The segment is sized the same, but the target carries no field names. Which fields are shells depends only on the pointer graph, never on names. Every other pointer field targets the full layout, so the layouts are declared pointees first, depth first in name order. This can move a layout's declaration in the class. `<Struct>_POINTER` always targets the full layout.

`withTargetLayout` is a restricted method, which the FFM class calls when it is initialized. The module that holds the class needs native access (`--enable-native-access=ALL-UNNAMED`, or its module name), as its downcalls already do. Without it, HotSpot warns by default and fails with `--illegal-native-access=deny`. Native Image builds pass the same flag.

### Counted element views

A `ptr<T>` field may name the integer field of the same record that counts the elements it points to:

```text
struct Program size=48 align=8
  field functions type=ptr<Function> offset=0 count=functionCount
  field functionCount type=u64 offset=8
  field text type=ptr<u16> offset=16 count=textLength
  field textLength type=u32 offset=24
  field version type=u32 offset=28
  field roots type=ptr<ptr<Function>> offset=32 count=rootCount
  field rootCount type=usize offset=40
end
```

The pair is a contract on the record: a non-null pointer addresses `count` consecutive elements that stay valid for as long as the record does, and a null pointer has a zero count. The carrier, layout, C ABI, `seam.h` and `seam.ll` do not change; `seam.json` records `"count"` on the pointer field, so the relation is part of the fingerprint. `T` is a declared struct, a non-`bool` scalar, or a pointer to a struct or opaque type (`ptr<ptr<Node>>`, an array of element pointers such as a tree's child list; the views read the element pointers as stored, so a null element comes back null and is the caller's to check); the count is a plain `u8`, `u16`, `u32`, `u64` or `usize` field (no `enum=`/`flags=`) that counts at most one pointer. A count is never negative, so signed carriers, `isize` included, are rejected: declare a C `int` count as `u32` (the layout is identical) and a `size_t` count as `usize`. A view that should share another view's count is not expressible yet. Generated accessors (see `examples/views.seam`):

- **Rust**: `unsafe fn functions(&self) -> &[Function]` on the record (the field's name; Rust keeps methods and fields apart). It is `unsafe` because records are `Copy` with public raw fields, so safe code can build one whose pointer and count disagree. Its `# Safety` contract: the record came from the producer, or a non-null pointer addresses `count` initialized, aligned elements that stay valid and unmodified while the record is borrowed, and a null one has a zero count. Null is the empty slice; `debug_assert!` checks a null pointer's zero count, the `isize` size limit and alignment. Indexing past the count panics as any slice does. A pointer array is `&[*mut T]`, the field's own pointer spelling, with the same rules.
- **Java FFM**: every view is **read-only**: the elements are the producer's memory, as Rust's shared `&[T]` is. Beside the `_OFFSET` constants, each view gets:
  - `long <Struct>_<field>_count(record)`: the count widened unsigned. A `u64` above `Long.MAX_VALUE` reads negative, which every view rejects, so take the view before looping on a `u64` count: a loop bounded by a negative count silently runs zero times.
  - `<Struct>_<field>(record)`: the elements as one segment of `count * sizeof(T)` bytes (`count * ADDRESS.byteSize()` for a pointer array) in the global scope, for callers that manage the lifetime themselves.
  - `<Struct>_<field>(record, Arena owner)`: the same view bound to `owner`, so a read after the owner closes throws `IllegalStateException` instead of reading freed memory. A confined owner makes the view confined to its thread (`WrongThreadException` elsewhere).
  - `<Struct>_<field>_at(view, i)`: element `i` of a view taken once, the loop shape. A struct element is `view.asSlice(i * size, size)`, a scalar `view.getAtIndex(layout, i)`, and a pointer array's element the address at `i` as a read-only, zero-length segment in the global scope. It is deliberately unsized, unlike a `ptr<Struct>` field, so a null element cannot be read by mistake. The owner form sizes it. No call is restricted, and there is no `reinterpret` per element. The bound is the view's own size (`view.byteSize() / size`), which is the count for a view from the generated accessor; any other segment gets its own size as the bound.
  - `<Struct>_<field>_at(view, i, Arena owner)`, pointer arrays only: the element's pointee as a read-only segment of `T`'s size (0 for an opaque `T`) bound to `owner`. It asserts the pointee lives as long as the owner does, as when one arena owns a whole result, pointees included. A null element stays a null segment. Scoping costs one restricted call per element.
  - `<Struct>_<field>(record, i)`: element `i` through one view (one `reinterpret`). Struct and pointer elements also have `(record, i, owner)`, scoped to `owner` (a pointer array's goes through the owner form of `_at`). Every returned segment is read-only, elements included.

  There is one failure mode, checked unconditionally: `IndexOutOfBoundsException`, whose message names the struct, the field and the count field. It covers an index outside `0 <= i < count`, a null pointer with a nonzero count, a `u64` count above `Long.MAX_VALUE`, a count whose byte size overflows a `long`, and a non-null pointer misaligned for the element. A null pointer with a zero count is an empty view. Every accessor reads the record through its segment's bounds. A record an import returns, or one read through `<Struct>_POINTER`, is already sized; reinterpret any other zero-length pointer to `<Struct>_LAYOUT.byteSize()` first. The record's own lifetime stays the caller's contract; the owner overloads bind the view, not the record.
- **Native Image**: the record's getter keeps returning the typed pointer, and an element struct's interface gains `T addressOf(long index)`. SVM accepts only C-interface methods in a `@CStruct` interface, so the bounded accessor is an `@AlwaysInline` static method of the class: `<Struct>_<field>At(record, i)` returns the element record (`addressOf(i)`), the scalar (`read(WordFactory.signed(i))`), or for a pointer array the element's struct or opaque interface (`((WordPointer) record.field()).read(WordFactory.signed(i))`: one load of the field and one scaled load) after an `assert` of `0 <= i < count`, which compiles out unless the image is built with `-ea`; the image then holds one load of the pointer field and the scaled access. The helpers are not for `@Uninterruptible` code (an enabled assert would allocate there); use `record.field().read(...)` or `record.field().addressOf(i)` directly. A struct used as an element may not have a field named `addressOf`.
- **Truffle**: records are not guest-visible objects (cells take and copy buffers), so cells generate nothing for views.

The accessor names `<Struct>_<field>`, `<Struct>_<field>At`, `<Struct>_<field>_count` and `<Struct>_<field>_at` may not be function names, and two views' names may not meet (a view of `p_count` beside a view of `p`).

### Records by value

A declared `struct` may be a function parameter or return type (`param p type=Pt`, `return=Pt`). C, Rust and FFM use the real by-value C ABI. Native Image's C interface carries only primitives and words, so that boundary uses a reference form, `<symbol>_myna_ref`, taking `const T *` for each record and returning a record through a trailing `T *result`:

- Imports: the native side implements the by-value symbol; the implementing Rust crate invokes `native_image_shims!(path::to::seam)` beside it to define the reference forms, which Java's `@CFunction`s bind. ThinLTO inlines each shim into its implementation. `seam.h` declares the reference form (`<symbol>_myna_ref`) beside each by-value import's prototype; a C provider linked through `c_library`/`c_library_static` must also define it — the C counterpart of `native_image_shims!` — e.g. `void pt_flip_myna_ref(struct Pt const * p, int32_t k, struct Pt * result) { *result = pt_flip(*p, k); }`.
- Exports: Native Image exports only the reference form, and the Java target takes the record's struct interface (and opaque interfaces for opaque pointers) plus `result`. `seam.rs` adds an `#[inline]` by-value wrapper and `seam.h` a `static inline` one, so Rust and C call by value.
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

`import` means Java calls an external native implementation. For imports, `seam.rs` also defines `assert_implementations!()`: invoked where the Rust implementations are in scope under their symbol names, it fails compilation if any implementation's signature differs from the descriptor's lowered ABI (including `*const`/`*mut` from `access`). With `rust_implementation=trait` on the module, `seam.rs` also generates a safe trait for implementing the imports over their declared contracts and `define_implementations!`, which defines every import's entry point from a type implementing it; see [implementing imports in Rust](ownership.md#implementing-imports-in-rust). For a module with imports, the records and these macros are also written separately, as `seam_types.rs` and `seam_impl.rs`, so the implementation can live in a different crate from the records; see [records and implementation in separate crates](ownership.md#records-and-implementation-in-separate-crates). `export` means Native Image exposes a Java method. Rust declarations may call either symbol; downstream implementations define imports separately. Logical names and linker symbols are independent; `symbol` defaults to the logical name. Symbols and logical names are unique within a module.

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

On a `borrow` buffer an approved `align` is a caller contract (debug builds assert it in the non-cell wrappers; Truffle cells copy a buffer that violates it); `dereferenceable` and `noalias` are still rejected there. An approved `align` is a power of two of at most 2^29, and at most 4096 on a `borrow` buffer, whose wrappers lend a present empty buffer an aligned dummy. A Truffle cell lends a present empty buffer on any other byte pointer a page-aligned dummy under a fact up to 4096, and above that a dangling pointer aligned to the fact (the address `N` itself), so cells allocate and copy nothing for it. Parameter facts: `nonnull`, `nocapture`, `readonly`, `writeonly`, `noalias`, `align`, `dereferenceable`. Function facts: `nounwind`, `noreturn`, `willreturn`, `nofree`. Booleans use `true`/`false`; alignments and byte counts are positive decimal integers. LLVM 23 renders `nocapture` as `captures(none)`.

Provenance: `explicit_contract`, `compiler_proven`, `abi_derived`, `language_derived`, `heuristic`. Version 1 emits only explicit/proven enabled facts. Others fail strict validation or are omitted under `--relaxed`, while remaining visible in JSON. `noalias` with unapproved provenance always fails. Contradictions fail in either mode.

Facts describe the entire exported/native operation, including runtime transitions and error paths. A read-only Java body is not evidence that its Native Image entry wrapper is globally read-only. There is no automatic inference of `noalias`, `memory(none)`, `nosync`, or `speculatable`.

## Constants, enums and flag sets

A module may name integer values the native side already defines:

```text
const ANALYSIS_MAGIC type=u32 value=0x3141514b

enum Status type=i32
  value null_result = -1
  value ok = 0
  value syntax_error = 1
end

flags FunctionFlags type=u32 c_bitfields=true
  bit exported = 0
  bit suspending = 1
end

struct IrFunction size=72 align=8
  field flags type=u32 offset=64 flags=FunctionFlags
end
import status symbol=frontend_status return=i32 error=no_failure enum=Status
  param result type=ptr<FrontendResult> nullable=true ownership=borrowed access=read
end
```

- `const NAME type=INT value=INTEGER` is one line. `NAME` is `[A-Z][A-Z0-9_]*`. `INT` is one of
  `i8 u8 i16 u16 i32 u32 i64 u64 isize usize`; `bool`, `char`, floats and `void` are rejected. A value is decimal
  or `0x` hex (a magnitude) and must fit the carrier.
- `enum GROUP type=INT` lists `value name = INTEGER` lines and `flags GROUP type=INT` lists
  `bit name = INDEX` lines, each closed by `end`. Group names follow the struct-name rule and join the declared
  types; value and bit names are `[a-z][a-z0-9_]*`. A flag set needs an unsigned carrier for `c_bitfields=true`.
- A struct `field`, an import or export `param`, a return (`enum=`/`flags=` on the header) and a callback
  parameter or return may carry `enum=GROUP` or `flags=GROUP` (at most one) when its type is the group's carrier.
  Parameters write the options before any fact.
- **Narrowed field references.** A struct field's `enum=`, and a `when ... enum=` case, may also name an enum
  whose integer carrier is narrower than the field's integer type, when the field's type represents every
  declared value (always, when both are unsigned or both signed; a signed enum with a negative value on an
  unsigned field is rejected). One code table then serves a `u32` field and a `u64` operand alike. The field
  keeps its raw integer type in C, Rust and Java, and readers check that the raw value fits the enum's
  carrier, so upper bits never alias a declared code: Rust gets `<field>_as_<enum>()` returning
  `Option<Enum>` (`None` when the carrier cannot hold the value; casts and a compare, no panic), the Native
  Image `<field>As<Enum>(record)` case accessor returns `null` for such a value as for an unlisted one, and the
  enum gets `int` switch keys (below). Flag sets, parameters and returns keep the exact carrier.
- **Enums are open sets.** A value outside the list is legal: the name functions return `NULL`/`None`/`null`,
  the Java `<Enum>.of(v)` returns `null`, and flag sets expose the unknown bits (`hasUnknown`). Nothing checks a
  returned value against the list, and a reference adds no optimizer fact (`range` would let LLVM delete a
  caller's check for a value the list omits). A reference changes types and names only.
- **Constants are compile-time literals in every backend.** C gets typed `#define`s (`INTn_C`/`UINTn_C`),
  carrier typedefs, name functions and `<GROUP>_ALL` masks in `seam.h`; Rust gets `pub const`s, a
  `#[repr(transparent)]` newtype per group with associated consts and `name()`, and flag operators; Java gets
  `static final` primitives in `<Class>Constants`. There are no globals in `seam.ll`, no Java `enum` in
  `<Class>Constants`, and nothing to link or load.
- `c_bitfields=true` on a flag set is opt-in. It adds a `struct <Group>Bits` and turns each annotated field in
  `seam.h` into an anonymous union of the typedef and the bits, for C code and for Native Image `@CBitfield`
  accessors. Offsets and sizes are unchanged. It costs an extra shift and a narrower load over a mask test, so
  leave it off unless a reader wants named bits.

### Bit ranges

A flag set may also pack small numbers into ranges of bits beside its one-bit flags:

```text
flags ClassFlags type=u32
  bit open = 0
  bit interface = 1
  bit external = 2
  range kind = 8..15 enum=ExceptionKind
  range depth = 16..19
end
```

- `range name = LOW..HIGH` declares the bits `LOW` to `HIGH` inclusive (`HIGH - LOW + 1` bits) inside the
  carrier. Ranges and bits may not overlap. A flag set may consist of ranges alone.
- `enum=ENUM` is optional. The enum's carrier must hold every value of the width (an unsigned carrier of at
  least that many bits, a signed one of more), and each declared value must be non-negative and fit the width.
- Range names follow the bit-name rule and share a namespace with the set's bits, including each range's
  derived `<name>_shift` and `<name>_mask`. `all` and `none` are reserved, and a range may not be named after a
  Rust flag or derived trait method (`contains`, `intersects`, `union`, `when`, `clone`, `clone_from`, `eq`, `ne`, `hash`, `hash_slice`, `fmt`, `bitor`, `bitand`, `assert_receiver_is_total_eq`) or take another member's Java getter or `with`
  setter name.
- Every backend that emits bit constants also emits `<GROUP>_<NAME>_SHIFT` (`LOW`) and `<GROUP>_<NAME>_MASK`
  (the range in place), typed with the carrier. `<GROUP>_ALL` includes every range, so `hasUnknown` means a bit
  outside every declared bit and range.
- C: `static inline` getter `<module>_<group>_<name>(flags)` and setter
  `<module>_<group>_with_<name>(flags, value)`, which returns the new word. With `enum=` they use the enum's
  typedef. With `c_bitfields=true` the bits struct gains a member of the range's width, and Native Image a
  `@CBitfield` getter and setter typed as `<Class>Constants` types the range: the enum's Java carrier with
  `enum=`, otherwise the flag carrier's Java type (`byte`, `short`, `int` or `long`, the same signed
  primitive for signed and unsigned carriers). The read takes the unsigned bit-field's value and narrows it to
  that type, so a value with the type's top bit set reads negative, exactly as the `<Class>Constants` literal
  of an unsigned carrier does (`u8` 255 is `(byte) -1`), and the two compare equal with `==`. A write stores
  the value's low bits.
- Rust: `fn <name>(self)` returns the enum newtype (built directly, every value is valid) or the carrier, and
  `fn with_<name>(self, value) -> Self`. Java `<Class>Constants.<Flags>`: `<name>(flags)` and
  `with<Name>(flags, value)`, with the enum's carrier as the value type.
- Setters mask the value to the width without checking it. Truffle `constants()` exposes `<name>_shift` and
  `<name>_mask` in the flag set's object. The canonical JSON records `ranges` under a flag set only when it has
  some, so sets without ranges keep their fingerprints.

### Fields whose meaning depends on a tag

A field of a tagged record can name the group that applies for each value of the record's discriminant:

```text
struct Node size=80 align=8
  field tag type=u32 offset=0 enum=NodeTag
  field operand type=u64 offset=24
    when tag=binary enum=BinaryOp
    when tag=unary enum=UnaryOp
  field flags type=u64 offset=56
    when tag=write flags=WriteFlags
end
```

- `when TAG=VALUE enum=ENUM` or `flags=FLAGS` lines directly follow the `field` line they annotate. `TAG` is
  another field of the same struct that carries `enum=`, and `VALUE` one of that enum's values. A field's
  `when` lines all name the same tag field, name each value at most once, and reference groups whose carrier is
  the field's type (or, for an enum, [narrower](#constants-enums-and-flag-sets) than it). A field with `when` lines carries no `enum=`/`flags=` of its own.
- Nothing changes in the layout or the ABI, and nothing checks the tag: an accessor reads the field as the
  named group whatever the tag holds. The caller reads the case the tag selects.
- Rust: `<field>_as_<group>()` on the record (`operand_as_binary_op()`), one per field and group, returning
  the group's newtype, or `Option<Enum>` for an enum narrower than the field (`None` when the raw value does
  not fit the enum's carrier). Native Image: a static `<field>As<Group>(record)` on the class for each enum case
  (`operandAsBinaryOp(node)`), returning the `@CEnum` or `null` for an unlisted value, like `<name>Enum`.
  Flag cases keep the raw `@CField`, tested with `<Class>Constants`. The `@CField` and the FFM offset name
  each case in a comment. C and Truffle cells are unchanged.
- The `@CEnum` accessor is a convenience with the cost of `<name>Enum`: an `==` against a constant folds
  once inlined, but a `switch` on the result also pays `of`, the enum's ordinal and javac's switch map. Hot
  paths should switch on the raw field, `record.<field>()`, with the `<Class>Constants` literals as labels,
  which compiles to immediate compares, when the field and the enum share a carrier. For a 64-bit or narrowed
  enum, switch on `<Enum>.key(raw)` with the `_KEY` labels instead (below): casting the raw field to `int`
  aliases a wide value onto a declared code.
- **Switch keys.** Java cannot `switch` on a `long`, and a narrowed field's value is wider than its enum. One
  rule covers both: an enum gets `int` switch keys in `<Class>Constants.<Enum>` when its carrier is 64 bits
  wide (`i64`, `u64`, `isize`, `usize`) or a struct field references it narrowed, and every declared value is
  an `int` other than `Integer.MIN_VALUE` (`0..Integer.MAX_VALUE` unsigned, so a `u32` enum with a larger value
  gets none). Keys are a `public static final int <VALUE>_KEY` per value, holding the value's number (`u8`
  255 is `255`, though the enum's own constant is `(byte) 255`), and `public static int key(long value)`, which
  returns `(int) value` when the enum's carrier holds `value` and an `int` label can name it, and
  `Integer.MIN_VALUE`, which no label equals, otherwise. A raw value whose low bits equal a code therefore
  cannot alias its label, from any field width: `switch (UnaryOp.key(record.operand())) { case
  UnaryOp.NEGATE_KEY -> …; default -> … }` compiles the same over a `u32` field and a `u64` operand. It is
  branch-free in source and allocates nothing. Other enums get nothing new. In a keyed enum a value may not be
  named `key`, nor `<value>_key` beside `<value>`.
- The canonical JSON records a field's `cases` (`tag`, `value`, and `enum` or `flags`) only when it has some,
  so they are part of the fingerprint.

The canonical JSON gains `constants`, `enums` and `flagSets` sections and `enum`/`flags` keys on references.
Entries are sorted by name and values are decimal strings, so the fingerprint is stable.

The Native Image class gains `<name>Enum` methods for `enum=` returns (the plain import still returns the
carrier) and typed overloads for `enum=` parameters, a nested `@CEnum` Java enum per enum, and `@CConstant`
accessors for constants that fold to the literal. `<Class>Constants` carries the literals and, per group,
`<Enum>.name(v)` or `<Flags>.<bit>(flags)` and `hasUnknown`. See
[the C context](#native-image-c-context-library-linkage-and-handwritten-callbacks) for what an image needs.

## Persisted formats

`family`, `format`, `node`, `element`, `kind` and `union` declare versioned on-disk formats of tagged nodes with
backward references, separate from the call-boundary types above. Myna generates a canonical writer and a
validating reader for Rust (`formats.rs`) and Java (`<Family>Nodes`, `<Family>Writer`, `<Family>Exception`,
`<Format>Reader`). Each format carries a layout fingerprint that `pinned=` locks. See
[persisted formats](formats.md).

## Determinism

Declarations and fact keys are sorted, parameter/field order is preserved, and output includes no timestamps or input paths. `seam.abi` is the SHA-256 of canonical `seam.json` including target ABI class, generator/schema version, ABI shape, Java adaptation, and fact provenance. It intentionally changes when contracts change, even if the C signature remains identical. Constants, enum values and flag bits sort by name, and numbers are canonical decimal strings, so a hex or decimal spelling in the source does not change the fingerprint. Fingerprints detect drift; they do not prove semantic correctness or authenticate a provider binary.

## Native Image leaf-call policy

Imports default to the binding ABI 2 leaf contract: short, bounded, nonblocking execution with no Java reentry. Use `native_leaf=false@explicit_contract` to opt out and retain a normal `@CFunction` transition. Explicit `native_leaf=true@explicit_contract` is accepted but redundant. Exports retain normal isolate entry; `noreturn` imports must opt out. The effective policy is recorded in schema 3 JSON as `nativeCallPolicy`, with a separate `bindingAbiVersion: 2`; the module's `abi` remains the consumer's own protocol version. Leaf policy changes fingerprints but emits no LLVM attributes. See [migration](binding-abi-2.md) and [performance](performance.md).

## Resource lifecycles and call borrows

`resource NAME representation=native threading=confined|shared create=FUNCTION[,FUNCTION…] destroy=FUNCTION` declares a unique native resource lifecycle, with one constructor or [several](ownership.md#several-constructors). `NAME` must already be an opaque type — or, for the `handle=PARAMETER` form below, the struct the create initializes in place. With `handle=PARAMETER.FIELD` or `handle=PARAMETER` and `success=N[,N…]`, the create reports a status and writes the resource through that parameter instead of returning it (a field of the struct it points to, or that struct itself, which `NAME` then names); only Truffle cells generate an owner for those forms. `borrow FUNCTION PARAMETER length=PARAMETER lifetime=call` declares a complete non-escaping byte-buffer contract; `lifetime=resource:NAME` instead declares a read-only buffer that the create of resource `NAME` keeps until that resource is destroyed (Rust, FFM and Native Image owners, or a Truffle cell for a `handle=` create; see [borrows held by a resource](ownership.md#borrows-held-by-a-resource)). `borrow FUNCTION SOURCES lengths=LENGTHS count=COUNT lifetime=call [elements_nullable=true]` declares a [batch](ownership.md#batches-of-buffers): an array of read buffers with a parallel array of lengths. A function may carry several `borrow` lines, each naming a buffer and a length of its own; its other parameters may be scalars or pointers, and its result a scalar or void, or the resource pointer of a [constructor](ownership.md#constructors-with-call-scoped-buffers). A buffer that is `nullable=true` (the default for a pointer) may be absent, and then reaches the function as the null pointer with length zero; `nullable=false` declares one that is always present. These opt into generated ownership APIs and stronger validation; metadata-only `ownership=` on a pointer does not do so by itself. See [ownership.md](ownership.md) for exact obligations, supported signatures, and the consumer-implemented Native Image API.

## Native Image C context, library linkage, and handwritten callbacks

The generated Native Image class is always annotated `@CContext(<Class>Directives.class)`. The generated `<Class>Directives` embeds this generation's `seam.h`, which `@CStruct` records and `@CConstant` accessors need at image build, and writes it once per image-builder JVM to a temporary `myna-seam*` directory that is removed when the builder exits. Every image that compiles the Native Image class must therefore also compile `<Class>Directives.java`, because it is that class's `@CContext`. If a `c_context` is named, compile that class too. Its headers arrive first. The embedded query header supplies the generated types, layout checks, declared constants and fingerprint. The standalone `seam.h` also publishes the function prototypes and module ABI version, which the consumer can declare independently in its context. FFM-only, JNI-only and Truffle-FFM-only images need nothing new.

The header check is automatic and needs no flag: there is no `--features` option and nothing in `native-image.properties`. The Native Image class declares `@CConstant("<MODULE>_SEAM_ABI_<fingerprint>") static native int seamHeaderCheck()`, where `<fingerprint>` is its `ABI_FINGERPRINT`, and only the `seam.h` of the same generation defines `#define <MODULE>_SEAM_ABI_<fingerprint> 1` (beside the `<MODULE>_SEAM_ABI` string macro). The image builder resolves every C element of the Native Image class in one C query against its `@CContext`, so when the Java and the embedded header come from different generations the query fails to compile and the build stops with `Error compiling query code`, naming `<MODULE>_SEAM_ABI_<fingerprint>` as undeclared. The check runs in exactly the image builds whose class path holds the Native Image class, the builds that need `seam.h` already; FFM-only, JNI-only and Truffle-FFM-only images never compile or load `<Class>Directives`. Nothing calls `seamHeaderCheck()`, so no binding changes: leaf status, transitions and machine code are those of a build without the check, and the macro value never reaches the image. The builder needs a C compiler for the query, as it already does for linking; otherwise prepare a CAP cache with `-H:+UseCAPCache` from the same generation.

Only the image builder uses `<Class>Directives`, and only because it is the Native Image class's `@CContext`. No run-time code reaches it, so the header text never enters an image. The check catches Java files from different generations mixed together. It does not detect a provider compiled against an older `seam.h`, because the embedded header comes from the same generator run as the Java.

Module options `c_context=qualified.DirectivesClass` and `c_library=library_name` add a consumer C context and `@CLibrary`. They can be used independently. `c_context` is no longer the annotation value. Each instance of the generated context creates its own instance of the named class through its public no-argument constructor. The generated context delegates `isInConfiguration`, `getMacroDefinitions`, `getOptions`, `getLibraries` and `getLibraryPaths` to it. Its `getHeaderFiles()` returns the consumer's headers first, then `seam.h`, so a consumer context must not include `seam.h` itself. `c_context` may not name the generated `<Class>Directives`. `c_library_static=true` requests static linkage and requires `c_library`; its default is false. These options require at least one native import.

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

The consumer supplies the public `CContext.Directives` implementation, with a public no-argument constructor, on the image builder's classpath and on the classpath that compiles `<Class>Directives.java`. It can provide headers, macros, library search paths, and other build settings. `c_library` is the library name without the platform prefix/extension, as expected by Native Image. Annotations are on the class, so they also cover private native methods generated for small-integer lowering. See [CContext](https://www.graalvm.org/sdk/javadoc/org/graalvm/nativeimage/c/CContext.html) and [CLibrary](https://www.graalvm.org/sdk/javadoc/org/graalvm/nativeimage/c/function/CLibrary.html).

`java_type` on a callback references an existing fully qualified Java function-pointer interface. The generator uses that type in Native Image parameters, returns, and explicitly requested entry-point literals, and **does not emit a replacement interface**. The handwritten interface must extend `CFunctionPointer` and supply its own `@InvokeCFunctionPointer` method with the declared ABI. The descriptor remains the signature source for C, Rust, FFM, and LLVM; keeping the handwritten Java signature consistent is the consumer's obligation, since descriptor validation does not load consumer classes.

Omit `callback=` on an export if its entry-point literal should also remain handwritten. Existing callback declarations without `java_type` continue generating nested interfaces. Callback retention and lifetime management remain the consumer's responsibility.

Both linkage configuration and handwritten type mappings are serialized and fingerprinted; target overrides preserve them. The executable [Bemo integration fixture](../tests/bemo/bemo.seam) links a static Rust archive using only the generated annotations and consumer directives, then calls back through a handwritten interface and literal.

## Truffle cells

`truffle_cells=true` on the module generates `<NativeClass>Truffle.java`: one Truffle cell per import, calling the module's existing bindings with language-neutral coercions from `myna-truffle`. `truffle_backend=native|ffm`, `truffle_coercion=strict|wrap` and `truffle_transitions=direct|boundary` refine it and require the option. Per import, `truffle_cell=false@explicit_contract` opts out (required for by-value records and callback parameters) and `truffle_raw_pointers=true@explicit_contract` accepts guest numbers as pointers. Per byte-pointer parameter, `truffle_buffer=true@explicit_contract` lends raw bytes (no terminator, heap arrays pinned when large) where the parameter would otherwise be treated as a C string; it is a provider fact, not an LLVM attribute. The configuration is serialized under `"truffle"` and fingerprinted; descriptors without it keep their fingerprints. See [Truffle native cells](truffle-cells.md).

## FFM callback targets and static JNI

Callbacks may additionally declare `ffm_java=qualified.Class.method ffm_sentinel=INTEGER`. For an `i32` or `i64` status both fields are required. A `void` callback may declare `ffm_java` alone and must omit the sentinel. Its adapter discards exceptions; the target must record any failure the caller needs to observe. The generated FFM upcall invokes the public static target with FFM carriers, catches Java exceptions and, for status returns, returns the reserved sentinel. The hosted Feature registers this static target for AOT upcalls. These fields are independent of the SVM `java_type` interface mapping and are included in the ABI fingerprint.

Callbacks without a static FFM target still provide scoped factories accepting a Java callback and, for non-void returns, an explicit failure sentinel. Stub lifetime and thread access follow the supplied arena. Imports that call back must opt out of native leaves. See the [adapter guide](jvm-roadmap.md) for callback ownership and library lifetimes.

The CLI's optional `--jni` flag projects imports into static SVM JNI methods. It does not change the canonical C signature or introduce JNI handles into seam types. Declared `borrow` relationships additionally generate a direct `ByteBuffer` overload per function, taking one buffer for each borrow (`null` for a nullable buffer that is absent); raw pointers remain explicit unsafe `long` addresses. The adapter emits its own concrete JNI signatures and library lifecycle metadata.

### Variadic imports for Rust callers

Use `variadic_after` to name the final fixed parameter. Each import declares one
call shape. Several shapes can name the same native symbol when their fixed
parameters and return type agree.

```
import dbConfigInt symbol=sqlite3_db_config return=i32 variadic_after=op error=no_failure native_leaf=false@explicit_contract
  param database type=ptr<Database> ownership=borrowed
  param op type=i32
  param value type=i32
  param result type=ptr<i32> nullable=true access=write ownership=borrowed
end
```

Generate into a dedicated output directory with `myna generate INPUT.seam
--rust-only --out DIRECTORY`. This produces the descriptor, fingerprint and
Rust bindings for the entire seam. Each typed wrapper calls a C-variadic extern
function. Trailing arguments must use promoted types (`i32`, `i64`, `f64`, or
a pointer). The caller must select an operation that consumes exactly the
declared trailing types and uphold the pointer contracts. Java, JNI and LLVM
contract generation for variadic imports is unsupported.

Variadic imports expose an unsafe Rust call. Resource lifecycle and buffer
borrow directives cannot name these imports, and automatic C string wrappers
are omitted for them. Rust implementation traits require fixed-arity imports.

### Standalone Java constants

Run `myna generate INPUT.seam --out DIR --constants-only shared.tape` to generate
the constants source under `DIR/shared/tape/` in package `shared.tape`. Compile it into a separate jar using a plain JDK.
Its literals, enum helpers and flag operations depend only on the Java standard library.
The package choice applies to this output and leaves the descriptor fingerprint intact.
Use `--check` with the same arguments to verify the generated source.

### Reusing a live in-place resource

A confined resource with a struct handle may declare `reuse=FUNCTION` alongside
`handle=PARAMETER` and `success=...`. Use this for a returning, non-unwinding import
that takes the current contents of the struct, consumes them even on failure, and
reinitializes the same storage. Its handle is a non-null borrowed `read_write` pointer.
The provider must leave nothing requiring destruction on failure and must not retain
new input pointers beyond the call. The success codes have the same meaning as on create.

The Truffle cell takes an open owner of that resource and returns the native status.
Success keeps the same owner live. Failure closes the owner and releases the backing
struct without calling the destructor on the consumed value. A coercion or linkage
error before native entry leaves the original owner live. Close dependent owners before
reuse and discard borrowed views into the old contents. The low-level static binding
and cell `invoke` use raw carriers and leave those ownership obligations to their caller.
