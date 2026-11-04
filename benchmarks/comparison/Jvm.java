package bench;
import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
public final class Jvm {
  static native long step(long value);
  static final MethodHandle manual;
  static final MemorySegment address;
  static final class Unbound {
    static final MethodHandle HANDLE = Linker.nativeLinker().downcallHandle(
        FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG));
  }
  static {
    System.load(System.getProperty("myna.bench.library"));
    address = SymbolLookup.loaderLookup().find("myna_bench_step").orElseThrow();
    manual = Linker.nativeLinker().downcallHandle(
        address,
        FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG));
  }
  public static void main(String[] args) throws Throwable {
    String name = args[0]; int count = Integer.parseInt(args[1]);
    Measure.Loop loop = switch (name) {
      case "control" -> Measure::reference;
      case "generated-ffm" -> (n, x) -> { for (int i=0; i<n; ++i) x=GeneratedFFM.step(x); return x; };
      case "manual-ffm" -> (n, x) -> { for (int i=0; i<n; ++i) x=(long)Unbound.HANDLE.invokeExact(address, x); return x; };
      case "manual-bound-ffm" -> (n, x) -> { for (int i=0; i<n; ++i) x=(long)manual.invokeExact(x); return x; };
      case "jni" -> (n, x) -> { for (int i=0; i<n; ++i) x=step(x); return x; };
      default -> throw new IllegalArgumentException(name);
    };
    Measure.run(name, count, loop);
  }
}
