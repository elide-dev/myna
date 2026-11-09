// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package dev.elide.myna.truffle;

import com.oracle.truffle.api.CompilerDirectives;
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
 * <p>Copies of up to {@link Scratch#LARGEST} bytes come from the current thread's scratch stack
 * ({@link Scratch}); larger copies, and copies that do not fit beside outstanding ones, get a
 * confined arena of their own. A pinned array does not move, so its address stays valid across any
 * safepoint between the pin and the call; an unpinned heap address is never handed to native code.
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

  private static final Borrow NONE = new Borrow(0, 0, null, null, 0, false, null, null, false);

  // Kept to the 48 bytes of a Borrow without scratch copies: what must be undone is one owner.
  private final long address;
  private final long length;
  private final Object owner;
  private final MemorySegment copy;
  private final byte[] array;
  private final Object interopSource;
  private final int offset;
  private final boolean terminated;
  private final boolean writeBack;
  private boolean released;

  /**
   * @param owner what release undoes: a {@link Scratch} stack, the copy's own {@link Arena}, a
   *     {@link PinnedObject}, or {@code null} for lent native memory
   * @param copy the segment holding a copy at {@code offset}: the scratch block or the arena's
   *     allocation
   * @param terminated whether the copy has a NUL terminator after {@code length} bytes
   */
  private Borrow(
      long address,
      long length,
      Object owner,
      MemorySegment copy,
      int offset,
      boolean terminated,
      byte[] array,
      Object interopSource,
      boolean writeBack) {
    this.address = address;
    this.length = length;
    this.owner = owner;
    this.copy = copy;
    this.offset = offset;
    this.terminated = terminated;
    this.array = array;
    this.interopSource = interopSource;
    this.writeBack = writeBack;
  }

  /** The null borrow for a nullable parameter. */
  public static Borrow none() {
    return NONE;
  }

  /** Lends existing native memory for the call. */
  public static Borrow direct(long address, long length) {
    return new Borrow(address, length, null, null, 0, false, null, null, false);
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

  /**
   * Copies into the current thread's scratch stack when the copy fits, otherwise into an arena of
   * its own. The copy is pushed only once it is complete, so a failure leaves the stack unchanged.
   */
  @TruffleBoundary
  private static Borrow copyOf(
      byte[] bytes, Object source, boolean writable, boolean nulTerminated) {
    long size = (long) bytes.length + (nulTerminated ? 1 : 0);
    if (size <= Scratch.LARGEST) {
      Scratch scratch = Scratches.CURRENT.get();
      MemorySegment block = scratch.reserve(size);
      if (block != null) {
        int start = (int) scratch.top;
        MemorySegment.copy(bytes, 0, block, ValueLayout.JAVA_BYTE, start, bytes.length);
        if (nulTerminated) block.set(ValueLayout.JAVA_BYTE, start + bytes.length, (byte) 0);
        Borrow borrow =
            new Borrow(
                block.address() + start,
                bytes.length,
                scratch,
                block,
                start,
                nulTerminated,
                bytes,
                source,
                writable);
        scratch.top = start + Scratch.reservation(size);
        return borrow;
      }
    }
    Arena arena = Arena.ofConfined();
    MemorySegment segment = arena.allocate(Math.max(size, 1));
    MemorySegment.copy(bytes, 0, segment, ValueLayout.JAVA_BYTE, 0, bytes.length);
    if (nulTerminated) segment.set(ValueLayout.JAVA_BYTE, bytes.length, (byte) 0);
    return new Borrow(
        segment.address(), bytes.length, arena, segment, 0, nulTerminated, bytes, source, writable);
  }

  public long address() {
    return address;
  }

  /** The borrowed byte count, excluding any NUL terminator. */
  public long length() {
    return length;
  }

  /**
   * Ends the borrow: writes copies back, unpins, frees. Idempotent; never throws for direct.
   *
   * @throws IllegalStateException if a scratch copy is released out of order or on another thread
   *     than the one that acquired it; the borrow and the stack are left as they were, so releasing
   *     in order afterwards still works
   */
  public void release() {
    if (released) return;
    Object owner = this.owner;
    if (owner instanceof Scratch scratch) {
      scratch.check(offset, offset + Scratch.reservation(length + (terminated ? 1 : 0)));
      released = true;
      if (writeBack) releaseScratch(scratch);
      else scratch.top = offset;
      return;
    }
    released = true;
    if (owner instanceof Arena arena) releaseCopy(arena);
    else if (owner != null) Pins.unpin(owner);
  }

  @TruffleBoundary
  private void releaseScratch(Scratch scratch) {
    try {
      writeBack();
    } finally {
      scratch.top = offset;
    }
  }

  @TruffleBoundary
  private void releaseCopy(Arena arena) {
    try {
      if (writeBack) writeBack();
    } finally {
      arena.close();
    }
  }

  private void writeBack() {
    MemorySegment.copy(copy, ValueLayout.JAVA_BYTE, offset, array, 0, array.length);
    if (interopSource != null) writeBackInterop();
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
      return new Borrow(address, array.length, pinned, null, 0, false, array, null, false);
    }

    static void unpin(Object pin) {
      ((PinnedObject) pin).close();
    }
  }

  /**
   * The run-time-initialized holder of every thread's scratch stack, so that no block, and no
   * address, is created at build time or reaches the image heap.
   */
  private static final class Scratches {
    static final ThreadLocal<Scratch> CURRENT = ThreadLocal.withInitial(Scratch::new);
  }

  /**
   * One thread's bump allocator for small copies: a single native block, pushed on acquisition and
   * popped on release. Cells release borrows in reverse acquisition order (nested {@code finally}
   * blocks), and a call from native code back into guest code acquires and releases above the
   * borrows of the call it interrupts, so use is last-in, first-out by construction. Copies are
   * contiguous, so a copy is the innermost exactly when it ends at the top; any other release, or a
   * release on another thread, throws and leaves the stack unchanged.
   *
   * <p>A copy that does not fit beside outstanding copies takes the arena path; the next
   * acquisition on an empty stack grows the block geometrically to that high-water mark, up to
   * {@link #LIMIT} bytes. Blocks come from automatic arenas: a replaced block, and the block of a
   * thread that has ended (with its {@code ThreadLocal} entry), is freed by the collector once
   * unreachable, so each live thread that has copied retains at most one block of at most {@link
   * #LIMIT} bytes. A borrow that is never released keeps its bytes reserved for the thread's
   * lifetime; later copies stack above it or fall back to arenas.
   */
  static final class Scratch {
    /** The first block's size in bytes. */
    static final long INITIAL = 4096;

    /** The largest block in bytes; offsets into it fit an {@code int}. */
    static final long LIMIT = 64 * 1024;

    /** The largest copy, terminator included, that the stack takes. */
    static final long LARGEST = 16 * 1024;

    /** Copies start on 16-byte boundaries, as {@code malloc} results do. */
    static final long ALIGNMENT = 16;

    private final Thread thread = Thread.currentThread();
    private MemorySegment block;
    private long wanted = INITIAL;

    /** The end of the innermost copy: zero when the stack is empty. */
    long top;

    /**
     * The bytes a copy of {@code size} bytes occupies; an empty copy still gets its own address.
     */
    static long reservation(long size) {
      return (Math.max(size, 1) + ALIGNMENT - 1) & -ALIGNMENT;
    }

    /** The block to copy {@code size} bytes into at {@link #top}, or {@code null} if it is full. */
    MemorySegment reserve(long size) {
      long end = top + reservation(size);
      wanted = Math.max(wanted, end);
      if (top == 0 && (block == null || block.byteSize() < wanted)) {
        long capacity = block == null ? INITIAL : block.byteSize();
        while (capacity < wanted && capacity < LIMIT) capacity *= 2;
        if (block == null || capacity != block.byteSize())
          block = Arena.ofAuto().allocate(capacity, ALIGNMENT);
      }
      return end <= block.byteSize() ? block : null;
    }

    /**
     * Fails unless the copy at {@code start} ending at {@code end} is innermost, on this thread.
     */
    void check(int start, long end) {
      if (top != end || thread != Thread.currentThread()) {
        CompilerDirectives.transferToInterpreter();
        throw misuse(start);
      }
    }

    @TruffleBoundary
    private IllegalStateException misuse(int start) {
      if (thread != Thread.currentThread())
        return new IllegalStateException(
            "borrow released on thread "
                + Thread.currentThread().getName()
                + " but acquired on "
                + thread.getName());
      return new IllegalStateException(
          "borrow released out of order: the copy at offset "
              + start
              + " is not the innermost (top "
              + top
              + ")");
    }
  }
}
