// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package dev.elide.myna.truffle;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

/**
 * A native address as an interop pointer. Guest code never sees raw numbers as pointers unless a
 * binding opts in; a cell returns this for every pointer-typed result and accepts it, or any other
 * interop pointer, for pointer-typed parameters. The null address is also interop {@code null}.
 */
@ExportLibrary(InteropLibrary.class)
public final class NativePointer implements TruffleObject {
  public static final NativePointer NULL = new NativePointer(0);

  private final long address;

  private NativePointer(long address) {
    this.address = address;
  }

  public static NativePointer of(long address) {
    return address == 0 ? NULL : new NativePointer(address);
  }

  public long address() {
    return address;
  }

  @ExportMessage
  boolean isPointer() {
    return true;
  }

  @ExportMessage
  long asPointer() {
    return address;
  }

  @ExportMessage
  boolean isNull() {
    return address == 0;
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof NativePointer p && p.address == address;
  }

  @Override
  public int hashCode() {
    return Long.hashCode(address);
  }

  @Override
  public String toString() {
    return "NativePointer(0x" + Long.toHexString(address) + ")";
  }
}
