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
        var failure = new AtomicReference<Throwable>();
        var thread = new Thread(() -> {
          try {
            rejects(WrongThreadException.class, () -> binding.checksum(bytes));
            rejects(WrongThreadException.class, () -> binding.transfer(bytes, target, copied));
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
      }
      rejects(IllegalStateException.class, () -> binding.liveCount());
    }
    verifyConfinedResources(Path.of(args[0]));
    System.out.println("Explicit FFM library lifetime and buffer borrowing passed");
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
