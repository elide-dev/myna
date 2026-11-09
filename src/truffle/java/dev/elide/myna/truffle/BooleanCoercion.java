// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package dev.elide.myna.truffle;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;

/** Coerces an interop boolean to the C {@code bool} carrier. Numbers are not truthy here. */
public final class BooleanCoercion extends Coercion {
  public BooleanCoercion(String function, String parameter) {
    super(function, parameter);
  }

  public boolean execute(Object value) {
    if (value instanceof Boolean b) return b;
    if (value == null) throw mismatch("bool", value);
    InteropLibrary interop = interop();
    if (interop.isBoolean(value)) {
      try {
        return interop.asBoolean(value);
      } catch (UnsupportedMessageException e) {
        throw CompilerDirectives.shouldNotReachHere(e);
      }
    }
    throw mismatch("bool", value);
  }
}
