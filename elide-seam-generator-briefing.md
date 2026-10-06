# Elide Seam Generator — Implementation Briefing

## Objective

Build a single, canonical **Seam Generator** for Elide that describes and generates ABI boundaries between execution domains.

The generator should unify the metadata and code generation currently needed for:

- Java FFM bindings
- GraalVM Native Image C API / `@CEntryPoint` bindings
- C-compatible headers
- Rust `extern "C"` bindings
- ABI adaptation shims
- LLVM declaration / contract modules for ThinLTO-aware optimization

The central design principle is:

> **Do not build separate FFM, C API, Rust, and LLVM generators. Build one language-neutral Seam IR and multiple backends over it.**

The first implementation should prioritize ABI correctness and deterministic code generation. Optimizer metadata should be supported from the beginning, but only emitted where it is explicitly declared or mechanically proven safe.

---

# 1. Scope

## 1.1 In scope

The generator must represent and emit:

- exported functions
- imported functions
- callbacks
- symbol names
- calling conventions
- scalar argument and return types
- pointer arguments and returns
- structs / records
- enums
- nullability
- mutability / access intent
- ownership and lifetime conventions
- error / exception translation conventions
- ABI adaptation shims
- generated Java FFM descriptors and handles
- generated Native Image C entry points or compatible glue
- generated Rust FFI declarations
- generated C headers
- generated LLVM IR declarations and optional wrapper definitions
- semantic optimizer contracts with provenance

## 1.2 Explicitly out of scope for the first implementation

Do **not** attempt to:

- translate arbitrary Graal IR into LLVM IR
- reconstruct LLVM IR from Native Image machine code
- model every function in a Native Image executable
- automatically infer aggressive LLVM aliasing guarantees such as `noalias`
- replace Native Image's normal compiler backend
- manufacture fake FatLTO objects whose embedded IR claims to implement machine-code functions
- make LLVM optimize inside opaque Native Image-generated functions

The optimizer goal is narrower:

> Let LLVM optimize **around** the Native Image boundary by giving it an accurate declaration-level contract for seam functions.

---

# 2. Architecture

The intended architecture is:

```text
Java/Kotlin declarations
annotations / DSL / generated metadata
Native Image analysis facts
manual ABI overrides
             |
             v
        +-----------+
        |  Seam IR  |
        +-----------+
             |
   +---------+----------+-----------+------------+-------------+
   |                    |           |            |             |
   v                    v           v            v             v
C header             Java FFM    NI C glue    Rust FFI    LLVM contract
generator             generator   generator    generator      generator
```

Every backend must consume the same normalized Seam IR.

No backend should independently rediscover ABI facts from source declarations.

---

# 3. Core Principle: Seam IR Is the Source of Truth

The Seam IR should model the ABI independently of Java, C, Rust, or LLVM syntax.

Example conceptual representation:

```yaml
function:
  logicalName: bufferRead
  symbol: elide_buffer_read
  direction: export
  callingConvention: c

  return:
    type: i64

  parameters:
    - name: buffer
      type:
        kind: pointer
        pointee: ElideBuffer
      nullable: false
      capture: false
      access: read

    - name: destination
      type:
        kind: pointer
        pointee: u8
      nullable: false
      capture: false
      access: write

  effects:
    throwsAcrossBoundary: false
    returnsNormally: true
    memory:
      readsArgumentMemory: true
      writesArgumentMemory: true
      readsGlobalMemory: false
      writesGlobalMemory: false
```

This representation is illustrative, not a mandated serialization format.

Internally, prefer typed classes / sealed interfaces over loosely typed maps.

---

# 4. Recommended IR Model

## 4.1 Module

A seam module represents one generated ABI namespace.

Suggested fields:

```text
SeamModule
- name
- abiVersion
- targetTriple
- dataLayout
- namespace / symbolPrefix
- functions[]
- structs[]
- enums[]
- constants[]
- metadata
```

The IR should be target-aware where ABI layout depends on the target.

Do not hard-code LP64 assumptions globally.

---

## 4.2 Function

Suggested model:

```text
SeamFunction
- logicalName
- symbolName
- direction
    - IMPORT
    - EXPORT
    - CALLBACK
- callingConvention
- visibility
- returnType
- parameters[]
- errorConvention
- semanticContract
- source
```

`logicalName` is the language-neutral identity.

`symbolName` is the exact linker-visible symbol.

These must not be conflated.

---

## 4.3 Parameter

Suggested model:

```text
SeamParameter
- name
- type
- nullable
- ownership
- lifetime
- access
- capture
- alignment
- dereferenceableBytes
- semanticContract
```

Recommended enums:

```text
Ownership:
- BORROWED
- OWNED_BY_CALLER
- OWNED_BY_CALLEE
- TRANSFERRED_TO_CALLEE
- TRANSFERRED_TO_CALLER
- UNSPECIFIED

Access:
- NONE
- READ
- WRITE
- READ_WRITE
- UNSPECIFIED

Capture:
- NO_CAPTURE
- MAY_CAPTURE
- UNSPECIFIED
```

Do not infer strong optimizer attributes from `UNSPECIFIED`.

---

## 4.4 Types

Implement a normalized ABI type system.

Minimum useful set:

```text
SeamType
- Void
- Bool(width, representation)
- Integer(width, signedness)
- Float(width)
- Pointer(pointee?, addressSpace?)
- Array(element, count)
- Struct(name, fields, layout)
- Enum(name, underlyingType, values)
- FunctionPointer(signature)
- OpaqueHandle(name)
```

Avoid making Java classes directly represent ABI types.

For example:

```text
MemorySegment
```

is not itself an ABI type.

Depending on context it may lower to:

```text
ptr
```

or to a structured descriptor representing pointer + length.

---

# 5. Semantic Contracts

Semantic optimizer information must be represented separately from basic ABI shape.

Suggested function-level concepts:

```text
- noUnwind
- noReturn
- willReturn
- noFree
- memoryEffects
- allocator
- deallocator
- allocationSizeParameter
- allocationAlignmentParameter
```

Suggested parameter-level concepts:

```text
- nonNull
- noCapture
- readOnly
- writeOnly
- noAlias
- alignment
- dereferenceableBytes
```

Do not map these directly from arbitrary Java semantics.

LLVM attributes have exact optimizer-visible meanings and can cause miscompilation if they are too strong.

---

# 6. Provenance Is Mandatory

Every semantic fact capable of changing optimizer behavior should carry provenance.

Example:

```text
SemanticFact<T>
- value
- provenance
- confidence
- sourceLocation?
- explanation?
```

Recommended provenance kinds:

```text
EXPLICIT_CONTRACT
ABI_DERIVED
LANGUAGE_DERIVED
COMPILER_PROVEN
HEURISTIC
```

Recommended emission policy:

| Provenance | Emit as LLVM optimizer contract? |
|---|---|
| EXPLICIT_CONTRACT | Yes |
| ABI_DERIVED | Yes, where semantics are exact |
| LANGUAGE_DERIVED | Only for clearly equivalent semantics |
| COMPILER_PROVEN | Yes |
| HEURISTIC | No by default |

The LLVM backend should have a strict mode that rejects unsafe or ambiguous attributes.

---

# 7. First-Pass Attribute Policy

Start conservative.

## Safe or relatively straightforward candidates

When proven or explicitly declared:

- `nonnull`
- `nocapture`
- `readonly`
- `writeonly`
- `nounwind`
- `noreturn`
- `willreturn`
- alignment
- `dereferenceable(N)`
- `allocsize`
- allocator-like return `noalias`, if the allocation contract really guarantees it

## High-risk candidates

Do not infer automatically in the first implementation:

- argument `noalias`
- broad `memory(none)`
- broad `memory(read)`
- `speculatable`
- `nosync`
- assumptions about GC or object reachability
- assumptions derived only from Java reference identity
- assumptions derived only from local source inspection

`noalias` in particular should require an explicit contract or compiler proof.

---

# 8. Source Frontend

The frontend should discover seam declarations and normalize them into Seam IR.

Possible input mechanisms can include:

- Java/Kotlin annotations
- existing FFM declarations
- Native Image `@CEntryPoint` declarations
- a dedicated Elide seam annotation DSL
- generated metadata from build tooling
- compiler analysis facts
- explicit configuration for exceptional ABI cases

Recommended direction:

```java
@SeamExport(symbol = "elide_buffer_length")
static long bufferLength(
    @SeamNonNull
    @SeamNoCapture
    @SeamReadOnly
    NativeBuffer buffer
) { ... }
```

The exact annotation API is not prescribed here.

The important property is:

> Source declarations are normalized into the same internal representation regardless of how they were expressed.

---

# 9. Native Image Integration

The generator should integrate with Native Image through a build-time feature or equivalent build tooling.

Responsibilities may include:

- locating seam-exported methods
- validating supported signatures
- resolving exact native symbols
- generating `@CEntryPoint` glue where required
- supplying reachability metadata if required
- collecting conservative compiler-proven properties
- emitting Seam IR before final image linkage

The first version should **not** depend on deep Graal compiler internals unless necessary.

Prefer explicit contracts first.

Compiler-derived enrichment can be added incrementally.

---

# 10. Backends

## 10.1 C Header Backend

Generate stable C-compatible declarations.

Example:

```c
int64_t elide_buffer_read(
    const struct ElideBuffer *buffer,
    uint8_t *destination
);
```

Responsibilities:

- symbol declaration
- portable integer widths
- struct definitions
- enums
- visibility macros
- calling-convention macros
- ownership/nullability documentation where C cannot encode them
- generated-file version / ABI identifier

This output should be consumable independently of Elide build tooling.

---

## 10.2 Rust Backend

Generate Rust FFI declarations.

Example:

```rust
unsafe extern "C" {
    pub fn elide_buffer_read(
        buffer: *const ElideBuffer,
        destination: *mut u8,
    ) -> i64;
}
```

Optionally generate thin safe wrappers later.

For the initial version, focus on correct raw ABI declarations.

Do not over-promise Rust lifetimes from weak seam metadata.

---

## 10.3 Java FFM Backend

Generate:

- `MemoryLayout`s
- `FunctionDescriptor`s
- downcall handles
- upcall descriptors
- symbol lookup glue
- struct layouts
- constant offsets
- marshalling helpers where explicitly modeled

Example output shape:

```java
static final FunctionDescriptor ELIDE_BUFFER_READ =
    FunctionDescriptor.of(
        ValueLayout.JAVA_LONG,
        ValueLayout.ADDRESS,
        ValueLayout.ADDRESS
    );
```

Avoid hand-maintained duplicated descriptors.

---

## 10.4 Native Image C API Backend

Generate or validate:

- `@CEntryPoint` wrappers
- imported native symbols
- callback trampolines
- exception translation
- native thread / isolate adaptation where required

Keep ABI adaptation explicit in Seam IR.

For example:

```text
Java exception
    ->
status code + out parameter
```

should be modeled, not hidden in a backend-specific special case.

---

## 10.5 LLVM Contract Backend

Generate LLVM IR or bitcode containing declarations for seam functions.

Example:

```llvm
declare i64 @elide_buffer_read(
    ptr nonnull nocapture readonly %buffer,
    ptr nonnull nocapture writeonly %destination
) nounwind willreturn
```

The output may be:

```text
elide-seam.ll
```

and/or:

```text
elide-seam.bc
```

The bitcode should be built with the same hermetic LLVM toolchain used by Elide, currently LLVM 23.

The declaration module participates in the final ThinLTO universe alongside Rust/C/C++ LLVM modules.

The actual Native Image object remains a native object that satisfies those external declarations.

---

# 11. LLVM Integration Model

Use this model:

```text
                     +------------------+
                     | elide-native.o   |
                     | machine code     |
                     +--------+---------+
                              |
                              | definitions
                              |
Rust ThinLTO -----------------+
C/C++ ThinLTO ----------------+
elide-seam.bc ----------------+
                              |
                              v
                        LLVM 23 / LLD
                              |
                              v
                         final binary
```

`elide-seam.bc` describes the ABI and semantic contracts.

It does **not** claim to provide LLVM definitions for the Native Image implementations.

Do not place declaration-only IR into a FatLTO replacement section of `elide-native.o` and expect LLVM to treat it as supplemental metadata. Standard FatLTO semantics treat embedded IR as the LTO representation of the object.

Keep the contract module separate unless LLVM provides a clearly defined supplemental-metadata mechanism that satisfies this use case.

---

# 12. Optional LLVM Wrapper Backend

A useful later extension is generation of importable LLVM wrapper bodies.

Example:

```llvm
declare ptr @__elide_native_alloc(i64)

define noalias ptr @elide_alloc(i64 %size)
    nounwind
    allocsize(0)
{
    %result = call ptr @__elide_native_alloc(i64 %size)
    ret ptr %result
}
```

This permits ThinLTO to inline the wrapper while leaving the actual Native Image body opaque.

Potential uses:

- ABI normalization
- status-code handling
- constant argument injection
- null checks
- symbol renaming
- calling-convention adaptation
- cheap checks that disappear after inlining

Do not make this a requirement for v1.

---

# 13. Error and Exception ABI

Exception behavior must be explicit.

A Java method that may throw cannot simply be emitted as:

```llvm
nounwind
```

unless the generated seam prevents exceptions from crossing the native boundary.

Recommended error models:

```text
STATUS_RETURN
NULL_SENTINEL
INTEGER_SENTINEL
OUT_PARAMETER
TAGGED_RESULT
ABORT
NO_FAILURE
```

Example:

```text
Java:
    Buffer read(...)

ABI:
    int32_t elide_buffer_read(..., Result *out)
```

The seam model should describe both the logical operation and the concrete ABI lowering.

---

# 14. ABI Validation

The project needs a validator independent of code generation.

Validation should reject:

- unsupported Java types crossing the seam
- target-dependent layout without a target specification
- incomplete struct layout
- duplicate linker symbols
- incompatible import/export signatures for the same symbol
- `nonnull` on nullable ABI representations
- contradictory memory contracts
- `readonly` + `writeonly`
- `noreturn` + ordinary return semantics
- `noalias` without approved provenance
- impossible ownership combinations
- callback signatures that cannot be represented by the selected ABI

Fail the build rather than emitting ambiguous ABI code.

---

# 15. Determinism and ABI Fingerprinting

Generated seam artifacts must be deterministic.

Given identical:

- source declarations
- target
- ABI configuration
- generator version

the output must be byte-for-byte stable where practical.

Generate an ABI fingerprint from the normalized Seam IR.

Example:

```text
ELIDE_SEAM_ABI = sha256(normalized-ir)
```

Expose it in generated artifacts where useful.

This allows:

- build cache validation
- runtime compatibility checks
- generated-artifact drift detection
- binary compatibility testing

---

# 16. Serialization

Provide a stable serialized representation of Seam IR for debugging and tooling.

Recommended:

```text
JSON
```

or another deterministic text representation.

Example artifact:

```text
elide-seam.json
```

This is not necessarily the primary compiler interface.

It exists so developers and build agents can inspect exactly what the generator believes the ABI is.

The serialized form should include semantic-fact provenance.

---

# 17. Build Artifacts

A representative build should be able to emit:

```text
build/seam/
    elide-seam.json
    elide-seam.h
    elide-seam.rs
    ElideSeamFFM.java
    elide-seam.ll
    elide-seam.bc
```

Native Image-specific generated sources may live in an internal generated-source directory.

Not every target has to emit every backend.

---

# 18. Implementation Phases

## Phase 1 — Seam IR and validation

Implement:

- core type model
- function model
- parameter model
- ownership/access/nullability
- semantic fact + provenance model
- deterministic normalization
- validation
- JSON/debug serialization

Acceptance criteria:

- representative seam definitions can be expressed without backend-specific types
- invalid contracts are rejected
- IR serialization is stable across builds

---

## Phase 2 — Existing ABI generation

Implement:

- C header backend
- Rust raw FFI backend
- Java FFM backend
- Native Image glue for the supported subset

Acceptance criteria:

- at least one existing manually maintained seam can be replaced end-to-end
- generated declarations agree across C, Rust, Java, and Native Image
- ABI tests pass on supported targets

This phase should produce immediate engineering value even if LLVM work stops here.

---

## Phase 3 — LLVM declaration backend

Implement:

- `.ll` emission
- `.bc` assembly using hermetic LLVM 23
- conservative function attributes
- conservative parameter attributes
- build integration with the final ThinLTO link

Acceptance criteria:

- final link succeeds with `elide-native.o` satisfying declarations from `elide-seam.bc`
- ThinLTO sees the generated declarations
- optimization remarks confirm attributes are visible at callers
- no runtime behavior changes in ABI conformance tests

---

## Phase 4 — Semantic enrichment

Add sources for stronger facts:

- explicit Elide annotations
- generated ownership metadata
- Native Image analysis
- Graal escape/call analysis where stable and worthwhile

Acceptance criteria:

- each emitted optimizer contract has inspectable provenance
- compiler-proven facts are reproducible
- unsafe facts can be disabled globally

---

## Phase 5 — LLVM wrappers

Optionally generate importable wrapper definitions around opaque native symbols.

Acceptance criteria:

- wrappers are visible to ThinLTO
- wrappers inline into Rust/C++ callers where expected
- final calls terminate at the opaque Native Image symbol
- ABI identity and debugability remain clear

---

# 19. Testing Strategy

## 19.1 Golden generation tests

For every backend, maintain golden outputs.

Input:

```text
sample Seam IR
```

Expected outputs:

```text
sample.h
sample.rs
SampleFFM.java
sample.ll
```

Diff generated outputs in CI.

---

## 19.2 ABI conformance tests

Build small cross-language fixtures.

Examples:

```text
Java -> C -> Java
Rust -> Native Image
Native Image -> Rust callback
Java FFM -> Rust
```

Exercise:

- integer widths
- signedness
- pointers
- nullability
- structs
- enums
- callbacks
- error handling
- ownership transfer

---

## 19.3 LLVM semantic tests

For each emitted optimizer attribute, create a tiny IR-level test proving LLVM consumes it as intended.

Examples:

- `readonly` permits load motion
- `nocapture` affects escape reasoning
- `nonnull` removes null branches where legal
- `nounwind` eliminates unwind edges
- allocator contracts permit alias reasoning

Use LLVM optimization remarks and/or FileCheck-style output tests.

Do not rely only on successful compilation.

---

## 19.4 Negative semantic tests

For dangerous attributes, test that the generator refuses unsupported inference.

Examples:

```text
unknown aliasing
    -> must not emit noalias

unknown capture
    -> must not emit nocapture

unknown memory effects
    -> must not emit memory(none)
```

---

# 20. Suggested Package Structure

Illustrative only:

```text
elide-seam/
    model/
        SeamModule
        SeamFunction
        SeamParameter
        SeamType
        SemanticFact
        Provenance

    frontend/
        AnnotationScanner
        NativeImageScanner
        ExplicitConfigLoader

    validate/
        SeamValidator
        AbiLayoutValidator
        SemanticContractValidator

    backend/
        c/
        rust/
        ffm/
        nativeimage/
        llvm/

    serialize/
        SeamJson

    test/
        fixtures/
        golden/
        abi/
        llvm/
```

Keep frontend, model, validation, and backend concerns separate.

---

# 21. Design Rules

The implementing agent should follow these rules:

1. **The Seam IR is language-neutral.**
   Do not leak `MemorySegment`, Rust references, LLVM `Type`, or Graal compiler classes into the core model.

2. **Backends are projections, not authorities.**
   They consume the IR; they do not reinterpret source independently.

3. **ABI shape and semantic optimizer contracts are separate concepts.**
   A declaration can be ABI-correct while carrying no optimizer facts.

4. **Optimizer contracts require provenance.**
   Never silently infer an aggressive LLVM attribute.

5. **Prefer under-optimization to miscompilation.**
   Missing an attribute costs performance. An unsound attribute can corrupt program behavior.

6. **Generated artifacts must be inspectable.**
   Emit readable debug IR and metadata even when the primary output is binary.

7. **The generator must support incremental enrichment.**
   Explicit annotations first, compiler proofs later.

8. **Do not make LLVM support a prerequisite for basic seam generation.**
   C/FFM/Rust/Native Image generation must remain independently useful.

---

# 22. Recommended MVP

The MVP should automate one meaningful existing Elide seam end-to-end.

Pick a seam with:

- several scalar arguments
- one or more pointers
- at least one non-null contract
- a simple ownership convention
- no exotic callbacks
- no complicated exception propagation

Generate:

```text
C header
Rust extern declaration
Java FFM descriptor
Native Image glue
Seam JSON
LLVM declaration module
```

For LLVM, restrict the MVP to attributes such as:

```text
nonnull
nounwind
willreturn
readonly / writeonly only where explicit
nocapture only where explicit
```

Do not implement automatic `noalias` inference in the MVP.

---

# 23. Expected End State

The desired long-term developer experience is approximately:

```java
@Seam.Export
@Seam.NoThrow
static long bufferLength(
    @Seam.NonNull
    @Seam.Borrowed
    @Seam.ReadOnly
    NativeBuffer buffer
) {
    ...
}
```

from which the build derives, consistently:

```c
int64_t elide_buffer_length(const ElideBuffer *);
```

```rust
unsafe extern "C" {
    pub fn elide_buffer_length(buffer: *const ElideBuffer) -> i64;
}
```

```java
FunctionDescriptor.of(
    JAVA_LONG,
    ADDRESS
)
```

and:

```llvm
declare i64 @elide_buffer_length(
    ptr nonnull nocapture readonly
) nounwind willreturn
```

subject to the rule that attributes such as `nocapture` are emitted only when their provenance is approved.

---

# 24. Strategic Rationale

This project should be treated as **ABI infrastructure**, not merely binding generation.

The common representation gives Elide one place to encode:

- what crosses the runtime boundary
- how it is represented
- who owns it
- what memory it may touch
- whether it may fail
- what the optimizer is allowed to assume

That creates three benefits from the same metadata:

```text
interoperability
    -> FFM / C / Rust / Native Image bindings

correctness
    -> ABI validation / ownership / error conventions

optimization
    -> LLVM declarations / ThinLTO contracts
```

The architecture should therefore be designed once around the Seam IR, while individual backend capabilities can land incrementally.

---

# 25. First Implementation Task List

The implementing agent should begin with:

1. Inventory existing Elide FFM, `@CEntryPoint`, C, and Rust seams.
2. Select one representative seam as the MVP fixture.
3. Define the core language-neutral type model.
4. Define `SeamFunction`, `SeamParameter`, and module-level IR.
5. Define `SemanticFact<T>` and provenance.
6. Implement validation.
7. Implement deterministic JSON serialization.
8. Implement C header generation.
9. Implement Rust raw FFI generation.
10. Implement Java FFM generation.
11. Implement the minimal Native Image glue backend needed by the fixture.
12. Implement LLVM textual declaration generation.
13. Assemble generated `.ll` to `.bc` with the hermetic LLVM 23 toolchain.
14. Add `elide-seam.bc` to a ThinLTO test link beside a native implementation object.
15. Verify emitted attributes with LLVM optimization remarks.
16. Add golden tests and ABI conformance tests.
17. Migrate one real Elide seam away from handwritten declarations.
18. Only then expand annotation inference and Native Image/Graal analysis integration.

The first milestone is complete when one production-quality seam has **one source of ABI truth** and every language/toolchain-facing representation is generated from it.
