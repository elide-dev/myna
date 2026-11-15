<!-- Decisions recorded 2026-11-15, overriding the recommendations below where they differ:
     1. @CBitfield: generated as an opt-in per flag set (`c_bitfields=true`), not omitted (section 12, decision 6).
     2. Imports with enum= results: the plain name stays the raw leaf binding; `<name>Enum` is the typed form (decision 4).
     3. Typed opaque pointers (`interface Opaque extends PointerBase`) ship in the same breaking release (decision 7).
     4. No linkable LLVM constants (decision 2). All other recommendations in section 12 are accepted as written.
     Direction: use the best Native Image and Truffle APIs; API-breaking bumps are acceptable. -->

# Design: named constants, enums and flag sets in the seam descriptor (myna#126)

Status: design only, against `main` at 0.18.0. Nothing in the repository was changed.
Experiments ran in `mktemp -d` directories with the pinned toolchain (clang 23.1.4, rustc 1.101.0-nightly / LLVM 23.1.3, the pinned GraalVM `javac`/`native-image`); commands and key output are in sections 7a and 8.

Revision 3 (current). Direction from the user: use the best Native Image and Truffle APIs, breaking generated-API changes are allowed. The C interface (`@CStruct`/`@CField`, `@CEnum` and its companions, `@CConstant`) is now first-class Native Image output. Every one was measured on the pinned fork (section 7a); where one costs something, the design keeps it and puts a faster form beside it, and the numbers are reported. Section 13 lists every API break. The C ABI (carriers, layouts, symbols) is unchanged throughout.

## 1. Summary

- Three new declarations: `const`, `enum … end`, `flags … end`; two new reference options, `enum=` and `flags=`, on `field`, `param`, and `import`/`export`/`callback` headers (the return).
- **C and Rust**: typed macros plus a carrier `typedef` per group in C; `#[repr(transparent)]` newtypes with associated constants in Rust, now used as the type of annotated struct fields and function signatures (breaking, ABI-identical).
- **Java, all runtimes**: a dependency-free `<Class>Constants.java` of literal `static final` primitives, flag predicates and name functions. This is what HotSpot FFM consumers use, and it is the fastest form on SVM too (immediates, zero image-heap objects).
- **Native Image** (breaking):
  - records become `@CStruct` typed-pointer interfaces with `@CField` accessors. Machine code is identical to today's raw offset reads (measured), and `ptr<Struct>` parameters and returns become typed.
  - each enum is a `@CEnum` Java enum with `@CEnumConstant`, `@CEnumValue`, `@CEnumLookup`, a literal `value` field and a generated `of(int)`.
  - each const also has a `@CConstant` accessor, which compiles to the same immediate as the literal.
  - the class always carries a `@CContext` whose generated directives supply `seam.h`, and a `@CConstant` read of the header fingerprint fails the image build on a mismatch.
- **Where the SVM forms cost something, a raw form sits beside the typed one**, and the plain-named import stays the raw leaf binding:
  - a `@CEnum` return type does not build on a no-transition `@CFunction`, the Binding ABI 2 default;
  - on a transition call it adds two calls (one indirect) and a table load;
  - an unlisted value becomes `null`;
  - `@CBitfield` is one shift slower than a mask test and needs a bitfield in the C struct, so flag accessors are mask predicates instead;
  - `@CField` cannot be enum-typed, and `@CStruct` interfaces cannot carry default methods.
- **Truffle**: cells keep primitive results, which is what folds in compiled guest code. New: PE-friendly `TruffleString` name lookups and a guest-visible constants object whose member reads fold. These are designed but not yet measured under runtime compilation.
- **No linkable LLVM objects, no `seam.ll` changes, no optimizer facts** (sections 7, 8.1, 8.5). The pinned Native Image has no LLVM backend (7a.6).

## 2. DSL

```text
const ANALYSIS_MAGIC type=u32 value=0x3141514b

enum Status type=i32
  value null_result = -1
  value ok = 0
  value syntax_error = 1
  value type_error = 2
  value internal_error = 3
  value module_error = 4
end

flags FunctionFlags type=u32
  bit exported = 0
  bit suspending = 1
  bit external = 2
end

flags SignatureFlags type=u32
  bit suspending = 0
end

struct IrFunction size=72 align=8
  …
  field flags type=u32 offset=64 flags=FunctionFlags
end
struct IrSignature size=32 align=8
  …
  field flags type=u32 offset=8 flags=SignatureFlags
end
import status symbol=komodo_frontend_status return=i32 error=no_failure enum=Status
  param result type=ptr<FrontendResult> nullable=true ownership=borrowed access=read
end
```

(Status and bit names above are illustrative; the numbers are Komodo's: `owned-ir/src/lib.rs` packs `exported | suspending<<1 | 4`, `SubstrateNativeIrImporter.java` tests `(flags & 4) != 0` and `(flags & ~7) != 0`, `seam/src/lib.rs` returns statuses 0..4 and `-1` for null. `IrExpression.flags` is a `u64` that packs `local_type << 1`, so it stays unannotated in v1.)

Grammar:

```text
const   := "const" CONST_NAME "type=" INT_SCALAR "value=" INTEGER        # top level, one line
enum    := "enum" GROUP_NAME "type=" INT_SCALAR NL ("value" VALUE_NAME "=" INTEGER NL)+ "end"
flags   := "flags" GROUP_NAME "type=" INT_SCALAR NL ("bit" VALUE_NAME "=" BIT_INDEX NL)+ "end"
ref     := "enum=" GROUP_NAME | "flags=" GROUP_NAME                      # at most one per field/param/header
INT_SCALAR := i8|u8|i16|u16|i32|u32|i64|u64|isize|usize                  # not bool, char, f32, f64, void
INTEGER    := -?[0-9]+ | 0[xX][0-9a-fA-F]+                               # BigInteger; hex is a magnitude
BIT_INDEX  := [0-9]+
CONST_NAME := [A-Z][A-Z0-9_]*      GROUP_NAME := identifier (same rule as struct names)
VALUE_NAME := [a-z][a-z0-9_]*
```

`value`/`bit` lines are exactly four tokens (`value ok = 0`), as the issue writes them. This is the one place a line is not `key=value`; see open decision 13.

Parser changes (`frontend/Dsl.java`):

- `const`: top-level like `opaque`; `takeRequired("type")`, `takeRequired("value")`, `empty(opts)`.
- `enum`/`flags` join `struct|import|export|callback` as block headers; `value` requires an `enum` header, `bit` a `flags` header.
- `field`: `take(opts, "enum", null)` and `take(opts, "flags", null)` before `empty(opts)`.
- `param`: the same two `take`s **before** `facts(opts)`, otherwise they become facts and fail with "requires VALUE@PROVENANCE".
- `end` of import/export: the same two `take`s on the header options before `facts(options)`; `end` of callback: before `empty(options)`.
- One `Dsl.integer(String) → BigInteger` helper for decimal and hex. `sentinel=`, `ffm_sentinel=` and `success=` keep their current decimal parsing in this change.

## 3. IR (`model/Seam.java`)

```java
public record Constant(String name, Scalar type, BigInteger value) {}
public record EnumValue(String name, BigInteger value) {}
public record EnumGroup(String name, Scalar type, List<EnumValue> values) {}   // sorted by value
public record FlagBit(String name, int bit) {}
public record FlagSet(String name, Scalar type, List<FlagBit> bits) {}          // sorted by bit
public enum GroupKind { ENUM, FLAGS }
public record GroupRef(GroupKind kind, String name) {}

Field(String name, Type type, long offset, GroupRef group)                      // + 3-arg compat ctor
Parameter(…, SortedMap<String, Fact> facts, GroupRef group)                     // + compat ctor
Function(…, SortedMap<String, Fact> facts, GroupRef returnsGroup)               // + compat ctor
Signature(…, String ffmSentinel, GroupRef returnsGroup)                         // + compat ctors
Module(…, TruffleConfig truffleConfig,
       List<Constant> constants, List<EnumGroup> enums, List<FlagSet> flagSets) // + compat ctor; sorted by name
```

`Module.constantsClass()` returns `nativeClass() + "Constants"` (`MynaConstants` by default). `ByValue.lower`, `withTarget` and `abiParameters` copy the new components through. Canonical order (groups by name, values by number) means reordering lines in the descriptor changes neither outputs nor the fingerprint.

## 4. Validation (`validate/Validator.java`)

One range helper replaces the three inline copies (the `integer_sentinel` check, the `ffm_sentinel` check, and #125's `success=` check should call it when that PR lands):

```java
static BigInteger min(Scalar s) / max(Scalar s);            // by width and signedness; usize/isize 64-bit
static boolean fits(Scalar carrier, BigInteger value);
```

| Rule | Message (all prefixed `line N:` only for parse errors) |
| --- | --- |
| carrier not an integer scalar | `enum Status: carrier must be an integer scalar, found f32` (same for `const X:`, `flags F:`) |
| empty group | `enum Status: declares no values` / `flags FunctionFlags: declares no bits` |
| group name reuses a type or generated name | existing `duplicate type: Status`, `type shadows scalar`, `type shadows generated name` (groups join the `types` set) |
| group name equals a function symbol (Rust tuple-struct constructor shares the value namespace) | `enum Status: name collides with function symbol Status` |
| duplicate const | `duplicate constant: ANALYSIS_MAGIC` |
| const name equals a function symbol | `const X: name collides with function symbol X` |
| const value outside carrier | `const ANALYSIS_MAGIC: value 4294967296 does not fit u32` |
| duplicate value name | `enum Status: duplicate value name: ok` |
| duplicate value | `enum Status: values ok and success are both 0` |
| value outside carrier | `enum Status: value internal_error = 2147483648 does not fit i32` |
| duplicate bit name | `flags FunctionFlags: duplicate bit name: exported` |
| duplicate bit index | `flags FunctionFlags: bits exported and public both use bit 0` |
| bit outside carrier | `flags FunctionFlags: bit external = 32 is outside u32 (bits 0..31)` |
| reserved bit name | `flags FunctionFlags: bit name is reserved: all` (`all`, `none`) |
| undeclared reference | `field IrFunction.flags: undeclared flag set: FunctionFlagz` / `import status: undeclared enum: Statuz` / `param compile.mode: …` |
| wrong kind | `import status: enum=FunctionFlags names a flag set; use flags=FunctionFlags` |
| both options | `field IrFunction.flags: enum= and flags= are mutually exclusive` |
| carrier mismatch (exact scalar required, pointers rejected) | `field IrFunction.flags: flags=FunctionFlags has carrier u32, but the field's type is u64` |
| generated C names collide with each other | `C name KOMODO_FRONTEND_STATUS_OK is generated by both const STATUS_OK and enum Status value ok` |
| generated C name collides with a fixed header macro (`<MODULE>_SEAM_ABI`, `<MODULE>_BINDING_ABI_VERSION`, the `<MODULE>_SEAM_H` include guard) | `const SEAM_ABI: C name KOMODO_FRONTEND_SEAM_ABI is reserved by the generated header` (also reachable through a group, e.g. `enum Seam` with `value abi`, or `flags Binding` with `bit abi_version`) |
| malformed lines (parser) | `line 12: expected: value NAME = INTEGER`, `line 12: value requires an enum block`, `line 12: invalid integer: 0xZZ` |

Naming needs no keyword table for values: lowercase-only names are upper-cased in every language, and no C, Rust or Java keyword is all-caps, so `value default = 0` is legal (`DEFAULT`). Upper-casing lowercase names is injective, so there are no case-folded duplicates. `CONST_NAME` goes through the existing `identifier()` check (which already rejects `ABI_FINGERPRINT` and `BINDING_ABI_VERSION`). `<Class>Constants` is checked against declared type names like the Truffle class is. The C check runs on the final macro names, seeded with the three fixed header macros, so every route to a clash (const, enum value, flag bit, the generated `_ALL`) is covered by one set.

## 5. Canonical JSON and fingerprint (`serialize/SeamJson.java`)

Three top-level sections, each emitted only when non-empty, after `"functions"` and before `"resources"`; `schemaVersion` stays 3:

```json
  "constants": [{"name":"ANALYSIS_MAGIC","type":"u32","value":"826364235"}],
  "enums": [{"name":"Status","type":"i32","values":[{"name":"null_result","value":"-1"},{"name":"ok","value":"0"}]}],
  "flagSets": [{"name":"FunctionFlags","type":"u32","bits":[{"name":"exported","bit":0},{"name":"suspending","bit":1},{"name":"external","bit":2}]}]
```

References are appended to the object they annotate, only when present:

```json
{"name":"flags","type":"u32","offset":64,"flags":"FunctionFlags"}
{"logicalName":"status", … ,"facts":{},"returnEnum":"Status"}
{"name":"mode","type":"i32", … ,"facts":{},"enum":"Mode"}
```

- Values are canonical decimal strings (as `sentinel` already is): exact for the full u64 range, and `0x10` and `16` produce the same fingerprint.
- A descriptor without these declarations serializes byte-for-byte as today, so its fingerprint is unchanged within a generator version (`generatorVersion` already changes every fingerprint per release).
- A changed value, name, carrier, bit index or reference changes the fingerprint and therefore `seam.abi`, `<MODULE>_SEAM_ABI`, `ABI_FINGERPRINT` and `!myna.contract`.

## 6. Generated output

Rendering is a function of the canonical value only: enum values in decimal, flag masks and unsigned consts in hex, signed consts in decimal.

### 6.1 C (`seam.h`)

Typed macros, prefixed `<MODULE>_` like the existing `<MODULE>_SEAM_ABI`; group names converted CamelCase → UPPER_SNAKE.

```c
/* const ANALYSIS_MAGIC: u32 */
#define KOMODO_FRONTEND_ANALYSIS_MAGIC UINT32_C(0x3141514b)
/* enum Status: i32 */
#define KOMODO_FRONTEND_STATUS_NULL_RESULT (INT32_C(-1))
#define KOMODO_FRONTEND_STATUS_OK INT32_C(0)
#define KOMODO_FRONTEND_STATUS_SYNTAX_ERROR INT32_C(1)
#define KOMODO_FRONTEND_STATUS_INTERNAL_ERROR INT32_C(3)
static inline const char *komodo_frontend_status_name(int32_t value) {
  switch (value) {
    case KOMODO_FRONTEND_STATUS_NULL_RESULT: return "null_result";
    case KOMODO_FRONTEND_STATUS_OK: return "ok";
    …
    default: return NULL;
  }
}
/* flags FunctionFlags: u32 */
#define KOMODO_FRONTEND_FUNCTION_FLAGS_EXPORTED UINT32_C(0x1)
#define KOMODO_FRONTEND_FUNCTION_FLAGS_SUSPENDING UINT32_C(0x2)
#define KOMODO_FRONTEND_FUNCTION_FLAGS_EXTERNAL UINT32_C(0x4)
#define KOMODO_FRONTEND_FUNCTION_FLAGS_ALL UINT32_C(0x7)

struct IrFunction { …  uint32_t flags; /* flags FunctionFlags */ … };
/* IMPORT; error=NO_FAILURE; returns enum Status */
int32_t komodo_frontend_status(struct FrontendResult const * result);
```

- Not a C `enum`: before C23 enumerators are `int`, so `u32 ≥ 2^31` and every 64-bit value is a C23 extension (clang `-std=c17 -pedantic` warns, section 8.1), and the enum's own type is implementation-chosen. The header must stay valid C11 and C++.
- `INTn_C`/`UINTn_C` give the right type and width, work in `#if`, `case` labels, array sizes and static initializers. Negative values are parenthesized. A carrier's minimum is written `(-INT64_C(9223372036854775807) - 1)` (and likewise for i32) because the positive literal does not fit the type.
- Flag use is the plain idiom: `(f.flags & KOMODO_FRONTEND_FUNCTION_FLAGS_EXTERNAL) != 0`, `flags & ~…_ALL` for unknown bits.
- Each group also gets `typedef int32_t komodo_frontend_status_t;` / `typedef uint32_t komodo_frontend_function_flags_t;`, used for annotated struct fields and prototypes. It is the same type to the compiler, so existing C callers and providers still compile. The typedef names join the C-name collision set.
- The struct keeps a plain integer member for a flag set. An anonymous union with a C bitfield (what `@CBitfield` would need) is not emitted; see 7a.9.

### 6.2 Rust (`seam.rs`)

```rust
pub const ANALYSIS_MAGIC: u32 = 0x3141514b;

/// enum Status: i32. Open set: every i32 is a valid `Status`; undeclared values equal no constant.
#[repr(transparent)]
#[derive(Clone, Copy, PartialEq, Eq, Hash, Debug)]
pub struct Status(pub i32);
impl Status {
    pub const NULL_RESULT: Self = Self(-1);
    pub const OK: Self = Self(0);
    pub const SYNTAX_ERROR: Self = Self(1);
    pub const INTERNAL_ERROR: Self = Self(3);
    #[inline]
    pub const fn name(self) -> Option<&'static str> {
        match self.0 { -1 => Some("null_result"), 0 => Some("ok"), 1 => Some("syntax_error"), 3 => Some("internal_error"), _ => None }
    }
}

/// flags FunctionFlags: u32.
#[repr(transparent)]
#[derive(Clone, Copy, PartialEq, Eq, Hash, Debug)]
pub struct FunctionFlags(pub u32);
impl FunctionFlags {
    pub const NONE: Self = Self(0);
    pub const EXPORTED: Self = Self(0x1);
    pub const SUSPENDING: Self = Self(0x2);
    pub const EXTERNAL: Self = Self(0x4);
    pub const ALL: Self = Self(0x7);
    #[inline(always)] pub const fn contains(self, other: Self) -> bool { self.0 & other.0 == other.0 }
    #[inline(always)] pub const fn intersects(self, other: Self) -> bool { self.0 & other.0 != 0 }
    #[inline(always)] pub const fn union(self, other: Self) -> Self { Self(self.0 | other.0) }
    /// `self` if `on`, otherwise no bits. Written as a multiply so a single bit lowers to a shift.
    #[inline(always)] pub const fn when(self, on: bool) -> Self { Self(self.0 * on as u32) }
}
impl core::ops::BitOr for FunctionFlags { type Output = Self; #[inline(always)] fn bitor(self, o: Self) -> Self { Self(self.0 | o.0) } }
impl core::ops::BitAnd for FunctionFlags { type Output = Self; #[inline(always)] fn bitand(self, o: Self) -> Self { Self(self.0 & o.0) } }

pub struct IrFunction { …
    pub flags: FunctionFlags,          // was u32; same size, alignment and offset
… }
unsafe extern "C" { pub fn komodo_frontend_status(result: *const FrontendResult) -> Status; }   // was -> i32
```

- **Breaking:** annotated struct fields, `extern` signatures, `<Name>Fn` types and callback aliases use the newtype. `#[repr(transparent)]` guarantees the same layout and the same C ABI as the carrier, so `seam.h`, `seam.ll` and the provider's object code are unaffected, but `assert_implementations!` now requires implementations to spell the newtype. Use is `f.flags.contains(FunctionFlags::EXTERNAL)` and `match abi::status(r) { Status::OK => …, _ => … }` (derived `PartialEq + Eq` makes the constants usable as patterns). Every carrier value remains a valid `Status`, so reading foreign data stays defined.
- Komodo's `flags: u32::from(f.exported) | (u32::from(f.suspending) << 1)` becomes `FunctionFlags::EXPORTED.when(f.exported) | FunctionFlags::SUSPENDING.when(f.suspending)`, which compiles to the same single `lea` (section 8.2).
- Not `#[repr(i32)] enum`: reading an undeclared foreign value into one is undefined behaviour, and rustc exploits it (section 8.2: `!range` on the load, a jump table with a `poison` slot, no bounds check). The newtype compiles to exactly the raw-integer code.

### 6.3 LLVM (`seam.ll`)

No change apart from the fingerprint it already embeds. No globals, no metadata, no attributes. `myna apply` and `myna-llvm` are untouched. Reasons in section 7.

### 6.4 Java (`<Class>Constants.java`, default `MynaConstants.java`)

One new file, generated only when the descriptor declares a constant, enum or flag set. No imports, no dependency on SVM, Truffle or FFM classes, so TruffleData (FFM + Truffle on HotSpot) and Komodo (all three) use the same file. It is not listed in `native-image.properties`: it has no static initializer and nothing to initialize.

```java
// Generated by myna.
package dev.elide.myna.generated;

public final class MynaConstants {
    private MynaConstants() {}
    public static final int ANALYSIS_MAGIC = 0x3141514b;

    /** enum Status: i32 */
    public static final class Status {
        private Status() {}
        public static final int NULL_RESULT = -1;
        public static final int OK = 0;
        public static final int SYNTAX_ERROR = 1;
        public static final int INTERNAL_ERROR = 3;
        public static String name(int value) {
            return switch (value) {
                case NULL_RESULT -> "null_result";
                case OK -> "ok";
                case SYNTAX_ERROR -> "syntax_error";
                case INTERNAL_ERROR -> "internal_error";
                default -> null;
            };
        }
    }

    /** flags FunctionFlags: u32 */
    public static final class FunctionFlags {
        private FunctionFlags() {}
        public static final int EXPORTED = 0x1;
        public static final int SUSPENDING = 0x2;
        public static final int EXTERNAL = 0x4;
        public static final int ALL = 0x7;
    }
}
```

- Every field is a primitive with a literal initializer (`(byte) 0xc8` for narrow carriers is still a constant expression), so each is a JLS compile-time constant: usable as a `case` label, inlined by javac into the consumer's bytecode.
- Java type is the existing same-width signed carrier (`Types`): `u32` → `int` (`0x80000000` prints negative, bits preserved), `u64`/`usize` → `long` (`0xffffffffffffffffL`), `u8` → `byte`.
- Komodo's importer becomes `(flags & FunctionFlags.EXTERNAL) != 0`, `(flags & ~FunctionFlags.ALL) != 0`, `case Status.OK ->`.
- No Java `enum`, no boxed values, no arrays, no static initializer in this class. It has no imports, so it is the form for HotSpot FFM consumers and it is also what the Native Image and Truffle classes use internally.
- Flag sets also get static predicates here, one per bit, usable on every runtime:

```java
        public static boolean exported(int flags) { return (flags & EXPORTED) != 0; }
        public static boolean external(int flags) { return (flags & EXTERNAL) != 0; }
        public static boolean hasUnknown(int flags) { return (flags & ~ALL) != 0; }
```

`<Class>FFM.java` changes only by a comment on annotated offsets and imports.

### 6.5 Native Image (`<Class>.java`), breaking

```java
@CContext(MynaDirectives.class)                    // always present now
public final class MynaNative {
    /** Fails the image build when the seam.h on the C context is not the one these bindings came from. */
    @CConstant("KOMODO_FRONTEND_SEAM_ABI") static native String seamHeaderAbi();

    @CConstant("KOMODO_FRONTEND_ANALYSIS_MAGIC") public static native int analysisMagic();

    @CEnum("int32_t")
    public enum Status {
        @CEnumConstant("KOMODO_FRONTEND_STATUS_NULL_RESULT") NULL_RESULT(-1),
        @CEnumConstant("KOMODO_FRONTEND_STATUS_OK") OK(0),
        @CEnumConstant("KOMODO_FRONTEND_STATUS_SYNTAX_ERROR") SYNTAX_ERROR(1),
        @CEnumConstant("KOMODO_FRONTEND_STATUS_INTERNAL_ERROR") INTERNAL_ERROR(3);
        /** Literal value: a constant enum folds to an immediate, a variable one is a single field load. */
        public final int value;
        Status(int value) { this.value = value; }
        /** SVM's conversions, for handwritten C-interface signatures that use this type. */
        @CEnumValue public native int cValue();
        @CEnumLookup public static native Status lookup(int value);
        /** Generated lookup: a switch, no image-heap table, no virtual call. Null for an unlisted value. */
        public static Status of(int value) {
            switch (value) { case -1: return NULL_RESULT; case 0: return OK; case 1: return SYNTAX_ERROR; case 3: return INTERNAL_ERROR; default: return null; }
        }
    }

    @CStruct(value = "IrFunction", addStructKeyword = true)
    public interface IrFunction extends PointerBase {
        int SIZE = 72;
        int ALIGNMENT = 8;
        @CField("idLength") long idLength();
        @CField("idLength") void idLength(long value);
        /** flags FunctionFlags: test with MynaConstants.FunctionFlags.external(flags()) */
        @CField("flags") int flags();
        @CField("flags") void flags(int value);
        @CFieldAddress("flags") CIntPointer addressOfFlags();
        …
    }

    // enum=Status on a leaf import: the plain name stays the raw no-transition binding
    @CFunction(value = "komodo_frontend_status", transition = Transition.NO_TRANSITION)
    public static native int status(PointerBase result);
    public static Status statusEnum(PointerBase result) { return Status.of(status(result)); }

    // enum=Mode on a parameter: a typed overload beside the raw one
    public static native int compileWith(…, int mode);
    public static int compileWith(…, Mode mode) { return compileWith(…, mode.value); }

    // ptr<IrProgram> return: typed pointer
    public static native IrProgram irProgram(PointerBase result);
}
```

Rules, each backed by a measurement in 7a:

| Element | Generated | Why |
| --- | --- | --- |
| Record | `@CStruct` interface with `@CField` get/set and `@CFieldAddress`; `SIZE`/`ALIGNMENT` as interface constants | Identical code to `((Pointer) r).readInt(off)` (7a.8). Typed pointers, `StackValue.get(IrFunction.class)` and `SizeOf` work. Offsets are computed by the target C compiler from `seam.h`, whose `SEAM_ASSERT`s reject any disagreement with the descriptor. |
| `ptr<Struct>` parameter/return | the struct's interface type | Free: word types are erased. |
| Field with `enum=` | `@CField` stays the carrier; typed read is `Tag.of(e.tag())` | The image builder rejects an enum-typed `@CField`. |
| Field with `flags=` | `@CField` stays the carrier; predicates in `<Class>Constants.<Flags>` | `@CBitfield` measured one shift slower and needs a C bitfield (7a.9); `@CStruct` interfaces reject default methods, so predicates cannot live on the interface. |
| Enum | `@CEnum` enum, `@CEnumConstant` per value, `@CEnumValue cValue()`, `@CEnumLookup lookup(int)`, literal `value`, generated `of(int)` | The annotations make it a first-class C-interface type for consumer-written signatures and source the values from the header. Myna's own wrappers use `value` and `of`, which are faster than SVM's conversions (7a.3 vs 7a.8). |
| Import return with `enum=` | raw primitive binding under the plain name (leaf status untouched) plus `<name>Enum(...)` | A `@CEnum` return does not build on a leaf (7a.2), and any enum-object result costs a jump table and a pointer compare (7a.8). The raw form also keeps unlisted values. |
| Import parameter with `enum=` | raw primitive binding plus a typed overload passing `value` | Constant enum → `movl $3`; variable → null check and one load (7a.8). |
| Import parameter/return with `flags=` | raw carrier only | There is no typed value form for a bit set in Java that is free. |
| Export / callback with `enum=`/`flags=` | raw carriers in the Java target and callback interface | A typed entry would turn an unlisted incoming value into `null` and a `null` result into an exception inside the entry stub. |
| Const | `@CConstant` accessor beside the literal in `<Class>Constants` | Same immediate (7a.4). The literal remains for `case` labels and HotSpot. |
| C context | generated `<Class>Directives` (embeds `seam.h`, writes it to a temporary directory at image build, delegates libraries and options to the consumer's `c_context` class when one is set) | Measured working (7a.8). No consumer configuration needed; `c_context` users keep theirs. The directory must be removed on JVM exit: my experiment's version left one `myna-seam*` directory in `/tmp` per image build. |
| Header check | `seamHeaderAbi()` compared with `ABI_FINGERPRINT` in `<Class>Directives`, which is also a hosted `Feature`. Consumers opt in with `--features=<package>.<Class>Directives`; the generated `native-image.properties` does not register it. The Feature reads `ABI_FINGERPRINT` from the loaded class, does nothing in an image without the Native Image class, and its error names both fingerprints | 7a.4. Opt-in by user decision, so images that do not compile the Native Image class need nothing new. The FFM-specific `<Class>ForeignFeature` cannot host the check: SVM consumers do not register it. Known limit: the embedded header comes from the same generator run, so the check catches mixed Java generations, not a provider built against an older `seam.h`. |

Nothing here stores an address or a runtime value in the image heap: the enum instances, their `value` fields and the `CEnumRuntimeData` are build-time constants. Leaf status is decided only by `native_leaf`, as before.

### 6.6 Truffle (`<Class>Truffle.java` and `myna-truffle`)

- **Cell results and arguments stay primitive.** A cell over `status` still implements `executeInt`; a guest compares the number with a constant and partial evaluation folds exactly as it does today. Wrapping the result in an enum object or an interop object would add an allocation or an image-heap lookup on the compiled path and lose unlisted values. Cells call the raw binding, so leaf status and the direct-transition stub are untouched.
- **Names for guests and diagnostics**, generated per enum, DSL-free:

```java
    public static final class StatusNames {
        private static final TruffleString OK = TruffleString.fromConstant("ok", TruffleString.Encoding.UTF_8);
        …
        /** Folds to a constant for a constant value; otherwise a switch. No boundary needed. Null if unlisted. */
        public static TruffleString name(int value) {
            switch (value) { case 0: return OK; case 1: return SYNTAX_ERROR; …; default: return null; }
        }
    }
```

  The class is added to the generated `--initialize-at-build-time` list; the `TruffleString`s are image-heap constants holding only text.
- **Guest-visible constants**: `<Class>Truffle.constants()` returns a `NativeConstants` (new class in `myna-truffle`, where the DSL is allowed). It is an interop object with one member per group and per const (`constants.Status.ok`, `constants.FunctionFlags.external`, `constants.ANALYSIS_MAGIC`). Member names and values are `@CompilationFinal(dimensions = 1)` arrays; `readMember` has a cached specialization on the member string identity, so a compiled guest read of `constants.Status.ok` folds to the number; the uncached lookup is behind `@TruffleBoundary`. `u64` values above `Long.MAX_VALUE` are returned as raw bits, like cell results.
- **Not measured.** I did not build a runtime-compilation image for these. They follow the documented rules (build-time-initialized classes, constants in `@CompilationFinal` arrays, slow path behind a boundary), and the test plan requires `-H:+PrintRuntimeCompileMethods` plus a compiled-guest check before they are called done. Whether `@CField` accessors may appear on a cell's compiled path is likewise untested; cells do not need them (they use `Memory`), so the design does not rely on it.

## 7. Performance and memory analysis

| Question | Answer | Evidence |
| --- | --- | --- |
| Do header/`const`/`static final` constants occupy storage? | No. They are immediates in instruction encodings. | 8.1: `icmp eq i32 %0, 826364235`, no `@` globals; 8.3: `ldc 826364235`, `bipush -8` |
| Should constants be `linkonce_odr`/`hidden`/`unnamed_addr` globals in `seam.ll` or a new bitcode file? | No. | 8.1: with a bitcode consumer ThinLTO imports the initializer and produces the same `cmpl $imm` as the macro (symbol gone). With a machine-code consumer in the same link the constant stays as 4 bytes of `.rodata` and the code is `lea; cmpl (%rcx)`. Native Image code is always the second case, and Java cannot read the global at all. |
| What else does an object form lose? | `#if`, `case` labels, array sizes, Rust patterns and `const` contexts, Java `switch` — all need language-level constants. It also adds a second definition that can disagree with the header (my first run of 8.1 had a mistyped initializer in the `.ll`; everything linked and the comparison was silently wrong). | — |
| Should Rust and C read values from one bitcode source? | No. The descriptor is the single source; every output is a projection of it and carries the fingerprint, which both sides can already compare. | — |
| Is there anything to deduplicate? | Not for constants. Name strings are the only data, and they exist only when a name function is called. | 8.4 |
| Name tables: cost and placement | Unused: zero bytes in C, Rust and a Native Image. Used in Rust: `private unnamed_addr` string constants plus a relative (relocation-free) lookup table, one copy per LTO unit. Used in C without LTO: strings land in `.rodata.str1.1` (`SHF_MERGE|SHF_STRINGS`), which the linker merges to one copy across translation units; each using TU also gets a 32-byte `.data.rel.ro` pointer table. | 8.4 |
| Native Image: any image-heap objects? | None. The constants classes are not reachable: absent from `used_classes`, `used_methods` and the call tree; `name()` strings absent from the binary. The consumer's bytecode has no `Fieldref` to them and HotSpot never loads them. | 8.3 |
| Truffle partial evaluation | The values are already literals in the consumer's bytecode before Graal parses it; there is no field read to fold, so no `@CompilationFinal` or boundary question arises. `name()` is a switch over constant strings and needs no `@TruffleBoundary`. | 8.3 |
| Cost of a Java `enum` instead | Per read: an int→instance lookup, `ordinal()`, a `$SwitchMap$` array load, then the switch. In the image: the enum class, its instances, `$VALUES`, name strings and `values()` kept reachable through reflection metadata. Unknown values become `null`. | 8.3 |
| Rust `#[repr(int)] enum` | UB for undeclared values, and miscompiled in practice. Newtype + consts: identical machine code to raw integers (LLVM merged the functions into aliases). | 8.2 |
| Flag ergonomics at zero overhead | `contains` ≡ `f & 4 != 0` (aliased by LLVM); `when`/`|` ≡ shift-and-or (aliased). C and Java use the mask idiom directly. | 8.2 |
| Could enum ranges feed LLVM `range`? | Not soundly. See below. | 8.5 |
| Does SVM's `@CEnum` give a near-zero-overhead typed enum? | Not on this fork. A `@CEnum` return does not build on a leaf call; on a transition call it costs two calls (one indirect) and a table load. A generated `of(int)` switch is cheaper but still a jump table and a pointer compare where the raw form is one `cmpl`. Enum *arguments* through the literal `value` field are free for constants. | 7a.1–7a.3, 7a.8 |
| `@CStruct`/`@CField` vs raw offset reads | Identical machine code. | 7a.8 |
| `@CBitfield` vs mask test | One extra shift and a narrower load (65 vs 55 bytes for the test method), and it needs a C bitfield in the struct. | 7a.9 |
| Is a `@CConstant` read an immediate? | Yes, byte-identical code to a `static final` literal. It adds a header query at image build and a `CContext` dependency, and removes nothing. | 7a.4 |
| What can be deduplicated in an SVM image? | Constants are immediates (nothing to share). Strings are interned image-wide: the same literal in two classes is one image-heap object, so Java `name()` strings exist once per image, shared with any equal literal anywhere else. | 7a.5 |
| Would an LLVM backend for Native Image change the linkable-constant answer? | The pinned distribution has none. Even with one, javac has already inlined the values into bytecode before any backend runs. | 7a.6 |

**Range facts.** The issue puts "no checking of returned values against the enum" out of scope, so the generator would be asserting something nobody verifies. `docs/dsl.md` requires every optimizer fact to carry provenance and emits only `explicit_contract`/`compiler_proven`; an enum declaration is neither, it is a list of names. The damage is concrete: with `range(i32 0, 5)` on `komodo_frontend_status`, LLVM deletes a caller's `== -1` check (8.5), and Komodo's implementation really does return `-1` for a null result. On a field there is no place to attach it (`myna apply` annotates declarations and direct call sites, not loads in consumer code) short of the repr-enum, which is the UB case above. The upside is also small: where the provider's body is visible to LTO, LLVM already infers return ranges itself (8.2 shows `range(i32 10, 31)` inferred on a definition). If this is ever wanted it should be a separate, explicit fact on the reference (`closed=true@explicit_contract`), emitted as the convex hull on import returns only, with its own issue.

## 7a. SVM C interface (`org.graalvm.nativeimage.c.constant` and friends)

Built with the pinned fork's `native-image` (GraalVM CE 25.5.5, `25.0.4.1.1+1-jvmci-25.5-b24`) in a `mktemp -d` directory. Source reading used the `svm.src.zip` shipped in that distribution; `CInterfaceEnumTool.java` and `CEnumRuntimeData.java` there are byte-identical to the fork checkout I read.

Test setup: `seam.h` with the typed macros of section 6.1, a C library with `int32_t kf_status(int32_t)` and `kf_take`, a `CContext.Directives` class that includes the header, and

```java
@CEnum("int32_t") public enum Status {
    @CEnumConstant("KF_STATUS_NULL_RESULT") NULL_RESULT, @CEnumConstant("KF_STATUS_OK") OK, …;
    @CEnumValue public native int value();
    @CEnumLookup public static native Status of(int value);
}
@CConstant("KF_ANALYSIS_MAGIC") public static native int magic();
@CFunction(value = "kf_status", transition = NO_TRANSITION) static native int statusInt(int x);
@CFunction(value = "kf_status")                             static native int statusIntT(int x);
@CFunction(value = "kf_status")                             static native Status statusEnum(int x);
@CFunction(value = "kf_take",   transition = NO_TRANSITION) static native int takeEnum(Status s);
```

Built with `-H:-DeleteLocalSymbols -H:+PrintHeapHistogram -H:+PrintAnalysisCallTree`, each measured caller `@NeverInline`.

### 7a.1 How SVM lowers `@CEnum`

From the sources: `SizeAndSignednessVerifier` builds one `CEnumRuntimeData` object per enum type at image build and stores it in the image heap. It holds `long[] javaToC` indexed by `ordinal()`, and for C→Java either an `Enum<?>[]` (`CEnumArrayLookup`, when the value spread is under 5× the count) or a `Map<Long, Enum<?>>` with boxed keys (`CEnumMapLookup`). `CCallStubMethod` rewrites an enum parameter into an invoke of `CEnumRuntimeData.enumToInt` (a null check, an `ordinal` load, an array load) and an enum return into an invoke of `CEnumRuntimeData.longToEnum`, which sign/zero-extends and then makes a **virtual** call to `lookupEnum`, followed by a class-cast guard. It is a method call on a heap object, not an intrinsic. `@CField` accessors cannot be enum-typed at all (`InfoTreeBuilder`: "Wrong type: expected a primitive type or a Word type"), so the struct-field case in the question does not exist; Myna's record accessors are raw `Pointer.readInt(offset)` anyway.

### 7a.2 `@CEnum` returns cannot be leaf calls

With `statusEnum` declared `transition = NO_TRANSITION` the image build fails:

```text
com.oracle.svm.shared.util.VMError$HostedError: Found 2 violations of @Uninterruptible usage:
- Missing @Uninterruptible annotation: t.N.statusEnum(int):N$Status is annotated with @Uninterruptible,
  but calls com.oracle.svm.core.c.enums.CEnumRuntimeData.longToEnum(long):Enum which is not annotated.
```

Binding ABI 2 makes every import a no-transition leaf unless it opts out, and Komodo's `status` import is a leaf. Typing it as a `@CEnum` would mean forcing it to a transition, which the leaf rule forbids a representation choice from doing, or not building. (Enum *parameters* do build on leaves: `enumToInt` is `@Uninterruptible`.)

### 7a.3 Cost where it does build

```text
Main_viaInt   (leaf, int == 3)             57 bytes:  callq N_statusInt ; cmpl $0x3,%eax ; cmov
Main_viaIntT  (transition, int == 3)      180 bytes:  transition inlined ; cmpl $0x3,%ecx ; cmov
Main_viaEnum  (transition, == Status.X)    65 bytes:  callq N_statusEnum ; leaq 0x278c70(%r14),%rcx ; cmpq %rax,%rcx
  N_statusEnum stub                        224 bytes:  transition ; callq CEnumRuntimeData_longToEnum ; class-cast check
  CEnumRuntimeData_longToEnum              129 bytes:  extend ; callq *%rax            (virtual lookupEnum)
  CEnumArrayLookup_lookupEnum              114 bytes:  bounds check ; movl 0x8(%rcx,%rsi,4),%eax   (image-heap array load)
  CEnumMapLookup_lookupEnum                299 bytes:  (sparse values: boxed-Long map get)
Main_passInt  : movl $0x3,%edi ; callq N_takeInt
Main_passEnum : leaq 0x278c70(%r14),%rdi ; callq N_takeEnum
  N_takeEnum  : null check ; movl 0x8(%rdi),%eax (ordinal) ; cmpl $0x6 ; movl 0x278c08(%r14,%rax,8),%edi ; callq *%r10
Status.value() on a constant enum: movl 0x278c08(%r14),%eax      (a load from javaToC, not an immediate)
```

So an enum return costs, beyond the transition it forces, two extra calls (one indirect) and an image-heap array load (or a map probe with a boxed key for sparse values), then a pointer compare instead of an integer compare; a constant enum argument costs a null check, two loads and a bounds check instead of `movl $3`. The enum→value mapping is not folded even ahead of time for a constant enum, so there is no reason to expect partial evaluation to fold it either (the arrays are plain, not `@CompilationFinal`); I did not run a runtime-compilation test for this.

Image heap, from `-H:+PrintHeapHistogram`:

```text
   6   144  t.N$Status            1   40  t.N$Status[]         1   40  CEnumArrayLookup
   3    72  t.N$Sparse            1   24  t.N$Sparse[]         1   32  CEnumMapLookup
```

plus each enum's `javaToC` `long[]`, its `Class`/hub, its constant-name `String`s (`strings app | grep -c '^INTERNAL_ERROR$'` → 1), and for the sparse enum the map and boxed `Long`s. The runtime data is one object per enum type, shared by all call sites; that is the only "dedup" it offers. The literal-constant design has none of these objects (section 8.3: zero classes reachable).

Open-set rule: an unlisted value from native code becomes `null`, with no way to recover the number.

```text
unlisted dense: null, unlisted sparse: null, of(99): null
switch on unlisted: java.lang.NullPointerException
null enum arg: java.lang.NullPointerException: [no exception stack trace available because exception is thrown from code that must be allocation free]
```

Komodo's importer rejects unknown flags and statuses with its own diagnostics (`(flags & ~7) != 0` → "unknown function flags"); a `null` loses the value it reports.

### 7a.4 `@CConstant` / `@CEnumConstant`: values from the header

`CInterfaceInvocationPlugin.replaceConstant` replaces a `@CConstant` call with a `ConstantNode` during bytecode parsing, and the machine code confirms it:

```text
Main_magicLiteral   : cmpl $0x3141514b,%edi      (static final int)
Main_magicCConstant : cmpl $0x3141514b,%edi      (@CConstant read from seam.h)   — identical bytes otherwise
Main_viaCConstant   : cmpl $0x3,%eax             (same as Main_viaInt)
```

So Myna *could* generate one `@CConstant` method per value against `seam.h`. What it costs: every image build runs a C query through the consumer's `CContext` (a C compiler at image build, or a prepared `-H:+UseCAPCache` directory), the consumer's `Directives` must include `seam.h`, the values stop being Java compile-time constants (no `case` labels, no javac folding, not usable from the FFM or Truffle classes on HotSpot, methods instead of fields), and a second copy of each value is still needed for HotSpot. What it buys: a build-time guarantee that Java's values are the provider header's values. The fingerprint already gives that guarantee if it is checked, and it can be checked with **one** query for the whole seam (values, layouts, signatures, contracts) instead of one per constant:

```java
@CConstant("KF_SEAM_ABI") static native String headerAbi();          // in the @CContext class
// hosted Feature.beforeAnalysis:
String header = CConstant.ValueAccess.get(N.class, "headerAbi", String.class);
if (!header.equals(ABI_FINGERPRINT)) throw new IllegalStateException(…);
```

```text
matching header:  "MYNA header fingerprint verified: 9e33817b…4d18"   Finished generating 'app' in 11.7s.
other header:     Error: Feature defined by u.Check unexpectedly failed with a(n) java.lang.IllegalStateException.
                  Caused by: … seam.h fingerprint 9e33817b…4d18 differs from generated bindings deadbeef
```

Section 6.5 uses both: a `@CConstant` accessor per const beside the literal, and this one-query fingerprint check, on by default because the Native Image class now always has a C context. Build time with the `CContext` query was within noise of the build without one (11.7 s vs 12.0 s for a hello-world-sized image).

### 7a.5 Other SVM representations, and deduplication

| Candidate | Finding |
| --- | --- |
| `@CStruct` + `@CField` typed with a `@CEnum` | Rejected by the image builder (primitive or word only). |
| `@CBitfield` | Reads a C bitfield member whose position the C compiler chose, queried from the header. Myna flag sets are explicit masks over a plain integer at a declared offset; the equivalent code is `(readInt(off) & mask) != 0`, which needs no query. Flag predicates are generated as static methods instead (6.4, 7a.9). |
| Word types (`interface Status extends SignedWord`) | Erased to a machine word, no heap object, and every value stays representable, so this is the only typed SVM form that respects the open-set rule. But a word is 64 bits: as the return of an `i32` C function it reads the whole register, whose upper half the C ABI leaves undefined. It cannot be a `switch` selector, and it does not exist on HotSpot. No gain over `int` plus constants. |
| `@CPointerTo` | For typed pointers to scalars/structs; the existing typed carriers already cover it. Nothing enum-specific. |
| `@Fold`, `@AlwaysInline`, `ImageSingletons` | Nothing to fold or inline: javac has already replaced every use with a literal. A singleton would be an image-heap object where none is needed. |
| Image-heap string interning | Works image-wide. The same literal returned from two different classes is one object (`x == y` → `true`, and one copy of the bytes in the binary). Java `name()` strings therefore exist once per image and are shared with any equal literal in the consumer's code. This is the deduplication SVM actually provides for this feature, and the design gets it without doing anything. |
| Constants themselves | Immediates in each instruction; there is no object to merge. |
| Truffle cells / runtime-compiled code | Unaffected: cells keep primitive carriers, and the constants never appear in a cell. |

### 7a.6 LLVM backend

```text
$ native-image … -H:CompilerBackend=llvm …
Error: The LLVM backend module 'org.graalvm.nativeimage.llvm' is not available. Use --tool:llvm-backend or select a different compiler backend.
$ native-image … --tool:llvm-backend …
Error: Unknown name in option specification: tool:llvm-backend
```

The fork's source tree still contains `com.oracle.svm.core.graal.llvm`, but the pinned distribution ships no such module (`lib/svm/builder` has no LLVM backend jar). So SVM code is machine code in the final link, as `docs/thinlto.md` already says. If that backend were shipped, Java methods would become bitcode and leaf calls could inline across the language boundary, which is a separate and much larger topic; it would still not make linkable constants useful, because the constant values are already literals in the bytecode the backend compiles.

### 7a.7 Conclusion of the first round

Raw primitives with literal constants are the fastest form on SVM for values coming *from* native code, and `@CConstant` ties them. That result stands. Sections 7a.8 and 7a.9 measure the typed forms the design now generates around it.

### 7a.8 Typed forms the design generates (second image)

Setup: the enum of section 6.5 (`@CEnum` plus literal `value` and switch `of`), a `@CStruct` interface over `struct IrFunction`, a generated `Directives` that embeds the header text and writes it to a temporary directory in `getHeaderFiles()`, leaf `@CFunction`s returning `int`.

```text
Main_viaRaw     57 bytes   callq N_statusRaw ; cmpl $0x3,%eax ; cmov
Main_viaOf      65 bytes   callq N_status (wrapper not inlined: switch + raw call) ; leaq 0x2782a8(%r14),%rcx ; cmpq %rax,%rcx
Main_viaOf     297 bytes   with @AlwaysInline on the wrapper and of(): callq N_statusRaw ; addl $1 ; cmpl $5 ; ja ;
                           jmpq *%rcx (jump table) ; leaq <enum>(%r14) ; cmpq           — no heap table, no virtual call
Main_switchRaw  97 bytes   cmpl $0 ; je ; cmpl $3 ; je
Main_switchOf  154 bytes   null check ; movl 0x8(%rax),%edi (ordinal) ; cmpl $6 ; jae ; cmpl ; je ; cmpl ; je
Main_passConst             movl $0x3,%edi ; callq N_takeRaw              (take(Status.INTERNAL_ERROR) through `value`)
Main_passVar               cmpq %r14,%rdi ; je <NPE> ; movl 0x10(%rdi),%edi ; callq N_takeRaw
Main_extRawOffset 55 bytes movl 0xc(%rdi),%eax ; testb $0x4,%al          (((Pointer) r).readInt(12) & 4) != 0
Main_extCField    55 bytes movl 0xc(%rdi),%eax ; testb $0x4,%al          (f.flags() & 4) != 0 — identical
```

- Typed **arguments** are free: the literal `value` field of a constant enum folds to the immediate, which SVM's own `@CEnumValue` did not do (7a.3: a load from `javaToC`).
- Typed **results** are not free in any form. The generated `of` removes SVM's virtual call and heap table, but an enum object still has to be produced and compared. This is why the plain-named import stays raw and the typed one is a second method.
- `@CField` is free, so records move to `@CStruct`.
- With the `@CEnum` annotations present but SVM's `lookup`/`cValue` unused, the image holds one 16-byte `CEnumNoLookup` per enum plus the enum's own instances (24 bytes each) and `Class`.
- Also confirmed in a third image: interface constants (`int SIZE = 16`) are accepted on a `@CStruct` interface, `SizeOf.get` agrees with them, `StackValue.get(IrFunction.class)` and a `@CFieldAddress` accessor work, an unannotated `interface Opaque extends PointerBase` is accepted as a `@CFunction` return, and the embedded-header directives build with no consumer configuration (image build 11.7 s, the same as without a C context).

### 7a.9 `@CBitfield` and default methods

`seam.h` variant with `union { uint32_t flags; struct { uint32_t exported : 1; uint32_t suspending : 1; uint32_t external : 1; } flagsBits; };` and `@CBitfield("flagsBits.external") boolean external();` on the interface. The nested name is accepted and the result is correct, but:

```text
Main_extCBitfield 65 bytes  movsbl 0xc(%rdi),%eax ; movzbl %al,%eax ; shll $0x1d,%eax ; testl $0x80000000,%eax
Main_extCField    55 bytes  movl 0xc(%rdi),%eax ; testb $0x4,%al
```

It also changes the C struct's source form and relies on the compiler's bitfield allocation order (LSB-first on every supported target, but implementation-defined in C). The obvious alternative, a default method on the interface, is rejected by the image builder:

```text
Error: Unexpected method without annotation
    method t.N$IrFunction.externalD()
```

So flag predicates are static methods in `<Class>Constants.<Flags>` over the `@CField` value, which compile to the 55-byte form. `@CBitfield` is not generated; it remains possible as an opt-in (open decision 6).

## 8. Experiment evidence

All under one `mktemp -d` directory; removed afterwards.

### 8.1 C forms and ThinLTO

`use.c` compares a word against (a) `UINT32_C(0x3141514b)`, (b) `static const uint32_t`, (c) `extern const uint32_t kf_magic_obj` defined in `def.ll` as `@kf_magic_obj = hidden local_unnamed_addr constant i32 …`.

```text
$ clang -O2 -S -emit-llvm use.c -o - | grep -E '^@|icmp|load'
@kf_magic_obj = external local_unnamed_addr constant i32, align 4
  %2 = icmp eq i32 %0, 826364235                      ; a: macro
  %2 = icmp eq i32 %0, 826364235                      ; b: static const
  %2 = load i32, ptr @kf_magic_obj, align 4, !tbaa !9 ; c: object
$ clang -O0 -c use.c && llvm-objdump -dr use0.o        # c(), no LTO
  movq (%rip), %rcx    R_X86_64_REX_GOTPCRELX kf_magic_obj-0x4
  cmpl (%rcx), %eax
$ clang -O2 -flto=thin -fuse-ld=lld main.o use.bc.o def.o -o app    # consumer is bitcode
  <c>: xorl %eax,%eax ; cmpl $imm32,%edi ; sete %al ; retq          # folded; llvm-nm: no kf_magic_obj
$ clang -O2 -flto=thin -fuse-ld=lld main.o use.native.o def.o -o app2   # consumer is machine code
  <c>: leaq -0x3cf3(%rip),%rcx  # <kf_magic_obj> ; cmpl (%rcx),%edi
  0000000000000bd4 0000000000000004 r kf_magic_obj
$ clang -std=c17 -pedantic -fsyntax-only en.c     # enum E { HIGH = 0x80000000u, BIG = 0x100000000ull };
  warning: enumerator value which exceeds the range of 'int' is a C23 extension (2147483648 is too large)
  warning: enumerator value which exceeds the range of 'int' is a C23 extension (4294967296 is too large)
```

### 8.2 Rust: repr enum vs newtype, flags

`rustc --edition 2021 --crate-type=lib -Copt-level=3 -Cpanic=abort --emit=llvm-ir,asm lib.rs`

```text
@switch.table.read_enum = private unnamed_addr constant [4 x i8] [i8 10, i8 20, i8 poison, i8 30]
define noundef range(i32 10, 31) i32 @read_enum(ptr … %r) {
  %_2 = load i32, ptr %r, align 4, !range !10, !noundef !11     ; !10 = !{i32 0, i32 4}
  %switch.gep = getelementptr inbounds nuw i8, ptr @switch.table.read_enum, i64 %0   ; no bounds check
@read_raw = unnamed_addr alias i32 (ptr), ptr @read_newtype                 ; newtype match ≡ raw match
@is_external_typed = unnamed_addr alias i1 (i32), ptr @is_external_raw      ; contains ≡ f & 4 != 0
@pack_when = unnamed_addr alias i32 (i1, i1), ptr @pack_shift               ; when()|when() ≡ e | s<<1
pack_shift:  leal (%rdi,%rsi,2), %eax ; retq
```

A foreign value of 2 yields poison and 5 reads past the table in `read_enum`; `read_newtype` range-checks and returns the default. Writing `when` as `if on { self.0 } else { 0 }` instead gave `lea; test; cmov`, hence the multiply form.

### 8.3 Java and Native Image

`javac -d out g/*.java`; `Use.viaConst` uses `MynaConstants.ANALYSIS_MAGIC`, `FunctionFlags.ALL/EXTERNAL`, `case Status.OK`/`INTERNAL_ERROR`; `Use.viaEnum` uses a hand-written Java enum.

```text
$ javap -c -p -cp out g.Use
  viaConst:  1: ldc #9 // int 826364235   9: bipush -8   19: iconst_4
            31: lookupswitch { 0: 56  3: 68  default: 73 }
  viaEnum:   0: getstatic g/Use$1.$SwitchMap$g$StatusEnum:[I
             4: invokestatic g/StatusEnum.of:(I)Lg/StatusEnum;
             7: invokevirtual g/StatusEnum.ordinal:()I
            10: iaload ; 11: lookupswitch …
$ java -Xlog:class+load -cp out g.Use | grep ' g\.'
  g.Use, g.Use$1, g.StatusEnum                       # no MynaConstants*
$ native-image -cp out -H:+UnlockExperimentalVMOptions -H:+PrintAnalysisCallTree -o use g.Use
$ grep -c MynaConstants reports/*.txt
  used_classes: 0   used_packages: 0   used_methods: 0   call_tree: 0
$ grep 'g\.' reports/used_classes_*.txt reports/used_methods_*.txt
  g.StatusEnum, g.Use, g.StatusEnum.of(int), g.StatusEnum.values(), g.Use.main, g.Use.viaEnum
  (call tree: StatusEnum.values() is an entry, "parsing reason: java.lang.reflect.Method.methodAccessor")
$ strings -a use | grep -c syntax_error      → 0     # Status.name() strings not in the image
$ strings -a use | grep -c SYNTAX_ERROR      → 1     # the Java enum's constant name is
```

### 8.4 Name tables

```text
Rust, name() never called:   grep '^@' names.ll  → (no globals)
Rust, name() called:         5 × private unnamed_addr constant [N x i8] c"…", align 1
                             @switch.table.status_name_ptr.rel = private unnamed_addr constant [5 x i32] [… sub(ptrtoint, ptrtoint) …]
C, static inline unused:     no .rodata section, only `T t1` in the object
C, used in two TUs:          .rodata.str1.1 … AMS (mergeable strings); linked without LTO: 1 copy of "syntax_error"
                             plus .data.rel.ro 0x20 per using TU (pointer table)
```

### 8.5 A `range` fact deletes a real check

```text
$ opt -O2 -S c.ll      # declare i32 @komodo_frontend_status(ptr); caller branches on == -1
  %neg = icmp eq i32 %s, -1 ; br i1 %neg, label %bad, …        ; kept
$ opt -O2 -S c.ll      # declare range(i32 0, 5) i32 @komodo_frontend_status(ptr)
define range(i32 0, 5) i32 @check(ptr %r) {
  %s = tail call i32 @komodo_frontend_status(ptr %r)
  ret i32 %s                                                    ; null-result path removed
```

## 9. Test plan

1. **Golden fixture** `tests/golden/constants/` with its own `.seam` (as `tests/golden/truffle/` does), so the main golden stays byte-identical: one const (`u32` hex), one `u64` const at `0xffffffffffffffff`, one `i64` const at the minimum, one enum with a negative value, one flag set using bit 31 of `u32`, a `u8` enum, and `enum=`/`flags=` on a field, a param, an import return and a callback return. Goldens for `seam.h`, `seam.rs`, `seam.ll`, `seam.json`, `seam.abi`, all Java files including `MynaConstants.java`.
2. **Fingerprint tests** (`SeamJson`): unchanged for every existing fixture; changes when a value, a name, a bit index, a carrier or a reference changes; identical for `0x10` vs `16` and for reordered `value` lines.
3. **Validation negatives**, one parameterized test per row of the table in section 4, asserting the message names the declaration. Boundary cases per carrier through the shared `fits` helper (reuse #125's argument table).
4. **Parser tests**: `enum=` on a `param` and on an import header is not treated as a fact; unknown option on `const` fails; `value` outside an enum fails with a line number; unterminated `enum` block.
5. **Generated-source compile checks**: C header under `-std=c11 -pedantic -Werror` and as C++17 with `_Static_assert`s on each macro's value and `sizeof`; `seam.rs` with `#![deny(warnings)]` and `const _: () = assert!(…)` on each constant, plus a `match` using the constants as patterns; Java compiled by javac with a `switch` over every enum constant. A generator-level assertion that every Java initializer is a literal and the class has no static block.
6. **LTO check** (`tests/llvm_integration.py`): C and Rust consumers using the constants, `-flto=thin` through LLD; assert no symbol or global derived from the constants (`llvm-nm`, saved optimized bitcode), the immediate appears in the compare, and no name string is in the binary when `*_name` is unused; with it used from two objects, exactly one copy of each string. Retain IR and disassembly under `build/reports/thinlto/<target>/`.
7. **Native Image check** (`tests/native_image_integration.py`): image whose Java side switches on an import's returned status and tests a record's flags through the constants; build with `-H:+PrintAnalysisCallTree` and assert `<Class>Constants` is absent from `used_classes_*.txt`; run it for the functional result. Fields are inlined by javac, so field use reaches no class; calling a flag predicate (`<Flags>.<bit>(flags)`, `hasUnknown`) reaches `<Class>Constants$<Flags>`, which has no initializer and is expected in the list, and only it.
   The same test, with `-H:-DeleteLocalSymbols`, disassembles and asserts:
   - a consumer comparing the raw import result uses an immediate;
   - a `@CField` read is a single load at the descriptor's offset;
   - `take(Status.X)` passes an immediate;
   - the raw import's stub contains no call into `CEnumRuntimeData` and the import is still a leaf.

   Functional cases: `statusEnum` returns `null` for an unlisted value while `status` returns the number; `StackValue.get` and `SizeOf` agree with `SIZE`; a consumer-written `@CFunction` (transition) taking and returning the generated `@CEnum` type builds and runs.
   Header check: a positive build, and a negative build against a `seam.h` regenerated from a descriptor with one changed enum value, asserting the image build fails with both fingerprints in the message. A build with a consumer `c_context` (the Bemo fixture) proves delegation.
   A generator-level test asserts that adding `enum=`/`flags=` never changes an import's transition.
8. **Truffle** (`tests/truffle_integration.py`): cells over annotated imports are unchanged (golden). New: `StatusNames.name` and `NativeConstants.readMember` must appear in the `-H:+PrintRuntimeCompileMethods` listing; a compiled guest loop reading `constants.Status.ok` and comparing it with a cell result must run without deoptimizing and without allocating (the existing allocation check); HotSpot run of the same guest code for FFM cells. `benchmarks/cells` gains one case comparing a guest compare against a literal with one against `constants.Status.ok`.
9. `--check` covers the new file for downstream checked-in outputs (Komodo).

## 10. Documentation

- `docs/dsl.md`: new section "Constants, enums and flag sets" (grammar, naming, the open-set rule, the statement that references add no checking and no optimizer facts); a sentence in "Determinism" on canonical ordering and decimal values; list the new JSON sections.
- `docs/performance.md`: the table from section 7 in short form with the retained evidence paths.
- `docs/thinlto.md`: one paragraph: constants are deliberately not link inputs.
- `docs/performance.md` also records the C-interface measurements (7a.2, 7a.3, 7a.8, 7a.9) with the retained disassembly and histogram, and why the plain-named import stays raw.
- `docs/truffle-cells.md`: `NativeConstants`, the name lookups, and the statement that cell results stay primitive.
- Release notes and `docs/releases.md`: the break table of section 13, with before/after snippets for record accessors and Rust signatures. `docs/binding-abi-2.md`: one line that the binding ABI version is unchanged.
- `README.md` output list: `<Class>Constants.java`. `AGENTS.md`: one line that constants stay compile-time literals in every backend (no globals, no Java enums).

## 11. Follow-ups mentioned, not in scope

- `success=Status.ok,Status.partial` on resources and `sentinel=Status.internal_error` on exports could resolve to numbers in the IR before serialization, leaving JSON and fingerprints as they are for numeric descriptors.
- Multi-bit fields and named combinations inside a flag set (`IrExpression.flags` packs a type code above bit 0).
- Guest strings as enum arguments in Truffle cells (`"ok"` coerced to 0): a slow-path convenience, not designed here.

## 12. Open decisions, ranked

1. **Rust shape.** Recommend the `#[repr(transparent)]` newtype with associated constants. Alternatives: bare `pub const`s in a `pub mod` (no type, no `match` exhaustiveness help either way), or `#[repr(int)] enum` (rejected: UB on foreign values, 8.2).
2. **Linkable LLVM constants.** Recommend none, and no `seam.ll` change (8.1).
3. **Range/closed-set optimizer facts.** Recommend none in this feature; a later explicit `closed=true@explicit_contract` if a consumer asks (8.5).
4. **Which import name is raw.** Recommend the plain name stays the raw leaf binding and the typed result is `<name>Enum` (7a.2, 7a.8: the typed result is never free and loses unlisted values; AGENTS.md requires generated bindings to match handwritten `@CFunction`s). Alternative: typed under the plain name and `<name>Raw` beside it, which is a larger break and makes the slower form the default.
5. **`@CStruct` for every record.** Recommend yes, for all descriptors, as the breaking change (identical code, typed pointers). It makes a C-compiler query part of every image build that uses a seam with structs; `-H:+UseCAPCache` covers builds without one. Alternative: keep static accessor classes and add the interfaces beside them for one release.
6. **`@CBitfield`.** Recommend not generating it (7a.9: slower by a shift, needs a C bitfield and an anonymous union in `seam.h`). Alternative: opt-in `c_bitfields=true` per flag set for consumers who want accessor methods on the struct interface and accept the cost.
7. **Typed opaque pointers.** `ptr<Opaque>` could become an `interface Opaque extends PointerBase` (accepted by the builder, free). Recommend doing it in the same breaking release; it is not required by #126.
8. **A Java enum on HotSpot.** Recommend none in `<Class>Constants` (ints only), so FFM consumers need no GraalVM SDK types and get the fastest form. Alternative: a plain un-annotated enum there with the same `value`/`of`, at the price of two enum types per group.
9. **Truffle guest constants object.** Recommend `NativeConstants` in `myna-truffle` plus generated name lookups, pending the runtime-compilation measurements in the test plan. Alternative: names only, leaving constants to language glue.
10. **Rust typed fields and signatures.** Recommend typing them (breaking, ABI-identical). Alternative: keep carriers and construct the newtype at use sites.
11. **C form.** Recommend typed macros only. Optional addition: a real enum type under `#if __STDC_VERSION__ >= 202311L || defined(__cplusplus)` so debuggers print names; it costs no runtime storage but doubles the C surface.
12. **Name functions.** Recommend always generating them for enums (zero cost unused, 8.4). Alternatives: opt-in per enum (`names=true`), or none. Flag sets get none in v1.
13. **`value ok = 0` vs `key=value`.** Recommend the issue's four-token form, strict. Alternative that keeps "every option is key=value": `value ok value=0`.
14. **Hex input and rendering.** Recommend decimal or `0x` hex input, canonical decimal strings in JSON, and the fixed rendering rule in section 6 (so equal fingerprints always give byte-identical outputs). Alternative: remember the source radix, which would need to be fingerprinted.
15. **Naming strictness.** Recommend lowercase value/bit names, UPPER_SNAKE const names, group names in the type namespace, `all`/`none` reserved in flag sets, and C-name collision detection on the final macro names.
16. **Canonical order.** Recommend sorting values by number and bits by index. Alternative: preserve declaration order (then reordering is an ABI change).
17. **Reference carrier match.** Recommend exact scalar equality and no pointers. Alternative: allow `ptr<carrier>` out-parameters.
18. **Integer carriers.** Recommend excluding `bool` and `char`; `usize`/`isize` allowed as 64-bit.

## 13. API breaks (for release notes; Komodo, TruffleData, Bemo)

The C ABI does not change: carriers, layouts, symbols, `seam.ll` contracts and `BINDING_ABI_VERSION` (2) are as today. Proposed as 0.19.0 with a BREAKING section. Fingerprints change for every descriptor, as they do in every release (`generatorVersion`).

| # | Break | Applies to | Who is affected | Migration |
| --- | --- | --- | --- | --- |
| 1 | Native Image records are `@CStruct` interfaces: `MynaNative.IrFunction.flags(record)` becomes `record.flags()`; the nested static accessor classes are gone; `SIZE`/`ALIGNMENT` move to the interface | every descriptor with a `struct`, whether or not it declares constants | Komodo (its Native Image record reads; its importer reads through FFM offsets and is unaffected), Bemo | mechanical: receiver call instead of static call |
| 2 | `ptr<Struct>` parameters and returns of `@CFunction`s, callback interfaces and export targets are the struct interface type instead of `PointerBase` | every descriptor passing struct pointers | Komodo, Bemo; export target methods must change their parameter types | `WordFactory.pointer(...)` call sites still infer the type; declared variables and export signatures need the new type |
| 3 | The Native Image class always has `@CContext(<Class>Directives)`, and the image build runs a C query against the embedded `seam.h`. With `--features=<package>.<Class>Directives`, the build fails if the header's fingerprint differs from the bindings' (`seam.h fingerprint <header> differs from generated bindings <bindings>`) | every Native Image consumer | Komodo, Bemo. A consumer `c_context` class is no longer the annotation value; the generated context instantiates it and delegates to it | none if a C compiler is available at image build (it already is for linking); otherwise prepare a CAP cache. Add the `--features` flag to opt into the check |
| 4 | New generated files: `<Class>Constants.java` (when constants are declared) and `<Class>Directives.java` (always); `native-image.properties` lists the new Truffle name classes | images that compile the Native Image class must also compile `<Class>Directives.java`, its `@CContext`, and any `c_context` delegate. FFM-only, JNI-only and Truffle-FFM-only images need nothing new | Komodo checks generated Java in, so its `--check` and source lists change; builds that compile the Native Image class without its context | add the files |
| 5 | Rust: annotated struct fields, `extern` signatures, `<Name>Fn` and callback aliases use the newtype; `assert_implementations!` requires it in implementations | descriptors that add `enum=`/`flags=` | Komodo's Rust provider once it annotates (`flags: u32` expressions become `FunctionFlags`) | wrap/unwrap with `.0`, or use the constants |
| 6 | Annotated imports gain `<name>Enum` methods and typed overloads; a descriptor whose logical names already end in `Enum` next to an annotated import is rejected | descriptors that add `enum=` | none known | rename |
| 7 | `myna-truffle` gains `NativeConstants`; `<Class>Truffle` gains `constants()` and `<Enum>Names` | Truffle consumers | TruffleData, Komodo: additive, but `constants` and `<Enum>Names` become reserved names | none |
| 8 | `seam.h` gains carrier typedefs and uses them in annotated fields and prototypes | descriptors that add references | source-compatible in C and C++ | none |

Not broken: FFM output (comments and nothing else), Truffle cell signatures and behaviour, leaf status of every import, export entry-point signatures at the C level, existing descriptors' text.

