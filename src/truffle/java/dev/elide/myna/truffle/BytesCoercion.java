// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package dev.elide.myna.truffle;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.HostCompilerDirectives;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.strings.AbstractTruffleString;
import java.lang.foreign.MemorySegment;

/**
 * Coerces a guest value to borrowed bytes for a byte-pointer parameter: a {@code ptr<u8>}, {@code
 * ptr<i8>} or {@code ptr<char>} import parameter, or the buffer of a {@code borrow} declaration.
 * Strings are accepted only for read access, since the native side may not write into them: Java
 * strings, guest strings that are {@code TruffleString}s (read through {@link GuestText}, with no
 * Java string made of them), and any other interop string through {@code asString}.
 */
public final class BytesCoercion extends Coercion {
  /** The kinds of value this call site has lent; only their paths are compiled. */
  private static final int NATIVE = 1;

  private static final int HEAP = 2;
  private static final int TEXT = 4;
  private static final int POINTER = 8;
  private static final int OTHER = 16;
  private static final int GUEST_TEXT = 32;

  /** Not a kind: whether this call site has lent native memory whose scope a call must check. */
  private static final int SCOPED = 64;

  /** Not a kind: whether this call site has lent an empty native buffer. */
  private static final int EMPTY = 128;

  private final boolean nullable;
  private final boolean writable;
  private final boolean strings;
  private final boolean buffer;
  private final boolean terminated;
  @CompilationFinal private int seen;

  /** {@link Borrow#pinThreshold()}, read on the first heap array; negative until then. */
  @CompilationFinal private int pinThreshold = -1;

  /** Inserted on the first guest string, like the interop library on the first interop value. */
  @Child private GuestText guestText;

  /**
   * @param nullable whether interop {@code null} lends the null address
   * @param writable whether the native side may write, which forbids strings and read-only buffers
   *     and copies heap bytes back after the call
   * @param buffer whether the cell derives the byte count from the borrowed bytes (a {@code borrow}
   *     declaration): then a pointer must carry a size, and strings are not NUL-terminated. C
   *     string parameters get a terminating NUL and accept bare pointers.
   */
  public BytesCoercion(
      String function, String parameter, boolean nullable, boolean writable, boolean buffer) {
    this(function, parameter, nullable, writable, buffer, !buffer);
  }

  /**
   * @param terminated whether copies get a terminating NUL, as a C string parameter needs. A
   *     parameter declared {@code truffle_buffer} is neither a borrow nor terminated: it accepts
   *     bare pointers, and heap arrays are pinned when large instead of copied with a terminator.
   */
  public BytesCoercion(
      String function,
      String parameter,
      boolean nullable,
      boolean writable,
      boolean buffer,
      boolean terminated) {
    super(function, parameter);
    if (buffer && terminated) throw new IllegalArgumentException("a borrow is not terminated");
    this.nullable = nullable;
    this.writable = writable;
    this.strings = !writable;
    this.buffer = buffer;
    this.terminated = terminated;
  }

  /**
   * Lends {@code value} for one call. The borrow is created here and filled in place, and nothing
   * on this path hands it to code that partial evaluation does not inline, so a compiled cell that
   * releases it keeps it out of the heap.
   *
   * <p>A kind this call site has not seen yet is recorded and the dispatch repeated with the same
   * borrow. In runtime-compiled code that branch is a deoptimization. A host JIT (cells called from
   * Java on HotSpot) compiles it, because this method is shared by every call site and each new
   * site takes it once; were it to produce a second borrow, or pass this one on, the JIT could no
   * longer scalar-replace the borrow on any path.
   */
  public Borrow execute(Object value) {
    Borrow borrow = new Borrow();
    int seen = this.seen;
    while (true) {
      if ((seen & NATIVE) != 0 && value instanceof NativeBytes bytes) {
        if (writable && !bytes.isWritable()) throw mismatch("writable buffer", value);
        long size = bytes.size();
        borrow.lend(size == 0 ? emptyAddress() : bytes.address(), size);
        return borrow;
      }
      if ((seen & HEAP) != 0 && value instanceof HeapBytes bytes) {
        if (writable && !bytes.isWritable()) throw mismatch("writable buffer", value);
        borrow.lendArray(bytes.array(), writable, terminated, pinThreshold());
        return borrow;
      }
      if ((seen & GUEST_TEXT) != 0 && value instanceof AbstractTruffleString text) {
        Object pushed = pushGuestText(text);
        if (pushed instanceof Borrow.Scratch scratch) {
          borrow.lendPushed(scratch);
          return borrow;
        }
        // Too long for the scratch stack: lent as its Java string, by the same code below.
        value = pushed;
        seen |= TEXT;
      }
      if ((seen & TEXT) != 0 && value instanceof String text) {
        borrow.lendString(text, terminated);
        return borrow;
      }
      if ((seen & POINTER) != 0 && value instanceof NativePointer pointer) {
        pointer(pointer.address(), value, borrow);
        return borrow;
      }
      if ((seen & OTHER) != 0 && kind(value) == OTHER) {
        slow(value, borrow);
        return borrow;
      }
      seen = learn(value);
    }
  }

  /**
   * Records the kind of {@code value} and returns the kinds seen so far. In runtime-compiled code
   * this deoptimizes; in code compiled ahead of time it is an ordinary call, kept out of {@link
   * #execute} so that the host compiler still inlines {@code execute}, which keeps the borrow out
   * of the heap there too.
   */
  @HostCompilerDirectives.InliningCutoff
  private int learn(Object value) {
    CompilerDirectives.transferToInterpreterAndInvalidate();
    return this.seen |= kind(value);
  }

  /**
   * The segment whose scope a call must check for the memory {@link #execute} lent in place for
   * {@code value}, or {@code null} when there is nothing to check: anything but native memory lent
   * in place (a copy, a pin, a bare pointer, the empty buffer), or native memory of the global
   * scope. Only FFM cells ask, to pass the guest's own segment to their downcall, which then checks
   * its confinement and liveness and keeps its arena open for the call. A {@link NativeBytes} views
   * native memory only, so the segment is never a heap segment, which a critical downcall would
   * refuse.
   *
   * <p>Until this call site has lent a segment with a scope to check, compiled code tests for one
   * and returns {@code null}; the first such segment deoptimizes and is checked from then on.
   */
  public MemorySegment segment(Object value) {
    if ((seen & NATIVE) == 0 || !(value instanceof NativeBytes bytes)) return null;
    MemorySegment scoped = bytes.scoped;
    if ((seen & SCOPED) == 0) {
      if (scoped == null) return null;
      learnScoped();
    }
    return scoped;
  }

  /** Records that this call site lends segments with a scope to check; see {@link #learn}. */
  @HostCompilerDirectives.InliningCutoff
  private void learnScoped() {
    CompilerDirectives.transferToInterpreterAndInvalidate();
    seen |= SCOPED;
  }

  /**
   * The dummy address an empty buffer lends. Fetching it is a call, and a call on any path makes
   * compiled code keep every value live across it in memory, a loop's own result included, so a
   * call site that has not lent an empty buffer deoptimizes on its first one instead.
   */
  @HostCompilerDirectives.InliningCutoff
  private long emptyAddress() {
    if ((seen & EMPTY) == 0) {
      CompilerDirectives.transferToInterpreterAndInvalidate();
      seen |= EMPTY;
    }
    return Borrow.emptyAddress();
  }

  private int kind(Object value) {
    if (value instanceof NativeBytes) return NATIVE;
    if (value instanceof HeapBytes) return HEAP;
    if (strings && value instanceof String) return TEXT;
    if (strings && value instanceof AbstractTruffleString) return GUEST_TEXT;
    if (value instanceof NativePointer) return POINTER;
    return OTHER;
  }

  private int pinThreshold() {
    int threshold = pinThreshold;
    return threshold >= 0 ? threshold : learnPinThreshold();
  }

  @HostCompilerDirectives.InliningCutoff
  private int learnPinThreshold() {
    CompilerDirectives.transferToInterpreterAndInvalidate();
    return pinThreshold = Math.max(Borrow.pinThreshold(), 0);
  }

  /**
   * Pushes a guest string onto the scratch stack and returns the stack, or returns the string as a
   * Java string when it does not fit there. Out of line in code compiled ahead of time, where a
   * larger {@link #execute} would no longer be inlined into the cell.
   */
  @HostCompilerDirectives.InliningCutoff
  private Object pushGuestText(AbstractTruffleString text) {
    GuestText node = guestText;
    if (node == null) {
      CompilerDirectives.transferToInterpreterAndInvalidate();
      guestText = node = insert(new GuestText());
    }
    Borrow.Scratch pushed = node.push(text);
    return pushed != null ? pushed : javaString(text);
  }

  @TruffleBoundary
  private static String javaString(AbstractTruffleString text) {
    return text.toJavaStringUncached();
  }

  private void slow(Object value, Borrow borrow) {
    if (value == null) {
      pointer(0, value, borrow);
      return;
    }
    // An owner is an interop pointer, but no byte pointer is a declared resource.
    if (value instanceof NativeResource owner) throw unexpectedOwner(owner);
    InteropLibrary interop = interop();
    try {
      if (strings && interop.isString(value)) {
        borrow.lendString(interop.asString(value), terminated);
        return;
      }
      if (interop.hasBufferElements(value)) {
        borrow.lendBuffer(interop, value, writable, terminated);
        return;
      }
      if (interop.isPointer(value)) {
        pointer(interop.asPointer(value), value, borrow);
        return;
      }
      if (interop.isNull(value)) {
        pointer(0, value, borrow);
        return;
      }
    } catch (UnsupportedMessageException e) {
      throw CompilerDirectives.shouldNotReachHere(e);
    } catch (IllegalArgumentException e) {
      CompilerDirectives.transferToInterpreter();
      throw NativeCellException.of(this, function, ": parameter ", parameter, ": ", e.getMessage());
    }
    throw mismatch(expected(), value);
  }

  /** A bare pointer has no size, so it cannot supply a derived byte count. */
  private void pointer(long address, Object value, Borrow borrow) {
    if (address == 0) {
      if (!nullable) throw mismatch(expected(), value);
      return;
    }
    if (buffer) throw mismatch(expected(), value);
    borrow.lend(address, 0);
  }

  @TruffleBoundary
  private String expected() {
    return (strings ? "string, " : "")
        + (buffer ? "sized buffer" : "buffer or pointer")
        + (nullable ? " or null" : "");
  }
}
