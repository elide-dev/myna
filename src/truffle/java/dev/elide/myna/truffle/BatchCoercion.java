// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package dev.elide.myna.truffle;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

/**
 * Coerces a guest array of byte buffers or strings to a batch: the pointer and length arrays of a
 * {@code borrow F SOURCES lengths=LENGTHS count=COUNT} declaration. Every element is lent by the
 * single-buffer coercion, under exactly its rules (pin, copy or lend in place, strings for read
 * access), and the batch takes over what each element must undo, so one {@link Borrow#release()}
 * ends them all, in reverse. Guest {@code null}, or an empty array, is the empty batch: null arrays
 * with count zero.
 *
 * <p>Each element is read from the guest array exactly once, and lent as it is read: what the call
 * lends, and what its release undoes, is that snapshot, whatever the guest stores into the array
 * meanwhile. A failure to lend element {@code k} releases the {@code k} elements before it.
 *
 * <p>The arrays take {@code 16 * count} bytes of the current thread's scratch stack, above which
 * the elements' copies stack, so a batch of small buffers costs what as many single borrows do; a
 * batch whose arrays do not fit there takes an arena of its own, as a large copy does.
 *
 * <p>FFM cells pass a single buffer's own segment to the downcall, which then holds its scope for
 * the call. The addresses of a batch are in memory, where the linker holds nothing, so an FFM cell
 * copies an element whose scope has anything to check (any but the global scope) instead of lending
 * it in place; the copy reads through the segment, a scope-checked access.
 */
public final class BatchCoercion extends Coercion {
  /** The most elements a batch takes: {@code 16 * count} bytes of arrays fit an {@code int}. */
  static final int MAX_COUNT = (Integer.MAX_VALUE - 8) / 16;

  @Child private BytesCoercion element;
  private final boolean wide;
  private final long maxLength;
  private final boolean scoped;

  /**
   * @param elementsNullable whether an element may be absent: interop {@code null}, lent as the
   *     null pointer with length zero
   * @param wide whether each length is 8 bytes; otherwise 4
   * @param maxLength the largest byte length the lengths' type carries
   * @param ffm whether the cell calls through FFM, which copies scoped native memory
   */
  public BatchCoercion(
      String function,
      String parameter,
      boolean elementsNullable,
      boolean wide,
      long maxLength,
      boolean ffm) {
    super(function, parameter);
    this.element = new BytesCoercion(function, parameter, elementsNullable, false, true);
    this.wide = wide;
    this.maxLength = maxLength;
    this.scoped = ffm;
  }

  /** Lends every element of {@code value} for one call; see the class comment. */
  public Borrow execute(Object value) {
    Borrow batch = new Borrow();
    if (value == null || value instanceof NativeVoid) return batch;
    InteropLibrary interop = interop();
    int count;
    try {
      if (!interop.hasArrayElements(value)) {
        if (interop.isNull(value)) return batch;
        throw mismatch("array of buffers or strings, or null", value);
      }
      long size = interop.getArraySize(value);
      if (size > MAX_COUNT) throw tooMany(size);
      count = (int) size;
    } catch (UnsupportedMessageException e) {
      throw CompilerDirectives.shouldNotReachHere(e);
    }
    if (count == 0) return batch;
    batch.lendBatch(count);
    try {
      for (int i = 0; i < count; i++) {
        Object item = interop.readArrayElement(value, i);
        Borrow lent = lend(item);
        batch.absorb(i, lent, wide);
        if (lent.length() > maxLength) throw tooLong(i, lent.length());
      }
    } catch (InvalidArrayIndexException | UnsupportedMessageException e) {
      batch.release();
      throw shrank(e);
    } catch (RuntimeException | Error e) {
      batch.release();
      throw e;
    }
    return batch;
  }

  /** One element, by the single-buffer coercion; scoped native memory is copied in FFM cells. */
  private Borrow lend(Object item) {
    // Checked on every element, not only once the call site has seen such memory: a single
    // borrow's downcall would still hold the scope of the first one, a batch's would not.
    if (scoped && item instanceof NativeBytes bytes && bytes.scoped != null)
      return copyScoped(bytes.scoped);
    return element.execute(item);
  }

  /**
   * A copy of guest native memory whose scope the call cannot hold, read through its segment: a
   * scope-checked access, which refuses a closed arena or one confined to another thread.
   */
  @TruffleBoundary
  private Borrow copyScoped(MemorySegment segment) {
    byte[] bytes;
    try {
      bytes = segment.toArray(ValueLayout.JAVA_BYTE);
    } catch (IllegalStateException | WrongThreadException e) {
      throw NativeCellException.of(
          this,
          function,
          ": parameter ",
          parameter,
          ": a batch element's native memory is ",
          e instanceof WrongThreadException ? "confined to another thread" : "closed");
    }
    Borrow borrow = new Borrow();
    borrow.lendArray(bytes, false, false, Integer.MAX_VALUE);
    return borrow;
  }

  @TruffleBoundary
  private NativeCellException tooMany(long size) {
    return NativeCellException.of(
        this,
        function,
        ": parameter ",
        parameter,
        ": a batch takes at most ",
        MAX_COUNT,
        " buffers, got ",
        size);
  }

  @TruffleBoundary
  private NativeCellException tooLong(int index, long length) {
    return NativeCellException.of(
        this,
        function,
        ": parameter ",
        parameter,
        ": element ",
        index,
        " has ",
        length,
        " bytes, more than its length type carries");
  }

  @TruffleBoundary
  private NativeCellException shrank(Exception e) {
    return NativeCellException.of(
        this,
        function,
        ": parameter ",
        parameter,
        ": the array changed while it was lent: ",
        e.getMessage());
  }
}
