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
- A module that declares `abi=N` emits it as `<MODULE>_ABI_VERSION` (C), `ABI_VERSION` (Rust) and
  `<Class>Constants.ABI_VERSION`, typed `i32`; `<Class>Constants` is generated for it alone if needed.
- A struct field can name the group that applies per value of a tag field with `when tag=binary
  enum=BinaryOp` lines (#134). Rust gains `<field>_as_<group>()` record accessors and Native Image
  `<field>As<Enum>(record)`; the layout and ABI are unchanged and nothing checks the tag (see
  [fields whose meaning depends on a tag](../dsl.md#fields-whose-meaning-depends-on-a-tag)).

## Counted element views

A `ptr<T>` record field may name the integer field counting its elements with `count=FIELD` (#133; see
[the descriptor format](../dsl.md#counted-element-views)). Rust records gain an `unsafe fn` `&[T]` accessor, FFM
gains `<Struct>_<field>(record)` and the `Objects.checkIndex`ed `<Struct>_<field>(record, i)`, and Native
Image gains `addressOf(long)` on element structs and an asserted, always-inlined `<Struct>_<field>At(record, i)`. Counts are `u8`, `u16`, `u32`, `u64` or `usize`; elements are structs, scalars, or pointers to a struct or opaque type (`ptr<ptr<T>>`). The C ABI is
unchanged; `seam.json` and the fingerprint record the relation, and descriptors without `count=` generate
the same output as before.

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
| 12 | In an enum with a 64-bit carrier whose values all fit an `int` (now given `int` switch keys), a value named `key`, or `<value>_key` beside `<value>`, is rejected. | such enums | rename the value |
| 13 | Rust names that collided in `seam.rs` and failed in rustc are now rejected at validation: a const (upper-cased), a function and an enum or flag set newtype of the same name. | descriptors that did not compile in Rust | rename one |

Not broken: FFM output (comments only), Truffle cell signatures and behaviour, leaf status of every
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
- **#123** (#125): `success=` values that the create's return scalar can never represent (so every create
  would fail at run time) are rejected at validation.

## Dependencies

- Elide 1.6.2 to 1.6.3, pinned to the released v1.6.3 assets. The toolchain stays 2026.11.6 and the Graal
  distribution 25.5.5-elide.28.
- Crate patch updates: `bumpalo` 3.20.4, `cc` 1.11.2, `errno` 0.3.15 and `bit-vec` 0.10.2.

Regenerate matching outputs together and rebuild native libraries against them. Binding ABI 2 and native
value ABI v3 are unchanged. No existing published artifacts or tags are replaced.
