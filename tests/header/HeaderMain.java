// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package fixture.header;

import com.oracle.svm.shared.NeverInline;
import org.graalvm.nativeimage.StackValue;
import org.graalvm.nativeimage.c.struct.SizeOf;

/**
 * Bindings whose only C context is the generated one: @CStruct layouts and @CConstant values come
 * from the seam.h it embeds, and every accessor equals the descriptor's literal.
 */
public final class HeaderMain {
  private HeaderMain() {}

  private static void check(boolean condition, String what) {
    if (!condition) throw new AssertionError(what);
  }

  // Measured pairs: a @CConstant accessor beside the same literal. The test compares their code.
  @NeverInline("measured")
  static boolean magicCConstant(int value) {
    return value == HeaderApi.analysisMagic();
  }

  @NeverInline("measured")
  static boolean magicLiteral(int value) {
    return value == 0x3141514b;
  }

  @NeverInline("measured")
  static boolean levelCConstant(byte value) {
    return value == HeaderApi.level();
  }

  @NeverInline("measured")
  static boolean levelLiteral(byte value) {
    return value == (byte) 0xc8;
  }

  @NeverInline("measured")
  static long limitCConstant(long value) {
    return value & HeaderApi.limit();
  }

  @NeverInline("measured")
  static long limitLiteral(long value) {
    return value & 0xffffffffffffffffL;
  }

  public static void main(String[] args) {
    check(HeaderApi.analysisMagic() == 0x3141514b, "analysisMagic");
    check(HeaderApi.highBit() == 0x80000000, "highBit keeps bit 31");
    check(HeaderApi.limit() == 0xffffffffffffffffL, "limit keeps every bit");
    check(HeaderApi.floor() == Long.MIN_VALUE, "floor");
    check(HeaderApi.level() == (byte) 0xc8, "level narrows to its raw bits");
    check(HeaderApi.small() == Byte.MIN_VALUE, "small");
    check(HeaderApi.width() == (short) 0xffff, "width narrows to its raw bits");
    check(HeaderApi.depth() == Short.MIN_VALUE, "depth");
    check(HeaderApi.span() == 4096L, "span");
    // Both forms of each pair see the same arguments, unknown to the analysis (args is empty), so
    // neither can be specialized to a constant argument.
    int none = args.length;
    check(magicCConstant(0x3141514b + none) && !magicCConstant(none), "magic @CConstant");
    check(magicLiteral(0x3141514b + none) && !magicLiteral(none), "magic literal");
    check(levelCConstant((byte) (0xc8 + none)) && !levelCConstant((byte) none), "level @CConstant");
    check(levelLiteral((byte) (0xc8 + none)) && !levelLiteral((byte) none), "level literal");
    check(limitCConstant(5L + none) == 5L && limitCConstant(-1L + none) == -1L, "limit @CConstant");
    check(limitLiteral(5L + none) == 5L && limitLiteral(-1L + none) == -1L, "limit literal");
    // The layout comes from the embedded seam.h: no consumer context names it.
    check(SizeOf.get(HeaderApi.Pair.class) == HeaderApi.Pair.SIZE, "SizeOf");
    HeaderApi.Pair pair = StackValue.get(HeaderApi.Pair.class);
    pair.left(42);
    pair.status(1);
    check(pair.left() == 42 && pair.status() == 1, "Pair");
    System.out.println(
        "analysisMagic() == 0x3141514b: " + (HeaderApi.analysisMagic() == 0x3141514b));
    System.out.println("Generated C context, header check and @CConstant accessors passed");
  }
}
