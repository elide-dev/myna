// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package dev.elide.myna.truffle;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;

/**
 * Coerces a guest value to borrowed bytes for a byte-pointer parameter: a {@code ptr<u8>}, {@code
 * ptr<i8>} or {@code ptr<char>} import parameter, or the buffer of a {@code borrow} declaration.
 * Strings are accepted only for read access, since the native side may not write into them.
 */
public final class BytesCoercion extends Coercion {
  /** The kinds of value this call site has lent; only their paths are compiled. */
  private static final int NATIVE = 1;

  private static final int HEAP = 2;
  private static final int TEXT = 4;
  private static final int POINTER = 8;
  private static final int OTHER = 16;

  private final boolean nullable;
  private final boolean writable;
  private final boolean strings;
  private final boolean buffer;
  @CompilationFinal private int seen;

  /** {@link Borrow#pinThreshold()}, read on the first heap array; negative until then. */
  @CompilationFinal private int pinThreshold = -1;

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
    super(function, parameter);
    this.nullable = nullable;
    this.writable = writable;
    this.strings = !writable;
    this.buffer = buffer;
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
        borrow.lend(size == 0 ? Borrow.emptyAddress() : bytes.address(), size);
        return borrow;
      }
      if ((seen & HEAP) != 0 && value instanceof HeapBytes bytes) {
        if (writable && !bytes.isWritable()) throw mismatch("writable buffer", value);
        borrow.lendArray(bytes.array(), writable, !buffer, pinThreshold());
        return borrow;
      }
      if ((seen & TEXT) != 0 && value instanceof String text) {
        borrow.lendString(text, !buffer);
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
      CompilerDirectives.transferToInterpreterAndInvalidate();
      this.seen = seen |= kind(value);
    }
  }

  private int kind(Object value) {
    if (value instanceof NativeBytes) return NATIVE;
    if (value instanceof HeapBytes) return HEAP;
    if (strings && value instanceof String) return TEXT;
    if (value instanceof NativePointer) return POINTER;
    return OTHER;
  }

  private int pinThreshold() {
    int threshold = pinThreshold;
    if (threshold < 0) {
      CompilerDirectives.transferToInterpreterAndInvalidate();
      pinThreshold = threshold = Math.max(Borrow.pinThreshold(), 0);
    }
    return threshold;
  }

  private void slow(Object value, Borrow borrow) {
    if (value == null) {
      pointer(0, value, borrow);
      return;
    }
    InteropLibrary interop = interop();
    try {
      if (strings && interop.isString(value)) {
        borrow.lendString(interop.asString(value), !buffer);
        return;
      }
      if (interop.hasBufferElements(value)) {
        borrow.lendBuffer(interop, value, writable, !buffer);
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
      // A shared NativeResource can be closed by another thread between isPointer() and
      // asPointer(), exactly as in PointerCoercion: a guest error, not an internal invariant.
      if (value instanceof NativeResource resource) throw closed(resource);
      throw CompilerDirectives.shouldNotReachHere(e);
    } catch (IllegalArgumentException e) {
      CompilerDirectives.transferToInterpreter();
      throw NativeCellException.of(this, function, ": parameter ", parameter, ": ", e.getMessage());
    }
    if (value instanceof NativeResource resource && resource.isClosed()) throw closed(resource);
    throw mismatch(expected(), value);
  }

  private NativeCellException closed(NativeResource resource) {
    CompilerDirectives.transferToInterpreter();
    return NativeCellException.of(
        this, function, ": parameter ", parameter, " received a closed ", resource.type());
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

  @CompilerDirectives.TruffleBoundary
  private String expected() {
    return (strings ? "string, " : "")
        + (buffer ? "sized buffer" : "buffer or pointer")
        + (nullable ? " or null" : "");
  }
}
