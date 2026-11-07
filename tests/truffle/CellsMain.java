// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package fixture.cells;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.nodes.Node;
import dev.elide.myna.truffle.*;
import org.graalvm.polyglot.Context;
/** Native Image: cells through the interop wrapper, then from runtime-compiled Truffle code. */
public final class CellsMain {
  /** Wraps a cell and records whether its most recent execution ran in compiled code. */
  static final class Probe extends NativeCell {
    static volatile boolean lastExecutionCompiled;
    @Node.Child private NativeCell inner;
    Probe(NativeCell inner) { super("probe:" + inner.name(), inner.arity()); this.inner = inner; }
    @Override public Object execute(Object[] arguments) {
      Object result = inner.execute(arguments);
      boolean compiled = CompilerDirectives.inCompiledCode();
      lastExecutionCompiled = compiled;
      return new Object[] {result, compiled};
    }
  }
  /** Mirrors the fork's CFunctionRuntimeCompilationTest: compile immediately, then verify results in compiled code. */
  static void compiled(String what, NativeFunction probe, java.util.function.LongFunction<Object[]> args, java.util.function.LongFunction<Object> expected) {
    boolean compiled = false;
    for (long i = 0; i < 10 && !compiled; i++) {
      Object[] outcome = (Object[]) probe.call(args.apply(i));
      Object want = expected.apply(i);
      if (want != null && !outcome[0].equals(want)) throw new AssertionError(what + ": " + outcome[0] + " at " + i);
      compiled = (Boolean) outcome[1];
    }
    if (!compiled) throw new AssertionError(what + " root did not run in compiled code");
    for (long i = 0; i < 10_000; i++) {
      Object[] outcome = (Object[]) probe.call(args.apply(i));
      Object want = expected.apply(i);
      if (want != null && !outcome[0].equals(want)) throw new AssertionError(what + " compiled: " + outcome[0] + " at " + i);
      if (!(Boolean) outcome[1]) throw new AssertionError(what + " left compiled code at " + i);
    }
    System.out.println(what + " ran in compiled code");
  }
  public static void main(String[] args) throws Exception {
    if (CellApi.BINDING_ABI_VERSION != 2) throw new AssertionError("binding ABI");
    String runtime = Truffle.getRuntime().getName();
    System.out.println("Truffle runtime: " + runtime);
    if (runtime.contains("Default") || runtime.contains("Interpreted"))
      throw new AssertionError("optimizing Truffle runtime missing from the image: " + runtime);
    CellChecks.run(CellApiTruffle::function);
    try (Context context = Context.newBuilder().allowExperimentalOptions(true)
        .option("engine.CompileImmediately", "true").option("engine.BackgroundCompilation", "false")
        .option("engine.MultiTier", "false").option("engine.CompilationFailureAction", "Throw").build()) {
      context.enter();
      try {
        compiled("add", new Probe(new CellApiTruffle.add_Cell()).function(), i -> new Object[] {(int) i, 1}, i -> (int) i + 1);
        compiled("sleepyAdd", new Probe(new CellApiTruffle.sleepyAdd_Cell()).function(), i -> new Object[] {i, 2L}, i -> i + 2);
        byte[] large = new byte[Borrow.pinThreshold() * 2];
        java.util.Arrays.fill(large, (byte) 1);
        HeapBytes pinned = new HeapBytes(large, false);
        compiled("checksum", new Probe(new CellApiTruffle.checksum_Cell()).function(), i -> new Object[] {pinned}, i -> (long) large.length);
        compiled("strlen", new Probe(new CellApiTruffle.strlen_Cell()).function(), i -> new Object[] {"héllo"}, i -> 6L);
        Object counter = CellApiTruffle.counterCreate().call(0L);
        compiled("counterNext", new Probe(new CellApiTruffle.counterNext_Cell()).function(), i -> new Object[] {counter}, i -> null);
        ((NativeResource) counter).close();
        if (!CellApiTruffle.counterLive().call().equals(0L)) throw new AssertionError("live counters");
      } finally {
        context.leave();
      }
    }
    System.out.println("Truffle cells compiled and passed");
  }
}
