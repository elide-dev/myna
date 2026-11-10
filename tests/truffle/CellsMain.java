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
  static volatile Object allocationCanary;
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
  /** Language glue holding the concrete cell type and calling its primitive entry point. */
  static final class TypedAdd extends NativeCell {
    @Node.Child private CellApiTruffle.add_Cell inner = new CellApiTruffle.add_Cell();
    TypedAdd() { super("typed:add", 2); }
    @Override public Object execute(Object[] arguments) {
      int result = inner.executeInt(arguments);
      return new Object[] {result, CompilerDirectives.inCompiledCode()};
    }
  }
  /** Runs a cell many times inside one compiled root, so the host can measure what a call allocates. */
  static final class Repeat extends NativeCell {
    static final int CALLS = 4096;
    @Node.Child private NativeCell inner;
    Repeat(NativeCell inner) { super("repeat:" + inner.name(), inner.arity()); this.inner = inner; }
    @Override public Object execute(Object[] arguments) {
      long sum = 0;
      for (int i = 0; i < CALLS; i++) sum += ((Number) inner.execute(arguments)).longValue();
      return new Object[] {sum, CompilerDirectives.inCompiledCode()};
    }
  }
  /**
   * Java heap bytes one call allocates from compiled code must not exceed {@code perCall}: the
   * argument array, boxed carriers and the borrow are scalar-replaced, so only what a call must
   * allocate (a pin) remains. The slack covers the host call's own result and argument arrays.
   */
  static void allocates(String what, NativeCell cell, long perCall, long expected, Object... arguments) {
    var bean = (com.sun.management.ThreadMXBean) java.lang.management.ManagementFactory.getThreadMXBean();
    long thread = Thread.currentThread().threadId();
    NativeFunction repeat = new Repeat(cell).function();
    for (int warmup = 0; warmup < 20; warmup++) repeat.call(arguments);
    long before = bean.getThreadAllocatedBytes(thread);
    Object[] outcome = (Object[]) repeat.call(arguments);
    long allocated = bean.getThreadAllocatedBytes(thread) - before;
    if (!(Boolean) outcome[1]) throw new AssertionError(what + " did not run in compiled code");
    if ((Long) outcome[0] != expected * Repeat.CALLS) throw new AssertionError(what + ": " + outcome[0]);
    long limit = perCall * Repeat.CALLS + 1024;
    if (allocated > limit) throw new AssertionError(what + " allocated " + allocated + " bytes over " + Repeat.CALLS + " calls, limit " + limit);
    System.out.println(what + " allocated " + allocated + " bytes over " + Repeat.CALLS + " compiled calls");
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
        compiled("typed add", new TypedAdd().function(), i -> new Object[] {(int) i, 1}, i -> (int) i + 1);
        compiled("sleepyAdd", new Probe(new CellApiTruffle.sleepyAdd_Cell()).function(), i -> new Object[] {i, 2L}, i -> i + 2);
        byte[] large = new byte[Borrow.pinThreshold() * 2];
        java.util.Arrays.fill(large, (byte) 1);
        HeapBytes pinned = new HeapBytes(large, false);
        compiled("checksum", new Probe(new CellApiTruffle.checksum_Cell()).function(), i -> new Object[] {pinned}, i -> (long) large.length);
        compiled("strlen", new Probe(new CellApiTruffle.strlen_Cell()).function(), i -> new Object[] {"héllo"}, i -> 6L);
        // Two scratch copies per call from compiled code, released in reverse order: the thread's
        // stack is empty again afterwards, so the next copy lands where the first one did.
        Borrow before = Borrow.ofString("probe", true);
        long base = before.address();
        before.release();
        compiled("pair", new Probe(new CellApiTruffle.pair_Cell()).function(),
            i -> new Object[] {"left" + i, "right" + (i % 7)}, i -> CellChecks.pair("left" + i, "right" + (i % 7)));
        Borrow after = Borrow.ofString("probe", true);
        if (after.address() != base) throw new AssertionError("scratch stack unbalanced after compiled pair calls");
        after.release();
        // What a compiled call allocates: nothing for scalars, lent native memory and scratch
        // copies, and only the pin itself for a pinned heap array (never the borrow).
        var bean = (com.sun.management.ThreadMXBean) java.lang.management.ManagementFactory.getThreadMXBean();
        if (!bean.isThreadAllocatedMemorySupported()) throw new AssertionError("allocation counter required");
        bean.setThreadAllocatedMemoryEnabled(true);
        long canaryBefore = bean.getThreadAllocatedBytes(Thread.currentThread().threadId());
        allocationCanary = new byte[4096];
        if (bean.getThreadAllocatedBytes(Thread.currentThread().threadId()) - canaryBefore < 4096)
          throw new AssertionError("allocation counter canary failed");
        byte[] small = new byte[64];
        java.util.Arrays.fill(small, (byte) 2);
        java.lang.foreign.MemorySegment lent = java.lang.foreign.Arena.global().allocate(64);
        lent.fill((byte) 3);
        allocates("add", new CellApiTruffle.add_Cell(), 0, 5, 2, 3);
        allocates("checksum of native bytes", new CellApiTruffle.checksum_Cell(), 0, 64 * 3, NativeBytes.of(lent, false));
        allocates("checksum of a copied heap array", new CellApiTruffle.checksum_Cell(), 0, 64 * 2, new HeapBytes(small, false));
        allocates("checksum of a pinned heap array", new CellApiTruffle.checksum_Cell(), 32, large.length, pinned);
        allocates("sleepyAdd", new CellApiTruffle.sleepyAdd_Cell(), 0, 9, 4L, 5L);
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
