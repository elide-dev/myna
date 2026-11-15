// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package dev.elide.myna.truffle;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.strings.AbstractTruffleString;
import java.lang.foreign.MemorySegment;

/**
 * Coerces a guest value to the read-only buffer of a {@code lifetime=resource:NAME} borrow, which a
 * create keeps until the resource it makes is destroyed. The borrow outlives the call, so nothing
 * is pinned or put on a scratch stack, both of which end with the call:
 *
 * <ul>
 *   <li>{@link NativeBytes} is lent in place, and the borrow keeps it reachable, which keeps an
 *       automatic arena's memory allocated until the borrow ends. An FFM cell also passes its
 *       segment to the create call (see {@link #segment}), so that call checks and holds its scope;
 *   <li>heap arrays, strings (as UTF-8, unterminated) and other interop buffers are copied into
 *       native memory the borrow owns, so later guest writes to the source do not reach the native
 *       side;
 *   <li>{@code null}, for a nullable buffer, lends the null address and length zero;
 *   <li>a bare pointer has no size and is refused, as for any borrow.
 * </ul>
 *
 * <p>The cell hands the borrow to the new resource with {@link Borrow#keep()}, or releases it when
 * the create fails. A create allocates (its owner, the runtime's struct), so this coercion does not
 * profile kinds or avoid allocating the borrow; it is kept apart from {@link BytesCoercion} so that
 * none of this reaches the call-scoped borrows' paths.
 */
public final class HeldBytesCoercion extends Coercion {
  private final boolean nullable;

  /** The parameter's {@code align} fact, or zero for none; see {@link BytesCoercion}. */
  private final long align;

  /**
   * @param nullable whether {@code null} lends the null address with length zero
   */
  public HeldBytesCoercion(String function, String parameter, boolean nullable) {
    this(function, parameter, nullable, 0);
  }

  /**
   * @param align the parameter's {@code align=N@explicit_contract} fact, or zero for none: native
   *     memory that is not divisible by it is copied instead of lent in place
   */
  public HeldBytesCoercion(String function, String parameter, boolean nullable, long align) {
    super(function, parameter);
    if (align < 0 || (align & (align - 1)) != 0)
      throw new IllegalArgumentException("align must be a power of two: " + align);
    this.nullable = nullable;
    this.align = align <= 1 ? 0 : align;
  }

  /** Lends {@code value} until the borrow is released or, once kept, its resource ends it. */
  public Borrow execute(Object value) {
    Borrow borrow = new Borrow();
    if (value instanceof NativeBytes bytes) {
      long size = bytes.size();
      if (align > 1 && size != 0 && (bytes.address() & (align - 1)) != 0) slow(value, borrow);
      else {
        borrow.lend(size == 0 ? Borrow.emptyAddress() : bytes.address(), size);
        borrow.retain(bytes);
      }
    } else if (value instanceof HeapBytes bytes) {
      borrow.lendHeld(bytes.array(), align);
    } else if (value instanceof String text) {
      borrow.lendHeldText(text, align);
    } else if (value instanceof AbstractTruffleString text) {
      borrow.lendHeldText(javaString(text), align);
    } else {
      slow(value, borrow);
    }
    return borrow;
  }

  /**
   * The segment whose scope the create call must check for the memory {@link #execute} lent in
   * place for {@code value}, or {@code null} when there is nothing to check: anything but native
   * memory (a copy, null), an empty buffer, or native memory of the global scope or viewed from an
   * address, as for {@link BytesCoercion#segment}. Only FFM cells ask, to pass the guest's own
   * segment to the create's downcall, which then refuses a closed segment or one confined to
   * another thread before native code runs, and keeps a shared arena open for the call. After the
   * call the borrow only keeps the buffer reachable; no scope is held for the resource's lifetime.
   *
   * <p>Unlike a call borrow's, this site does not profile whether it has seen such a segment: the
   * test is a type check and a field read, and the cell's call already profiles which patterns of
   * segments it has made.
   */
  public MemorySegment segment(Object value) {
    if (!(value instanceof NativeBytes bytes)) return null;
    // Memory that violates the align fact was copied: nothing of the guest's is passed in place.
    if (align > 1 && bytes.size() != 0 && (bytes.address() & (align - 1)) != 0) return null;
    return bytes.scoped;
  }

  @TruffleBoundary
  private static String javaString(AbstractTruffleString text) {
    return text.toJavaStringUncached();
  }

  private void slow(Object value, Borrow borrow) {
    if (value == null) {
      absent(value);
      return;
    }
    // An owner is never a buffer, open or closed: only a pointer to its resource takes one.
    if (value instanceof NativeResource owner) throw unexpectedOwner(owner);
    InteropLibrary interop = interop();
    try {
      if (interop.isString(value)) {
        borrow.lendHeldText(interop.asString(value), align);
        return;
      }
      if (interop.hasBufferElements(value)) {
        borrow.lendHeldBuffer(interop, value, align);
        return;
      }
      if ((interop.isPointer(value) && interop.asPointer(value) == 0) || interop.isNull(value)) {
        absent(value);
        return;
      }
    } catch (UnsupportedMessageException e) {
      // A value that stops answering what it claimed (a buffer without a size) is a guest error.
      throw mismatch(expected(), value);
    } catch (IllegalArgumentException e) {
      CompilerDirectives.transferToInterpreter();
      throw NativeCellException.of(this, function, ": parameter ", parameter, ": ", e.getMessage());
    }
    throw mismatch(expected(), value);
  }

  /** Null lends the null address with length zero, for a nullable buffer only. */
  private void absent(Object value) {
    if (!nullable) throw mismatch(expected(), value);
  }

  @TruffleBoundary
  private String expected() {
    return "string, sized buffer" + (nullable ? " or null" : "");
  }
}
