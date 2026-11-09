// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package dev.elide.myna.truffle;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;

/**
 * Coerces a guest value to borrowed bytes for a byte-pointer parameter: a {@code ptr<u8>}, {@code
 * ptr<i8>} or {@code ptr<char>} import parameter, or the buffer of a {@code borrow} declaration.
 * Strings are accepted only for read access, since the native side may not write into them.
 */
public final class BytesCoercion extends Coercion {
  private final boolean nullable;
  private final boolean writable;
  private final boolean strings;
  private final boolean buffer;

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

  public Borrow execute(Object value) {
    if (value instanceof NativeBytes bytes) {
      if (writable && !bytes.isWritable()) throw mismatch("writable buffer", value);
      return bytes.size() == 0 ? EmptyBytes.borrow() : Borrow.direct(bytes.address(), bytes.size());
    }
    if (value instanceof HeapBytes bytes) {
      if (writable && !bytes.isWritable()) throw mismatch("writable buffer", value);
      return Borrow.ofArray(bytes.array(), writable, !buffer);
    }
    if (strings && value instanceof String text) return Borrow.ofString(text, !buffer);
    return slow(value);
  }

  private Borrow slow(Object value) {
    if (value == null) return pointer(0, value);
    if (value instanceof NativePointer pointer) return pointer(pointer.address(), value);
    InteropLibrary interop = interop();
    try {
      if (strings && interop.isString(value))
        return Borrow.ofString(interop.asString(value), !buffer);
      if (interop.hasBufferElements(value))
        return Borrow.ofBuffer(interop, value, writable, !buffer);
      if (interop.isPointer(value)) return pointer(interop.asPointer(value), value);
      if (interop.isNull(value)) return pointer(0, value);
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
  private Borrow pointer(long address, Object value) {
    if (address == 0) {
      if (!nullable) throw mismatch(expected(), value);
      return Borrow.none();
    }
    if (buffer) throw mismatch(expected(), value);
    return Borrow.direct(address, 0);
  }

  @CompilerDirectives.TruffleBoundary
  private String expected() {
    return (strings ? "string, " : "")
        + (buffer ? "sized buffer" : "buffer or pointer")
        + (nullable ? " or null" : "");
  }
}
