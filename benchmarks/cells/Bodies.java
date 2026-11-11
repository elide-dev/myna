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
import com.oracle.truffle.api.strings.TruffleString;
import dev.elide.myna.truffle.Borrow;
import dev.elide.myna.truffle.HeapBytes;
import dev.elide.myna.truffle.NativeBytes;
import dev.elide.myna.truffle.NativeCell;
import dev.elide.myna.truffle.NativeFunction;
import dev.elide.myna.truffle.NativeResource;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.charset.StandardCharsets;
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

  /** Eleven characters of which two take two UTF-8 bytes each: 13 bytes. */
  public static final String TEXT_UTF8 = "h\u00e9llo w\u00f6rld";

  /** Seven characters outside Latin-1 that take three UTF-8 bytes each: 21 bytes. */
  public static final String TEXT_WIDE = "\u3053\u3093\u306b\u3061\u306f\u4e16\u754c";

  /** The text a string case lends; the Python oracle agrees on its UTF-8 length. */
  public static String text(String name) {
    return name.endsWith("-utf8") ? TEXT_UTF8 : name.endsWith("-wide") ? TEXT_WIDE : TEXT;
  }

  /** The same text as a guest string: UTF-16, as JavaScript strings are. */
  static TruffleString guestText(String name) {
    return TruffleString.fromJavaStringUncached(text(name), TruffleString.Encoding.UTF_16);
  }

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

  /**
   * The pattern in an arena that stays open for the process: a shared one, whose scope a downcall
   * acquires atomically, or one confined to the measuring thread, whose owner it checks.
   */
  static MemorySegment scopedPattern(int size, boolean shared) {
    MemorySegment segment = (shared ? Arena.ofShared() : Arena.ofConfined()).allocate(size);
    MemorySegment.copy(pattern(size), 0, segment, ValueLayout.JAVA_BYTE, 0, size);
    return segment;
  }

  /** The slab's two byte buffers in one shared arena that stays open for the process. */
  static MemorySegment[] sharedSlab() {
    Arena arena = Arena.ofShared();
    MemorySegment data = arena.allocate(SLAB_SIZE);
    MemorySegment aux = arena.allocate(16);
    MemorySegment.copy(pattern(SLAB_SIZE), 0, data, ValueLayout.JAVA_BYTE, 0, SLAB_SIZE);
    MemorySegment.copy(pattern(16), 0, aux, ValueLayout.JAVA_BYTE, 0, 16);
    return new MemorySegment[] {data, aux};
  }

  public static Bodies create(String name) {
    if (name.contains("-create-")) {
      int size = Integer.parseInt(name.substring(name.lastIndexOf('-') + 1));
      String kind = name.substring(0, name.lastIndexOf('-'));
      return switch (kind) {
        case "direct-create-adopt" -> new DirectCreate(name, size, true);
        case "direct-create-scan" -> new DirectCreate(name, size, false);
        case "glue-create-adopt-native" -> new GlueCreate(name, new CellsTruffle.adopt_Cell(), NativeBytes.of(nativePattern(size), false));
        case "glue-create-adopt-copy" -> new GlueCreate(name, new CellsTruffle.adopt_Cell(), new HeapBytes(pattern(size), false));
        case "glue-create-scan-native" -> new GlueCreate(name, new CellsTruffle.scan_Cell(), NativeBytes.of(nativePattern(size), false));
        case "glue-create-scan-copy" -> new GlueCreate(name, new CellsTruffle.scan_Cell(), new HeapBytes(pattern(size), false));
        default -> throw new IllegalArgumentException(name);
      };
    }
    if (name.contains("-sum-")) {
      int size = Integer.parseInt(name.substring(name.lastIndexOf('-') + 1));
      String kind = name.substring(0, name.lastIndexOf('-'));
      return switch (kind) {
        case "direct-sum" -> new DirectSum(name, size);
        case "glue-sum-native" -> new GlueSum(name, NativeBytes.of(nativePattern(size), false));
        case "glue-sum-pin", "glue-sum-copy" -> new GlueSum(name, new HeapBytes(pattern(size), false));
        case "glue-sum-mixed" -> new MixedSum(name, size);
        case "hand-sum-pin" -> new HandSum(name, size, false);
        case "hand-sum-step-pin" -> new HandSum(name, size, true);
        case "inline-sum-pin" -> new InlineSum(name, size, false);
        case "inline-sum-step-pin" -> new InlineSum(name, size, true);
        case "direct-sum-step" -> new DirectSumStep(name, size);
        case "glue-sum-step-native" -> new GlueSum(name, new CellsTruffle.sumStep_Cell(), NativeBytes.of(nativePattern(size), false));
        case "glue-sum-step-pin" -> new GlueSum(name, new CellsTruffle.sumStep_Cell(), new HeapBytes(pattern(size), false));
        case "direct-ffm-sum" -> new DirectFfmSum(name, nativePattern(size));
        case "direct-ffm-sum-shared" -> new DirectFfmSum(name, scopedPattern(size, true));
        case "direct-ffm-sum-confined" -> new DirectFfmSum(name, scopedPattern(size, false));
        case "typed-ffm-sum-native" -> new TypedFfmSum(name, NativeBytes.of(nativePattern(size), false));
        case "typed-ffm-sum-shared" -> new TypedFfmSum(name, NativeBytes.of(scopedPattern(size, true), false));
        case "typed-ffm-sum-confined" -> new TypedFfmSum(name, NativeBytes.of(scopedPattern(size, false), false));
        case "glue-ffm-sum-native" -> new GlueSum(name, new FfmCellsTruffle.sum_Cell(), NativeBytes.of(nativePattern(size), false));
        case "glue-ffm-sum-shared" -> new GlueSum(name, new FfmCellsTruffle.sum_Cell(), NativeBytes.of(scopedPattern(size, true), false));
        case "glue-ffm-sum-confined" -> new GlueSum(name, new FfmCellsTruffle.sum_Cell(), NativeBytes.of(scopedPattern(size, false), false));
        case "glue-ffm-sum-copy" -> new GlueSum(name, new FfmCellsTruffle.sum_Cell(), new HeapBytes(pattern(size), false));
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
      case "glue-text", "glue-text-utf8", "glue-text-wide" -> new GlueText(name, text(name));
      case "glue-text-truffle", "glue-text-truffle-utf8", "glue-text-truffle-wide" -> new GlueText(name, guestText(name));
      case "glue-ffm-text" -> new GlueText(name, new FfmCellsTruffle.text_Cell(), TEXT);
      case "glue-text-truffle-fresh" -> new GlueFreshText(name);
      case "interop-leaf" -> new Interop(name, CellsTruffle.leaf());
      case "interop-step" -> new Interop(name, CellsTruffle.step());
      case "hand-step" -> new HandStep(name);
      case "glue-boundary-step" -> new Glue(name, new BoundaryCellsTruffle.step_Cell());
      case "direct-slab" -> new DirectSlab(name, false);
      case "direct-slab-step" -> new DirectSlab(name, true);
      case "hand-slab-step" -> new HandSlab(name);
      case "invoke-slab" -> new InvokeSlab(name, false);
      case "invoke-slab-step" -> new InvokeSlab(name, true);
      case "glue-slab-native" -> new GlueSlab(name, new CellsTruffle.slab_Cell(), false);
      case "glue-slab-heap" -> new GlueSlab(name, new CellsTruffle.slab_Cell(), true);
      case "glue-slab-bytes-heap" -> new GlueSlab(name, new CellsTruffle.slabBytes_Cell(), true);
      case "glue-slab-step-native" -> new GlueSlab(name, new CellsTruffle.slabStep_Cell(), false);
      case "glue-boundary-slab-step-native" -> new GlueSlab(name, new BoundaryCellsTruffle.slabStep_Cell(), false);
      case "direct-ffm-slab" -> new DirectFfmSlab(name, nativePattern(SLAB_SIZE), nativePattern(16));
      case "direct-ffm-slab-shared" -> new DirectFfmSlab(name, sharedSlab());
      case "direct-ffm-slab-mixed" -> new DirectFfmSlabMixed(name);
      case "glue-ffm-slab-native" -> new GlueSlab(name, new FfmCellsTruffle.slab_Cell(), false, false);
      case "glue-ffm-slab-shared" -> new GlueSlab(name, new FfmCellsTruffle.slab_Cell(), sharedSlab());
      case "typed-ffm-slab-native" -> new TypedFfmSlab(name, NativeBytes.of(nativePattern(SLAB_SIZE), false), NativeBytes.of(nativePattern(16), false));
      case "typed-ffm-slab-shared" -> new TypedFfmSlab(name, sharedSlab());
      case "typed-ffm-slab-mixed" -> new TypedFfmSlab(name, NativeBytes.of(nativePattern(SLAB_SIZE), false), new HeapBytes(pattern(16), false));
      case "glue-ffm-slab-heap" -> new GlueSlab(name, new FfmCellsTruffle.slab_Cell(), true, true);
      case "glue-ffm-slab-mixed" -> new GlueSlab(name, new FfmCellsTruffle.slab_Cell(), false, true);
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

  /**
   * A create and its release per iteration with raw addresses and the caller's own struct: the
   * floor for a create cell, which also allocates the runtime's struct and an owner.
   */
  static final class DirectCreate extends Bodies {
    private final long address;
    private final long size;
    private final long struct;
    private final boolean adopt;

    DirectCreate(String name, int size, boolean adopt) {
      super(name);
      this.address = nativePattern(size).address();
      this.size = size;
      this.struct = Arena.global().allocate(16, 8).address();
      this.adopt = adopt;
    }

    @Override
    long run(int count, long x, Object extra) {
      for (int i = 0; i < count; i++) {
        if (adopt) {
          if (Cells.adopt(WordFactory.pointer(address), size, WordFactory.pointer(struct)) != 0) throw failure(null);
          Cells.adoptedRelease(WordFactory.pointer(struct));
        } else {
          if (Cells.scan(WordFactory.pointer(address), size, WordFactory.pointer(struct)) != 0) throw failure(null);
          Cells.scannedRelease(WordFactory.pointer(struct));
        }
      }
      return x + Cells.createdTotal();
    }
  }

  /** A create cell and its owner's close per iteration: the guest passes a buffer for the struct. */
  static final class GlueCreate extends Bodies {
    @Node.Child private NativeCell cell;

    // Deliberately not final: guest values, which partial evaluation must not fold to constants.
    private Object buffer;
    private Object out;

    GlueCreate(String name, NativeCell cell, Object buffer) {
      super(name);
      this.cell = cell;
      this.buffer = buffer;
      this.out = NativeBytes.of(Arena.global().allocate(16, 8), true);
    }

    @Override
    long run(int count, long x, Object extra) {
      for (int i = 0; i < count; i++) ((NativeResource) cell.execute(new Object[] {buffer, out})).close();
      return x + Cells.createdTotal();
    }
  }

  /** The FFM binding with the caller's own segment: the floor for an FFM cell that lends bytes. */
  static final class DirectFfmSum extends Bodies {
    private final MemorySegment segment;
    private final long size;

    DirectFfmSum(String name, MemorySegment segment) {
      super(name);
      this.segment = segment;
      this.size = segment.byteSize();
    }

    @Override
    long run(int count, long x, Object extra) {
      try {
        for (int i = 0; i < count; i++) x = FfmCellsFFM.sum(segment, size, x);
      } catch (Throwable t) {
        throw failure(t);
      }
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

  /** Typed glue over the FFM sum cell, lending one buffer. */
  static final class TypedFfmSum extends Bodies {
    @Node.Child private FfmCellsTruffle.sum_Cell cell = new FfmCellsTruffle.sum_Cell();

    // Deliberately not final: a guest value, which partial evaluation must not fold to a constant.
    private Object buffer;

    TypedFfmSum(String name, Object buffer) {
      super(name);
      this.buffer = buffer;
    }

    @Override
    long run(int count, long x, Object extra) {
      for (int i = 0; i < count; i++) x = cell.executeLong(new Object[] {buffer, x});
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

    // Deliberately not final: a guest value, which partial evaluation must not fold to a constant.
    private Object buffer;

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

  /** One call site that lends native memory and a pinned heap array alternately. */
  static final class MixedSum extends Bodies {
    @Node.Child private NativeCell cell = new CellsTruffle.sum_Cell();
    private Object nativeBuffer;
    private Object heapBuffer;

    MixedSum(String name, int size) {
      super(name);
      this.nativeBuffer = NativeBytes.of(nativePattern(size), false);
      this.heapBuffer = new HeapBytes(pattern(size), false);
    }

    @Override
    long run(int count, long x, Object extra) {
      for (int i = 0; i < count; i++) x = (Long) cell.execute(new Object[] {(i & 1) == 0 ? nativeBuffer : heapBuffer, x});
      return x;
    }
  }

  /** A string argument: a Java string, or a guest string as a Truffle language passes it. */
  static final class GlueText extends Bodies {
    @Node.Child private NativeCell cell;

    // Deliberately not final: a guest value, which partial evaluation must not fold to a constant.
    private Object text;

    GlueText(String name, Object text) {
      this(name, new CellsTruffle.text_Cell(), text);
    }

    GlueText(String name, NativeCell cell, Object text) {
      super(name);
      this.cell = cell;
      this.text = text;
    }

    @Override
    long run(int count, long x, Object extra) {
      for (int i = 0; i < count; i++) x = (Long) cell.execute(new Object[] {text, x});
      return x;
    }
  }

  /**
   * A guest string that is new on every call, as guest code that builds its strings passes them:
   * nothing cached on an earlier call (a Java string, a transcoded copy) can be reused.
   */
  static final class GlueFreshText extends Bodies {
    @Node.Child private NativeCell cell = new CellsTruffle.text_Cell();
    @Node.Child private TruffleString.FromByteArrayNode fromBytes = TruffleString.FromByteArrayNode.create();

    // Not final, like the other guest values.
    private byte[] bytes = TEXT.getBytes(StandardCharsets.ISO_8859_1);

    GlueFreshText(String name) {
      super(name);
    }

    @Override
    long run(int count, long x, Object extra) {
      for (int i = 0; i < count; i++) {
        TruffleString fresh = fromBytes.execute(bytes, 0, bytes.length, TruffleString.Encoding.ISO_8859_1, false);
        x = (Long) cell.execute(new Object[] {fresh, x});
      }
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
    private byte[] array; // not final, like the cells' guest values
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
    private byte[] array; // not final, like the cells' guest values
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

  /** The FFM slab binding with the caller's own segments: the floor for the FFM slab cells. */
  static final class DirectFfmSlab extends Bodies {
    private final MemorySegment data;
    private final MemorySegment aux;

    DirectFfmSlab(String name, MemorySegment data, MemorySegment aux) {
      super(name);
      this.data = data;
      this.aux = aux;
    }

    DirectFfmSlab(String name, MemorySegment[] buffers) {
      this(name, buffers[0], buffers[1]);
    }

    @Override
    long run(int count, long x, Object extra) {
      try {
        for (int i = 0; i < count; i++) x = FfmCellsFFM.slab(data, SLAB_SIZE, aux, aux, data, 3, -1, 7, 11, x);
      } catch (Throwable t) {
        throw failure(t);
      }
      return x;
    }
  }

  /**
   * The FFM slab binding with a native segment and a heap array, copied for each call as the cells
   * copy it: through Myna's own borrow, NUL-terminated as a C string parameter is.
   */
  static final class DirectFfmSlabMixed extends Bodies {
    private final MemorySegment data = nativePattern(SLAB_SIZE);
    private final MemorySegment other = nativePattern(16);
    private byte[] aux = pattern(16); // not final, like the cells' guest values

    DirectFfmSlabMixed(String name) {
      super(name);
    }

    /** The call behind one boundary with the copy's address, as the cells make their FFM calls. */
    @CompilerDirectives.TruffleBoundary
    private static long call(MemorySegment data, long aux, MemorySegment other, long x) {
      try {
        return FfmCellsFFM.slab(data, SLAB_SIZE, MemorySegment.ofAddress(aux), other, other, 3, -1, 7, 11, x);
      } catch (Throwable t) {
        throw failure(t);
      }
    }

    @Override
    long run(int count, long x, Object extra) {
      for (int i = 0; i < count; i++) {
        // Copied in compiled code, which keeps the borrow out of the heap, as the cells do.
        Borrow copy = Borrow.ofArray(aux, false, true);
        try {
          x = call(data, copy.address(), other, x);
        } finally {
          copy.release();
        }
      }
      return x;
    }
  }

  /** Typed glue over the FFM slab cell: ten guest values, primitive result. */
  static final class TypedFfmSlab extends Bodies {
    @Node.Child private FfmCellsTruffle.slab_Cell cell = new FfmCellsTruffle.slab_Cell();

    // Not final: guest values, which partial evaluation must not fold to constants.
    private Object data;
    private Object aux;
    private Object context = NativePointer.of(nativePattern(16).address());
    private Object output = NativePointer.of(nativePattern(16).address());

    TypedFfmSlab(String name, Object data, Object aux) {
      super(name);
      this.data = data;
      this.aux = aux;
    }

    TypedFfmSlab(String name, MemorySegment[] buffers) {
      this(name, NativeBytes.of(buffers[0], false), NativeBytes.of(buffers[1], false));
    }

    @Override
    long run(int count, long x, Object extra) {
      for (int i = 0; i < count; i++)
        x = cell.executeLong(new Object[] {data, (long) SLAB_SIZE, aux, context, output, 3, -1, 7L, 11L, x});
      return x;
    }
  }

  /** The cells' static invoke entries: native carriers and raw addresses, no coercion. */
  static final class InvokeSlab extends Bodies {
    private final long data = nativePattern(SLAB_SIZE).address();
    private final long aux = nativePattern(16).address();
    private final boolean step;

    InvokeSlab(String name, boolean step) {
      super(name);
      this.step = step;
    }

    @Override
    long run(int count, long x, Object extra) {
      for (int i = 0; i < count; i++)
        x =
            step
                ? CellsTruffle.slabStep_Cell.invoke(data, SLAB_SIZE, aux, aux, data, 3, -1, 7, 11, x)
                : CellsTruffle.slab_Cell.invoke(data, SLAB_SIZE, aux, aux, data, 3, -1, 7, 11, x);
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

    // Not final: guest values, which partial evaluation must not fold to constants.
    private Object data;
    private Object aux;
    private Object context;
    private Object output;

    GlueSlab(String name, NativeCell cell, boolean heap) {
      this(name, cell, heap, heap);
    }

    GlueSlab(String name, NativeCell cell, MemorySegment[] buffers) {
      super(name);
      this.cell = cell;
      this.data = NativeBytes.of(buffers[0], false);
      this.aux = NativeBytes.of(buffers[1], false);
      this.context = NativePointer.of(nativePattern(16).address());
      this.output = NativePointer.of(nativePattern(16).address());
    }

    /** Either byte buffer from the heap or from native memory, independently. */
    GlueSlab(String name, NativeCell cell, boolean heapData, boolean heapAux) {
      super(name);
      this.cell = cell;
      this.data = heapData ? new HeapBytes(pattern(SLAB_SIZE), false) : NativeBytes.of(nativePattern(SLAB_SIZE), false);
      this.aux = heapAux ? new HeapBytes(pattern(16), false) : NativeBytes.of(nativePattern(16), false);
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

    // Not final: guest values, which partial evaluation must not fold to constants.
    private Object data = NativeBytes.of(nativePattern(SLAB_SIZE), false);
    private Object aux = NativeBytes.of(nativePattern(16), false);
    private Object context = NativePointer.of(nativePattern(16).address());
    private Object output = NativePointer.of(nativePattern(16).address());

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
