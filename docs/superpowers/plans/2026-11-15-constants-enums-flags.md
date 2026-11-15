# Constants, Enums and Flag Sets (with integration fixes) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship myna#126 (named constants, enums and flag sets for every backend, using the best Native Image C-interface and Truffle APIs) on the `integration/constants` branch together with fixes for #120 and #121, the already-merged Detail PRs #124/#125, and dependency bumps, ending locally green for one combined PR.

**Architecture:** The descriptor gains `const`, `enum … end`, `flags … end` and `enum=`/`flags=` references. They flow through the IR, validation and canonical JSON (fingerprint), then each backend projects them in its own constant form: C typed macros + typedefs, Rust `#[repr(transparent)]` newtypes, a dependency-free `<Class>Constants.java`, and Native Image `@CEnum`/`@CConstant`/`@CStruct`/`@CField` (opt-in `@CBitfield`) with a generated `@CContext` that checks the header fingerprint at image build. Truffle cells keep primitive carriers; guests get PE-foldable names and constants. The C ABI does not change.

**Tech Stack:** Java 25 (generator, Elide build), GraalVM Native Image 25.5.5-elide.28 C interface, Truffle API + DSL (`myna-truffle`), Rust (edition 2024), C11/C++17 headers, LLVM 23 ThinLTO, Python integration tests.

**Spec:** `docs/superpowers/specs/2026-11-15-constants-enums-flags-design.md` (Fable design rev 3 plus the decisions header at its top). Section numbers below (§) refer to it. Issues: elide-dev/myna#126, #120, #121.

## Global Constraints

- Work only in `/build/worktrees/myna/integration-constants` on branch `integration/constants`. Run Elide builds/tests sequentially; never two at once in this checkout.
- C ABI unchanged: carriers, layouts, symbols, `seam.ll` contracts and `BINDING_ABI_VERSION` (2) stay as today.
- No linkable LLVM objects, no `seam.ll` change, no optimizer facts from enums (§6.3, §7).
- Every Java constant is a primitive `static final` with a literal initializer; no Java `enum` in `<Class>Constants`, no boxed values, arrays or static initializers there (§6.4).
- Cells never change a binding's leaf status, never embed addresses; everything on a cell's compiled path survives partial evaluation on Substrate; slow paths behind `@TruffleBoundary` (AGENTS.md).
- Adding `enum=`/`flags=` never changes an import's transition (§6.5).
- AOT FFM adapters stay build-time initialized; native symbol addresses runtime-initialized; no process addresses in the image heap.
- Descriptors that declare no constants/enums/flags serialize `seam.json` byte-for-byte as today; `schemaVersion` stays 3.
- Generator version (`Seam.VERSION`) stays `0.18.0` on this branch; the release PR bumps it and regenerates goldens.
- Golden outputs are regenerated, never hand-edited: `scripts/build.sh` then `build/dist/myna generate examples/<x>.seam --out tests/golden/<dir>` (main golden: `examples/buffer.seam` → `tests/golden`; `examples/truffle.seam` → `tests/golden/truffle`; new `examples/constants.seam` → `tests/golden/constants`).
- Gates: focused tasks while developing; `mise run test`, `mise run lint`, `python3 scripts/check-hygiene.py` at the end.

## Review Focus

1. **`seam.h` larger than 64 KiB** embedded in `<Class>Directives`: a single Java string constant over 65535 UTF-8 bytes fails javac, so the header is split into segments. The header text is image-build-only: `<Class>Directives` is used by the image builder and must never be reachable at run time, so no header bytes reach a consumer's image heap or binary. Tests in Task 11.
2. **Extreme values**: `u64` 0xffffffffffffffff, `i64`/`i32` minimum, bit 31 of `u32` and bit 63 of `u64` must render as compiling, correctly typed constants in C (`(-INT64_C(9223372036854775807) - 1)`), Rust and Java (`0xffffffffffffffffL`, `0x80000000`), and in `@CConstant`/`@CEnum` builds. Fixture in Task 5; asserted by Tasks 8–12.
3. **Unlisted native values**: `<name>Enum` returns `null` while the plain import returns the number; Rust newtypes hold any carrier value; Truffle `<Group>Names.name` returns `null`. Tests in Tasks 9, 12, 13.
4. **Consumer `c_context`** (Bemo) still applies its headers, libraries and options through delegation, and image builds leave no `myna-seam*` temp directory behind. Tests in Task 11.
5. **Existing descriptors** (no constants) still build and run end to end after the `@CStruct` / typed-pointer break: Bemo fixture, `tests/abi`, `tests/values`, ownership and callback fixtures. Covered by Task 10's integration run.

---

### Task 1: Dependency and pin bumps

**Files:**
- Modify: `mise.toml:11` (Elide `1.6.2` → `1.6.3`), `mise.lock` (via `mise lock`), `Dockerfile:33` (`ELIDE_VERSION`, plus any checksum next to it), `AGENTS.md:5`, `README.md:140,273`, `docs/ci.md:21` (also correct its stale toolchain `2026.11.5` → current pin), `docs/benchmarking.md:25-26,112`
- Modify: `Cargo.lock`, `crates/myna-values/fuzz/Cargo.lock`

- [ ] **Step 1:** Check `gh release view v2026.11.7 --repo elide-dev/toolchain`. If published, also bump every `2026.11.6` toolchain reference (`mise.toml` asset patterns and version, `AGENTS.md`, `README.md`, `docs/`, `Dockerfile` if present). If not published, leave it and note that in the commit message.
- [ ] **Step 2:** Bump Elide to 1.6.3 in the files above; run `mise lock` and `mise install`; then `mise run setup`.
- [ ] **Step 3:** `cargo update -p bumpalo --precise 3.20.4 && cargo update -p cc --precise 1.11.2 && cargo update -p errno --precise 0.3.15 && cargo update -p bit-vec --precise 0.10.2` at the root; `cargo update` in `crates/myna-values/fuzz`. Do not touch `allocator-api2` (dependabot ignore: ≥0.3 breaks bumpalo).
- [ ] **Step 4:** Run `mise run test:rust` and `mise run build:jvm`, then `scripts/test-jvm.sh`. Expected: all pass.
- [ ] **Step 5:** Commit `build(deps): Elide 1.6.3, crate patch updates` (name the toolchain if bumped).

### Task 2: #121 — a failed create always reports its status

**Files:**
- Modify: `src/main/java/dev/elide/myna/backend/TruffleCells.java:617-650` (the `status` and `noResource` arms)
- Modify: `src/truffle/java/dev/elide/myna/truffle/NativeStruct.java` (add the best-effort copy)
- Test: `src/test/java/dev/elide/myna/TruffleCellsTest.java`, `src/test/java/dev/elide/myna/truffle/TruffleRuntimeTest.java`
- Regenerate: `tests/golden/truffle/*`

**Interfaces:**
- Produces: `NativeStruct.copyToAfterFailure(Object guest, NativeCellException failure) -> NativeCellException` in `myna-truffle`: attempts the copy-back; a copy failure is added to `failure` as suppressed; returns `failure` for the caller to throw.

- [ ] **Step 1:** Write `TruffleRuntimeTest.failedCreateKeepsStatusWhenCopyBackFails`: a create with `handle=` and `success=0` returns status 7, and the guest buffer's interop `writeBufferByte` throws during copy-back. Assert the thrown exception is a `NativeStatusException` with `status() == 7` and one suppressed `NativeCellException` whose message is `buffer stopped taking the struct a create wrote`. Add `failedCreateWithNoResourceKeepsNoResourceWhenCopyBackFails` for the `created == 0` arm.
- [ ] **Step 2:** Run `elide test` filtered to `TruffleRuntimeTest`; expect both to fail (plain `NativeCellException` escapes).
- [ ] **Step 3:** Emit `throw out.copyToAfterFailure(arguments[i], NativeCellException.status(...))` (and the `noResource` equivalent) instead of `copyTo` then `throw`.
- [ ] **Step 4:** Add `TruffleCellsTest.failedCreateThrowsStatusAfterBestEffortCopy` asserting the generated text contains `throw out.copyToAfterFailure(` and no bare `out.copyTo(arguments[1]);\n    throw`. Regenerate truffle goldens. Run the JVM tests; expect pass.
- [ ] **Step 5:** Commit `fix(truffle): a failed create reports its status even when copy-back fails (#121)`.

### Task 3: #120a — held borrows check and hold their segment's scope for the create call

**Files:**
- Modify: `src/main/java/dev/elide/myna/backend/TruffleCells.java` (FFM backend, held-borrow argument path near lines 175-240 and 520-570)
- Modify: `src/truffle/java/dev/elide/myna/truffle/HeldBytesCoercion.java`, `Borrow.java` as needed
- Test: `src/test/java/dev/elide/myna/truffle/TruffleRuntimeTest.java`, `TruffleCellsTest.java`; regenerate `tests/golden/truffle/*`

- [ ] **Step 1:** Write `TruffleRuntimeTest.heldBorrowRefusesClosedSegment` (FFM backend): `Arena.ofShared()`, allocate, close, pass `NativeBytes.of(segment, false)` to a held-borrow create. Assert the cell throws (the same exception type and message call borrows produce for a closed scope; see the #116 tests for the expected form) and the native function was not invoked (fixture counter stays 0). Add `heldBorrowRefusesForeignThreadConfinedSegment` with `Arena.ofConfined()` from another thread.
- [ ] **Step 2:** Run; expect failure (native code is called with a dangling address).
- [ ] **Step 3:** For FFM cells, pass a held borrow's `NativeBytes` segment to the downcall as a `MemorySegment` argument so the linker checks and holds its scope for the call, exactly as call borrows do; the borrow still `keep()`s the buffer afterwards. Native Image backend unchanged.
- [ ] **Step 4:** Run JVM tests (including #124's held+2-call-borrows test); expect pass. Regenerate truffle goldens.
- [ ] **Step 5:** Commit `fix(truffle): held borrows check their segment's scope for the create call (#120)`.

### Task 4: #120b — lent copies are aligned; `align` facts are honored

**Files:**
- Modify: `src/truffle/java/dev/elide/myna/truffle/Borrow.java` (`Copy.of` at ~line 399, `Copy.held`, scratch pushes, in-place heap segments)
- Modify: `src/main/java/dev/elide/myna/backend/TruffleCells.java` (pass a parameter's `align` fact into its coercion)
- Modify: `src/main/java/dev/elide/myna/validate/Validator.java` (allow `align` on borrowed byte buffers and `truffle_buffer` parameters if not already)
- Test: `src/test/java/dev/elide/myna/truffle/TruffleRuntimeTest.java`; `docs/truffle-cells.md`

**Interfaces:**
- Produces: every copy a borrow lends (scratch, arena, malloc, held) starts on a 16-byte boundary (`Scratch.ALIGNMENT`). A parameter with `align=N@explicit_contract` is never lent in place at an address not divisible by N: such a heap array/segment is copied instead.

- [ ] **Step 1:** Write `TruffleRuntimeTest.lentCopiesAreSixteenByteAligned`: for lengths 1, 3, 255, 256, 4097 and after pushing an odd-length string first, lend a `byte[]` through call and held borrows (FFM and image-memory paths available in the test) and assert `address % 16 == 0` in the native fixture (record the address it receives). Add `alignFactForcesCopyOfMisalignedSegment`: `align=8@explicit_contract`, lend `heapSegment.asSlice(1)`; assert the address received is divisible by 8 and the bytes match.
- [ ] **Step 2:** Run; expect failure on the unaligned `Copy.of` arena path and on the slice.
- [ ] **Step 3:** Allocate `Copy.of` with `Scratch.ALIGNMENT`; ensure every scratch start is aligned; honor the `align` fact by copying when the in-place address is misaligned.
- [ ] **Step 4:** Run; expect pass. Document the 16-byte guarantee and `align` in `docs/truffle-cells.md`.
- [ ] **Step 5:** Commit `fix(truffle): aligned lent copies; align facts force a copy (#120)`.

### Task 5: Shared integer ranges, IR model and parser

**Files:**
- Create: `src/main/java/dev/elide/myna/validate/Ranges.java`, `src/main/java/dev/elide/myna/model/Names.java`
- Modify: `src/main/java/dev/elide/myna/model/Seam.java`, `src/main/java/dev/elide/myna/frontend/Dsl.java`, `src/main/java/dev/elide/myna/validate/Validator.java` (integer_sentinel, ffm_sentinel), `OwnershipValidator.java` (#125's success check)
- Create: `examples/constants.seam`
- Test: `src/test/java/dev/elide/myna/ConstantsTest.java` (new), existing `OwnershipTest`, `GeneratorTest`

**Interfaces:**
- Produces (`Ranges`, public): `static BigInteger min(Scalar s, TargetAbi abi)`, `static BigInteger max(Scalar s, TargetAbi abi)`, `static boolean fits(Scalar s, BigInteger value, TargetAbi abi)`; `usize`/`isize` 64-bit; `char` signedness from `abi.charUnsigned()`.
- Produces (IR, §3 exactly): `Constant`, `EnumValue`, `EnumGroup`, `FlagBit`, `FlagSet(String name, Scalar type, List<FlagBit> bits, boolean cBitfields)`, `GroupKind`, `GroupRef`; `Field.group`, `Parameter.group`, `Function.returnsGroup`, `Signature.returnsGroup`; `Module.constants()/enums()/flagSets()`, `Module.constantsClass()`, `Module.directivesClass()` (`nativeClass() + "Directives"`). Compat constructors keep every existing call site compiling. Enum values sorted by value, bits by index, groups/consts by name.
- Produces (`Names`, public): `upperSnake(String camel)` (`FunctionFlags` → `FUNCTION_FLAGS`), `lowerSnake(String camel)`, `lowerCamel(String upperSnake)` (`ANALYSIS_MAGIC` → `analysisMagic`), `cMacro(Module m, String group /*null for const*/, String name)` (`KOMODO_FRONTEND_STATUS_OK`, `KOMODO_FRONTEND_ANALYSIS_MAGIC`), `cTypedef(Module m, String group)` (`komodo_frontend_status_t`), `cNameFunction(Module m, String group)` (`komodo_frontend_status_name`), `cBitsStruct(Module m, String group)` (`komodo_frontend_function_flags_bits`).
- Produces (DSL, §2): `const`, `enum`/`flags` blocks with `value NAME = INTEGER` / `bit NAME = INDEX` lines (exactly four tokens), `flags … c_bitfields=true`, and `enum=`/`flags=` taken on `field`, `param` (before `facts`), import/export header (before `facts`) and callback header. `Dsl.integer(String) -> BigInteger` accepts `-?[0-9]+` and `0[xX][0-9a-fA-F]+`.

- [ ] **Step 1:** Write `ConstantsTest.rangesMatchCarriers` (parameterized: i8 −128..127, u8 0..255, i32, u32 0..4294967295, i64 min/max, u64 0..18446744073709551615, usize = u64, char signed on x86-64 and unsigned on `aarch64-unknown-linux-gnu`) and `ConstantsTest.parsesDeclarationsAndReferences` (parses `examples/constants.seam`; asserts the IR values, canonical sorting, `0x10` == `16`, and that `enum=` on a param/import header produced a `GroupRef` and no fact). Parser negatives: `value ok 0` → `line N: expected: value NAME = INTEGER`; `value` outside an enum → `line N: value requires an enum block`; `0xZZ` → `line N: invalid integer: 0xZZ`; unterminated `enum` → `unterminated block`; unknown option on `const` fails.
- [ ] **Step 2:** Run; expect compile failure / FAIL.
- [ ] **Step 3:** Implement `Ranges`, IR records, `Names`, parser. Replace the three inline range checks (integer_sentinel, ffm_sentinel, #125 success) with `Ranges.fits`, keeping their existing messages.
- [ ] **Step 4:** Write `examples/constants.seam` per §9.1: `u32` hex const, `u64` const `0xffffffffffffffff`, `i64` const at minimum, `i32` enum with a negative value, `u8` enum, `u32` flag set using bit 31, a `u64` flag set with `c_bitfields=true` and bit 63, a struct with an `enum=` field and a `flags=` field (one of them on the bitfield set), an import with `enum=` return (leaf, default), an import with an `enum=` param, a transition import (`native_leaf=false@explicit_contract`) with an `enum=` return, an opaque type used by pointer, a callback with `enum=` return, `truffle_cells=true`, `java_class=Consts`.
- [ ] **Step 5:** Run JVM tests; expect pass (existing goldens unchanged). Commit `feat(dsl): constants, enums and flag sets in the descriptor (#126)`.

### Task 6: Validation

**Files:**
- Create: `src/main/java/dev/elide/myna/validate/ConstantsValidator.java` (called from `Validator.validate` before `OwnershipValidator`)
- Test: `src/test/java/dev/elide/myna/ConstantsTest.java`

**Interfaces:**
- Consumes: `Ranges`, `Names`, IR from Task 5.
- Produces: every rule and message in §4's table, verbatim, plus: `c_bitfields` requires an unsigned carrier (`flags F: c_bitfields requires an unsigned carrier, found i32`); a logical name equal to `<name>Enum` of an import with an `enum=` return is rejected (`logical name collides with the typed enum wrapper of status: statusEnum`); `<Class>Constants` and `<Class>Directives` collide-checks against declared types like the Truffle class; group names and `<Group>Names` checked against generated Truffle names when cells are on; generated `@CBitfield` accessor names (`<field><Bit>` lower camel, e.g. `flagsExported`) must not collide with other accessors of the struct.
- C-name collision set is seeded with `<MODULE>_SEAM_ABI`, `<MODULE>_BINDING_ABI_VERSION`, `<MODULE>_SEAM_H`, and includes every macro, `_ALL`, typedef, name function and bits-struct name.

- [ ] **Step 1:** Write `ConstantsTest.rejects` parameterized over one descriptor snippet per rule (each §4 row plus the additions above), asserting `IllegalArgumentException` whose message equals the expected text.
- [ ] **Step 2:** Run; expect FAIL.
- [ ] **Step 3:** Implement `ConstantsValidator`.
- [ ] **Step 4:** Run; expect pass. Commit `feat(validate): constants, enums and flag sets (#126)`.

### Task 7: Canonical JSON and fingerprint

**Files:**
- Modify: `src/main/java/dev/elide/myna/serialize/SeamJson.java`
- Test: `src/test/java/dev/elide/myna/ConstantsTest.java`; create `tests/golden/constants/seam.json`, `seam.abi` (regenerated)

- [ ] **Step 1:** Write `ConstantsTest.fingerprintTracksValues`: existing fixtures (`examples/*.seam`) keep their current `seam.abi` (compare against `tests/golden/seam.abi` and `tests/golden/truffle/seam.abi`); fingerprint changes when a value, a value name, a bit index, a carrier, `c_bitfields` or a reference changes; is identical for `0x10` vs `16` and for reordered `value` lines. `canonicalShape` asserts the §5 JSON fragments, with `"cBitfields":true` only when set.
- [ ] **Step 2:** Run; expect FAIL. **Step 3:** Implement §5 (sections after `"functions"`, before `"resources"`; references appended only when present; values as decimal strings). **Step 4:** Run; pass. **Step 5:** Commit `feat(json): constants, enums and flag sets in the canonical descriptor (#126)`.

### Task 8: C header

**Files:**
- Modify: `src/main/java/dev/elide/myna/backend/CHeader.java`, `Types.java` (typedef-aware field/param/return types)
- Test: `src/test/java/dev/elide/myna/ConstantsTest.java`; `tests/llvm_integration.py` (compile check); regenerate `tests/golden/constants/seam.h`

**Interfaces:**
- Produces: §6.1 output. Rendering: enum values decimal; flag masks and unsigned consts hex; signed consts decimal; negatives parenthesized; minimums as `(-INT64_C(9223372036854775807) - 1)` / `(-INT32_C(2147483647) - 1)` (and i8/i16 analogues). Per group `typedef <carrier> <cTypedef>;`, used in annotated fields and prototypes. Per enum `static inline const char *<cNameFunction>(<carrier> value)` returning `NULL` when unlisted. Per flag set `<MACRO>_ALL`. For a `c_bitfields=true` flag set: a top-level `struct <cBitsStruct> { <ctype> <bit> : 1; <ctype> : <gap>; … };` (unnamed gaps keep bit positions; LSB-first), and each annotated field becomes `union { <typedef> <field>; struct <cBitsStruct> <field>Bits; };` so `offsetof(struct S, <field>)` and existing asserts still hold.

- [ ] **Step 1:** Write `ConstantsTest.cHeaderRendersExtremes` asserting exact lines for the fixture's u64 max, i64 min, i32 negative, bit 31 mask (`UINT32_C(0x80000000)`), typedef use in a prototype, and the bitfield union. In `tests/llvm_integration.py` add `test_constants_header`: compile `tests/golden/constants/seam.h` with `-std=c11 -pedantic -Werror` and as C++17 (`-std=c++17 -pedantic -Werror`), with a TU of `_Static_assert`s on each macro's value and `sizeof`, a `switch` using enum macros as case labels, and a runtime check that setting `<field>Bits.<bit>` sets exactly the mask bit (LSB-first assumption).
- [ ] **Step 2:** Run; FAIL. **Step 3:** Implement. **Step 4:** Regenerate `tests/golden/constants/seam.h`; run `elide test` (ConstantsTest) and `scripts/python.sh tests/llvm_integration.py`; pass. **Step 5:** Commit `feat(c): typed constant macros, typedefs, name functions and opt-in bitfields (#126)`.

### Task 9: Rust

**Files:**
- Modify: `src/main/java/dev/elide/myna/backend/RustFfi.java`, `RustOwnership.java`, `Types.java` (newtype-aware Rust types, including `assert_implementations!`, `native_image_shims!`, `<Name>Fn`, callback aliases)
- Test: `ConstantsTest`; `tests/llvm_integration.py` or the Rust fixture used by `tests/e2e.py`; regenerate `tests/golden/constants/seam.rs`

**Interfaces:**
- Produces: §6.2 exactly: `pub const` for consts; per enum `#[repr(transparent)] #[derive(Clone, Copy, PartialEq, Eq, Hash, Debug)] pub struct G(pub <carrier>)` with associated consts and `pub const fn name(self) -> Option<&'static str>`; per flag set the same newtype plus `NONE`, `ALL`, `contains`, `intersects`, `union`, `when` (multiply form), `BitOr`, `BitAnd`. Annotated struct fields, extern signatures, `<Name>Fn` and callback aliases use the newtype.

- [ ] **Step 1:** Add a Rust check crate (or extend the existing generated-crate test) that includes `tests/golden/constants/seam.rs` under `#![deny(warnings)]`, has `const _: () = assert!(...)` on every constant's `.0`, a `match` using enum constants as patterns with a `_` arm, an implementation of each import checked by `assert_implementations!`, and a test that `Status(12345).name()` is `None` (unlisted values stay representable).
- [ ] **Step 2:** Run; FAIL. **Step 3:** Implement. **Step 4:** Regenerate golden; run `mise run test:rust` and the fixture test; pass. **Step 5:** Commit `feat(rust): transparent newtypes for enums and flag sets (#126)`.

### Task 10: Native Image records as `@CStruct`, typed struct and opaque pointers (breaking)

**Files:**
- Modify: `src/main/java/dev/elide/myna/backend/NativeImage.java` (replace static accessor classes at ~374-430), `Types.java` (Native Image pointer types), `NativeOwnership.java`, `JniAbi.java`, `StaticJni.java`, `NativeImageProperties.java` if it lists accessor classes
- Modify fixtures that use accessors or `PointerBase` signatures: `tests/abi/BufferOps.java`, `tests/values/ValuesMain.java`, `tests/bemo/*`, `tests/callbacks/*`, `tests/ownership/*`, `tests/native_image_integration.py`
- Regenerate: `tests/golden/*`, `tests/golden/truffle/*`

**Interfaces:**
- Produces: each record `R` is `@CStruct(value = "R", addStructKeyword = true) public interface R extends PointerBase` nested in `<Class>`, with `int SIZE`, `int ALIGNMENT`, `@CField("f") T f()`, `@CField("f") void f(T value)`, `@CFieldAddress("f") P addressOfF()` (`addressOf` + upper-camel field name). Each opaque type `O` is `public interface O extends PointerBase {}` nested in `<Class>`. `ptr<R>` / `ptr<O>` in fields, `@CFunction` params/returns, callback interfaces and export targets use `R` / `O`; `ptr<ptr<…>>` keeps today's types. Unsigned and narrow fields follow whatever SVM's `@CField` signedness rules require for the raw-bits carriers; record the rule in a code comment.

- [ ] **Step 1:** Write `GeneratorTest.nativeImageRecordsAreCStructs` asserting the generated interface text for a struct with every scalar type, a `ptr<Struct>` field and a `ptr<Opaque>` field, and `typedOpaquePointers` for a `@CFunction` returning `ptr<Opaque>`.
- [ ] **Step 2:** Run; FAIL. **Step 3:** Implement; update fixtures and the other backends that reference the Native Image class.
- [ ] **Step 4:** Regenerate goldens. Run `scripts/test-jvm.sh`, `scripts/build.sh`, `scripts/python.sh tests/e2e.py`, `scripts/python.sh tests/native_image_integration.py`. Add to the native image test: `StackValue.get(R.class)` and `SizeOf.get(R.class)` equal `R.SIZE`; a `@CField` read disassembles to one load at the descriptor's offset (`-H:-DeleteLocalSymbols`, objdump of the measured method). Expected: all pass, including Bemo.
- [ ] **Step 5:** Commit `feat(native-image)!: records are @CStruct interfaces; typed struct and opaque pointers`.

### Task 11: Native Image C context, header check and `@CConstant`

**Files:**
- Create (generated): `<Class>Directives.java` via new `src/main/java/dev/elide/myna/backend/NativeImageDirectives.java` (registered in `Generator`)
- Modify: `NativeImage.java` (`@CContext(<Class>Directives.class)` always; `@CConstant` accessors), `NativeImageForeign.java` (header check in the generated Feature), `Dsl`/docs semantics of `c_context` (now delegated)
- Test: `GeneratorTest`, `tests/native_image_integration.py`, `tests/bemo/*`

**Interfaces:**
- Produces: `<Class>Directives implements CContext.Directives` with a public no-arg constructor. Holds `seam.h` text split into string constants of at most 16 KiB each, joined at first use; `getHeaderFiles()` writes it once per builder JVM to a `Files.createTempDirectory("myna-seam")` directory deleted by a shutdown hook, and returns the consumer's headers first, then `"\"<dir>/seam.h\""`. When the descriptor names `c_context`, an instance of that class is created and every other `Directives` method delegates to it (`isInConfiguration`, `getMacroDefinitions`, `getOptions`, `getLibraries`, `getLibraryPaths`).
- Produces: in `<Class>`: `@CConstant("<MODULE>_SEAM_ABI") static native String seamHeaderAbi();` and per const `@CConstant("<macro>") public static native <carrier> <lowerCamel(NAME)>();`. The generated Feature reads `CConstant.ValueAccess.get(<Class>.class, "seamHeaderAbi", String.class)` in `beforeAnalysis` and throws `IllegalStateException("seam.h fingerprint " + header + " differs from generated bindings " + ABI_FINGERPRINT)` on mismatch.

- [ ] **Step 1:** Add native image tests: `header_check_passes` (fixture builds and prints `analysisMagic()` == literal); `header_check_rejects_stale_header` (regenerate `seam.h` from a copy of the fixture with one enum value changed, build, assert the build fails and the message contains both fingerprints); `bemo_context_delegates` (Bemo fixture builds and runs with its own directives); `no_temp_dirs_left` (no `myna-seam*` under the temp dir after the builds); `header_text_build_time_only` (`<Class>Directives` absent from `used_classes_*.txt` under `-H:+PrintAnalysisCallTree`, and a distinctive line of `seam.h`, e.g. its `#define <MODULE>_SEAM_ABI` line, absent from `strings -a` of the image: consumers carry no header text at run time); `large_header` (a generated descriptor with enough structs to make `seam.h` > 64 KiB builds). `GeneratorTest.directivesSplitLargeHeaders` asserts no string constant over 16 KiB.
- [ ] **Step 2:** Run; FAIL. **Step 3:** Implement. **Step 4:** Regenerate goldens; run the JVM tests and `tests/native_image_integration.py`; pass. **Step 5:** Commit `feat(native-image)!: generated C context checks seam.h at image build; @CConstant accessors (#126)`.

### Task 12: `<Class>Constants.java`, `@CEnum` enums, typed wrappers, opt-in `@CBitfield`

**Files:**
- Create: `src/main/java/dev/elide/myna/backend/JavaConstants.java` (registered in `Generator` when the module declares any const/enum/flag set)
- Modify: `NativeImage.java`, `JavaFfm.java` (comments only), `Types.java`
- Test: `ConstantsTest`, `GeneratorTest`, `tests/native_image_integration.py`, `tests/ffm_binding.py`; regenerate `tests/golden/constants/*`

**Interfaces:**
- Produces `<Class>Constants` (§6.4): consts as `public static final <carrier>`; per enum nested class with value fields and `public static String name(<carrier> value)`; per flag set nested class with masks, `ALL`, one `public static boolean <bit>(<carrier> flags)` predicate per bit and `hasUnknown(<carrier>)`. No imports. Not listed in `native-image.properties`.
- Produces in `<Class>` (§6.5): per enum `@CEnum("<cTypedef>") public enum G` with `@CEnumConstant("<macro>") VALUE(<literal>)`, `public final <carrier> value`, `@CEnumValue public native <carrier> cValue()`, `@CEnumLookup public static native G lookup(<carrier> value)`, `public static G of(<carrier> value)` (switch, `null` when unlisted). For an import with `enum=` return: the plain binding unchanged plus `public static G <name>Enum(<params>) { return G.of(<name>(<args>)); }`. For params with `enum=`: an overload taking `G` and passing `.value`. Exports and callbacks keep raw carriers. For a `c_bitfields=true` flag set on a field: `@CBitfield("<field>Bits.<bit>") boolean <field><Bit>()` and setter on the `@CStruct` interface.

- [ ] **Step 1:** Tests: `ConstantsTest.javaConstantsAreLiterals` (parse the generated class: every field initializer is a literal or a cast of one, no `static {`, no imports); javac-compile a consumer with a `switch` over every enum constant and mask predicates, run on HotSpot. Native image test `constants_and_enums`: the consumer switches on the raw import result (disassembly shows an immediate compare and no call into `CEnumRuntimeData` in the raw import's stub; the import is still `NO_TRANSITION`); `statusEnum` returns `null` for an unlisted value while `status` returns the number; `take(G.X)` passes an immediate; a consumer-written transitioning `@CFunction` taking and returning `G` builds and runs; `<Class>Constants` absent from `used_classes_*.txt` (`-H:+PrintAnalysisCallTree`); `@CBitfield` accessors read/write the same bits as the masks. `GeneratorTest.enumReferencesNeverChangeTransition`.
- [ ] **Step 2:** Run; FAIL. **Step 3:** Implement. **Step 4:** Regenerate goldens; run JVM tests, `tests/ffm_binding.py`, `tests/native_image_integration.py`; pass. **Step 5:** Commit `feat(java): constants class, @CEnum enums, typed wrappers and opt-in @CBitfield (#126)`.

### Task 13: Truffle names and guest constants

**Files:**
- Create: `src/truffle/java/dev/elide/myna/truffle/NativeConstants.java`
- Modify: `src/main/java/dev/elide/myna/backend/TruffleCells.java` (nested `<Group>Names`, `constants()`), `NativeImageProperties.java` (build-time init of the new classes)
- Test: `src/test/java/dev/elide/myna/truffle/TruffleRuntimeTest.java`, `TruffleCellsTest`, `tests/truffle_integration.py`, `tests/truffle/*`, `benchmarks/cells/*`

**Interfaces:**
- Produces: `public final class NativeConstants implements TruffleObject` exporting `hasMembers`, `getMembers`, `isMemberReadable`, `readMember` (cached specialization keyed on member-name identity, uncached path `@TruffleBoundary`); built from `@CompilationFinal(dimensions = 1)` name and value arrays; group members are nested `NativeConstants`; `u64` values above `Long.MAX_VALUE` as raw `long` bits. Generated `<Class>Truffle.constants()` returns a build-time-initialized instance; `<Class>Truffle.<Group>Names.name(<carrier>) -> TruffleString` (`TruffleString.fromConstant(..., UTF_8)`, `null` when unlisted). Cell results stay primitive; cells unchanged (golden diff limited to the new members).

- [ ] **Step 1:** Tests: `TruffleRuntimeTest.nativeConstantsReadMembers` (`constants.Status.ok`, `constants.FunctionFlags.external`, `constants.ANALYSIS_MAGIC`, unknown member → `UnknownIdentifierException`); `namesReturnNullWhenUnlisted`. In `tests/truffle_integration.py`: `<Group>Names.name` and `NativeConstants.readMember` appear in `-H:+PrintRuntimeCompileMethods`; a compiled guest loop reading `constants.Status.ok` and comparing with a cell result runs without deoptimizing and without allocating (existing allocation check); same guest code on HotSpot with FFM cells. Add a `benchmarks/cells` case (literal vs `constants.Status.ok`), advisory.
- [ ] **Step 2:** Run; FAIL. **Step 3:** Implement. **Step 4:** Run `scripts/test-jvm.sh`, `scripts/python.sh tests/truffle_integration.py`; pass; record the benchmark result in the commit message. **Step 5:** Commit `feat(truffle): enum names and guest-visible constants that fold (#126)`.

### Task 14: LTO and image evidence

**Files:**
- Modify: `tests/llvm_integration.py`, `tests/native_image_integration.py`
- Evidence under `build/reports/thinlto/<target>/` and `build/reports/native-image/constants/` (retained by the tests, not committed)

- [ ] **Step 1:** Add `test_constants_lto`: C and Rust consumers of `tests/golden/constants` linked with `-flto=thin` through LLD; assert no symbol derived from a constant (`llvm-nm`), the immediate appears in the compare (saved optimized bitcode and disassembly), no name string in the binary when `*_name` is unused, exactly one copy of each string when it is used from two objects.
- [ ] **Step 2:** Run `scripts/python.sh tests/llvm_integration.py`; pass. Ensure Task 12's disassembly and heap-histogram evidence is retained.
- [ ] **Step 3:** Commit `test: LTO and image evidence for constants (#126)`.

### Task 15: Documentation and release notes draft

**Files:**
- Modify: `docs/dsl.md` (new "Constants, enums and flag sets" section; "Determinism" sentence; `c_context` delegation in the Native Image section), `docs/performance.md`, `docs/thinlto.md`, `docs/truffle-cells.md`, `docs/binding-abi-2.md`, `README.md` (output list: `<Class>Constants.java`, `<Class>Directives.java`), `AGENTS.md` (one line: constants stay compile-time literals in every backend; no globals, no Java enums in `<Class>Constants`)
- Create: `docs/releases/unreleased.md` with the §13 break table (rows 1–8 plus the `@CBitfield` opt-in and typed opaque pointers), before/after snippets for record accessors and Rust signatures, and the #120/#121/#122/#123 fixes and dependency bumps

- [ ] **Step 1:** Write the docs. **Step 2:** Run `python3 scripts/check-hygiene.py` and `mise run check:style`; pass. **Step 3:** Commit `docs: constants, enums and flag sets; breaking changes for the next release (#126)`.

### Task 16: Full gate

- [ ] **Step 1:** `mise run test` (no `MYNA_SKIP_MIRI_FUZZ`), `mise run lint`, `python3 scripts/check-hygiene.py`, `mise run check:repeatable`. Expected: all exit 0.
- [ ] **Step 2:** `git log --oneline origin/main..HEAD` and `git diff --stat origin/main` reviewed; tree clean.
- [ ] **Step 3:** Stop and report to the user with the gate results; the combined PR is pushed only on their go-ahead.
