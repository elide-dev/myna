// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package dev.elide.myna.truffle;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.exception.AbstractTruffleException;
import com.oracle.truffle.api.nodes.Node;

/**
 * A guest-visible error raised by a native cell: wrong arity, an argument that does not coerce to
 * the declared carrier, or misuse of a borrowed buffer or owned resource. Guest languages observe
 * it as an ordinary runtime error. The one native outcome a cell reports is a resource create that
 * says no resource exists: that is a {@link NativeStatusException}, which carries the result.
 */
public sealed class NativeCellException extends AbstractTruffleException
    permits NativeStatusException {
  private static final long serialVersionUID = 1L;

  NativeCellException(String message, Node location) {
    super(message, location);
  }

  /**
   * A resource create returned a result that its seam does not list as success. Built behind a
   * boundary and without leaving compiled code: a failed create is an ordinary outcome.
   */
  @TruffleBoundary
  public static NativeStatusException status(String function, long status, Node location) {
    return new NativeStatusException(
        function + " created no resource: status " + status, location, status);
  }

  /** A resource create reported success and wrote no resource. */
  @TruffleBoundary
  public static NativeCellException noResource(String function, Node location) {
    return create(function + " reported success but wrote no resource", location);
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
