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
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.word.PointerBase;
import org.graalvm.word.WordFactory;

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
 *   <li>Strings are encoded as UTF-8, NUL-terminated when the parameter is a C string. Java strings
 *       and guest strings ({@link GuestText}) are encoded straight onto the scratch stack.
 * </ul>
 *
 * <p>Copies of up to {@link Scratch#LARGEST} bytes come from the current thread's scratch stack
 * ({@link Scratch}); larger copies, and copies that do not fit beside outstanding ones, get a
 * confined arena of their own. A pinned array does not move, so its address stays valid across any
 * safepoint between the pin and the call; an unpinned heap address is never handed to native code.
 *
 * <p>A borrow can also outlive its call: a {@code lifetime=resource:NAME} borrow lends a buffer to
 * the create of a resource, which keeps it until that resource is destroyed. Such a borrow is never
 * pinned and never on a scratch stack, both of which end with the call: native memory is lent in
 * place and kept reachable, and everything else is copied into native memory of its own. A create
 * that succeeds hands it to the new resource with {@link #keep()}, after which {@link #release()}
 * leaves it alone and the resource ends it when it is destroyed.
 *
 * <p>A borrow that a compiled cell acquires and releases is not allocated: lending native memory
 * and scratch copies cost no Java heap, and a pin costs only the {@code PinnedObject} itself. That
 * holds because nothing on the lend and release paths needs a class-initialization check (which
 * would keep the method out of runtime compilation) or passes the borrow to a method behind a
 * Truffle boundary; {@code tests/truffle_integration.py} checks both.
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
   * at build time and stays inlineable by partial evaluation. Reading it is a call out of compiled
   * code for that reason; {@link BytesCoercion} reads it once per call site.
   */
  public static int pinThreshold() {
    return Threshold.VALUE;
  }

  private static final class Threshold {
    static final int VALUE = Integer.getInteger("myna.truffle.pinThreshold", 256);
  }

  /** What {@link #release()} undoes. */
  private static final int LENT = 0;

  private static final int PINNED = 1;
  private static final int SCRATCH = 2;
  private static final int COPIED = 3;

  /** Guest text of one byte per UTF-16 code unit: Latin-1, of which ASCII is copied as it is. */
  static final int LATIN_1 = 1;

  /** Guest text of two bytes per UTF-16 code unit, little-endian like every Myna target. */
  static final int UTF_16 = 2;

  private static final Borrow NONE = new Borrow();

  // A borrow is filled in place, by exactly one lend method, and never handed to a method that
  // partial evaluation does not inline: compiled code then keeps it in registers instead of
  // allocating it, whichever kinds of value a call site has seen. Anything that must leave
  // compiled code (the scratch push, an arena copy, write-back) takes and returns existing objects
  // and primitives only.
  private int kind;
  private long address;
  private long length;

  /**
   * A {@code PinnedObject}, the thread's {@link Scratch}, the {@link Copy} of a large copy or of a
   * held borrow, or the native memory a held borrow lends in place.
   */
  private Object owner;

  /** The array a writable copy is written back to on release; {@code null} for read-only copies. */
  private byte[] array;

  /** The interop buffer a writable copy came from, written back after {@link #array}. */
  private Object interopSource;

  /** Where a scratch copy starts in its thread's block. */
  private int offset;

  /** Whether a scratch copy has a NUL terminator after {@link #length} bytes. */
  private boolean terminated;

  private boolean released;

  /** The null borrow until a lend method fills it. */
  Borrow() {}

  /** The null borrow for a nullable parameter. */
  public static Borrow none() {
    return NONE;
  }

  /** Lends existing native memory for the call. */
  public static Borrow direct(long address, long length) {
    Borrow borrow = new Borrow();
    borrow.lend(address, length);
    return borrow;
  }

  /** Lends a heap array: pinned when large and in a Native Image, otherwise copied. */
  public static Borrow ofArray(byte[] array, boolean writable, boolean nulTerminated) {
    Borrow borrow = new Borrow();
    borrow.lendArray(array, writable, nulTerminated, pinThreshold());
    return borrow;
  }

  /** Lends UTF-8 text, NUL-terminated for C strings. */
  public static Borrow ofString(String text, boolean nulTerminated) {
    Borrow borrow = new Borrow();
    borrow.lendString(text, nulTerminated);
    return borrow;
  }

  /**
   * Copies any interop buffer, optionally NUL-terminated; writable borrows are written back through
   * the same library. The terminator reserves one extra byte and writes a {@code 0} past the
   * content; {@link #length()} still excludes it, and {@link #release()} only writes back {@code
   * length} bytes, so the terminator never reaches the source buffer.
   */
  public static Borrow ofBuffer(
      InteropLibrary interop, Object buffer, boolean writable, boolean nulTerminated)
      throws UnsupportedMessageException {
    Borrow borrow = new Borrow();
    borrow.lendBuffer(interop, buffer, writable, nulTerminated);
    return borrow;
  }

  /** A non-null address for an empty buffer; a call out of compiled code, like any slow path. */
  @TruffleBoundary
  static long emptyAddress() {
    return EmptyBytes.address();
  }

  void lend(long address, long length) {
    this.address = address;
    this.length = length;
  }

  /** Lends bytes that {@code owner} must undo on release. */
  private void own(int kind, Object owner, long address, long length) {
    this.kind = kind;
    this.owner = owner;
    this.address = address;
    this.length = length;
  }

  /**
   * Keeps {@code source}, the native memory a held borrow lends in place, reachable for as long as
   * the borrow lasts: an automatic arena's memory then stays allocated until the resource that
   * holds the borrow is destroyed.
   */
  void retain(Object source) {
    this.owner = source;
  }

  /**
   * Lends a copy of {@code bytes} that lasts until {@link #release()} or, once kept, {@link
   * #end()}: native memory of its own, never the scratch stack or a pin, which end with the call.
   * An empty buffer lends the dummy address and copies nothing.
   */
  void lendHeld(byte[] bytes) {
    if (bytes.length == 0) {
      lend(emptyAddress(), 0);
      return;
    }
    Copy copy = Copy.held(bytes);
    own(COPIED, copy, copy.address, bytes.length);
  }

  /** Lends a held copy of {@code text} as UTF-8, without a terminator. */
  void lendHeldText(String text) {
    lendHeld(utf8(text));
  }

  /** Lends a held copy of any interop buffer, read through {@code interop}. */
  void lendHeldBuffer(InteropLibrary interop, Object buffer) throws UnsupportedMessageException {
    lendHeld(read(interop, buffer, false));
  }

  /**
   * @param threshold {@link #pinThreshold()}, passed in so that compiled code reads it once per
   *     call site and not on every call
   */
  void lendArray(byte[] array, boolean writable, boolean nulTerminated, int threshold) {
    if (!nulTerminated && array.length >= threshold && ImageInfo.inImageRuntimeCode())
      Pins.pin(this, array);
    else lendCopy(array, null, writable, nulTerminated);
  }

  /**
   * Text is encoded straight onto the scratch stack, with no intermediate array; it is always
   * followed by a NUL there, which a buffer parameter simply does not read. Text too long for the
   * stack, or that does not fit beside outstanding copies, is encoded into an array first.
   */
  void lendString(String text, boolean nulTerminated) {
    Scratch scratch = Scratch.pushText(text);
    if (scratch != null) lendPushed(scratch);
    else lendCopy(utf8(text), null, false, nulTerminated);
  }

  /**
   * Owns the terminated text that {@code scratch} has just pushed ({@link Scratch#pushText} or
   * {@link Scratch#pushUnits}). The push happens outside the borrow, which only records it: the
   * code that encodes never sees the borrow, so it need not be inlined for the borrow to stay out
   * of the heap.
   */
  void lendPushed(Scratch scratch) {
    int length = scratch.pushed;
    int start = (int) (scratch.top - Scratch.reservation(length + 1L));
    own(SCRATCH, scratch, scratch.base + start, length);
    this.offset = start;
    this.terminated = true;
  }

  void lendBuffer(InteropLibrary interop, Object buffer, boolean writable, boolean nulTerminated)
      throws UnsupportedMessageException {
    lendCopy(read(interop, buffer, writable), buffer, writable, nulTerminated);
  }

  @TruffleBoundary
  private static byte[] utf8(String text) {
    return text.getBytes(StandardCharsets.UTF_8);
  }

  @TruffleBoundary
  private static byte[] read(InteropLibrary interop, Object buffer, boolean writable)
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
    return bytes;
  }

  /**
   * Copies into the current thread's scratch stack when the copy fits, otherwise into an arena of
   * its own. The copy is pushed only once it is complete, so a failure leaves the stack unchanged.
   */
  private void lendCopy(byte[] bytes, Object source, boolean writable, boolean nulTerminated) {
    long size = (long) bytes.length + (nulTerminated ? 1 : 0);
    Scratch scratch = size <= Scratch.LARGEST ? Scratch.push(bytes, nulTerminated) : null;
    if (scratch != null) {
      int start = (int) (scratch.top - Scratch.reservation(size));
      own(SCRATCH, scratch, scratch.base + start, bytes.length);
      this.offset = start;
      this.terminated = nulTerminated;
    } else {
      Copy copy = Copy.of(bytes, nulTerminated);
      own(COPIED, copy, copy.address, bytes.length);
    }
    if (writable) {
      this.array = bytes;
      this.interopSource = source;
    }
  }

  public long address() {
    return address;
  }

  /** The borrowed byte count, excluding any NUL terminator. */
  public long length() {
    return length;
  }

  /**
   * Hands this borrow to the resource a create made with it: {@link #release()} no longer ends it,
   * and the resource calls {@link #end()} once it has been destroyed. Only a held borrow (one
   * {@link HeldBytesCoercion} lent) may be kept; anything else ends with its call.
   *
   * @return this borrow, for the resource to hold
   */
  public Borrow keep() {
    // A pin or a scratch copy ends with its call; holding one would leak it or break the stack.
    if (released || this == NONE || kind == PINNED || kind == SCRATCH || array != null)
      throw new IllegalStateException("only a held borrow can outlive its call");
    released = true;
    return this;
  }

  /** Ends a borrow that {@link #keep()} handed to a resource; called once, by that resource. */
  void end() {
    Object owner = this.owner;
    this.owner = null;
    if (kind == COPIED) Copy.release((Copy) owner, null, null);
  }

  /**
   * Ends the borrow: writes copies back, unpins, frees. Idempotent; never throws for direct. A
   * borrow that a resource {@linkplain #keep() keeps} is already released as far as this method is
   * concerned: the resource ends it.
   *
   * @throws IllegalStateException if a scratch copy is released out of order or on another thread
   *     than the one that acquired it; the borrow and the stack are left as they were, so releasing
   *     in order afterwards still works
   */
  public void release() {
    if (released) return;
    int kind = this.kind;
    if (kind == SCRATCH) {
      Scratch scratch = (Scratch) owner;
      scratch.check(offset, offset + Scratch.reservation(length + (terminated ? 1 : 0)));
      released = true;
      if (array != null) Scratch.writeBackAndPop(scratch, offset, array, interopSource);
      else scratch.top = offset;
      return;
    }
    released = true;
    if (kind == PINNED) Pins.unpin(owner);
    else if (kind == COPIED) Copy.release((Copy) owner, array, interopSource);
  }

  /** Copies the native bytes back into the borrowed array, then into its interop source. */
  private static void writeBack(MemorySegment copy, long offset, byte[] array, Object source) {
    MemorySegment.copy(copy, ValueLayout.JAVA_BYTE, offset, array, 0, array.length);
    if (source != null) writeBackInterop(array, source);
  }

  /**
   * Guest code can shrink or detach the source buffer while the call runs (from a callback or
   * another thread). Bytes that no longer exist, or a buffer that no longer accepts writes, cannot
   * be written back: that is a guest error, reported after every surviving byte has been written,
   * not an internal one.
   */
  private static void writeBackInterop(byte[] array, Object source) {
    InteropLibrary interop = InteropLibrary.getUncached(source);
    try {
      for (int i = 0; i < array.length; i++) interop.writeBufferByte(source, i, array[i]);
    } catch (InvalidBufferOffsetException e) {
      throw NativeCellException.of(
          null, "borrowed buffer shrank before write-back of byte ", e.getByteOffset());
    } catch (UnsupportedMessageException e) {
      throw NativeCellException.of(
          null, "borrowed buffer stopped accepting writes before write-back");
    }
  }

  /**
   * A copy too large for the scratch stack, or one that did not fit: an arena of its own. A held
   * borrow's copy is one too, made by {@link #held}.
   */
  private static final class Copy {
    /** The arena, or {@code null} for a held copy {@code malloc}ed in a Native Image. */
    final Arena arena;

    final MemorySegment segment;
    final long address;

    private Copy(Arena arena, MemorySegment segment) {
      this.arena = arena;
      this.segment = segment;
      this.address = segment.address();
    }

    @TruffleBoundary
    static Copy of(byte[] bytes, boolean nulTerminated) {
      Arena arena = Arena.ofConfined();
      MemorySegment segment = arena.allocate(Math.max(bytes.length + (nulTerminated ? 1L : 0L), 1));
      MemorySegment.copy(bytes, 0, segment, ValueLayout.JAVA_BYTE, 0, bytes.length);
      if (nulTerminated) segment.set(ValueLayout.JAVA_BYTE, bytes.length, (byte) 0);
      return new Copy(arena, segment);
    }

    /**
     * The copy a held borrow lends: native memory that lasts until the resource holding the borrow
     * is destroyed, which may happen on any thread (a shared resource's close or cleaner), so not a
     * confined arena. In a Native Image it is {@code malloc}ed and freed explicitly; on HotSpot it
     * is a shared arena of its own. A pin would also outlive the call, but for as long as the guest
     * keeps the resource open the collector could neither move nor free the array's memory, and the
     * guest's later writes to the array would reach the native side.
     */
    @TruffleBoundary
    static Copy held(byte[] bytes) {
      long size = bytes.length;
      Copy copy;
      if (ImageInfo.inImageRuntimeCode()) {
        copy = new Copy(null, MemorySegment.ofAddress(Malloc.allocate(size)).reinterpret(size));
      } else {
        Arena arena = Arena.ofShared();
        copy = new Copy(arena, arena.allocate(size, Scratch.ALIGNMENT));
      }
      MemorySegment.copy(bytes, 0, copy.segment, ValueLayout.JAVA_BYTE, 0, bytes.length);
      return copy;
    }

    @TruffleBoundary
    static void release(Copy copy, byte[] array, Object source) {
      try {
        if (array != null) writeBack(copy.segment, 0, array, source);
      } finally {
        if (copy.arena != null) copy.arena.close();
        else Malloc.free(copy.address);
      }
    }
  }

  /**
   * Native Image unmanaged memory, for held copies and {@link NativeStruct}s; loaded only inside an
   * image.
   */
  static final class Malloc {
    static long allocate(long size) {
      return UnmanagedMemory.<PointerBase>malloc(WordFactory.unsigned(size)).rawValue();
    }

    static long zeroed(long size) {
      return UnmanagedMemory.<PointerBase>calloc(WordFactory.unsigned(size)).rawValue();
    }

    static void free(long address) {
      UnmanagedMemory.free(WordFactory.pointer(address));
    }
  }

  /** Native Image pinning; loaded only inside an image, where {@link PinnedObject} exists. */
  private static final class Pins {
    static void pin(Borrow borrow, byte[] array) {
      PinnedObject pin = PinnedObject.create(array);
      borrow.own(PINNED, pin, pin.addressOfArrayElement(0).rawValue(), array.length);
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

    /** Text up to this length is written byte by byte; a bulk copy only pays off beyond it. */
    static final int SHORT_TEXT = 64;

    /** Copies start on 16-byte boundaries, as {@code malloc} results do. */
    static final long ALIGNMENT = 16;

    private final Thread thread = Thread.currentThread();
    private MemorySegment block;
    private long wanted = INITIAL;

    /** The address of {@link #block}, read by compiled code after a push. */
    long base;

    /**
     * The byte length of the text most recently pushed by {@link #pushText} or {@link #pushUnits}.
     */
    int pushed;

    /**
     * Where text is copied out of its string, a chunk at a time, before it is packed or encoded.
     */
    private final char[] chars = new char[SHORT_TEXT];

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
        if (block == null || capacity != block.byteSize()) {
          block = Arena.ofAuto().allocate(capacity, ALIGNMENT);
          base = block.address();
        }
      }
      return end <= block.byteSize() ? block : null;
    }

    /**
     * Copies {@code bytes} onto the current thread's stack and returns that stack, whose {@link
     * #top} is then the end of the copy, or returns {@code null} when the copy does not fit. The
     * result is an existing object, so the caller's borrow stays out of this call.
     */
    @TruffleBoundary
    static Scratch push(byte[] bytes, boolean nulTerminated) {
      Scratch scratch = Scratches.CURRENT.get();
      long size = (long) bytes.length + (nulTerminated ? 1 : 0);
      MemorySegment block = scratch.reserve(size);
      if (block == null) return null;
      int start = (int) scratch.top;
      MemorySegment.copy(bytes, 0, block, ValueLayout.JAVA_BYTE, start, bytes.length);
      if (nulTerminated) block.set(ValueLayout.JAVA_BYTE, start + bytes.length, (byte) 0);
      scratch.top = start + reservation(size);
      return scratch;
    }

    /**
     * Writes {@code text} as UTF-8 and a NUL onto the current thread's stack and returns that
     * stack, with {@link #pushed} set to the byte length, or returns {@code null} when the text is
     * too long or does not fit. Nothing is pushed in that case.
     */
    @TruffleBoundary
    static Scratch pushText(String text) {
      int length = text.length();
      if (length >= LARGEST) return null;
      Scratch scratch = Scratches.CURRENT.get();
      MemorySegment block = scratch.reserve(length + 1L);
      if (block == null) return null;
      int start = (int) scratch.top;
      if (length <= SHORT_TEXT) {
        // Short text: copy the characters out in bulk, then pack eight to a word and write the
        // word. Whether every character was ASCII is checked once at the end; nothing is pushed
        // until the top moves, so starting over as UTF-8 then leaves the stack as it was.
        char[] chars = scratch.chars;
        text.getChars(0, length, chars, 0);
        long base = scratch.base;
        int bits = 0;
        int i = 0;
        for (; i + 8 <= length; i += 8) {
          long word = 0;
          for (int k = 0; k < 8; k++) {
            char c = chars[i + k];
            bits |= c;
            word |= (long) (c & 0xff) << (k << 3);
          }
          Memory.putLong(base, block, start + i, word);
        }
        for (; i < length; i++) {
          char c = chars[i];
          bits |= c;
          Memory.putByte(base, block, start + i, (byte) c);
        }
        if (bits >= 0x80) return scratch.encode(null, 0, length, LOADED);
        Memory.putByte(base, block, start + length, (byte) 0);
      } else {
        // Longer text: one byte per character for ASCII, then the terminator, copied in bulk.
        for (int i = 0; i < length; i++)
          if (text.charAt(i) >= 0x80) return scratch.encode(text, 0, length, STRING);
        block.setString(start, text);
      }
      scratch.pushed = length;
      scratch.top = start + reservation(length + 1L);
      return scratch;
    }

    /**
     * {@link #pushText} for guest text given as its UTF-16 code units: {@code length} bytes of
     * {@code units} from {@code offset}, in the form {@link Borrow#LATIN_1} or {@link
     * Borrow#UTF_16}. Latin-1 text is copied as it is when it turns out to be ASCII, whatever the
     * string knew about itself; a string made from a Java string does not know until asked.
     */
    @TruffleBoundary
    static Scratch pushUnits(byte[] units, int offset, int length, int form) {
      Scratch scratch = Scratches.CURRENT.get();
      if (form == UTF_16) return scratch.encode(units, offset, length >> 1, UTF_16);
      MemorySegment block = length < LARGEST ? scratch.reserve(length + 1L) : null;
      if (block != null) {
        int start = (int) scratch.top;
        int bits = 0;
        if (length <= SHORT_TEXT) {
          // Short text: write each byte, and check once at the end that all were ASCII.
          long base = scratch.base;
          for (int i = 0; i < length; i++) {
            byte unit = units[offset + i];
            bits |= unit;
            Memory.putByte(base, block, start + i, unit);
          }
          if (bits >= 0) Memory.putByte(base, block, start + length, (byte) 0);
        } else {
          // Longer text: check first, then copy in bulk.
          for (int i = 0; i < length; i++) bits |= units[offset + i];
          if (bits >= 0) {
            MemorySegment.copy(units, offset, block, ValueLayout.JAVA_BYTE, start, length);
            block.set(ValueLayout.JAVA_BYTE, start + length, (byte) 0);
          }
        }
        if (bits >= 0) {
          scratch.pushed = length;
          scratch.top = start + reservation(length + 1L);
          return scratch;
        }
      }
      return scratch.encode(units, offset, length, LATIN_1);
    }

    /** {@link #encode} sources besides the guest forms: a Java string, and {@link #chars} as is. */
    private static final int STRING = 3;

    private static final int LOADED = 4;

    /**
     * Encodes {@code count} UTF-16 code units as UTF-8 onto this stack, followed by a NUL, exactly
     * as {@code String.getBytes(UTF_8)} does: an unpaired surrogate becomes {@code ?}. The units
     * come from {@code source} starting at {@code offset}, as {@code form} says: the characters of
     * a string, the bytes of an array (one per unit, or two little-endian), or the units already in
     * {@link #chars}, which then hold all of them.
     *
     * <p>The worst case is reserved (three bytes per unit, two for Latin-1, and the terminator),
     * but only what was written is pushed, so a release computes the same extent from the byte
     * length. Returns {@code null}, pushing nothing, when the worst case exceeds {@link #LARGEST}
     * or does not fit beside outstanding copies.
     */
    private Scratch encode(Object source, int offset, int count, int form) {
      long worst = (form == LATIN_1 ? 2L : 3L) * count + 1;
      MemorySegment block = worst <= LARGEST ? reserve(worst) : null;
      if (block == null) return null;
      int start = (int) top;
      long base = this.base;
      char[] chars = this.chars;
      int at = start;
      for (int done = 0; done < count; ) {
        int chunk = Math.min(count - done, SHORT_TEXT);
        int from = offset + done;
        switch (form) {
          case STRING -> ((String) source).getChars(from, from + chunk, chars, 0);
          case LATIN_1 -> {
            byte[] bytes = (byte[]) source;
            for (int i = 0; i < chunk; i++) chars[i] = (char) (bytes[from + i] & 0xff);
          }
          case UTF_16 -> {
            byte[] bytes = (byte[]) source;
            from = offset + 2 * done;
            for (int i = 0; i < chunk; i++, from += 2)
              chars[i] = (char) (bytes[from] & 0xff | bytes[from + 1] << 8);
          }
          default -> {}
        }
        // A surrogate pair is never split: a chunk's last high half waits for the next chunk.
        if (done + chunk < count && Character.isHighSurrogate(chars[chunk - 1])) chunk--;
        for (int i = 0; i < chunk; i++) {
          char c = chars[i];
          if (c < 0x80) {
            Memory.putByte(base, block, at++, (byte) c);
          } else if (c < 0x800) {
            Memory.putByte(base, block, at++, (byte) (0xc0 | c >> 6));
            Memory.putByte(base, block, at++, (byte) (0x80 | c & 0x3f));
          } else if (!Character.isSurrogate(c)) {
            Memory.putByte(base, block, at++, (byte) (0xe0 | c >> 12));
            Memory.putByte(base, block, at++, (byte) (0x80 | c >> 6 & 0x3f));
            Memory.putByte(base, block, at++, (byte) (0x80 | c & 0x3f));
          } else if (i + 1 < chunk
              && Character.isHighSurrogate(c)
              && Character.isLowSurrogate(chars[i + 1])) {
            int point = Character.toCodePoint(c, chars[++i]);
            Memory.putByte(base, block, at++, (byte) (0xf0 | point >> 18));
            Memory.putByte(base, block, at++, (byte) (0x80 | point >> 12 & 0x3f));
            Memory.putByte(base, block, at++, (byte) (0x80 | point >> 6 & 0x3f));
            Memory.putByte(base, block, at++, (byte) (0x80 | point & 0x3f));
          } else {
            Memory.putByte(base, block, at++, (byte) '?');
          }
        }
        done += chunk;
      }
      Memory.putByte(base, block, at, (byte) 0);
      pushed = at - start;
      top = start + reservation(pushed + 1L);
      return this;
    }

    /** Writes the copy at {@code start} back, then pops it whether or not that succeeded. */
    @TruffleBoundary
    static void writeBackAndPop(Scratch scratch, int start, byte[] array, Object source) {
      try {
        writeBack(scratch.block, start, array, source);
      } finally {
        scratch.top = start;
      }
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
