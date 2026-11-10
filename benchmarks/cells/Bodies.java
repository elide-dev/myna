// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package bench.nodes;

import bench.BoundaryCellsTruffle;
import bench.Cells;
import bench.CellsTruffle;
import bench.FfmCellsFFM;
import bench.FfmCellsTruffle;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.nodes.Node;
import dev.elide.myna.truffle.HeapBytes;
import dev.elide.myna.truffle.NativeBytes;
import dev.elide.myna.truffle.NativeCell;
import dev.elide.myna.truffle.NativeFunction;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import dev.elide.myna.truffle.NativePointer;
import org.graalvm.nativeimage.PinnedObject;
import org.graalvm.word.WordFactory;

/**
 * One measured loop per case, each a cell so the host drives it through {@link NativeFunction}.
 * Every body runs its whole loop inside one runtime-compiled Truffle root and reports whether that
 * root executed as compiled code; nothing here is reachable from the generated cells.
 */
public abstract class Bodies extends NativeCell {
  /** Whether the most recent loop ran in compiled code; read by the host after each sample. */
  public static volatile boolean lastCompiled;

  public static final String TEXT = "hello world";

  /** The slab entry's data size and what one call adds to its salt; the Python oracle agrees. */
  public static final int SLAB_SIZE = 256;

  public static final long SLAB_DELTA = SLAB_SIZE + 7 + 7 + 3 - 1 + 7 + 11 + 2;

  Bodies(String name) {
    super(name, 3);
  }

  abstract long run(int count, long seed, Object extra);

  @Override
  public final Object execute(Object[] arguments) {
    checkArity(arguments);
    int count = (Integer) arguments[0];
    long seed = (Long) arguments[1];
    long result = run(count, seed, arguments[2]);
    lastCompiled = CompilerDirectives.inCompiledCode();
    return result;
  }

  static AssertionError failure(Throwable cause) {
    CompilerDirectives.transferToInterpreter();
    return new AssertionError(cause);
  }

  /** Pattern bytes that the Python oracle recomputes independently. */
  public static byte[] pattern(int size) {
    byte[] bytes = new byte[size];
    for (int i = 0; i < size; i++) bytes[i] = (byte) (i * 31 + 7);
    return bytes;
  }

  static MemorySegment nativePattern(int size) {
    MemorySegment segment = Arena.global().allocate(size);
    MemorySegment.copy(pattern(size), 0, segment, ValueLayout.JAVA_BYTE, 0, size);
    return segment;
  }

  public static Bodies create(String name) {
    if (name.contains("-sum-")) {
      int size = Integer.parseInt(name.substring(name.lastIndexOf('-') + 1));
      String kind = name.substring(0, name.lastIndexOf('-'));
      return switch (kind) {
        case "direct-sum" -> new DirectSum(name, size);
        case "glue-sum-native" -> new GlueSum(name, NativeBytes.of(nativePattern(size), false));
        case "glue-sum-pin", "glue-sum-copy" -> new GlueSum(name, new HeapBytes(pattern(size), false));
        case "hand-sum-pin" -> new HandSum(name, size, false);
        case "hand-sum-step-pin" -> new HandSum(name, size, true);
        case "inline-sum-pin" -> new InlineSum(name, size, false);
        case "inline-sum-step-pin" -> new InlineSum(name, size, true);
        case "direct-sum-step" -> new DirectSumStep(name, size);
        case "glue-sum-step-native" -> new GlueSum(name, new CellsTruffle.sumStep_Cell(), NativeBytes.of(nativePattern(size), false));
        case "glue-sum-step-pin" -> new GlueSum(name, new CellsTruffle.sumStep_Cell(), new HeapBytes(pattern(size), false));
        default -> throw new IllegalArgumentException(name);
      };
    }
    return switch (name) {
      case "control" -> new Control(name);
      case "direct-leaf" -> new DirectLeaf(name);
      case "direct-step" -> new DirectStep(name);
      case "direct-ffm-leaf" -> new DirectFfmLeaf(name);
      case "direct-ffm-step" -> new DirectFfmStep(name);
      case "direct-text" -> new DirectText(name);
      case "glue-leaf" -> new Glue(name, new CellsTruffle.leaf_Cell());
      case "glue-step" -> new Glue(name, new CellsTruffle.step_Cell());
      case "glue-ffm-leaf" -> new Glue(name, new FfmCellsTruffle.leaf_Cell());
      case "glue-ffm-step" -> new Glue(name, new FfmCellsTruffle.step_Cell());
      case "typed-leaf" -> new TypedLeaf(name);
      case "typed-step" -> new TypedStep(name);
      case "typed-ffm-leaf" -> new TypedFfmLeaf(name);
      case "typed-ffm-step" -> new TypedFfmStep(name);
      case "glue-text" -> new GlueText(name, new CellsTruffle.text_Cell());
      case "interop-leaf" -> new Interop(name, CellsTruffle.leaf());
      case "interop-step" -> new Interop(name, CellsTruffle.step());
      case "hand-step" -> new HandStep(name);
      case "glue-boundary-step" -> new Glue(name, new BoundaryCellsTruffle.step_Cell());
      case "direct-slab" -> new DirectSlab(name, false);
      case "direct-slab-step" -> new DirectSlab(name, true);
      case "hand-slab-step" -> new HandSlab(name);
      case "glue-slab-native" -> new GlueSlab(name, new CellsTruffle.slab_Cell(), false);
      case "glue-slab-heap" -> new GlueSlab(name, new CellsTruffle.slab_Cell(), true);
      case "glue-slab-step-native" -> new GlueSlab(name, new CellsTruffle.slabStep_Cell(), false);
      case "glue-boundary-slab-step-native" -> new GlueSlab(name, new BoundaryCellsTruffle.slabStep_Cell(), false);
      case "interop-slab-native" -> new InteropSlab(name, CellsTruffle.slab());
      case "interop-slab-step-native" -> new InteropSlab(name, CellsTruffle.slabStep());
      case "nfi-leaf" -> new Nfi(name, false);
      case "nfi-text" -> new Nfi(name, true);
      default -> throw new IllegalArgumentException(name);
    };
  }

  static final class Control extends Bodies {
    Control(String name) {
      super(name);
    }

    @Override
    long run(int count, long x, Object extra) {
      for (int i = 0; i < count; i++) x = x * 1664525L + 1013904223L;
      return x;
    }
  }

  /** Plain Java calls to the generated bindings: the floor a cell should match. */
  static final class DirectLeaf extends Bodies {
    DirectLeaf(String name) {
      super(name);
    }

    @Override
    long run(int count, long x, Object extra) {
      for (int i = 0; i < count; i++) x = Cells.leaf(x);
      return x;
    }
  }

  static final class DirectStep extends Bodies {
    DirectStep(String name) {
      super(name);
    }

    @Override
    long run(int count, long x, Object extra) {
      for (int i = 0; i < count; i++) x = Cells.step(x);
      return x;
    }
  }

  static final class DirectFfmLeaf extends Bodies {
    DirectFfmLeaf(String name) {
      super(name);
    }

    @Override
    long run(int count, long x, Object extra) {
      try {
        for (int i = 0; i < count; i++) x = FfmCellsFFM.leaf(x);
      } catch (Throwable t) {
        throw failure(t);
      }
      return x;
    }
  }

  static final class DirectFfmStep extends Bodies {
    DirectFfmStep(String name) {
      super(name);
    }

    @Override
    long run(int count, long x, Object extra) {
      try {
        for (int i = 0; i < count; i++) x = FfmCellsFFM.step(x);
      } catch (Throwable t) {
        throw failure(t);
      }
      return x;
    }
  }

  static final class DirectSum extends Bodies {
    private final long address;
    private final long size;

    DirectSum(String name, int size) {
      super(name);
      this.address = nativePattern(size).address();
      this.size = size;
    }

    @Override
    long run(int count, long x, Object extra) {
      for (int i = 0; i < count; i++) x = Cells.sum(WordFactory.pointer(address), size, x);
      return x;
    }
  }

  static final class DirectText extends Bodies {
    private final long address;

    DirectText(String name) {
      super(name);
      this.address = Arena.global().allocateFrom(TEXT).address();
    }

    @Override
    long run(int count, long x, Object extra) {
      for (int i = 0; i < count; i++) x = Cells.text(WordFactory.pointer(address), x);
      return x;
    }
  }

  /** Language-specific glue: the consumer adopts the cell as a child and calls it directly. */
  static final class Glue extends Bodies {
    @Node.Child private NativeCell cell;

    Glue(String name, NativeCell cell) {
      super(name);
      this.cell = cell;
    }

    @Override
    long run(int count, long x, Object extra) {
      for (int i = 0; i < count; i++) x = (Long) cell.execute(new Object[] {x});
      return x;
    }
  }

  /**
   * Typed glue: the consumer holds the concrete generated cell and calls its primitive {@code
   * executeLong}, which declares no exception, so no box and no handler are involved.
   */
  static final class TypedLeaf extends Bodies {
    @Node.Child private CellsTruffle.leaf_Cell cell = new CellsTruffle.leaf_Cell();

    TypedLeaf(String name) {
      super(name);
    }

    @Override
    long run(int count, long x, Object extra) {
      for (int i = 0; i < count; i++) x = cell.executeLong(new Object[] {x});
      return x;
    }
  }

  static final class TypedStep extends Bodies {
    @Node.Child private CellsTruffle.step_Cell cell = new CellsTruffle.step_Cell();

    TypedStep(String name) {
      super(name);
    }

    @Override
    long run(int count, long x, Object extra) {
      for (int i = 0; i < count; i++) x = cell.executeLong(new Object[] {x});
      return x;
    }
  }

  static final class TypedFfmLeaf extends Bodies {
    @Node.Child private FfmCellsTruffle.leaf_Cell cell = new FfmCellsTruffle.leaf_Cell();

    TypedFfmLeaf(String name) {
      super(name);
    }

    @Override
    long run(int count, long x, Object extra) {
      for (int i = 0; i < count; i++) x = cell.executeLong(new Object[] {x});
      return x;
    }
  }

  static final class TypedFfmStep extends Bodies {
    @Node.Child private FfmCellsTruffle.step_Cell cell = new FfmCellsTruffle.step_Cell();

    TypedFfmStep(String name) {
      super(name);
    }

    @Override
    long run(int count, long x, Object extra) {
      for (int i = 0; i < count; i++) x = cell.executeLong(new Object[] {x});
      return x;
    }
  }

  static final class GlueSum extends Bodies {
    @Node.Child private NativeCell cell;
    private final Object buffer;

    GlueSum(String name, Object buffer) {
      this(name, new CellsTruffle.sum_Cell(), buffer);
    }

    GlueSum(String name, NativeCell cell, Object buffer) {
      super(name);
      this.cell = cell;
      this.buffer = buffer;
    }

    @Override
    long run(int count, long x, Object extra) {
      for (int i = 0; i < count; i++) x = (Long) cell.execute(new Object[] {buffer, x});
      return x;
    }
  }

  static final class GlueText extends Bodies {
    @Node.Child private NativeCell cell;

    GlueText(String name, NativeCell cell) {
      super(name);
      this.cell = cell;
    }

    @Override
    long run(int count, long x, Object extra) {
      for (int i = 0; i < count; i++) x = (Long) cell.execute(new Object[] {TEXT, x});
      return x;
    }
  }

  /** The generic path: the cell's executable wrapper through a dispatched interop library. */
  static final class Interop extends Bodies {
    @Node.Child private InteropLibrary interop = InteropLibrary.getFactory().createDispatched(1);
    private final NativeFunction function;

    Interop(String name, NativeFunction function) {
      super(name);
      this.function = function;
    }

    @Override
    long run(int count, long x, Object extra) {
      try {
        for (int i = 0; i < count; i++) x = (Long) interop.execute(function, x);
      } catch (InteropException e) {
        throw failure(e);
      }
      return x;
    }
  }

  /** Truffle NFI as the dynamic-FFI baseline: the bound function arrives as the extra argument. */
  static final class Nfi extends Bodies {
    @Node.Child private InteropLibrary interop = InteropLibrary.getFactory().createDispatched(1);
    private final boolean text;

    Nfi(String name, boolean text) {
      super(name);
      this.text = text;
    }

    @Override
    long run(int count, long x, Object bound) {
      try {
        if (text) {
          for (int i = 0; i < count; i++) x = (Long) interop.execute(bound, TEXT, x);
        } else {
          for (int i = 0; i < count; i++) x = (Long) interop.execute(bound, x);
        }
      } catch (InteropException e) {
        throw failure(e);
      }
      return x;
    }
  }

  /** The hand-written shape cells are compared with: a binding call behind one Truffle boundary. */
  static final class HandStep extends Bodies {
    HandStep(String name) {
      super(name);
    }

    @CompilerDirectives.TruffleBoundary
    private static long call(long x) {
      return Cells.step(x);
    }

    @Override
    long run(int count, long x, Object extra) {
      for (int i = 0; i < count; i++) x = call(x);
      return x;
    }
  }

  static final class DirectSumStep extends Bodies {
    private final long address;
    private final long size;

    DirectSumStep(String name, int size) {
      super(name);
      this.address = nativePattern(size).address();
      this.size = size;
    }

    @Override
    long run(int count, long x, Object extra) {
      for (int i = 0; i < count; i++) x = Cells.sumStep(WordFactory.pointer(address), size, x);
      return x;
    }
  }

  /** Hand-written pin, call and unpin behind one Truffle boundary, compiled ahead of time. */
  static final class HandSum extends Bodies {
    private final byte[] array;
    private final boolean step;

    HandSum(String name, int size, boolean step) {
      super(name);
      this.array = pattern(size);
      this.step = step;
    }

    @CompilerDirectives.TruffleBoundary
    private static long leaf(byte[] array, long x) {
      PinnedObject pin = PinnedObject.create(array);
      try {
        return Cells.sum(pin.addressOfArrayElement(0), array.length, x);
      } finally {
        pin.close();
      }
    }

    @CompilerDirectives.TruffleBoundary
    private static long step(byte[] array, long x) {
      PinnedObject pin = PinnedObject.create(array);
      try {
        return Cells.sumStep(pin.addressOfArrayElement(0), array.length, x);
      } finally {
        pin.close();
      }
    }

    @Override
    long run(int count, long x, Object extra) {
      if (step) for (int i = 0; i < count; i++) x = step(array, x);
      else for (int i = 0; i < count; i++) x = leaf(array, x);
      return x;
    }
  }

  /** The same pin, call and unpin visible to partial evaluation. */
  static final class InlineSum extends Bodies {
    private final byte[] array;
    private final boolean step;

    InlineSum(String name, int size, boolean step) {
      super(name);
      this.array = pattern(size);
      this.step = step;
    }

    @Override
    long run(int count, long x, Object extra) {
      for (int i = 0; i < count; i++) {
        PinnedObject pin = PinnedObject.create(array);
        try {
          x =
              step
                  ? Cells.sumStep(pin.addressOfArrayElement(0), array.length, x)
                  : Cells.sum(pin.addressOfArrayElement(0), array.length, x);
        } finally {
          pin.close();
        }
      }
      return x;
    }
  }

  /** Raw calls to the ten-argument slab entry: the floor for the slab cells. */
  static final class DirectSlab extends Bodies {
    private final long data = nativePattern(SLAB_SIZE).address();
    private final long aux = nativePattern(16).address();
    private final boolean step;

    DirectSlab(String name, boolean step) {
      super(name);
      this.step = step;
    }

    @Override
    long run(int count, long x, Object extra) {
      for (int i = 0; i < count; i++)
        x =
            step
                ? Cells.slabStep(
                    WordFactory.pointer(data), SLAB_SIZE, WordFactory.pointer(aux), WordFactory.pointer(aux),
                    WordFactory.pointer(data), 3, -1, 7, 11, x)
                : Cells.slab(
                    WordFactory.pointer(data), SLAB_SIZE, WordFactory.pointer(aux), WordFactory.pointer(aux),
                    WordFactory.pointer(data), 3, -1, 7, 11, x);
      return x;
    }
  }

  /** The transitioning slab entry behind one hand-written Truffle boundary. */
  static final class HandSlab extends Bodies {
    private final long data = nativePattern(SLAB_SIZE).address();
    private final long aux = nativePattern(16).address();

    HandSlab(String name) {
      super(name);
    }

    @CompilerDirectives.TruffleBoundary
    private static long call(long data, long aux, long x) {
      return Cells.slabStep(
          WordFactory.pointer(data), SLAB_SIZE, WordFactory.pointer(aux), WordFactory.pointer(aux),
          WordFactory.pointer(data), 3, -1, 7, 11, x);
    }

    @Override
    long run(int count, long x, Object extra) {
      for (int i = 0; i < count; i++) x = call(data, aux, x);
      return x;
    }
  }

  /** Slab cells through consumer glue: ten guest values per call, native or heap byte buffers. */
  static final class GlueSlab extends Bodies {
    @Node.Child private NativeCell cell;
    private final Object data;
    private final Object aux;
    private final Object context;
    private final Object output;

    GlueSlab(String name, NativeCell cell, boolean heap) {
      super(name);
      this.cell = cell;
      this.data = heap ? new HeapBytes(pattern(SLAB_SIZE), false) : NativeBytes.of(nativePattern(SLAB_SIZE), false);
      this.aux = heap ? new HeapBytes(pattern(16), false) : NativeBytes.of(nativePattern(16), false);
      this.context = NativePointer.of(nativePattern(16).address());
      this.output = NativePointer.of(nativePattern(16).address());
    }

    @Override
    long run(int count, long x, Object extra) {
      for (int i = 0; i < count; i++)
        x = (Long) cell.execute(new Object[] {data, (long) SLAB_SIZE, aux, context, output, 3, -1, 7L, 11L, x});
      return x;
    }
  }

  /** Slab cells through the generic interop wrapper. */
  static final class InteropSlab extends Bodies {
    @Node.Child private InteropLibrary interop = InteropLibrary.getFactory().createDispatched(1);
    private final NativeFunction function;
    private final Object data = NativeBytes.of(nativePattern(SLAB_SIZE), false);
    private final Object aux = NativeBytes.of(nativePattern(16), false);
    private final Object context = NativePointer.of(nativePattern(16).address());
    private final Object output = NativePointer.of(nativePattern(16).address());

    InteropSlab(String name, NativeFunction function) {
      super(name);
      this.function = function;
    }

    @Override
    long run(int count, long x, Object extra) {
      try {
        for (int i = 0; i < count; i++)
          x = (Long) interop.execute(function, data, (long) SLAB_SIZE, aux, context, output, 3, -1, 7L, 11L, x);
      } catch (InteropException e) {
        throw failure(e);
      }
      return x;
    }
  }
}
