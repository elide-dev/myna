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

This declaration is an explicit implementation contract: the constructor returns either null or sole ownership of a fresh, live resource; the destructor consumes that ownership exactly once. Constructor scalar arguments are supported. All scalar values must be handled without undefined behavior; invalid inputs can return null. A successful allocation satisfies the destructor's requirements. Both functions must be native imports, returning normally without unwinding across the ABI. Resources are opaque; no Rust or Java object layout is assumed.

`threading=confined` requires access and destruction on the creating/adopting thread. `threading=shared` explicitly permits access and destruction from other threads, with the native implementation providing any necessary synchronization. It does not request reference counting or shared ownership. These are strong contracts, not properties inferred from the function signatures.

### Rust

`seam.rs` retains the raw ABI and adds `ownership::Ticket`:

- `create(...) -> Option<Ticket>` adopts successful allocations.
- `Drop` calls the declared destructor, including during Rust unwinding.
- `borrow() -> TicketBorrow<'_>` ties a view to its owner; moving or dropping the owner while that view remains in use fails compilation.
- `into_raw()` consumes the owner without destroying the resource, transferring the release obligation to the caller.
- `unsafe from_raw(...)` accepts that obligation back. It requires the correct allocator/resource type, sole ownership, valid thread context, and no outstanding borrows.

Confined owners are neither `Send` nor `Sync`; shared owners explicitly implement both under the descriptor's contract. Borrowed views do not create Rust references into opaque native objects or imply `noalias`. Their raw-pointer escape hatch remains subject to unsafe FFI obligations. They are not lifetime-checked after conversion to a raw pointer.

### Native Image: consumer-implemented lifecycle

A module with ownership declarations also generates `<NativeClass>Ownership.java` (`SeamOwnership.java` with default naming). Its API includes:

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

Use `NativeApiOwnership.Ticket.create(myResourceFactory)` to allocate and return your own managed object. The generated factory passes a reusable destructor callback and the required thread (`null` for shared resources). A null native result returns null without invoking the consumer. An exception or null result from adoption triggers immediate native cleanup. **A failed adoption must roll back its registrations without freeing the address**; a successful adoption transfers the sole release obligation to the consumer. `adoptUnsafe` supports existing native allocations under the same contract; null arguments are rejected before adoption.

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

`borrow` declares a complete buffer contract: the implementation uses only the supplied byte extent, handles every permitted scalar argument without additional memory-safety preconditions, does not free or retain the buffer, and stops using it before returning. Read borrows prohibit mutation through any alias during the call; read/write borrows require exclusive access during the call. For asynchronous operations or retained callbacks, this declaration is invalid.

The first version supports one byte buffer per function, `read` or `read_write` access, 32/64-bit signed or unsigned byte lengths, scalar additional arguments, and scalar/void results. Pointer results, extra pointer arguments, and additional `align`, `dereferenceable`, or `noalias` requirements are rejected. `nocapture=true` and, for read access, `readonly=true` require approved provenance even in relaxed mode.

Rust wrappers take `&[u8]` or `&mut [u8]` and derive the length. Length overflow panics before the foreign call. Empty slices are supported. No allocation, copying, or runtime ownership bookkeeping is introduced by these wrappers.

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

The generated call path performs acquire → address/length validation → direct C API call → release in `finally`, with a reachability fence. It adds no per-call Java allocation, copying, monitor, or registry lookup; the consumer hooks determine any retention cost. With an externally guaranteed lifetime/access discipline, those hooks can be no-ops. The raw `@CFunction` remains available for callers that already enforce all contracts.

Negative lengths, lengths outside the ABI integer range, and null addresses with nonzero lengths fail before native entry. For empty buffers at address zero, generated code supplies a non-null stack dummy with length zero. The consumer must validate actual allocation bounds; an address and length alone cannot prove those bounds.

An additional `byte[]` overload pins the array for the call and unpins it afterward. It is a convenience path separate from off-heap access. Array callers must prevent conflicting concurrent access. No pointer may escape the pin's lifetime.

## Adoption priorities and remaining work

Implemented now: native resource construction/destruction contracts, scalar constructor arguments, Rust RAII and ownership transfer, owner-bound opaque borrows, confined/shared thread contracts, pluggable Native Image ownership, and zero-copy off-heap and pinned-array call borrows. Ownership metadata is serialized and fingerprinted. Existing descriptors without these declarations retain their output shape. No ownership label silently becomes an LLVM optimizer attribute.

Next, in adoption order:

1. Native operations consuming owned resources, with explicit transfer-on-success/failure rules; multiple buffers and typed element views.
2. Returned buffer views tied to an owner, including mutation/invalidation rules and pooled-buffer retention.
3. SVM-managed Java objects represented by isolate-bound rooted handles; release of a handle is distinct from destruction of its referent.
4. Retained asynchronous borrows/callbacks, retain/release protocols, and additional FFM/JNI projections.

These future forms currently fail parsing/validation rather than receiving invented safe lifetimes. GC-rooted Java objects must not be declared as `representation=native`. Cross-isolate ownership and automatic lifetime inference are not implemented.

The [owned example](../examples/ownership.seam) and [consumer fixture](../tests/ownership/OwnershipMain.java) provide concrete starting points. Integration tests execute both generated Rust ownership and Native Image consumer APIs against actual native allocations, check failure cleanup and repeated release, and compile-fail Rust borrow escape and confined cross-thread moves.
