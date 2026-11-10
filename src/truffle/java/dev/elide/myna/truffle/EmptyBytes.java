// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package dev.elide.myna.truffle;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

/**
 * A non-null address for empty borrows, so a buffer that is present never reaches a buffer
 * parameter as the null pointer: a {@code nullable=false} parameter never receives null, and a
 * nullable one receives it only for a buffer that is absent. Allocated on first use at run time: a
 * Native Image must not bake the address into its heap.
 */
final class EmptyBytes {
  private static final MemorySegment DUMMY = Arena.global().allocate(1);

  private EmptyBytes() {}

  static long address() {
    return DUMMY.address();
  }
}
