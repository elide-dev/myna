package bench;
import org.graalvm.nativeimage.c.function.CFunction;
public final class Native {
  @CFunction("myna_bench_step") static native long manual(long value);
  @CFunction(value="myna_bench_leaf", transition=CFunction.Transition.NO_TRANSITION)
  static native long leaf(long value);
  public static void main(String[] args) throws Throwable {
    String name=args[0]; int count=Integer.parseInt(args[1]);
    Measure.Loop loop = switch (name) {
      case "control" -> Measure::reference;
      case "generated-normal" -> (n, x) -> { for(int i=0;i<n;++i) x=Generated.step(x); return x; };
      case "manual-normal" -> (n, x) -> { for(int i=0;i<n;++i) x=manual(x); return x; };
      case "generated-leaf" -> (n, x) -> { for(int i=0;i<n;++i) x=Generated.leaf(x); return x; };
      case "manual-leaf" -> (n, x) -> { for(int i=0;i<n;++i) x=leaf(x); return x; };
      default -> throw new IllegalArgumentException(name);
    };
    Measure.run(name, count, loop);
  }
}
