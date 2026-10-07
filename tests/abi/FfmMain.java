import dev.elide.seam.generated.SeamFFM;
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
        if (SeamFFM.inspect(value) != 42) throw new AssertionError("typed FFM result");
      }
      // Records by value: FFM lowers the C ABI itself, unlike Native Image @CFunction.
      var triple = arena.allocate(SeamFFM.Triple_LAYOUT);
      triple.set(ValueLayout.JAVA_LONG, SeamFFM.Triple_a_OFFSET, 1L);
      triple.set(ValueLayout.JAVA_LONG, SeamFFM.Triple_b_OFFSET, 2L);
      triple.set(ValueLayout.JAVA_LONG, SeamFFM.Triple_c_OFFSET, 3L);
      var rotated = SeamFFM.rotate(arena, triple, 1);
      if (rotated.get(ValueLayout.JAVA_LONG, SeamFFM.Triple_a_OFFSET) != 2
          || rotated.get(ValueLayout.JAVA_LONG, SeamFFM.Triple_c_OFFSET) != 1)
        throw new AssertionError("by-value FFM record");
    }
    System.out.println("AOT FFM -> Rust passed");
  }
}
