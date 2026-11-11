// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package fixture.cells;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.nodes.Node;
import dev.elide.myna.truffle.*;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import org.graalvm.nativeimage.ImageInfo;
import org.graalvm.polyglot.Context;
/** FFM-backend cells against the shared fixture library, on HotSpot and in a Native Image. */
public final class FfmCellsMain {
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
   * Java heap bytes from runtime-compiled code, per pattern of lent segments: the borrow, the
   * argument array, the boxed result and every segment made from an address stay out of the heap.
   */
  static void allocates(String what, NativeCell cell, long expected, Object... arguments) {
    var bean = (com.sun.management.ThreadMXBean) java.lang.management.ManagementFactory.getThreadMXBean();
    long thread = Thread.currentThread().threadId();
    NativeFunction repeat = new Repeat(cell).function();
    for (int warmup = 0; warmup < 20; warmup++) repeat.call(arguments);
    long before = bean.getThreadAllocatedBytes(thread);
    Object[] outcome = (Object[]) repeat.call(arguments);
    long allocated = bean.getThreadAllocatedBytes(thread) - before;
    if (!(Boolean) outcome[1]) throw new AssertionError(what + " did not run in compiled code");
    if ((Long) outcome[0] != expected * Repeat.CALLS) throw new AssertionError(what + ": " + outcome[0]);
    if (allocated > 1024) throw new AssertionError(what + " allocated " + allocated + " bytes over " + Repeat.CALLS + " calls");
    System.out.println(what + " allocated " + allocated + " bytes over " + Repeat.CALLS + " compiled calls");
  }
  static void compiledAllocations() throws Exception {
    try (Context context = Context.newBuilder().allowExperimentalOptions(true)
        .option("engine.CompileImmediately", "true").option("engine.BackgroundCompilation", "false")
        .option("engine.MultiTier", "false").option("engine.CompilationFailureAction", "Throw").build();
        Arena confined = Arena.ofConfined(); Arena shared = Arena.ofShared()) {
      context.enter();
      try {
        MemorySegment global = Arena.global().allocate(4), globalOut = Arena.global().allocate(4);
        MemorySegment mine = confined.allocate(4), mineOut = confined.allocate(4), common = shared.allocate(4);
        global.fill((byte) 1);
        mine.fill((byte) 1);
        common.fill((byte) 1);
        allocates("checksum of global memory", new CellApiTruffle.checksum_Cell(), 4, NativeBytes.of(global, false));
        allocates("checksum of a confined segment", new CellApiTruffle.checksum_Cell(), 4, NativeBytes.of(mine, false));
        allocates("checksum of a shared segment", new CellApiTruffle.checksum_Cell(), 4, NativeBytes.of(common, false));
        allocates("checksum of a copied heap array", new CellApiTruffle.checksum_Cell(), 4, new HeapBytes(new byte[] {1, 1, 1, 1}, false));
        allocates("strlen of a string", new CellApiTruffle.strlen_Cell(), 11, "hello world");
        // Every pattern of two lent buffers: neither with a scope to check, either, both.
        allocates("transfer, no scope", new CellApiTruffle.transfer_Cell(), 4, NativeBytes.of(global, false), NativeBytes.of(globalOut, true), NativePointer.NULL);
        allocates("transfer, output scoped", new CellApiTruffle.transfer_Cell(), 4, NativeBytes.of(global, false), NativeBytes.of(mineOut, true), NativePointer.NULL);
        allocates("transfer, input scoped", new CellApiTruffle.transfer_Cell(), 4, NativeBytes.of(mine, false), NativeBytes.of(globalOut, true), NativePointer.NULL);
        allocates("transfer, both scoped", new CellApiTruffle.transfer_Cell(), 4, NativeBytes.of(mine, false), NativeBytes.of(mineOut, true), NativePointer.NULL);
        allocates("transfer, scoped and copied", new CellApiTruffle.transfer_Cell(), 4, NativeBytes.of(mine, false), new HeapBytes(new byte[4], true), NativePointer.NULL);
        // A mapped file's segment is checked like any other.
        java.nio.file.Path file = java.nio.file.Files.createTempFile("myna-cells", ".bin");
        try (var channel = java.nio.channels.FileChannel.open(file, java.nio.file.StandardOpenOption.READ, java.nio.file.StandardOpenOption.WRITE);
            Arena arena = Arena.ofConfined()) {
          channel.write(java.nio.ByteBuffer.wrap(new byte[] {2, 2, 2, 2}));
          MemorySegment mapped = channel.map(java.nio.channels.FileChannel.MapMode.READ_ONLY, 0, 4, arena);
          allocates("checksum of a mapped segment", new CellApiTruffle.checksum_Cell(), 8, NativeBytes.of(mapped, false));
        } finally {
          java.nio.file.Files.delete(file);
        }
      } finally {
        context.leave();
      }
    }
  }
  public static void main(String[] args) throws Exception {
    System.load(args[0]);
    CellChecks.run(CellApiTruffle::function);
    CellChecks.scopes(CellApiTruffle::function);
    // The static entry still takes raw addresses, whose validity is the caller's to keep.
    try (Arena arena = Arena.ofConfined()) {
      MemorySegment bytes = arena.allocate(4);
      bytes.fill((byte) 5);
      if (CellApiTruffle.checksum_Cell.invoke(bytes.address(), 4) != 20) throw new AssertionError("invoke checksum");
    }
    if (CellApiTruffle.CELLS.length != 33) throw new AssertionError("cells: " + CellApiTruffle.CELLS.length);
    // Only an image compiles at run time here; on HotSpot the fixture runs without a compiler.
    if (ImageInfo.inImageRuntimeCode()) compiledAllocations();
    System.out.println("Truffle FFM cells passed");
  }
}
