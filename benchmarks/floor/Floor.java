package bench;
import org.graalvm.nativeimage.c.function.CFunction;
import com.oracle.svm.shared.Uninterruptible;
import com.oracle.svm.shared.AlwaysInline;
import org.graalvm.nativeimage.c.function.CFunctionPointer;
import org.graalvm.nativeimage.c.function.InvokeCFunctionPointer;

/** Bounded uninterruptible caller experiments remain separate from the portable generated API. */
public final class Floor {
  @CFunction(value="myna_bench_leaf", transition=CFunction.Transition.NO_TRANSITION)
  static native long leaf(long value);
  @Uninterruptible(reason="Fixed 4-call batch of explicit bounded native leaves")
  static long javaBatch4(long x) { for(int i=0;i<4;i++) x=Generated.leaf(x); return x; }
  @Uninterruptible(reason="Fixed 16-call batch of explicit bounded native leaves")
  static long javaBatch16(long x) { for(int i=0;i<16;i++) x=Generated.leaf(x); return x; }
  @Uninterruptible(reason="Fixed 64-call batch of explicit bounded native leaves")
  static long javaBatch64(long x) { for(int i=0;i<64;i++) x=Generated.leaf(x); return x; }
  @Uninterruptible(reason="Fixed 256-call batch of explicit bounded native leaves")
  static long javaBatch256(long x) { for(int i=0;i<256;i++) x=Generated.leaf(x); return x; }
  interface InlineLeaf extends CFunctionPointer {
    @AlwaysInline("Primitive-only bounded leaf pointer call")
    @Uninterruptible(reason="Atomic primitive-only native call with no managed references", mayBeInlined=true)
    @InvokeCFunctionPointer(transition=CFunction.Transition.NO_TRANSITION)
    long invoke(long x);
  }
  static InlineLeaf pointer;
  @CFunction("myna_bench_leaf_address") static native InlineLeaf leafAddress();
  @CFunction("myna_bench_identity_address") static native InlineLeaf identityAddress();
  @CFunction(value="myna_bench_identity", transition=CFunction.Transition.NO_TRANSITION)
  static native long identity(long value);
  public static void main(String[] args) throws Throwable {
    String name=args[0]; int count=Integer.parseInt(args[1]);
    if (count <= 0 || count % 256 != 0) throw new IllegalArgumentException("count must be a positive multiple of 256");
    pointer=name.startsWith("identity-") ? identityAddress() : leafAddress();
    Measure.Loop loop = switch(name) {
      case "inline-pointer" -> (n,x) -> { InlineLeaf fn=pointer; for(int i=0;i<n;i++) x=fn.invoke(x); return x; };
      case "identity-generated" -> (n,x) -> { for(int i=0;i<n;i++) x=Identity.leaf(x); return x; };
      case "identity-manual" -> (n,x) -> { for(int i=0;i<n;i++) x=identity(x); return x; };
      case "identity-inline-pointer" -> (n,x) -> { InlineLeaf fn=pointer; for(int i=0;i<n;i++) x=fn.invoke(x); return x; };
      case "control" -> Measure::reference;
      case "generated-normal" -> (n,x) -> { for(int i=0;i<n;i++) x=Generated.step(x); return x; };
      case "generated-leaf" -> (n,x) -> { for(int i=0;i<n;i++) x=Generated.leaf(x); return x; };
      case "manual-leaf" -> (n,x) -> { for(int i=0;i<n;i++) x=leaf(x); return x; };
      case "java-batch-4" -> (n,x) -> { for(int i=0;i<n;i+=4) x=javaBatch4(x); return x; };
      case "java-batch-16" -> (n,x) -> { for(int i=0;i<n;i+=16) x=javaBatch16(x); return x; };
      case "java-batch-64" -> (n,x) -> { for(int i=0;i<n;i+=64) x=javaBatch64(x); return x; };
      case "java-batch-256" -> (n,x) -> { for(int i=0;i<n;i+=256) x=javaBatch256(x); return x; };
      case "batch-4" -> (n,x) -> { for(int i=0;i<n;i+=4) x=Batch.batch4(x); return x; };
      case "batch-16" -> (n,x) -> { for(int i=0;i<n;i+=16) x=Batch.batch16(x); return x; };
      case "batch-64" -> (n,x) -> { for(int i=0;i<n;i+=64) x=Batch.batch64(x); return x; };
      case "batch-256" -> (n,x) -> { for(int i=0;i<n;i+=256) x=Batch.batch256(x); return x; };
      default -> throw new IllegalArgumentException(name);
    };
    FloorMeasure.run(name, count, loop);
  }
}
