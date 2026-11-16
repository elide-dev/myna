// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package dev.elide.myna.truffle;

import java.lang.foreign.MemorySegment;

/**
 * A batch of buffers lent to one call ({@code borrow F SOURCES lengths=… count=…}): the pointer
 * array at {@link #address()}, followed by the length array, for {@link #length()} elements, and
 * whatever must be undone once the call returns. {@link BatchCoercion} fills it.
 *
 * <p>Both arrays take {@code 16 * count} bytes of the current thread's scratch stack, and the
 * elements' scratch copies stack above them, so {@link #release()} pops them all at once; arrays
 * too large for the stack take a confined arena of their own, as a large copy does. Pins and arena
 * copies of elements are kept in an array made only when the first such element arrives, the one
 * allocation lending needs beyond the pins and copies themselves, and are undone in reverse.
 *
 * <p>Like a {@link Borrow}, a batch that a compiled cell acquires and releases is not allocated:
 * nothing on its paths passes it to code that partial evaluation does not inline.
 */
public final class Batch {
  /** The scratch stack, whose top the batch pops back to {@link #mark} on release. */
  private Borrow.Scratch scratch;

  /** The memory the arrays are in: the scratch block, or {@link #arrays}' segment. */
  private MemorySegment block;

  private long address;

  /** Where the arrays start in {@link #block}. */
  private long offset;

  private int count;
  private long mark;

  /** Where the stack's top must be on release: the end of the last element's scratch copy. */
  private long top;

  /** The arena of arrays too large for the scratch stack; {@code null} otherwise. */
  private Borrow.Copy arrays;

  /** The pins and arena copies of elements, by index; {@code null} until the first one. */
  private Object[] owners;

  private boolean released;

  /** The empty batch until {@link #reserve} fills it: null arrays with count zero. */
  Batch() {}

  /** The address of the pointer array; {@code 0} for the empty batch. */
  public long address() {
    return address;
  }

  /** The number of elements. */
  public long length() {
    return count;
  }

  /** The address of the length array: {@link #length()} lengths after the pointers. */
  public long lengths() {
    return count == 0 ? 0 : address + 8L * count;
  }

  /** Reserves the arrays of {@code count} elements; see the class comment. */
  void reserve(int count) {
    long size = 16L * count;
    Borrow.Scratch scratch = Borrow.Scratch.region(size);
    this.count = count;
    if (scratch != null) {
      long start = scratch.top - Borrow.Scratch.reservation(size);
      this.scratch = scratch;
      this.block = scratch.block;
      this.offset = start;
      this.address = scratch.base + start;
      this.mark = start;
    } else {
      scratch = Borrow.Scratch.current();
      Borrow.Copy arrays = Borrow.Copy.region(size);
      this.scratch = scratch;
      this.arrays = arrays;
      this.block = arrays.segment;
      this.offset = 0;
      this.address = arrays.address;
      this.mark = scratch.top;
    }
    this.top = scratch.top;
  }

  /** Writes element {@code index}: its address and its length, 8 bytes wide or 4. */
  private void put(int index, long elementAddress, long elementLength, boolean wide) {
    long base = address - offset;
    Memory.putLong(base, block, offset + 8L * index, elementAddress);
    if (wide) Memory.putLong(base, block, offset + 8L * count + 8L * index, elementLength);
    else Memory.putInt(base, block, offset + 8L * count + 4L * index, (int) elementLength);
  }

  /**
   * Takes over element {@code index}, which {@code element} has just lent: a scratch copy pops with
   * the batch, and a pin or an arena copy is kept until release.
   */
  void absorb(int index, Borrow element, boolean wide) {
    put(index, element.address(), element.length(), wide);
    Object owner = element.handOver();
    if (owner != null) own(index, owner);
    top = scratch.top;
  }

  /**
   * Takes over element {@code index}: {@code length} bytes the scratch stack holds at {@code at}.
   */
  void absorbScratch(int index, long at, long length, boolean wide) {
    put(index, at, length, wide);
    top = scratch.top;
  }

  /** Takes over element {@code index}: an arena copy of its own. */
  void absorbCopy(int index, Borrow.Copy copy, long length, boolean wide) {
    put(index, copy.address, length, wide);
    own(index, copy);
  }

  /** The scratch stack's current top, where the next copy goes. */
  long stackTop() {
    return scratch.top;
  }

  private void own(int index, Object owner) {
    Object[] owners = this.owners;
    if (owners == null) this.owners = owners = new Object[count];
    owners[index] = owner;
  }

  /**
   * Ends the batch: unpins and frees its elements in reverse, then pops its scratch region and
   * frees any arena its arrays took. Idempotent.
   *
   * @throws IllegalStateException if the batch is released out of order or on another thread than
   *     the one that lent it; the batch and the stack are left as they were
   */
  public void release() {
    if (released || count == 0) return;
    Borrow.Scratch scratch = this.scratch;
    scratch.check((int) mark, top);
    released = true;
    try {
      Object[] owners = this.owners;
      if (owners != null)
        for (int i = owners.length - 1; i >= 0; i--) {
          Object owner = owners[i];
          if (owner instanceof Borrow.Copy copy) Borrow.Copy.release(copy, null, null);
          else if (owner != null) Borrow.Pins.unpin(owner);
        }
    } finally {
      scratch.top = mark;
      if (arrays != null) Borrow.Copy.release(arrays, null, null);
    }
  }
}
