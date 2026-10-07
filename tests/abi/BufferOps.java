package fixture;

import dev.elide.myna.generated.MynaNative;
import org.graalvm.nativeimage.CurrentIsolate;
import org.graalvm.nativeimage.StackValue;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CLongPointer;
import org.graalvm.word.PointerBase;

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

  public static void scale(PointerBase value, int factor, PointerBase result) {
    MynaNative.Pair.left(result, MynaNative.Pair.left(value) * factor);
    MynaNative.Pair.right(result, MynaNative.Pair.right(value) * factor);
  }

  public static void spread(PointerBase value, PointerBase result) {
    long a = MynaNative.Triple.a(value);
    MynaNative.Triple.a(result, a);
    MynaNative.Triple.b(result, a + MynaNative.Triple.b(value));
    MynaNative.Triple.c(result, a + MynaNative.Triple.c(value));
  }

  /** Java -> Rust by-value imports through their reference-form shims; one bit per check. */
  public static int structProbe() {
    PointerBase pair = StackValue.get(MynaNative.Pair.SIZE);
    PointerBase swapped = StackValue.get(MynaNative.Pair.SIZE);
    MynaNative.Pair.left(pair, -5L);
    MynaNative.Pair.right(pair, 1 << 30);
    MynaNative.swapPair(pair, swapped);
    int mask = 0;
    if (MynaNative.Pair.left(swapped) == 1 << 30 && MynaNative.Pair.right(swapped) == -5) mask |= 1;
    PointerBase triple = StackValue.get(MynaNative.Triple.SIZE);
    PointerBase rotated = StackValue.get(MynaNative.Triple.SIZE);
    MynaNative.Triple.a(triple, 1);
    MynaNative.Triple.b(triple, 2);
    MynaNative.Triple.c(triple, Long.MIN_VALUE);
    MynaNative.rotate(triple, 1, rotated);
    if (MynaNative.Triple.a(rotated) == 2
        && MynaNative.Triple.b(rotated) == Long.MIN_VALUE
        && MynaNative.Triple.c(rotated) == 1) mask |= 2;
    return mask;
  }
}
