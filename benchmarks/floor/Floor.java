package bench;
import org.graalvm.nativeimage.c.function.CFunction;
import com.oracle.svm.shared.Uninterruptible;
import example.leaf.LongLeaf;
import example.leaf.LeafBindings;

/** Bounded uninterruptible caller experiments remain separate from the portable generated API. */
public final class Floor {
  @CFunction(value="myna_bench_leaf", transition=CFunction.Transition.NO_TRANSITION)
  static native long leaf(long value);
  @Uninterruptible(reason="Fixed 4-call batch of explicit bounded native leaves")
  static long javaBatch4(Generated.leaf_Leaf fn, long x) { for(int i=0;i<4;i++) x=fn.invoke(x); return x; }
  @Uninterruptible(reason="Fixed 16-call batch of explicit bounded native leaves")
  static long javaBatch16(Generated.leaf_Leaf fn, long x) { for(int i=0;i<16;i++) x=fn.invoke(x); return x; }
  @Uninterruptible(reason="Fixed 64-call batch of explicit bounded native leaves")
  static long javaBatch64(Generated.leaf_Leaf fn, long x) { for(int i=0;i<64;i++) x=fn.invoke(x); return x; }
  @Uninterruptible(reason="Fixed 256-call batch of explicit bounded native leaves")
  static long javaBatch256(Generated.leaf_Leaf fn, long x) { for(int i=0;i<256;i++) x=fn.invoke(x); return x; }
  static LongLeaf pointer;
  static Generated.leaf_Leaf defaultPointer;
  @CFunction("myna_bench_identity_address") static native LongLeaf identityAddress();
  @CFunction(value="myna_bench_identity", transition=CFunction.Transition.NO_TRANSITION)
  static native long identity(long value);
  static volatile byte[] allocation;
  static void stress() throws Exception {
    LongLeaf fn=LeafBindings.resolve();
    var generated=Generated.leafLeafPointer();
    long before=java.lang.management.ManagementFactory.getGarbageCollectorMXBeans().stream()
        .mapToLong(b -> Math.max(0,b.getCollectionCount())).sum();
    var failure=new java.util.concurrent.atomic.AtomicReference<Throwable>();
    var ready=new java.util.concurrent.CountDownLatch(1);
    Thread collector=new Thread(() -> {
      try {
        ready.countDown();
        for(int i=0;i<512;i++) { allocation=new byte[65536]; if(i%16==0) System.gc(); }
      } catch(Throwable t) { failure.set(t); }
    });
    collector.start(); ready.await();
    long x=123456789L, expected=x;
    for(int i=0;i<1048576 || collector.isAlive();i++) {
      x=fn.invoke(x); expected=expected*1664525L+1013904223L;
      if(x!=expected) throw new AssertionError("leaf during GC");
      if(i%256==0 && javaBatch256(generated,x)!=Measure.reference(256,x)) throw new AssertionError("bounded batch during GC");
    }
    collector.join();
    if(failure.get()!=null) throw new AssertionError(failure.get());
    long after=java.lang.management.ManagementFactory.getGarbageCollectorMXBeans().stream()
        .mapToLong(b -> Math.max(0,b.getCollectionCount())).sum();
    if(after<=before) throw new AssertionError("No GC observed during stress test");
    System.out.println("Leaf and bounded-batch GC stress passed; collections="+(after-before));
  }
  public static void main(String[] args) throws Throwable {
    if(args[0].equals("stress")) { stress(); return; }
    String name=args[0]; int count=Integer.parseInt(args[1]);
    if (count <= 0 || count % 256 != 0) throw new IllegalArgumentException("count must be a positive multiple of 256");
    defaultPointer=Generated.leafLeafPointer();
    pointer=name.startsWith("identity-") ? identityAddress() : LeafBindings.resolve();
    Measure.Loop loop = switch(name) {
      case "inline-pointer" -> (n,x) -> { LongLeaf fn=pointer; for(int i=0;i<n;i++) x=fn.invoke(x); return x; };
      case "identity-generated" -> (n,x) -> { for(int i=0;i<n;i++) x=Identity.leaf(x); return x; };
      case "identity-manual" -> (n,x) -> { for(int i=0;i<n;i++) x=identity(x); return x; };
      case "identity-inline-pointer" -> (n,x) -> { LongLeaf fn=pointer; for(int i=0;i<n;i++) x=fn.invoke(x); return x; };
      case "inline-pointer-u4" -> (n,x) -> { LongLeaf fn=pointer; for(int i=0;i<n;i+=4) { x=fn.invoke(x); x=fn.invoke(x); x=fn.invoke(x); x=fn.invoke(x); } return x; };
      case "identity-inline-pointer-u4" -> (n,x) -> { LongLeaf fn=pointer; for(int i=0;i<n;i+=4) { x=fn.invoke(x); x=fn.invoke(x); x=fn.invoke(x); x=fn.invoke(x); } return x; };
      case "inline-pointer-u16" -> (n,x) -> { LongLeaf fn=pointer; for(int i=0;i<n;i+=16) { x=fn.invoke(x); x=fn.invoke(x); x=fn.invoke(x); x=fn.invoke(x); x=fn.invoke(x); x=fn.invoke(x); x=fn.invoke(x); x=fn.invoke(x); x=fn.invoke(x); x=fn.invoke(x); x=fn.invoke(x); x=fn.invoke(x); x=fn.invoke(x); x=fn.invoke(x); x=fn.invoke(x); x=fn.invoke(x); } return x; };
      case "identity-inline-pointer-u16" -> (n,x) -> { LongLeaf fn=pointer; for(int i=0;i<n;i+=16) { x=fn.invoke(x); x=fn.invoke(x); x=fn.invoke(x); x=fn.invoke(x); x=fn.invoke(x); x=fn.invoke(x); x=fn.invoke(x); x=fn.invoke(x); x=fn.invoke(x); x=fn.invoke(x); x=fn.invoke(x); x=fn.invoke(x); x=fn.invoke(x); x=fn.invoke(x); x=fn.invoke(x); x=fn.invoke(x); } return x; };
      case "control" -> Measure::reference;
      case "generated-normal" -> (n,x) -> { for(int i=0;i<n;i++) x=Generated.step(x); return x; };
      case "generated-leaf" -> (n,x) -> { for(int i=0;i<n;i++) x=Generated.leaf(x); return x; };
      case "manual-leaf" -> (n,x) -> { for(int i=0;i<n;i++) x=leaf(x); return x; };
      case "java-batch-4" -> (n,x) -> { for(int i=0;i<n;i+=4) x=javaBatch4(defaultPointer,x); return x; };
      case "java-batch-16" -> (n,x) -> { for(int i=0;i<n;i+=16) x=javaBatch16(defaultPointer,x); return x; };
      case "java-batch-64" -> (n,x) -> { for(int i=0;i<n;i+=64) x=javaBatch64(defaultPointer,x); return x; };
      case "java-batch-256" -> (n,x) -> { for(int i=0;i<n;i+=256) x=javaBatch256(defaultPointer,x); return x; };
      case "batch-4" -> (n,x) -> { for(int i=0;i<n;i+=4) x=Batch.batch4(x); return x; };
      case "batch-16" -> (n,x) -> { for(int i=0;i<n;i+=16) x=Batch.batch16(x); return x; };
      case "batch-64" -> (n,x) -> { for(int i=0;i<n;i+=64) x=Batch.batch64(x); return x; };
      case "batch-256" -> (n,x) -> { for(int i=0;i<n;i+=256) x=Batch.batch256(x); return x; };
      default -> throw new IllegalArgumentException(name);
    };
    FloorMeasure.run(name, count, loop);
  }
}
