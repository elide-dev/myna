// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package dev.elide.myna.truffle;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

/**
 * A read-only interop array of guest values, for host code that calls a cell with a batch of
 * buffers ({@code borrow F SOURCES lengths=… count=…}): any value a single buffer accepts, a {@link
 * HeapBytes}, {@link NativeBytes} or string, or {@code null} where elements may be absent. Guest
 * languages pass their own arrays.
 */
@ExportLibrary(InteropLibrary.class)
public final class BufferArray implements TruffleObject {
  private final Object[] elements;

  private BufferArray(Object[] elements) {
    this.elements = elements;
  }

  /** An array of {@code elements}, which it copies. */
  public static BufferArray of(Object... elements) {
    return new BufferArray(elements.clone());
  }

  @ExportMessage
  boolean hasArrayElements() {
    return true;
  }

  @ExportMessage
  long getArraySize() {
    return elements.length;
  }

  @ExportMessage
  boolean isArrayElementReadable(long index) {
    return index >= 0 && index < elements.length;
  }

  @ExportMessage
  Object readArrayElement(long index) throws InvalidArrayIndexException {
    if (index < 0 || index >= elements.length) throw InvalidArrayIndexException.create(index);
    Object element = elements[(int) index];
    return element == null ? NativeVoid.INSTANCE : element;
  }

  @Override
  public String toString() {
    return "BufferArray[" + elements.length + "]";
  }
}
