// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package dev.elide.myna.truffle;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import java.math.BigInteger;

/**
 * Coerces a guest number to an 8-, 16-, 32- or 64-bit integer carrier. The result is a {@code long}
 * whose low bits are the carrier value: signed carriers are sign-extended, unsigned carriers are
 * zero-extended, and the generated cell narrows with a plain cast, which preserves the raw bits
 * that Myna's unsigned carriers expect.
 */
public final class IntegerCoercion extends Coercion {
  private final int bits;
  private final boolean unsigned;
  private final CoercionPolicy policy;
  private final long min;
  private final long max;

  public IntegerCoercion(
      String function, String parameter, int bits, boolean unsigned, CoercionPolicy policy) {
    super(function, parameter);
    if (bits != 8 && bits != 16 && bits != 32 && bits != 64)
      throw new IllegalArgumentException("unsupported integer width: " + bits);
    this.bits = bits;
    this.unsigned = unsigned;
    this.policy = policy;
    if (unsigned) {
      this.min = 0;
      this.max = bits == 64 ? Long.MAX_VALUE : (1L << bits) - 1;
    } else {
      this.min = bits == 64 ? Long.MIN_VALUE : -(1L << (bits - 1));
      this.max = bits == 64 ? Long.MAX_VALUE : (1L << (bits - 1)) - 1;
    }
  }

  public int bits() {
    return bits;
  }

  public boolean unsigned() {
    return unsigned;
  }

  public long execute(Object value) {
    if (value instanceof Integer i) return narrow(i, value);
    if (value instanceof Long l) return narrow(l, value);
    // Host code passes raw java.math.BigInteger (the only lossless boxed Java type for u64/usize
    // magnitudes in [2^63, 2^64)) straight into a cell. Truffle interop does not recognize a plain
    // BigInteger (fitsInLong=false, fitsInBigInteger=false in both 25.5.5-elide.28 and upstream),
    // so route it through big() directly rather than letting it fall through every interop probe
    // in slow() and be rejected. Stays behind big()'s @TruffleBoundary like the interop path.
    if (value instanceof BigInteger b) return big(b, value);
    return slow(value);
  }

  private long narrow(long value, Object original) {
    if (value < min || value > max) {
      if (policy == CoercionPolicy.STRICT) throw mismatch(range(), original);
      return truncate(value);
    }
    return value;
  }

  private long truncate(long value) {
    if (bits == 64) return value;
    long masked = value & ((1L << bits) - 1);
    if (unsigned) return masked;
    long sign = 1L << (bits - 1);
    return (masked ^ sign) - sign;
  }

  private long slow(Object value) {
    if (value == null) throw mismatch(range(), value);
    InteropLibrary interop = interop();
    try {
      if (interop.fitsInLong(value)) return narrow(interop.asLong(value), value);
      if (interop.fitsInBigInteger(value)) return big(interop.asBigInteger(value), value);
      if (interop.fitsInDouble(value)) {
        double d = interop.asDouble(value);
        if (policy == CoercionPolicy.STRICT) throw mismatch(range(), value);
        if (Double.isNaN(d) || Double.isInfinite(d)) return 0;
        return truncate(Math.abs(d) < 0x1p63 ? (long) d : modular(d));
      }
    } catch (UnsupportedMessageException e) {
      throw CompilerDirectives.shouldNotReachHere(e);
    }
    throw mismatch(range(), value);
  }

  /** Doubles beyond the long range wrap modulo 2^64, as ToInt32 does, instead of saturating. */
  @CompilerDirectives.TruffleBoundary
  private static long modular(double d) {
    return new java.math.BigDecimal(d).toBigInteger().longValue();
  }

  /** Big-integer arithmetic stays out of compiled code. */
  @CompilerDirectives.TruffleBoundary
  private long big(BigInteger big, Object value) {
    // A BigInteger that fits in a long takes the same path a boxed Long would: narrow it to the
    // carrier, applying STRICT range checks or WRAP truncation as for Integer/Long. This branch is
    // dead for the interop path, where slow() only reaches big() when fitsInLong is false
    // (bitLength() >= 64); it makes a raw host BigInteger behave identically to a Truffle interop
    // number of the same magnitude, and lets STRICT signed/unsigned-narrower carriers accept a
    // BigInteger whose value is in range (e.g. BigInteger.valueOf(3) on a STRICT i64).
    if (big.bitLength() < 64) return narrow(big.longValue(), value);
    if (policy == CoercionPolicy.STRICT) {
      if (unsigned && bits == 64 && big.signum() >= 0 && big.bitLength() <= 64)
        return big.longValue();
      throw mismatch(range(), value);
    }
    return truncate(big.longValue());
  }

  @CompilerDirectives.TruffleBoundary
  private String range() {
    return (unsigned ? "u" : "i")
        + bits
        + " in ["
        + min
        + ", "
        + (unsigned && bits == 64 ? "2^64)" : max + "]");
  }
}
