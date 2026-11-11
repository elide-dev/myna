// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package dev.elide.myna.truffle;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.nodes.Node;

/**
 * Converts one guest argument to one Myna carrier. Coercions use {@link InteropLibrary} only, so
 * they are correct for every Truffle language; the interop library is inserted lazily, so a
 * monomorphic call site that only ever passes Java boxed primitives never dispatches through it.
 */
public abstract class Coercion extends Node {
  /** Dispatch limit before the interop library becomes megamorphic. */
  static final int INTEROP_LIMIT = 3;

  protected final String function;
  protected final String parameter;
  @Child private InteropLibrary interop;

  protected Coercion(String function, String parameter) {
    this.function = function;
    this.parameter = parameter;
  }

  protected final InteropLibrary interop() {
    InteropLibrary library = interop;
    if (library == null) {
      CompilerDirectives.transferToInterpreterAndInvalidate();
      library = insert(InteropLibrary.getFactory().createDispatched(INTEROP_LIMIT));
      interop = library;
    }
    return library;
  }

  /** A failing coercion is never a fast path: leave compiled code, then build the message. */
  protected final NativeCellException mismatch(String expected, Object actual) {
    CompilerDirectives.transferToInterpreter();
    return NativeCellException.type(function, parameter, expected, actual, this);
  }

  /**
   * An owner passed where no resource is declared. Its address is a live resource's internal state,
   * which only a parameter typed as a pointer to that resource may receive.
   */
  protected final NativeCellException unexpectedOwner(NativeResource owner) {
    CompilerDirectives.transferToInterpreter();
    return NativeCellException.of(
        this, function, ": parameter ", parameter, " takes no resource, got a ", owner.type());
  }

  public final String parameterName() {
    return parameter;
  }
}
