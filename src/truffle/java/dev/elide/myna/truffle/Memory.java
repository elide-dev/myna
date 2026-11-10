// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package dev.elide.myna.truffle;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteOrder;
import org.graalvm.nativeimage.ImageInfo;

/**
 * Scalar access to native memory that partial evaluation can compile. {@code MemorySegment}
 * accessors go through var-handle machinery that runtime compilation must not reach, so inside a
 * Native Image the reads and writes use {@link ImageMemory}'s word operations; HotSpot keeps the
 * segment accessors. The choice is a build-time constant (this package initializes at build time),
 * so the other branch is not part of the image. Callers bounds-check first.
 */
final class Memory {
  private static final boolean IMAGE = ImageInfo.inImageCode();
  private static final ValueLayout.OfShort SHORT_BE =
      ValueLayout.JAVA_SHORT_UNALIGNED.withOrder(ByteOrder.BIG_ENDIAN);
  private static final ValueLayout.OfInt INT_BE =
      ValueLayout.JAVA_INT_UNALIGNED.withOrder(ByteOrder.BIG_ENDIAN);
  private static final ValueLayout.OfLong LONG_LE =
      ValueLayout.JAVA_LONG_UNALIGNED.withOrder(ByteOrder.LITTLE_ENDIAN);
  private static final ValueLayout.OfLong LONG_BE =
      ValueLayout.JAVA_LONG_UNALIGNED.withOrder(ByteOrder.BIG_ENDIAN);

  private Memory() {}

  private static boolean little(ByteOrder order) {
    return order == ByteOrder.LITTLE_ENDIAN;
  }

  private static short order(ByteOrder order, short value) {
    return little(order) ? value : Short.reverseBytes(value);
  }

  private static int order(ByteOrder order, int value) {
    return little(order) ? value : Integer.reverseBytes(value);
  }

  private static long order(ByteOrder order, long value) {
    return little(order) ? value : Long.reverseBytes(value);
  }

  static byte getByte(MemorySegment s, long offset) {
    return IMAGE ? ImageMemory.getByte(s.address(), offset) : s.get(ValueLayout.JAVA_BYTE, offset);
  }

  static void putByte(MemorySegment s, long offset, byte value) {
    if (IMAGE) ImageMemory.putByte(s.address(), offset, value);
    else s.set(ValueLayout.JAVA_BYTE, offset, value);
  }

  /** {@link #putByte(MemorySegment, long, byte)} for a caller that already holds the address. */
  static void putByte(long address, MemorySegment s, long offset, byte value) {
    if (IMAGE) ImageMemory.putByte(address, offset, value);
    else s.set(ValueLayout.JAVA_BYTE, offset, value);
  }

  /** A little-endian {@code long} for a caller that already holds the address. */
  static void putLong(long address, MemorySegment s, long offset, long value) {
    if (IMAGE) ImageMemory.putLong(address, offset, value);
    else s.set(LONG_LE, offset, value);
  }

  /** The pointer-sized word at {@code address}, which the caller vouches for. */
  static long getAddress(long address) {
    return IMAGE ? ImageMemory.getLong(address, 0) : hostAddress(address);
  }

  /** HotSpot reads through a segment, which runtime compilation must not reach. */
  @TruffleBoundary
  private static long hostAddress(long address) {
    return MemorySegment.ofAddress(address).reinterpret(8).get(LONG_LE, 0);
  }

  static short getShort(MemorySegment s, ByteOrder order, long offset) {
    return IMAGE
        ? order(order, ImageMemory.getShort(s.address(), offset))
        : s.get(little(order) ? ValueLayout.JAVA_SHORT_UNALIGNED : SHORT_BE, offset);
  }

  static void putShort(MemorySegment s, ByteOrder order, long offset, short value) {
    if (IMAGE) ImageMemory.putShort(s.address(), offset, order(order, value));
    else s.set(little(order) ? ValueLayout.JAVA_SHORT_UNALIGNED : SHORT_BE, offset, value);
  }

  static int getInt(MemorySegment s, ByteOrder order, long offset) {
    return IMAGE
        ? order(order, ImageMemory.getInt(s.address(), offset))
        : s.get(little(order) ? ValueLayout.JAVA_INT_UNALIGNED : INT_BE, offset);
  }

  static void putInt(MemorySegment s, ByteOrder order, long offset, int value) {
    if (IMAGE) ImageMemory.putInt(s.address(), offset, order(order, value));
    else s.set(little(order) ? ValueLayout.JAVA_INT_UNALIGNED : INT_BE, offset, value);
  }

  static long getLong(MemorySegment s, ByteOrder order, long offset) {
    return IMAGE
        ? order(order, ImageMemory.getLong(s.address(), offset))
        : s.get(little(order) ? ValueLayout.JAVA_LONG_UNALIGNED : LONG_BE, offset);
  }

  static void putLong(MemorySegment s, ByteOrder order, long offset, long value) {
    if (IMAGE) ImageMemory.putLong(s.address(), offset, order(order, value));
    else s.set(little(order) ? ValueLayout.JAVA_LONG_UNALIGNED : LONG_BE, offset, value);
  }

  static float getFloat(MemorySegment s, ByteOrder order, long offset) {
    return Float.intBitsToFloat(getInt(s, order, offset));
  }

  static void putFloat(MemorySegment s, ByteOrder order, long offset, float value) {
    putInt(s, order, offset, Float.floatToRawIntBits(value));
  }

  static double getDouble(MemorySegment s, ByteOrder order, long offset) {
    return Double.longBitsToDouble(getLong(s, order, offset));
  }

  static void putDouble(MemorySegment s, ByteOrder order, long offset, double value) {
    putLong(s, order, offset, Double.doubleToRawLongBits(value));
  }
}
