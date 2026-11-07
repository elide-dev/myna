// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package dev.elide.myna.truffle;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.exception.AbstractTruffleException;
import com.oracle.truffle.api.nodes.Node;

/**
 * A guest-visible error raised by a native cell: wrong arity, an argument that does not coerce to
 * the declared carrier, or misuse of a borrowed buffer or owned resource. Guest languages observe
 * it as an ordinary runtime error; it never carries a native failure, because cells only call
 * {@code error=no_failure} imports.
 */
public final class NativeCellException extends AbstractTruffleException {
  private static final long serialVersionUID = 1L;

  private NativeCellException(String message, Node location) {
    super(message, location);
  }

  /**
   * Callers leave compiled code before reaching the boundary factories here, since a directive
   * inside a boundary is a no-op: see {@code Coercion.mismatch} and {@code NativeCell.checkArity}.
   */
  public static NativeCellException create(String message, Node location) {
    return new NativeCellException(message, location);
  }

  /** Builds the message behind a boundary, so no string work reaches compiled code. */
  @TruffleBoundary
  public static NativeCellException of(Node location, Object... parts) {
    var message = new StringBuilder();
    for (Object part : parts) message.append(part);
    return new NativeCellException(message.toString(), location);
  }

  @TruffleBoundary
  public static NativeCellException arity(String function, int expected, int actual, Node node) {
    return create(function + " expects " + expected + " arguments, got " + actual, node);
  }

  @TruffleBoundary
  public static NativeCellException type(
      String function, String parameter, String expected, Object actual, Node node) {
    return create(
        function
            + ": parameter "
            + parameter
            + " expects "
            + expected
            + ", got "
            + describe(actual),
        node);
  }

  /** Describes a value without invoking guest behavior. */
  static String describe(Object value) {
    if (value == null) return "null";
    if (value instanceof Number || value instanceof Boolean || value instanceof Character)
      return value.getClass().getSimpleName() + " " + value;
    if (value instanceof String s) return "string \"" + s + "\"";
    return value.getClass().getSimpleName();
  }
}
