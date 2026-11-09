// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package dev.elide.myna.truffle;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;

/**
 * Coerces an interop pointer ({@link NativePointer}, an open {@link NativeResource}, {@link
 * NativeBytes}, or any other object answering {@code isPointer}) to a raw address. Interop {@code
 * null} is accepted only where the seam declares the parameter nullable; plain numbers only where
 * the binding opted in with {@code truffle_raw_pointers}.
 */
public final class PointerCoercion extends Coercion {
  private final boolean nullable;
  private final boolean rawNumbers;

  public PointerCoercion(String function, String parameter, boolean nullable, boolean rawNumbers) {
    super(function, parameter);
    this.nullable = nullable;
    this.rawNumbers = rawNumbers;
  }

  public long execute(Object value) {
    if (value instanceof NativePointer p) return checked(p.address(), value);
    return slow(value);
  }

  private long checked(long address, Object value) {
    if (address == 0 && !nullable) throw mismatch("non-null pointer", value);
    return address;
  }

  private long slow(Object value) {
    if (value == null) return checked(0, value);
    if (value instanceof NativeResource resource && resource.isClosed()) {
      CompilerDirectives.transferToInterpreter();
      throw NativeCellException.of(
          this, function, ": parameter ", parameter, " received a closed ", resource.type());
    }
    InteropLibrary interop = interop();
    try {
      if (interop.isPointer(value)) return checked(interop.asPointer(value), value);
      if (interop.isNull(value)) return checked(0, value);
      if (rawNumbers && interop.fitsInLong(value)) return checked(interop.asLong(value), value);
    } catch (UnsupportedMessageException e) {
      // A shared NativeResource can be closed by another thread between the isPointer() guard and
      // the asPointer() dispatch; that is a normal interop outcome, not an internal invariant.
      if (value instanceof NativeResource resource) {
        CompilerDirectives.transferToInterpreter();
        throw NativeCellException.of(
            this, function, ": parameter ", parameter, " received a closed ", resource.type());
      }
      throw CompilerDirectives.shouldNotReachHere(e);
    }
    throw mismatch(nullable ? "pointer or null" : "pointer", value);
  }
}
