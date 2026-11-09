// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package dev.elide.myna.truffle;

import static org.junit.jupiter.api.Assertions.*;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidBufferOffsetException;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.nodes.Node;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.ref.Cleaner;
import java.math.BigInteger;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/** The language-neutral runtime on HotSpot with Truffle's default runtime. */
class TruffleRuntimeTest {
  private static final InteropLibrary INTEROP = InteropLibrary.getUncached();

  /** A hand-written cell shaped like a generated one: coerces, calls, converts. */
  static final class SumCell extends NativeCell {
    @Node.Child private IntegerCoercion left;
    @Node.Child private IntegerCoercion right;

    SumCell(CoercionPolicy policy) {
      super("sum", 2);
      left = new IntegerCoercion("sum", "left", 32, false, policy);
      right = new IntegerCoercion("sum", "right", 32, false, policy);
    }

    @Override
    public Object execute(Object[] arguments) {
      checkArity(arguments);
      return (int) left.execute(arguments[0]) + (int) right.execute(arguments[1]);
    }
  }

  /** A one-argument cell shaped like a generated {@code ptr<T>} binding, for coercion tests. */
  static final class PointerCell extends NativeCell {
    @Node.Child private PointerCoercion pointer;

    PointerCell(boolean nullable, boolean rawNumbers) {
      super("f", 1);
      pointer = new PointerCoercion("f", "p", nullable, rawNumbers);
    }

    void useInterop(InteropLibrary library) throws ReflectiveOperationException {
      var field = Coercion.class.getDeclaredField("interop");
      field.setAccessible(true);
      field.set(pointer, library);
    }

    @Override
    public Object execute(Object[] arguments) {
      checkArity(arguments);
      return pointer.execute(arguments[0]);
    }
  }

  /** A buffer that is neither {@link NativeBytes} nor {@link HeapBytes}: the generic copy path. */
  @ExportLibrary(InteropLibrary.class)
  static final class ForeignBuffer implements TruffleObject {
    final byte[] data;
    final boolean writable;

    ForeignBuffer(byte[] data, boolean writable) {
      this.data = data;
      this.writable = writable;
    }

    @ExportMessage
    boolean hasBufferElements() {
      return true;
    }

    @ExportMessage
    boolean isBufferWritable() {
      return writable;
    }

    @ExportMessage
    long getBufferSize() {
      return data.length;
    }

    @ExportMessage
    byte readBufferByte(long offset) throws InvalidBufferOffsetException {
      if (offset < 0 || offset >= data.length) throw InvalidBufferOffsetException.create(offset, 1);
      return data[(int) offset];
    }

    @ExportMessage
    void readBuffer(long offset, byte[] destination, int destinationOffset, int length)
        throws InvalidBufferOffsetException {
      if (offset < 0 || offset + length > data.length)
        throw InvalidBufferOffsetException.create(offset, length);
      System.arraycopy(data, (int) offset, destination, destinationOffset, length);
    }

    @ExportMessage
    void writeBufferByte(long offset, byte value)
        throws InvalidBufferOffsetException, UnsupportedMessageException {
      if (!writable) throw UnsupportedMessageException.create();
      if (offset < 0 || offset >= data.length) throw InvalidBufferOffsetException.create(offset, 1);
      data[(int) offset] = value;
    }

    @ExportMessage
    short readBufferShort(ByteOrder order, long offset) throws UnsupportedMessageException {
      throw UnsupportedMessageException.create();
    }

    @ExportMessage
    void writeBufferShort(ByteOrder order, long offset, short value)
        throws UnsupportedMessageException {
      throw UnsupportedMessageException.create();
    }

    @ExportMessage
    int readBufferInt(ByteOrder order, long offset) throws UnsupportedMessageException {
      throw UnsupportedMessageException.create();
    }

    @ExportMessage
    void writeBufferInt(ByteOrder order, long offset, int value)
        throws UnsupportedMessageException {
      throw UnsupportedMessageException.create();
    }

    @ExportMessage
    long readBufferLong(ByteOrder order, long offset) throws UnsupportedMessageException {
      throw UnsupportedMessageException.create();
    }

    @ExportMessage
    void writeBufferLong(ByteOrder order, long offset, long value)
        throws UnsupportedMessageException {
      throw UnsupportedMessageException.create();
    }

    @ExportMessage
    float readBufferFloat(ByteOrder order, long offset) throws UnsupportedMessageException {
      throw UnsupportedMessageException.create();
    }

    @ExportMessage
    void writeBufferFloat(ByteOrder order, long offset, float value)
        throws UnsupportedMessageException {
      throw UnsupportedMessageException.create();
    }

    @ExportMessage
    double readBufferDouble(ByteOrder order, long offset) throws UnsupportedMessageException {
      throw UnsupportedMessageException.create();
    }

    @ExportMessage
    void writeBufferDouble(ByteOrder order, long offset, double value)
        throws UnsupportedMessageException {
      throw UnsupportedMessageException.create();
    }
  }

  /** An interop pointer from another source, like an NFI value. */
  @ExportLibrary(InteropLibrary.class)
  static final class ForeignPointer implements TruffleObject {
    final long address;

    ForeignPointer(long address) {
      this.address = address;
    }

    @ExportMessage
    boolean isPointer() {
      return true;
    }

    @ExportMessage
    long asPointer() {
      return address;
    }
  }

  /**
   * A clean pointer that answers isPointer but refuses asPointer, for the coercion's catch path.
   */
  @ExportLibrary(InteropLibrary.class)
  static final class LyingPointer implements TruffleObject {
    @ExportMessage
    boolean isPointer() {
      return true;
    }

    @ExportMessage
    long asPointer() throws UnsupportedMessageException {
      throw UnsupportedMessageException.create();
    }
  }

  @ExportLibrary(InteropLibrary.class)
  static final class ForeignNull implements TruffleObject {
    @ExportMessage
    boolean isNull() {
      return true;
    }
  }

  @ExportLibrary(InteropLibrary.class)
  static final class ForeignString implements TruffleObject {
    @ExportMessage
    boolean isString() {
      return true;
    }

    @ExportMessage
    String asString() {
      return "héllo";
    }
  }

  @ExportLibrary(InteropLibrary.class)
  static final class ForeignBoolean implements TruffleObject {
    @ExportMessage
    boolean isBoolean() {
      return true;
    }

    @ExportMessage
    boolean asBoolean() {
      return true;
    }
  }

  private static String bytesAt(long address, int length) {
    byte[] bytes = new byte[length];
    MemorySegment.copy(
        MemorySegment.ofAddress(address).reinterpret(length),
        ValueLayout.JAVA_BYTE,
        0,
        bytes,
        0,
        length);
    return new String(bytes, StandardCharsets.UTF_8);
  }

  @Test
  void cellsRunThroughInteropAndHostCalls() throws Exception {
    NativeFunction sum = new SumCell(CoercionPolicy.STRICT).function();
    assertTrue(INTEROP.isExecutable(sum));
    assertTrue(INTEROP.hasExecutableName(sum));
    assertEquals("sum", INTEROP.getExecutableName(sum));
    assertEquals(42, INTEROP.execute(sum, 40, 2));
    assertEquals(42, INTEROP.execute(sum, 40L, 2.0));
    assertEquals(42, sum.call(41, 1));
    assertEquals("sum", sum.name());
    assertEquals(2, sum.arity());
    assertEquals("NativeFunction sum/2", sum.toString());
    assertEquals("sum", sum.callTarget().getRootNode().getName());
    assertTrue(sum.callTarget().getRootNode().isInternal());
    assertEquals("NativeCell sum", sum.callTarget().getRootNode().toString());
    assertEquals("sum", ((NativeCellRootNode) sum.callTarget().getRootNode()).cell().name());
    var arity = assertThrows(NativeCellException.class, () -> INTEROP.execute(sum, 1));
    assertEquals("sum expects 2 arguments, got 1", arity.getMessage());
    var type = assertThrows(NativeCellException.class, () -> sum.call(1, "two"));
    assertEquals(
        "sum: parameter right expects i32 in [-2147483648, 2147483647], got string \"two\"",
        type.getMessage());
    // A second function over a fresh cell, through the same cached interop node shape.
    NativeFunction other = new SumCell(CoercionPolicy.WRAP).function();
    assertEquals(3, INTEROP.execute(other, 1, 2));
    assertEquals(-2147483648, INTEROP.execute(other, 2147483647, 1));
  }

  @Test
  void integerCoercionsHonorWidthSignAndPolicy() {
    var i8 = new IntegerCoercion("f", "p", 8, false, CoercionPolicy.STRICT);
    var u8 = new IntegerCoercion("f", "p", 8, true, CoercionPolicy.STRICT);
    var u16 = new IntegerCoercion("f", "p", 16, true, CoercionPolicy.WRAP);
    var i32 = new IntegerCoercion("f", "p", 32, false, CoercionPolicy.WRAP);
    var u32 = new IntegerCoercion("f", "p", 32, true, CoercionPolicy.STRICT);
    var i64 = new IntegerCoercion("f", "p", 64, false, CoercionPolicy.STRICT);
    var u64 = new IntegerCoercion("f", "p", 64, true, CoercionPolicy.STRICT);
    var u64wrap = new IntegerCoercion("f", "p", 64, true, CoercionPolicy.WRAP);
    // Doubles beyond the long range wrap modulo 2^64 rather than saturating.
    assertEquals(0, i32.execute(0x1p64));
    assertEquals(4096, u16.execute(0x1p63 + 4096.0));
    assertEquals(-8446744073709551616L, u64wrap.execute(1e19));
    assertEquals(-1, i32.execute(-1.0));
    assertEquals(8, i8.bits());
    assertTrue(u8.unsigned());
    assertFalse(i8.unsigned());
    assertEquals(-1, i8.execute(-1));
    assertEquals(127, i8.execute(127L));
    assertEquals(255, u8.execute(255));
    assertEquals((byte) -1, (byte) u8.execute(255));
    assertThrows(NativeCellException.class, () -> i8.execute(128));
    assertThrows(NativeCellException.class, () -> u8.execute(-1));
    assertThrows(NativeCellException.class, () -> u8.execute(256L));
    assertEquals(0, u16.execute(65536));
    assertEquals(65535, u16.execute(-1));
    assertEquals(-2147483648L, i32.execute(2147483648L));
    assertEquals(5, i32.execute(4294967301L));
    assertEquals(4294967295L, u32.execute(4294967295L));
    assertThrows(NativeCellException.class, () -> u32.execute(4294967296L));
    assertEquals(Long.MIN_VALUE, i64.execute(Long.MIN_VALUE));
    assertEquals(Long.MAX_VALUE, u64.execute(Long.MAX_VALUE));
    assertThrows(NativeCellException.class, () -> u64.execute(-1));
    // Doubles: integral values are accepted everywhere; fractions and non-finite depend on policy.
    assertEquals(3, i64.execute(3.0));
    assertThrows(NativeCellException.class, () -> i64.execute(3.5));
    assertEquals(3, i32.execute(3.75));
    assertEquals(-3, i32.execute(-3.75));
    assertEquals(0, i32.execute(Double.NaN));
    assertEquals(0, i32.execute(Double.POSITIVE_INFINITY));
    assertEquals(0x80, u16.execute(65536.0 + 128));
    // Big integers: u64 takes the full unsigned range strictly, anything else needs wrap.
    assertEquals(-1L, u64.execute(big("18446744073709551615")));
    assertThrows(NativeCellException.class, () -> u64.execute(big("18446744073709551616")));
    assertThrows(
        NativeCellException.class,
        () -> u64.execute(new ForeignNumber(BigInteger.ONE.negate().shiftLeft(70))));
    assertEquals(0L, u64wrap.execute(big("18446744073709551616")));
    assertThrows(NativeCellException.class, () -> i64.execute(big("18446744073709551615")));
    assertEquals(3, i64.execute(big("3")));
    assertEquals(3, u64.execute(big("3")));
    // Big integers that fit neither a long nor an exact double: STRICT rejects them on every
    // carrier except u64 (the full unsigned 64-bit range), and WRAP truncates them to the
    // carrier width in two's complement on every carrier.
    var i8wrap = new IntegerCoercion("f", "p", 8, false, CoercionPolicy.WRAP);
    var u8wrap = new IntegerCoercion("f", "p", 8, true, CoercionPolicy.WRAP);
    var i16wrap = new IntegerCoercion("f", "p", 16, false, CoercionPolicy.WRAP);
    var u32wrap = new IntegerCoercion("f", "p", 32, true, CoercionPolicy.WRAP);
    var i64wrap = new IntegerCoercion("f", "p", 64, false, CoercionPolicy.WRAP);
    var p2_63p1 = big("9223372036854775809");
    var p2_64p1 = big("18446744073709551617");
    var p2_1024 = new ForeignNumber(BigInteger.ONE.shiftLeft(1024));
    var n2_63p1 = big("-9223372036854775809");
    // STRICT: u64 accepts 2^63+1 (within [0, 2^64)); every other carrier rejects out-of-range bigs.
    assertEquals(-9223372036854775807L, u64.execute(p2_63p1));
    assertThrows(NativeCellException.class, () -> i64.execute(p2_63p1));
    assertThrows(NativeCellException.class, () -> u64.execute(p2_64p1));
    var bigReject = assertThrows(NativeCellException.class, () -> u32.execute(p2_63p1));
    assertEquals(
        "f: parameter p expects u32 in [0, 4294967295], got ForeignNumber", bigReject.getMessage());
    // WRAP: 2^63+1 truncates to the carrier width; the low 64 bits are 0x8000000000000001, so
    // the 64-bit carriers keep -9223372036854775807 and every narrower carrier narrows to 1.
    assertEquals(-9223372036854775807L, i64wrap.execute(p2_63p1));
    assertEquals(-9223372036854775807L, u64wrap.execute(p2_63p1));
    assertEquals(1, i8wrap.execute(p2_63p1));
    assertEquals(1, u8wrap.execute(p2_63p1));
    assertEquals(1, i16wrap.execute(p2_63p1));
    assertEquals(1, u16.execute(p2_63p1));
    assertEquals(1, i32.execute(p2_63p1));
    assertEquals(1, u32wrap.execute(p2_63p1));
    // WRAP: 2^64+1 and 2^1024 truncate to the low 64 bits (1 and 0 respectively).
    assertEquals(1, i64wrap.execute(p2_64p1));
    assertEquals(0L, i64wrap.execute(p2_1024));
    // WRAP: a negative big integer truncates to its low 64 bits too.
    assertEquals(9223372036854775807L, i64wrap.execute(n2_63p1));
    assertEquals(-1, i8wrap.execute(n2_63p1));
    assertEquals(255, u8wrap.execute(n2_63p1));
    // Doubles beyond the long range that are NOT big integers still truncate through modular():
    // fitsInBigInteger is false, so they skip big() and reach the double path, whose magnitude
    // is >= 2^63 and therefore routes through modular() rather than the (long) cast. Fresh
    // coercion nodes keep the LargeDouble type under the interop dispatch limit.
    var modI32 = new IntegerCoercion("f", "p", 32, false, CoercionPolicy.WRAP);
    var modU16 = new IntegerCoercion("f", "p", 16, true, CoercionPolicy.WRAP);
    assertEquals(0, modI32.execute(new LargeDouble(0x1p64)));
    assertEquals(4096, modU16.execute(new LargeDouble(0x1p63 + 4096.0)));
    var message = assertThrows(NativeCellException.class, () -> u64.execute("x"));
    assertEquals("f: parameter p expects u64 in [0, 2^64), got string \"x\"", message.getMessage());
    assertThrows(NativeCellException.class, () -> i32.execute(new ForeignNull()));
    assertEquals("p", i32.parameterName());
    assertThrows(
        IllegalArgumentException.class, () -> new IntegerCoercion("f", "p", 12, false, null));
  }

  @Test
  void booleanAndFloatingCoercions() {
    var flag = new BooleanCoercion("f", "flag");
    assertTrue(flag.execute(true));
    assertFalse(flag.execute(false));
    assertTrue(flag.execute(new ForeignBoolean()));
    assertThrows(NativeCellException.class, () -> flag.execute(1));
    var number = new FloatingCoercion("f", "x");
    assertEquals(1.5, number.execute(1.5));
    assertEquals(2.0, number.execute(2));
    assertEquals(3.0, number.execute(3L));
    assertEquals(0.5, number.execute(0.5f));
    assertEquals(0.5, number.execute((Object) Float.valueOf(0.5f)));
    assertEquals(9007199254740993.0, number.execute(big("9007199254740993")));
    assertEquals(1.0, number.execute((Object) Short.valueOf((short) 1)));
    assertThrows(NativeCellException.class, () -> number.execute("1"));
    assertThrows(
        NativeCellException.class,
        () -> number.execute(new ForeignNumber(BigInteger.ONE.shiftLeft(100))));
  }

  @Test
  void pointerCoercionsAcceptInteropPointersAndGuardNullsAndNumbers() {
    var strict = new PointerCoercion("f", "p", false, false);
    var nullable = new PointerCoercion("f", "p", true, false);
    var raw = new PointerCoercion("f", "p", true, true);
    assertEquals(0x1000, strict.execute(NativePointer.of(0x1000)));
    assertEquals(0x2000, strict.execute(new ForeignPointer(0x2000)));
    assertThrows(NativeCellException.class, () -> strict.execute(NativePointer.NULL));
    assertThrows(NativeCellException.class, () -> strict.execute(new ForeignNull()));
    assertThrows(NativeCellException.class, () -> strict.execute(new ForeignPointer(0)));
    assertEquals(0, nullable.execute(NativePointer.of(0)));
    assertEquals(0, nullable.execute(new ForeignNull()));
    var numbers = assertThrows(NativeCellException.class, () -> nullable.execute(0x3000L));
    assertEquals("f: parameter p expects pointer or null, got Long 12288", numbers.getMessage());
    assertEquals(
        "f: parameter p expects pointer, got Integer 1",
        assertThrows(NativeCellException.class, () -> strict.execute(1)).getMessage());
    assertEquals(0x3000, raw.execute(0x3000L));
    assertEquals(0x3000, raw.execute(0x3000));
    assertThrows(NativeCellException.class, () -> raw.execute("0x3000"));
    try (Arena arena = Arena.ofConfined()) {
      MemorySegment segment = arena.allocate(16);
      NativeBytes bytes = NativeBytes.of(segment, true);
      assertEquals(segment.address(), strict.execute(bytes));
    }
    var resource = new NativeResource("Ticket", 0x4000, address -> {}, null);
    assertEquals(0x4000, strict.execute(resource));
    resource.close();
    var closed = assertThrows(NativeCellException.class, () -> strict.execute(resource));
    assertEquals("f: parameter p received a closed Ticket", closed.getMessage());
  }

  @Test
  void hostNullUsesCoercionErrorsAndNullablePointerSemantics() {
    var sum = new SumCell(CoercionPolicy.STRICT).function();
    assertEquals(
        "sum: parameter right expects i32 in [-2147483648, 2147483647], got null",
        assertThrows(NativeCellException.class, () -> sum.call(1, null)).getMessage());
    assertEquals(
        "f: parameter p expects bool, got null",
        assertThrows(NativeCellException.class, () -> new BooleanCoercion("f", "p").execute(null))
            .getMessage());
    assertEquals(
        "f: parameter p expects number, got null",
        assertThrows(NativeCellException.class, () -> new FloatingCoercion("f", "p").execute(null))
            .getMessage());
    for (boolean raw : new boolean[] {false, true}) {
      assertEquals(0L, new PointerCell(true, raw).function().call((Object) null));
      assertEquals(
          "f: parameter p expects non-null pointer, got null",
          assertThrows(
                  NativeCellException.class,
                  () -> new PointerCell(false, raw).function().call((Object) null))
              .getMessage());
    }
    for (boolean writable : new boolean[] {false, true}) {
      for (boolean buffer : new boolean[] {false, true}) {
        Borrow borrow = new BytesCoercion("f", "p", true, writable, buffer).execute(null);
        assertEquals(0, borrow.address());
        assertEquals(0, borrow.length());
        borrow.release();
        assertTrue(
            assertThrows(
                    NativeCellException.class,
                    () -> new BytesCoercion("f", "p", false, writable, buffer).execute(null))
                .getMessage()
                .endsWith("got null"));
      }
    }
  }

  @Test
  void pointerCoercionSurvivesConcurrentCloseOfSharedResource() throws Exception {
    var resource = new NativeResource("Ticket", 0x5000L, address -> {}, null);
    var cell = new PointerCell(false, false);
    // Force a close after the real isPointer probe and before the real asPointer dispatch.
    // Inject only the scheduling library, keeping production free of test hooks.
    var interop =
        new InteropLibrary() {
          @Override
          public boolean accepts(Object receiver) {
            return receiver == resource;
          }

          @Override
          public boolean isPointer(Object receiver) {
            boolean pointer = INTEROP.isPointer(receiver);
            Thread closer = new Thread(resource::close, "close-between-pointer-messages");
            closer.start();
            try {
              closer.join(5000);
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
              throw new AssertionError(e);
            }
            assertFalse(closer.isAlive());
            assertTrue(pointer);
            assertTrue(resource.isClosed());
            return pointer;
          }

          @Override
          public long asPointer(Object receiver) throws UnsupportedMessageException {
            return INTEROP.asPointer(receiver);
          }
        };
    cell.useInterop(interop);
    var function = cell.function();
    assertEquals(
        "f: parameter p received a closed Ticket",
        assertThrows(NativeCellException.class, () -> function.call(resource)).getMessage());
  }

  @Test
  void pointerCoercionFailsSafelyOnMisbehavingInterop() throws Exception {
    var value = new LyingPointer();
    var cell = new PointerCell(true, true);
    // Dispatch the messages directly: Truffle's assertion wrapper otherwise rejects the liar
    // before the coercion sees UnsupportedMessageException, hiding the assertions-off behavior.
    cell.useInterop(
        new InteropLibrary() {
          @Override
          public boolean accepts(Object receiver) {
            return receiver == value;
          }

          @Override
          public boolean isPointer(Object receiver) {
            return value.isPointer();
          }

          @Override
          public long asPointer(Object receiver) throws UnsupportedMessageException {
            return value.asPointer();
          }
        });
    var function = cell.function();
    var error = assertThrows(AssertionError.class, () -> function.call(value));
    assertInstanceOf(UnsupportedMessageException.class, error.getCause());
  }

  @Test
  void nativePointersAndVoidAreInteropValues() throws Exception {
    NativePointer p = NativePointer.of(0x10);
    assertTrue(INTEROP.isPointer(p));
    assertEquals(0x10, INTEROP.asPointer(p));
    assertFalse(INTEROP.isNull(p));
    assertTrue(INTEROP.isNull(NativePointer.of(0)));
    assertSame(NativePointer.NULL, NativePointer.of(0));
    assertEquals(0x10, p.address());
    assertEquals(NativePointer.of(0x10), p);
    assertEquals(NativePointer.of(0x10).hashCode(), p.hashCode());
    assertNotEquals(p, NativePointer.of(0x11));
    assertNotEquals(p, "0x10");
    assertEquals("NativePointer(0x10)", p.toString());
    assertTrue(INTEROP.isNull(NativeVoid.INSTANCE));
    assertEquals("NativeVoid", NativeVoid.INSTANCE.toString());
    assertSame(NativeVoid.INSTANCE, Carriers.none());
    assertEquals(NativePointer.of(7), Carriers.pointer(7));
    assertEquals(255, Carriers.u8((byte) -1));
    assertEquals(65535, Carriers.u16((short) -1));
    assertEquals(4294967295L, Carriers.u32(-1));
    assertEquals(7, Carriers.length32(7, false, null));
    assertEquals(-1, Carriers.length32(4294967295L, true, null));
    assertThrows(NativeCellException.class, () -> Carriers.length32(2147483648L, false, null));
    assertThrows(NativeCellException.class, () -> Carriers.length32(4294967296L, true, null));
    assertEquals("null", NativeCellException.describe(null));
    assertEquals("Character a", NativeCellException.describe('a'));
    assertEquals("Boolean true", NativeCellException.describe(true));
    assertEquals("NativeVoid", NativeCellException.describe(NativeVoid.INSTANCE));
  }

  @Test
  void nativeBytesReadAndWriteThroughInteropWithBounds() throws Exception {
    try (Arena arena = Arena.ofConfined()) {
      MemorySegment segment = arena.allocate(16);
      NativeBytes bytes = NativeBytes.of(segment.address(), 16, true);
      assertEquals(segment.address(), bytes.address());
      assertEquals(16, bytes.size());
      assertTrue(bytes.isWritable());
      assertEquals(16, bytes.segment().byteSize());
      assertTrue(INTEROP.hasBufferElements(bytes));
      assertTrue(INTEROP.isBufferWritable(bytes));
      assertEquals(16, INTEROP.getBufferSize(bytes));
      assertTrue(INTEROP.isPointer(bytes));
      assertEquals(segment.address(), INTEROP.asPointer(bytes));
      INTEROP.writeBufferByte(bytes, 0, (byte) 1);
      INTEROP.writeBufferShort(bytes, ByteOrder.LITTLE_ENDIAN, 2, (short) 2);
      INTEROP.writeBufferInt(bytes, ByteOrder.BIG_ENDIAN, 4, 3);
      INTEROP.writeBufferLong(bytes, ByteOrder.LITTLE_ENDIAN, 8, 4);
      assertEquals(1, INTEROP.readBufferByte(bytes, 0));
      assertEquals(2, INTEROP.readBufferShort(bytes, ByteOrder.LITTLE_ENDIAN, 2));
      assertEquals(512, INTEROP.readBufferShort(bytes, ByteOrder.BIG_ENDIAN, 2));
      assertEquals(3, INTEROP.readBufferInt(bytes, ByteOrder.BIG_ENDIAN, 4));
      assertEquals(0x03000000, INTEROP.readBufferInt(bytes, ByteOrder.LITTLE_ENDIAN, 4));
      assertEquals(4, INTEROP.readBufferLong(bytes, ByteOrder.LITTLE_ENDIAN, 8));
      assertEquals(0x0400000000000000L, INTEROP.readBufferLong(bytes, ByteOrder.BIG_ENDIAN, 8));
      INTEROP.writeBufferFloat(bytes, ByteOrder.LITTLE_ENDIAN, 0, 1.5f);
      assertEquals(1.5f, INTEROP.readBufferFloat(bytes, ByteOrder.LITTLE_ENDIAN, 0));
      INTEROP.writeBufferFloat(bytes, ByteOrder.BIG_ENDIAN, 0, 2.5f);
      assertEquals(2.5f, INTEROP.readBufferFloat(bytes, ByteOrder.BIG_ENDIAN, 0));
      INTEROP.writeBufferDouble(bytes, ByteOrder.LITTLE_ENDIAN, 8, 3.5);
      assertEquals(3.5, INTEROP.readBufferDouble(bytes, ByteOrder.LITTLE_ENDIAN, 8));
      INTEROP.writeBufferDouble(bytes, ByteOrder.BIG_ENDIAN, 8, 4.5);
      assertEquals(4.5, INTEROP.readBufferDouble(bytes, ByteOrder.BIG_ENDIAN, 8));
      byte[] copy = new byte[4];
      INTEROP.readBuffer(bytes, 0, copy, 0, 4);
      assertEquals(copy[3], INTEROP.readBufferByte(bytes, 3));
      assertThrows(InvalidBufferOffsetException.class, () -> INTEROP.readBufferByte(bytes, 16));
      assertThrows(InvalidBufferOffsetException.class, () -> INTEROP.readBufferByte(bytes, -1));
      assertThrows(
          InvalidBufferOffsetException.class,
          () -> INTEROP.readBufferLong(bytes, ByteOrder.LITTLE_ENDIAN, 9));
      assertThrows(
          InvalidBufferOffsetException.class, () -> INTEROP.readBuffer(bytes, 14, copy, 0, 4));
      assertThrows(
          InvalidBufferOffsetException.class, () -> INTEROP.readBuffer(bytes, 0, copy, 0, -1));
      NativeBytes readOnly = NativeBytes.of(segment.asReadOnly(), true);
      assertFalse(readOnly.isWritable());
      assertFalse(INTEROP.isBufferWritable(readOnly));
      assertThrows(
          UnsupportedMessageException.class, () -> INTEROP.writeBufferByte(readOnly, 0, (byte) 1));
      assertThrows(
          UnsupportedMessageException.class,
          () -> INTEROP.writeBufferInt(readOnly, ByteOrder.LITTLE_ENDIAN, 0, 1));
      assertEquals(
          "NativeBytes(0x" + Long.toHexString(segment.address()) + ", 16)", bytes.toString());
    }
    assertThrows(IllegalArgumentException.class, () -> NativeBytes.of(0, 4, true));
    assertThrows(IllegalArgumentException.class, () -> NativeBytes.of(16, -1, true));
    assertThrows(
        IllegalArgumentException.class,
        () -> NativeBytes.of(MemorySegment.ofArray(new byte[4]), true));
    assertEquals(0, NativeBytes.of(0, 0, false).size());
  }

  @Test
  void heapBytesReadAndWriteThroughInteropWithBounds() throws Exception {
    byte[] array = new byte[16];
    HeapBytes bytes = new HeapBytes(array, true);
    assertSame(array, bytes.array());
    assertTrue(bytes.isWritable());
    assertTrue(INTEROP.hasBufferElements(bytes));
    assertTrue(INTEROP.isBufferWritable(bytes));
    assertEquals(16, INTEROP.getBufferSize(bytes));
    assertFalse(INTEROP.isPointer(bytes));
    INTEROP.writeBufferByte(bytes, 0, (byte) 1);
    INTEROP.writeBufferShort(bytes, ByteOrder.LITTLE_ENDIAN, 2, (short) 2);
    INTEROP.writeBufferInt(bytes, ByteOrder.BIG_ENDIAN, 4, 3);
    INTEROP.writeBufferLong(bytes, ByteOrder.LITTLE_ENDIAN, 8, 4);
    assertEquals(1, array[0]);
    assertEquals(2, array[2]);
    assertEquals(3, array[7]);
    assertEquals(4, array[8]);
    assertEquals(1, INTEROP.readBufferByte(bytes, 0));
    assertEquals(2, INTEROP.readBufferShort(bytes, ByteOrder.LITTLE_ENDIAN, 2));
    assertEquals(3, INTEROP.readBufferInt(bytes, ByteOrder.BIG_ENDIAN, 4));
    assertEquals(4, INTEROP.readBufferLong(bytes, ByteOrder.LITTLE_ENDIAN, 8));
    INTEROP.writeBufferFloat(bytes, ByteOrder.BIG_ENDIAN, 0, 1.5f);
    assertEquals(1.5f, INTEROP.readBufferFloat(bytes, ByteOrder.BIG_ENDIAN, 0));
    INTEROP.writeBufferDouble(bytes, ByteOrder.LITTLE_ENDIAN, 8, 2.5);
    assertEquals(2.5, INTEROP.readBufferDouble(bytes, ByteOrder.LITTLE_ENDIAN, 8));
    byte[] copy = new byte[8];
    INTEROP.readBuffer(bytes, 8, copy, 0, 8);
    assertEquals(
        2.5,
        ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN
            ? Double.longBitsToDouble(
                java.nio.ByteBuffer.wrap(copy).order(ByteOrder.LITTLE_ENDIAN).getLong())
            : 2.5);
    assertThrows(InvalidBufferOffsetException.class, () -> INTEROP.readBufferByte(bytes, 16));
    assertThrows(
        InvalidBufferOffsetException.class, () -> INTEROP.readBuffer(bytes, 12, copy, 0, 8));
    assertThrows(
        InvalidBufferOffsetException.class, () -> INTEROP.readBuffer(bytes, 0, copy, 0, -1));
    HeapBytes readOnly = new HeapBytes(array, false);
    assertThrows(
        UnsupportedMessageException.class, () -> INTEROP.writeBufferByte(readOnly, 0, (byte) 1));
    assertThrows(
        UnsupportedMessageException.class,
        () -> INTEROP.writeBufferDouble(readOnly, ByteOrder.LITTLE_ENDIAN, 0, 1));
    assertEquals("HeapBytes(16)", bytes.toString());
  }

  @Test
  void bytesCoercionLendsNativeMemoryCopiesHeapBytesAndEncodesStrings() throws Exception {
    var cString = new BytesCoercion("f", "text", false, false, false);
    var readBuffer = new BytesCoercion("f", "bytes", false, false, true);
    var writeBuffer = new BytesCoercion("f", "bytes", false, true, true);
    var nullable = new BytesCoercion("f", "text", true, false, false);
    // Strings: UTF-8, NUL-terminated for C strings, bare bytes for borrows.
    Borrow text = cString.execute("héllo");
    assertEquals(6, text.length());
    assertEquals("héllo\0", bytesAt(text.address(), 7));
    text.release();
    text.release();
    Borrow foreign = readBuffer.execute(new ForeignString());
    assertEquals(6, foreign.length());
    assertEquals("héllo", bytesAt(foreign.address(), 6));
    foreign.release();
    assertThrows(NativeCellException.class, () -> writeBuffer.execute("immutable"));
    // Native memory is lent in place.
    try (Arena arena = Arena.ofConfined()) {
      MemorySegment segment = arena.allocate(8);
      NativeBytes bytes = NativeBytes.of(segment, true);
      Borrow direct = writeBuffer.execute(bytes);
      assertEquals(segment.address(), direct.address());
      assertEquals(8, direct.length());
      direct.release();
      NativeBytes readOnly = NativeBytes.of(segment.asReadOnly(), true);
      assertThrows(NativeCellException.class, () -> writeBuffer.execute(readOnly));
      assertEquals(segment.address(), readBuffer.execute(readOnly).address());
      Borrow empty = readBuffer.execute(NativeBytes.of(0, 0, false));
      assertNotEquals(0, empty.address());
      assertEquals(0, empty.length());
      // A C string parameter takes a bare pointer; a borrow needs a size for its derived count.
      Borrow pointer = cString.execute(new ForeignPointer(segment.address()));
      assertEquals(segment.address(), pointer.address());
      assertEquals(0, pointer.length());
      assertEquals(
          segment.address(), cString.execute(NativePointer.of(segment.address())).address());
      assertEquals(
          "f: parameter bytes expects string, sized buffer, got NativePointer",
          assertThrows(
                  NativeCellException.class,
                  () -> readBuffer.execute(NativePointer.of(segment.address())))
              .getMessage());
      assertThrows(
          NativeCellException.class,
          () -> writeBuffer.execute(new ForeignPointer(segment.address())));
    }
    // Heap bytes are copied on HotSpot and written back when writable.
    byte[] array = {1, 2, 3, 4};
    Borrow copy = writeBuffer.execute(new HeapBytes(array, true));
    assertEquals(4, copy.length());
    MemorySegment view = MemorySegment.ofAddress(copy.address()).reinterpret(4);
    assertEquals(3, view.get(ValueLayout.JAVA_BYTE, 2));
    view.set(ValueLayout.JAVA_BYTE, 0, (byte) 9);
    copy.release();
    assertEquals(9, array[0]);
    assertThrows(NativeCellException.class, () -> writeBuffer.execute(new HeapBytes(array, false)));
    Borrow readCopy = readBuffer.execute(new HeapBytes(array, false));
    MemorySegment.ofAddress(readCopy.address())
        .reinterpret(4)
        .set(ValueLayout.JAVA_BYTE, 0, (byte) 7);
    readCopy.release();
    assertEquals(9, array[0]);
    Borrow emptyCopy = readBuffer.execute(new HeapBytes(new byte[0], false));
    assertNotEquals(0, emptyCopy.address());
    emptyCopy.release();
    // Any other interop buffer goes through the library, element by element on the way back.
    byte[] data = {5, 6, 7};
    Borrow generic = writeBuffer.execute(new ForeignBuffer(data, true));
    assertEquals(3, generic.length());
    MemorySegment.ofAddress(generic.address())
        .reinterpret(3)
        .set(ValueLayout.JAVA_BYTE, 1, (byte) 8);
    generic.release();
    assertArrayEquals(new byte[] {5, 8, 7}, data);
    var readOnlyForeign =
        assertThrows(
            NativeCellException.class, () -> writeBuffer.execute(new ForeignBuffer(data, false)));
    assertEquals(
        "f: parameter bytes: read-only buffer for a writable borrow", readOnlyForeign.getMessage());
    Borrow readGeneric = readBuffer.execute(new ForeignBuffer(data, false));
    assertEquals(3, readGeneric.length());
    readGeneric.release();
    // Nulls and mismatches.
    assertEquals(0, nullable.execute(new ForeignNull()).address());
    assertEquals(0, nullable.execute(NativePointer.NULL).address());
    assertSame(Borrow.none(), nullable.execute(new ForeignNull()));
    assertEquals(
        "f: parameter text expects string, buffer or pointer, got Integer 1",
        assertThrows(NativeCellException.class, () -> cString.execute(1)).getMessage());
    assertEquals(
        "f: parameter text expects string, buffer or pointer, got NativePointer",
        assertThrows(NativeCellException.class, () -> cString.execute(NativePointer.NULL))
            .getMessage());
    assertEquals(
        "f: parameter bytes expects sized buffer, got Boolean true",
        assertThrows(NativeCellException.class, () -> writeBuffer.execute(true)).getMessage());
    assertEquals(
        "f: parameter text expects string, buffer or pointer or null, got Long 3",
        assertThrows(NativeCellException.class, () -> nullable.execute(3L)).getMessage());
    assertTrue(Borrow.pinThreshold() > 0);
  }

  @Test
  void foreignBufferBorrowedAsCStringIsNulTerminated() {
    var cString = new BytesCoercion("f", "text", false, false, false);
    // Copies reuse the thread's scratch block without zeroing it, so a missing terminator would
    // read stale non-zero data (the OOB read the bug report describes), not allocator luck.
    byte[] junk = new byte[64];
    java.util.Arrays.fill(junk, (byte) 'Z');
    Borrow.ofArray(junk, false, false).release();
    byte[] data = new byte[24];
    java.util.Arrays.fill(data, (byte) 'A');
    Borrow borrow = cString.execute(new ForeignBuffer(data, false));
    assertEquals(24, borrow.length());
    MemorySegment view = MemorySegment.ofAddress(borrow.address()).reinterpret(25);
    for (int i = 0; i < 24; i++) assertEquals(data[i], view.get(ValueLayout.JAVA_BYTE, i));
    assertEquals(0, view.get(ValueLayout.JAVA_BYTE, 24));
    borrow.release();
    assertArrayEquals(data, data);
    var writableCString = new BytesCoercion("f", "text", false, true, false);
    byte[] out = new byte[24];
    java.util.Arrays.fill(out, (byte) 'A');
    Borrow writableBorrow = writableCString.execute(new ForeignBuffer(out, true));
    assertEquals(24, writableBorrow.length());
    MemorySegment writableSeg = MemorySegment.ofAddress(writableBorrow.address()).reinterpret(25);
    writableSeg.set(ValueLayout.JAVA_BYTE, 1, (byte) 'b');
    writableSeg.set(ValueLayout.JAVA_BYTE, 24, (byte) 0x7f);
    writableBorrow.release();
    byte[] expected = new byte[24];
    java.util.Arrays.fill(expected, (byte) 'A');
    expected[1] = 'b';
    assertArrayEquals(expected, out);
  }

  @Test
  void borrowsOfLargeArraysCopyOnHotSpot() {
    byte[] large = new byte[Borrow.pinThreshold() + 1];
    large[Borrow.pinThreshold()] = 42;
    Borrow borrow = Borrow.ofArray(large, true, false);
    MemorySegment view = MemorySegment.ofAddress(borrow.address()).reinterpret(large.length);
    assertEquals(42, view.get(ValueLayout.JAVA_BYTE, Borrow.pinThreshold()));
    view.set(ValueLayout.JAVA_BYTE, 0, (byte) 1);
    borrow.release();
    assertEquals(1, large[0]);
    Borrow direct = Borrow.direct(0x10, 3);
    assertEquals(0x10, direct.address());
    assertEquals(3, direct.length());
    direct.release();
  }

  interface ThreadBody {
    void run() throws Exception;
  }

  /**
   * Runs on a thread of its own, so the thread's scratch stack starts empty and at its first size.
   */
  private static void onFreshThread(ThreadBody body) throws Exception {
    var failure = new AtomicReference<Throwable>();
    Thread thread =
        new Thread(() -> {
          try {
            body.run();
          } catch (Throwable e) {
            failure.set(e);
          }
        });
    thread.start();
    thread.join();
    if (failure.get() instanceof Exception e) throw e;
    if (failure.get() instanceof Error e) throw e;
  }

  /**
   * Two C strings, coerced and released exactly as a generated cell does: each borrow opens its own
   * try, so a failure releases the earlier ones, and release runs in reverse order. The call
   * concatenates both strings as native code would read them, through their NUL terminators, and
   * may first run a nested call, as native code calling back into guest code would.
   */
  static final class PairCell extends NativeCell {
    @Node.Child private BytesCoercion coerce_first;
    @Node.Child private BytesCoercion coerce_second;
    final List<Long> addresses = java.util.Collections.synchronizedList(new ArrayList<>());
    Runnable callback = () -> {};

    PairCell() {
      super("pair", 2);
      coerce_first = new BytesCoercion("pair", "first", false, false, false);
      coerce_second = new BytesCoercion("pair", "second", false, false, false);
    }

    @Override
    public Object execute(Object[] arguments) {
      checkArity(arguments);
      Borrow borrow_first = coerce_first.execute(arguments[0]);
      try {
        Borrow borrow_second = coerce_second.execute(arguments[1]);
        try {
          addresses.add(borrow_first.address());
          addresses.add(borrow_second.address());
          callback.run();
          return cString(borrow_first.address()) + cString(borrow_second.address());
        } finally {
          borrow_second.release();
        }
      } finally {
        borrow_first.release();
      }
    }
  }

  /** Reads a C string up to its terminator, as native code would. */
  private static String cString(long address) {
    return MemorySegment.ofAddress(address).reinterpret(Long.MAX_VALUE).getString(0);
  }

  @Test
  void scratchCopiesNestAndReleaseLastInFirstOut() throws Exception {
    onFreshThread(() -> {
      Borrow first = Borrow.ofString("first", true);
      Borrow second = Borrow.ofString("second", true);
      // Bump allocation: the second copy sits on the next 16-byte boundary.
      assertEquals(first.address() + 16, second.address());
      assertEquals("first", cString(first.address()));
      assertEquals("second", cString(second.address()));
      // Out of order fails loudly and changes nothing; in order afterwards still works.
      var misuse = assertThrows(IllegalStateException.class, first::release);
      assertEquals(
          "borrow released out of order: the copy at offset 0 is not the innermost (top 32)",
          misuse.getMessage());
      assertEquals("second", cString(second.address()));
      Borrow third = Borrow.ofString("third", true);
      assertEquals(second.address() + 16, third.address());
      third.release();
      second.release();
      first.release();
      first.release();
      // An empty stack reuses the block from its start; an empty copy still gets an address.
      Borrow empty = Borrow.ofArray(new byte[0], false, false);
      assertEquals(first.address(), empty.address());
      Borrow next = Borrow.ofString("x", true);
      assertEquals(first.address() + 16, next.address());
      next.release();
      empty.release();
      // A borrow cannot be released by another thread, which would race the owner's stack.
      Borrow owned = Borrow.ofString("owned", true);
      var other = new AtomicReference<Throwable>();
      Thread thief =
          new Thread(() -> other.set(assertThrows(RuntimeException.class, owned::release)));
      thief.start();
      thief.join();
      assertTrue(other.get().getMessage().startsWith("borrow released on thread "));
      owned.release();
      assertEquals(first.address(), Borrow.ofString("y", true).address());
    });
  }

  @Test
  void scratchCopiesWriteBackAndTerminateCStrings() throws Exception {
    onFreshThread(() -> {
      // Dirty the block so a missing terminator would read stale bytes.
      byte[] junk = new byte[64];
      java.util.Arrays.fill(junk, (byte) 'Z');
      Borrow dirty = Borrow.ofArray(junk, false, false);
      long base = dirty.address();
      dirty.release();
      Borrow text = Borrow.ofString("abc", true);
      assertEquals(base, text.address());
      assertEquals(3, text.length());
      assertEquals("abc", cString(text.address()));
      text.release();
      var cString = new BytesCoercion("f", "text", false, false, false);
      Borrow foreign = cString.execute(new ForeignBuffer(new byte[] {'h', 'i'}, false));
      assertEquals(base, foreign.address());
      assertEquals("hi", cString(foreign.address()));
      foreign.release();
      // Writable heap bytes and generic buffers are written back from the scratch copy, and only
      // their own length: the terminator never reaches the source.
      byte[] array = {1, 2, 3};
      Borrow outer = Borrow.ofString("outer", true);
      Borrow heap = Borrow.ofArray(array, true, true);
      assertEquals(outer.address() + 16, heap.address());
      MemorySegment view = MemorySegment.ofAddress(heap.address()).reinterpret(4);
      assertEquals(0, view.get(ValueLayout.JAVA_BYTE, 3));
      view.set(ValueLayout.JAVA_BYTE, 0, (byte) 9);
      view.set(ValueLayout.JAVA_BYTE, 3, (byte) 8);
      heap.release();
      assertArrayEquals(new byte[] {9, 2, 3}, array);
      byte[] data = {5, 6, 7};
      var writable = new BytesCoercion("f", "bytes", false, true, true);
      Borrow generic = writable.execute(new ForeignBuffer(data, true));
      assertEquals(outer.address() + 16, generic.address());
      MemorySegment.ofAddress(generic.address())
          .reinterpret(3)
          .set(ValueLayout.JAVA_BYTE, 2, (byte) 4);
      generic.release();
      assertArrayEquals(new byte[] {5, 6, 4}, data);
      assertEquals("outer", cString(outer.address()));
      outer.release();
      // A write-back that fails still pops the copy.
      var refusing = new BytesCoercion("f", "bytes", false, true, true);
      Borrow refused = refusing.execute(new LateRefusingBuffer());
      assertThrows(AssertionError.class, refused::release);
      assertEquals(base, Borrow.ofString("after", true).address());
    });
  }

  /** Writable when borrowed, then refuses the write-back: release must still pop its copy. */
  @ExportLibrary(InteropLibrary.class)
  static final class LateRefusingBuffer implements TruffleObject {
    @ExportMessage
    boolean hasBufferElements() {
      return true;
    }

    @ExportMessage
    boolean isBufferWritable() {
      return true;
    }

    @ExportMessage
    long getBufferSize() {
      return 2;
    }

    @ExportMessage
    byte readBufferByte(long offset) {
      return 0;
    }

    @ExportMessage
    void readBuffer(long offset, byte[] destination, int destinationOffset, int length) {}

    @ExportMessage
    void writeBufferByte(long offset, byte value) throws UnsupportedMessageException {
      throw UnsupportedMessageException.create();
    }

    @ExportMessage
    short readBufferShort(ByteOrder order, long offset) {
      return 0;
    }

    @ExportMessage
    void writeBufferShort(ByteOrder order, long offset, short value) {}

    @ExportMessage
    int readBufferInt(ByteOrder order, long offset) {
      return 0;
    }

    @ExportMessage
    void writeBufferInt(ByteOrder order, long offset, int value) {}

    @ExportMessage
    long readBufferLong(ByteOrder order, long offset) {
      return 0;
    }

    @ExportMessage
    void writeBufferLong(ByteOrder order, long offset, long value) {}

    @ExportMessage
    float readBufferFloat(ByteOrder order, long offset) {
      return 0;
    }

    @ExportMessage
    void writeBufferFloat(ByteOrder order, long offset, float value) {}

    @ExportMessage
    double readBufferDouble(ByteOrder order, long offset) {
      return 0;
    }

    @ExportMessage
    void writeBufferDouble(ByteOrder order, long offset, double value) {}
  }

  @Test
  void oversizeCopiesTakeTheArenaPathAndTheBlockGrows() throws Exception {
    onFreshThread(() -> {
      int largest = (int) Borrow.Scratch.LARGEST;
      Borrow anchor = Borrow.ofString("a", true);
      long base = anchor.address();
      anchor.release();
      // Larger than any scratch copy: an arena of its own, written back all the same.
      byte[] huge = new byte[largest + 1];
      huge[largest] = 3;
      Borrow outside = Borrow.ofArray(huge, true, false);
      assertNotEquals(base, outside.address());
      assertEquals(
          3,
          MemorySegment.ofAddress(outside.address())
              .reinterpret(largest + 1)
              .get(ValueLayout.JAVA_BYTE, largest));
      MemorySegment.ofAddress(outside.address())
          .reinterpret(1)
          .set(ValueLayout.JAVA_BYTE, 0, (byte) 6);
      Borrow above = Borrow.ofString("above", true);
      assertEquals(base, above.address());
      above.release();
      outside.release();
      assertEquals(6, huge[0]);
      // The largest scratch copy fits once the empty stack grows the block (terminator included).
      Borrow big = Borrow.ofArray(new byte[largest - 17], false, true);
      long grown = big.address();
      Borrow small = Borrow.ofString("s", true);
      assertEquals(grown + largest - 16, small.address());
      // A copy that does not fit beside outstanding ones takes an arena, and raises the mark.
      byte[] second = new byte[largest - 1];
      second[0] = 1;
      Borrow overflow = Borrow.ofArray(second, false, true);
      long overflowAddress = overflow.address();
      assertEquals(
          1, MemorySegment.ofAddress(overflowAddress).reinterpret(1).get(ValueLayout.JAVA_BYTE, 0));
      // Off the stack: small is still the innermost copy, and the arena copy releases in any order.
      small.release();
      overflow.release();
      big.release();
      // The next empty-stack acquisition grows to the high-water mark: both now fit together.
      Borrow again = Borrow.ofArray(new byte[largest - 17], false, true);
      Borrow beside = Borrow.ofArray(new byte[largest - 1], false, true);
      assertEquals(again.address() + largest - 16, beside.address());
      beside.release();
      again.release();
    });
  }

  @Test
  void cellBorrowsReleaseInReverseOrderAcrossFailuresAndReentry() throws Exception {
    onFreshThread(() -> {
      var cell = new PairCell();
      NativeFunction pair = cell.function();
      assertEquals("héllo, world", pair.call("héllo", ", world"));
      long base = cell.addresses.get(0);
      assertEquals(base + 16, (long) cell.addresses.get(1));
      // The second coercion fails after the first borrow: the first is released.
      assertThrows(NativeCellException.class, () -> pair.call("kept", 1));
      assertEquals("ab", pair.call("a", "b"));
      assertEquals(base, (long) cell.addresses.get(2));
      // A nested call (native code calling back into guest code) borrows above the outer call
      // and releases before it returns, leaving the outer copies intact.
      var inner = new PairCell();
      NativeFunction innerPair = inner.function();
      var nested = new AtomicReference<Object>();
      cell.callback = () -> nested.set(innerPair.call("in", "ner"));
      assertEquals("outer", pair.call("out", "er"));
      assertEquals("inner", nested.get());
      assertEquals(base + 32, (long) inner.addresses.get(0));
      assertEquals(base + 48, (long) inner.addresses.get(1));
      cell.callback = () -> {};
      assertEquals("xy", pair.call("x", "y"));
      assertEquals(base, (long) cell.addresses.get(cell.addresses.size() - 2));
    });
  }

  @Test
  void scratchStacksArePerThreadUnderConcurrentUse() throws Exception {
    // One cell (as for a threading=shared resource) runs on many threads at once; each thread
    // copies into its own stack, so no thread sees another's bytes.
    int threads = 8;
    var cell = new PairCell();
    NativeFunction pair = cell.function();
    var failures = new java.util.concurrent.ConcurrentLinkedQueue<Throwable>();
    var start = new java.util.concurrent.CountDownLatch(1);
    var workers = new ArrayList<Thread>();
    for (int t = 0; t < threads; t++) {
      int id = t;
      Thread worker =
          new Thread(() -> {
            try {
              start.await();
              for (int i = 0; i < 2000; i++) {
                String left = "t" + id + ":" + i;
                String right = "-".repeat(i % 40);
                Object joined = pair.call(left, right);
                if (!(left + right).equals(joined))
                  throw new AssertionError(joined + " != " + left + right);
              }
            } catch (Throwable e) {
              failures.add(e);
            }
          });
      workers.add(worker);
      worker.start();
    }
    start.countDown();
    for (Thread worker : workers) worker.join();
    assertTrue(failures.isEmpty(), () -> failures.toString());
    assertEquals(threads * 2000 * 2, cell.addresses.size());
  }

  @Test
  void resourcesCloseOnceAndRespectConfinement() throws Exception {
    AtomicLong destroyed = new AtomicLong();
    var shared = new NativeResource("Ticket", 0x20, destroyed::set, null);
    assertEquals("Ticket", shared.type());
    assertNull(shared.requiredThread());
    assertEquals(0x20, shared.address());
    assertTrue(INTEROP.isPointer(shared));
    assertEquals(0x20, INTEROP.asPointer(shared));
    assertTrue(INTEROP.hasMembers(shared));
    assertTrue(INTEROP.isMemberInvocable(shared, "close"));
    assertFalse(INTEROP.isMemberInvocable(shared, "address"));
    Object members = INTEROP.getMembers(shared);
    assertEquals(1, INTEROP.getArraySize(members));
    assertEquals("close", INTEROP.readArrayElement(members, 0));
    assertTrue(INTEROP.isArrayElementReadable(members, 0));
    assertFalse(INTEROP.isArrayElementReadable(members, 1));
    assertThrows(
        com.oracle.truffle.api.interop.InvalidArrayIndexException.class,
        () -> INTEROP.readArrayElement(members, 1));
    assertEquals("Ticket(0x20)", shared.toString());
    assertThrows(
        com.oracle.truffle.api.interop.UnknownIdentifierException.class,
        () -> INTEROP.invokeMember(shared, "open"));
    assertThrows(NativeCellException.class, () -> INTEROP.invokeMember(shared, "close", 1));
    assertSame(NativeVoid.INSTANCE, INTEROP.invokeMember(shared, "close"));
    assertEquals(0x20, destroyed.get());
    assertTrue(shared.isClosed());
    assertFalse(INTEROP.isPointer(shared));
    assertThrows(UnsupportedMessageException.class, () -> INTEROP.asPointer(shared));
    assertEquals(
        "Ticket is closed", assertThrows(NativeCellException.class, shared::address).getMessage());
    destroyed.set(0);
    shared.close();
    assertEquals(0, destroyed.get(), "second close is a no-op");
    assertEquals("Ticket(closed)", shared.toString());
    assertThrows(
        IllegalArgumentException.class,
        () -> new NativeResource("Ticket", 0, destroyed::set, null));
    assertThrows(IllegalArgumentException.class, () -> new NativeResource("Ticket", 1, null, null));

    var confined = new NativeResource("Ticket", 0x30, destroyed::set, Thread.currentThread());
    assertSame(Thread.currentThread(), confined.requiredThread());
    assertEquals(0x30, confined.address());
    var failure = new AtomicReference<Throwable>();
    var seen = new AtomicReference<Boolean>();
    Thread other =
        new Thread(
            () -> {
              seen.set(INTEROP.isPointer(confined));
              try {
                confined.address();
              } catch (NativeCellException expected) {
                failure.set(expected);
              }
              try {
                confined.close();
              } catch (NativeCellException expected) {
                failure.set(expected);
              }
            },
            "foreign");
    other.start();
    other.join();
    assertEquals(Boolean.FALSE, seen.get());
    assertTrue(failure.get().getMessage().startsWith("Ticket is confined to "));
    assertTrue(failure.get().getMessage().endsWith("; accessed from foreign"));
    assertFalse(confined.isClosed());
    assertThrows(IllegalStateException.class, () -> confined.registerCleanup(Cleaner.create()));
    confined.close();
    assertEquals(0x30, destroyed.get());

    var cleaned = new NativeResource("Ticket", 0x40, destroyed::set, null);
    Cleaner.Cleanable cleanable = cleaned.registerCleanup(Cleaner.create());
    cleanable.clean();
    assertEquals(0x40, destroyed.get());
    assertTrue(cleaned.isClosed());
  }

  @Test
  void coercionsInsertOneInteropLibraryAndReuseIt() {
    var coercion = new IntegerCoercion("f", "p", 64, false, CoercionPolicy.STRICT);
    List<Node> before = new ArrayList<>();
    coercion.getChildren().forEach(before::add);
    assertTrue(before.isEmpty());
    assertEquals(3, coercion.execute(big("3")));
    assertEquals(3, coercion.execute(big("3")));
    List<Node> after = new ArrayList<>();
    coercion.getChildren().forEach(after::add);
    assertEquals(1, after.size());
  }

  /** An object whose messages contradict each other: every coercion must still fail safely. */
  @ExportLibrary(InteropLibrary.class)
  static final class Liar implements TruffleObject {
    @ExportMessage
    boolean isNumber() {
      return true;
    }

    @ExportMessage
    boolean fitsInByte() {
      return false;
    }

    @ExportMessage
    boolean fitsInShort() {
      return false;
    }

    @ExportMessage
    boolean fitsInInt() {
      return false;
    }

    @ExportMessage
    boolean fitsInLong() {
      return true;
    }

    @ExportMessage
    boolean fitsInBigInteger() {
      return false;
    }

    @ExportMessage
    boolean fitsInFloat() {
      return false;
    }

    @ExportMessage
    boolean fitsInDouble() {
      return true;
    }

    @ExportMessage
    byte asByte() throws UnsupportedMessageException {
      throw UnsupportedMessageException.create();
    }

    @ExportMessage
    short asShort() throws UnsupportedMessageException {
      throw UnsupportedMessageException.create();
    }

    @ExportMessage
    int asInt() throws UnsupportedMessageException {
      throw UnsupportedMessageException.create();
    }

    @ExportMessage
    long asLong() throws UnsupportedMessageException {
      throw UnsupportedMessageException.create();
    }

    @ExportMessage
    BigInteger asBigInteger() throws UnsupportedMessageException {
      throw UnsupportedMessageException.create();
    }

    @ExportMessage
    float asFloat() throws UnsupportedMessageException {
      throw UnsupportedMessageException.create();
    }

    @ExportMessage
    double asDouble() throws UnsupportedMessageException {
      throw UnsupportedMessageException.create();
    }

    @ExportMessage
    boolean isBoolean() {
      return true;
    }

    @ExportMessage
    boolean asBoolean() throws UnsupportedMessageException {
      throw UnsupportedMessageException.create();
    }

    @ExportMessage
    boolean isPointer() {
      return true;
    }

    @ExportMessage
    long asPointer() throws UnsupportedMessageException {
      throw UnsupportedMessageException.create();
    }

    @ExportMessage
    boolean isString() {
      return true;
    }

    @ExportMessage
    String asString() throws UnsupportedMessageException {
      throw UnsupportedMessageException.create();
    }
  }

  /** A buffer whose size and contents disagree. */
  @ExportLibrary(InteropLibrary.class)
  static final class ShrinkingBuffer implements TruffleObject {
    final long size;

    ShrinkingBuffer(long size) {
      this.size = size;
    }

    @ExportMessage
    boolean hasBufferElements() {
      return true;
    }

    @ExportMessage
    boolean isBufferWritable() {
      return true;
    }

    @ExportMessage
    long getBufferSize() {
      return size;
    }

    @ExportMessage
    byte readBufferByte(long offset) throws InvalidBufferOffsetException {
      throw InvalidBufferOffsetException.create(offset, 1);
    }

    @ExportMessage
    void readBuffer(long offset, byte[] destination, int destinationOffset, int length)
        throws InvalidBufferOffsetException {
      throw InvalidBufferOffsetException.create(offset, length);
    }

    @ExportMessage
    void writeBufferByte(long offset, byte value) throws InvalidBufferOffsetException {
      throw InvalidBufferOffsetException.create(offset, 1);
    }

    @ExportMessage
    short readBufferShort(ByteOrder order, long offset) throws UnsupportedMessageException {
      throw UnsupportedMessageException.create();
    }

    @ExportMessage
    void writeBufferShort(ByteOrder order, long offset, short value)
        throws UnsupportedMessageException {
      throw UnsupportedMessageException.create();
    }

    @ExportMessage
    int readBufferInt(ByteOrder order, long offset) throws UnsupportedMessageException {
      throw UnsupportedMessageException.create();
    }

    @ExportMessage
    void writeBufferInt(ByteOrder order, long offset, int value)
        throws UnsupportedMessageException {
      throw UnsupportedMessageException.create();
    }

    @ExportMessage
    long readBufferLong(ByteOrder order, long offset) throws UnsupportedMessageException {
      throw UnsupportedMessageException.create();
    }

    @ExportMessage
    void writeBufferLong(ByteOrder order, long offset, long value)
        throws UnsupportedMessageException {
      throw UnsupportedMessageException.create();
    }

    @ExportMessage
    float readBufferFloat(ByteOrder order, long offset) throws UnsupportedMessageException {
      throw UnsupportedMessageException.create();
    }

    @ExportMessage
    void writeBufferFloat(ByteOrder order, long offset, float value)
        throws UnsupportedMessageException {
      throw UnsupportedMessageException.create();
    }

    @ExportMessage
    double readBufferDouble(ByteOrder order, long offset) throws UnsupportedMessageException {
      throw UnsupportedMessageException.create();
    }

    @ExportMessage
    void writeBufferDouble(ByteOrder order, long offset, double value)
        throws UnsupportedMessageException {
      throw UnsupportedMessageException.create();
    }
  }

  @Test
  void misbehavingInteropObjectsFailSafely() {
    Liar liar = new Liar();
    assertThrows(
        Throwable.class,
        () -> new IntegerCoercion("f", "p", 32, false, CoercionPolicy.STRICT).execute(liar));
    assertThrows(Throwable.class, () -> new FloatingCoercion("f", "p").execute(liar));
    assertThrows(Throwable.class, () -> new BooleanCoercion("f", "p").execute(liar));
    assertThrows(Throwable.class, () -> new PointerCoercion("f", "p", true, true).execute(liar));
    assertThrows(
        Throwable.class, () -> new BytesCoercion("f", "p", true, false, false).execute(liar));
    var read = new BytesCoercion("f", "p", false, false, true);
    var shrank =
        assertThrows(NativeCellException.class, () -> read.execute(new ShrinkingBuffer(4)));
    assertEquals("f: parameter p: buffer shrank while being borrowed", shrank.getMessage());
    var huge =
        assertThrows(NativeCellException.class, () -> read.execute(new ShrinkingBuffer(1L << 40)));
    assertTrue(huge.getMessage().contains("too large to copy"));
    // A buffer that rejects the write-back after lending writable bytes.
    Borrow borrow = new BytesCoercion("f", "p", false, true, true).execute(new RefusingBuffer());
    assertThrows(Throwable.class, borrow::release);
  }

  /** Lends writable bytes, then refuses the write-back. */
  @ExportLibrary(InteropLibrary.class)
  static final class RefusingBuffer implements TruffleObject {
    @ExportMessage
    boolean hasBufferElements() {
      return true;
    }

    @ExportMessage
    boolean isBufferWritable() {
      return true;
    }

    @ExportMessage
    long getBufferSize() {
      return 2;
    }

    @ExportMessage
    byte readBufferByte(long offset) {
      return 0;
    }

    @ExportMessage
    void readBuffer(long offset, byte[] destination, int destinationOffset, int length) {}

    @ExportMessage
    void writeBufferByte(long offset, byte value) throws UnsupportedMessageException {
      throw UnsupportedMessageException.create();
    }

    @ExportMessage
    short readBufferShort(ByteOrder order, long offset) throws UnsupportedMessageException {
      throw UnsupportedMessageException.create();
    }

    @ExportMessage
    void writeBufferShort(ByteOrder order, long offset, short value)
        throws UnsupportedMessageException {
      throw UnsupportedMessageException.create();
    }

    @ExportMessage
    int readBufferInt(ByteOrder order, long offset) throws UnsupportedMessageException {
      throw UnsupportedMessageException.create();
    }

    @ExportMessage
    void writeBufferInt(ByteOrder order, long offset, int value)
        throws UnsupportedMessageException {
      throw UnsupportedMessageException.create();
    }

    @ExportMessage
    long readBufferLong(ByteOrder order, long offset) throws UnsupportedMessageException {
      throw UnsupportedMessageException.create();
    }

    @ExportMessage
    void writeBufferLong(ByteOrder order, long offset, long value)
        throws UnsupportedMessageException {
      throw UnsupportedMessageException.create();
    }

    @ExportMessage
    float readBufferFloat(ByteOrder order, long offset) throws UnsupportedMessageException {
      throw UnsupportedMessageException.create();
    }

    @ExportMessage
    void writeBufferFloat(ByteOrder order, long offset, float value)
        throws UnsupportedMessageException {
      throw UnsupportedMessageException.create();
    }

    @ExportMessage
    double readBufferDouble(ByteOrder order, long offset) throws UnsupportedMessageException {
      throw UnsupportedMessageException.create();
    }

    @ExportMessage
    void writeBufferDouble(ByteOrder order, long offset, double value)
        throws UnsupportedMessageException {
      throw UnsupportedMessageException.create();
    }
  }

  @Test
  void executableWrapperDispatchesCachedAndMegamorphic() throws Exception {
    NativeFunction sum = new SumCell(CoercionPolicy.STRICT).function();
    InteropLibrary cached = InteropLibrary.getFactory().create(sum);
    assertEquals(3, cached.execute(sum, 1, 2));
    assertEquals(4, cached.execute(sum, 2, 2));
    InteropLibrary dispatched = InteropLibrary.getFactory().createDispatched(3);
    for (int i = 0; i < 5; i++) {
      NativeFunction fresh = new SumCell(CoercionPolicy.STRICT).function();
      assertEquals(i + 1, dispatched.execute(fresh, i, 1));
      assertEquals(i + 2, dispatched.execute(fresh, i, 2));
    }
  }

  static ForeignNumber big(String value) {
    return new ForeignNumber(new BigInteger(value));
  }

  /** An interop number of any magnitude, like a guest big integer. */
  @ExportLibrary(InteropLibrary.class)
  static final class ForeignNumber implements TruffleObject {
    final BigInteger value;

    ForeignNumber(BigInteger value) {
      this.value = value;
    }

    @ExportMessage
    boolean isNumber() {
      return true;
    }

    @ExportMessage
    boolean fitsInByte() {
      return value.bitLength() < 8;
    }

    @ExportMessage
    boolean fitsInShort() {
      return value.bitLength() < 16;
    }

    @ExportMessage
    boolean fitsInInt() {
      return value.bitLength() < 32;
    }

    @ExportMessage
    boolean fitsInLong() {
      return value.bitLength() < 64;
    }

    @ExportMessage
    boolean fitsInBigInteger() {
      return true;
    }

    @ExportMessage
    boolean fitsInFloat() {
      return value.bitLength() < 24;
    }

    @ExportMessage
    boolean fitsInDouble() {
      return value.bitLength() < 53;
    }

    @ExportMessage
    byte asByte() throws UnsupportedMessageException {
      if (!fitsInByte()) throw UnsupportedMessageException.create();
      return value.byteValue();
    }

    @ExportMessage
    short asShort() throws UnsupportedMessageException {
      if (!fitsInShort()) throw UnsupportedMessageException.create();
      return value.shortValue();
    }

    @ExportMessage
    int asInt() throws UnsupportedMessageException {
      if (!fitsInInt()) throw UnsupportedMessageException.create();
      return value.intValue();
    }

    @ExportMessage
    long asLong() throws UnsupportedMessageException {
      if (!fitsInLong()) throw UnsupportedMessageException.create();
      return value.longValue();
    }

    @ExportMessage
    BigInteger asBigInteger() {
      return value;
    }

    @ExportMessage
    float asFloat() throws UnsupportedMessageException {
      if (!fitsInFloat()) throw UnsupportedMessageException.create();
      return value.floatValue();
    }

    @ExportMessage
    double asDouble() throws UnsupportedMessageException {
      if (!fitsInDouble()) throw UnsupportedMessageException.create();
      return value.doubleValue();
    }
  }

  /**
   * An interop number that fits in neither {@code long} nor {@code BigInteger} but does fit in
   * {@code double}; a magnitude at or beyond {@code 2^63} drives it through {@code modular()}
   * instead of {@code big()}. Exercises the double-beyond-long-range truncation path.
   */
  @ExportLibrary(InteropLibrary.class)
  static final class LargeDouble implements TruffleObject {
    final double value;

    LargeDouble(double value) {
      this.value = value;
    }

    @ExportMessage
    boolean isNumber() {
      return true;
    }

    @ExportMessage
    boolean fitsInByte() {
      return false;
    }

    @ExportMessage
    boolean fitsInShort() {
      return false;
    }

    @ExportMessage
    boolean fitsInInt() {
      return false;
    }

    @ExportMessage
    boolean fitsInLong() {
      return false;
    }

    @ExportMessage
    boolean fitsInBigInteger() {
      return false;
    }

    @ExportMessage
    boolean fitsInFloat() {
      return false;
    }

    @ExportMessage
    boolean fitsInDouble() {
      return true;
    }

    @ExportMessage
    byte asByte() throws UnsupportedMessageException {
      throw UnsupportedMessageException.create();
    }

    @ExportMessage
    short asShort() throws UnsupportedMessageException {
      throw UnsupportedMessageException.create();
    }

    @ExportMessage
    int asInt() throws UnsupportedMessageException {
      throw UnsupportedMessageException.create();
    }

    @ExportMessage
    long asLong() throws UnsupportedMessageException {
      throw UnsupportedMessageException.create();
    }

    @ExportMessage
    BigInteger asBigInteger() throws UnsupportedMessageException {
      throw UnsupportedMessageException.create();
    }

    @ExportMessage
    float asFloat() throws UnsupportedMessageException {
      throw UnsupportedMessageException.create();
    }

    @ExportMessage
    double asDouble() {
      return value;
    }
  }

  private static void inject(Coercion coercion, InteropLibrary library)
      throws ReflectiveOperationException {
    var field = Coercion.class.getDeclaredField("interop");
    field.setAccessible(true);
    field.set(coercion, library);
  }

  private static Object readWidth(Object buffer, int width, long offset) throws Exception {
    return switch (width) {
      case 1 -> INTEROP.readBufferByte(buffer, offset);
      case 2 -> INTEROP.readBufferShort(buffer, ByteOrder.LITTLE_ENDIAN, offset);
      case 4 -> INTEROP.readBufferInt(buffer, ByteOrder.BIG_ENDIAN, offset);
      default -> INTEROP.readBufferLong(buffer, ByteOrder.LITTLE_ENDIAN, offset);
    };
  }

  private static void writeWidth(Object buffer, int width, long offset) throws Exception {
    switch (width) {
      case 1 -> INTEROP.writeBufferByte(buffer, offset, (byte) 1);
      case 2 -> INTEROP.writeBufferShort(buffer, ByteOrder.BIG_ENDIAN, offset, (short) 1);
      case 4 -> INTEROP.writeBufferFloat(buffer, ByteOrder.LITTLE_ENDIAN, offset, 1);
      default -> INTEROP.writeBufferDouble(buffer, ByteOrder.BIG_ENDIAN, offset, 1);
    }
  }

  @Test
  void interopBufferOffsetsRejectEveryOutOfRangeExtent() throws Exception {
    try (Arena arena = Arena.ofConfined()) {
      MemorySegment segment = arena.allocate(16);
      for (Object buffer :
          new Object[] {NativeBytes.of(segment, true), new HeapBytes(new byte[16], true)}) {
        long size = INTEROP.getBufferSize(buffer);
        for (int width : new int[] {1, 2, 4, 8}) {
          writeWidth(buffer, width, size - width);
          readWidth(buffer, width, size - width);
          for (long offset :
              new long[] {
                -1,
                -width,
                Long.MIN_VALUE,
                Long.MAX_VALUE,
                Long.MAX_VALUE - width + 1,
                size - width + 1,
                size,
                Integer.MAX_VALUE + 1L,
                Integer.MIN_VALUE
              }) {
            String where = buffer + " width " + width + " offset " + offset;
            assertThrows(
                InvalidBufferOffsetException.class, () -> readWidth(buffer, width, offset), where);
            assertThrows(
                InvalidBufferOffsetException.class, () -> writeWidth(buffer, width, offset), where);
          }
        }
        byte[] destination = new byte[16];
        INTEROP.readBuffer(buffer, size, destination, 0, 0);
        for (long[] extent :
            new long[][] {
              {size + 1, 0},
              {-1, 0},
              {Long.MIN_VALUE, 1},
              {Long.MAX_VALUE, 1},
              {1, 16},
              {0, -1},
              {0, Integer.MIN_VALUE},
              {size, 1}
            }) {
          assertThrows(
              InvalidBufferOffsetException.class,
              () -> INTEROP.readBuffer(buffer, extent[0], destination, 0, (int) extent[1]),
              buffer + " extent " + extent[0] + "+" + extent[1]);
        }
        // The destination array's bounds are Java's: never a native or heap overrun.
        assertThrows(
            IndexOutOfBoundsException.class,
            () -> INTEROP.readBuffer(buffer, 0, destination, 15, 2));
        assertThrows(
            IndexOutOfBoundsException.class,
            () -> INTEROP.readBuffer(buffer, 0, destination, -1, 1));
      }
    }
    NativeBytes empty = NativeBytes.of(0, 0, true);
    assertThrows(InvalidBufferOffsetException.class, () -> INTEROP.readBufferByte(empty, 0));
    assertThrows(
        InvalidBufferOffsetException.class, () -> INTEROP.writeBufferByte(empty, 0, (byte) 0));
    INTEROP.readBuffer(empty, 0, new byte[0], 0, 0);
  }

  @Test
  void untrustedInteropBufferSizesAreGuestErrors() throws Exception {
    // Bypass Truffle's assertion wrapper: it would reject the lie before the coercion sees it,
    // hiding the assertions-off behavior a production image has.
    for (long size : new long[] {-1, Integer.MIN_VALUE, Long.MIN_VALUE, -(1L << 32) + 4}) {
      var buffer = new ShrinkingBuffer(size);
      var coercion = new BytesCoercion("f", "p", false, false, true);
      inject(coercion, direct(buffer));
      var error = assertThrows(NativeCellException.class, () -> coercion.execute(buffer));
      assertEquals("f: parameter p: negative interop buffer size: " + size, error.getMessage());
    }
  }

  /** Dispatches buffer messages straight to an exported object, without assertion checks. */
  private static InteropLibrary direct(ShrinkingBuffer buffer) {
    return new InteropLibrary() {
      @Override
      public boolean accepts(Object receiver) {
        return receiver == buffer;
      }

      @Override
      public boolean hasBufferElements(Object receiver) {
        return buffer.hasBufferElements();
      }

      @Override
      public boolean isBufferWritable(Object receiver) {
        return buffer.isBufferWritable();
      }

      @Override
      public long getBufferSize(Object receiver) {
        return buffer.getBufferSize();
      }
    };
  }

  /** A writable guest buffer that guest code shrinks while a native call has it borrowed. */
  @ExportLibrary(InteropLibrary.class)
  static final class ResizableBuffer implements TruffleObject {
    final byte[] data;
    int size;
    boolean detached;

    ResizableBuffer(int size) {
      this.data = new byte[size];
      this.size = size;
    }

    @ExportMessage
    boolean hasBufferElements() {
      return true;
    }

    @ExportMessage
    boolean isBufferWritable() {
      return !detached;
    }

    @ExportMessage
    long getBufferSize() {
      return size;
    }

    @ExportMessage
    byte readBufferByte(long offset) throws InvalidBufferOffsetException {
      if (offset < 0 || offset >= size) throw InvalidBufferOffsetException.create(offset, 1);
      return data[(int) offset];
    }

    @ExportMessage
    void readBuffer(long offset, byte[] destination, int destinationOffset, int length)
        throws InvalidBufferOffsetException {
      if (offset < 0 || length < 0 || offset > size - length)
        throw InvalidBufferOffsetException.create(offset, length);
      System.arraycopy(data, (int) offset, destination, destinationOffset, length);
    }

    @ExportMessage
    void writeBufferByte(long offset, byte value)
        throws InvalidBufferOffsetException, UnsupportedMessageException {
      if (detached) throw UnsupportedMessageException.create();
      if (offset < 0 || offset >= size) throw InvalidBufferOffsetException.create(offset, 1);
      data[(int) offset] = value;
    }

    @ExportMessage
    short readBufferShort(ByteOrder order, long offset) throws UnsupportedMessageException {
      throw UnsupportedMessageException.create();
    }

    @ExportMessage
    void writeBufferShort(ByteOrder order, long offset, short value)
        throws UnsupportedMessageException {
      throw UnsupportedMessageException.create();
    }

    @ExportMessage
    int readBufferInt(ByteOrder order, long offset) throws UnsupportedMessageException {
      throw UnsupportedMessageException.create();
    }

    @ExportMessage
    void writeBufferInt(ByteOrder order, long offset, int value)
        throws UnsupportedMessageException {
      throw UnsupportedMessageException.create();
    }

    @ExportMessage
    long readBufferLong(ByteOrder order, long offset) throws UnsupportedMessageException {
      throw UnsupportedMessageException.create();
    }

    @ExportMessage
    void writeBufferLong(ByteOrder order, long offset, long value)
        throws UnsupportedMessageException {
      throw UnsupportedMessageException.create();
    }

    @ExportMessage
    float readBufferFloat(ByteOrder order, long offset) throws UnsupportedMessageException {
      throw UnsupportedMessageException.create();
    }

    @ExportMessage
    void writeBufferFloat(ByteOrder order, long offset, float value)
        throws UnsupportedMessageException {
      throw UnsupportedMessageException.create();
    }

    @ExportMessage
    double readBufferDouble(ByteOrder order, long offset) throws UnsupportedMessageException {
      throw UnsupportedMessageException.create();
    }

    @ExportMessage
    void writeBufferDouble(ByteOrder order, long offset, double value)
        throws UnsupportedMessageException {
      throw UnsupportedMessageException.create();
    }
  }

  @Test
  void writeBackToABufferShrunkDuringTheCallIsAGuestError() {
    var buffer = new ResizableBuffer(4);
    Borrow borrow = new BytesCoercion("f", "p", false, true, true).execute(buffer);
    assertEquals(4, borrow.length());
    // The native side fills its copy; a guest callback then shrinks the source buffer.
    MemorySegment.ofAddress(borrow.address()).reinterpret(4).fill((byte) 7);
    buffer.size = 2;
    var error = assertThrows(NativeCellException.class, borrow::release);
    assertEquals("borrowed buffer shrank before write-back of byte 2", error.getMessage());
    assertArrayEquals(new byte[] {7, 7, 0, 0}, buffer.data, "surviving bytes are written back");
    borrow.release();
  }

  @Test
  void writeBackToABufferDetachedDuringTheCallIsAGuestError() {
    var buffer = new ResizableBuffer(4);
    Borrow borrow = new BytesCoercion("f", "p", false, true, true).execute(buffer);
    MemorySegment.ofAddress(borrow.address()).reinterpret(4).fill((byte) 7);
    // A guest callback detaches the source buffer: it stops accepting writes altogether.
    buffer.detached = true;
    var error = assertThrows(NativeCellException.class, borrow::release);
    assertEquals("borrowed buffer stopped accepting writes before write-back", error.getMessage());
    assertArrayEquals(new byte[4], buffer.data, "nothing is written to a detached buffer");
    borrow.release();
  }

  /** Closes the resource during the coercion's isPointer() probe, then answers as told. */
  private static InteropLibrary closingDuringIsPointer(NativeResource resource, boolean answer) {
    return new InteropLibrary() {
      @Override
      public boolean accepts(Object receiver) {
        return receiver == resource;
      }

      @Override
      public boolean isPointer(Object receiver) {
        Thread closer = new Thread(resource::close, "closer");
        closer.start();
        try {
          closer.join(5000);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          throw new AssertionError(e);
        }
        assertTrue(resource.isClosed());
        return answer;
      }

      @Override
      public long asPointer(Object receiver) throws UnsupportedMessageException {
        return INTEROP.asPointer(receiver);
      }
    };
  }

  @Test
  void concurrentCloseOfAResourcePassedAsBytesOrPointerIsAGuestError() throws Exception {
    // isPointer() true then close: asPointer() fails. isPointer() false after close: no type error.
    for (boolean answer : new boolean[] {true, false}) {
      var bytesResource = new NativeResource("Ticket", 0x7000L, address -> {}, null);
      var bytes = new BytesCoercion("f", "p", false, false, false);
      inject(bytes, closingDuringIsPointer(bytesResource, answer));
      assertEquals(
          "f: parameter p received a closed Ticket",
          assertThrows(NativeCellException.class, () -> bytes.execute(bytesResource)).getMessage());
      var pointerResource = new NativeResource("Ticket", 0x7000L, address -> {}, null);
      var pointer = new PointerCoercion("f", "p", false, false);
      inject(pointer, closingDuringIsPointer(pointerResource, answer));
      assertEquals(
          "f: parameter p received a closed Ticket",
          assertThrows(NativeCellException.class, () -> pointer.execute(pointerResource))
              .getMessage());
    }
  }

  @Test
  void closeRacingCellCallsYieldsTheAddressOrAGuestErrorAndDestroysOnce() throws Exception {
    for (int round = 0; round < 100; round++) {
      long address = 0x8000L + round * 16;
      var destroyed = new java.util.concurrent.atomic.AtomicInteger();
      var resource = new NativeResource("Ticket", address, a -> destroyed.incrementAndGet(), null);
      var function = new PointerCell(false, false).function();
      var start = new java.util.concurrent.CountDownLatch(1);
      var unexpected = new AtomicReference<Throwable>();
      var threads = new ArrayList<Thread>();
      for (int t = 0; t < 4; t++) {
        boolean closer = t >= 2;
        threads.add(
            new Thread(
                () -> {
                  try {
                    start.await();
                    if (closer) {
                      resource.close();
                      return;
                    }
                    for (int i = 0; i < 200; i++) {
                      try {
                        Object result = function.call(resource);
                        if (!Long.valueOf(address).equals(result))
                          unexpected.compareAndSet(null, new AssertionError("got " + result));
                      } catch (NativeCellException e) {
                        if (!e.getMessage().equals("f: parameter p received a closed Ticket"))
                          unexpected.compareAndSet(null, e);
                      }
                    }
                  } catch (Throwable t2) {
                    unexpected.compareAndSet(null, t2);
                  }
                },
                (closer ? "closer-" : "caller-") + t));
      }
      threads.forEach(Thread::start);
      start.countDown();
      for (Thread thread : threads) thread.join(10_000);
      if (unexpected.get() != null) throw new AssertionError("round " + round, unexpected.get());
      assertEquals(1, destroyed.get(), "concurrent closes destroy exactly once");
      assertEquals(
          "f: parameter p received a closed Ticket",
          assertThrows(NativeCellException.class, () -> function.call(resource)).getMessage());
    }
  }
}
