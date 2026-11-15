// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package dev.elide.myna.truffle;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

/**
 * A seam's named constants as a guest-visible object: one member per {@code const} (its number) and
 * one per enum or flag set (a nested {@code NativeConstants} whose members are the values, or the
 * masks of the bits). A generated {@code <Class>Truffle.constants()} returns one, built when its
 * class initializes (at image build time in a Native Image).
 *
 * <p>Numbers are what a cell returning the carrier gives a guest, boxed once when the object is
 * built: an {@code Integer} up to {@code i32}/{@code u16}, a {@code Long} holding the number for
 * {@code u32} and the signed 64-bit carriers, and raw {@code long} bits for {@code u64}, so {@code
 * u64} values above {@code Long.MAX_VALUE} are negative exactly as cell results are. Names and
 * values sit in {@link CompilationFinal} arrays, and {@code readMember} caches on the receiver's
 * identity and the member string's equality (identity first, so a constant name skips the compare),
 * so a compiled guest read of {@code constants.Status.ok} with a constant member name folds to the
 * number; past the cache limit a {@link TruffleBoundary} lookup answers.
 */
@ExportLibrary(InteropLibrary.class)
public final class NativeConstants implements TruffleObject {
  @CompilationFinal(dimensions = 1)
  private final String[] names;

  @CompilationFinal(dimensions = 1)
  private final Object[] values;

  private final MemberNames members;

  /**
   * MemberNames in the given order; each value is a {@link Byte}, {@link Short}, {@link Integer},
   * {@link Long} or a nested {@code NativeConstants}.
   */
  public NativeConstants(String[] names, Object[] values) {
    if (names.length != values.length)
      throw new IllegalArgumentException("names and values differ in length");
    for (int i = 0; i < names.length; i++) {
      if (names[i] == null) throw new IllegalArgumentException("null member name");
      for (int j = 0; j < i; j++)
        if (names[j].equals(names[i]))
          throw new IllegalArgumentException("duplicate member: " + names[i]);
      Object v = values[i];
      if (!(v instanceof Byte
          || v instanceof Short
          || v instanceof Integer
          || v instanceof Long
          || v instanceof NativeConstants))
        throw new IllegalArgumentException("member " + names[i] + " is not a constant: " + v);
    }
    this.names = names.clone();
    this.values = values.clone();
    this.members = new MemberNames(this.names);
  }

  /** The index of a member, or -1. */
  @TruffleBoundary
  int indexOf(String member) {
    for (int i = 0; i < names.length; i++) if (names[i].equals(member)) return i;
    return -1;
  }

  @TruffleBoundary
  Object lookup(String member) throws UnknownIdentifierException {
    int index = indexOf(member);
    if (index < 0) throw UnknownIdentifierException.create(member);
    return values[index];
  }

  @ExportMessage
  boolean hasMembers() {
    return true;
  }

  @ExportMessage
  Object getMembers(@SuppressWarnings("unused") boolean includeInternal) {
    return members;
  }

  @ExportMessage
  static final class IsMemberReadable {
    @Specialization(
        guards = {
          "receiver == cachedReceiver",
          "member == cachedMember || cachedMember.equals(member)"
        },
        limit = "4")
    static boolean cached(
        @SuppressWarnings("unused") NativeConstants receiver,
        @SuppressWarnings("unused") String member,
        @Cached("receiver") @SuppressWarnings("unused") NativeConstants cachedReceiver,
        @Cached("member") @SuppressWarnings("unused") String cachedMember,
        @Cached("cachedReceiver.indexOf(cachedMember) >= 0") boolean readable) {
      return readable;
    }

    @Specialization(replaces = "cached")
    static boolean uncached(NativeConstants receiver, String member) {
      return receiver.indexOf(member) >= 0;
    }
  }

  @ExportMessage
  static final class ReadMember {
    @Specialization(
        guards = {
          "receiver == cachedReceiver",
          "member == cachedMember || cachedMember.equals(member)"
        },
        limit = "4")
    static Object cached(
        @SuppressWarnings("unused") NativeConstants receiver,
        @SuppressWarnings("unused") String member,
        @Cached("receiver") NativeConstants cachedReceiver,
        @Cached("member") String cachedMember,
        @Cached("cachedReceiver.indexOf(cachedMember)") int index)
        throws UnknownIdentifierException {
      // A cached unknown member still allocates its exception on every read.
      if (index < 0) throw UnknownIdentifierException.create(cachedMember);
      return cachedReceiver.values[index];
    }

    @Specialization(replaces = "cached")
    static Object uncached(NativeConstants receiver, String member)
        throws UnknownIdentifierException {
      return receiver.lookup(member);
    }
  }

  /** The member names, as an interop array of strings. */
  @ExportLibrary(InteropLibrary.class)
  static final class MemberNames implements TruffleObject {
    @CompilationFinal(dimensions = 1)
    private final String[] names;

    MemberNames(String[] names) {
      this.names = names;
    }

    @ExportMessage
    boolean hasArrayElements() {
      return true;
    }

    @ExportMessage
    long getArraySize() {
      return names.length;
    }

    @ExportMessage
    boolean isArrayElementReadable(long index) {
      return index >= 0 && index < names.length;
    }

    @ExportMessage
    Object readArrayElement(long index) throws InvalidArrayIndexException {
      if (index < 0 || index >= names.length) throw InvalidArrayIndexException.create(index);
      return names[(int) index];
    }
  }
}
