// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package dev.elide.myna.truffle;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

/**
 * A non-null address for empty borrows, so a {@code nullable=false} buffer parameter never receives
 * the null pointer. Allocated on first use at run time: a Native Image must not bake the address
 * into its heap.
 */
final class EmptyBytes {
  private static final MemorySegment DUMMY = Arena.global().allocate(1);

  private EmptyBytes() {}

  static long address() {
    return DUMMY.address();
  }
}
