// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
import fixture.owned.NativeApi;
import fixture.owned.NativeApiOwnership;
import fixture.owned.ConfinedApiOwnership;
import java.lang.ref.Cleaner;
import java.util.concurrent.atomic.AtomicLong;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CLongPointer;
import org.graalvm.word.WordFactory;

/** Consumer implementation: cleanup and off-heap access policy live outside generated code. */
public final class OwnershipMain {
  private static final Cleaner CLEANER = Cleaner.create();

  // Contains no reference to the Cleaner referent.
  static final class Allocation implements Runnable {
    final AtomicLong address;
    final NativeApiOwnership.NativeDestructor destructor;

    Allocation(long address, NativeApiOwnership.NativeDestructor destructor) {
      this.address = new AtomicLong(address);
      this.destructor = destructor;
    }

    public void run() {
      long owned = address.getAndSet(0);
      if (owned != 0) destructor.destroy(owned);
    }
  }

  static final class Owner {
    final Allocation allocation;
    final Cleaner.Cleanable registration;

    Owner(long address, NativeApiOwnership.NativeDestructor destructor, Thread requiredThread) {
      check(requiredThread == null);
      allocation = new Allocation(address, destructor);
      registration = CLEANER.register(this, allocation);
    }

    long address() {
      long address = allocation.address.get();
      if (address == 0) throw new IllegalStateException("released");
      return address;
    }
  }

  static final class Buffer implements NativeApiOwnership.NativeBuffer {
    /** Acquisitions and releases of labelled buffers, in the order the wrappers made them. */
    static final StringBuilder EVENTS = new StringBuilder();

    final CCharPointer pointer = UnmanagedMemory.malloc(3);
    final String label;
    int leases;
    long size = 3;
    boolean failAcquire;
    boolean closed;
    boolean writableAllowed = true;

    Buffer() {
      this(null);
    }

    Buffer(String label) {
      this.label = label;
    }

    public void acquire(boolean writable) {
      if (closed || failAcquire || (writable && !writableAllowed)) throw new IllegalStateException("unavailable");
      leases++;
      if (label != null) EVENTS.append('+').append(label).append(writable ? "w " : "r ");
    }

    public long address() {
      return pointer.rawValue();
    }

    public long byteSize() {
      return size;
    }

    public void release(boolean writable) {
      check(--leases == 0);
      if (label != null) EVENTS.append('-').append(label).append(writable ? "w " : "r ");
    }

    void dispose() {
      check(leases == 0);
      closed = true;
      UnmanagedMemory.free(pointer);
    }
  }

  static void check(boolean condition) {
    if (!condition) throw new AssertionError();
  }

  static void rejects(Runnable action) {
    try {
      action.run();
    } catch (IllegalStateException | IllegalArgumentException expected) {
      return;
    }
    throw new AssertionError("expected rejection");
  }

  public static void main(String[] args) throws Throwable {
    check(NativeApi.liveCount() == 0);
    Owner owner = NativeApiOwnership.Ticket.create(Owner::new);
    check(owner.address() != 0 && NativeApi.liveCount() == 1);
    // Deterministically exercise the same callback registered with GC; don't rely on GC timing.
    Thread cleanup = new Thread(owner.registration::clean);
    cleanup.start();
    cleanup.join();
    owner.registration.clean();
    check(NativeApi.liveCount() == 0);
    rejects(owner::address);
    rejects(
        () ->
            NativeApiOwnership.Ticket.create(
                (address, destructor, thread) -> {
                  throw new IllegalStateException("adoption failed");
                }));
    check(NativeApi.liveCount() == 0);
    try {
      NativeApiOwnership.Ticket.create((address, destructor, thread) -> null);
      throw new AssertionError();
    } catch (NullPointerException expected) {
      check(NativeApi.liveCount() == 0);
    }
    NativeApi.failNext();
    check(
        NativeApiOwnership.Ticket.create(
                (address, destructor, thread) -> {
                  throw new AssertionError("must not adopt null");
                })
            == null);
    check(NativeApi.liveCount() == 0);

    verifyConfinedResources();

    byte[] bytes = {1, 2, (byte) 255};
    check(NativeApiOwnership.checksum(bytes) == 258);
    NativeApiOwnership.increment(bytes);
    check(bytes[0] == 2 && bytes[1] == 3 && bytes[2] == 0);
    check(NativeApiOwnership.checksum(new byte[0]) == 0);
    NativeApiOwnership.increment(new byte[0]);

    Buffer buffer = new Buffer();
    buffer.pointer.write(0, (byte) 4);
    buffer.pointer.write(1, (byte) 5);
    buffer.pointer.write(2, (byte) 6);
    for (int i = 0; i < 1000; i++) check(NativeApiOwnership.checksum(buffer) == 15);
    NativeApiOwnership.increment(buffer);
    check(NativeApiOwnership.checksum(buffer) == 18 && buffer.leases == 0);
    buffer.writableAllowed = false;
    rejects(() -> NativeApiOwnership.increment(buffer));
    check(NativeApiOwnership.checksum(buffer) == 18 && buffer.leases == 0);
    buffer.writableAllowed = true;
    buffer.size = -1;
    rejects(() -> NativeApiOwnership.checksum(buffer));
    check(buffer.leases == 0);
    buffer.size = 3;
    buffer.failAcquire = true;
    rejects(() -> NativeApiOwnership.increment(buffer));
    check(buffer.leases == 0);
    buffer.failAcquire = false;
    buffer.dispose();
    rejects(() -> NativeApiOwnership.checksum(buffer));
    NativeApiOwnership.NativeBuffer empty =
        new NativeApiOwnership.NativeBuffer() {
          public void acquire(boolean writable) {}

          public long address() {
            return 0;
          }

          public long byteSize() {
            return 0;
          }

          public void release(boolean writable) {}
        };
    check(NativeApiOwnership.checksum(empty) == 0);
    NativeApiOwnership.increment(empty);
    verifySeveralBuffers(empty);
    System.out.println("Native Image consumer ownership and off-heap buffers passed");
  }

  /** Two buffers with lengths of their own beside a pointer that passes through unchanged. */
  private static void verifySeveralBuffers(NativeApiOwnership.NativeBuffer empty) {
    CLongPointer copied = UnmanagedMemory.malloc(8);
    // Lambdas cannot capture a word, so they rebuild the pointer from its address.
    long address = copied.rawValue();
    try {
      // Arrays: each non-empty one is pinned for the call; an empty one gets the stack dummy.
      byte[] source = {1, 2, 3, 4};
      byte[] target = new byte[3];
      copied.write(-1);
      check(NativeApiOwnership.transfer(source, target, copied) == 6 && copied.read() == 3);
      check(target[0] == 1 && target[1] == 2 && target[2] == 3);
      check(NativeApiOwnership.transfer(new byte[0], target, copied) == 0 && copied.read() == 0);
      check(NativeApiOwnership.transfer(source, new byte[0], copied) == 0 && copied.read() == 0);
      check(NativeApiOwnership.transfer(new byte[0], new byte[0], WordFactory.nullPointer()) == 0);
      check(target[0] == 1 && target[1] == 2 && target[2] == 3);
      byte[] wide = new byte[4];
      copied.write(-1);
      check(NativeApiOwnership.transfer(source, wide, WordFactory.nullPointer()) == 10 && copied.read() == -1);
      check(wide[3] == 4);

      // Off-heap: acquired in parameter order, released in reverse, each exactly once.
      Buffer input = new Buffer("in");
      Buffer output = new Buffer("out");
      for (int i = 0; i < 3; i++) input.pointer.write(i, (byte) (7 + i));
      check(NativeApiOwnership.transfer(input, output, copied) == 24 && copied.read() == 3);
      check(output.pointer.read(0) == 7 && output.pointer.read(2) == 9);
      check(Buffer.EVENTS.toString().equals("+inr +outw -outw -inr "));
      check(input.leases == 0 && output.leases == 0);
      for (int i = 0; i < 1000; i++) check(NativeApiOwnership.transfer(input, output, copied) == 24);
      check(input.leases == 0 && output.leases == 0);

      // A later buffer that cannot be acquired releases the earlier one and nothing else.
      Buffer.EVENTS.setLength(0);
      output.writableAllowed = false;
      rejects(() -> NativeApiOwnership.transfer(input, output, WordFactory.pointer(address)));
      check(Buffer.EVENTS.toString().equals("+inr -inr "));
      output.writableAllowed = true;
      // A later buffer that fails validation releases both, in reverse.
      Buffer.EVENTS.setLength(0);
      output.size = -1;
      rejects(() -> NativeApiOwnership.transfer(input, output, WordFactory.pointer(address)));
      check(Buffer.EVENTS.toString().equals("+inr +outw -outw -inr "));
      output.size = 3;
      // The first buffer failing means the second is never touched.
      Buffer.EVENTS.setLength(0);
      input.failAcquire = true;
      rejects(() -> NativeApiOwnership.transfer(input, output, WordFactory.pointer(address)));
      check(Buffer.EVENTS.length() == 0 && input.leases == 0 && output.leases == 0);
      input.failAcquire = false;
      input.size = -1;
      rejects(() -> NativeApiOwnership.transfer(input, output, WordFactory.pointer(address)));
      check(Buffer.EVENTS.toString().equals("+inr -inr "));
      input.size = 3;

      // Empty off-heap buffers at address zero, in either position, and a null pass-through.
      copied.write(-1);
      check(NativeApiOwnership.transfer(empty, output, copied) == 0 && copied.read() == 0);
      check(NativeApiOwnership.transfer(input, empty, copied) == 0 && copied.read() == 0);
      check(NativeApiOwnership.transfer(empty, empty, WordFactory.nullPointer()) == 0);
      check(output.pointer.read(0) == 7 && input.leases == 0 && output.leases == 0);
      input.dispose();
      output.dispose();
    } finally {
      UnmanagedMemory.free(copied);
    }
  }

  private static void verifyConfinedResources() throws Throwable {
    var owner = ConfinedApiOwnership.Ticket.create((address, destructor, thread) ->
        new ConfinedOwner<>(address, destructor::destroy, thread));
    check(owner.address() != 0 && NativeApi.liveCount() == 1);
    try {
      owner.rejectsOtherThread();
      check(NativeApi.liveCount() == 1);
    } finally { owner.close(); }
    owner.close();
    ConfinedOwner.rejects(IllegalStateException.class, owner::address);
    check(NativeApi.liveCount() == 0);
    ConfinedOwner.rejects(IllegalStateException.class, () -> ConfinedApiOwnership.Ticket.create(
        (address, destructor, thread) -> {
          check(thread == Thread.currentThread());
          throw new IllegalStateException("confined adoption failed");
        }));
    ConfinedOwner.rejects(NullPointerException.class, () -> ConfinedApiOwnership.Ticket.create(
        (address, destructor, thread) -> null));
    check(NativeApi.liveCount() == 0);
    NativeApi.failNext();
    check(ConfinedApiOwnership.Ticket.create((address, destructor, thread) -> {
      throw new AssertionError("must not adopt null");
    }) == null);
  }
}
