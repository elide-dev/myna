// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package dev.elide.myna.truffle;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import java.lang.ref.Cleaner;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A guest-visible owner of one unique native allocation declared with Myna's {@code resource}
 * contract. It is an interop pointer while open, closes exactly once (explicitly, through its
 * {@code close} member, or through an optional {@link Cleaner}), and refuses access from any thread
 * other than its required thread when the resource is confined.
 *
 * <p>Release never runs while a borrow of the resource is active on another thread only if the
 * consumer keeps to the confinement rule; shared resources rely on the native implementation's own
 * thread safety, exactly as the Native Image ownership API does.
 */
@ExportLibrary(InteropLibrary.class)
public final class NativeResource implements TruffleObject {
  /** Frees one allocation; called at most once per resource, never with the null address. */
  @FunctionalInterface
  public interface Destructor {
    void destroy(long address);
  }

  /** Release state kept apart from the owner so a cleaner does not retain the referent. */
  private static final class State implements Runnable {
    final long address;
    final Destructor destructor;
    final AtomicBoolean released = new AtomicBoolean();

    State(long address, Destructor destructor) {
      this.address = address;
      this.destructor = destructor;
    }

    @Override
    public void run() {
      if (released.compareAndSet(false, true)) destructor.destroy(address);
    }
  }

  private static final String CLOSE = "close";

  private final String type;
  private final State state;
  private final Thread requiredThread;

  /**
   * @param type the resource's declared name, for diagnostics
   * @param address the live allocation; zero is rejected
   * @param destructor the seam's destroy function
   * @param requiredThread the only thread that may access or close a confined resource; null for a
   *     shared resource
   */
  public NativeResource(String type, long address, Destructor destructor, Thread requiredThread) {
    if (address == 0) throw new IllegalArgumentException("null " + type);
    if (destructor == null) throw new IllegalArgumentException("missing destructor");
    this.type = type;
    this.state = new State(address, destructor);
    this.requiredThread = requiredThread;
  }

  public String type() {
    return type;
  }

  public Thread requiredThread() {
    return requiredThread;
  }

  public boolean isClosed() {
    return state.released.get();
  }

  private boolean accessible() {
    return requiredThread == null || requiredThread == Thread.currentThread();
  }

  /** The live address; fails after close or from a foreign thread. */
  public long address() {
    if (!accessible()) throw wrongThread();
    if (isClosed()) {
      CompilerDirectives.transferToInterpreter();
      throw NativeCellException.of(null, type, " is closed");
    }
    return state.address;
  }

  /** Releases the allocation exactly once. A second close is a no-op. */
  public void close() {
    if (!accessible()) throw wrongThread();
    state.run();
  }

  /**
   * Registers release with a cleaner, for shared resources only: a cleaner thread cannot honor a
   * confined resource's thread. Explicit close still wins; the cleaner then finds nothing to do.
   */
  public Cleaner.Cleanable registerCleanup(Cleaner cleaner) {
    if (requiredThread != null)
      throw new IllegalStateException("confined " + type + " cannot be released by a cleaner");
    return cleaner.register(this, state);
  }

  @TruffleBoundary
  private NativeCellException wrongThread() {
    return NativeCellException.of(
        null,
        type,
        " is confined to ",
        requiredThread.getName(),
        "; accessed from ",
        Thread.currentThread().getName());
  }

  @ExportMessage
  boolean isPointer() {
    return accessible() && !isClosed();
  }

  @ExportMessage
  long asPointer() throws UnsupportedMessageException {
    if (!isPointer()) throw UnsupportedMessageException.create();
    return state.address;
  }

  @ExportMessage
  boolean hasMembers() {
    return true;
  }

  @ExportMessage
  Object getMembers(@SuppressWarnings("unused") boolean includeInternal) {
    return Members.INSTANCE;
  }

  @ExportMessage
  boolean isMemberInvocable(String member) {
    return CLOSE.equals(member);
  }

  @ExportMessage
  Object invokeMember(String member, Object[] arguments) throws UnknownIdentifierException {
    if (!CLOSE.equals(member)) throw UnknownIdentifierException.create(member);
    if (arguments.length != 0) throw NativeCellException.arity(CLOSE, 0, arguments.length, null);
    close();
    return NativeVoid.INSTANCE;
  }

  @Override
  public String toString() {
    return type + (isClosed() ? "(closed)" : "(0x" + Long.toHexString(state.address) + ")");
  }

  /** The member names of every resource. */
  @ExportLibrary(InteropLibrary.class)
  static final class Members implements TruffleObject {
    static final Members INSTANCE = new Members();

    @ExportMessage
    boolean hasArrayElements() {
      return true;
    }

    @ExportMessage
    long getArraySize() {
      return 1;
    }

    @ExportMessage
    boolean isArrayElementReadable(long index) {
      return index == 0;
    }

    @ExportMessage
    Object readArrayElement(long index) throws InvalidArrayIndexException {
      if (index != 0) throw InvalidArrayIndexException.create(index);
      return CLOSE;
    }
  }
}
