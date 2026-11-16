// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package dev.elide.myna.truffle;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.strings.AbstractTruffleString;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import org.graalvm.nativeimage.ImageInfo;

/**
 * What an {@code access=cstring} parameter's cell does around its borrow. It takes strings only: a
 * Java {@code String}, a {@code TruffleString}, or any interop string, converted with {@code
 * asTruffleString}. Once the string is lent NUL-terminated, the encoded bytes are scanned for an
 * embedded NUL, which would end the string early on the C side: no Java string is made, and the
 * scan reads the bytes the borrow just wrote.
 */
public final class CStrings {
  private CStrings() {}

  private static final boolean IMAGE = ImageInfo.inImageCode();

  /**
   * Returns {@code value} as a string the coercion lends, or throws a guest error for anything that
   * is not a string. A nullable parameter also takes interop {@code null}.
   */
  public static Object strings(
      Object value, boolean nullable, String function, String parameter, Node node) {
    if (value instanceof AbstractTruffleString || value instanceof String) return value;
    return foreign(value, nullable, function, parameter, node);
  }

  @TruffleBoundary
  private static Object foreign(
      Object value, boolean nullable, String function, String parameter, Node node) {
    if (value == null) {
      if (nullable) return null;
    } else {
      InteropLibrary interop = InteropLibrary.getUncached(value);
      if (nullable && interop.isNull(value)) return value;
      if (interop.isString(value))
        try {
          return interop.asTruffleString(value);
        } catch (UnsupportedMessageException e) {
          // Not a string after all: reported below.
        }
    }
    throw NativeCellException.type(function, parameter, "string", value, node);
  }

  /** Refuses a lent string whose encoded bytes hold a NUL before its terminator. */
  public static void checkLent(Borrow borrow, String function, String parameter, Node node) {
    long address = borrow.address();
    long length = borrow.length();
    if (address == 0 || length == 0) return;
    if (IMAGE) {
      for (long i = 0; i < length; i++)
        if (ImageMemory.getByte(address, i) == 0) throw embeddedNul(function, parameter, node);
      return;
    }
    MemorySegment bytes = MemorySegment.ofAddress(address).reinterpret(length);
    for (long i = 0; i < length; i++)
      if (bytes.get(ValueLayout.JAVA_BYTE, i) == 0) throw embeddedNul(function, parameter, node);
  }

  private static NativeCellException embeddedNul(String function, String parameter, Node node) {
    CompilerDirectives.transferToInterpreter();
    return message(function, parameter, node);
  }

  @TruffleBoundary
  private static NativeCellException message(String function, String parameter, Node node) {
    return NativeCellException.of(node, function, " ", parameter, ": embedded NUL in a C string");
  }
}
