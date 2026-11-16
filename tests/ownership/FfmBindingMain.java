// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
import fixture.owned.NativeApiFFM;
import fixture.owned.ConfinedApiFFM;
import java.lang.foreign.*;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

/** Runs unchanged on HotSpot and Native Image. */
public final class FfmBindingMain {
  @FunctionalInterface interface Action { void run() throws Throwable; }
  private static void rejects(Class<? extends Throwable> type, Action action) throws Throwable {
    try { action.run(); } catch (Throwable failure) {
      if (type.isInstance(failure)) return;
      throw failure;
    }
    throw new AssertionError("expected " + type.getName());
  }
  public static void main(String[] args) throws Throwable {
    for (int iteration = 0; iteration < 3; iteration++) {
      NativeApiFFM.LibraryBinding binding;
      try (var library = Arena.ofConfined(); var memory = Arena.ofConfined()) {
        binding = new NativeApiFFM.LibraryBinding(Path.of(args[0]), library);
        var bytes = memory.allocateFrom(ValueLayout.JAVA_BYTE, (byte) 1, (byte) 2, (byte) 3);
        if (binding.checksum(bytes.asReadOnly()) != 6) throw new AssertionError("checksum");
        binding.increment(bytes);
        if (binding.checksum(bytes) != 9) throw new AssertionError("increment");
        if (binding.checksum(MemorySegment.NULL) != 0) throw new AssertionError("empty");
        binding.increment(bytes.asSlice(0, 0));
        rejects(IllegalArgumentException.class, () -> binding.increment(bytes.asReadOnly()));
        rejects(IllegalArgumentException.class, () -> binding.checksum(MemorySegment.ofArray(new byte[1])));
        rejects(IllegalArgumentException.class, () -> binding.checksum(MemorySegment.NULL.reinterpret(1)));
        var closedArena = Arena.ofConfined();
        var closed = closedArena.allocate(1);
        closedArena.close();
        rejects(IllegalStateException.class, () -> binding.checksum(closed));
        // Two segments with lengths of their own beside a pointer that passes through unchanged.
        var target = memory.allocate(2);
        var copied = memory.allocate(ValueLayout.JAVA_LONG);
        copied.set(ValueLayout.JAVA_LONG, 0, -1);
        if (binding.transfer(bytes.asReadOnly(), target, copied) != 5 || copied.get(ValueLayout.JAVA_LONG, 0) != 2
            || target.get(ValueLayout.JAVA_BYTE, 0) != 2 || target.get(ValueLayout.JAVA_BYTE, 1) != 3)
          throw new AssertionError("transfer");
        if (binding.transfer(bytes, target, MemorySegment.NULL) != 5) throw new AssertionError("null pass-through");
        // Either buffer may be empty, at a null address or not; the other is still borrowed.
        if (binding.transfer(MemorySegment.NULL, target, copied) != 0 || copied.get(ValueLayout.JAVA_LONG, 0) != 0)
          throw new AssertionError("empty input");
        copied.set(ValueLayout.JAVA_LONG, 0, -1);
        if (binding.transfer(bytes, target.asSlice(2, 0), copied) != 0 || copied.get(ValueLayout.JAVA_LONG, 0) != 0)
          throw new AssertionError("empty output");
        if (binding.transfer(MemorySegment.NULL, MemorySegment.NULL, MemorySegment.NULL) != 0)
          throw new AssertionError("both empty");
        // Every buffer is validated, whichever position it has.
        rejects(IllegalArgumentException.class, () -> binding.transfer(bytes, target.asReadOnly(), copied));
        rejects(IllegalArgumentException.class, () -> binding.transfer(MemorySegment.ofArray(new byte[1]), target, copied));
        rejects(IllegalArgumentException.class, () -> binding.transfer(bytes, MemorySegment.ofArray(new byte[1]), copied));
        rejects(IllegalArgumentException.class, () -> binding.transfer(bytes, MemorySegment.NULL.reinterpret(1), copied));
        rejects(IllegalStateException.class, () -> binding.transfer(closed, target, copied));
        rejects(IllegalStateException.class, () -> binding.transfer(bytes, closed, copied));
        if (copied.get(ValueLayout.JAVA_LONG, 0) != 0 || target.get(ValueLayout.JAVA_BYTE, 0) != 2)
          throw new AssertionError("a rejected transfer reached native code");
        // A nullable buffer: null, or the null address with length zero, is an absent buffer and
        // reaches native code as the null pointer with length zero, which the fixture reports as
        // ABSENT. A segment that is present, empty or not, never has the null address.
        final long absent = 1L << 32;
        var text = memory.allocate(2);
        if (binding.decode(bytes.asReadOnly(), text) != 3 || text.get(ValueLayout.JAVA_BYTE, 0) != 3
            || text.get(ValueLayout.JAVA_BYTE, 1) != 4) throw new AssertionError("decode");
        if (binding.decode(bytes, null) != (3 | absent)) throw new AssertionError("absent output");
        if (binding.decode(bytes, MemorySegment.NULL) != (3 | absent)) throw new AssertionError("null output");
        if (binding.decode(bytes, MemorySegment.ofAddress(0)) != (3 | absent)) throw new AssertionError("zero output");
        if (binding.decode(bytes, text.asSlice(2, 0)) != 3) throw new AssertionError("empty output is present");
        // The required buffer stays a present one at the null address; only the other is absent.
        if (binding.decode(MemorySegment.NULL, null) != absent) throw new AssertionError("empty input, absent output");
        if (binding.decode(MemorySegment.NULL, text) != 0) throw new AssertionError("empty input");
        // A buffer that is present is validated as any other is; an absent one is not looked at.
        rejects(IllegalArgumentException.class, () -> binding.decode(bytes, text.asReadOnly()));
        rejects(IllegalArgumentException.class, () -> binding.decode(bytes, MemorySegment.ofArray(new byte[1])));
        rejects(IllegalArgumentException.class, () -> binding.decode(bytes, MemorySegment.ofArray(new byte[0])));
        rejects(IllegalArgumentException.class, () -> binding.decode(bytes, MemorySegment.NULL.reinterpret(1)));
        rejects(IllegalStateException.class, () -> binding.decode(bytes, closed));
        rejects(NullPointerException.class, () -> binding.decode(null, text));
        if (binding.decode(bytes, MemorySegment.NULL.asReadOnly()) != (3 | absent)) throw new AssertionError("read-only null");
        // One optional segment.
        if (binding.tally(bytes.asReadOnly()) != 9) throw new AssertionError("tally");
        if (binding.tally(null) != absent || binding.tally(MemorySegment.NULL) != absent) throw new AssertionError("absent input");
        if (binding.tally(bytes.asSlice(3, 0)) != 0) throw new AssertionError("empty input is present");
        rejects(IllegalArgumentException.class, () -> binding.tally(MemorySegment.ofArray(new byte[0])));
        rejects(IllegalStateException.class, () -> binding.tally(closed));
        batches(binding, memory, closed);
        var failure = new AtomicReference<Throwable>();
        var thread = new Thread(() -> {
          try {
            rejects(WrongThreadException.class, () -> binding.checksum(bytes));
            rejects(WrongThreadException.class, () -> binding.transfer(bytes, target, copied));
            rejects(WrongThreadException.class, () -> binding.decode(bytes, text));
            rejects(WrongThreadException.class, () -> binding.tally(bytes));
            rejects(WrongThreadException.class, () -> binding.liveCount());
          } catch (Throwable error) { failure.set(error); }
        });
        thread.start(); thread.join();
        if (failure.get() != null) throw failure.get();
        var missing = new NativeApiFFM.LibraryBinding(symbol -> java.util.Optional.empty());
        rejects(UnsatisfiedLinkError.class, () -> missing.liveCount());
        var owner = NativeApiFFM.Ticket.create(binding, (address, destructor, threadConstraint) -> {
          if (threadConstraint != null || address.address() == 0) throw new AssertionError("shared owner");
          return (Action) () -> destructor.destroy(address);
        });
        if (binding.liveCount() != 1) throw new AssertionError("owned resource");
        owner.run();
        if (binding.liveCount() != 0) throw new AssertionError("consumer destruction");
        rejects(IllegalStateException.class, () -> NativeApiFFM.Ticket.create(binding,
            (address, destructor, threadConstraint) -> { throw new IllegalStateException("adoption failed"); }));
        rejects(NullPointerException.class, () -> NativeApiFFM.Ticket.create(binding,
            (address, destructor, threadConstraint) -> null));
        if (binding.liveCount() != 0) throw new AssertionError("failed adoption leaked");
        // An unavailable destructor must preserve the adoption error and report cleanup failure.
        var lookup = SymbolLookup.libraryLookup(Path.of(args[0]), library);
        var noDestructor = new NativeApiFFM.LibraryBinding(symbol -> symbol.equals("ticket_destroy")
            ? java.util.Optional.empty() : lookup.find(symbol));
        var adoptedAddress = new AtomicReference<MemorySegment>();
        var adoptionFailure = new IllegalStateException("original adoption error");
        try {
          NativeApiFFM.Ticket.create(noDestructor, (address, destructor, threadConstraint) -> {
            adoptedAddress.set(address);
            throw adoptionFailure;
          });
          throw new AssertionError("adoption should fail");
        } catch (IllegalStateException expected) {
          if (expected != adoptionFailure || expected.getSuppressed().length != 1
              || !(expected.getSuppressed()[0] instanceof UnsatisfiedLinkError))
            throw new AssertionError("cleanup hid adoption error", expected);
        } finally {
          if (adoptedAddress.get() != null) binding.ticketDestroy(adoptedAddress.get());
        }
        if (binding.liveCount() != 0) throw new AssertionError("cleanup fixture leaked");
        binding.failNext();
        if (NativeApiFFM.Ticket.create(binding, (address, destructor, threadConstraint) -> {
          throw new AssertionError("null creation must not be adopted");
        }) != null) throw new AssertionError("null native allocation");
        // Two constructors of one resource, named after their imports, each lending its segment
        // for the call only: the resource keeps what it computed, never the buffer.
        NativeApiFFM.ResourceFactory<Action> adopt = (address, destructor, threadConstraint) -> {
          if (threadConstraint != null || address.address() == 0) throw new AssertionError("shared owner");
          return () -> destructor.destroy(address);
        };
        var values = new java.util.ArrayList<MemorySegment>();
        NativeApiFFM.ResourceFactory<Action> observe = (address, destructor, threadConstraint) -> {
          values.add(address);
          return () -> destructor.destroy(address);
        };
        var source = memory.allocateFrom(ValueLayout.JAVA_BYTE, (byte) 1, (byte) 2, (byte) 3);
        var parsed = NativeApiFFM.Parsed.parse(binding, observe, source.asReadOnly());
        source.set(ValueLayout.JAVA_BYTE, 0, (byte) 100);
        var empty = NativeApiFFM.Parsed.parse(binding, observe, memory.allocate(0));
        var analyzed = NativeApiFFM.Parsed.analyze(binding, observe, source, (byte) 2);
        if (binding.parsedValue(values.get(0)) != 6 || binding.parsedValue(values.get(1)) != 0
            || binding.parsedValue(values.get(2)) != 210 || binding.liveCount() != 3)
          throw new AssertionError("constructors");
        // An absent source reaches native code as null, which parse reports as failure.
        if (NativeApiFFM.Parsed.parse(binding, adopt, null) != null
            || NativeApiFFM.Parsed.parse(binding, adopt, MemorySegment.NULL) != null)
          throw new AssertionError("absent source");
        // A constructor's buffer is validated before native code runs, as any call buffer is.
        rejects(IllegalArgumentException.class, () -> NativeApiFFM.Parsed.analyze(binding, adopt,
            MemorySegment.ofArray(new byte[1]), (byte) 1));
        rejects(IllegalStateException.class, () -> NativeApiFFM.Parsed.analyze(binding, adopt, closed, (byte) 1));
        binding.failNext();
        if (NativeApiFFM.Parsed.analyze(binding, adopt, source, (byte) 1) != null) throw new AssertionError("failed analyze");
        parsed.run(); empty.run(); analyzed.run();
        // The release accepts null as a no-op; no owner ever passes it one.
        binding.release(MemorySegment.NULL);
        if (binding.liveCount() != 0) throw new AssertionError("constructed resources leaked");
      }
      rejects(IllegalStateException.class, () -> binding.liveCount());
    }
    verifyConfinedResources(Path.of(args[0]));
    System.out.println("Explicit FFM library lifetime and buffer borrowing passed");
  }

  // The fixture's batch digest (tests/ownership/native.rs): the count, then each element's length
  // and bytes, or NULL_ELEMENT for an absent one; the empty batch folds in ABSENT.
  static final long FNV_OFFSET = 0xcbf29ce484222325L;
  static final long NULL_ELEMENT = -2L;
  static long mix(long hash, long value) { return (hash ^ value) * 0x100000001b3L; }
  static long fold(long hash, byte[] bytes) {
    hash = mix(hash, bytes.length);
    for (byte b : bytes) hash = mix(hash, b & 0xff);
    return hash;
  }
  static long foldBatch(long hash, byte[]... buffers) {
    if (buffers.length == 0) return mix(hash, 1L << 32);
    hash = mix(hash, buffers.length);
    for (byte[] buffer : buffers) hash = buffer == null ? mix(hash, NULL_ELEMENT) : fold(hash, buffer);
    return hash;
  }

  /** Three buffers of different sizes, one empty; the empty batch; two batches in one call. */
  private static void batches(NativeApiFFM.LibraryBinding binding, Arena memory, MemorySegment closed) throws Throwable {
    byte[] a = {1, 2, 3}, b = {(byte) 255}, empty = {};
    var sa = memory.allocateFrom(ValueLayout.JAVA_BYTE, a);
    var sb = memory.allocateFrom(ValueLayout.JAVA_BYTE, b);
    var three = new MemorySegment[] {sa, sb.asSlice(1, 0), sb.asReadOnly()};
    if (binding.digest(three) != foldBatch(FNV_OFFSET, a, empty, b)) throw new AssertionError("batch digest");
    if (binding.digest(new MemorySegment[] {MemorySegment.NULL}) != foldBatch(FNV_OFFSET, empty))
      throw new AssertionError("an element at address zero is present and empty");
    if (binding.digest(new MemorySegment[0]) != foldBatch(FNV_OFFSET)) throw new AssertionError("empty batch");
    var many = new MemorySegment[20];
    var bytes = new byte[20][];
    for (int i = 0; i < many.length; i++) {
      bytes[i] = new byte[i];
      for (int j = 0; j < i; j++) bytes[i][j] = (byte) (i * 7 + j);
      many[i] = memory.allocateFrom(ValueLayout.JAVA_BYTE, bytes[i]);
    }
    if (binding.digest(many) != foldBatch(FNV_OFFSET, bytes)) throw new AssertionError("large batch");
    var key = memory.allocateFrom(ValueLayout.JAVA_BYTE, (byte) 7, (byte) 8);
    long expected = foldBatch(foldBatch(fold(FNV_OFFSET, new byte[] {7, 8}), a, b), b, null, empty);
    if (binding.merge(key, new MemorySegment[] {sa, sb}, new MemorySegment[] {sb, null, sb.asSlice(0, 0)}) != expected)
      throw new AssertionError("two batches");
    expected = foldBatch(foldBatch(fold(FNV_OFFSET, new byte[] {7, 8}), a), null, null);
    if (binding.merge(key, new MemorySegment[] {sa}, new MemorySegment[] {MemorySegment.NULL, null}) != expected)
      throw new AssertionError("absent elements");
    // An element is lent in place only when nothing can close it during the call: confined to
    // this thread under a native leaf, or the global scope. Anything else is lent as a copy.
    MemorySegment[] confined = {sa};
    if (binding.firstLeaf(confined) != sa.address()) throw new AssertionError("confined element of a leaf lent in place");
    if (binding.firstCall(confined) == sa.address()) throw new AssertionError("confined element of a non-leaf copied");
    var global = MemorySegment.ofAddress(sa.address()).reinterpret(3);
    if (binding.firstLeaf(new MemorySegment[] {global}) != sa.address()
        || binding.firstCall(new MemorySegment[] {global}) != sa.address())
      throw new AssertionError("global element lent in place");
    try (var shared = Arena.ofShared()) {
      var copy = shared.allocateFrom(ValueLayout.JAVA_BYTE, a);
      if (binding.firstLeaf(new MemorySegment[] {copy}) == copy.address()) throw new AssertionError("shared element copied");
      if (binding.digest(new MemorySegment[] {copy, sb}) != foldBatch(FNV_OFFSET, a, b)) throw new AssertionError("shared element bytes");
    }
    var automatic = Arena.ofAuto().allocateFrom(ValueLayout.JAVA_BYTE, b);
    if (binding.firstLeaf(new MemorySegment[] {automatic}) == automatic.address()) throw new AssertionError("auto element copied");
    if (binding.firstLeaf(new MemorySegment[0]) != 0) throw new AssertionError("empty first");
    // A constructor that takes a batch keeps what it read, never the buffers.
    var bundles = new java.util.ArrayList<MemorySegment>();
    NativeApiFFM.ResourceFactory<Action> keep = (address, destructor, threadConstraint) -> {
      bundles.add(address);
      return () -> destructor.destroy(address);
    };
    var bundle = NativeApiFFM.Bundle.create(binding, keep, new MemorySegment[] {sa, sb});
    var none = NativeApiFFM.Bundle.create(binding, keep, new MemorySegment[0]);
    if (binding.bundleValue(bundles.get(0)) != foldBatch(FNV_OFFSET, a, b)
        || binding.bundleValue(bundles.get(1)) != foldBatch(FNV_OFFSET))
      throw new AssertionError("batch constructor");
    rejects(IllegalStateException.class, () -> NativeApiFFM.Bundle.create(binding, keep, new MemorySegment[] {closed}));
    bundle.run(); none.run();
    // Each element is validated exactly as a single buffer is, before the call.
    rejects(NullPointerException.class, () -> binding.digest(new MemorySegment[] {sa, null}));
    rejects(NullPointerException.class, () -> binding.digest(null));
    rejects(IllegalArgumentException.class, () -> binding.digest(new MemorySegment[] {MemorySegment.ofArray(a)}));
    rejects(IllegalStateException.class, () -> binding.digest(new MemorySegment[] {sa, closed}));
    rejects(IllegalArgumentException.class, () -> binding.digest(new MemorySegment[] {MemorySegment.NULL.reinterpret(1)}));
    var failure = new AtomicReference<Throwable>();
    var thread = new Thread(() -> {
      try {
        rejects(WrongThreadException.class, () -> binding.digest(new MemorySegment[] {sa}));
      } catch (Throwable error) { failure.set(error); }
    });
    thread.start(); thread.join();
    if (failure.get() != null) throw failure.get();
  }

  private static void verifyConfinedResources(Path path) throws Throwable {
    // The library is shared: the resource's requiredThread must supply the confinement.
    try (var library = Arena.ofShared()) {
      var binding = new ConfinedApiFFM.LibraryBinding(path, library);
      var owner = ConfinedApiFFM.Ticket.create(binding, (address, destructor, thread) ->
          new ConfinedOwner<>(address, destructor::destroy, thread));
      if (owner.address().address() == 0 || binding.liveCount() != 1)
        throw new AssertionError("confined allocation");
      try {
        owner.rejectsOtherThread();
        if (binding.liveCount() != 1) throw new AssertionError("wrong-thread destruction");
      } finally { owner.close(); }
      owner.close();
      rejects(IllegalStateException.class, owner::address);
      if (binding.liveCount() != 0) throw new AssertionError("confined release");
      rejects(IllegalStateException.class, () -> ConfinedApiFFM.Ticket.create(binding,
          (address, destructor, thread) -> {
            if (thread != Thread.currentThread()) throw new AssertionError("adoption thread");
            throw new IllegalStateException("confined adoption failed");
          }));
      rejects(NullPointerException.class, () -> ConfinedApiFFM.Ticket.create(binding,
          (address, destructor, thread) -> null));
      if (binding.liveCount() != 0) throw new AssertionError("confined adoption leaked");
      binding.failNext();
      if (ConfinedApiFFM.Ticket.create(binding, (address, destructor, thread) -> {
        throw new AssertionError("must not adopt null");
      }) != null) throw new AssertionError("confined null allocation");
    }
  }
}
