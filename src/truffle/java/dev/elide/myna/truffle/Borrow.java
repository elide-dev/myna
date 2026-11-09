// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package dev.elide.myna.truffle;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidBufferOffsetException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.charset.StandardCharsets;
import org.graalvm.nativeimage.ImageInfo;
import org.graalvm.nativeimage.PinnedObject;

/**
 * Native bytes lent to one call: the address and byte length a cell passes, plus whatever must be
 * undone when the call returns. The dynamic extent is exactly Myna's {@code lifetime=call} borrow.
 *
 * <ul>
 *   <li>{@link NativeBytes} and other interop pointers lend their address: no copy, no pin.
 *   <li>Heap arrays ({@link HeapBytes}) are pinned in a Native Image for the call when they reach
 *       {@link #pinThreshold()} bytes, otherwise copied into temporary native memory and, for
 *       writable borrows, copied back on release. HotSpot always copies.
 *   <li>Other interop buffers are read through {@link InteropLibrary} into a copy and written back
 *       element by element when writable.
 *   <li>Strings are encoded as UTF-8, NUL-terminated when the parameter is a C string.
 * </ul>
 *
 * <p>A pinned array does not move, so its address stays valid across any safepoint between the pin
 * and the call; an unpinned heap address is never handed to native code.
 */
public final class Borrow {
  /**
   * Heap arrays at least this long are pinned rather than copied in a Native Image. {@code
   * benchmarks/cells} measured pinning as a fixed cost (about 15 ns over lending native memory)
   * while copying adds about 0.04 ns per byte on top of a similar fixed cost, so the two are within
   * a few nanoseconds below 256 bytes and copying loses clearly above it; small arrays are copied
   * so that short-lived garbage is not pinned in the collector's chunks. {@code
   * -Dmyna.truffle.pinThreshold} overrides it. The value lives in a run-time-initialized holder so
   * the build host's setting is never baked into an image while {@code Borrow} itself initializes
   * at build time and stays inlineable by partial evaluation.
   */
  public static int pinThreshold() {
    return Threshold.VALUE;
  }

  private static final class Threshold {
    static final int VALUE = Integer.getInteger("myna.truffle.pinThreshold", 256);
  }

  private static final Borrow NONE = new Borrow(0, 0, null, null, null, null, false, null);

  private final long address;
  private final long length;
  private final Arena arena;
  private final MemorySegment copy;
  private final byte[] array;
  private final Object interopSource;
  private final boolean writeBack;
  private final Object pin;
  private boolean released;

  private Borrow(
      long address,
      long length,
      Arena arena,
      MemorySegment copy,
      byte[] array,
      Object interopSource,
      boolean writeBack,
      Object pin) {
    this.address = address;
    this.length = length;
    this.arena = arena;
    this.copy = copy;
    this.array = array;
    this.interopSource = interopSource;
    this.writeBack = writeBack;
    this.pin = pin;
  }

  /** The null borrow for a nullable parameter. */
  public static Borrow none() {
    return NONE;
  }

  /** Lends existing native memory for the call. */
  public static Borrow direct(long address, long length) {
    return new Borrow(address, length, null, null, null, null, false, null);
  }

  /** Lends a heap array: pinned when large and in a Native Image, otherwise copied. */
  public static Borrow ofArray(byte[] array, boolean writable, boolean nulTerminated) {
    if (!nulTerminated && array.length >= pinThreshold() && ImageInfo.inImageRuntimeCode())
      return Pins.pin(array);
    return copyOf(array, null, writable, nulTerminated);
  }

  /** Lends UTF-8 text, NUL-terminated for C strings. */
  @TruffleBoundary
  public static Borrow ofString(String text, boolean nulTerminated) {
    return copyOf(text.getBytes(StandardCharsets.UTF_8), null, false, nulTerminated);
  }

  /**
   * Copies any interop buffer, optionally NUL-terminated; writable borrows are written back through
   * the same library. The terminator reserves one extra byte and writes a {@code 0} past the
   * content; {@link #length()} still excludes it, and {@link #release()} only writes back {@code
   * length} bytes, so the terminator never reaches the source buffer.
   */
  @TruffleBoundary
  public static Borrow ofBuffer(
      InteropLibrary interop, Object buffer, boolean writable, boolean nulTerminated)
      throws UnsupportedMessageException {
    long size = interop.getBufferSize(buffer);
    // A guest buffer's size is untrusted: a negative one must not reach the array allocation.
    if (size < 0) throw new IllegalArgumentException("negative interop buffer size: " + size);
    if (size > Integer.MAX_VALUE - 8)
      throw new IllegalArgumentException("interop buffer too large to copy: " + size);
    if (writable && !interop.isBufferWritable(buffer))
      throw new IllegalArgumentException("read-only buffer for a writable borrow");
    byte[] bytes = new byte[(int) size];
    try {
      interop.readBuffer(buffer, 0, bytes, 0, bytes.length);
    } catch (InvalidBufferOffsetException e) {
      throw new IllegalArgumentException("buffer shrank while being borrowed", e);
    }
    return copyOf(bytes, buffer, writable, nulTerminated);
  }

  @TruffleBoundary
  private static Borrow copyOf(
      byte[] bytes, Object source, boolean writable, boolean nulTerminated) {
    long size = (long) bytes.length + (nulTerminated ? 1 : 0);
    Arena arena = Arena.ofConfined();
    MemorySegment segment = arena.allocate(Math.max(size, 1));
    MemorySegment.copy(bytes, 0, segment, ValueLayout.JAVA_BYTE, 0, bytes.length);
    if (nulTerminated) segment.set(ValueLayout.JAVA_BYTE, bytes.length, (byte) 0);
    return new Borrow(
        segment.address(), bytes.length, arena, segment, bytes, source, writable, null);
  }

  public long address() {
    return address;
  }

  /** The borrowed byte count, excluding any NUL terminator. */
  public long length() {
    return length;
  }

  /** Ends the borrow: writes copies back, unpins, frees. Idempotent; never throws for direct. */
  public void release() {
    if (released) return;
    released = true;
    if (pin != null) {
      Pins.unpin(pin);
      return;
    }
    if (arena != null) releaseCopy();
  }

  @TruffleBoundary
  private void releaseCopy() {
    try {
      if (writeBack) {
        MemorySegment.copy(copy, ValueLayout.JAVA_BYTE, 0, array, 0, array.length);
        if (interopSource != null) writeBackInterop();
      }
    } finally {
      arena.close();
    }
  }

  /**
   * Guest code can shrink or detach the source buffer while the call runs (from a callback or
   * another thread). Bytes that no longer exist, or a buffer that no longer accepts writes, cannot
   * be written back: that is a guest error, reported after every surviving byte has been written,
   * not an internal one.
   */
  private void writeBackInterop() {
    InteropLibrary interop = InteropLibrary.getUncached(interopSource);
    try {
      for (int i = 0; i < array.length; i++) interop.writeBufferByte(interopSource, i, array[i]);
    } catch (InvalidBufferOffsetException e) {
      throw NativeCellException.of(
          null, "borrowed buffer shrank before write-back of byte ", e.getByteOffset());
    } catch (UnsupportedMessageException e) {
      throw NativeCellException.of(
          null, "borrowed buffer stopped accepting writes before write-back");
    }
  }

  /** Native Image pinning; loaded only inside an image, where {@link PinnedObject} exists. */
  private static final class Pins {
    static Borrow pin(byte[] array) {
      PinnedObject pinned = PinnedObject.create(array);
      long address = pinned.addressOfArrayElement(0).rawValue();
      return new Borrow(address, array.length, null, null, array, null, false, pinned);
    }

    static void unpin(Object pin) {
      ((PinnedObject) pin).close();
    }
  }
}
