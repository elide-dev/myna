// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package fixture.records;

import com.oracle.svm.shared.NeverInline;
import org.graalvm.nativeimage.StackValue;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.word.Pointer;
import org.graalvm.word.PointerBase;
import org.graalvm.word.WordFactory;

/** Native Image records as @CStruct interfaces, against C's view of the same memory. */
public final class RecordsMain {
  private RecordsMain() {}

  private static void check(boolean condition, String what) {
    if (!condition) throw new AssertionError(what);
  }

  // Measured pairs: each @CField accessor beside the raw offset access that record accessors
  // generated before 0.19 (`((Pointer) record).readX(offset)`). The test compares their code.
  @NeverInline("measured")
  static long readU64(RecordsApi.Rec rec) {
    return rec.unsigned64();
  }

  @NeverInline("measured")
  static long rawReadU64(PointerBase rec) {
    return ((Pointer) rec).readLong(8);
  }

  @NeverInline("measured")
  static double readF64(RecordsApi.Rec rec) {
    return rec.ratio();
  }

  @NeverInline("measured")
  static double rawReadF64(PointerBase rec) {
    return ((Pointer) rec).readDouble(32);
  }

  @NeverInline("measured")
  static RecordsApi.Rec readNext(RecordsApi.Rec rec) {
    return rec.next();
  }

  @NeverInline("measured")
  static PointerBase rawReadNext(PointerBase rec) {
    return ((Pointer) rec).readWord(40);
  }

  @NeverInline("measured")
  static int readU32(RecordsApi.Rec rec) {
    return rec.unsigned32();
  }

  @NeverInline("measured")
  static int rawReadU32(PointerBase rec) {
    return ((Pointer) rec).readInt(60);
  }

  @NeverInline("measured")
  static float readF32(RecordsApi.Rec rec) {
    return rec.scale();
  }

  @NeverInline("measured")
  static float rawReadF32(PointerBase rec) {
    return ((Pointer) rec).readFloat(64);
  }

  @NeverInline("measured")
  static short readI16(RecordsApi.Rec rec) {
    return rec.signed16();
  }

  @NeverInline("measured")
  static short rawReadI16(PointerBase rec) {
    return ((Pointer) rec).readShort(68);
  }

  @NeverInline("measured")
  static short readU16(RecordsApi.Rec rec) {
    return rec.unsigned16();
  }

  @NeverInline("measured")
  static short rawReadU16(PointerBase rec) {
    return ((Pointer) rec).readShort(70);
  }

  @NeverInline("measured")
  static byte readChar(RecordsApi.Rec rec) {
    return rec.letter();
  }

  @NeverInline("measured")
  static byte rawReadChar(PointerBase rec) {
    return ((Pointer) rec).readByte(73);
  }

  @NeverInline("measured")
  static byte readI8(RecordsApi.Rec rec) {
    return rec.signed8();
  }

  @NeverInline("measured")
  static byte rawReadI8(PointerBase rec) {
    return ((Pointer) rec).readByte(74);
  }

  @NeverInline("measured")
  static byte readU8(RecordsApi.Rec rec) {
    return rec.unsigned8();
  }

  @NeverInline("measured")
  static byte rawReadU8(PointerBase rec) {
    return ((Pointer) rec).readByte(75);
  }

  @NeverInline("measured")
  static boolean readBool(RecordsApi.Rec rec) {
    return rec.flag();
  }

  @NeverInline("measured")
  static boolean rawReadBool(PointerBase rec) {
    return ((Pointer) rec).readByte(72) != 0;
  }

  @NeverInline("measured")
  static void writeU64(RecordsApi.Rec rec, long value) {
    rec.unsigned64(value);
  }

  @NeverInline("measured")
  static void rawWriteU64(PointerBase rec, long value) {
    ((Pointer) rec).writeLong(8, value);
  }

  @NeverInline("measured")
  static void writeNext(RecordsApi.Rec rec, RecordsApi.Rec value) {
    rec.next(value);
  }

  @NeverInline("measured")
  static void rawWriteNext(PointerBase rec, PointerBase value) {
    ((Pointer) rec).writeWord(40, value);
  }

  @NeverInline("measured")
  static void writeU32(RecordsApi.Rec rec, int value) {
    rec.unsigned32(value);
  }

  @NeverInline("measured")
  static void rawWriteU32(PointerBase rec, int value) {
    ((Pointer) rec).writeInt(60, value);
  }

  @NeverInline("measured")
  static void writeU16(RecordsApi.Rec rec, short value) {
    rec.unsigned16(value);
  }

  @NeverInline("measured")
  static void rawWriteU16(PointerBase rec, short value) {
    ((Pointer) rec).writeShort(70, value);
  }

  @NeverInline("measured")
  static void writeU8(RecordsApi.Rec rec, byte value) {
    rec.unsigned8(value);
  }

  @NeverInline("measured")
  static void rawWriteU8(PointerBase rec, byte value) {
    ((Pointer) rec).writeByte(75, value);
  }

  @NeverInline("measured")
  static void writeBool(RecordsApi.Rec rec, boolean value) {
    rec.flag(value);
  }

  @NeverInline("measured")
  static void rawWriteBool(PointerBase rec, boolean value) {
    ((Pointer) rec).writeByte(72, (byte) (value ? 1 : 0));
  }

  private static long offset(RecordsApi.Rec rec, PointerBase field) {
    return field.rawValue() - rec.rawValue();
  }

  public static void main(String[] args) {
    check(SizeOf.get(RecordsApi.Rec.class) == RecordsApi.Rec.SIZE, "SizeOf");
    check(RecordsApi.Rec.SIZE == 80 && RecordsApi.Rec.ALIGNMENT == 8, "descriptor layout");
    RecordsApi.Rec rec = StackValue.get(RecordsApi.Rec.class);
    // SVM's offsets come from seam.h; they are the descriptor's.
    long[] offsets = {
      offset(rec, rec.addressOfSigned64()), offset(rec, rec.addressOfUnsigned64()),
      offset(rec, rec.addressOfSize()), offset(rec, rec.addressOfOffset()),
      offset(rec, rec.addressOfRatio()), offset(rec, rec.addressOfNext()),
      offset(rec, rec.addressOfHandle()), offset(rec, rec.addressOfSigned32()),
      offset(rec, rec.addressOfUnsigned32()), offset(rec, rec.addressOfScale()),
      offset(rec, rec.addressOfSigned16()), offset(rec, rec.addressOfUnsigned16()),
      offset(rec, rec.addressOfFlag()), offset(rec, rec.addressOfLetter()),
      offset(rec, rec.addressOfSigned8()), offset(rec, rec.addressOfUnsigned8())
    };
    long[] declared = {0, 8, 16, 24, 32, 40, 48, 56, 60, 64, 68, 70, 72, 73, 74, 75};
    check(java.util.Arrays.equals(offsets, declared), "field offsets");
    // Java writes extremes as raw bits; C checks each one (one bit per field).
    rec.signed64(Long.MIN_VALUE);
    rec.unsigned64(-1L);
    rec.size(-1L);
    rec.offset(Long.MIN_VALUE);
    rec.ratio(1.5);
    rec.next(rec);
    check(rec.addressOfNext().read().rawValue() == rec.rawValue(), "read the struct pointer slot");
    rec.handle(WordFactory.pointer(0x1234));
    rec.signed32(Integer.MIN_VALUE);
    rec.unsigned32(0x80000000);
    rec.scale(-2.5f);
    rec.signed16(Short.MIN_VALUE);
    rec.unsigned16((short) 0xffff);
    rec.flag(true);
    rec.letter((byte) 'z');
    rec.signed8(Byte.MIN_VALUE);
    rec.unsigned8((byte) 0xff);
    check(rec.next().unsigned8() == (byte) 0xff, "typed struct pointer field");
    int mask = RecordsApi.exchange(rec);
    check(mask == 0xffff, "C read Java's fields: " + Integer.toBinaryString(mask));
    // C wrote the opposite extremes; Java reads the same raw bits.
    check(rec.signed64() == Long.MAX_VALUE, "i64");
    check(rec.unsigned64() == Long.MIN_VALUE, "u64 bit 63");
    check(rec.size() == 1 && rec.offset() == -1, "usize/isize");
    check(rec.ratio() == -0.25 && rec.scale() == 0.5f, "f64/f32");
    check(rec.next().isNull(), "null struct pointer");
    check(rec.handle().rawValue() == 0x5678, "opaque pointer field");
    check(rec.signed32() == Integer.MAX_VALUE, "i32");
    check(rec.unsigned32() == -1 && Integer.toUnsignedLong(rec.unsigned32()) == 0xffffffffL, "u32");
    check(rec.signed16() == Short.MAX_VALUE, "i16");
    check(rec.unsigned16() == (short) 0x8000 && Short.toUnsignedInt(rec.unsigned16()) == 0x8000, "u16");
    check(!rec.flag(), "bool false");
    check(rec.letter() == 'a', "char");
    check(rec.signed8() == Byte.MAX_VALUE, "i8");
    check(rec.unsigned8() == (byte) 0x80 && Byte.toUnsignedInt(rec.unsigned8()) == 0x80, "u8");
    // Any nonzero byte reads as true, as the raw readByte(...) != 0 accessor did.
    rec.addressOfFlag().write((byte) 2);
    check(rec.flag(), "bool nonzero");
    rec.flag(true);
    check(rec.addressOfFlag().read() == 1, "bool writes 1");
    // A typed opaque result from a transitioning @CFunction.
    RecordsApi.Handle handle = RecordsApi.handleOf(rec);
    check(handle.rawValue() == 0x5678, "typed opaque return");
    // Run every measured pair on the same values, so neither side is specialized differently.
    int value = args.length + 0x7f;
    for (int i = 0; i < 2; i++) {
      check(readU64(rec) == rawReadU64(rec) && readF64(rec) == rawReadF64(rec), "measured u64/f64");
      check(readNext(rec).equal(rawReadNext(rec)) && readU32(rec) == rawReadU32(rec), "measured ptr/u32");
      check(readF32(rec) == rawReadF32(rec) && readI16(rec) == rawReadI16(rec), "measured f32/i16");
      check(readU16(rec) == rawReadU16(rec) && readChar(rec) == rawReadChar(rec), "measured u16/char");
      check(readI8(rec) == rawReadI8(rec) && readU8(rec) == rawReadU8(rec), "measured i8/u8");
      check(readBool(rec) == rawReadBool(rec), "measured bool");
      RecordsApi.Rec target = i == 0 ? rec : rec.next();
      if (i == 0) {
        writeU64(target, value);
        writeNext(target, rec);
        writeU32(target, value);
        writeU16(target, (short) value);
        writeU8(target, (byte) value);
        writeBool(target, (value & 1) != 0);
      } else {
        rawWriteU64(target, value);
        rawWriteNext(target, rec);
        rawWriteU32(target, value);
        rawWriteU16(target, (short) value);
        rawWriteU8(target, (byte) value);
        rawWriteBool(target, (value & 1) != 0);
      }
      check(rec.unsigned64() == value && rec.unsigned32() == value, "measured writes");
      check(rec.unsigned16() == (short) value && rec.unsigned8() == (byte) value, "measured narrow writes");
    }
    System.out.println("Native Image @CStruct records passed");
  }
}
