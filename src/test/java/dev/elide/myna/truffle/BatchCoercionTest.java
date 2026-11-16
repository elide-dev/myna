// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package dev.elide.myna.truffle;

import static org.junit.jupiter.api.Assertions.*;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/** Batches of buffers (#132): {@link BatchCoercion} and the batch kind of {@link Borrow}. */
class BatchCoercionTest {
  /** Where the scratch stack's top is: a probe borrow lands there, and is released at once. */
  private static long top() {
    Borrow probe = Borrow.ofString("probe", true);
    long at = probe.address();
    probe.release();
    return at;
  }

  private static byte[] element(Batch batch, int index, boolean wide) {
    long count = batch.length();
    MemorySegment arrays = MemorySegment.ofAddress(batch.address()).reinterpret(16 * count);
    long address = arrays.getAtIndex(ValueLayout.JAVA_LONG, index);
    long length =
        wide
            ? arrays.getAtIndex(ValueLayout.JAVA_LONG, count + index)
            : arrays.getAtIndex(ValueLayout.JAVA_INT, 2 * count + index);
    if (address == 0) {
      assertEquals(0, length);
      return null;
    }
    return MemorySegment.ofAddress(address).reinterpret(length).toArray(ValueLayout.JAVA_BYTE);
  }

  @Test
  void theEmptyBatchIsNullArraysWithCountZero() {
    var coercion = new BatchCoercion("f", "sources", false, true, Long.MAX_VALUE, false, false);
    for (Object empty : new Object[] {null, NativeVoid.INSTANCE, BufferArray.of()}) {
      Batch batch = coercion.execute(empty);
      assertEquals(0, batch.address());
      assertEquals(0, batch.length());
      batch.release();
    }
    NativeCellException wrong =
        assertThrows(
            NativeCellException.class, () -> coercion.execute(new HeapBytes(new byte[1], false)));
    assertTrue(
        wrong.getMessage().contains("array of buffers or strings, or null"), wrong.getMessage());
  }

  @Test
  void eachElementIsLentAsASingleBufferIs() {
    long top = top();
    try (Arena arena = Arena.ofConfined()) {
      byte[] a = {1, 2, 3}, b = {(byte) 255};
      MemorySegment native1 = arena.allocateFrom(ValueLayout.JAVA_BYTE, b);
      for (boolean wide : new boolean[] {true, false}) {
        var coercion =
            new BatchCoercion(
                "f", "sources", true, wide, wide ? Long.MAX_VALUE : 4294967295L, false, false);
        Batch batch =
            coercion.execute(
                BufferArray.of(
                    new HeapBytes(a, false),
                    new HeapBytes(new byte[0], false),
                    NativeBytes.of(native1, false),
                    "hé",
                    null));
        assertEquals(5, batch.length());
        assertArrayEquals(a, element(batch, 0, wide));
        assertArrayEquals(new byte[0], element(batch, 1, wide));
        assertArrayEquals(b, element(batch, 2, wide));
        assertArrayEquals("hé".getBytes(StandardCharsets.UTF_8), element(batch, 3, wide));
        assertNull(element(batch, 4, wide));
        // Native memory is lent in place.
        MemorySegment arrays = MemorySegment.ofAddress(batch.address()).reinterpret(16 * 5);
        assertEquals(native1.address(), arrays.getAtIndex(ValueLayout.JAVA_LONG, 2));
        batch.release();
        batch.release();
        assertEquals(top, top());
      }
    }
  }

  @Test
  void largeBatchesAndElementsTakeArenas() {
    long top = top();
    var coercion = new BatchCoercion("f", "sources", false, true, Long.MAX_VALUE, false, false);
    Object[] many = new Object[1100];
    for (int i = 0; i < many.length; i++) many[i] = new HeapBytes(new byte[] {(byte) i}, false);
    Batch batch = coercion.execute(BufferArray.of(many));
    assertEquals(1100, batch.length());
    assertArrayEquals(new byte[] {(byte) 1099}, element(batch, 1099, true));
    batch.release();
    // The stack grows toward the high-water mark on each next use, so its base moves until the
    // block holds every copy; a balanced stack then lends the probe at the same place each round.
    long previous = -1;
    top = top();
    for (int round = 0; round < 8 && top != previous; round++) {
      coercion.execute(BufferArray.of(many)).release();
      previous = top;
      top = top();
    }
    assertEquals(previous, top);
    byte[] large = new byte[20_000];
    large[19_999] = 7;
    batch = coercion.execute(BufferArray.of(new HeapBytes(large, false), "x"));
    assertArrayEquals(large, element(batch, 0, true));
    batch.release();
    assertEquals(top, top());
  }

  @Test
  void aFailedElementReleasesThoseBeforeIt() {
    long top = top();
    var coercion = new BatchCoercion("f", "sources", false, false, 2, false, false);
    byte[] large = new byte[20_000];
    for (Object[] elements :
        new Object[][] {
          {new HeapBytes(new byte[] {1}, false), null},
          {new HeapBytes(large, false), "ok", 42},
          {"ok", new HeapBytes(new byte[3], false)},
        }) {
      assertThrows(NativeCellException.class, () -> coercion.execute(BufferArray.of(elements)));
      assertEquals(top, top());
    }
    NativeCellException tooLong =
        assertThrows(NativeCellException.class, () -> coercion.execute(BufferArray.of("abc")));
    assertTrue(tooLong.getMessage().contains("element 0 has 3 bytes"), tooLong.getMessage());
    NativeCellException shrank =
        assertThrows(NativeCellException.class, () -> coercion.execute(new Shrinking()));
    assertTrue(shrank.getMessage().contains("element 1 cannot be read"), shrank.getMessage());
    assertEquals(top, top());
    // An untrusted size outside 0 to MAX_COUNT is refused before anything is lent.
    for (long size : new long[] {Integer.MAX_VALUE, -1, Long.MIN_VALUE}) {
      NativeCellException bad =
          assertThrows(NativeCellException.class, () -> coercion.execute(new Sized(size)));
      assertTrue(bad.getMessage().contains("got an array of size " + size), bad.getMessage());
      assertEquals(top, top());
    }
    // In the arena fallback too, a failed element releases the arena and every element before it.
    var wide = new BatchCoercion("f", "sources", false, true, Long.MAX_VALUE, false, false);
    Object[] many = new Object[1100];
    java.util.Arrays.fill(many, "x");
    many[1099] = 42;
    assertThrows(NativeCellException.class, () -> wide.execute(BufferArray.of(many)));
    long previous = -1;
    top = top();
    for (int round = 0; round < 8 && top != previous; round++) {
      assertThrows(NativeCellException.class, () -> wide.execute(BufferArray.of(many)));
      previous = top;
      top = top();
    }
    assertEquals(previous, top);
  }

  /**
   * A host {@code ProxyArray} whose size is negative, through a polyglot context: the cell refuses
   * it as a guest error instead of passing a negative count to native code.
   */
  @Test
  void aNegativeHostArraySizeIsAGuestError() {
    var cell = new CountCell();
    try (org.graalvm.polyglot.Context context =
        org.graalvm.polyglot.Context.newBuilder()
            .option("engine.WarnInterpreterOnly", "false")
            .build()) {
      var function = context.asValue(cell.function());
      var negative =
          new org.graalvm.polyglot.proxy.ProxyArray() {
            public Object get(long index) {
              throw new ArrayIndexOutOfBoundsException();
            }

            public void set(long index, org.graalvm.polyglot.Value value) {
              throw new UnsupportedOperationException();
            }

            public long getSize() {
              return -1;
            }
          };
      var failure =
          assertThrows(
              org.graalvm.polyglot.PolyglotException.class, () -> function.execute(negative));
      assertTrue(failure.getMessage().contains("got an array of size -1"), failure.getMessage());
      assertEquals(
          2L, function.execute(org.graalvm.polyglot.proxy.ProxyArray.fromArray("a", "b")).asLong());
    }
  }

  /**
   * An FFM leaf cell lends a confined segment in place; guest code run while a later element is
   * read (here a host proxy array's {@code get(1)}) closes its arena. The recheck before the call
   * refuses the batch, so the call never sees the freed address, and the stack is balanced.
   */
  @Test
  void anElementClosedWhileLaterOnesAreCoercedFailsBeforeTheCall() {
    long top = top();
    var cell = new LeafCell();
    try (org.graalvm.polyglot.Context context =
        org.graalvm.polyglot.Context.newBuilder()
            .option("engine.WarnInterpreterOnly", "false")
            .build()) {
      var function = context.asValue(cell.function());
      Arena arena = Arena.ofConfined();
      MemorySegment first = arena.allocate(4);
      NativeBytes element = NativeBytes.of(first, false);
      var closing =
          new org.graalvm.polyglot.proxy.ProxyArray() {
            public Object get(long index) {
              if (index == 0) return element;
              arena.close();
              return "later";
            }

            public void set(long index, org.graalvm.polyglot.Value value) {
              throw new UnsupportedOperationException();
            }

            public long getSize() {
              return 2;
            }
          };
      var failure =
          assertThrows(
              org.graalvm.polyglot.PolyglotException.class, () -> function.execute(closing));
      assertTrue(failure.getMessage().contains("was closed before the call"), failure.getMessage());
      assertEquals(0, cell.calls);
      // Without the close, the same element is lent in place and the call is made.
      try (Arena open = Arena.ofConfined()) {
        MemorySegment live = open.allocate(4);
        assertEquals(
            live.address(),
            function
                .execute(
                    org.graalvm.polyglot.proxy.ProxyArray.fromArray(
                        NativeBytes.of(live, false), "x"))
                .asLong());
        assertEquals(1, cell.calls);
      }
    }
    assertEquals(top, top());
  }

  /** An FFM leaf cell that returns the address lent for element 0, as the call would see it. */
  static final class LeafCell extends NativeCell {
    @com.oracle.truffle.api.nodes.Node.Child
    private BatchCoercion batch =
        new BatchCoercion("first", "sources", false, true, Long.MAX_VALUE, true, true);

    int calls;

    LeafCell() {
      super("first", 1);
    }

    @Override
    public Object execute(Object[] arguments) {
      checkArity(arguments);
      Batch lent = batch.execute(arguments[0]);
      try {
        lent.recheck(this);
        calls++;
        return MemorySegment.ofAddress(lent.address()).reinterpret(8).get(ValueLayout.JAVA_LONG, 0);
      } finally {
        lent.release();
      }
    }
  }

  /** The sentinel thread inherits nothing: an inheritable thread-local's childValue never runs. */
  @Test
  void theSentinelInheritsNoThreadLocals() {
    var calls = new java.util.concurrent.atomic.AtomicInteger();
    var local =
        new InheritableThreadLocal<String>() {
          @Override
          protected String childValue(String parent) {
            calls.incrementAndGet();
            return parent;
          }
        };
    local.set("parent");
    try {
      Thread sentinel = BatchCoercion.Sentinel.create();
      assertEquals(Thread.State.NEW, sentinel.getState());
      assertNotNull(BatchCoercion.Sentinel.THREAD);
      assertEquals(0, calls.get());
    } finally {
      local.remove();
    }
  }

  /**
   * Two elements lent in place are both checked again before the call, and the arrays and lengths
   * the call receives are where the batch says.
   */
  @Test
  void everyElementLentInPlaceIsRechecked() {
    var leaf = new BatchCoercion("f", "sources", false, true, Long.MAX_VALUE, true, true);
    Batch empty = leaf.execute(null);
    assertEquals(0, empty.lengths());
    empty.recheck(null);
    empty.release();
    Arena first = Arena.ofConfined();
    Arena second = Arena.ofConfined();
    Batch batch =
        leaf.execute(
            BufferArray.of(
                NativeBytes.of(first.allocate(2), false),
                NativeBytes.of(second.allocate(3), false)));
    assertEquals(batch.address() + 16, batch.lengths());
    batch.recheck(null);
    second.close();
    NativeCellException closed = assertThrows(NativeCellException.class, () -> batch.recheck(null));
    assertTrue(closed.getMessage().contains("was closed before the call"), closed.getMessage());
    batch.release();
    first.close();
  }

  /**
   * A scoped element that is closed, too large for the scratch stack, or confined to another thread
   * is refused before the call; an array without a size and a foreign null are handled.
   */
  @Test
  void unavailableScopedElementsAndOddArraysAreRefused() throws Exception {
    long top = top();
    var copying = new BatchCoercion("f", "sources", false, true, Long.MAX_VALUE, true, false);
    var leaf = new BatchCoercion("f", "sources", false, true, Long.MAX_VALUE, true, true);
    Arena closing = Arena.ofConfined();
    MemorySegment big = closing.allocate(20_000);
    closing.close();
    for (var coercion : new BatchCoercion[] {copying, leaf}) {
      NativeCellException refused =
          assertThrows(
              NativeCellException.class,
              () -> coercion.execute(BufferArray.of(NativeBytes.of(big, false))));
      assertTrue(refused.getMessage().contains("closed or confined"), refused.getMessage());
    }
    var elsewhere = new java.util.concurrent.atomic.AtomicReference<MemorySegment>();
    Thread owner = new Thread(() -> elsewhere.set(Arena.ofConfined().allocate(4)));
    owner.start();
    owner.join();
    NativeCellException foreign =
        assertThrows(
            NativeCellException.class,
            () -> leaf.execute(BufferArray.of(NativeBytes.of(elsewhere.get(), false))));
    assertTrue(foreign.getMessage().contains("closed or confined"), foreign.getMessage());
    assertEquals(top, top());
    assertEquals(0, copying.execute(new TruffleRuntimeTest.ForeignNull()).length());
    NativeCellException sizeless =
        assertThrows(NativeCellException.class, () -> copying.execute(new Sizeless()));
    assertTrue(
        sizeless.getMessage().contains("array of buffers or strings"), sizeless.getMessage());
  }

  /**
   * On a fresh thread, whose scratch block is small, a batch whose arrays or scoped copies do not
   * fit beside an outstanding copy takes arenas instead, and still lends exactly its bytes.
   */
  @Test
  void batchesThatDoNotFitBesideOutstandingCopiesTakeArenas() throws Exception {
    var failure = new java.util.concurrent.atomic.AtomicReference<Throwable>();
    Thread thread =
        new Thread(() -> {
          try {
            Borrow outstanding = Borrow.ofString("x".repeat(3000), true);
            var copying =
                new BatchCoercion("f", "sources", false, true, Long.MAX_VALUE, true, false);
            Object[] many = new Object[100];
            java.util.Arrays.fill(many, "y");
            Batch arrays = copying.execute(BufferArray.of(many));
            assertArrayEquals(new byte[] {'y'}, element(arrays, 99, true));
            arrays.release();
            try (Arena shared = Arena.ofShared()) {
              MemorySegment scoped = shared.allocate(2000);
              scoped.fill((byte) 3);
              Batch copied = copying.execute(BufferArray.of(NativeBytes.of(scoped, false)));
              assertEquals(3, element(copied, 0, true)[1999]);
              copied.release();
            }
            outstanding.release();
          } catch (Throwable e) {
            failure.set(e);
          }
        });
    thread.start();
    thread.join();
    if (failure.get() != null) throw new AssertionError(failure.get());
  }

  /** A cell that lends a batch and returns its count. */
  static final class CountCell extends NativeCell {
    @com.oracle.truffle.api.nodes.Node.Child
    private BatchCoercion batch =
        new BatchCoercion("count", "sources", false, true, Long.MAX_VALUE, false, false);

    CountCell() {
      super("count", 1);
    }

    @Override
    public Object execute(Object[] arguments) {
      checkArity(arguments);
      Batch lent = batch.execute(arguments[0]);
      try {
        return lent.length();
      } finally {
        lent.release();
      }
    }
  }

  @Test
  void aBatchReleasedOutOfOrderFailsAndLeavesTheStack() {
    var coercion = new BatchCoercion("f", "sources", false, true, Long.MAX_VALUE, false, false);
    Batch batch = coercion.execute(BufferArray.of("abc"));
    Borrow later = Borrow.ofString("later", true);
    assertThrows(IllegalStateException.class, batch::release);
    later.release();
    batch.release();
  }

  @Test
  void ffmLeafCellsLendConfinedElementsInPlace() {
    long top = top();
    var leaf = new BatchCoercion("f", "sources", false, true, Long.MAX_VALUE, true, true);
    try (Arena confined = Arena.ofConfined();
        Arena shared = Arena.ofShared()) {
      MemorySegment mine = confined.allocate(3);
      MemorySegment everyone = shared.allocate(2);
      everyone.fill((byte) 6);
      Batch batch =
          leaf.execute(
              BufferArray.of(NativeBytes.of(mine, false), NativeBytes.of(everyone, false)));
      MemorySegment arrays = MemorySegment.ofAddress(batch.address()).reinterpret(32);
      assertEquals(mine.address(), arrays.getAtIndex(ValueLayout.JAVA_LONG, 0));
      assertNotEquals(everyone.address(), arrays.getAtIndex(ValueLayout.JAVA_LONG, 1));
      assertArrayEquals(new byte[] {6, 6}, element(batch, 1, true));
      batch.release();
    }
    Arena closing = Arena.ofConfined();
    MemorySegment closed = closing.allocate(3);
    closing.close();
    assertThrows(
        NativeCellException.class,
        () -> leaf.execute(BufferArray.of(NativeBytes.of(closed, false))));
    // A segment too large for the scratch stack is copied into an arena of its own.
    try (Arena shared = Arena.ofShared()) {
      MemorySegment big = shared.allocate(20_000);
      big.set(ValueLayout.JAVA_BYTE, 19_999, (byte) 9);
      Batch batch = leaf.execute(BufferArray.of(NativeBytes.of(big, false)));
      assertEquals(9, element(batch, 0, true)[19_999]);
      batch.release();
    }
    assertEquals(top, top());
  }

  @Test
  void ffmCellsCopyScopedElements() {
    long top = top();
    var coercion = new BatchCoercion("f", "sources", false, true, Long.MAX_VALUE, true, false);
    MemorySegment global = Arena.global().allocate(2);
    global.fill((byte) 4);
    try (Arena arena = Arena.ofConfined()) {
      MemorySegment confined = arena.allocate(3);
      confined.fill((byte) 5);
      Batch batch =
          coercion.execute(
              BufferArray.of(NativeBytes.of(global, false), NativeBytes.of(confined, false)));
      MemorySegment arrays = MemorySegment.ofAddress(batch.address()).reinterpret(32);
      assertEquals(global.address(), arrays.getAtIndex(ValueLayout.JAVA_LONG, 0));
      assertNotEquals(confined.address(), arrays.getAtIndex(ValueLayout.JAVA_LONG, 1));
      assertArrayEquals(new byte[] {5, 5, 5}, element(batch, 1, true));
      batch.release();
    }
    Arena closing = Arena.ofConfined();
    MemorySegment closed = closing.allocate(3);
    closing.close();
    assertThrows(
        NativeCellException.class,
        () -> coercion.execute(BufferArray.of("a", NativeBytes.of(closed, false))));
    assertEquals(top, top());
  }

  @Test
  void bufferArraysAreReadOnlyInteropArrays() throws Exception {
    InteropLibrary interop = InteropLibrary.getUncached();
    BufferArray array = BufferArray.of("a", null);
    assertEquals(2, interop.getArraySize(array));
    assertTrue(interop.isArrayElementReadable(array, 1));
    assertFalse(interop.isArrayElementReadable(array, 2));
    assertFalse(interop.isArrayElementReadable(array, -1));
    assertEquals(NativeVoid.INSTANCE, interop.readArrayElement(array, 1));
    assertThrows(InvalidArrayIndexException.class, () -> interop.readArrayElement(array, 2));
    assertThrows(InvalidArrayIndexException.class, () -> interop.readArrayElement(array, -1));
    assertEquals("BufferArray[2]", array.toString());
  }

  /** An array that reports two elements but has none by the time they are read. */
  @ExportLibrary(InteropLibrary.class)
  static final class Shrinking implements TruffleObject {
    @ExportMessage
    boolean hasArrayElements() {
      return true;
    }

    @ExportMessage
    long getArraySize() {
      return 2;
    }

    @ExportMessage
    boolean isArrayElementReadable(long index) {
      return false;
    }

    @ExportMessage
    Object readArrayElement(long index) throws InvalidArrayIndexException {
      if (index == 0) return "a";
      throw InvalidArrayIndexException.create(index);
    }
  }

  /** An array that has elements but cannot say how many. */
  @ExportLibrary(InteropLibrary.class)
  static final class Sizeless implements TruffleObject {
    @ExportMessage
    boolean hasArrayElements() {
      return true;
    }

    @ExportMessage
    long getArraySize() throws com.oracle.truffle.api.interop.UnsupportedMessageException {
      throw com.oracle.truffle.api.interop.UnsupportedMessageException.create();
    }

    @ExportMessage
    boolean isArrayElementReadable(long index) {
      return false;
    }

    @ExportMessage
    Object readArrayElement(long index) throws InvalidArrayIndexException {
      throw InvalidArrayIndexException.create(index);
    }
  }

  /** An array whose size is whatever it is told, larger than any batch or negative. */
  @ExportLibrary(InteropLibrary.class)
  static final class Sized implements TruffleObject {
    final long size;

    Sized(long size) {
      this.size = size;
    }

    @ExportMessage
    boolean hasArrayElements() {
      return true;
    }

    @ExportMessage
    long getArraySize() {
      return size;
    }

    @ExportMessage
    boolean isArrayElementReadable(long index) {
      return false;
    }

    @ExportMessage
    Object readArrayElement(long index) throws InvalidArrayIndexException {
      throw InvalidArrayIndexException.create(index);
    }
  }
}
