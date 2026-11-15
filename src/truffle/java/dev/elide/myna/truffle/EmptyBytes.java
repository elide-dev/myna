// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package dev.elide.myna.truffle;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

/**
 * A non-null address for empty borrows, so a buffer that is present never reaches a buffer
 * parameter as the null pointer: a {@code nullable=false} parameter never receives null, and a
 * nullable one receives it only for a buffer that is absent. It is page aligned, so a parameter
 * with an {@code align} fact up to 4096 receives a valid address for an empty buffer; above that
 * the address is the fact itself, which nothing dereferences for an empty buffer. Allocated on
 * first use at run time: a Native Image must not bake the address into its heap.
 */
final class EmptyBytes {
  /**
   * The dummy's alignment. Under a stricter fact an empty buffer is lent the dangling address of
   * the fact itself instead ({@link OverAlignedBytesCoercion}).
   */
  static final long ALIGNMENT = 4096;

  private static final MemorySegment DUMMY = Arena.global().allocate(1, ALIGNMENT);

  private EmptyBytes() {}

  static long address() {
    return DUMMY.address();
  }
}
