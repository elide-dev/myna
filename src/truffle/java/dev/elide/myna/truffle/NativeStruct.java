// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package dev.elide.myna.truffle;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidBufferOffsetException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.nodes.Node;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteOrder;

/**
 * The struct a resource create writes through, in memory the runtime owns. The guest passes a
 * buffer for that parameter, as its C caller would pass a struct, but the native side never sees
 * the guest's memory: it writes here, the cell reads the resource from here, and the guest's buffer
 * receives a copy.
 *
 * <p>That is what keeps guest code from breaking ownership. The handle cannot be replaced between
 * the create writing it and the cell adopting it; a buffer too small for the struct is refused
 * instead of overrun; and a resource initialized in place lives in this memory, not the guest's, so
 * the guest can neither initialize one struct twice nor free or overwrite a live resource.
 *
 * <p>The memory is zeroed, and it is freed by the collector once nothing reaches it: after the call
 * for a create that writes a handle field, and after its destroy for a resource initialized in
 * place, whose owner keeps this object until then. Everything here is behind a boundary: a create
 * is not a path worth compiling.
 */
public final class NativeStruct {
  private static final ValueLayout.OfLong WORD =
      ValueLayout.JAVA_LONG_UNALIGNED.withOrder(ByteOrder.LITTLE_ENDIAN);

  private final MemorySegment segment;
  private final long address;

  private NativeStruct(MemorySegment segment) {
    this.segment = segment;
    this.address = segment.address();
  }

  /**
   * Allocates {@code size} zeroed bytes for the struct, after checking that {@code guest} can take
   * a copy of it: a writable buffer of at least that many bytes.
   *
   * @throws NativeCellException if {@code guest} is anything else, including a resource's owner
   */
  @TruffleBoundary
  public static NativeStruct allocate(
      long size, Object guest, String function, String parameter, Node node) {
    if (!takes(guest, size))
      throw NativeCellException.type(
          function, parameter, "writable buffer of at least " + size + " bytes", guest, node);
    return new NativeStruct(Arena.ofAuto().allocate(size, 16));
  }

  private static boolean takes(Object guest, long size) {
    if (guest instanceof NativeBytes bytes) return bytes.isWritable() && bytes.size() >= size;
    if (guest == null || guest instanceof NativeResource) return false;
    InteropLibrary interop = InteropLibrary.getUncached(guest);
    try {
      return interop.hasBufferElements(guest)
          && interop.isBufferWritable(guest)
          && interop.getBufferSize(guest) >= size;
    } catch (UnsupportedMessageException e) {
      return false;
    }
  }

  /** Where the native side writes the struct. */
  public long address() {
    return address;
  }

  /** The pointer the create wrote at {@code offset}: the resource's handle. */
  @TruffleBoundary
  public long pointerAt(long offset) {
    return segment.get(WORD, offset);
  }

  /**
   * Copies the struct to the guest's buffer, so the guest reads its fields as a C caller would.
   *
   * @throws NativeCellException if the buffer no longer takes the bytes: guest code shrank or
   *     detached it during the call
   */
  @TruffleBoundary
  public void copyTo(Object guest) {
    long size = segment.byteSize();
    if (guest instanceof NativeBytes bytes) {
      MemorySegment.copy(segment, 0, bytes.segment(), 0, size);
      return;
    }
    InteropLibrary interop = InteropLibrary.getUncached(guest);
    try {
      for (long i = 0; i < size; i++)
        interop.writeBufferByte(guest, i, segment.get(ValueLayout.JAVA_BYTE, i));
    } catch (InvalidBufferOffsetException | UnsupportedMessageException e) {
      throw NativeCellException.of(null, "buffer stopped taking the struct a create wrote");
    }
  }

  /**
   * Copies the struct to the guest's buffer for a create that succeeded. If the buffer no longer
   * takes it, the new resource is released before the error is reported: nothing is left that the
   * guest holds no owner for.
   */
  @TruffleBoundary
  public void copyTo(Object guest, NativeResource created) {
    try {
      copyTo(guest);
    } catch (RuntimeException failure) {
      created.close();
      throw failure;
    }
  }
}
