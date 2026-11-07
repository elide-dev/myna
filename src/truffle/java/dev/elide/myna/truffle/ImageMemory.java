// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package dev.elide.myna.truffle;

import org.graalvm.word.Pointer;
import org.graalvm.word.WordFactory;

/**
 * Native memory access inside a Native Image: word operations, which are compiler intrinsics that
 * partial evaluation compiles. Little-endian, like every Myna target; callers bounds-check first.
 * Only an image executes this class (see {@link Memory}); the Truffle integration test covers it.
 */
final class ImageMemory {
  private ImageMemory() {}

  private static Pointer at(long address, long offset) {
    return WordFactory.pointer(address + offset);
  }

  static byte getByte(long address, long offset) {
    return at(address, offset).readByte(0);
  }

  static void putByte(long address, long offset, byte value) {
    at(address, offset).writeByte(0, value);
  }

  static short getShort(long address, long offset) {
    return at(address, offset).readShort(0);
  }

  static void putShort(long address, long offset, short value) {
    at(address, offset).writeShort(0, value);
  }

  static int getInt(long address, long offset) {
    return at(address, offset).readInt(0);
  }

  static void putInt(long address, long offset, int value) {
    at(address, offset).writeInt(0, value);
  }

  static long getLong(long address, long offset) {
    return at(address, offset).readLong(0);
  }

  static void putLong(long address, long offset, long value) {
    at(address, offset).writeLong(0, value);
  }
}
