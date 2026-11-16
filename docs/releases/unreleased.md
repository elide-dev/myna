# Myna (unreleased)

This release adds constants, enums and flag sets to the descriptor (#126), makes Native Image records
`@CStruct` interfaces, and fixes four reported problems. **It contains breaking changes for Native
Image consumers.** The C ABI, carriers, layouts, symbols, `seam.ll` contracts and `BINDING_ABI_VERSION`
(2) are unchanged. Fingerprints change for every descriptor, as in every release.

## Constants, enums and flag sets

`const`, `enum` and `flags` declarations name integer values, and `enum=`/`flags=` on fields,
parameters, returns and callback types reference them (see
[the descriptor format](../dsl.md#constants-enums-and-flag-sets)).

- **C**: typed `#define`s, carrier typedefs, name functions and `<GROUP>_ALL` masks in `seam.h`.
- **Rust**: `pub const`s and a `#[repr(transparent)]` newtype per group with associated consts,
  `name()` and flag operators. A newtype compiles to the same code as the raw integer.
- **Java**: `<Class>Constants` with `static final` literals, `<Enum>.name(v)` and `<Flags>.<bit>(flags)`. The
  Native Image class gains nested `@CEnum` enums, `<name>Enum` methods, typed overloads and `@CConstant`
  accessors that fold to the literal.
- **Truffle**: `constants()` and `<Enum>Names` (see below).
- Enums are open sets: undeclared values are legal and names return `NULL`/`None`/`null`. References add no
  optimizer facts. Constants are compile-time literals in every backend, with no globals and no Java enums
  in `<Class>Constants`.
- `@CBitfield` accessors and a C bitfield union in `seam.h` are opt-in per flag set with `c_bitfields=true`.
- Flag sets can declare bit ranges, `range kind = 8..15 [enum=Kind]` (#134), with `_SHIFT`/`_MASK`
  constants, getters and `with` setters in C, Rust and Java, and a `@CBitfield` member with
  `c_bitfields=true`. `<GROUP>_ALL` and `hasUnknown` count the ranges as declared bits (see
  [bit ranges](../dsl.md#bit-ranges)).
- A 64-bit enum whose values all fit an `int` gets `<VALUE>_KEY` labels and `key(long)` in
  `<Class>Constants`, so Java can `switch` on it (Java has no `switch` on `long`); see
  [tagged fields](../dsl.md#fields-whose-meaning-depends-on-a-tag).
- A struct field's `enum=` and a `when ... enum=` case may name an enum with a narrower carrier than the
  field when the field's type holds every value, so one code table serves a `u32` field and a `u64`
  operand. The field stays its raw integer; Rust gets `<field>_as_<enum>() -> Option<Enum>`, the Native Image
  case accessor returns `null` for a value the carrier cannot hold, and the enum gets the same `int` switch
  keys (see [narrowed field references](../dsl.md#constants-enums-and-flag-sets)). Descriptors that use it were
  rejected before, so existing output is unchanged.
- A module that declares `abi=N` emits it as `<MODULE>_ABI_VERSION` (C), `ABI_VERSION` (Rust) and
  `<Class>Constants.ABI_VERSION`, typed `i32`; `<Class>Constants` is generated for it alone if needed.
- A struct field can name the group that applies per value of a tag field with `when tag=binary
  enum=BinaryOp` lines (#134). Rust gains `<field>_as_<group>()` record accessors and Native Image
  `<field>As<Enum>(record)`; the layout and ABI are unchanged and nothing checks the tag (see
  [fields whose meaning depends on a tag](../dsl.md#fields-whose-meaning-depends-on-a-tag)).

## Counted element views

A `ptr<T>` record field may name the integer field counting its elements with `count=FIELD` (#133; see
[the descriptor format](../dsl.md#counted-element-views)). Rust records gain an `unsafe fn` `&[T]` accessor, FFM
gains read-only views `<Struct>_<field>(record)` and `(record, Arena owner)`, `<Struct>_<field>_count(record)`, the loop helper
`<Struct>_<field>_at(view, i)` and element forms, with every bad count or index an `IndexOutOfBoundsException`, and Native
Image gains `addressOf(long)` on element structs and an asserted, always-inlined `<Struct>_<field>At(record, i)`. Counts are `u8`, `u16`, `u32`, `u64` or `usize`; elements are structs, scalars, or pointers to a struct or opaque type (`ptr<ptr<T>>`). The C ABI is
unchanged; `seam.json` and the fingerprint record the relation, and descriptors without `count=` generate
the same output as before.

## Sized struct pointers in FFM (#135)

Java FFM types `ptr<Struct>` parameters, returns and fields of imports, callbacks and structs, and the
out slot of a struct resource's out-pointer constructor, with a
new `<Struct>_POINTER` constant, `ValueLayout.ADDRESS.withTargetLayout(<Struct>_LAYOUT)`. The constant
is emitted only for structs some pointer targets. An import's `ptr<Struct>` result, a callback's
`ptr<Struct>` argument and `record.get(<Struct>_POINTER, offset)` are segments of the struct's size, so
records and their counted views need no `reinterpret`. **A null one is sized too**, and reading through
it is an unchecked native read that crashes. Test `address() == 0` first (see breaking change 15).
Pointers to opaque types, scalars and pointers, a counted view's pointer, and pointer-array elements stay
zero-length, and Truffle FFM cells are unchanged. Every pointer field inside a cycle of pointer fields
(`next: ptr<Node>`, or a mutual cycle) targets a padding layout of the pointee's size and alignment.
Layouts are declared pointees first (see [types and layouts](../dsl.md#types-and-layouts)). Layouts stay
static final constants, with no work per call. The C ABI, `seam.json` and fingerprints are unchanged.

## Resources with several constructors (#139)

- `create=` takes a comma-separated list of imports. Rust, Native Image and FFM owners gain one
  constructor per import, named after it; a resource with one constructor keeps `create` and
  generates the same output, and `seam.json` lists constructors only when there are several (see
  [several constructors](../ownership.md#several-constructors)).
- A resource constructor, including one that returns the resource pointer, may declare call-scoped
  buffers. Its owner constructors take the buffers and lend them for the call only, as every call
  borrow wrapper does; Truffle cells lend the guest's buffer. Call borrows on any other
  pointer-returning function stay rejected.
- A resource destroy may take a `nullable=true` parameter; no generated owner passes it null.
- Ownership and call-buffer validation errors name the function and parameter they concern.
- Adopting these in an existing descriptor changes Truffle cell entry points, as declaring a resource
  or a borrow always has:
  - a function that becomes a resource's `destroy` loses its raw cell entry (`release_Cell.invoke(long)`),
    since a resource is destroyed through its owner. Code that holds a constructor cell's raw address
    frees it through `<Class>Native.<destroy>(WordFactory.pointer(address))`;
  - a buffer that becomes a borrow drops its length argument from the cell (`syntax_Cell` goes from
    `(buffer, length)` to `(buffer)`). Cells take `Object[]`, so an old two-argument call fails at run
    time in the cell's arity check, not at compile time.
- `rust_implementation=trait` on the module generates, for Rust code that *implements* the imports,
  a safe `<Module>Implementation` trait and `define_implementations!`, which defines every
  `extern "C"` entry point from an implementing type. Call borrows arrive as slices, returned
  resources as associated types owned through `Box`, and everything else as declared. Modules
  without the option generate the same output (see
  [implementing imports in Rust](../ownership.md#implementing-imports-in-rust)).

## Status-plus-out-pointer constructors, `usize` lengths, C strings (#141)

- A resource's `handle=PARAMETER` may name a `ptr<ptr<R>>` out-parameter: the "status plus handle"
  constructor. FFM and Native Image owners take an `onFailure` callback for any status outside
  `success=`, Rust returns `Result<R, Status>`, and Truffle cells adopt it as other status creates
  (see [status plus out-pointer](../ownership.md#status-plus-out-pointer)).
- A buffer length may be `usize` (`size_t`), treated as 64-bit unsigned in every wrapper.
- A constructor that keeps its read-only input (`lifetime=resource:`) is wrapped beyond Truffle cells:
  Rust owners carry the buffer's lifetime (`Doc<'held>`) behind a closure-scoped safe form
  (`Doc::with_create`) and an unsafe owner constructor, and FFM and Native Image copy the bytes
  into memory the owner frees after the destroy. A Truffle cell holds a buffer only for a `handle=`
  create (see [borrows held by a resource](../ownership.md#borrows-held-by-a-resource)).
- `access=cstring` declares a read-only NUL-terminated `ptr<u8>` with no length: `&CStr` in the
  Rust wrappers and implementer trait, a `String` overload in FFM, a guest string in Truffle cells
  (see [C strings](../ownership.md#c-strings)).
- An import may no longer be named `NAME_address` with no parameters beside an import `NAME`: it
  would collide with that binding's accessor.
- The Native Image class gains `NAME_address()` per import too, from the linked `CGlobalData` symbol
  read at run time; its `native-image.properties` now initializes `MynaSymbols` at build time
  whenever the module has imports.
- `LibraryBinding.NAME_address()` exposes the address an FFM binding resolved for each import,
  from the same lookup, for symbols handed to native code rather than called.

## Batches of borrowed buffers (#132)

`borrow F SOURCES lengths=LENGTHS count=COUNT lifetime=call [elements_nullable=true]` declares an
array of read buffers with a parallel array of lengths: `SOURCES[i]` holds `LENGTHS[i]` bytes for
`i < COUNT`, each read during the call only (see
[batches of buffers](../ownership.md#batches-of-buffers)). One import may declare several beside
single buffers.

- **Rust**: the wrapper takes `&[&[u8]]` (`&[Option<&[u8]>]` with nullable elements) and builds both
  arrays on the stack for up to eight buffers and panics above that; a `_with_scratch` form, on the
  wrapper and on a resource constructor over a batch import, takes caller scratch for more.
  Nothing allocates, so `seam.rs` builds in `no_std` crates without an allocator. `seam.rs` gains
  the `BufferBatch` and `OptionalBufferBatch` views an implementation builds from the raw arrays.
- **Native Image**: `NativeBuffer[]` and `byte[][]` overloads that acquire or pin each element under the
  single-buffer rules, from one snapshot of the array, with the arrays on the stack for up to eight
  elements.
- **FFM**: a `MemorySegment[]` overload, each segment validated as a single buffer is, lent in place
  only when nothing can close it during the call and copied otherwise.
- The empty batch passes null arrays with count zero. `seam.h` and `seam.ll` are unchanged: the arrays
  carry the `nocapture` and `readonly` facts they declare. Static JNI keeps the raw binding.
- **Truffle cells** take a guest array of buffers or strings, or null for the empty batch, and lend
  each element under the single-buffer rules, with both arrays on the scratch stack. FFM cells lend
  a confined segment in place under a native leaf, as the host wrapper does, checking it again right
  before the call since coercing later elements may run guest code, and copy other scoped
  native elements onto the scratch stack, since a downcall cannot hold their scope. A compiled call
  allocates nothing beyond its elements' own lending. `BufferArray` passes a batch from host code.

## Breaking changes

| # | Change | Affects | Migration |
| --- | --- | --- | --- |
| 1 | Native Image records are `@CStruct` interfaces. `MynaNative.IrFunction.flags(record)` becomes `record.flags()`; the nested static accessor classes are gone; `SIZE` and `ALIGNMENT` move to the interface. A record field named `isNull`, `isNonNull`, `rawValue`, `equal`, `notEqual` or `addressOf<Other>` is rejected. | every descriptor with a `struct` | call on the receiver |
| 2 | `ptr<Struct>` and `ptr<Opaque>` parameters and returns of `@CFunction`s, callbacks, export targets and ownership wrappers use the struct or opaque interface instead of `PointerBase`. | every descriptor passing struct or opaque pointers | `WordFactory.pointer(...)` still infers the type; declared variables and export signatures need the new type |
| 3 | The Native Image class is always `@CContext(<Class>Directives.class)`. A consumer `c_context` is instantiated and delegated to, not the annotation value, and must not add `seam.h` itself. | every Native Image consumer | none for most; a `c_context` needs a public no-argument constructor |
| 4 | New generated files: `<Class>Directives.java` (always) and `<Class>Constants.java` (when constants, enums or flags are declared, or the module line names `abi=`). An image that compiles the Native Image class must also compile `<Class>Directives.java` (the `@CContext`, with `seam.h` embedded in segments of at most 16 KiB, read only at image build) and any `c_context` delegate. The image build then checks automatically that `seam.h` is the bindings' own generation; no `--features` flag. FFM-only, JNI-only and Truffle-FFM-only images need nothing new. | images with the Native Image class; consumers that check generated Java in | add the files |
| 5 | Rust: annotated struct fields, `extern` signatures, `<Name>Fn` and callback aliases use the newtype, and `assert_implementations!` requires it. | descriptors that add `enum=`/`flags=` | wrap or unwrap with `.0`, or use the constants |
| 6 | Annotated imports gain `<name>Enum` methods and typed overloads. A descriptor whose names already end in `Enum` next to an annotated import is rejected, as are declared types named `CEnum`, `CBitfield` and other SVM names. | descriptors that add `enum=` | rename |
| 7 | `myna-truffle` gains `NativeConstants`; `<Class>Truffle` gains `constants()` and `<Enum>Names`. `constants` and the `<Enum>Names` classes are reserved. | Truffle consumers | none unless a cell is named `constants` |
| 8 | `seam.h` uses carrier typedefs in annotated fields and prototypes. | descriptors that add references | source-compatible in C and C++ |
| 9 | `@CBitfield` accessors and the C bitfield union exist only with `c_bitfields=true` on the flag set. | descriptors using flag sets | add the option to ask for them |
| 10 | `align=N@explicit_contract` is accepted on borrow buffers (a power of two, at most 4096). | descriptors that declare it | none; see the fixes below |
| 11 | A module that names `abi=` gains `ABI_VERSION` (C `<MODULE>_ABI_VERSION`, Rust, `<Class>Constants`), and `<Class>Constants.java` is generated for it even without other constants. With `abi=`, a `const ABI_VERSION`, or a function, enum or flag set named `ABI_VERSION`, is rejected. | descriptors with `abi=`, including those that worked around its absence with `const ABI_VERSION` | use the generated `ABI_VERSION`; remove the const |
| 12 | In an enum given `int` switch keys (a 64-bit carrier, or a narrowed field reference, with values that all fit an `int`), a value named `key`, or `<value>_key` beside `<value>`, is rejected. | such enums | rename the value |
| 13 | Rust names that collided in `seam.rs` and failed in rustc are now rejected at validation: a const (upper-cased), a function and an enum or flag set newtype of the same name. | descriptors that did not compile in Rust | rename one |
| 14 | An import named `NAME_address` with no parameters beside an import `NAME` is rejected: FFM bindings gain a `NAME_address()` accessor per import. | descriptors with such a pair of imports | rename one |
| 15 | `NativeBytes` buffer messages over a closed arena, or a confined arena's memory on another thread, throw `NativeCellException` (a guest error a guest `try`/`catch` handles), with the `IllegalStateException` or `WrongThreadException` the segment accessors throw as its cause. HotSpot previously let those raw host exceptions escape (an internal error to guest languages), and a Native Image did not check at all (#128). | hosts that caught `IllegalStateException`/`WrongThreadException` from `NativeBytes` messages | catch `NativeCellException`, or inspect `getCause()` |
| 16 | FFM `ptr<Struct>` results, callback arguments and field reads are segments of the struct's size, not zero-length. **A null one is a sized segment at address 0, so a read through it, or through a dangling pointer, is an unchecked native read that crashes the process where it used to throw `IndexOutOfBoundsException`.** A pointer misaligned for the struct, including a sentinel such as `(T *)-1`, is an `IllegalArgumentException` after the call completes. Structs that a pointer targets gain `<Struct>_POINTER`, and struct layouts can be declared in a different order. The FFM class of such a module calls the restricted `withTargetLayout` when it is initialized, so with `--illegal-native-access=deny` it needs native access even before its first downcall. | FFM consumers of struct pointers | test `address() == 0` or `equals(MemorySegment.NULL)` before reading, never `byteSize() == 0`; existing `reinterpret` calls still work; declare sentinel-carrying pointers `ptr<void>`; enable native access for the module, as downcalls already require |

Not broken: FFM output apart from row 16 (otherwise additions and comments only), Truffle cell signatures and behaviour (except row 15), leaf status of every
import, export entry-point signatures at the C level.

### The header check

The check is automatic in every image that uses the Native Image bindings and needs no flag; nothing is
registered in `native-image.properties` and there is no `--features` option. `<Class>` declares
`@CConstant("<MODULE>_SEAM_ABI_<fingerprint>") static native int seamHeaderCheck()`, and only the `seam.h`
of the same generation defines that macro. When the Java and the embedded header come from different runs,
the image build's C query fails (`Error compiling query code`, naming `<MODULE>_SEAM_ABI_<fingerprint>`).
Nothing calls the method, so bindings keep their leaf status and code. `<Class>Directives` is only a
`CContext.Directives`, touched only through `<Class>`'s `@CContext`; FFM-only, JNI-only and
Truffle-FFM-only images need nothing new. The check needs a C compiler at image build (or a CAP cache)
and catches mixed generations of the generated Java, not a provider compiled against an older header.
`seamHeaderCheck` is reserved: a function or const accessor of that name is rejected.

### Before and after

Record accessors in Native Image:

```java
// before
int flags = MynaNative.IrFunction.flags(record);
MynaNative.IrFunction.flags(record, flags | 1);
// after
int flags = record.flags();
record.flags(flags | 1);
```

Rust, for a field or import that references `flags=FunctionFlags`:

```rust
// before
unsafe extern "C" { fn ir_function_flags(f: *const IrFunction) -> u32; }
if (flags & 4) != 0 { /* external */ }
// after
unsafe extern "C" { fn ir_function_flags(f: *const IrFunction) -> FunctionFlags; }
if flags.contains(FunctionFlags::EXTERNAL) { /* external */ }
let raw: u32 = flags.0;
```

### Known limitation

Narrow integer `@CField` reads (`i8`, `u8`, `char`, `i16`, `u16`) carry one extra register sign-extension
today. It is tracked upstream as elide-dev/graal#239; `@CField` is kept and a test pins the instruction.

## Truffle

- `constants()` returns a `NativeConstants` object that folds in compiled code. Values use the guest
  carriers cells return: `u8` and `u16` are `int`, `u32` is `long`, `u64` is its raw bits. The member
  lookup cache keys on string equality.
- `<Enum>Names.name(value)` returns a `TruffleString` or `null`, with overloads for the guest carriers.
- A Truffle class with no cells compiles; its `function(String)` always throws `IllegalArgumentException`.

## Fixes

- **#120 (alignment)**: every copy a borrow lends starts on a 16-byte boundary. `align=N@explicit_contract`
  on a borrow buffer is a caller contract: it is emitted in `seam.ll`, Truffle cells copy a buffer that
  violates it, and other wrappers check it with a Java `assert` or Rust `debug_assert!` that compile out in
  release builds. Empty buffers get aligned dummies, and Native Image `byte[]` overloads copy when the
  alignment is above 8. The second part of the report (TruffleData UTF-16 and `text_out`) was not
  reproduced. If it persists, declare `align=2@explicit_contract` on `data` and `text_out`, pass UTF-16 as
  bytes rather than guest strings, pass a null `text_out` unless `WANT_TEXT` is set, and report the exact
  failing call.
- **#121**: a failed create reports its `NativeStatusException` even when copying the guest buffer back fails.
- **#122** (#124): a Truffle FFM cell over a create with one held borrow and two or more call borrows
  generated Java that did not compile. Such a create now skips the in-place fast path.
- **#128**: in a Native Image, `NativeBytes`' scalar buffer messages (`read/writeBuffer{Byte,Short,Int,Long,Float,Double}`)
  read and wrote the segment with word operations and no scope check, so a guest could touch freed memory
  once a confined arena was closed. Every buffer message now checks the scope first, in both runtimes, and
  fails with a `NativeCellException` whose cause is what the segment's accessors throw (breaking change
  14). The check covers confined arenas fully. It does not protect a shared arena closed by another
  thread: in compiled code one check may cover a whole loop of reads. Embedders keep shared arenas open
  while guest code can reach the buffer.
- **#123** (#125): `success=` values that the create's return scalar can never represent (so every create
  would fail at run time) are rejected at validation.

## Dependencies

- Elide 1.6.2 to 1.6.3, pinned to the released v1.6.3 assets. The toolchain stays 2026.11.6 and the Graal
  distribution 25.5.5-elide.28.
- Crate patch updates: `bumpalo` 3.20.4, `cc` 1.11.2, `errno` 0.3.15 and `bit-vec` 0.10.2.

Regenerate matching outputs together and rebuild native libraries against them. Binding ABI 2 and native
value ABI v3 are unchanged. No existing published artifacts or tags are replaced.
