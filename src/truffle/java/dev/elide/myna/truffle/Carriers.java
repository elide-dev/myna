// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package dev.elide.myna.truffle;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.nodes.Node;

/**
 * Result and length conversions used by generated cells. Unsigned carriers leave Myna's bindings as
 * raw bits; guest code receives their numeric value, except {@code u64}, which stays a raw {@code
 * long} because interop has no unsigned 64-bit primitive.
 */
public final class Carriers {
  private Carriers() {}

  public static int u8(byte raw) {
    return raw & 0xff;
  }

  public static int u16(short raw) {
    return raw & 0xffff;
  }

  public static long u32(int raw) {
    return Integer.toUnsignedLong(raw);
  }

  public static Object pointer(long address) {
    return NativePointer.of(address);
  }

  public static Object none() {
    return NativeVoid.INSTANCE;
  }

  /** A borrowed length for a 32-bit length parameter; the declared range is checked first. */
  public static int length32(long length, boolean unsigned, Node node) {
    if (length > (unsigned ? 4294967295L : 2147483647L)) {
      CompilerDirectives.transferToInterpreter();
      throw NativeCellException.of(node, "buffer length exceeds ABI range: ", length);
    }
    return (int) length;
  }
}
