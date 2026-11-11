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
import org.graalvm.nativeimage.ImageInfo;

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
 * <p>The memory is zeroed and freed explicitly, exactly once: by the cell once the create has
 * returned for a struct that holds a handle field ({@link #allocate}), and by the resource's
 * destroy, or the cell when the create fails, for a resource initialized in place ({@link
 * #allocateInPlace}). Nothing registers it with a cleaner: a resource the guest forgets is never
 * destroyed, and the native side may still reach the struct it lives in, so its memory is kept with
 * it. In a Native Image it is {@code calloc}ed; on HotSpot it is an arena of its own, confined to
 * the creating thread for a struct the cell frees and shared for one a resource frees, since a
 * shared resource may be destroyed on any thread.
 */
public final class NativeStruct {
  private static final boolean IMAGE = ImageInfo.inImageCode();
  private static final ValueLayout.OfLong WORD =
      ValueLayout.JAVA_LONG_UNALIGNED.withOrder(ByteOrder.LITTLE_ENDIAN);

  private final long address;
  private final long size;

  /** The arena on HotSpot; {@code null} in a Native Image. */
  private final Arena arena;

  /** The struct's bytes on HotSpot; {@code null} in a Native Image, which reads words directly. */
  private final MemorySegment segment;

  private boolean freed;

  private NativeStruct(long address, long size, Arena arena, MemorySegment segment) {
    this.address = address;
    this.size = size;
    this.arena = arena;
    this.segment = segment;
  }

  /**
   * Allocates {@code size} zeroed bytes for a struct the cell frees with {@link #free()} once the
   * create has returned, after checking that {@code guest} can take a copy of it: a writable buffer
   * of at least that many bytes.
   *
   * @throws NativeCellException if {@code guest} is anything else, including a resource's owner
   */
  @TruffleBoundary
  public static NativeStruct allocate(
      long size, Object guest, String function, String parameter, Node node) {
    return allocate(size, guest, function, parameter, node, false);
  }

  /**
   * Allocates a struct that a resource is initialized in place in: the resource that keeps it frees
   * it when it is destroyed, on whichever thread that happens, and the cell frees it if the create
   * fails.
   */
  @TruffleBoundary
  public static NativeStruct allocateInPlace(
      long size, Object guest, String function, String parameter, Node node) {
    return allocate(size, guest, function, parameter, node, true);
  }

  private static NativeStruct allocate(
      long size, Object guest, String function, String parameter, Node node, boolean shared) {
    if (!takes(guest, size))
      throw NativeCellException.type(
          function, parameter, "writable buffer of at least " + size + " bytes", guest, node);
    if (IMAGE) return new NativeStruct(Borrow.Malloc.zeroed(size), size, null, null);
    Arena arena = shared ? Arena.ofShared() : Arena.ofConfined();
    MemorySegment segment = arena.allocate(size, 16);
    return new NativeStruct(segment.address(), size, arena, segment);
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
  public long pointerAt(long offset) {
    return word(offset);
  }

  private long word(long offset) {
    return IMAGE ? ImageMemory.getLong(address, offset) : segment.get(WORD, offset);
  }

  private byte byteAt(long offset) {
    return IMAGE
        ? ImageMemory.getByte(address, offset)
        : segment.get(ValueLayout.JAVA_BYTE, offset);
  }

  /** Frees the struct; a second call does nothing. */
  @TruffleBoundary
  public void free() {
    if (freed) return;
    freed = true;
    if (arena == null) Borrow.Malloc.free(address);
    else arena.close();
  }

  /**
   * Copies the struct to the guest's buffer, so the guest reads its fields as a C caller would.
   *
   * @throws NativeCellException if the buffer no longer takes the bytes: guest code shrank or
   *     detached it during the call
   */
  @TruffleBoundary
  public void copyTo(Object guest) {
    if (guest instanceof NativeBytes bytes) {
      // Its size was checked when the struct was allocated, and a NativeBytes does not change
      // size; its memory must still be alive, as for any lent NativeBytes.
      MemorySegment target = bytes.segment();
      if (!target.scope().isAlive())
        throw NativeCellException.of(null, "buffer stopped taking the struct a create wrote");
      long to = bytes.address();
      long i = 0;
      for (; i + 8 <= size; i += 8) Memory.putLong(to, target, i, word(i));
      for (; i < size; i++) Memory.putByte(to, target, i, byteAt(i));
      return;
    }
    InteropLibrary interop = InteropLibrary.getUncached(guest);
    try {
      for (long i = 0; i < size; i++) interop.writeBufferByte(guest, i, byteAt(i));
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
