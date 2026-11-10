// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package dev.elide.myna.truffle;

import com.oracle.truffle.api.interop.ExceptionType;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.nodes.Node;

/**
 * A resource create reported, through a result its seam does not list as success, that no resource
 * exists. Host code reads the result with {@link #status()}; guest code catches an ordinary runtime
 * error whose {@code status} member is that number.
 *
 * <p>Exporting the member means exporting the exception messages too: an explicit export replaces
 * the defaults every other {@link NativeCellException} keeps.
 */
@ExportLibrary(InteropLibrary.class)
public final class NativeStatusException extends NativeCellException {
  private static final long serialVersionUID = 1L;
  private static final String STATUS = "status";

  private final long status;

  NativeStatusException(String message, Node location, long status) {
    super(message, location);
    this.status = status;
  }

  /** The native result of the failed create. */
  public long status() {
    return status;
  }

  @ExportMessage
  boolean isException() {
    return true;
  }

  @ExportMessage
  RuntimeException throwException() {
    throw this;
  }

  @ExportMessage
  ExceptionType getExceptionType() {
    return ExceptionType.RUNTIME_ERROR;
  }

  @ExportMessage
  boolean hasExceptionMessage() {
    return true;
  }

  @ExportMessage
  Object getExceptionMessage() {
    return getMessage();
  }

  @ExportMessage
  boolean hasMembers() {
    return true;
  }

  @ExportMessage
  Object getMembers(@SuppressWarnings("unused") boolean includeInternal) {
    return StatusMembers.INSTANCE;
  }

  @ExportMessage
  boolean isMemberReadable(String member) {
    return STATUS.equals(member);
  }

  @ExportMessage
  Object readMember(String member) throws UnknownIdentifierException {
    if (!STATUS.equals(member)) throw UnknownIdentifierException.create(member);
    return status;
  }

  /** The member names of every status error. */
  @ExportLibrary(InteropLibrary.class)
  static final class StatusMembers implements TruffleObject {
    static final StatusMembers INSTANCE = new StatusMembers();

    @ExportMessage
    boolean hasArrayElements() {
      return true;
    }

    @ExportMessage
    long getArraySize() {
      return 1;
    }

    @ExportMessage
    boolean isArrayElementReadable(long index) {
      return index == 0;
    }

    @ExportMessage
    Object readArrayElement(long index) throws InvalidArrayIndexException {
      if (index != 0) throw InvalidArrayIndexException.create(index);
      return STATUS;
    }
  }
}
