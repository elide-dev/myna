// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package dev.elide.myna.truffle;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;

/**
 * Coerces any interop number to {@code double}; generated cells cast to {@code float} for {@code
 * f32}. Rounding a double into a float is the C semantics of the call, not a coercion failure.
 */
public final class FloatingCoercion extends Coercion {
  public FloatingCoercion(String function, String parameter) {
    super(function, parameter);
  }

  public double execute(Object value) {
    if (value instanceof Double d) return d;
    if (value instanceof Integer i) return i;
    if (value instanceof Long l) return l;
    if (value instanceof Float f) return f;
    if (value == null) throw mismatch("number", value);
    InteropLibrary interop = interop();
    try {
      if (interop.fitsInDouble(value)) return interop.asDouble(value);
      if (interop.fitsInLong(value)) return interop.asLong(value);
    } catch (UnsupportedMessageException e) {
      throw CompilerDirectives.shouldNotReachHere(e);
    }
    throw mismatch("number", value);
  }
}
