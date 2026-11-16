// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package dev.elide.myna.truffle;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import java.lang.foreign.MemorySegment;

/**
 * Coerces a guest array of byte buffers or strings to a batch: the pointer and length arrays of a
 * {@code borrow F SOURCES lengths=LENGTHS count=COUNT} declaration. Every element is lent by the
 * single-buffer coercion, under exactly its rules (pin, copy or lend in place, strings for read
 * access), and the {@link Batch} takes over what each element must undo, so one {@link
 * Batch#release()} ends them all, in reverse. Guest {@code null}, or an empty array, is the empty
 * batch: null arrays with count zero.
 *
 * <p>The array's size is read once, and each element exactly once, as it is lent: what the call
 * lends, and what its release undoes, is what was read, whatever the guest stores into the array
 * meanwhile (and never re-read). An FFM cell validates the elements it lent in place again right
 * before its call ({@link Batch#recheck}), since coercing later elements may run guest code. A
 * failure to lend element {@code k} releases the {@code k} elements before it.
 *
 * <p>The arrays take {@code 16 * count} bytes of the current thread's scratch stack, above which
 * the elements' copies stack, so a batch of small buffers costs what as many single borrows do; a
 * batch whose arrays do not fit there takes an arena of its own, as a large copy does.
 *
 * <p>FFM cells pass a single buffer's own segment to the downcall, which then holds its scope for
 * the call. The addresses of a batch are in memory, where the linker holds nothing, so an FFM cell
 * lends an element of scoped native memory in place only when nothing can close it during the call,
 * as the host wrapper does: confined to the calling thread, under a native leaf. It copies any
 * other (shared, automatic, or confined under an import that can call back) through its segment, a
 * scope-checked read, straight onto the scratch stack.
 */
public final class BatchCoercion extends Coercion {
  /** The most elements a batch takes: {@code 16 * count} bytes of arrays fit an {@code int}. */
  static final int MAX_COUNT = (Integer.MAX_VALUE - 8) / 16;

  @Child private BytesCoercion element;
  private final boolean wide;
  private final long maxLength;
  private final boolean scoped;
  private final boolean leaf;

  /**
   * @param elementsNullable whether an element may be absent: interop {@code null}, lent as the
   *     null pointer with length zero
   * @param wide whether each length is 8 bytes; otherwise 4
   * @param maxLength the largest byte length the lengths' type carries
   * @param ffm whether the cell calls through FFM, which copies scoped native memory unless nothing
   *     can close it during the call
   * @param leaf whether the import is a native leaf, which cannot call back into guest code: a
   *     segment confined to the calling thread then stays open for the call
   */
  public BatchCoercion(
      String function,
      String parameter,
      boolean elementsNullable,
      boolean wide,
      long maxLength,
      boolean ffm,
      boolean leaf) {
    super(function, parameter);
    this.element = new BytesCoercion(function, parameter, elementsNullable, false, true);
    this.wide = wide;
    this.maxLength = maxLength;
    this.scoped = ffm;
    this.leaf = leaf;
  }

  /** Lends every element of {@code value} for one call; see the class comment. */
  public Batch execute(Object value) {
    Batch batch = new Batch();
    if (value == null || value instanceof NativeVoid) return batch;
    InteropLibrary interop = interop();
    int count;
    try {
      if (!interop.hasArrayElements(value)) {
        if (interop.isNull(value)) return batch;
        throw mismatch("array of buffers or strings, or null", value);
      }
      // A guest array's size is untrusted: nothing negative or too large is lent.
      long size = interop.getArraySize(value);
      if (size < 0 || size > MAX_COUNT) throw badSize(size);
      count = (int) size;
    } catch (UnsupportedMessageException e) {
      // An array that has elements but no size is no array a batch can take.
      throw mismatch("array of buffers or strings, or null", value);
    }
    if (count == 0) return batch;
    batch.reserve(count);
    try {
      for (int i = 0; i < count; i++) {
        Object item;
        try {
          item = interop.readArrayElement(value, i);
        } catch (InvalidArrayIndexException | UnsupportedMessageException e) {
          throw unreadable(i);
        }
        long length;
        if (scoped && item instanceof NativeBytes bytes && bytes.scoped != null)
          length = lendScoped(batch, i, bytes);
        else {
          Borrow lent = element.execute(item);
          length = lent.length();
          batch.absorb(i, lent, wide);
        }
        if (length > maxLength) throw tooLong(i, length);
      }
    } catch (RuntimeException | Error e) {
      batch.release();
      throw e;
    }
    return batch;
  }

  /**
   * Lends element {@code index}, guest native memory whose scope an FFM downcall would hold for a
   * single buffer but cannot for an address in memory. In place when nothing can close it during
   * the call (a segment confined to this thread, under a native leaf); otherwise copied through its
   * segment, a scope-checked read, onto the scratch stack, or into an arena when too large for it.
   * Neither path allocates on the Java heap beyond the arena.
   */
  private long lendScoped(Batch batch, int index, NativeBytes bytes) {
    MemorySegment segment = bytes.scoped;
    if (leaf && confinedHere(segment)) {
      Borrow lent = element.execute(bytes);
      batch.absorb(index, lent, wide);
      // Validated again right before the call, once guest code can no longer run: Batch.recheck.
      batch.lentInPlace(segment);
      return lent.length();
    }
    long length = bytes.size();
    Borrow.Scratch scratch;
    try {
      scratch = Borrow.Scratch.pushSegment(segment);
    } catch (IllegalStateException | WrongThreadException e) {
      throw unavailable(index);
    }
    if (scratch != null) {
      batch.absorbScratch(
          index, scratch.base + scratch.top - Borrow.Scratch.reservation(length), length, wide);
      return length;
    }
    Borrow.Copy copy = Borrow.Copy.ofSegment(segment);
    if (copy == null) throw unavailable(index);
    batch.absorbCopy(index, copy, length, wide);
    return length;
  }

  /**
   * Whether {@code segment} is open and confined to the calling thread, which alone can close it:
   * accessible from this thread but not from one that never starts.
   */
  @TruffleBoundary
  private static boolean confinedHere(MemorySegment segment) {
    return segment.scope().isAlive()
        && segment.isAccessibleBy(Thread.currentThread())
        && !segment.isAccessibleBy(Sentinel.THREAD);
  }

  /**
   * A thread that never starts, in a run-time-initialized holder so that no {@code Thread} is
   * created at image build time.
   */
  static final class Sentinel {
    static final Thread THREAD = create();

    /**
     * Never inherits the creating thread's inheritable thread-locals: nothing of whichever thread
     * first lends a batch is copied into it, and no {@code childValue} runs.
     */
    static Thread create() {
      return Thread.ofPlatform().inheritInheritableThreadLocals(false).unstarted(() -> {});
    }
  }

  @TruffleBoundary
  private NativeCellException badSize(long size) {
    return NativeCellException.of(
        this,
        function,
        ": parameter ",
        parameter,
        ": a batch takes 0 to ",
        MAX_COUNT,
        " buffers, got an array of size ",
        size);
  }

  @TruffleBoundary
  private NativeCellException unreadable(int index) {
    return NativeCellException.of(
        this, function, ": parameter ", parameter, ": element ", index, " cannot be read");
  }

  @TruffleBoundary
  private NativeCellException unavailable(int index) {
    return NativeCellException.of(
        this,
        function,
        ": parameter ",
        parameter,
        ": element ",
        index,
        "'s native memory is closed or confined to another thread");
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
}
