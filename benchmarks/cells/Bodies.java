// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package bench.nodes;

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
      case "glue-text" -> new GlueText(name, new CellsTruffle.text_Cell());
      case "interop-leaf" -> new Interop(name, CellsTruffle.leaf());
      case "interop-step" -> new Interop(name, CellsTruffle.step());
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

  static final class GlueSum extends Bodies {
    @Node.Child private NativeCell cell;
    private final Object buffer;

    GlueSum(String name, Object buffer) {
      super(name);
      this.cell = new CellsTruffle.sum_Cell();
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
}
