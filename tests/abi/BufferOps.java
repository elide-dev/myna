package fixture;

import dev.elide.seam.generated.SeamNative;
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
    return SeamNative.inspect(slot);
  }

  /** Sign of {@code value}; -128 for zero when strict. Fails loudly if the leaf import mis-extends. */
  public static byte classify(short value, boolean strict) {
    if (value == 12345) {
      int mask = 0;
      if (SeamNative.widen((byte) -1, (byte) 0, (short) 0, (short) 0, false) == -1) mask |= 1;
      if (SeamNative.widen((byte) 0, (byte) 255, (short) 0, (short) 0, false) == 255) mask |= 2;
      if (SeamNative.widen((byte) 0, (byte) 0, (short) -2, (short) 0, false) == -2) mask |= 4;
      if (SeamNative.widen((byte) 0, (byte) 0, (short) 0, (short) 65535, false) == 65535) mask |= 8;
      if (SeamNative.widen((byte) 0, (byte) 0, (short) 0, (short) 0, true) == 1000) mask |= 16;
      if (SeamNative.lowUnsigned(0x1ff) == (byte) 0xff) mask |= 32;
      if (SeamNative.lowSigned(0x1ff) == (byte) -1) mask |= 64;
      return (byte) mask;
    }
    if (SeamNative.widen((byte) -1, (byte) 255, (short) -2, (short) 65535, true) != 66787)
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
        SeamNative.forEach(
            SeamNative.Literals.visit.getFunctionPointer(), CurrentIsolate.getCurrentThread(), 5);
    return SeamNative.reducer().invoke(visited, 7);
  }

  public static void scale(PointerBase value, int factor, PointerBase result) {
    SeamNative.Pair.left(result, SeamNative.Pair.left(value) * factor);
    SeamNative.Pair.right(result, SeamNative.Pair.right(value) * factor);
  }

  public static void spread(PointerBase value, PointerBase result) {
    long a = SeamNative.Triple.a(value);
    SeamNative.Triple.a(result, a);
    SeamNative.Triple.b(result, a + SeamNative.Triple.b(value));
    SeamNative.Triple.c(result, a + SeamNative.Triple.c(value));
  }

  /** Java -> Rust by-value imports through their reference-form shims; one bit per check. */
  public static int structProbe() {
    PointerBase pair = StackValue.get(SeamNative.Pair.SIZE);
    PointerBase swapped = StackValue.get(SeamNative.Pair.SIZE);
    SeamNative.Pair.left(pair, -5L);
    SeamNative.Pair.right(pair, 1 << 30);
    SeamNative.swapPair(pair, swapped);
    int mask = 0;
    if (SeamNative.Pair.left(swapped) == 1 << 30 && SeamNative.Pair.right(swapped) == -5) mask |= 1;
    PointerBase triple = StackValue.get(SeamNative.Triple.SIZE);
    PointerBase rotated = StackValue.get(SeamNative.Triple.SIZE);
    SeamNative.Triple.a(triple, 1);
    SeamNative.Triple.b(triple, 2);
    SeamNative.Triple.c(triple, Long.MIN_VALUE);
    SeamNative.rotate(triple, 1, rotated);
    if (SeamNative.Triple.a(rotated) == 2
        && SeamNative.Triple.b(rotated) == Long.MIN_VALUE
        && SeamNative.Triple.c(rotated) == 1) mask |= 2;
    return mask;
  }
}
