// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package dev.elide.myna.truffle;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;

/**
 * Coerces an interop pointer ({@link NativePointer}, {@link NativeBytes}, or any other object
 * answering {@code isPointer}) to a raw address. An open {@link NativeResource} is accepted only
 * where the parameter points to that declared resource. Interop {@code null} is accepted only where
 * the seam declares the parameter nullable; plain numbers only where the binding opted in with
 * {@code truffle_raw_pointers}.
 */
public final class PointerCoercion extends Coercion {
  private final boolean nullable;
  private final boolean rawNumbers;
  private final String resource;

  public PointerCoercion(String function, String parameter, boolean nullable, boolean rawNumbers) {
    this(function, parameter, nullable, rawNumbers, null);
  }

  /**
   * @param resource the declared resource type the parameter points to, or {@code null} when it
   *     points to anything else. A {@link NativeResource} of another type is rejected: its address
   *     is a different native object, and the callee would read it as this one. Without a resource
   *     every owner is rejected, so a live resource's state never reaches an unrelated function.
   */
  public PointerCoercion(
      String function, String parameter, boolean nullable, boolean rawNumbers, String resource) {
    super(function, parameter);
    this.nullable = nullable;
    this.rawNumbers = rawNumbers;
    this.resource = resource;
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
    if (value instanceof NativeResource owner) {
      if (resource == null) throw unexpectedOwner(owner);
      if (!sameType(resource, owner.type())) throw wrongResource(owner);
      if (owner.isClosed()) throw closed(owner);
    }
    InteropLibrary interop = interop();
    try {
      if (interop.isPointer(value)) return checked(interop.asPointer(value), value);
      if (interop.isNull(value)) return checked(0, value);
      if (rawNumbers && interop.fitsInLong(value)) return checked(interop.asLong(value), value);
    } catch (UnsupportedMessageException e) {
      // A shared NativeResource can be closed by another thread between the isPointer() guard and
      // the asPointer() dispatch; that is a normal interop outcome, not an internal invariant.
      if (value instanceof NativeResource resource) throw closed(resource);
      throw CompilerDirectives.shouldNotReachHere(e);
    }
    // Closed after the first check but before isPointer(): report the close, not a type error.
    if (value instanceof NativeResource resource && resource.isClosed()) throw closed(resource);
    throw mismatch(nullable ? "pointer or null" : "pointer", value);
  }

  @CompilerDirectives.TruffleBoundary
  private static boolean sameType(String expected, String actual) {
    return expected.equals(actual);
  }

  private NativeCellException wrongResource(NativeResource owner) {
    CompilerDirectives.transferToInterpreter();
    return NativeCellException.of(
        this,
        function,
        ": parameter ",
        parameter,
        " expects a ",
        resource,
        " resource, got a ",
        owner.type());
  }

  private NativeCellException closed(NativeResource resource) {
    CompilerDirectives.transferToInterpreter();
    return NativeCellException.of(
        this, function, ": parameter ", parameter, " received a closed ", resource.type());
  }
}
