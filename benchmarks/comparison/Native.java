// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package bench;
import org.graalvm.nativeimage.c.function.CFunction;
public final class Native {
  @CFunction("myna_bench_step") static native long manual(long value);
  @CFunction(value="myna_bench_leaf", transition=CFunction.Transition.NO_TRANSITION)
  static native long leaf(long value);
  static long generatedNormal(int n, long x) {
    for(int i=0;i<n;++i) x=Generated.step(x); return x;
  }
  static long manualNormal(int n, long x) {
    for(int i=0;i<n;++i) x=manual(x); return x;
  }
  static long generatedLeaf(int n, long x) {
    for(int i=0;i<n;++i) x=Generated.leaf(x); return x;
  }
  static long manualLeaf(int n, long x) {
    for(int i=0;i<n;++i) x=leaf(x); return x;
  }
  static long generatedStaticJni(int n, long x) {
    for(int i=0;i<n;++i) x=GeneratedJNI.step(x); return x;
  }
  static long manualStaticJni(int n, long x) {
    for(int i=0;i<n;++i) x=ManualJNI.step(x); return x;
  }
  static final class GeneratedNormal implements Measure.Loop {
    public long run(int n, long x) { return generatedNormal(n, x); }
  }
  static final class ManualNormal implements Measure.Loop {
    public long run(int n, long x) { return manualNormal(n, x); }
  }
  static final class GeneratedLeaf implements Measure.Loop {
    public long run(int n, long x) { return generatedLeaf(n, x); }
  }
  static final class ManualLeaf implements Measure.Loop {
    public long run(int n, long x) { return manualLeaf(n, x); }
  }
  static final class GeneratedStaticJni implements Measure.Loop {
    public long run(int n, long x) { return generatedStaticJni(n, x); }
  }
  static final class ManualStaticJni implements Measure.Loop {
    public long run(int n, long x) { return manualStaticJni(n, x); }
  }
  public static void main(String[] args) throws Throwable {
    String name=args[0]; int count=Integer.parseInt(args[1]);
    Measure.Loop loop = switch (name) {
      case "control" -> new Measure.ReferenceLoop();
      case "generated-normal" -> new GeneratedNormal();
      case "manual-normal" -> new ManualNormal();
      case "generated-leaf" -> new GeneratedLeaf();
      case "manual-leaf" -> new ManualLeaf();
      case "generated-static-jni" -> new GeneratedStaticJni();
      case "manual-static-jni" -> new ManualStaticJni();
      default -> throw new IllegalArgumentException(name);
    };
    Measure.run(name, count, loop);
  }
}
