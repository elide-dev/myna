package dev.elide.myna;

import static dev.elide.myna.values.ValueTape.*;
import static org.junit.jupiter.api.Assertions.*;

import dev.elide.myna.values.ValueTape;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

final class ValueTapeTest {
  /** JVM adapter for testing the exact reader used by NativeValueTape. */
  private static class Tape extends ValueTape {
    final ByteBuffer nodes;
    final byte[] bytes;

    Tape(long[][] values, String text) {
      this(values, text, 3);
    }

    Tape(long[][] values, String text, int version) {
      this(values, text, version, 0);
    }

    Tape(long[][] values, String text, int version, int flags) {
      super(values.length, text.getBytes(StandardCharsets.UTF_8).length, version, flags);
      bytes = text.getBytes(StandardCharsets.UTF_8);
      nodes = ByteBuffer.allocate(values.length * NODE_SIZE).order(ByteOrder.nativeOrder());
      for (long[] value : values) {
        long payload = value[2], next = value[3];
        if ((value[0] == STRING || value[0] == DECIMAL)
            && value[1] <= 16
            && payload >= 0
            && payload + value[1] <= bytes.length) {
          long low = 0, high = 0;
          for (int i = 0; i < value[1]; i++) {
            if (i < 8) low |= (bytes[(int) payload + i] & 255L) << (i * 8);
            else high |= (bytes[(int) payload + i] & 255L) << ((i - 8) * 8);
          }
          payload = low;
          next = high;
        }
        nodes
            .putInt(
                (int) value[0]
                    | (version == 3 && (value[0] == STRING || value[0] == DECIMAL) && value[1] > 16
                        ? REGION_TEXT
                        : 0))
            .putInt((int) value[1])
            .putLong(payload)
            .putLong(next);
      }
    }

    @Override
    protected int readInt(long offset) {
      return nodes.getInt(Math.toIntExact(offset));
    }

    @Override
    protected long readLong(long offset) {
      return nodes.getLong(Math.toIntExact(offset));
    }

    @Override
    protected byte readByte(long offset) {
      return bytes[Math.toIntExact(offset)];
    }
  }

  private static Tape tape(long[]... nodes) {
    return new Tape(nodes, "");
  }

  @Test
  void nonFiniteFloatsRequireTheDescriptorCapability() {
    for (long bits :
        new long[] {
          0x7ff0000000000000L,
          0xfff0000000000000L,
          0x7ff8000000000042L,
          Long.MIN_VALUE,
          Double.doubleToRawLongBits(1.5)
        }) {
      var values = new long[][] {node(F64, 0, bits, 1)};
      var enabled = new Tape(values, "", ABI_VERSION, ALLOW_NON_FINITE);
      enabled.validate();
      assertEquals(ALLOW_NON_FINITE, enabled.flags());
      assertEquals(bits, enabled.bits(0));
      assertEquals(bits, Double.doubleToRawLongBits(enabled.doubleValue(0)));
      var strict = new Tape(values, "");
      assertEquals(0, strict.flags());
      if (Double.isFinite(Double.longBitsToDouble(bits))) strict.validate();
      else assertThrows(IllegalArgumentException.class, strict::validate);
    }
    var value = new long[][] {node(NULL, 0, 0, 1)};
    for (int flags : new int[] {2, 3, -1})
      assertThrows(IllegalArgumentException.class, () -> new Tape(value, "", ABI_VERSION, flags));
    assertThrows(IllegalArgumentException.class, () -> new Tape(value, "", 2, ALLOW_NON_FINITE));
    var decimal =
        new Tape(new long[][] {node(DECIMAL, 4, 0, 1)}, ".inf", ABI_VERSION, ALLOW_NON_FINITE);
    assertThrows(IllegalArgumentException.class, decimal::validate);
  }

  private static long[] node(int tag, long length, long bits, long next) {
    return new long[] {tag, length, bits, next};
  }

  private static void invalid(long[]... nodes) {
    assertThrows(IllegalArgumentException.class, () -> tape(nodes).validate());
  }

  @Test
  void descriptorLimitsAndDirectTextValidation() {
    class EmptyValue extends ValueTape {
      EmptyValue(long count, long bytes) {
        super(count, bytes);
      }

      @Override
      protected int readInt(long offset) {
        return 0;
      }

      @Override
      protected long readLong(long offset) {
        return offset == 16 ? 1 : 0;
      }

      @Override
      protected byte readByte(long offset) {
        throw new AssertionError("No text allocation");
      }
    }
    new EmptyValue(1, 0).validate();
    assertThrows(IllegalArgumentException.class, () -> new EmptyValue(Long.MAX_VALUE, 0));
    assertThrows(IllegalArgumentException.class, () -> new EmptyValue(1, -1));
    var malformed = tape(node(STRING | 0x200, 0, 0, 1));
    assertThrows(IllegalArgumentException.class, () -> malformed.stringValue(0));
    var negativeOffset = new Tape(new long[][] {node(STRING, 17, -1, 1)}, "", 2);
    assertThrows(IllegalArgumentException.class, () -> negativeOffset.stringValue(0));
    var oldInline = new Tape(new long[][] {node(STRING, 1, 0, 1)}, "x", 2);
    oldInline.validate();
    assertEquals("x", oldInline.stringValue(0));
    var differingSuffix = new Tape(new long[][] {node(STRING, 9, 0, 1)}, "12345678x");
    assertFalse(differingSuffix.stringEquals(0, Utf8Key.of("12345678y")));
  }

  @Test
  void exactNumbersAndBorrowedRegions() {
    for (String token :
        new String[] {
          "-0", "18446744073709551615", "1.234567890123456789012345678901", "1e-999999999999"
        }) {
      var t = new Tape(new long[][] {node(DECIMAL, token.length(), 0, 1)}, token);
      t.validate();
      assertEquals(token, t.numberText(0));
    }
    var integer = new Tape(new long[][] {node(DECIMAL, 2, 0, 1)}, "42");
    assertEquals(42, integer.decimalLongExact(0));
    var fraction = new Tape(new long[][] {node(DECIMAL, 3, 0, 1)}, "1.5");
    assertThrows(ArithmeticException.class, () -> fraction.decimalLongExact(0));
    var huge =
        new Tape(new long[][] {node(DECIMAL, "1e-999999999999".length(), 0, 1)}, "1e-999999999999");
    assertThrows(NumberFormatException.class, () -> huge.decimalValue(0));
    for (String token : new String[] {"", "01", "+1", "NaN", "1.", "1e", "1 2"}) {
      var t = new Tape(new long[][] {node(DECIMAL, token.length(), 0, 1)}, token);
      assertThrows(IllegalArgumentException.class, t::validate);
    }
    byte[] region = "xborrowed🦀".getBytes(StandardCharsets.UTF_8);
    var borrowed =
        new Tape(
            new long[][] {node(STRING | REGION_TEXT, region.length - 1, (1L << 32) | 1, 1)}, "") {
          @Override
          protected long regionLength(long id) {
            if (id != 1) throw new IllegalArgumentException();
            return region.length;
          }

          @Override
          protected byte readRegionByte(long id, long offset) {
            if (id != 1) throw new IllegalArgumentException();
            return region[(int) offset];
          }
        };
    borrowed.validate();
    assertFalse(borrowed.isInlineText(0));
    assertEquals("borrowed🦀", borrowed.stringValue(0));
    assertTrue(borrowed.stringEquals(0, Utf8Key.of("borrowed🦀")));
    byte[] copy = new byte[region.length - 1];
    assertEquals(copy.length, borrowed.copyStringUtf8(0, copy, 0));
    assertArrayEquals(java.util.Arrays.copyOfRange(region, 1, region.length), copy);
    invalid(node(STRING | REGION_TEXT, 1, 1L << 32, 1));
    invalid(node(BOOL | REGION_TEXT, 0, 0, 1));
    invalid(node(STRING | 0x200, 0, 0, 1));
    assertThrows(
        IllegalArgumentException.class, () -> new Tape(new long[][] {node(NULL, 0, 0, 1)}, "", 1));
    var old = new Tape(new long[][] {node(STRING, 17, 0, 1)}, "12345678901234567", 2);
    old.validate();
    assertEquals("12345678901234567", old.stringValue(0));
    assertThrows(
        IllegalArgumentException.class,
        () -> new Tape(new long[][] {node(DECIMAL, 1, 0, 1)}, "1", 2).validate());
  }

  @Test
  void nestedObjectsAndArrays() {
    var tape =
        new Tape(
            new long[][] {
              node(OBJECT, 2, 0, 10),
              node(STRING, 1, 0, 2),
              node(ARRAY, 2, 0, 7),
              node(I64, 0, -7, 4),
              node(ARRAY, 2, 0, 7),
              node(BOOL, 0, 1, 6),
              node(NULL, 0, 0, 7),
              node(STRING, 1, 1, 8),
              node(ARRAY, 1, 0, 10),
              node(U64, 0, -1, 10)
            },
            "ab");
    tape.validate();
    assertEquals(10, tape.nodeCount());
    assertEquals(2, tape.byteCount());
    assertEquals(1, tape.first(0));
    assertEquals(2, tape.find(0, new byte[] {'a'}));
    assertEquals(8, tape.find(0, new byte[] {'b'}));
    assertEquals(-1, tape.find(0, new byte[] {'c'}));
    assertEquals(-7, tape.signedValue(3));
    assertEquals(-1, tape.unsignedBits(9));
    assertTrue(tape.booleanValue(5));
    assertEquals("a", tape.stringValue(1));
    assertEquals('b', tape.stringByte(7, 0));
    assertFalse(tape.stringEquals(1, new byte[0]));
    assertFalse(tape.stringEquals(1, new byte[] {'b'}));
    assertThrows(IndexOutOfBoundsException.class, () -> tape.tag(-1));
    assertThrows(IndexOutOfBoundsException.class, () -> tape.tag(10));
    assertThrows(IndexOutOfBoundsException.class, () -> tape.stringByte(1, -1));
    assertThrows(IndexOutOfBoundsException.class, () -> tape.stringByte(1, 1));
    assertThrows(IllegalArgumentException.class, () -> tape.doubleValue(3));
    assertThrows(IllegalArgumentException.class, () -> tape.first(3));
  }

  @Test
  void scalarsAndEmptyContainers() {
    for (int tag : new int[] {NULL, BOOL, I64, U64, F64, STRING, ARRAY, OBJECT}) {
      var tape = tape(node(tag, 0, 0, 1));
      tape.validate();
    }
    assertFalse(tape(node(BOOL, 0, 0, 1)).booleanValue(0));
    assertEquals(-0.0, tape(node(F64, 0, Long.MIN_VALUE, 1)).doubleValue(0));
    var unicode = new Tape(new long[][] {node(STRING, 5, 0, 1)}, "\0🦀");
    unicode.validate();
    assertEquals("\0🦀", unicode.stringValue(0));
    assertTrue(unicode.stringEquals(0, "\0🦀".getBytes(StandardCharsets.UTF_8)));
  }

  @Test
  void malformedShapes() {
    assertThrows(IllegalArgumentException.class, () -> tape());
    invalid(node(NULL, 0, 0, 0));
    invalid(node(NULL, 0, 0, 2));
    invalid(node(NULL, 0, 0, 1), node(NULL, 0, 0, 2));
    invalid(node(99, 0, 0, 1));
    invalid(node(NULL, 1, 0, 1));
    invalid(node(NULL, 0, 1, 1));
    invalid(node(BOOL, 0, 2, 1));
    invalid(node(F64, 0, Double.doubleToRawLongBits(Double.NaN), 1));
    invalid(node(STRING, 17, 0, 1));
    invalid(node(STRING, 17, -1, 1));
    invalid(node(STRING, 17, 1, 1));
    invalid(node(ARRAY, 0, 1, 1));
    invalid(node(ARRAY, 1, 0, 1));
    invalid(node(ARRAY, 1, 0, 3), node(NULL, 0, 0, 3), node(NULL, 0, 0, 3));
    invalid(node(ARRAY, 2, 0, 4), node(ARRAY, 1, 0, 3), node(ARRAY, 0, 0, 4), node(NULL, 0, 0, 4));
    invalid(node(OBJECT, 1, 0, 3), node(NULL, 0, 0, 2), node(NULL, 0, 0, 3));
    invalid(node(OBJECT, 1, 0, 3), node(STRING, 17, 0, 3), node(NULL, 0, 0, 3));
    invalid(node(OBJECT, 1, 0, 2), node(STRING, 0, 0, 2));
    var malformed = tape(node(OBJECT, 1, 0, 2), node(STRING, 0, 0, 2));
    assertThrows(IllegalArgumentException.class, () -> malformed.find(0, new byte[0]));
    var badKey = tape(node(OBJECT, 1, 0, 3), node(NULL, 0, 0, 2), node(NULL, 0, 0, 3));
    assertThrows(IllegalArgumentException.class, () -> badKey.find(0, new byte[0]));
    var escape =
        tape(
            node(OBJECT, 1, 0, 3),
            node(STRING, 0, 0, 2),
            node(ARRAY, 1, 0, 4),
            node(NULL, 0, 0, 4));
    assertThrows(IllegalArgumentException.class, () -> escape.find(0, new byte[0]));
  }

  @Test
  void inlineBoundariesPreparedKeysAndCopies() {
    for (String text :
        new String[] {
          "",
          "1234567",
          "12345678",
          "123456789",
          "123456789abcdef",
          "123456789abcdef0",
          "123456789abcdef01",
          "🦀🦀🦀🦀",
          "🦀🦀🦀🦀x",
          "a\0b"
        }) {
      byte[] utf8 = text.getBytes(StandardCharsets.UTF_8);
      var tape = new Tape(new long[][] {node(STRING, utf8.length, 0, 1)}, text);
      tape.validate();
      assertEquals(text, tape.stringValue(0));
      assertEquals(1, tape.next(0));
      assertTrue(tape.stringEquals(0, Utf8Key.of(text)));
      assertFalse(tape.stringEquals(0, Utf8Key.of(text + "x")));
      byte[] copy = new byte[utf8.length + 2];
      assertEquals(utf8.length, tape.copyStringUtf8(0, copy, 1));
      assertArrayEquals(utf8, java.util.Arrays.copyOfRange(copy, 1, copy.length - 1));
      assertThrows(IndexOutOfBoundsException.class, () -> tape.copyStringUtf8(0, copy, -1));
    }
    assertThrows(IllegalArgumentException.class, () -> Utf8Key.of("\ud800"));
    var map =
        new Tape(
            new long[][] {node(OBJECT, 1, 0, 3), node(STRING, 1, 0, 2), node(I64, 0, 42, 3)}, "x");
    assertEquals(2, map.find(0, Utf8Key.of("x")));
    assertEquals(-1, map.find(0, Utf8Key.of("y")));
    var badUtf8 = tape(node(STRING, 1, 0xff, 0));
    assertThrows(IllegalArgumentException.class, () -> badUtf8.stringValue(0));
  }
}
