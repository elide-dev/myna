// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package dev.elide.myna.truffle;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidBufferOffsetException;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteOrder;

/**
 * A zero-copy guest buffer over native memory: an interop buffer and an interop pointer at once.
 * Cells lend its address directly. The caller keeps the memory alive for as long as the object is
 * reachable from guest code; this object does not own or free it.
 */
@ExportLibrary(InteropLibrary.class)
public final class NativeBytes implements TruffleObject {
  static {
    if (ByteOrder.nativeOrder() != ByteOrder.LITTLE_ENDIAN)
      throw new ExceptionInInitializerError("Myna targets are little-endian");
  }

  private final MemorySegment segment;
  private final boolean writable;

  // Read once: compiled code lends these without a call through the segment interface.
  private final long address;
  private final long size;

  private NativeBytes(MemorySegment segment, boolean writable) {
    this.segment = segment;
    this.writable = writable;
    this.address = segment.address();
    this.size = segment.byteSize();
  }

  /** Views {@code size} bytes at {@code address}; the caller vouches for the extent. */
  public static NativeBytes of(long address, long size, boolean writable) {
    if (address == 0 && size != 0) throw new IllegalArgumentException("null buffer address");
    if (size < 0) throw new IllegalArgumentException("negative buffer size");
    return new NativeBytes(MemorySegment.ofAddress(address).reinterpret(size), writable);
  }

  /** Views a native segment; its scope governs validity. Heap segments are rejected. */
  public static NativeBytes of(MemorySegment segment, boolean writable) {
    if (!segment.isNative()) throw new IllegalArgumentException("native segment required");
    return new NativeBytes(segment, writable && !segment.isReadOnly());
  }

  public long address() {
    return address;
  }

  public long size() {
    return size;
  }

  public boolean isWritable() {
    return writable;
  }

  public MemorySegment segment() {
    return segment;
  }

  @ExportMessage
  boolean isPointer() {
    return true;
  }

  @ExportMessage
  long asPointer() {
    return address;
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
    return size;
  }

  private void check(long offset, int width) throws InvalidBufferOffsetException {
    if (offset < 0 || offset > size - width)
      throw InvalidBufferOffsetException.create(offset, width);
  }

  private void checkWrite(long offset, int width)
      throws InvalidBufferOffsetException, UnsupportedMessageException {
    if (!writable) throw UnsupportedMessageException.create();
    check(offset, width);
  }

  @ExportMessage
  byte readBufferByte(long offset) throws InvalidBufferOffsetException {
    check(offset, 1);
    return Memory.getByte(segment, offset);
  }

  @ExportMessage
  void writeBufferByte(long offset, byte value)
      throws InvalidBufferOffsetException, UnsupportedMessageException {
    checkWrite(offset, 1);
    Memory.putByte(segment, offset, value);
  }

  @ExportMessage
  @TruffleBoundary
  void readBuffer(long offset, byte[] destination, int destinationOffset, int length)
      throws InvalidBufferOffsetException {
    if (length < 0) throw InvalidBufferOffsetException.create(offset, length);
    check(offset, 0);
    if (offset > size - length) throw InvalidBufferOffsetException.create(offset, length);
    MemorySegment.copy(
        segment, ValueLayout.JAVA_BYTE, offset, destination, destinationOffset, length);
  }

  @ExportMessage
  short readBufferShort(ByteOrder order, long offset) throws InvalidBufferOffsetException {
    check(offset, 2);
    return Memory.getShort(segment, order, offset);
  }

  @ExportMessage
  void writeBufferShort(ByteOrder order, long offset, short value)
      throws InvalidBufferOffsetException, UnsupportedMessageException {
    checkWrite(offset, 2);
    Memory.putShort(segment, order, offset, value);
  }

  @ExportMessage
  int readBufferInt(ByteOrder order, long offset) throws InvalidBufferOffsetException {
    check(offset, 4);
    return Memory.getInt(segment, order, offset);
  }

  @ExportMessage
  void writeBufferInt(ByteOrder order, long offset, int value)
      throws InvalidBufferOffsetException, UnsupportedMessageException {
    checkWrite(offset, 4);
    Memory.putInt(segment, order, offset, value);
  }

  @ExportMessage
  long readBufferLong(ByteOrder order, long offset) throws InvalidBufferOffsetException {
    check(offset, 8);
    return Memory.getLong(segment, order, offset);
  }

  @ExportMessage
  void writeBufferLong(ByteOrder order, long offset, long value)
      throws InvalidBufferOffsetException, UnsupportedMessageException {
    checkWrite(offset, 8);
    Memory.putLong(segment, order, offset, value);
  }

  @ExportMessage
  float readBufferFloat(ByteOrder order, long offset) throws InvalidBufferOffsetException {
    check(offset, 4);
    return Memory.getFloat(segment, order, offset);
  }

  @ExportMessage
  void writeBufferFloat(ByteOrder order, long offset, float value)
      throws InvalidBufferOffsetException, UnsupportedMessageException {
    checkWrite(offset, 4);
    Memory.putFloat(segment, order, offset, value);
  }

  @ExportMessage
  double readBufferDouble(ByteOrder order, long offset) throws InvalidBufferOffsetException {
    check(offset, 8);
    return Memory.getDouble(segment, order, offset);
  }

  @ExportMessage
  void writeBufferDouble(ByteOrder order, long offset, double value)
      throws InvalidBufferOffsetException, UnsupportedMessageException {
    checkWrite(offset, 8);
    Memory.putDouble(segment, order, offset, value);
  }

  @Override
  public String toString() {
    return "NativeBytes(0x" + Long.toHexString(segment.address()) + ", " + segment.byteSize() + ")";
  }
}
