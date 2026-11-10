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

One per-parameter fact refines an import's byte pointers:

- `truffle_buffer=true@explicit_contract` on a `ptr<u8>`, `ptr<i8>` or `ptr<char>` parameter with a
  declared access says it is a byte buffer, not a C string. A `borrow` says that too and derives
  the length as well, beside any other pointer parameters, so prefer it for a buffer with a length
  parameter of its own, whatever its access and whether or not it may be null. This fact is for
  the buffers a `borrow` cannot describe (an extent that is not one length parameter) and for
  counts the guest should keep passing. Without either, such an input is treated as a C string: a heap
  buffer is copied whole with a terminating NUL on every call and never pinned. With the
  fact, the cell lends the bytes as they are: native memory in place, a heap array pinned when it
  reaches the pin threshold and copied without a terminator otherwise, and a string as its UTF-8
  bytes. The length stays an ordinary guest argument, so the guest, not the cell, vouches that it
  does not exceed the buffer. The fact is fingerprinted and is not an LLVM attribute.

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
    public static final class add_Cell extends NativeCell {
        public static int invoke(int left, int right);    // native carriers, raw addresses
        ...
    }
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

A cell with a scalar result also overrides the matching typed method, `executeLong`, `executeInt`,
`executeDouble` or `executeBoolean` (the guest carrier: `long` for 64-bit, `usize` and `u32` results,
`int` for narrower integers and `char`, `double` for floats), and its `execute` boxes that value.
Language glue holding the concrete `*_Cell` type calls the typed method with no box and no exception
handler; through `NativeCell`, the typed methods default to unboxing `execute` and throw
`UnexpectedResultException` for any other result.

Every cell except a resource's `destroy` also has a static `invoke` with the binding's own
parameters: scalars in their Java carriers (unsigned ones as raw bits), every pointer, byte buffers
included, as a raw `long` address, and any borrowed length passed explicitly. It returns the guest
carrier of a scalar result and the raw address of a pointer or created resource. `invoke` coerces
nothing, borrows nothing and allocates nothing, so the caller keeps each address valid for the
call. It is the entry for host code that already holds native values: host code is compiled ahead
of time, where the argument array, boxed carriers, `NativePointer` and `Borrow` objects of
`execute` are not scalar-replaced. The cell's own `execute` methods coerce and then call it.

Partial evaluation inlines only methods of build-time-initialized classes, so every class that
takes part in a compiled guest call must initialize at image build time: `myna-truffle.jar` carries
a `native-image.properties` for its own classes (keeping the empty-buffer dummy and the pin
threshold at run time, so no address or build-host setting reaches the image heap), and the
generated `native-image.properties` next to the bindings lists the cell classes. The bindings a cell
calls keep their run-time address classes. A consumer's own node classes around cells need the same
`--initialize-at-build-time` treatment, as for any Truffle language.

The rule is per method, and it is silent when broken. A method that needs a class-initialization
check (a static call into, or a static field read of, a class that initializes at run time) is left
out of runtime compilation altogether: the image builds, the cell runs, and compiled code calls that
method as an opaque function. Whatever it returns then cannot be scalar-replaced, and if the method
is a cell body, the argument array and the boxed carriers are allocated for the call. `myna-truffle`
therefore reaches its run-time holders (the empty-buffer address, the pin threshold, the scratch
stacks) only from methods behind a `@TruffleBoundary`, and FFM cells call their bindings the same
way. `tests/truffle_integration.py` builds its image with `-H:+PrintRuntimeCompileMethods` and
fails if a cell or a coercion is missing from that listing; consumer glue can be checked the same
way.

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
| `ptr<T>` (non-byte pointee), `ptr<void>`, `fn<…>` return | `NativePointer`, open `NativeResource`, `NativeBytes`, any interop pointer; `null` only when nullable. Where `T` is a declared resource, a `NativeResource` must be of that resource | `NativePointer` |
| `ptr<u8>`, `ptr<i8>`, `ptr<char>` with `access=read` | strings (UTF-8, NUL-terminated), buffers, pointers | — |
| `ptr<u8>`, `ptr<i8>`, `ptr<char>` with `access=write` or `read_write` | buffers and pointers; strings are rejected | — |
| byte pointers declared `truffle_buffer` | strings (UTF-8, read access only), buffers, pointers; nothing is NUL-terminated and the count is a guest argument | — |
| byte pointers with unspecified access | pointers only, like any other pointer | — |
| `borrow` buffers (the count is derived) | strings for read access, `NativeBytes`, `HeapBytes`, any interop buffer; a bare pointer has no size and is rejected; `null` only when nullable, as an absent buffer of length zero | — |
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
guest-visible signature: the guest passes the buffer and the cell derives the byte count, checking
32-bit length ranges before the call. A function may declare several, each with a length of its
own, beside other pointer parameters:

```text
borrow transfer input length=count lifetime=call
borrow transfer output length=capacity lifetime=call
import transfer symbol=cells_transfer return=u64 error=no_failure nounwind=true@explicit_contract
  param input type=ptr<u8> nullable=false ownership=borrowed access=read nocapture=true@explicit_contract readonly=true@explicit_contract
  param count type=u64
  param output type=ptr<u8> nullable=false ownership=borrowed access=write nocapture=true@explicit_contract
  param capacity type=u32
  param copied type=ptr<i64> nullable=true ownership=borrowed access=write
end
```

The guest calls `transfer(input, output, copied)`: both lengths are derived, and the cell's arity
is three. A `write` buffer is a writable borrow like a `read_write` one: read-only buffers and
strings are rejected, and a copy starts from the guest's bytes and is written back on release. The other pointer keeps the coercion it would have without any borrow (an address here; a
C string or `truffle_buffer` bytes for a byte pointer with a declared access) and stays a guest
argument under its own contract. `invoke` keeps all five binding parameters. The cell does not
check that two buffers of a call are disjoint: lent native memory aliases and copies do not, and a
read buffer that overlaps a writable one breaks the guest's side of the contract either way.

A buffer declared `nullable=true` may be absent, as an optional output buffer is:

```text
borrow decode data length=length lifetime=call
borrow decode text length=capacity lifetime=call
import decode symbol=cells_decode return=u64 error=no_failure nounwind=true@explicit_contract
  param data type=ptr<u8> nullable=false ownership=borrowed access=read nocapture=true@explicit_contract readonly=true@explicit_contract
  param length type=u64
  param text type=ptr<u8> nullable=true ownership=borrowed access=write nocapture=true@explicit_contract
  param capacity type=u32
end
```

The guest calls `decode(data, text)` or `decode(data, null)`. For interop `null`, an interop
pointer to address zero (`NativePointer.NULL`) or host `null`, the cell lends the null address and
derives length zero, so the native function receives `NULL` and `0`; there is nothing to release.
Any other value is a buffer that is present and is lent as every borrow is, with a non-null address
even when it is empty, so the function can tell the two apart. Only null is absent: a pointer to
any other address still has no size to derive and is rejected, and a buffer declared
`nullable=false` rejects null as before. Pointers default to `nullable=true`, so a buffer declared
without `nullable` takes null. See [nullable buffers](ownership.md#nullable-buffers) for the same
declaration in the other wrappers.

Scalars and addresses coerce first. Every borrow of the call (declared buffers, C strings and
`truffle_buffer` bytes alike) is then acquired in parameter order, and a later one that fails
releases the earlier ones. Each borrow lasts exactly the dynamic extent of the call, as the contract
states, and is released in a `finally` of its own, in reverse acquisition order:

- native memory (`NativeBytes`, interop pointers) is lent in place;
- heap arrays are pinned for the call in a Native Image when they reach `Borrow.pinThreshold()`
  bytes, and copied into temporary native memory otherwise; HotSpot always copies;
- copies of writable borrows are written back on release;
- copies (heap arrays, strings, other interop buffers) of up to 16 KiB go on a per-thread scratch
  stack: one native block per thread, bumped on acquisition and popped on release, so a small copy
  costs no allocation. Larger copies, and copies that do not fit beside outstanding ones, get an
  arena of their own. Because cells release in reverse order, nested borrows and calls back into
  guest code are last-in, first-out; releasing out of order or on another thread throws
  `IllegalStateException` and leaves the stack unchanged.

A borrow that a compiled cell acquires and releases costs no Java heap: the `Borrow` is
scalar-replaced whichever kinds of value the call site has seen, lent native memory and scratch
copies allocate nothing, and a pin allocates only its `PinnedObject` (24 bytes), exactly as a
hand-written pin does. Each byte parameter profiles the kinds of value it has been given (native
buffer, heap buffer, Java string, raw pointer, anything else through interop) and compiles only
those paths. Several borrows on one call cost what each costs alone:
`tests/truffle_integration.py` measures 0 bytes per compiled `transfer` call between native
buffers and between copied heap arrays, and the two pins (48 bytes) between two pinned arrays.
An absent nullable buffer costs nothing either: 0 bytes per compiled `decode` call for interop
`null`, the null pointer and host `null` alike, and a call site that sees absent and present
buffers in turn stays in compiled code.

ASCII text is written straight onto the scratch stack, so a string argument allocates nothing
either; text with other characters is encoded into a UTF-8 array first.

The same holds for a cell's `execute` called from host code in a Native Image, which is compiled
ahead of time without partial evaluation: `tests/truffle_integration.py` measures 0 bytes per call
there for native memory, raw pointers, scratch copies, ASCII strings and absent nullable buffers,
and the pin alone for a pinned array. On HotSpot the host JIT decides: scalar replacement depends on its inlining and on
type profiles shared by every call site in the process. With the Graal JIT the borrow is
scalar-replaced as long as no call site lends through the generic interop path; C2 allocates it
(56 bytes per byte parameter) once the coercion has been compiled on its own. Host code that must
not allocate on any runtime calls `invoke`.

The scratch stack's lifetimes have three caveats:

- every thread that copies, virtual threads included, gets its own block of 4 KiB, growing to at
  most 64 KiB. The block comes from an automatic arena, so it is freed only after the thread and
  its `ThreadLocal` entry become unreachable, when the arena's `Cleaner` runs. Many short-lived
  virtual threads that each borrow can therefore hold up to 64 KiB of native memory apiece until
  the collector reclaims them;
- a borrow that is never released keeps its bytes reserved on that thread's stack for the
  thread's lifetime. Later copies stack above it, and once the block is full they fall back to
  arenas of their own;
- generated cells release in a `finally`, so if `release()` ever throws its misuse
  `IllegalStateException` while an exception is already propagating, the new exception replaces
  the original one. Generated code always releases in order on the acquiring thread, so this means
  an invariant was violated (for example, a consumer released a cell's borrow by hand). It is not
  expected behaviour.

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

A create declared with a `handle` keeps its C shape for the guest: the guest passes a buffer for
the struct the create writes through, and reads that struct's fields from it afterwards. The
native side never sees that buffer. The cell allocates the struct in memory the runtime owns
(zeroed, of the declared size), passes that, and copies it to the guest's buffer after the call,
on failure too, since the struct may say why. It compares the result with the statuses the
resource lists as `success`, reads the resource pointer from the handle field of its own struct
(or takes that struct's address, for a result initialized in place) and returns the owner.

Keeping the struct out of the guest's reach is what keeps ownership sound. The handle cannot be
replaced between the create writing it and the cell adopting it. A buffer too small for the
struct, a read-only one, a bare pointer, or another resource's owner is refused before the create
is called. A resource initialized in place lives in the runtime's struct until its destroy, so
guest code cannot initialize one struct twice, overwrite a live resource, or free the memory
under it; passing the same buffer again simply makes a second resource. What the guest's buffer
holds is a copy as the create left it.

Any result not listed raises `NativeStatusException`, a `NativeCellException` that carries the
result: `status()` for host code, a readable `status` member for guest code, on an ordinary
runtime error. Success with a null handle is a plain `NativeCellException`. The cell's static
`invoke` is unchanged by any of this: it returns the status and takes the struct's address,
exactly as the C function does, and the caller owns that memory.

A create may take pointers as well as scalars, coerced like any other cell's: a pointer to plain
memory, a byte buffer, or another resource's owner passed where its handle is expected. The native
side may keep any pointer it is given, so a resource created from another one holds it open:
closing the parent while a child is open raises `NativeCellException` and leaves it open, and it
closes normally once its children have. A shared parent that becomes unreachable while a child is
still open is freed when the last child is. A pointer parameter declared
`nocapture=true@explicit_contract` is not retained by the callee, so a resource passed for it is
not held. Plain memory passed to a create (a `NativePointer`, a `NativeBytes`) has no owner the
cell can hold; keeping it valid for as long as the created resource uses it stays with whoever
handed it to the guest.

Generated array convenience methods pin with a plain `try`/`finally`, not try-with-resources:
javac compiles try-with-resources to `Throwable.addSuppressed`, which Truffle's runtime-compilation
blocklist (`-H:+TruffleCheckBlockListMethods`) rejects on partially evaluated paths. Hand-written
code that runs around a cell on a compiled path should do the same.

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

A cell cannot make a transitioning call cheaper than the transition. Measured from compiled code,
the direct stub call, a generated `boundary` cell and a hand-written `@TruffleBoundary` around the
same `@CFunction` cost the same, with one array pinned or with ten arguments, because the stub call
is itself a call into ahead-of-time code (see [Measurement](#measurement)). What a cell adds over a
hand-written boundary is the contract (coercions, borrow lifetimes, ownership), at the cost of the
type checks on its guest values. The lever for a short call is the import's leaf status: a
`native_leaf` import (the binding ABI 2 default) has no transition and inlines, where
`native_leaf=false` pays the transition on every route.

FFM-backend cells call the module's `MynaFFM` static bindings (`_CriticalDowncall.HANDLE` for a
native leaf, `_Downcall.HANDLE` otherwise, over the `loaderLookup` address) from a static method behind a `@TruffleBoundary`, since those bindings
initialize at run time and invoke a method handle, and report linkage failures as guest errors,
while guest exceptions and fatal VM errors pass through. They need the generated foreign Feature on a Native Image, exactly
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
(five processes per case, a shared WSL2 workstation, so advisory) was measured at source revision
`83229a2`, before the per-thread scratch stack, the typed `execute*` methods and the
runtime-compilation fix described [below](#after-the-runtime-compilation-fix). Its copy and string
figures therefore show the earlier per-call confined arena, its borrow and FFM figures include
allocations that no longer happen, and it has none of the `typed-*`, slab or hand-pinned cases. It
has not been regenerated since; the figures below are that baseline's, in ns per call:

| Path | Direct Java call | Cell via glue | Cell via interop wrapper |
|---|---:|---:|---:|
| Native Image leaf | 1.52 | 1.51 | 1.52 |
| Native Image transition (direct stub, elide.26) | 4.87 | 4.87 | 4.87 |
| AOT FFM leaf | 5.49 | 7.54 | — |
| Truffle NFI (libffi), same leaf symbol | — | — | 21.0 |

Leaf and transitioning cells cost exactly their direct calls and allocate nothing, so the
coercions, the `Object[]` argument and the boxed result are all scalar-replaced; the executable
wrapper's `DirectCallNode` hop is inlined too, so the generic interop path needs no language glue
for scalar calls. FFM cells paid about 2 ns and 48 B per call over the direct FFM call in this
baseline. A Java string lent to a C string
parameter costs about 27 ns and 208 B (UTF-8 copy into a confined arena), against NFI's 37 ns for
the same conversion.

Borrows, in this baseline: lending `NativeBytes` cost about 2 ns and one 48-byte `Borrow` over the
raw pointer.
Pinning a heap array is a fixed cost of about 15 ns more, independent of size, while copying adds
about 0.04 ns per byte on top of a similar fixed cost: at 16 bytes pin and copy are within 2.4 ns,
at 256 bytes copying is 12 ns slower, at 4 KiB 180 ns slower. The default pin threshold is
therefore 256 bytes: small arrays are copied so short-lived garbage never pins collector chunks,
and the cost of that choice is bounded by the figures above. The threshold remains overridable and
should be revisited with the hosted ARM data and under GC pressure, which this harness does not
measure. For what each binding kind costs from guest code without cells, see
[calling Myna bindings from Truffle guest code](truffle-guests.md).

### After the runtime-compilation fix

The baseline above predates a fix to what compiled code could inline: `BytesCoercion.execute` and
every FFM cell body needed a class-initialization check, so they were left out of runtime
compilation and called as opaque functions. That accounted for the 48 bytes per borrow, the FFM
cells' allocation and most of their time. Measured before and after on a shared Apple M5 Max
(`aarch64-apple-darwin`, same build settings, three processes per case, guest values held as
non-constants; advisory, and not a checked-in baseline), in ns per call and Java heap bytes per
call:

| Case | Before | After | Direct call |
|---|---:|---:|---:|
| Ten-argument leaf, native buffers (`glue-slab-native`) | 8.41, 96 B | 2.96, 0 B | 3.59 |
| Same through the interop wrapper | 8.51, 96 B | 2.98, 0 B | 3.59 |
| Ten-argument transition (`glue-slab-step-native`) | 12.05, 96 B | 5.56, 0 B | 4.26 |
| Same, heap buffers copied (`glue-slab-heap`, leaf) | 28.66, 96 B | 21.95, 0 B | — |
| Sum of 256 native bytes (`glue-sum-native-256`) | 12.36, 48 B | 12.12, 0 B | 12.2 |
| Sum of 16 copied heap bytes | 14.29, 48 B | 10.48, 0 B | 4.6 |
| Sum of 256 pinned heap bytes, leaf | 20.76, 72 B | 16.84, 24 B | 16.65 by hand |
| Sum of 256 pinned heap bytes, transition | 24.27, 72 B | 20.38, 24 B | 19.29 by hand |
| Native and pinned alternating at one call site | 16.96, 60 B | 14.30, 12 B | — |
| Java string to a C string (`glue-text`) | 20.64, 72 B | 17.63, 24 B | 1.75 |
| FFM leaf cell (`glue-ffm-leaf`) | 9.29, 32 B | 4.63, 0 B | 4.21 |
| FFM transitioning cell | 9.23, 32 B | 4.55, 0 B | 4.26 |

"By hand" is a hand-written `PinnedObject` pin around the same binding; behind a
`@TruffleBoundary` and visible to partial evaluation it costs the same (16.65 and 16.99 ns for the
leaf, 19.29 and 19.51 ns across the transition). A bare transition costs the same on every route:
3.78 ns direct, 3.70 ns through the cell, 3.75 ns behind a hand-written boundary and 3.79 ns
through a `boundary` cell, against 1.03 ns for the leaf. The static `invoke` entry costs the direct
call (3.66 ns for the ten-argument leaf, 4.47 ns for its transition) and allocates nothing from
host code. What remains over the direct call in the ten-argument transition is the type checks on
ten guest values.

### Follow-ups

Findings that are tracked, not blocking:

- An ASCII Java string lent to a C string parameter is written straight onto the scratch stack:
  `glue-text` went from 17.6 ns and 24 B to 15.4 ns and 0 B (same host and method as the table
  above). Text with other characters still allocates its UTF-8 array, and a guest `TruffleString`
  still becomes a Java `String` through interop first; taking its bytes through the TruffleString
  API is not implemented.
- A byte-pointer parameter without a `borrow` declaration is a C string parameter unless it is
  declared `truffle_buffer`. Pinning is not a win at small sizes: the ten-argument entry with a
  256-byte and a 16-byte heap buffer costs 21.9 ns and 0 B as two terminated copies and 24.4 ns
  and 24 B with the larger one pinned. The fact is for buffers large enough that copying them on
  every call is the cost, and for callees that must not see a terminator.
- A `borrow` now sits beside other pointer parameters, a function may declare several, and a
  buffer may be write-only or nullable (see [Borrows](#borrows)); a resource create may take
  pointers. Still rejected: a pointer result beside a borrow, one length shared by two buffers,
  and length-less buffers, for which `truffle_buffer` is the cells-side answer.
- Not expressible yet: length-less buffers, and borrows that outlive the call.
- Heap arrays below the pin threshold are copied where a hand-written call would pin in place; at
  16 bytes the copy is faster than the pin (10.5 against 17.3 ns), at 256 bytes they are within
  2 ns. `-Dmyna.truffle.pinThreshold` moves the threshold.
- FFM cells pass `MemorySegment.ofAddress(address)` to the downcall, so the liveness and
  confinement checks of the caller's own segment do not apply during the call.
- FFM cells over leaf imports are critical: they call the module's static FFM entry, which for a
  native leaf is a critical downcall over the build-time typed adapter. See
  [Measurement](#measurement) for their cost against the direct and handwritten calls.
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
