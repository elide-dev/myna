# Ownership and borrowing

Ownership is a build-time contract projected into different runtime APIs. Native Image consumers choose their own resource management and GC integration. Generated Java does not prescribe `close()`, `Cleaner`, a reference-counting implementation, or a global resource registry.

## Native resources

```text
opaque Ticket
resource Ticket representation=native threading=shared create=ticketCreate destroy=ticketDestroy

import ticketCreate symbol=ticket_create return=ptr<Ticket> error=no_failure nounwind=true@explicit_contract
end
import ticketDestroy symbol=ticket_destroy return=void error=no_failure nounwind=true@explicit_contract
  param ticket type=ptr<Ticket> nullable=false ownership=transferred_to_callee access=read_write
end
```

This declaration is an explicit implementation contract: the constructor returns either null or sole ownership of a fresh, live resource; the destructor consumes that ownership exactly once. Constructors take scalar and pointer arguments. All scalar values must be handled without undefined behavior; invalid inputs can return null. Pointer arguments pass through every generated create wrapper unchanged, under their own declared contracts: the generator vouches for none of them, so the Rust `create` is an `unsafe fn` whenever it takes one. A resource created from another resource's handle (a stream from its session) must not outlive that parent. In the Native Image, FFM and Rust wrappers the parent's lifetime is the consumer's to enforce; Truffle cells enforce it for guest code, where the created owner holds its parent open unless the parameter is declared `nocapture`. A create may take [call-scoped buffers](#constructors-with-call-scoped-buffers). A successful allocation satisfies the destructor's requirements. The destructor's parameter may be `nullable=true` when it accepts null as a no-op, as `free` does: no generated owner passes it null, so the wrappers are the same, and `seam.ll` carries no `nonnull` fact for it. Both functions must be native imports, returning normally without unwinding across the ABI. Resources are opaque; no Rust or Java object layout is assumed.

### Several constructors

A resource may have several constructors, listed in `create=`:

```text
opaque Parsed
resource Parsed representation=native threading=shared create=parse,analyze destroy=release
```

Each constructor is validated and projected exactly as a single create is: it returns the resource
pointer (or, with `handle=`, reports a status and writes it through the named parameter, which every
constructor must then have), it is a returning `no_failure` native import with approved `nounwind`,
and it takes scalars and pointers only. A function constructs at most one resource and is never
also a destroy. The ownership APIs name each constructor's wrapper after its import:
`Parsed::parse(..)` and `Parsed::analyze(..)` in Rust, `Parsed.parse(factory, ..)` and
`Parsed.analyze(factory, ..)` in Java; such an import cannot be named after a member every owner
already has (`adoptUnsafe`, `DESTRUCTOR`, `from_raw`, `into_raw`, `borrow`, `pointer`, `_thread`).
A resource with exactly one constructor keeps the name `create` and generates what it always has,
and `seam.json` keeps its string form; several constructors are a list there, and therefore in the
fingerprint. Each constructor's Truffle cell returns an owner, as a single create's does.

### Constructors with call-scoped buffers

A constructor may declare `borrow ... lifetime=call` buffers, including one that returns the
resource pointer:

```text
borrow parse source length=length lifetime=call
import parse symbol=parsed_parse return=ptr<Parsed> error=no_failure nounwind=true@explicit_contract native_leaf=false@explicit_contract
  param source type=ptr<u8> nullable=true ownership=borrowed access=read nocapture=true@explicit_contract readonly=true@explicit_contract
  param length type=u64
end
```

The returned handle is a fresh owned resource, never a view into the buffer, and the buffer
contract already says the callee stops using the buffer before it returns. Every rule of a
[call-scoped buffer](#call-scoped-buffers-including-off-heap-storage) applies. The constructor's
wrappers take the buffers in place of pointer and length and lend them for the call only, through
the same buffer wrappers as any other call borrow: `Parsed::parse(source: Option<&[u8]>)` in Rust,
`Parsed.parse(factory, NativeBuffer)` and `Parsed.parse(factory, byte[])` in Native Image,
`Parsed.parse(binding, factory, MemorySegment)` in FFM; the Truffle cell lends the guest's buffer
as any cell does. The import's own buffer wrapper (`ownership::parse`, `NativeApiOwnership.parse`,
the `LibraryBinding` overload, the static JNI `parseBuffer`) is generated too and returns the raw
pointer. That pointer is owned: it leaks unless the caller adopts it (`from_raw`, `adoptUnsafe`)
or destroys it, so prefer the resource's constructor. The Rust wrapper is `#[must_use]`; Java has no
equivalent check, so treat the Java and JNI wrappers' result the same way. As for any nullable
buffer, a Native Image constructor whose buffers may all be absent must name the overload a literal
`null` means: `Parsed.parse(factory, (byte[]) null)`. A wrapper adds nothing to the create but the call borrow's own work: no allocation beyond
it and no change to leaf status or transitions. A call borrow on any other function that returns a
pointer stays rejected. A create that keeps its buffer past the call declares a
[held borrow](#borrows-held-by-a-resource) instead.

`threading=confined` requires access and destruction on the creating/adopting thread. `threading=shared` explicitly permits access and destruction from other threads, with the native implementation providing any necessary synchronization. It does not request reference counting or shared ownership. These are strong contracts, not properties inferred from the function signatures.

### Rust

`seam.rs` retains the raw ABI and adds `ownership::Ticket`:

- `create(...) -> Option<Ticket>` adopts successful allocations; with [several constructors](#several-constructors), each is an associated function named after its import.
- `Drop` calls the declared destructor, including during Rust unwinding.
- `borrow() -> TicketBorrow<'_>` ties a view to its owner; moving or dropping the owner while that view remains in use fails compilation.
- `into_raw()` consumes the owner without destroying the resource, transferring the release obligation to the caller.
- `unsafe from_raw(...)` accepts that obligation back. It requires the correct allocator/resource type, sole ownership, valid thread context, and no outstanding borrows.

Confined owners are neither `Send` nor `Sync`; shared owners explicitly implement both under the descriptor's contract. Borrowed views do not create Rust references into opaque native objects or imply `noalias`. Their raw-pointer escape hatch remains subject to unsafe FFI obligations. They are not lifetime-checked after conversion to a raw pointer.

### Implementing imports in Rust

`ownership` above is for a Rust *caller*. Where Rust *implements* the imports, `rust_implementation=trait` on the module line generates, in `seam.rs`, a safe trait named after the module (`ownership_demo` gives `OwnershipDemoImplementation`) and a macro that defines every import's `extern "C"` entry point from a type implementing it:

```rust
mod seam { include!(concat!(env!("SEAM_GENERATED"), "/seam.rs")); }
struct Demo;
impl seam::OwnershipDemoImplementation for Demo {
    type Parsed = Parsed; // the implementation's own type behind each `Parsed` handle
    type Ticket = Ticket;
    fn parse(source: Option<&[u8]>) -> Option<Box<Parsed>> { /* ... */ }
    fn release(parsed: Option<Box<Parsed>>) { drop(parsed) }
    fn parsedValue(parsed: &Parsed) -> u64 { parsed.value }
    fn decode(data: &[u8], text: Option<&mut [u8]>) -> u64 { /* ... */ }
    // ... one method per import
}
seam::define_implementations!(Demo, seam);
```

The trait has one method per import, so one `impl` covers the module:

- A [call-scoped buffer](#call-scoped-buffers-including-off-heap-storage) is a slice, and its length parameter drops out: `&[u8]` for read access, `&mut [u8]` for write or read/write, wrapped in `Option` when it is nullable. A `write` buffer is a `&mut [u8]` too, which Rust requires to hold initialized bytes: every generated wrapper lends initialized bytes, and a raw caller must do the same (a C caller lending an uninitialized `uint8_t out[64]` breaks the contract). There is no `&str` form: no borrow declares validated UTF-8 text yet.
- Each resource whose constructor returns its pointer is an associated type, bound `Send + Sync` for a `threading=shared` resource, which any thread may use and destroy; a confined resource's type has no bound. Its constructors return `Option<Box<Self::R>>` (`None` is a null result), its destroy takes `Box<Self::R>` (`Option<Box<Self::R>>` when the destroy is `nullable=true`), and any other borrowed `ptr<R>` parameter is `&Self::R` (`Option<&Self::R>` when nullable). Shared access stays shared: a `read_write` borrow of a resource is still `&Self::R`, so mutation goes through interior mutability, which the `Sync` bound makes thread-safe for a shared resource.
- Every other parameter and result, of every other import, is exactly as the raw binding declares it, raw pointers included.

`define_implementations!(Type)` expects the module's items in scope; `define_implementations!(Type, path)` takes the path of the module that includes `seam.rs`. Each entry point converts its arguments once and calls the implementation statically. It allocates nothing; `Box::into_raw` and `Box::from_raw` move the implementation's own allocation across the ABI. The slice conversions are `#[inline(always)]`. Rust ignores `#[inline]` on an exported function, so the entry point itself carries none. The entry points are `extern "C"`: as with any such function, a panic in the implementation aborts instead of unwinding across the ABI, so a `nounwind` import keeps its contract.

The caller's contract is checked where it can be. With debug assertions on, the entry points assert (and so abort) on each violation:

- a null pointer for a buffer declared `nullable=false`, whatever its length;
- an absent buffer with a nonzero length;
- a 64-bit length that is negative or past `isize::MAX` (a raw Java caller's `-1`, for one);
- a null pointer passed to a non-nullable destroy.

A release build never builds a slice, a reference or a `Box` from such arguments. It reads an empty slice for a bad buffer or length, and returns from a destroy given null. A null pointer for a non-nullable resource argument of any other import aborts in every build, because no result would be safe to make up: the `::core::panic!` cannot leave the `extern "C"` function.

`Box` is named unqualified in the trait and the macro: in a `no_std` crate with `alloc`, bring `alloc::boxed::Box` into scope where `seam.rs` is included and where the macro is invoked. Held borrows and resources with a `handle=` keep their raw form. A follow-up will map [batch borrows](https://github.com/elide-dev/myna/issues/132) once they exist. The generated items are named after the module and `myna_implementation`, and the generated code names `Box`, so no declaration may take any of these names. The option is serialized as `"rustImplementation": "trait"` and fingerprinted; modules without it generate exactly what they did. The [implementation fixture](../tests/ownership/implementation.rs) implements the ownership fixture's imports this way and runs under a [C caller](../tests/ownership/implemented.c).

### Native Image: consumer-implemented lifecycle

A module with ownership declarations also generates `<NativeClass>Ownership.java` (`MynaOwnership.java` with default naming). Its API includes:

```java
@FunctionalInterface
public interface NativeDestructor {
    void destroy(long address);
}

@FunctionalInterface
public interface ResourceFactory<T> {
    T adopt(long address, NativeDestructor destructor, Thread requiredThread);
}
```

Use `NativeApiOwnership.Ticket.create(myResourceFactory)` to allocate and return your own managed object (with several constructors, `NativeApiOwnership.Parsed.parse(myResourceFactory, ...)` and so on, one per import). The generated factory passes a reusable destructor callback and the required thread (`null` for shared resources). A null native result returns null without invoking the consumer. An exception or null result from adoption triggers immediate native cleanup. **A failed adoption must roll back its registrations without freeing the address**; a successful adoption transfers the sole release obligation to the consumer. `adoptUnsafe` supports existing native allocations under the same contract; null arguments are rejected before adoption.

The consumer owns access guards, exactly-once release, aliases, synchronization, and integration with its GC or scope system. The callback must not run while a native borrow is active. The generator cannot enforce those guarantees inside an arbitrary consumer implementation. Lifecycle callbacks are not automatically eligible for `NO_TRANSITION`.

GC registration belongs above this API: your resource factory can register cleanup with supported Native Image facilities and preserve any isolate/thread requirements. A cleanup action must not strongly retain its referent. A confined destructor must be dispatched to its required thread rather than run on a GC worker. The integration fixture demonstrates a consumer using a `Cleaner` with a separate cleanup state and atomic exactly-once release; the generator has no dependency on that choice. The test invokes the registered action deterministically rather than relying on GC timing.

## Call-scoped buffers, including off-heap storage

```text
borrow checksum bytes length=count lifetime=call
import checksum symbol=buffer_checksum return=u64 error=no_failure nounwind=true@explicit_contract
  param bytes type=ptr<u8> nullable=false ownership=borrowed access=read nocapture=true@explicit_contract readonly=true@explicit_contract
  param count type=u64
end
```

`borrow` declares a complete buffer contract: the implementation uses only the supplied byte extent, handles every permitted scalar argument without additional memory-safety preconditions, does not free or retain the buffer, and stops using it before returning. Read borrows prohibit mutation through any alias during the call; write and read/write borrows require exclusive access during the call. For asynchronous operations or retained callbacks, this declaration is invalid. A create that keeps its input until the resource it makes is released declares a [borrow held by that resource](#borrows-held-by-a-resource) instead.

Each buffer is a borrowed `ptr<u8>` with `read`, `write` or `read_write` access and a 32/64-bit signed or unsigned byte length (`usize`, which is `size_t`, counts as 64-bit unsigned); the function returns a scalar or void. A buffer declared `nullable=false` is always present; any other buffer is a [nullable buffer](#nullable-buffers) that may be absent. `nocapture=true` and, for read access, `readonly=true` require approved provenance even in relaxed mode. An `align=N@explicit_contract` fact on a buffer, call or held, is a caller contract: `seam.ll` emits the attribute as it does for any parameter, so the caller must pass an address divisible by `N`, at most 4096. LLVM's `align` does not depend on the extent, so every wrapper lends a present empty buffer a dummy aligned to `N` (an aligned arena allocation in FFM, an `_Alignas` local in the JNI shim, a rounded stack value in Native Image, an aligned static in Rust), and the assertion checks only what the caller lends. Debug builds check it at no release cost: the FFM, Native Image and JNI `ByteBuffer` wrappers use a Java `assert` (off on HotSpot without `-ea`, folded away in a Native Image built without assertions), and the Rust wrappers use `debug_assert!`, each a single AND of the address. Truffle cells do not trust the guest: native memory at a misaligned address is copied to an aligned copy (and written back when writable) instead of lent in place, heap copies start on 16 bytes or on `N` if larger, and a held borrow's copy likewise. The Native Image `byte[]` overloads pin an array, whose data is only 8-aligned, so with `N` above 8 they copy into aligned unmanaged memory (written back, then freed) instead. Additional `dereferenceable` or `noalias` requirements on a buffer are rejected.

A `write` buffer is a writable buffer, handled exactly like a `read_write` one by every wrapper: a mutable slice in Rust, a writable acquisition or segment in Java, a copy that is filled from the caller's bytes and written back in a Truffle cell. No wrapper relies on the callee not reading it, so the callee always sees the caller's initialized bytes, and the declaration needs no `writeonly` fact (an approved one remains an LLVM contract of its own).

A function may declare several borrows, and its other parameters may be pointers:

```text
borrow transfer input length=count lifetime=call
borrow transfer output length=capacity lifetime=call
import transfer symbol=buffer_transfer return=u64 error=no_failure nounwind=true@explicit_contract
  param input type=ptr<u8> nullable=false ownership=borrowed access=read nocapture=true@explicit_contract readonly=true@explicit_contract
  param count type=u64
  param output type=ptr<u8> nullable=false ownership=borrowed access=write nocapture=true@explicit_contract
  param capacity type=u32
  param copied type=ptr<u64> nullable=true ownership=borrowed access=write
end
```

- Each borrow names a buffer and a length parameter of its own. Two borrows of one buffer, one length shared by two buffers, and a buffer that is another borrow's length are rejected. Every rule above applies to each buffer. The order of the `borrow` lines does not matter: the descriptor sorts them by function and buffer, and wrappers follow the parameter order.
- Any other parameter is a scalar or a pointer. A pointer that is not a declared buffer **passes through every wrapper unchanged**, in the carrier the raw binding already uses for its type. The borrow declarations say nothing about it: it keeps its own declared nullability, access and ownership, and meeting them stays the caller's responsibility, exactly as on the raw binding. Callback, by-value record and isolate-thread parameters are rejected beside a borrow.
- A pointer result beside a borrow stays rejected, except for a resource's [constructor](#constructors-with-call-scoped-buffers), which returns a fresh resource: nothing yet ties any other returned pointer to the buffer it may point into.
- No wrapper checks that two buffers of one call are disjoint. Rust's borrow rules exclude a `&mut` slice that overlaps another; everywhere else, passing overlapping memory as a read buffer and a writable buffer breaks the read contract above and is the caller's error.

### C strings

```text
import titleLength symbol=title_length return=u64 error=no_failure nounwind=true@explicit_contract
  param title type=ptr<u8> nullable=false ownership=borrowed access=cstring nocapture=true@explicit_contract readonly=true@explicit_contract
end
```

`access=cstring` declares a read-only NUL-terminated string with no length: a borrowed `ptr<u8>`
of an import, read during the call up to its terminator and not retained. It needs approved
`nocapture` and `readonly`, the facts `seam.ll` then carries as for any read-only borrow, and it can
be no borrow's buffer or length. The raw bindings keep their pointer carriers (`uint8_t const *` in
`seam.h`, `*const u8` in Rust). The wrappers:

- **Rust**: `&::core::ffi::CStr` (`Option<&CStr>` when nullable), in the `ownership` wrapper of the
  import, and in the [implementer trait](#implementing-imports-in-rust), where a null one aborts.
- **FFM**: a `String` overload on `LibraryBinding`, which writes NUL-terminated UTF-8 into the
  call's confined arena (null is the null pointer when the parameter is nullable, and a
  `NullPointerException` otherwise).
- **Truffle cells**: strings only: a Java `String`, a `TruffleString`, or any interop string
  (converted with `asTruffleString`), lent NUL-terminated and read-only. Bytes, pointers and anything
  else are a guest error naming the function and parameter.
- **Native Image**: a `String` overload in the ownership class, converted with
  `CTypeConversion.toCString` for the call and freed after it. That conversion uses the image's
  default charset, UTF-8 unless the image was built with another `file.encoding`; FFM and Truffle
  always encode UTF-8.

A Java or guest string with an embedded NUL would end early on the C side, so every Java and
Truffle wrapper refuses one before copying it: FFM and Native Image throw
`IllegalArgumentException` naming the function and parameter, and a Truffle cell throws its guest
error after scanning the bytes it lent, with no Java string made (a nullable argument that is null is not checked). A Rust `&CStr` cannot contain one. An
`access=cstring` parameter takes no `align`, `dereferenceable` or `truffle_buffer` fact, and its import passes only scalars and pointers beside it. In the implementer trait, `Err` with a success status from an out-pointer constructor aborts in every build: the caller would own whatever its slot held.

### Nullable buffers

A buffer parameter that is `nullable=true` may be absent, as an optional output buffer is:

```text
borrow decode data length=length lifetime=call
borrow decode text length=capacity lifetime=call
import decode symbol=buffer_decode return=u64 error=no_failure nounwind=true@explicit_contract
  param data type=ptr<u8> nullable=false ownership=borrowed access=read nocapture=true@explicit_contract readonly=true@explicit_contract
  param length type=u64
  param text type=ptr<u8> nullable=true ownership=borrowed access=write nocapture=true@explicit_contract
  param capacity type=u32
end
```

- **An absent buffer reaches the native function as the null pointer with length zero**, in every wrapper. The declaration states that the function accepts exactly that for the parameter. No wrapper passes null with a nonzero length, or a null buffer for a parameter declared `nullable=false`.
- A buffer that is present behaves exactly as a non-nullable one does, and never has the null address: one that is present and empty still gets a non-null address with length zero, so the function can tell "no buffer" from "a buffer with no room".
- Pointer parameters default to `nullable=true`, so **a buffer declared without `nullable` is a nullable one**. Write `nullable=false` for a buffer that must always be there; such a declaration generates what it always has. A `nonnull` fact on a nullable buffer is rejected, as on any nullable pointer.
- Each wrapper spells "absent" in its own way:

| Wrapper | Present | Absent |
| --- | --- | --- |
| Rust | `Some(slice)` of `Option<&[u8]>` / `Option<&mut [u8]>` | `None` |
| Native Image, off-heap | a `NativeBuffer` | `null`: never acquired or released |
| Native Image, `byte[]` | an array | `null`: never pinned |
| FFM | a native `MemorySegment` | `null`, or a native segment of length zero at address zero such as `MemorySegment.NULL` |
| Static JNI | a direct `ByteBuffer` | `null` |
| Truffle cell | a buffer (or a string, for read access) | interop `null`, an interop pointer to address zero, or host `null` |

The example above generates `pub fn decode(data: &[u8], text: Option<&mut [u8]>) -> u64` in Rust, and `decode(data, null)` is unambiguous in Java because `data` has a type. Where every buffer of a function is nullable, a call that passes only literal `null`s fits both the `NativeBuffer` and the `byte[]` overload and must name the one it means, as in `tally((byte[]) null)` for a function with one optional buffer; a `null` held in a typed variable needs no cast.

Rust wrappers take `&[u8]` or `&mut [u8]` for each buffer (`Option<&[u8]>` or `Option<&mut [u8]>` for a nullable one) and derive each length. Length overflow panics before the foreign call. Empty slices are supported. No allocation, copying, or runtime ownership bookkeeping is introduced by these wrappers. A wrapper whose other parameters are all scalars is a safe `fn`: its slices vouch for everything it passes. A wrapper that passes any other pointer through is an `unsafe fn` with a `# Safety` section, because nothing vouches for that pointer; the example above generates

```rust
/// # Safety
/// Every pointer other than a borrowed buffer must satisfy the raw function's requirements.
pub unsafe fn transfer(input: &[u8], output: &mut [u8], copied: *mut u64) -> u64
```

Native Image wrappers accept a consumer implementation of:

```java
public interface NativeBuffer {
    void acquire(boolean writable);
    long address();
    long byteSize();
    void release(boolean writable);
}
```

A Netty/Bemo adapter can implement these methods on an existing buffer/view: retain or otherwise guard its backing allocation in `acquire`, expose the readable or writable region's address and extent, and release the guard afterward. **Reference counting alone does not establish exclusive mutable access.** Read-only buffers must reject writable acquisition; reader/writer indices, aliasing, and concurrent mutation remain the adapter's policy. The pointer and extent must remain stable for the entire acquired interval. Buffer slices should hold the same backing lifetime as their parent.

The generated call path performs acquire → address/length validation → direct C API call → release in `finally`, with a reachability fence. It adds no per-call Java allocation, copying, monitor, or registry lookup; the consumer hooks determine any retention cost. With an externally guaranteed lifetime/access discipline, those hooks can be no-ops. The raw generated binding remains available for callers that already enforce all contracts.

With several buffers, each is acquired and validated in parameter order and released in reverse, in a `finally` of its own. A buffer that cannot be acquired releases exactly the buffers already held; one that fails validation is released along with them. Either way the call never reaches native code. A `null` argument for a nullable buffer is skipped: nothing is acquired, validated or released for it, and the buffers around it are handled as before. Each buffer is acquired separately, so whether one owner may back two buffers of the same call (a read lease and a write lease at once) is the consumer's `acquire` policy. Other pointer parameters keep the raw binding's word carriers (`CIntPointer`, `PointerBase`, …) and are not checked.

Negative lengths, lengths outside the ABI integer range, and null addresses with nonzero lengths fail before native entry. For empty buffers at address zero, generated code supplies a non-null dummy with length zero (aligned to the buffer's `align` fact when it has one); that holds for a nullable buffer too, since a `NativeBuffer` that is passed is present whatever it reports. The consumer must validate actual allocation bounds; an address and length alone cannot prove those bounds.

An additional `byte[]` overload pins the array for the call and unpins it afterward. It is a convenience path separate from off-heap access. Array callers must prevent conflicting concurrent access. No pointer may escape the pin's lifetime. With several buffers the overload takes every buffer as a `byte[]`: non-empty arrays are pinned in parameter order and unpinned in reverse, and an empty array gets the stack dummy instead of a pin. A `null` array for a nullable buffer is not pinned and passes the null pointer. There is no overload that mixes arrays with `NativeBuffer`s.

### Batches of buffers

An import that takes a batch of buffers passes them as two parallel arrays and a count:

```text
borrow digest sources lengths=lengths count=count lifetime=call
import digest symbol=buffer_batch_digest return=u64 error=no_failure nounwind=true@explicit_contract native_leaf=false@explicit_contract
  param sources type=ptr<ptr<u8>> nullable=true ownership=borrowed access=read nocapture=true@explicit_contract readonly=true@explicit_contract
  param lengths type=ptr<u64> nullable=true ownership=borrowed access=read nocapture=true@explicit_contract readonly=true@explicit_contract
  param count type=u64
end
```

The contract is the single-buffer one applied per element: the callee reads only
`sources[i][0 .. lengths[i]]` for `i < count`, reads the two arrays only up to `count`, and keeps
none of it past the call.

- `sources` is a borrowed `ptr<ptr<u8>>` and `lengths` a borrowed pointer to `u32`, `u64`, `i32` or
  `i64`, each `access=read`, `nullable=true` and with approved `nocapture` and `readonly`; `count` is
  a 32- or 64-bit integer. Neither array may declare `align`, `dereferenceable`, `noalias` or
  `nonnull`: `seam.ll` gives them the facts a single read buffer gets, and no extent.
- **The empty batch is a null `sources` and a null `lengths` with count zero**, which every wrapper
  passes for an empty array of buffers.
- Elements are read-only and present: each is non-null, and a present empty element gets a non-null
  address with length zero. With `elements_nullable=true` an element may be absent instead, and then
  is the null pointer with length zero.
- An import may declare several batches beside single buffers. The three parameters of a batch
  belong to it alone. A batch ends with the call: `lifetime=resource:` is rejected.

| Wrapper | Batch | Absent element |
| --- | --- | --- |
| Rust | `&[&[u8]]`, or `&[Option<&[u8]>]` | `None` |
| Native Image, off-heap | `NativeBuffer[]` | `null` |
| Native Image, arrays | `byte[][]` | `null` |
| FFM | `MemorySegment[]` | `null`, or a native segment of length zero at address zero |

- **Rust** builds both arrays on the stack for up to eight buffers and allocates nothing, so
  `seam.rs` builds in a `no_std` crate without an allocator. Above eight, the plain wrapper panics
  with a message naming the `_with_scratch` form, which takes a `BatchScratch` after each batch:
  caller-provided `MaybeUninit` slices for the pointers and the lengths, at least as long as the
  batch (it panics otherwise).

  ```rust
  let mut pointers = vec![MaybeUninit::uninit(); parts.len()];
  let mut lengths = vec![MaybeUninit::uninit(); parts.len()];
  digest_with_scratch(&parts, seam::BatchScratch::new(&mut pointers, &mut lengths));
  ```

  A resource constructor whose import takes a batch has the same two forms:
  `Bundle::create(&parts)` keeps up to eight elements on the stack and panics above that, and
  `Bundle::create_with_scratch(&parts, scratch)` (`NAME_with_scratch` for a constructor named after
  its import) takes a `BatchScratch` after each batch. A length or count that does not fit its type
  panics before the foreign call. The lending
  helpers live in a hidden `ownership::batch` module; the names `BufferBatch`, `BufferBatchIter`,
  `OptionalBufferBatch`, `OptionalBufferBatchIter`, `BatchLength`, `BatchScratch` and `batch`, an
  import named `F_with_scratch` beside a batch import `F`, and a parameter named `P_scratch` beside
  a batch `P` are rejected.
- **Native Image** takes one snapshot of a `NativeBuffer[]` (one array copy), then acquires each
  element in order, after the function's single buffers, and releases them in reverse after the
  call; a store into the caller's array during the call cannot change what is released. A
  `byte[][]` pins each non-empty element and keeps the pins in its own array. Each element is
  validated as a single buffer is; a failure releases exactly the elements already held, and the
  call never reaches native code. Both arrays share one region: 128 bytes of stack for up to eight
  elements, unmanaged memory (freed after the call) otherwise. A count its type cannot carry is
  rejected first. A `null` batch array is rejected; pass an empty one.
- **FFM** snapshots the `MemorySegment[]` and validates each segment as a single buffer is (native,
  alive, accessible from the calling thread, within the length type). The linker holds the scope
  of a segment argument for the call, but not of an address the callee finds in memory, so an
  element is lent in place only when nothing can close it during the call:
  - the global scope (`scope().equals(Arena.global().scope())`, which holds for
    `MemorySegment.ofAddress(..).reinterpret(..)`), or
  - a segment confined to the calling thread, when the import is a native leaf: a leaf cannot call
    back into Java, which is the only way the owner thread could close it. Confinement is detected
    with public API: the segment is accessible from the calling thread but not from a thread that
    never starts (`isAccessibleBy`).

  Every other element (a shared or automatic arena, or a confined one under a non-leaf import) is
  copied into the call's confined arena, as a single buffer's temporary storage is, and the copy
  is lent. The arrays live in the same arena.
- **Static JNI** generates no buffer adapter for an import with a batch; its raw binding remains.
- **Truffle cells** do not lend batches yet: an import with one must declare
  `truffle_cell=false@explicit_contract`.

An implementation in Rust can read a batch through the safe views `seam.rs` defines when a module
declares one:

```rust
#[unsafe(no_mangle)]
pub unsafe extern "C" fn buffer_batch_digest(sources: *const *mut u8, lengths: *const u64, count: u64) -> u64 {
    let batch = unsafe { seam::BufferBatch::from_raw(sources, lengths, count) };
    batch.iter().map(|bytes| bytes.len() as u64).sum()
}
```

`BufferBatch<'a, L>` yields `&'a [u8]` and `OptionalBufferBatch<'a, L>` yields `Option<&'a [u8]>`,
where `L` is the length type (`u64` by default). Each is a `Copy`, `Send` and `Sync` view with
`len()`, `is_empty()`, `get(i)` (counted from the start) and `iter()`, and is `IntoIterator`; the
iterators are `ExactSizeIterator` and `FusedIterator`. `from_raw` requires that, unless the count
is zero, both arrays are non-null, valid for `count` reads, and stay valid and unmodified for
`'a`; that every length is non-negative and at most `isize::MAX`; and that each element is valid
for reads of its length (null only with length zero, which `OptionalBufferBatch` yields as `None`)
and unmutated for `'a`.

## Creates that report a status

Many C creates do not return the resource. They return a status and write the resource through a
parameter: into a field of a struct the caller allocates, or by initializing the caller's struct
in place, which is then released by passing the same struct back.

```text
opaque Layout
struct LayoutResult size=24 align=8
  field count type=u64 offset=0
  field data type=ptr<Layout> offset=8
  field capacity type=u64 offset=16
end
resource Layout representation=native threading=confined create=layoutCompile destroy=layoutRelease handle=result.data success=0,2

struct View size=16 align=8
  field sum type=i64 offset=0
  field scratch type=ptr<u8> offset=8
end
resource View representation=native threading=confined create=viewInit destroy=viewRelease handle=view success=0
```

- `handle=PARAMETER.FIELD`: the create writes the resource pointer into that field of the struct
  the parameter points to. The field must be a `ptr<resource>`, and the resource an opaque type.
- `handle=PARAMETER`: the struct the parameter points to is the resource, initialized in place.
  The resource then names that struct, and `destroy` takes a pointer to it.
- `success=N[,N…]` lists the results that mean the resource exists. It is required when the create
  returns an integer and not allowed when it returns `void`. Any other result means nothing was
  created and nothing is to be released.

The parameter must be a non-null pointer with `write` or `read_write` access. The contract is the
create's: on a listed status it has written a live resource and transferred sole ownership of it;
on any other it has left nothing to release.

Only Truffle cells generate an owner for the two struct forms, so the module must set `truffle_cells=true`
and the create must have a cell. A cell does not let the create write into guest memory: it
passes a struct the runtime owns and copies the result to the buffer the guest supplied. The Native Image, FFM and Rust ownership APIs do not wrap them;
their raw bindings, and any call-buffer wrappers the create declares, are generated as for any
other import. See [Truffle native cells](truffle-cells.md#ownership) for what the cell does.

### Status plus out-pointer

The usual C shape for "status plus handle" writes the handle through a `ptr<ptr<R>>` parameter,
and every ownership API wraps it:

```text
opaque Surface
resource Surface representation=native threading=confined create=surfaceOpen,surfaceFrom destroy=surfaceClose handle=out success=0
borrow surfaceOpen error length=capacity lifetime=call
import surfaceOpen symbol=surface_open return=i32 enum=SurfaceStatus error=no_failure nounwind=true@explicit_contract native_leaf=false@explicit_contract
  param width type=i32
  param out type=ptr<ptr<Surface>> nullable=true ownership=borrowed access=write
  param error type=ptr<u8> nullable=true ownership=borrowed access=write nocapture=true@explicit_contract
  param capacity type=usize
end
```

`handle=PARAMETER` names a `ptr<ptr<R>>` parameter with `access=write`, for an opaque or struct
resource `R`. The create returns an integer status, `success=` is required, and every constructor of
the resource takes this shape with one status type. The parameter may be declared nullable: every
wrapper passes a slot of its own. On a listed status the slot holds a live resource, adopted as any
create's is. On any other status nothing is adopted and the slot's contents are ignored, whatever
the create left there. A listed status with a null handle created nothing either, and every wrapper
treats it as a failure with that status. Statuses compare unsigned for unsigned results: a `u8`
status of 200 matches `success=200` even though Java carries it in a `byte`. The caller decides what that
status means:

- **FFM**: `ApiFFM.Surface.surfaceOpen(binding, factory, onFailure, width, error)` allocates the slot in
  a confined arena for the call (the FFM wrappers keep no per-thread scratch to take it from), and on any other status returns `onFailure.failed(status)`, from a
  generated `Surface.Failure<T>` whose `failed` takes the status in its Java carrier (`int` for an
  `i32`, the declared carrier for an `enum=` result): null, a value, or a throw. The caller can read
  the message buffer it lent there.
- **Native Image**: `ApiOwnership.Surface.surfaceOpen(factory, onFailure, width, error)`, the same
  with the slot on the stack, and a `NativeBuffer` and a `byte[]` overload when the create has call
  buffers.
- **Rust**: `Surface::surfaceOpen(width, error) -> Result<Surface, SurfaceStatus>`: `Err` carries the
  status, as the `enum=` newtype when the result has one and the raw integer otherwise.
- **Truffle cells**: as for a handle field, the guest lends an 8-byte buffer for the slot and
  receives a copy; any other status is a guest error that names it.

With one constructor the wrapper is `create`, as for any resource. In the [implementer
trait](#implementing-imports-in-rust) such a constructor returns `Result<Box<Self::R>, Status>` and
drops the out-parameter: on `Ok` the entry point writes the box into the slot and returns the first
`success=` value (a null slot, which only a nullable parameter allows, drops the box instead); on
`Err(status)` it returns that status, aborting in every build if it is a success value. The destroy
takes `Box<Self::R>` and other handles are `&Self::R`, as for any resource. A constructor with a held borrow gets no Native Image, FFM or Rust wrapper.

## Borrows held by a resource

Some creates keep their input. A decoder that adopts its input region records pointers into it in
the result it initializes, and reads them until that result is released:

```text
struct Adopted size=24 align=8
  field data type=ptr<u8> offset=0
  field length type=u64 offset=8
  field sum type=i64 offset=16
end
resource Adopted representation=native threading=confined create=adopt destroy=adoptedRelease handle=adopted success=0
borrow adopt data length=length lifetime=resource:Adopted
borrow adopt key length=keyLength lifetime=call
import adopt symbol=decode_adopted return=i32 error=no_failure nounwind=true@explicit_contract native_leaf=false@explicit_contract
  param data type=ptr<u8> nullable=true ownership=borrowed access=read readonly=true@explicit_contract
  param length type=u64
  param key type=ptr<u8> nullable=true ownership=borrowed access=read nocapture=true@explicit_contract readonly=true@explicit_contract
  param keyLength type=u32
  param adopted type=ptr<Adopted> nullable=false ownership=borrowed access=write
end
```

`lifetime=resource:NAME` declares that the create of resource `NAME` keeps the buffer from the call
until that resource is destroyed. The contract is the create's: on a listed status it may read the
buffer's `length` bytes at any time until its `destroy` returns, and it never writes them, frees
them, or uses them after that; on any other status it has kept nothing. The other rules of a
`borrow` apply unchanged (a borrowed `ptr<u8>`, a length of its own, `readonly`, no alignment or
extent preconditions), with three differences:

- the function must be a constructor of `NAME`. For a resource the Rust, FFM and Native Image owners
  wrap (one whose create returns its pointer, or writes it through a `ptr<ptr<R>>` out-parameter),
  any constructor may hold. For a struct handle (`handle=PARAMETER.FIELD` or a struct initialized in
  place) the create must have a Truffle cell, since only a cell's owner wraps it. A Truffle cell
  holds a buffer only beside the struct or slot of a `handle=` create, so a create that returns its
  pointer and holds a buffer opts out of cells with `truffle_cell=false@explicit_contract`;
- the buffer is read-only (`access=read`): nothing could be written back once the call has returned;
- the buffer is captured, so it must not be declared `nocapture`.

A held borrow may sit beside call borrows on the same create, as `key` does above: those still end
with the call. The validator rejects a held borrow anywhere else with the rule it breaks.

Each owner keeps a held buffer alive in its own way:

- **Rust**: the owner carries the buffer's lifetime, `Doc<'held>`, and the constructor takes
  `data: &'held [u8]`. The borrow checker then keeps the bytes alive and unmodified until the owner
  is dropped, and `Drop` still calls the destroy first. Call borrows beside it are lent for the call
  only, and a C string as `&CStr`. Owners of resources without held borrows keep their plain type.
- **FFM and Native Image**: the constructor copies the bytes, once, into native memory the owner
  holds: an arena in FFM (shared or confined as the resource is), unmanaged memory in Native Image.
  The owner's destructor frees it after the native destroy returns, through the same destructor the
  factory receives, never a cleaner; a create that makes nothing frees it at once. The caller's
  segment, `NativeBuffer` or array may then change or close immediately. These wrappers exist for a
  constructor with exactly one held buffer and no other buffer, batch or C string; any other keeps
  only its raw binding.
- A writable held buffer is not declarable (above): the native side would write into memory no Java
  caller could observe safely after the call.
- Static JNI generates no wrapper for a function with a held buffer; its raw binding remains, and
  its caller keeps the buffer alive until the release. See [Truffle native cells](truffle-cells.md#borrows-held-by-a-resource) for what the cell
does with each kind of guest value.

## Truffle cells

With `truffle_cells=true`, the same declarations project into guest-callable cells: each `borrow` drops its length from the guest signature and lends native memory in place, pins or copies heap bytes for exactly the call (or, for a borrow held by a resource, until that resource is destroyed), takes interop `null` for a nullable buffer that is absent, and a `resource` returns a guest-visible owner with exactly-once close and thread confinement. See [Truffle native cells](truffle-cells.md).

## Adoption priorities and remaining work

Implemented now: native resource construction/destruction contracts, several constructors per resource, scalar and pointer constructor arguments, call-scoped buffers on a constructor, destructors that accept null, creates that report a status and write their resource through a parameter, and borrows such a create keeps until its resource is destroyed (Truffle cells), Rust RAII and ownership transfer, owner-bound opaque borrows, confined/shared thread contracts, pluggable Native Image ownership, and zero-copy off-heap and pinned-array call borrows, several per function, beside pass-through pointers, and nullable where a buffer may be absent, and batches of read buffers with a parallel array of lengths. FFM additionally projects these declarations into consumer-owned resource factories and scoped `MemorySegment` borrows; static JNI projects call borrows into direct-buffer overloads. See the [adapter guide](jvm-roadmap.md). Ownership metadata is serialized and fingerprinted. No ownership label silently becomes an LLVM optimizer attribute.

Next, in adoption order:

1. Native operations consuming owned resources, with explicit transfer-on-success/failure rules; typed element views; buffers without a length parameter of their own, and one length shared by several buffers.
2. Returned buffer views tied to an owner, including mutation/invalidation rules and pooled-buffer retention.
3. SVM-managed Java objects represented by isolate-bound rooted handles; release of a handle is distinct from destruction of its referent.
4. Retained asynchronous buffer borrows and retain/release protocols beyond a create keeping its read-only input until its resource is destroyed; writable held buffers. FFM upcall stubs already have explicit caller-owned arena lifetimes.

These future forms currently fail parsing/validation rather than receiving invented safe lifetimes. GC-rooted Java objects must not be declared as `representation=native`. Cross-isolate ownership and automatic lifetime inference are not implemented.

The [owned example](../examples/ownership.seam) and [consumer fixture](../tests/ownership/OwnershipMain.java) provide concrete starting points; the fixture is generated from [the example plus a call with several buffers beside another pointer and two calls with a nullable buffer](../tests/ownership/ownership.seam). Integration tests execute both generated Rust ownership and Native Image consumer APIs against actual native allocations, check failure cleanup and repeated release, run the two-buffer `transfer` import and the nullable `decode` and `tally` imports, with their buffer absent and present, through every wrapper (Rust, Native Image off-heap and array, FFM, static JNI), and compile-fail Rust borrow escape, confined cross-thread moves and a pointer-passing wrapper called outside `unsafe`.
