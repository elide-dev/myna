// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package fixture;

import dev.elide.myna.generated.MynaNative;
import org.graalvm.nativeimage.CurrentIsolate;
import org.graalvm.nativeimage.StackValue;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CLongPointer;

/** End-to-end fixture: Rust caller -> Native Image sum -> Rust inspect. */
public final class BufferOps {
  public static long sum(CCharPointer data, long length) {
    if (length < 0) throw new IllegalArgumentException("negative length");
    long result = 0;
    for (long i = 0; i < length; i++) result += data.read((int) i) & 0xff;
    CLongPointer slot = StackValue.get(8);
    slot.write(result);
    return MynaNative.inspect(slot);
  }

  /** Sign of {@code value}; -128 for zero when strict. Fails loudly if the leaf import mis-extends. */
  public static byte classify(short value, boolean strict) {
    if (value == 12345) {
      int mask = 0;
      if (MynaNative.widen((byte) -1, (byte) 0, (short) 0, (short) 0, false) == -1) mask |= 1;
      if (MynaNative.widen((byte) 0, (byte) 255, (short) 0, (short) 0, false) == 255) mask |= 2;
      if (MynaNative.widen((byte) 0, (byte) 0, (short) -2, (short) 0, false) == -2) mask |= 4;
      if (MynaNative.widen((byte) 0, (byte) 0, (short) 0, (short) 65535, false) == 65535) mask |= 8;
      if (MynaNative.widen((byte) 0, (byte) 0, (short) 0, (short) 0, true) == 1000) mask |= 16;
      if (MynaNative.lowUnsigned(0x1ff) == (byte) 0xff) mask |= 32;
      if (MynaNative.lowSigned(0x1ff) == (byte) -1) mask |= 64;
      return (byte) mask;
    }
    if (MynaNative.widen((byte) -1, (byte) 255, (short) -2, (short) 65535, true) != 66787)
      return 99;
    if (value == 0) return strict ? (byte) -128 : 0;
    return (byte) (value < 0 ? -1 : 1);
  }

  /** Unsigned bytes arrive as raw bits. */
  public static boolean isHigh(byte value) {
    return (value & 0xff) >= 128;
  }

  public static byte echo(byte value) {
    return value;
  }

  public static int visit(long value) {
    return (int) value * 2;
  }

  /** Rust calls back into {@link #visit} five times (sum 20), then Java calls Rust's reducer. */
  public static long exercise() {
    int visited =
        MynaNative.forEach(
            MynaNative.Literals.visit.getFunctionPointer(), CurrentIsolate.getCurrentThread(), 5);
    return MynaNative.reducer().invoke(visited, 7);
  }

  public static void scale(MynaNative.Pair value, int factor, MynaNative.Pair result) {
    result.left(value.left() * factor);
    result.right(value.right() * factor);
  }

  public static void spread(MynaNative.Triple value, MynaNative.Triple result) {
    long a = value.a();
    result.a(a);
    result.b(a + value.b());
    result.c(a + value.c());
  }

  /** Java -> Rust by-value imports through their reference-form shims; one bit per check. */
  public static int structProbe() {
    MynaNative.Pair pair = StackValue.get(MynaNative.Pair.class);
    MynaNative.Pair swapped = StackValue.get(MynaNative.Pair.SIZE);
    pair.left(-5L);
    pair.right(1 << 30);
    MynaNative.swapPair(pair, swapped);
    int mask = 0;
    if (swapped.left() == 1 << 30 && swapped.right() == -5) mask |= 1;
    MynaNative.Triple triple = StackValue.get(MynaNative.Triple.class);
    MynaNative.Triple rotated = StackValue.get(MynaNative.Triple.class);
    triple.a(1);
    triple.b(2);
    triple.c(Long.MIN_VALUE);
    MynaNative.rotate(triple, 1, rotated);
    if (rotated.a() == 2 && rotated.b() == Long.MIN_VALUE && rotated.c() == 1) mask |= 2;
    return mask;
  }
}
