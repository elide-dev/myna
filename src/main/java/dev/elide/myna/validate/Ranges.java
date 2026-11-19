// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package dev.elide.myna.validate;

import dev.elide.myna.model.Seam.Scalar;
import dev.elide.myna.model.Seam.TargetAbi;
import java.math.BigInteger;

/**
 * The value range of every integer carrier, shared by each rule that places a literal in a scalar.
 * {@code usize}/{@code isize} are 64-bit on every supported target; {@code char} follows the
 * target's C signedness.
 */
public final class Ranges {
  private Ranges() {}

  private static boolean unsigned(Scalar s, TargetAbi abi) {
    return switch (s) {
      case U8, U16, U32, U64, USIZE -> true;
      case CHAR -> abi.charUnsigned();
      case I8, I16, I32, I64, ISIZE -> false;
      default -> throw new IllegalArgumentException("not an integer scalar: " + s.text());
    };
  }

  public static BigInteger min(Scalar s, TargetAbi abi) {
    return unsigned(s, abi) ? BigInteger.ZERO : BigInteger.ONE.shiftLeft(s.bytes * 8 - 1).negate();
  }

  public static BigInteger max(Scalar s, TargetAbi abi) {
    int bits = s.bytes * 8;
    return BigInteger.ONE.shiftLeft(unsigned(s, abi) ? bits : bits - 1).subtract(BigInteger.ONE);
  }

  public static boolean fits(Scalar s, BigInteger value, TargetAbi abi) {
    return value.compareTo(min(s, abi)) >= 0 && value.compareTo(max(s, abi)) <= 0;
  }
}
