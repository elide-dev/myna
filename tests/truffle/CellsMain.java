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
  static volatile long hostSink;
  static long pinsPerCall;
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
  /** Creates and closes a resource many times inside one compiled root, to measure what a create allocates. */
  static final class Creates extends NativeCell {
    static final int CALLS = 1024;
    @Node.Child private NativeCell inner;
    Creates(NativeCell inner) { super("creates:" + inner.name(), inner.arity()); this.inner = inner; }
    @Override public Object execute(Object[] arguments) {
      long open = 0;
      for (int i = 0; i < CALLS; i++) {
        NativeResource created = (NativeResource) inner.execute(arguments);
        if (!created.isClosed()) open++;
        created.close();
      }
      return new Object[] {open, CompilerDirectives.inCompiledCode()};
    }
  }
  /** Java heap bytes per create and close from compiled code; returns them for comparison. */
  static long perCreate(String what, NativeCell cell, Object... arguments) {
    var bean = (com.sun.management.ThreadMXBean) java.lang.management.ManagementFactory.getThreadMXBean();
    long thread = Thread.currentThread().threadId();
    NativeFunction repeat = new Creates(cell).function();
    for (int warmup = 0; warmup < 20; warmup++) repeat.call(arguments);
    long before = bean.getThreadAllocatedBytes(thread);
    Object[] outcome = (Object[]) repeat.call(arguments);
    long allocated = bean.getThreadAllocatedBytes(thread) - before;
    if (!(Boolean) outcome[1]) throw new AssertionError(what + " did not run in compiled code");
    if ((Long) outcome[0] != Creates.CALLS) throw new AssertionError(what + ": " + outcome[0]);
    long perCall = allocated / Creates.CALLS;
    System.out.println(what + " allocated " + perCall + " bytes per compiled create and close");
    return perCall;
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
    pinsPerCall = allocated / Repeat.CALLS;
  }
  /** What a cell's typed execute allocates per call from host code, which is compiled ahead of time. */
  static void hostExecute(com.sun.management.ThreadMXBean bean, String what, long perCall, NativeCell cell, Object... arguments) throws Exception {
    long thread = Thread.currentThread().threadId();
    long sum = 0;
    for (int i = 0; i < 64; i++) sum += cell.executeLong(arguments);
    long before = bean.getThreadAllocatedBytes(thread);
    for (int i = 0; i < Repeat.CALLS; i++) sum += cell.executeLong(arguments);
    long allocated = bean.getThreadAllocatedBytes(thread) - before;
    hostSink = sum;
    if (allocated > perCall * Repeat.CALLS + 1024) throw new AssertionError(what + " allocated " + allocated + " bytes over " + Repeat.CALLS + " calls");
    System.out.println(what + " allocated " + allocated + " bytes over " + Repeat.CALLS + " calls");
  }
  /** A create cell from compiled code: every call returns an open owner of the right resource. */
  static void created(String what, NativeFunction probe, String type, Object... arguments) {
    boolean compiled = false;
    for (int i = 0; i < 2_000; i++) {
      Object[] outcome = (Object[]) probe.call(arguments);
      if (!(outcome[0] instanceof NativeResource owner) || !owner.type().equals(type) || owner.isClosed())
        throw new AssertionError(what + ": " + outcome[0]);
      owner.close();
      if (compiled && !(Boolean) outcome[1]) throw new AssertionError(what + " left compiled code at " + i);
      compiled |= (Boolean) outcome[1];
    }
    if (!compiled) throw new AssertionError(what + " root did not run in compiled code");
    System.out.println(what + " created its resource from compiled code");
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
        // Text of every UTF-8 length, encoded onto the scratch stack by compiled code, as Java
        // strings and as guest strings that are new on every call; an unpaired surrogate is one "?".
        java.util.function.LongFunction<String> text = i -> "h\u00e9llo \u20ac" + i + (i % 3 == 0 ? "\ud83d\ude00" : "\ud83d");
        compiled("strlen of text", new Probe(new CellApiTruffle.strlen_Cell()).function(),
            i -> new Object[] {text.apply(i)}, i -> (long) text.apply(i).getBytes(java.nio.charset.StandardCharsets.UTF_8).length);
        compiled("strlen of a guest string", new Probe(new CellApiTruffle.strlen_Cell()).function(),
            i -> new Object[] {CellChecks.guest(text.apply(i))}, i -> (long) text.apply(i).getBytes(java.nio.charset.StandardCharsets.UTF_8).length);
        compiled("strlen of a narrow guest string", new Probe(new CellApiTruffle.strlen_Cell()).function(),
            i -> new Object[] {CellChecks.guest(i % 2 == 0 ? "plain" + i : "h\u00e9llo" + i)}, i -> (i % 2 == 0 ? 5L : 6L) + Long.toString(i).length());
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
        compiled("pair of guest strings", new Probe(new CellApiTruffle.pair_Cell()).function(),
            i -> new Object[] {CellChecks.guest("l\u00e9ft" + i), CellChecks.guest("r\u20acght" + (i % 7))}, i -> CellChecks.pair("l\u00e9ft" + i, "r\u20acght" + (i % 7)));
        Borrow guests = Borrow.ofString("probe", true);
        if (guests.address() != base) throw new AssertionError("scratch stack unbalanced after compiled guest string calls");
        guests.release();
        // Two declared borrows on one import, both copied onto the scratch stack and the writable
        // one copied back, from compiled code: the stack is balanced afterwards as well.
        byte[] source = new byte[64];
        java.util.Arrays.fill(source, (byte) 2);
        byte[] sink = new byte[48];
        compiled("transfer", new Probe(new CellApiTruffle.transfer_Cell()).function(),
            i -> new Object[] {new HeapBytes(source, false), new HeapBytes(sink, true), NativePointer.NULL}, i -> 96L);
        if (sink[0] != 2 || sink[47] != 2) throw new AssertionError("compiled transfer did not write its copy back");
        Borrow balanced = Borrow.ofString("probe", true);
        if (balanced.address() != base) throw new AssertionError("scratch stack unbalanced after compiled transfer calls");
        balanced.release();
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
        // Text goes straight onto the scratch stack, ASCII or not: no UTF-8 array.
        allocates("strlen of a string", new CellApiTruffle.strlen_Cell(), 0, 11, "hello world");
        allocates("strlen of a Latin-1 string", new CellApiTruffle.strlen_Cell(), 0, 13, "h\u00e9llo w\u00f6rld");
        allocates("strlen of a wide string", new CellApiTruffle.strlen_Cell(), 0, 21, "\u3053\u3093\u306b\u3061\u306f\u4e16\u754c");
        allocates("checksum of a wide string", new CellApiTruffle.checksum_Cell(), 0, 3 * 0xe2 + 3 * 0x82 + 3 * 0xac, "\u20ac\u20ac\u20ac");
        // A guest string is read through the TruffleString API: no Java string, and no UTF-8
        // array for the ones that are not ASCII either.
        allocates("strlen of an ASCII guest string", new CellApiTruffle.strlen_Cell(), 0, 11, CellChecks.guest("hello world"));
        allocates("strlen of a Latin-1 guest string", new CellApiTruffle.strlen_Cell(), 0, 13, CellChecks.guest("h\u00e9llo w\u00f6rld"));
        allocates("strlen of a wide guest string", new CellApiTruffle.strlen_Cell(), 0, 21, CellChecks.guest("\u3053\u3093\u306b\u3061\u306f\u4e16\u754c"));
        allocates("checksum of a wide guest string", new CellApiTruffle.checksum_Cell(), 0, 3 * 0xe2 + 3 * 0x82 + 3 * 0xac, CellChecks.guest("\u20ac\u20ac\u20ac"));
        // A truffle_buffer parameter pins a large heap array (the pin is the only allocation) and
        // copies a small one without a terminator.
        allocates("sumBytes of a pinned heap array", new CellApiTruffle.sumBytes_Cell(), 32, large.length, pinned, (long) large.length, NativePointer.NULL);
        if (pinsPerCall < 16) throw new AssertionError("sumBytes did not pin its large heap array: " + pinsPerCall + " bytes per call");
        allocates("sumBytes of a copied heap array", new CellApiTruffle.sumBytes_Cell(), 0, 64 * 2, new HeapBytes(small, false), 64L, NativePointer.NULL);
        // Two declared borrows beside a pointer: lending native memory both ways and copying both
        // heap arrays allocate nothing, and two pinned arrays allocate their two pins.
        java.lang.foreign.MemorySegment written = java.lang.foreign.Arena.global().allocate(64);
        java.lang.foreign.MemorySegment copied = java.lang.foreign.Arena.global().allocate(8);
        allocates("transfer between native buffers", new CellApiTruffle.transfer_Cell(), 0, 64 * 3,
            NativeBytes.of(lent, false), NativeBytes.of(written, true), NativePointer.of(copied.address()));
        if (written.get(java.lang.foreign.ValueLayout.JAVA_BYTE, 63) != 3 || copied.get(java.lang.foreign.ValueLayout.JAVA_LONG, 0) != 64)
          throw new AssertionError("transfer between native buffers did not write in place");
        allocates("transfer between copied heap arrays", new CellApiTruffle.transfer_Cell(), 0, 64 * 2,
            new HeapBytes(small, false), new HeapBytes(new byte[64], true), NativePointer.NULL);
        allocates("transfer between pinned heap arrays", new CellApiTruffle.transfer_Cell(), 64, large.length,
            pinned, new HeapBytes(new byte[large.length], true), NativePointer.NULL);
        if (pinsPerCall < 32) throw new AssertionError("transfer did not pin both large heap arrays: " + pinsPerCall + " bytes per call");
        // A nullable borrow from compiled code. One call site sees an absent and a present output
        // in turn and stays compiled; absent, in any spelling, lends the null address and derives
        // length zero without allocating, exactly as lending native memory does.
        java.lang.foreign.MemorySegment decoded = java.lang.foreign.Arena.global().allocate(64);
        compiled("decode", new Probe(new CellApiTruffle.decode_Cell()).function(),
            i -> new Object[] {NativeBytes.of(lent, false), (i & 1) == 0 ? NativeVoid.INSTANCE : NativeBytes.of(decoded, true)},
            i -> (i & 1) == 0 ? 64L | CellChecks.ABSENT : 64L);
        if (decoded.get(java.lang.foreign.ValueLayout.JAVA_BYTE, 0) != 4 || decoded.get(java.lang.foreign.ValueLayout.JAVA_BYTE, 63) != 4)
          throw new AssertionError("compiled decode did not write in place");
        allocates("decode without an output buffer (interop null)", new CellApiTruffle.decode_Cell(), 0, 64L | CellChecks.ABSENT,
            NativeBytes.of(lent, false), NativeVoid.INSTANCE);
        allocates("decode without an output buffer (null pointer)", new CellApiTruffle.decode_Cell(), 0, 64L | CellChecks.ABSENT,
            NativeBytes.of(lent, false), NativePointer.NULL);
        allocates("decode without an output buffer (host null)", new CellApiTruffle.decode_Cell(), 0, 64L | CellChecks.ABSENT,
            NativeBytes.of(lent, false), null);
        allocates("decode into a native buffer", new CellApiTruffle.decode_Cell(), 0, 64,
            NativeBytes.of(lent, false), NativeBytes.of(decoded, true));
        allocates("decode into a copied heap array", new CellApiTruffle.decode_Cell(), 0, 64,
            NativeBytes.of(lent, false), new HeapBytes(new byte[64], true));
        // The static invoke entries take native carriers and raw addresses: from host code, which
        // is compiled ahead of time and scalar-replaces nothing, a call allocates nothing.
        if (CellApiTruffle.add_Cell.invoke(2, 3) != 5) throw new AssertionError("invoke add");
        if (CellApiTruffle.lowByte_Cell.invoke(0x1ffL) != 0xff) throw new AssertionError("invoke lowByte");
        // Every borrowed length is an explicit argument of invoke, in the binding's own order.
        copied.set(java.lang.foreign.ValueLayout.JAVA_LONG, 0, -1);
        if (CellApiTruffle.transfer_Cell.invoke(lent.address(), 64, written.address(), 16, copied.address()) != 16 * 3
            || copied.get(java.lang.foreign.ValueLayout.JAVA_LONG, 0) != 16) throw new AssertionError("invoke transfer");
        // invoke takes the null address and a zero length for an absent buffer, as the cell does.
        if (CellApiTruffle.decode_Cell.invoke(lent.address(), 64, 0, 0) != (64L | CellChecks.ABSENT)) throw new AssertionError("invoke decode");
        long hostThread = Thread.currentThread().threadId();
        long hostBefore = bean.getThreadAllocatedBytes(hostThread);
        long hostSum = 0;
        for (int i = 0; i < Repeat.CALLS; i++) hostSum += CellApiTruffle.checksum_Cell.invoke(lent.address(), 64) + CellApiTruffle.sleepyAdd_Cell.invoke(i, 1);
        long hostAllocated = bean.getThreadAllocatedBytes(hostThread) - hostBefore;
        if (hostSum != Repeat.CALLS * (64L * 3) + (long) Repeat.CALLS * (Repeat.CALLS + 1) / 2) throw new AssertionError("invoke sum " + hostSum);
        if (hostAllocated > 1024) throw new AssertionError("invoke from host code allocated " + hostAllocated + " bytes");
        System.out.println("invoke from host code allocated " + hostAllocated + " bytes over " + Repeat.CALLS + " calls");
        // A cell's typed execute from host code, which is compiled ahead of time and has no
        // partial evaluation: the borrow must still stay out of the heap.
        hostExecute(bean, "host execute: checksum of native bytes", 0, new CellApiTruffle.checksum_Cell(), NativeBytes.of(lent, false));
        hostExecute(bean, "host execute: checksum of a copied heap array", 0, new CellApiTruffle.checksum_Cell(), new HeapBytes(small, false));
        hostExecute(bean, "host execute: checksum of a pinned heap array", 32, new CellApiTruffle.checksum_Cell(), pinned);
        hostExecute(bean, "host execute: strlen of a string", 0, new CellApiTruffle.strlen_Cell(), "hello world");
        hostExecute(bean, "host execute: strlen of a Latin-1 string", 0, new CellApiTruffle.strlen_Cell(), "h\u00e9llo w\u00f6rld");
        hostExecute(bean, "host execute: strlen of a wide string", 0, new CellApiTruffle.strlen_Cell(), "\u3053\u3093\u306b\u3061\u306f\u4e16\u754c");
        // A guest string's bytes are read through a TruffleString node whose result, a 24-byte
        // InternalByteArray view, only partial evaluation keeps out of the heap: ahead-of-time code
        // also reaches the node's specializing call, whose result merges with it.
        hostExecute(bean, "host execute: strlen of an ASCII guest string", 24, new CellApiTruffle.strlen_Cell(), CellChecks.guest("hello world"));
        hostExecute(bean, "host execute: strlen of a Latin-1 guest string", 24, new CellApiTruffle.strlen_Cell(), CellChecks.guest("h\u00e9llo w\u00f6rld"));
        hostExecute(bean, "host execute: strlen of a wide guest string", 24, new CellApiTruffle.strlen_Cell(), CellChecks.guest("\u3053\u3093\u306b\u3061\u306f\u4e16\u754c"));
        java.lang.foreign.MemorySegment cText = java.lang.foreign.Arena.global().allocateFrom("hello world");
        hostExecute(bean, "host execute: strlen of native bytes", 0, new CellApiTruffle.strlen_Cell(), NativeBytes.of(cText, false));
        hostExecute(bean, "host execute: strlen of a raw pointer", 0, new CellApiTruffle.strlen_Cell(), NativePointer.of(cText.address()));
        // An absent nullable buffer, in each of its spellings, beside lent native memory.
        hostExecute(bean, "host execute: decode without an output buffer (null pointer)", 0, new CellApiTruffle.decode_Cell(), NativeBytes.of(lent, false), NativePointer.NULL);
        hostExecute(bean, "host execute: decode without an output buffer (interop null)", 0, new CellApiTruffle.decode_Cell(), NativeBytes.of(lent, false), NativeVoid.INSTANCE);
        hostExecute(bean, "host execute: decode without an output buffer (host null)", 0, new CellApiTruffle.decode_Cell(), NativeBytes.of(lent, false), null);
        // Creates that report a status and write their resource through a parameter, from
        // compiled code: each call makes an owner, which is closed before the next.
        java.lang.foreign.MemorySegment layoutResult = java.lang.foreign.Arena.global().allocate(24);
        created("layoutCompile", new Probe(new CellApiTruffle.layoutCompile_Cell()).function(), "Layout",
            new HeapBytes(new byte[] {9}, false), NativeBytes.of(layoutResult, true));
        java.lang.foreign.MemorySegment viewMemory = java.lang.foreign.Arena.global().allocate(16);
        created("viewInit", new Probe(new CellApiTruffle.viewInit_Cell()).function(), "View",
            new HeapBytes(new byte[] {4, 5}, false), NativeBytes.of(viewMemory, true));
        // A create whose borrow its resource holds: the bytes are read again on every release,
        // after the call that lent them returned, from a copy or from native memory in place.
        java.lang.foreign.MemorySegment adoptResult = java.lang.foreign.Arena.global().allocate(24);
        created("adopt", new Probe(new CellApiTruffle.adopt_Cell()).function(), "Adopted",
            new HeapBytes(new byte[] {4, 5}, false), "k", NativeBytes.of(adoptResult, true));
        if (!CellApiTruffle.adoptedLast().call().equals(9L)) throw new AssertionError("compiled adopt released a held copy early");
        created("adopt in place", new Probe(new CellApiTruffle.adopt_Cell()).function(), "Adopted",
            NativeBytes.of(lent, false), null, NativeBytes.of(adoptResult, true));
        if (!CellApiTruffle.adoptedLast().call().equals(64L * 3)) throw new AssertionError("compiled adopt in place");
        created("adopt of a large array", new Probe(new CellApiTruffle.adopt_Cell()).function(), "Adopted",
            pinned, null, NativeBytes.of(adoptResult, true));
        if (!CellApiTruffle.adoptedLast().call().equals((long) large.length)) throw new AssertionError("compiled adopt of a large array");
        // What a create allocates, held borrow or not: the owner, its state, the runtime's struct
        // and, for a held borrow, the borrow itself, its array and its copy.
        perCreate("layoutCompile (call borrow, copied)", new CellApiTruffle.layoutCompile_Cell(),
            new HeapBytes(new byte[] {9}, false), NativeBytes.of(layoutResult, true));
        long viewBytes = perCreate("viewInit (call borrow, in place)", new CellApiTruffle.viewInit_Cell(),
            new HeapBytes(new byte[] {4, 5}, false), NativeBytes.of(viewMemory, true));
        long heldNative = perCreate("adopt (held borrow, native memory in place)", new CellApiTruffle.adopt_Cell(),
            NativeBytes.of(lent, false), null, NativeBytes.of(adoptResult, true));
        long heldCopy = perCreate("adopt (held borrow, heap array copied)", new CellApiTruffle.adopt_Cell(),
            new HeapBytes(small, false), null, NativeBytes.of(adoptResult, true));
        perCreate("adopt (held borrow, string copied)", new CellApiTruffle.adopt_Cell(),
            "hello world", null, NativeBytes.of(adoptResult, true));
        if (heldNative > viewBytes + 128 || heldCopy > heldNative + 128)
          throw new AssertionError("a held borrow allocates more than its holder: " + heldNative + ", " + heldCopy + " against " + viewBytes);
        if (!CellApiTruffle.counterLive().call().equals(0L)) throw new AssertionError("live adoptions after the create loops");
        // Several byte parameters on one cell share the host inliner's budget for that cell: each
        // coercion must still be inlined, or its borrow is allocated on every path.
        hostExecute(bean, "host execute: pair of strings", 0, new CellApiTruffle.pair_Cell(), "left", "right");
        hostExecute(bean, "host execute: pair of guest strings", 48, new CellApiTruffle.pair_Cell(), CellChecks.guest("l\u00e9ft"), CellChecks.guest("r\u20acght"));
        hostExecute(bean, "host execute: transfer between native buffers", 0, new CellApiTruffle.transfer_Cell(),
            NativeBytes.of(lent, false), NativeBytes.of(written, true), NativePointer.of(copied.address()));
        hostExecute(bean, "host execute: transfer between copied heap arrays", 0, new CellApiTruffle.transfer_Cell(),
            new HeapBytes(small, false), new HeapBytes(new byte[64], true), NativePointer.NULL);
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
