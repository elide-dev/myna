// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package dev.elide.myna.truffle;

import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.strings.AbstractTruffleString;
import com.oracle.truffle.api.strings.InternalByteArray;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.api.strings.TruffleString.CodeRange;
import com.oracle.truffle.api.strings.TruffleString.Encoding;

/**
 * Lends a guest string (a {@link TruffleString}, as JavaScript and Python pass their strings)
 * without making a Java {@code String} of it. The string's UTF-16 code units are read through the
 * public TruffleString API and encoded straight onto the scratch stack.
 *
 * <p>The bytes are exactly those of {@code InteropLibrary.asString(string).getBytes(UTF_8)}, the
 * path every other interop string takes: {@code asString} transcodes to UTF-16 with the default
 * error handler, as {@link TruffleString.SwitchEncodingNode} does here, and the encoder replaces an
 * unpaired surrogate with {@code ?} as {@code String.getBytes} does. Going through UTF-8 instead
 * ({@code SwitchEncodingNode} to {@code UTF_8}) would differ for such strings, would allocate a
 * transcoded copy of every string that is not ASCII, and would keep that copy alive for as long as
 * the string. Copying with {@link TruffleString.CopyToNativeMemoryNode} needs a current context
 * with native access, which a cell cannot assume.
 *
 * <p>What allocates: nothing for a string that is already UTF-16 or ASCII and whose bytes are on
 * the Java heap, which is every JavaScript string and every ASCII string. A string in another
 * encoding with characters outside ASCII is transcoded to UTF-16 once, and TruffleString keeps that
 * copy with the string; a mutable string is copied on every call, since its bytes may change; a
 * string backed by native memory has its bytes copied into an array.
 */
final class GuestText extends Node {
  // One node per use, so each keeps the profile of its own encoding.
  @Child
  private TruffleString.SwitchEncodingNode toUtf16 = TruffleString.SwitchEncodingNode.create();

  @Child
  private TruffleString.GetCodeRangeImpreciseNode range =
      TruffleString.GetCodeRangeImpreciseNode.create();

  @Child
  private TruffleString.SwitchEncodingNode toLatin1 = TruffleString.SwitchEncodingNode.create();

  @Child
  private TruffleString.GetInternalByteArrayNode narrow =
      TruffleString.GetInternalByteArrayNode.create();

  @Child
  private TruffleString.GetInternalByteArrayNode wide =
      TruffleString.GetInternalByteArrayNode.create();

  /**
   * Pushes the UTF-8 bytes of {@code string}, followed by a NUL, onto the current thread's scratch
   * stack, for {@link Borrow#lendPushed} to own. The borrow itself stays with the caller.
   *
   * @return the stack, or {@code null}, with nothing pushed, when the bytes are too long for it or
   *     do not fit beside outstanding copies
   */
  Borrow.Scratch push(AbstractTruffleString string) {
    // The string itself when it is UTF-16 already or all ASCII; an immutable copy otherwise.
    TruffleString utf16 = toUtf16.execute(string, Encoding.UTF_16);
    // The range as the string knows it, without a scan: it may be wider than the text, never
    // narrower, so a string that reports Latin-1 or ASCII holds one byte per code unit.
    CodeRange known = range.execute(utf16, Encoding.UTF_16);
    if (known == CodeRange.ASCII || known == CodeRange.LATIN_1) {
      // Switching first is what makes the bytes Latin-1: a no-op for a string on the Java heap,
      // a compacted copy for a native one that stores two bytes per unit.
      TruffleString latin1 = toLatin1.execute(utf16, Encoding.ISO_8859_1);
      InternalByteArray units = narrow.execute(latin1, Encoding.ISO_8859_1);
      return Borrow.Scratch.pushUnits(
          units.getArray(), units.getOffset(), units.getLength(), Borrow.LATIN_1);
    }
    InternalByteArray units = wide.execute(utf16, Encoding.UTF_16);
    return Borrow.Scratch.pushUnits(
        units.getArray(), units.getOffset(), units.getLength(), Borrow.UTF_16);
  }
}
