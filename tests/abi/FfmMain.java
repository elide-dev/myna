import dev.elide.myna.generated.MynaFFM;
import java.lang.foreign.Arena;
import java.lang.foreign.ValueLayout;

/** The same generated, typed FFM callsite runs on a JVM and as an AOT Native Image. */
public final class FfmMain {
  public static void main(String[] args) throws Throwable {
    System.load(args[0]);
    try (var arena = Arena.ofConfined()) {
      var value = arena.allocate(ValueLayout.JAVA_LONG);
      value.set(ValueLayout.JAVA_LONG, 0, 42L);
      for (int i = 0; i < 100; i++) {
        if (MynaFFM.inspect(value) != 42) throw new AssertionError("typed FFM result");
      }
      // Records by value: FFM lowers the C ABI itself, unlike Native Image @CFunction.
      var triple = arena.allocate(MynaFFM.Triple_LAYOUT);
      triple.set(ValueLayout.JAVA_LONG, MynaFFM.Triple_a_OFFSET, 1L);
      triple.set(ValueLayout.JAVA_LONG, MynaFFM.Triple_b_OFFSET, 2L);
      triple.set(ValueLayout.JAVA_LONG, MynaFFM.Triple_c_OFFSET, 3L);
      var rotated = MynaFFM.rotate(arena, triple, 1);
      if (rotated.get(ValueLayout.JAVA_LONG, MynaFFM.Triple_a_OFFSET) != 2
          || rotated.get(ValueLayout.JAVA_LONG, MynaFFM.Triple_c_OFFSET) != 1)
        throw new AssertionError("by-value FFM record");
      // Sub-32-bit unsigned parameters must arrive zero-extended (zeroext), not sign-extended;
      // an optimized callee trusts the caller's extension and reads the full register.
      if (MynaFFM.widen((byte) 0, (byte) 200, (short) 0, (short) 0, false) != 200)
        throw new AssertionError("ffm u8 zero-extend");
      if (MynaFFM.widen((byte) 0, (byte) 0, (short) 0, (short) 0xFFFF, false) != 65535)
        throw new AssertionError("ffm u16 zero-extend");
      if (MynaFFM.widen((byte) -1, (byte) 255, (short) -2, (short) 65535, true) != 66787)
        throw new AssertionError("ffm narrow aggregate");
    }
    System.out.println("AOT FFM -> Rust passed");
  }
}
