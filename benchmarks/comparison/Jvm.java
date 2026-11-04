package bench;
import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
public final class Jvm {
  static native long step(long value);
  static final MethodHandle manual;
  static {
    System.load(System.getProperty("myna.bench.library"));
    manual = Linker.nativeLinker().downcallHandle(
        SymbolLookup.loaderLookup().find("myna_bench_step").orElseThrow(),
        FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG));
  }
  public static void main(String[] args) throws Throwable {
    String name = args[0]; int count = Integer.parseInt(args[1]);
    Measure.Loop loop = switch (name) {
      case "control" -> Measure::reference;
      case "generated-ffm" -> (n, x) -> { for (int i=0; i<n; ++i) x=GeneratedFFM.step(x); return x; };
      case "manual-ffm" -> (n, x) -> { for (int i=0; i<n; ++i) x=(long)manual.invokeExact(x); return x; };
      case "jni" -> (n, x) -> { for (int i=0; i<n; ++i) x=step(x); return x; };
      default -> throw new IllegalArgumentException(name);
    };
    Measure.run(name, count, loop);
  }
}
