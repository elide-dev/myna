import fixture.owned.NativeApi;
import fixture.owned.NativeApiOwnership;
import java.lang.ref.Cleaner;
import java.util.concurrent.atomic.AtomicLong;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.type.CCharPointer;

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
    final CCharPointer pointer = UnmanagedMemory.malloc(3);
    int leases;
    long size = 3;
    boolean failAcquire;
    boolean closed;
    boolean writableAllowed = true;

    public void acquire(boolean writable) {
      if (closed || failAcquire || (writable && !writableAllowed)) throw new IllegalStateException("unavailable");
      leases++;
    }

    public long address() {
      return pointer.rawValue();
    }

    public long byteSize() {
      return size;
    }

    public void release(boolean writable) {
      check(--leases == 0);
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

  public static void main(String[] args) throws Exception {
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
    System.out.println("Native Image consumer ownership and off-heap buffers passed");
  }
}
