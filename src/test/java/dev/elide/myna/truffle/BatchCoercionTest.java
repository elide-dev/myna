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

  private static byte[] element(Borrow batch, int index, boolean wide) {
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
    var coercion = new BatchCoercion("f", "sources", false, true, Long.MAX_VALUE, false);
    for (Object empty : new Object[] {null, NativeVoid.INSTANCE, BufferArray.of()}) {
      Borrow batch = coercion.execute(empty);
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
                "f", "sources", true, wide, wide ? Long.MAX_VALUE : 4294967295L, false);
        Borrow batch =
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
    var coercion = new BatchCoercion("f", "sources", false, true, Long.MAX_VALUE, false);
    Object[] many = new Object[1100];
    for (int i = 0; i < many.length; i++) many[i] = new HeapBytes(new byte[] {(byte) i}, false);
    Borrow batch = coercion.execute(BufferArray.of(many));
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
    var coercion = new BatchCoercion("f", "sources", false, false, 2, false);
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
    assertTrue(
        shrank.getMessage().contains("the array changed while it was lent"), shrank.getMessage());
    assertEquals(top, top());
    NativeCellException tooMany =
        assertThrows(NativeCellException.class, () -> coercion.execute(new Huge()));
    assertTrue(tooMany.getMessage().contains("at most"), tooMany.getMessage());
  }

  @Test
  void aBatchReleasedOutOfOrderFailsAndLeavesTheStack() {
    var coercion = new BatchCoercion("f", "sources", false, true, Long.MAX_VALUE, false);
    Borrow batch = coercion.execute(BufferArray.of("abc"));
    Borrow later = Borrow.ofString("later", true);
    assertThrows(IllegalStateException.class, batch::release);
    later.release();
    batch.release();
  }

  @Test
  void ffmCellsCopyScopedElements() {
    long top = top();
    var coercion = new BatchCoercion("f", "sources", false, true, Long.MAX_VALUE, true);
    MemorySegment global = Arena.global().allocate(2);
    global.fill((byte) 4);
    try (Arena arena = Arena.ofConfined()) {
      MemorySegment confined = arena.allocate(3);
      confined.fill((byte) 5);
      Borrow batch =
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

  /** An array larger than any batch. */
  @ExportLibrary(InteropLibrary.class)
  static final class Huge implements TruffleObject {
    @ExportMessage
    boolean hasArrayElements() {
      return true;
    }

    @ExportMessage
    long getArraySize() {
      return Integer.MAX_VALUE;
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
