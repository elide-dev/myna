// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package dev.elide.myna.truffle;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.strings.AbstractTruffleString;

/**
 * The check an {@code access=cstring} parameter's cell makes before lending a string: one with an
 * embedded NUL would end early on the C side, so it is a guest error. Only strings are scanned,
 * behind a boundary; any other value passes through untouched, and cells without such a parameter
 * never call this.
 */
public final class CStrings {
  private CStrings() {}

  /** Returns {@code value}, or throws if it is a string with an embedded NUL. */
  public static Object checked(Object value, String function, String parameter, Node node) {
    if (value instanceof AbstractTruffleString text) {
      if (hasNul(text)) throw embeddedNul(function, parameter, value, node);
    } else if (value instanceof String text) {
      if (hasNul(text)) throw embeddedNul(function, parameter, value, node);
    }
    return value;
  }

  @TruffleBoundary
  private static boolean hasNul(AbstractTruffleString text) {
    return text.toJavaStringUncached().indexOf('\0') >= 0;
  }

  /** Behind a boundary: String.indexOf is blocklisted for runtime compilation. */
  @TruffleBoundary
  private static boolean hasNul(String text) {
    return text.indexOf('\0') >= 0;
  }

  private static NativeCellException embeddedNul(
      String function, String parameter, Object value, Node node) {
    CompilerDirectives.transferToInterpreter();
    return NativeCellException.type(
        function, parameter, "C string without an embedded NUL", value, node);
  }
}
