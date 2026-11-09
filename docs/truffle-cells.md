# Truffle native cells

A **cell** is a Truffle node that calls one Myna import from guest code. Myna generates a cell per
import when a module opts in, and ships the language-neutral pieces (coercions, pointer and buffer
objects, resource owners, an interop-executable wrapper) in a small `myna-truffle` JAR. Every Truffle
guest language in the Elide ecosystem gets the same fast, safe path to native code without
hand-written native-call nodes.

The design principle is Myna's: **one contract, many backends.** A seam already knows each function's
signature, leaf status, ownership and borrow rules. A cell is one more projection of that contract,
and it never adds native-call machinery of its own: it calls the module's existing generated binding,
and the compiler decides how that call compiles.

## Opting in

```text
module cells target=x86_64-unknown-linux-gnu java_package=my.cells java_class=CellApi truffle_cells=true
import add return=i32 error=no_failure
  param left type=i32
  param right type=i32
end
```

`truffle_cells=true` adds `<NativeClass>Truffle.java` (`MynaTruffle.java` with default naming) to the
generated outputs. Three further module options require it:

| Option | Values | Meaning |
| --- | --- | --- |
| `truffle_backend` | `native` (default), `ffm` | Cells call the Native Image bindings, or the FFM bindings for consumers that also run on HotSpot. |
| `truffle_coercion` | `strict` (default), `wrap` | Integer arguments outside a carrier's range raise an error, or truncate to the carrier width in two's complement (JavaScript `ToInt32` style; non-finite doubles become zero). |
| `truffle_transitions` | `direct` (default), `boundary` | How a cell over a transitioning import reaches its AOT stub; see [Transitions](#transitions). |

Two per-import facts refine the module choice:

- `truffle_cell=false@explicit_contract` opts one import out. Imports with by-value records or
  callback parameters must opt out: those shapes need language glue.
- `truffle_raw_pointers=true@explicit_contract` lets guest numbers stand in for pointer arguments
  of that import (`bun:ffi` compatibility). By default a pointer argument must be an interop pointer
  or, where the parameter is nullable, interop `null`.

Exports are never cells. The option, backend, coercion policy and transition mode are serialized in
`seam.json` under `"truffle"` and change the fingerprint; a descriptor without the option keeps its
existing fingerprint.

## The generated class

```java
public final class CellApiTruffle {
    public static final String ABI_FINGERPRINT = "...";
    public static final String[] CELLS = {"add", ...};
    public static NativeFunction function(String name);   // a fresh executable for any cell
    public static NativeFunction add();                   // one factory per cell
    public static final class add_Cell extends NativeCell { ... }
}
```

Each `*_Cell` is a `NativeCell` (a Truffle `Node`) whose `execute(Object[])` checks arity, coerces
every argument through a child coercion node, calls `CellApi.add(...)` and converts the result. A
cell holds call-site profiles and is adopted once: create one per function object. `function()` wraps
a cell in `NativeFunction`, an interop-executable object with its own call target, so any Truffle
language calls it through `InteropLibrary.execute` with no language-specific code. That costs one
interop dispatch, which partial evaluation inlines for a monomorphic call site. A language that wants
the fastest path exposes the cell as one of its own function objects (for GraalJS, `JSFunctionData`
around `NativeFunction.callTarget()`); that glue lives in the consumer.

Partial evaluation inlines only methods of build-time-initialized classes, so every class that
takes part in a compiled guest call must initialize at image build time: `myna-truffle.jar` carries
a `native-image.properties` for its own classes (keeping the empty-buffer dummy and the pin
threshold at run time, so no address or build-host setting reaches the image heap), and the
generated `native-image.properties` next to the bindings lists the cell classes. The bindings a cell
calls keep their run-time address classes. A consumer's own node classes around cells need the same
`--initialize-at-build-time` treatment, as for any Truffle language.

Generated cells use no Truffle DSL. Consumers compile them against `myna-truffle.jar` and the public
Truffle API only; the DSL processor is needed to build `myna-truffle` itself, not to use it.

## Coercions

Coercions use `InteropLibrary` only, so they are correct for every Truffle language. The interop
library is inserted lazily, so a call site that only passes Java boxed primitives never dispatches
through it, and partial evaluation reduces a monomorphic site to a type check or two.

| Carrier | Accepts | Result to guest |
| --- | --- | --- |
| `i8`…`i64`, `u8`…`u64`, `usize`, `isize`, `char` | interop numbers that fit (integral doubles included); `u64` also takes big integers up to 2^64 | signed as is; `u8`/`u16`/`u32` as their numeric value; `u64` as raw bits (interop has no unsigned 64-bit primitive); `char` follows the target ABI — signed on x86-64 and Apple arm64 (sign-extended like `i8`), unsigned on AAPCS64 Linux (`aarch64-unknown-linux-gnu`, masked as `u8`) |
| `bool` | interop booleans only; numbers are not truthy | boolean |
| `f32`, `f64` | any interop number; rounding into `f32` is the call's C semantics | double |
| `ptr<T>` (non-byte pointee), `ptr<void>`, `fn<…>` return | `NativePointer`, open `NativeResource`, `NativeBytes`, any interop pointer; `null` only when nullable | `NativePointer` |
| `ptr<u8>`, `ptr<i8>`, `ptr<char>` with `access=read` | strings (UTF-8, NUL-terminated), buffers, pointers | — |
| `ptr<u8>`, `ptr<i8>`, `ptr<char>` with `access=write` or `read_write` | buffers and pointers; strings are rejected | — |
| byte pointers with unspecified access | pointers only, like any other pointer | — |
| `borrow` buffers (the count is derived) | strings for read access, `NativeBytes`, `HeapBytes`, any interop buffer; a bare pointer has no size and is rejected | — |
| `void` | — | `NativeVoid.INSTANCE` (interop `null`) |

Failures raise `NativeCellException`, an `AbstractTruffleException` that guest languages see as an
ordinary runtime error, with the function, parameter and expected carrier in the message. A failing
coercion always leaves compiled code first.

A language that wants its own semantics (JavaScript `ToInt32`, Python `__index__`) plugs in a
coercion node of its own in the consumer; it is not the default.

## Pointers and buffers

`NativePointer` is an interop pointer over a raw address; the null address is also interop `null`.
Guest code never sees raw numbers as pointers unless the import opted in.

Two guest buffer objects ship in `myna-truffle`:

- `NativeBytes` views native memory (an address and size, or a native `MemorySegment`). It is an
  interop buffer and an interop pointer at once, so cells lend its address with no copy and no pin.
  `toArrayBuffer(ptr, len)` in `bun:ffi` terms is a consumer API over it.
- `HeapBytes` wraps a Java `byte[]`, for languages without native memory. It is not an interop
  pointer: a heap address never reaches native code unpinned.

Any other interop buffer (anything answering `hasBufferElements`) is also accepted; it is read into a
copy through the library and, when the parameter is writable, written back element by element.

## Borrows

A `borrow FUNCTION PARAMETER length=PARAMETER lifetime=call` declaration drops the length from the
guest-visible signature: the guest passes one buffer and the cell derives the byte count, checking
32-bit length ranges before the call. The borrow lasts exactly the dynamic extent of the call, as the
contract states, and every borrow is released in a `finally`, in reverse acquisition order:

- native memory (`NativeBytes`, interop pointers) is lent in place;
- heap arrays are pinned for the call in a Native Image when they reach `Borrow.pinThreshold()`
  bytes, and copied into temporary native memory otherwise; HotSpot always copies;
- copies of writable borrows are written back on release.

A pinned array does not move, so its address stays valid across any safepoint between the pin and
the call. The pin/copy threshold defaults to 256 bytes (`-Dmyna.truffle.pinThreshold`); see
[Measurement](#measurement).

## Ownership

A `resource` declaration makes the `create` import's cell return a `NativeResource`: a guest-visible
owner that is an interop pointer while open, exposes a `close` member, closes exactly once, and
refuses access from any thread other than its required thread when the resource is confined. The
`destroy` import's cell takes that owner and closes it; a pointer or a foreign owner is rejected, and
a second destroy is a no-op. A failed creation returns `NativeVoid`. `registerCleanup(Cleaner)`
adds an optional cleaner for shared resources; a cleaner thread cannot honor confinement, so confined
owners refuse it. These are the same rules as the existing Native Image ownership API.

## Transitions

The hard constraints come from the compiler, not from Myna:

- **No C call that changes the thread state can be compiled into runtime-compiled code.** Since
  elide-dev/graal#196 (25.5.5-elide.26) compiled guest code calls a transitioning binding's AOT stub
  instead, at about 5 ns. Older forks fail the image build with a blocklist violation.
- **Leaves stay leaves.** A `native_leaf` import has no transition, so its cell inlines into compiled
  guest code (0.8–0.9 ns measured) on every fork version. A leaf must not block, call back into
  Java, throw or run long; the seam contract enforces that, and cells never change a binding's leaf
  status.

`truffle_transitions=direct` (the default) emits the direct stub call and needs elide.26 (the
pinned fork) to build a Native Image. `truffle_transitions=boundary` puts each transitioning call behind a
`@TruffleBoundary` static method instead: every fork compiles it, at the cost of a boundary call and
no inlining. Leaves are unaffected by the option. Myna's own fixture uses the default; the generator
tests cover `boundary`.

FFM-backend cells call the module's `MynaFFM` static bindings (`_Downcall.HANDLE` over the
`loaderLookup` address) and report linkage failures as guest errors, while guest exceptions and
fatal VM errors pass through. They need the generated foreign Feature on a Native Image, exactly
like any other FFM consumer; see [Measurement](#measurement) for their cost.

## Safety model

- Leaves: bounded, nonblocking, non-reentrant, enforced by the `native_leaf` contract.
- Transitioning calls may block, call back and take any time.
- Borrows end with the call. A borrow used after the call is a consumer bug the runtime cannot
  observe; heap copies are written back and freed on release, pins are released, so nothing dangles
  on the Java side.
- Resources: exactly-once release, thread confinement, no retained referents from cleaners.
- Isolates: every native target is resolved per isolate at run time; the runtime allocates its
  empty-buffer dummy lazily and bakes no address into an image heap.

## Testing

`mise run test` includes the JVM unit tests of `myna-truffle` and `tests/truffle_integration.py`,
which builds `tests/truffle/cells.seam` against a static C fixture and runs the same guest-level
checks (`tests/truffle/CellChecks.java`) twice: FFM cells on HotSpot, and Native Image cells from
runtime-compiled Truffle code (`--macro:truffle-svm`, with a polyglot context that compiles immediately in the foreground, as the
fork's `CFunctionRuntimeCompilationTest` does; the test fails unless each probed root observes
`CompilerDirectives.inCompiledCode()` and stays there). The image keeps the default compile
isolates, which elide-dev/graal#197 made work on the pinned fork. The pinned Truffle jars come from the Elide
GraalVM Maven repository and are verified against `tools/truffle.json`.

## Measurement

[`benchmarks/cells`](../benchmarks/cells/README.md) measures cells from runtime-compiled Truffle
code on a Native Image: `mise run bench:cells-build` then `mise run bench:cells`, or the `Truffle
cell costs` workflow on the hosted ARM machine. Every case is one process running one compiled root
that must observe compiled code; it compares the generated cells (consumer glue and the generic
interop wrapper) with plain Java calls to the same bindings, Truffle NFI as the dynamic-FFI
baseline, and the three borrow strategies by size, retaining raw samples, hashes, commands and
compilation traces as [benchmarking](benchmarking.md) requires.

The checked-in [Linux amd64 baseline](../benchmarks/cells/baselines/wsl2-linux-amd64-2026-11-07.md)
(five processes per case, a shared WSL2 workstation, so advisory) shows, in ns per call:

| Path | Direct Java call | Cell via glue | Cell via interop wrapper |
|---|---:|---:|---:|
| Native Image leaf | 1.52 | 1.51 | 1.52 |
| Native Image transition (direct stub, elide.26) | 4.87 | 4.87 | 4.87 |
| AOT FFM leaf | 5.49 | 7.54 | — |
| Truffle NFI (libffi), same leaf symbol | — | — | 21.0 |

Leaf and transitioning cells cost exactly their direct calls and allocate nothing, so the
coercions, the `Object[]` argument and the boxed result are all scalar-replaced; the executable
wrapper's `DirectCallNode` hop is inlined too, so the generic interop path needs no language glue
for scalar calls. FFM cells pay about 2 ns and 48 B per call over the direct FFM call: the boxing
is not scalar-replaced across the downcall's exception handler. A Java string lent to a C string
parameter costs about 27 ns and 208 B (UTF-8 copy into a confined arena), against NFI's 37 ns for
the same conversion.

Borrows: lending `NativeBytes` costs about 2 ns and one 48-byte `Borrow` over the raw pointer.
Pinning a heap array is a fixed cost of about 15 ns more, independent of size, while copying adds
about 0.04 ns per byte on top of a similar fixed cost: at 16 bytes pin and copy are within 2.4 ns,
at 256 bytes copying is 12 ns slower, at 4 KiB 180 ns slower. The default pin threshold is
therefore 256 bytes: small arrays are copied so short-lived garbage never pins collector chunks,
and the cost of that choice is bounded by the figures above. The threshold remains overridable and
should be revisited with the hosted ARM data and under GC pressure, which this harness does not
measure. For what each binding kind costs from guest code without cells, see
[calling Myna bindings from Truffle guest code](truffle-guests.md).

### Follow-ups

Findings from the first measurement that are tracked, not blocking:

- FFM cells and borrowing cells allocate 48 bytes per call in compiled code: the boxed result,
  the argument array and the `Borrow` are not scalar-replaced across the downcall's exception
  handler and the borrow's `finally`. Native leaf and transition cells allocate nothing.
- A Java string lent to a C string parameter costs about 27 ns per call, mostly the confined arena
  opened for the UTF-8 copy; a pooled scratch buffer would cut it.
- The generator has no FFM critical cell variant; the direct FFM leaf (5.5 ns) already beats the
  measured critical downcall figures on this fork, so none is planned until a consumer asks.
- Truffle NFI is an internal language: host code cannot `eval("nfi", ...)`, so the benchmark
  carries a two-method language that binds symbols through NFI signature objects
  (`signature.invokeMember("bind", symbol)`). Consumers embedding NFI need the same.
- The pinned toolchain's sysroot predates glibc 2.27, while the distribution's static libffi
  (linked by NFI) references `memfd_create`; images that link NFI need the weak syscall shim from
  `benchmarks/cells/native.c` until the toolchain's sysroot moves.

## Scope and consumers

In `myna-truffle` and the generator: cells, coercions, pointers, buffers, borrows, ownership, the
executable wrapper. In each consumer: exposing a cell as a real JS function or Python callable,
`bun:ffi`-style modules, `toArrayBuffer`, and dynamic (`dlopen` + signature) FFI over Truffle NFI.

Host Java `null` arguments follow the same nullable pointer and byte-buffer rules as interop null values. Non-nullable pointers, byte buffers, and scalar coercions reject them with `NativeCellException`.
