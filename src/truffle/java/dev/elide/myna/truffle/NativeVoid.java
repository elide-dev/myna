// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package dev.elide.myna.truffle;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

/** The result of a {@code void} call, and the null result of a failed resource creation. */
@ExportLibrary(InteropLibrary.class)
public final class NativeVoid implements TruffleObject {
  public static final NativeVoid INSTANCE = new NativeVoid();

  private NativeVoid() {}

  @ExportMessage
  boolean isNull() {
    return true;
  }

  @Override
  public String toString() {
    return "NativeVoid";
  }
}
