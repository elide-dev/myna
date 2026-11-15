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
 * {@link BytesCoercion} for a byte-pointer parameter with an {@code align=N@explicit_contract}
 * fact: no address that native code receives is lent at a lesser alignment. Aligned native memory
 * is lent in place; memory that is not is copied through its own buffer messages (and written back
 * by them when the parameter is writable); a bare pointer that is not aligned has no size to copy
 * and is refused; every other value is copied at an alignment of at least {@code N}, which for the
 * scratch stack and every other copy is already 16.
 *
 * <p>It is a class of its own so that {@link BytesCoercion#execute}, which a compiled call inlines
 * to keep its borrow out of the heap, carries nothing for parameters without the fact. This one
 * does not promise that: its borrow is a heap object, which is the price of a stricter address.
 */
public final class AlignedBytesCoercion extends Coercion {
  /** The kinds of value this call site has lent, as {@link BytesCoercion} records them. */
  private static final int NATIVE = 1;

  private static final int HEAP = 2;
  private static final int TEXT = 4;
  private static final int GUEST_TEXT = 32;
  private static final int POINTER = 8;
  private static final int OTHER = 16;

  /** Not a kind: whether this call site has lent native memory whose scope a call must check. */
  private static final int SCOPED = 64;

  private final boolean nullable;
  private final boolean writable;
  private final boolean strings;
  private final boolean buffer;
  private final boolean terminated;
  private final long align;
  @CompilationFinal private int seen;
  @CompilationFinal private int pinThreshold = -1;

  /**
   * @param align the parameter's alignment requirement in bytes, a power of two
   * @see BytesCoercion#BytesCoercion(String, String, boolean, boolean, boolean, boolean)
   */
  public AlignedBytesCoercion(
      String function,
      String parameter,
      boolean nullable,
      boolean writable,
      boolean buffer,
      boolean terminated,
      long align) {
    super(function, parameter);
    if (align < 1 || (align & (align - 1)) != 0)
      throw new IllegalArgumentException("align must be a power of two: " + align);
    if (buffer && terminated) throw new IllegalArgumentException("a borrow is not terminated");
    this.nullable = nullable;
    this.writable = writable;
    this.strings = !writable;
    this.buffer = buffer;
    this.terminated = terminated;
    this.align = align;
  }

  /** Lends {@code value} for one call, at an address divisible by {@link #align}. */
  @HostCompilerDirectives.InliningCutoff
  public Borrow execute(Object value) {
    Borrow borrow = new Borrow();
    if ((seen & kind(value)) == 0) learn(value);
    if (value instanceof NativeBytes bytes) {
      if (writable && !bytes.isWritable()) throw mismatch("writable buffer", value);
      long size = bytes.size();
      if (size == 0) borrow.lend(Borrow.emptyAddress(), 0);
      else if (misaligned(bytes.address())) slow(value, borrow);
      else borrow.lend(bytes.address(), size);
    } else if (value instanceof HeapBytes bytes) {
      if (writable && !bytes.isWritable()) throw mismatch("writable buffer", value);
      borrow.lendAlignedArray(bytes.array(), writable, terminated, pinThreshold(), align);
    } else if (strings && value instanceof String text) {
      borrow.lendAlignedString(text, terminated, align);
    } else if (strings && value instanceof AbstractTruffleString text) {
      borrow.lendAlignedString(javaString(text), terminated, align);
    } else if (value instanceof NativePointer pointer) {
      pointer(pointer.address(), value, borrow);
    } else {
      slow(value, borrow);
    }
    return borrow;
  }

  /**
   * The segment whose scope a call must check for the memory {@link #execute} lent in place, as for
   * {@link BytesCoercion#segment}; {@code null} for a copy, so memory that violates the fact is
   * never passed to a downcall as the guest's own segment.
   */
  public MemorySegment segment(Object value) {
    if ((seen & NATIVE) == 0 || !(value instanceof NativeBytes bytes)) return null;
    if (bytes.size() != 0 && misaligned(bytes.address())) return null;
    MemorySegment scoped = bytes.scoped;
    if ((seen & SCOPED) == 0) {
      if (scoped == null) return null;
      learnScoped();
    }
    return scoped;
  }

  /** As {@link BytesCoercion#scoped}. */
  public NativeBytes scoped(Object value) {
    if (segment(value) == null) return null;
    NativeBytes bytes = (NativeBytes) value;
    if (writable && !bytes.isWritable()) throw mismatch("writable buffer", value);
    return bytes;
  }

  private boolean misaligned(long address) {
    return (address & (align - 1)) != 0;
  }

  @HostCompilerDirectives.InliningCutoff
  private void learn(Object value) {
    CompilerDirectives.transferToInterpreterAndInvalidate();
    seen |= kind(value);
  }

  @HostCompilerDirectives.InliningCutoff
  private void learnScoped() {
    CompilerDirectives.transferToInterpreterAndInvalidate();
    seen |= SCOPED;
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
    if (threshold < 0) {
      CompilerDirectives.transferToInterpreterAndInvalidate();
      pinThreshold = threshold = Math.max(Borrow.pinThreshold(), 0);
    }
    return threshold;
  }

  @TruffleBoundary
  private static String javaString(AbstractTruffleString text) {
    return text.toJavaStringUncached();
  }

  /** Whatever is not native memory, a heap array or text: an interop buffer, string or pointer. */
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
        borrow.lendAlignedString(interop.asString(value), terminated, align);
        return;
      }
      if (interop.hasBufferElements(value)) {
        borrow.lendBuffer(interop, value, writable, terminated, align);
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

  /** A bare pointer has no size to copy: one that violates the fact is refused. */
  private void pointer(long address, Object value, Borrow borrow) {
    if (address == 0) {
      if (!nullable) throw mismatch(expected(), value);
      return;
    }
    if (buffer) throw mismatch(expected(), value);
    if (misaligned(address)) throw unaligned(address);
    borrow.lend(address, 0);
  }

  private NativeCellException unaligned(long address) {
    CompilerDirectives.transferToInterpreter();
    return NativeCellException.of(
        this,
        function,
        ": parameter ",
        parameter,
        ": pointer 0x",
        Long.toHexString(address),
        " is not aligned to ",
        Long.toString(align));
  }

  @TruffleBoundary
  private String expected() {
    return (strings ? "string, " : "")
        + (buffer ? "sized buffer" : "buffer or pointer")
        + (nullable ? " or null" : "");
  }
}
