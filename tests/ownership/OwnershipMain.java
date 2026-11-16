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
    verifyNullableBuffers(empty);
    verifyConstructors(empty);
    verifyBatches(empty);
    verifyHeld();
    verifySurfaces();
    // A C string: NUL-terminated for the call; an embedded NUL is refused.
    check(NativeApiOwnership.titleLength("h\u00e9llo") == 6);
    rejects(() -> NativeApiOwnership.titleLength("ab\0cd"));
    System.out.println("Native Image consumer ownership and off-heap buffers passed");
  }

  /**
   * A create that keeps its input: the wrapper copies it into unmanaged memory the owner's
   * destructor frees after the destroy, so the caller's bytes may change at once.
   */
  private static void verifyHeld() {
    check(NativeApi.liveCount() == 0);
    byte[] bytes = {4, 5, 6};
    Owner doc = NativeApiOwnership.Doc.create(Owner::new, bytes);
    bytes[0] = 0;
    check(NativeApi.docSum(WordFactory.pointer(doc.address())) == 15);
    Buffer buffer = new Buffer("held");
    buffer.pointer.write(0, (byte) 1);
    buffer.pointer.write(1, (byte) 2);
    buffer.pointer.write(2, (byte) 3);
    Buffer.EVENTS.setLength(0);
    Owner fromBuffer = NativeApiOwnership.Doc.create(Owner::new, buffer);
    check(Buffer.EVENTS.toString().equals("+heldr -heldr ") && buffer.leases == 0);
    buffer.pointer.write(0, (byte) 9);
    check(NativeApi.docSum(WordFactory.pointer(fromBuffer.address())) == 6);
    buffer.dispose();
    check(NativeApi.liveCount() == 2);
    doc.registration.clean();
    fromBuffer.registration.clean();
    check(NativeApi.liveCount() == 0);
  }

  /**
   * Status plus out-pointer constructors: a success status adopts the handle from the slot the
   * wrapper passes on its own stack; any other status goes to onFailure, which can read the message
   * the caller lent, and nothing is adopted.
   */
  private static void verifySurfaces() {
    check(NativeApi.liveCount() == 0);
    NativeApiOwnership.Surface.Failure<Owner> refuse = status -> { throw new AssertionError("status " + status); };
    byte[] message = new byte[32];
    Owner surface = NativeApiOwnership.Surface.surfaceOpen(Owner::new, refuse, 7, message);
    check(NativeApi.surfaceWidth(WordFactory.pointer(surface.address())) == 7);
    Owner from = NativeApiOwnership.Surface.surfaceFrom(Owner::new, refuse, new byte[] {1, 2, 3}, (byte[]) null);
    check(NativeApi.surfaceWidth(WordFactory.pointer(from.address())) == 6);
    String why = NativeApiOwnership.Surface.surfaceOpen((address, destructor, thread) -> {
      throw new AssertionError("a failed create was adopted");
    }, status -> status + ": " + new String(message, 0, 14, java.nio.charset.StandardCharsets.US_ASCII), -1, message);
    check(why.equals("3: negative width"));
    // A C string beside a call buffer on one constructor.
    Owner titled = NativeApiOwnership.Surface.surfaceTitled(Owner::new, refuse, "abc", message);
    check(NativeApi.surfaceWidth(WordFactory.pointer(titled.address())) == 3);
    titled.registration.clean();
    rejects(() -> NativeApiOwnership.Surface.surfaceTitled(Owner::new, refuse, "ab\0c", message));
    NativeApi.failNext();
    check(NativeApiOwnership.Surface.surfaceFrom(Owner::new, status -> null, new byte[] {1}, (byte[]) null) == null);
    check(NativeApi.liveCount() == 2);
    surface.registration.clean();
    from.registration.clean();
    check(NativeApi.liveCount() == 0);
  }

  /**
   * Two constructors of one resource, named after their imports, each lending its buffer for the
   * call only: the resource keeps what it computed, never the buffer. The release accepts null.
   */
  private static void verifyConstructors(NativeApiOwnership.NativeBuffer empty) {
    check(NativeApi.liveCount() == 0);
    Buffer source = new Buffer("source");
    source.pointer.write(0, (byte) 1);
    source.pointer.write(1, (byte) 2);
    source.pointer.write(2, (byte) 3);
    Buffer.EVENTS.setLength(0);
    Owner parsed = NativeApiOwnership.Parsed.parse(Owner::new, source);
    check(Buffer.EVENTS.toString().equals("+sourcer -sourcer ") && source.leases == 0);
    source.pointer.write(0, (byte) 100);
    check(NativeApi.parsedValue(WordFactory.pointer(parsed.address())) == 6);
    Owner analyzed = NativeApiOwnership.Parsed.analyze(Owner::new, source, (byte) 2);
    check(NativeApi.parsedValue(WordFactory.pointer(analyzed.address())) == 210);
    byte[] bytes = {4, 5};
    Owner fromArray = NativeApiOwnership.Parsed.analyze(Owner::new, bytes, (byte) 3);
    check(NativeApi.parsedValue(WordFactory.pointer(fromArray.address())) == 27);
    Owner fromEmpty = NativeApiOwnership.Parsed.parse(Owner::new, empty);
    check(NativeApi.parsedValue(WordFactory.pointer(fromEmpty.address())) == 0);
    check(NativeApi.liveCount() == 4);
    // An absent source reaches native code as the null pointer, which parse reports as failure:
    // the consumer is not called.
    check(NativeApiOwnership.Parsed.parse(Owner::new, (byte[]) null) == null);
    check(NativeApiOwnership.Parsed.parse(Owner::new, (NativeApiOwnership.NativeBuffer) null) == null);
    // A buffer that cannot be acquired never reaches native code, and nothing is created.
    source.failAcquire = true;
    rejects(() -> NativeApiOwnership.Parsed.analyze(Owner::new, source, (byte) 1));
    source.failAcquire = false;
    check(source.leases == 0 && NativeApi.liveCount() == 4);
    NativeApi.failNext();
    check(NativeApiOwnership.Parsed.analyze((address, destructor, thread) -> {
      throw new AssertionError("null creation must not be adopted");
    }, bytes, (byte) 1) == null);
    for (Owner owner : new Owner[] {parsed, analyzed, fromArray, fromEmpty}) owner.registration.clean();
    // The release accepts null as a no-op; no owner ever passes it one.
    NativeApi.release(WordFactory.nullPointer());
    check(NativeApi.liveCount() == 0);
    source.dispose();
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

  /**
   * A nullable buffer: {@code null} is an absent buffer, which is never acquired, pinned or
   * released and reaches native code as the null pointer with length zero. The fixture reports
   * that as {@code ABSENT}; a buffer that is present, empty or not, never has the null address.
   */
  private static void verifyNullableBuffers(NativeApiOwnership.NativeBuffer empty) {
    final long absent = 1L << 32;
    // Arrays, an optional output beside a required input: only what is non-empty is pinned.
    byte[] data = {1, 2, 3};
    byte[] text = new byte[2];
    check(NativeApiOwnership.decode(data, text) == 3 && text[0] == 2 && text[1] == 3);
    check(NativeApiOwnership.decode(data, (byte[]) null) == (3 | absent));
    check(NativeApiOwnership.decode(data, new byte[0]) == 3);
    check(NativeApiOwnership.decode(new byte[0], (byte[]) null) == absent);
    for (int i = 0; i < 1000; i++) {
      byte[] optional = (i & 1) == 0 ? null : text;
      check(NativeApiOwnership.decode(data, optional) == ((i & 1) == 0 ? 3 | absent : 3));
    }
    // One optional array. A literal null fits both overloads, so it names the one it means.
    check(NativeApiOwnership.tally(data) == 6);
    check(NativeApiOwnership.tally(new byte[0]) == 0);
    check(NativeApiOwnership.tally((byte[]) null) == absent);

    // Off-heap: an absent buffer is neither acquired nor released, and the other one still is.
    Buffer input = new Buffer("in");
    Buffer output = new Buffer("out");
    for (int i = 0; i < 3; i++) input.pointer.write(i, (byte) (7 + i));
    Buffer.EVENTS.setLength(0);
    check(NativeApiOwnership.decode(input, output) == 3);
    check(output.pointer.read(0) == 8 && output.pointer.read(2) == 10);
    check(Buffer.EVENTS.toString().equals("+inr +outw -outw -inr "));
    Buffer.EVENTS.setLength(0);
    check(NativeApiOwnership.decode(input, (NativeApiOwnership.NativeBuffer) null) == (3 | absent));
    check(Buffer.EVENTS.toString().equals("+inr -inr "));
    check(input.leases == 0 && output.leases == 0);
    // Present and empty at address zero is not absent: it gets the stack dummy.
    check(NativeApiOwnership.decode(input, empty) == 3);
    check(NativeApiOwnership.decode(empty, (NativeApiOwnership.NativeBuffer) null) == absent);
    for (int i = 0; i < 1000; i++) {
      NativeApiOwnership.NativeBuffer optional = (i & 1) == 0 ? null : output;
      check(NativeApiOwnership.decode(input, optional) == ((i & 1) == 0 ? 3 | absent : 3));
    }
    check(input.leases == 0 && output.leases == 0);
    // A buffer that is present keeps every rule: it must be acquirable and have a valid extent.
    Buffer.EVENTS.setLength(0);
    output.writableAllowed = false;
    rejects(() -> NativeApiOwnership.decode(input, output));
    check(Buffer.EVENTS.toString().equals("+inr -inr "));
    output.writableAllowed = true;
    Buffer.EVENTS.setLength(0);
    output.size = -1;
    rejects(() -> NativeApiOwnership.decode(input, output));
    check(Buffer.EVENTS.toString().equals("+inr +outw -outw -inr "));
    output.size = 3;
    // The buffer that is not nullable still is not.
    try {
      NativeApiOwnership.decode((NativeApiOwnership.NativeBuffer) null, output);
      throw new AssertionError("a null buffer was accepted where none may be absent");
    } catch (NullPointerException expected) {
      check(output.leases == 0);
    }
    // One optional off-heap buffer.
    Buffer.EVENTS.setLength(0);
    check(NativeApiOwnership.tally(input) == 24);
    check(Buffer.EVENTS.toString().equals("+inr -inr "));
    Buffer.EVENTS.setLength(0);
    check(NativeApiOwnership.tally((NativeApiOwnership.NativeBuffer) null) == absent);
    check(Buffer.EVENTS.length() == 0);
    check(NativeApiOwnership.tally(empty) == 0);
    input.dispose();
    output.dispose();
  }

  // The fixture's batch digest (tests/ownership/native.rs): the count, then each element's length
  // and bytes, or NULL_ELEMENT for an absent one; the empty batch folds in ABSENT.
  static final long FNV_OFFSET = 0xcbf29ce484222325L;

  static long mix(long hash, long value) {
    return (hash ^ value) * 0x100000001b3L;
  }

  static long fold(long hash, byte[] bytes) {
    hash = mix(hash, bytes.length);
    for (byte b : bytes) hash = mix(hash, b & 0xff);
    return hash;
  }

  static long foldBatch(long hash, byte[]... buffers) {
    if (buffers.length == 0) return mix(hash, 1L << 32);
    hash = mix(hash, buffers.length);
    for (byte[] buffer : buffers) hash = buffer == null ? mix(hash, -2L) : fold(hash, buffer);
    return hash;
  }

  /**
   * Batches: three buffers of different sizes, one empty, lent off-heap and from arrays; the empty
   * batch; more than eight elements; two batches beside a single buffer in one call; and a failure
   * that releases exactly the elements already acquired.
   */
  private static void verifyBatches(NativeApiOwnership.NativeBuffer empty) {
    byte[] a = {1, 2, 3}, b = {(byte) 255}, none = {};
    check(NativeApiOwnership.digest(new byte[][] {a, none, b}) == foldBatch(FNV_OFFSET, a, none, b));
    check(NativeApiOwnership.digest(new byte[0][]) == foldBatch(FNV_OFFSET));
    check(NativeApiOwnership.digest(new NativeApiOwnership.NativeBuffer[0]) == foldBatch(FNV_OFFSET));
    byte[][] many = new byte[20][];
    for (int i = 0; i < many.length; i++) {
      many[i] = new byte[i];
      for (int j = 0; j < i; j++) many[i][j] = (byte) (i * 7 + j);
    }
    check(NativeApiOwnership.digest(many) == foldBatch(FNV_OFFSET, many));
    Buffer first = new Buffer("a");
    Buffer second = new Buffer("b");
    for (int i = 0; i < 3; i++) first.pointer.write(i, a[i]);
    second.pointer.write(0, b[0]);
    second.size = 1;
    Buffer.EVENTS.setLength(0);
    check(NativeApiOwnership.digest(new NativeApiOwnership.NativeBuffer[] {first, empty, second})
        == foldBatch(FNV_OFFSET, a, none, b));
    check(Buffer.EVENTS.toString().equals("+ar +br -br -ar "));
    var large = new NativeApiOwnership.NativeBuffer[12];
    byte[][] expected = new byte[12][];
    for (int i = 0; i < large.length; i++) {
      // A buffer is acquired once per call; the others are present and empty.
      large[i] = i == 9 ? first : empty;
      expected[i] = i == 9 ? a : none;
    }
    check(NativeApiOwnership.digest(large) == foldBatch(FNV_OFFSET, expected));
    check(first.leases == 0);
    // A failure releases what was acquired, in reverse, and the call never reaches native code.
    Buffer.EVENTS.setLength(0);
    second.failAcquire = true;
    rejects(() -> NativeApiOwnership.digest(new NativeApiOwnership.NativeBuffer[] {first, second}));
    check(Buffer.EVENTS.toString().equals("+ar -ar ") && first.leases == 0 && second.leases == 0);
    second.failAcquire = false;
    Buffer.EVENTS.setLength(0);
    second.size = -1;
    rejects(() -> NativeApiOwnership.digest(new NativeApiOwnership.NativeBuffer[] {first, second}));
    check(Buffer.EVENTS.toString().equals("+ar +br -br -ar "));
    second.size = 1;
    try {
      NativeApiOwnership.digest(new NativeApiOwnership.NativeBuffer[] {first, null});
      throw new AssertionError("an absent element was accepted where none may be");
    } catch (NullPointerException expected2) {
      check(first.leases == 0);
    }
    try {
      NativeApiOwnership.digest(new byte[][] {a, null});
      throw new AssertionError("an absent element was accepted where none may be");
    } catch (NullPointerException expected2) {
      // Pinned elements were unpinned.
    }
    // Two batches beside a single buffer; the second batch's elements may be absent.
    byte[] key = {7, 8};
    long two = foldBatch(foldBatch(fold(FNV_OFFSET, key), a, b), b, null, none);
    check(NativeApiOwnership.merge(key, new byte[][] {a, b}, new byte[][] {b, null, none}) == two);
    Buffer keys = new Buffer("k");
    keys.pointer.write(0, (byte) 7);
    keys.pointer.write(1, (byte) 8);
    keys.size = 2;
    Buffer third = new Buffer("c");
    third.pointer.write(0, b[0]);
    third.size = 1;
    Buffer.EVENTS.setLength(0);
    check(NativeApiOwnership.merge(keys, new NativeApiOwnership.NativeBuffer[] {first, second},
        new NativeApiOwnership.NativeBuffer[] {third, null, empty}) == two);
    check(Buffer.EVENTS.toString().equals("+kr +ar +br +cr -cr -br -ar -kr "));
    third.dispose();
    long nothing = foldBatch(foldBatch(fold(FNV_OFFSET, key)));
    check(NativeApiOwnership.merge(key, new byte[0][], new byte[0][]) == nothing);
    // A constructor that takes a batch keeps what it read, never the buffers.
    check(NativeApi.liveCount() == 0);
    Buffer.EVENTS.setLength(0);
    Owner bundle = NativeApiOwnership.Bundle.create(Owner::new, new NativeApiOwnership.NativeBuffer[] {first, second});
    check(Buffer.EVENTS.toString().equals("+ar +br -br -ar "));
    check(NativeApi.bundleValue(WordFactory.pointer(bundle.address())) == foldBatch(FNV_OFFSET, a, b));
    Owner fromArrays = NativeApiOwnership.Bundle.create(Owner::new, new byte[][] {a, none, b});
    check(NativeApi.bundleValue(WordFactory.pointer(fromArrays.address())) == foldBatch(FNV_OFFSET, a, none, b));
    Owner empty0 = NativeApiOwnership.Bundle.create(Owner::new, new byte[0][]);
    check(NativeApi.bundleValue(WordFactory.pointer(empty0.address())) == foldBatch(FNV_OFFSET));
    check(NativeApi.liveCount() == 3);
    for (Owner owner : new Owner[] {bundle, fromArrays, empty0}) owner.registration.clean();
    check(NativeApi.liveCount() == 0);
    // The caller's array is read once: replacing an element during the call cannot make the
    // wrapper release a buffer it never acquired (second's release would fail its lease check).
    var batch = new NativeApiOwnership.NativeBuffer[1];
    batch[0] =
        new NativeApiOwnership.NativeBuffer() {
          public void acquire(boolean writable) {
            first.acquire(writable);
            batch[0] = second;
          }

          public long address() {
            return first.address();
          }

          public long byteSize() {
            return first.byteSize();
          }

          public void release(boolean writable) {
            first.release(writable);
          }
        };
    check(NativeApiOwnership.digest(batch) == foldBatch(FNV_OFFSET, a));
    check(first.leases == 0 && second.leases == 0 && batch[0] == second);
    first.dispose();
    second.dispose();
    keys.dispose();
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
