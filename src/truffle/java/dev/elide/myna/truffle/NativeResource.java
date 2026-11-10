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
import java.util.concurrent.atomic.AtomicInteger;

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

  /**
   * Release state kept apart from the owner so a cleaner does not retain the referent.
   *
   * <p>{@link #holds} is {@link #RELEASED} once the allocation is freed. Until then its low bits
   * count the open resources created from this one, which keep it from closing, and {@link
   * #PENDING} records that a cleaner wanted to free it while it was still held: the last dependent
   * to go frees it then.
   */
  private static final class State implements Runnable {
    static final int RELEASED = -1;
    static final int PENDING = 1 << 30;
    private static final State[] NONE = {};

    final long address;
    final Destructor destructor;
    final State[] parents;
    final AtomicInteger holds = new AtomicInteger();

    /** What must stay reachable while the allocation lives; dropped when it is freed. */
    volatile Object kept;

    State(long address, Destructor destructor, State[] parents, Object kept) {
      this.address = address;
      this.destructor = destructor;
      this.parents = parents;
      this.kept = kept;
    }

    boolean released() {
      return holds.get() == RELEASED;
    }

    /** Keeps this resource open for a dependent; fails once it is released or abandoned. */
    boolean hold() {
      while (true) {
        int value = holds.get();
        if (value < 0 || (value & PENDING) != 0) return false;
        if (holds.compareAndSet(value, value + 1)) return true;
      }
    }

    /** Ends one dependent's hold, freeing a resource whose cleaner was waiting for it. */
    void unhold() {
      while (true) {
        int value = holds.get();
        if (value == (PENDING | 1)) {
          if (holds.compareAndSet(value, RELEASED)) {
            destroy();
            return;
          }
        } else if (holds.compareAndSet(value, value - 1)) {
          return;
        }
      }
    }

    /** An explicit close: frees unless held, and returns how many dependents hold it open. */
    int close() {
      while (true) {
        int value = holds.get();
        if (value == RELEASED) return 0;
        int dependents = value & ~PENDING;
        if (dependents > 0) return dependents;
        if (holds.compareAndSet(value, RELEASED)) {
          destroy();
          return 0;
        }
      }
    }

    /** The cleaner's release: frees now, or leaves it to the last dependent. */
    @Override
    public void run() {
      while (true) {
        int value = holds.get();
        if (value == RELEASED) return;
        if ((value & ~PENDING) > 0) {
          if (holds.compareAndSet(value, value | PENDING)) return;
        } else if (holds.compareAndSet(value, RELEASED)) {
          destroy();
          return;
        }
      }
    }

    private void destroy() {
      try {
        destructor.destroy(address);
      } finally {
        kept = null;
        for (State parent : parents) parent.unhold();
      }
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
    this(type, address, destructor, requiredThread, null);
  }

  /**
   * A resource created from other resources, which it keeps open until it is closed itself.
   *
   * @param hold what {@link #hold} returned for the resources this one was created from; the new
   *     resource takes it over and gives it up when it is released
   */
  public NativeResource(
      String type, long address, Destructor destructor, Thread requiredThread, Object hold) {
    this(type, address, destructor, requiredThread, hold, null);
  }

  /**
   * A resource that lives in memory the runtime owns: a struct initialized in place.
   *
   * @param kept what must stay reachable until the resource is released: the {@link NativeStruct}
   *     the resource lives in
   */
  public NativeResource(
      String type,
      long address,
      Destructor destructor,
      Thread requiredThread,
      Object hold,
      Object kept) {
    if (address == 0) throw new IllegalArgumentException("null " + type);
    if (destructor == null) throw new IllegalArgumentException("missing destructor");
    this.type = type;
    this.state = new State(address, destructor, hold == null ? State.NONE : (State[]) hold, kept);
    this.requiredThread = requiredThread;
  }

  /**
   * Keeps every open resource among {@code candidates} from closing, for a native call that may
   * retain their handles in what it creates. Values that are not resources are ignored. The result
   * goes to the created resource's constructor, or to {@link #release} when nothing was created.
   *
   * @throws NativeCellException if one of the resources is already closed; nothing stays held
   */
  @TruffleBoundary
  public static Object hold(Object... candidates) {
    int count = 0;
    for (Object candidate : candidates) if (candidate instanceof NativeResource) count++;
    State[] held = new State[count];
    int next = 0;
    for (Object candidate : candidates) {
      if (!(candidate instanceof NativeResource resource)) continue;
      if (!resource.state.hold()) {
        for (int i = 0; i < next; i++) held[i].unhold();
        throw NativeCellException.of(null, resource.type, " is closed");
      }
      held[next++] = resource.state;
    }
    return held;
  }

  /** Gives up a {@link #hold} that no created resource took over. */
  @TruffleBoundary
  public static void release(Object hold) {
    for (State state : (State[]) hold) state.unhold();
  }

  public String type() {
    return type;
  }

  public Thread requiredThread() {
    return requiredThread;
  }

  public boolean isClosed() {
    return state.released();
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

  /**
   * Releases the allocation exactly once. A second close is a no-op.
   *
   * @throws NativeCellException if resources created from this one are still open: the native side
   *     may still use it through them, so they are closed first
   */
  public void close() {
    if (!accessible()) throw wrongThread();
    int dependents = state.close();
    if (dependents > 0) throw held(dependents);
  }

  @TruffleBoundary
  private NativeCellException held(int dependents) {
    return NativeCellException.of(
        null,
        type,
        " is held open by ",
        dependents,
        " resource(s) created from it; close them first");
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
