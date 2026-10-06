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
    }
    System.out.println("AOT FFM -> Rust passed");
  }
}
