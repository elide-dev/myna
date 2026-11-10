// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
import fixture.owned.NativeApiJNI;
import java.nio.ByteBuffer;
import java.lang.foreign.Arena;
import java.util.concurrent.atomic.AtomicReference;

public final class JniMain {
  private static native boolean nativeErrors(ByteBuffer heap, ByteBuffer direct);
  public static void main(String[] args) throws InterruptedException {
    if (NativeApiJNI.liveCount() != 0) throw new AssertionError("initial allocations");
    long ticket = NativeApiJNI.ticketCreate();
    if (ticket == 0 || NativeApiJNI.liveCount() != 1) throw new AssertionError("allocation");
    NativeApiJNI.ticketDestroy(ticket);
    NativeApiJNI.failNext();
    if (NativeApiJNI.ticketCreate() != 0 || NativeApiJNI.liveCount() != 0)
      throw new AssertionError("cleanup / null sentinel");
    var bytes = ByteBuffer.allocateDirect(5);
    bytes.put(new byte[] {99, 1, 2, 3, 99}).position(1).limit(4);
    if (NativeApiJNI.checksum(bytes.asReadOnlyBuffer()) != 6) throw new AssertionError("checksum");
    NativeApiJNI.increment(bytes);
    if (NativeApiJNI.checksum(bytes) != 9 || bytes.position() != 1) throw new AssertionError("borrow");
    bytes.position(4);
    if (NativeApiJNI.checksum(bytes) != 0) throw new AssertionError("empty");
    NativeApiJNI.increment(bytes);
    try {
      NativeApiJNI.increment(bytes.asReadOnlyBuffer());
      throw new AssertionError("accepted read-only buffer");
    } catch (IllegalArgumentException expected) { }
    if (!nativeErrors(ByteBuffer.allocate(1), ByteBuffer.allocateDirect(1))) throw new AssertionError("JNI pending exceptions");
    try {
      NativeApiJNI.checksum(ByteBuffer.allocate(1));
      throw new AssertionError("accepted heap buffer");
    } catch (IllegalArgumentException expected) { }
    verifyArenaBuffers();
    verifySeveralBuffers();
    System.out.println("Static JNI scalar, ownership and direct buffer calls passed");
  }

  /** Two direct buffers with lengths of their own beside a raw address that passes through. */
  private static void verifySeveralBuffers() {
    try (var arena = Arena.ofConfined()) {
      var copied = arena.allocate(java.lang.foreign.ValueLayout.JAVA_LONG);
      var source = ByteBuffer.allocateDirect(6);
      source.put(new byte[] {99, 1, 2, 3, 4, 99}).position(1).limit(5);
      var target = ByteBuffer.allocateDirect(4);
      target.position(1);
      // Only the remaining ranges are lent, and neither position moves.
      if (NativeApiJNI.transfer(source.asReadOnlyBuffer(), target, copied.address()) != 6
          || copied.get(java.lang.foreign.ValueLayout.JAVA_LONG, 0) != 3)
        throw new AssertionError("transfer");
      if (target.get(0) != 0 || target.get(1) != 1 || target.get(3) != 3) throw new AssertionError("transferred bytes");
      if (source.position() != 1 || target.position() != 1) throw new AssertionError("positions");
      if (NativeApiJNI.transfer(source, target, 0) != 6) throw new AssertionError("null pass-through");
      // Either buffer may be empty.
      copied.set(java.lang.foreign.ValueLayout.JAVA_LONG, 0, -1);
      target.position(4);
      if (NativeApiJNI.transfer(source, target, copied.address()) != 0
          || copied.get(java.lang.foreign.ValueLayout.JAVA_LONG, 0) != 0)
        throw new AssertionError("empty output");
      target.position(1);
      source.position(5);
      if (NativeApiJNI.transfer(source, target, copied.address()) != 0) throw new AssertionError("empty input");
      source.position(1);
      // Every buffer is checked before the native method is entered.
      rejects(IllegalArgumentException.class, () -> NativeApiJNI.transfer(source, target.asReadOnlyBuffer(), copied.address()));
      rejects(IllegalArgumentException.class, () -> NativeApiJNI.transfer(ByteBuffer.allocate(1), target, copied.address()));
      rejects(IllegalArgumentException.class, () -> NativeApiJNI.transfer(source, ByteBuffer.allocate(1), copied.address()));
      ByteBuffer closed;
      try (var inner = Arena.ofConfined()) { closed = inner.allocate(1).asByteBuffer(); }
      rejects(IllegalStateException.class, () -> NativeApiJNI.transfer(closed, target, copied.address()));
      rejects(IllegalStateException.class, () -> NativeApiJNI.transfer(source, closed, copied.address()));
      if (copied.get(java.lang.foreign.ValueLayout.JAVA_LONG, 0) != 0 || target.get(1) != 1)
        throw new AssertionError("a rejected transfer reached native code");
    }
  }

  private static void rejects(Class<? extends Throwable> expected, Runnable action) {
    try { action.run(); }
    catch (Throwable failure) {
      if (expected.isInstance(failure)) return;
      throw new AssertionError("unexpected buffer exception", failure);
    }
    throw new AssertionError("expected " + expected.getSimpleName());
  }

  private static void verifyArenaBuffers() throws InterruptedException {
    for (boolean shared : new boolean[] {false, true}) {
      ByteBuffer closed;
      try (var arena = shared ? Arena.ofShared() : Arena.ofConfined()) {
        closed = arena.allocate(1).asByteBuffer();
        closed.put(0, (byte) 42);
        if (NativeApiJNI.checksum(closed) != 42) throw new AssertionError("live arena buffer");
        if (!shared) {
          var failure = new AtomicReference<Throwable>();
          var thread = new Thread(() -> {
            try {
              rejects(WrongThreadException.class, () -> NativeApiJNI.checksum(closed));
              rejects(WrongThreadException.class, () -> NativeApiJNI.increment(closed));
            } catch (Throwable error) { failure.set(error); }
          });
          thread.start(); thread.join();
          if (failure.get() != null) throw new AssertionError("confined JNI buffer", failure.get());
        }
      }
      rejects(IllegalStateException.class, () -> NativeApiJNI.checksum(closed));
      rejects(IllegalStateException.class, () -> NativeApiJNI.increment(closed));
      closed.position(closed.limit());
      rejects(IllegalStateException.class, () -> NativeApiJNI.checksum(closed));
    }
  }
}
