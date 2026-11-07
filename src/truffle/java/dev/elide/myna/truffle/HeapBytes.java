// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package dev.elide.myna.truffle;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidBufferOffsetException;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.memory.ByteArraySupport;
import java.nio.ByteOrder;

/**
 * A guest buffer over a Java {@code byte[]}, for languages without native memory. Cells pin it for
 * the call in a Native Image when it is large, and copy it otherwise; see {@link Borrow}. It is not
 * an interop pointer: a heap address must never reach native code unpinned.
 */
@ExportLibrary(InteropLibrary.class)
public final class HeapBytes implements TruffleObject {
  private final byte[] array;
  private final boolean writable;

  public HeapBytes(byte[] array, boolean writable) {
    this.array = array;
    this.writable = writable;
  }

  public byte[] array() {
    return array;
  }

  public boolean isWritable() {
    return writable;
  }

  private static ByteArraySupport support(ByteOrder order) {
    return order == ByteOrder.LITTLE_ENDIAN
        ? ByteArraySupport.littleEndian()
        : ByteArraySupport.bigEndian();
  }

  private void check(long offset, int width) throws InvalidBufferOffsetException {
    if (offset < 0 || offset > array.length - width)
      throw InvalidBufferOffsetException.create(offset, width);
  }

  private void checkWrite(long offset, int width)
      throws InvalidBufferOffsetException, UnsupportedMessageException {
    if (!writable) throw UnsupportedMessageException.create();
    check(offset, width);
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
    return array.length;
  }

  @ExportMessage
  byte readBufferByte(long offset) throws InvalidBufferOffsetException {
    check(offset, 1);
    return array[(int) offset];
  }

  @ExportMessage
  void writeBufferByte(long offset, byte value)
      throws InvalidBufferOffsetException, UnsupportedMessageException {
    checkWrite(offset, 1);
    array[(int) offset] = value;
  }

  @ExportMessage
  void readBuffer(long offset, byte[] destination, int destinationOffset, int length)
      throws InvalidBufferOffsetException {
    if (length < 0) throw InvalidBufferOffsetException.create(offset, length);
    check(offset, 0);
    if (offset > array.length - length) throw InvalidBufferOffsetException.create(offset, length);
    System.arraycopy(array, (int) offset, destination, destinationOffset, length);
  }

  @ExportMessage
  short readBufferShort(ByteOrder order, long offset) throws InvalidBufferOffsetException {
    check(offset, 2);
    return support(order).getShort(array, offset);
  }

  @ExportMessage
  void writeBufferShort(ByteOrder order, long offset, short value)
      throws InvalidBufferOffsetException, UnsupportedMessageException {
    checkWrite(offset, 2);
    support(order).putShort(array, offset, value);
  }

  @ExportMessage
  int readBufferInt(ByteOrder order, long offset) throws InvalidBufferOffsetException {
    check(offset, 4);
    return support(order).getInt(array, offset);
  }

  @ExportMessage
  void writeBufferInt(ByteOrder order, long offset, int value)
      throws InvalidBufferOffsetException, UnsupportedMessageException {
    checkWrite(offset, 4);
    support(order).putInt(array, offset, value);
  }

  @ExportMessage
  long readBufferLong(ByteOrder order, long offset) throws InvalidBufferOffsetException {
    check(offset, 8);
    return support(order).getLong(array, offset);
  }

  @ExportMessage
  void writeBufferLong(ByteOrder order, long offset, long value)
      throws InvalidBufferOffsetException, UnsupportedMessageException {
    checkWrite(offset, 8);
    support(order).putLong(array, offset, value);
  }

  @ExportMessage
  float readBufferFloat(ByteOrder order, long offset) throws InvalidBufferOffsetException {
    check(offset, 4);
    return support(order).getFloat(array, offset);
  }

  @ExportMessage
  void writeBufferFloat(ByteOrder order, long offset, float value)
      throws InvalidBufferOffsetException, UnsupportedMessageException {
    checkWrite(offset, 4);
    support(order).putFloat(array, offset, value);
  }

  @ExportMessage
  double readBufferDouble(ByteOrder order, long offset) throws InvalidBufferOffsetException {
    check(offset, 8);
    return support(order).getDouble(array, offset);
  }

  @ExportMessage
  void writeBufferDouble(ByteOrder order, long offset, double value)
      throws InvalidBufferOffsetException, UnsupportedMessageException {
    checkWrite(offset, 8);
    support(order).putDouble(array, offset, value);
  }

  @Override
  public String toString() {
    return "HeapBytes(" + array.length + ")";
  }
}
