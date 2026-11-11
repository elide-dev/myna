// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package fixture.cells;
import com.oracle.truffle.api.interop.InteropLibrary;
import dev.elide.myna.truffle.*;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;
/** The same guest-level checks for the Native Image and FFM cell backends. */
public final class CellChecks {
  private static final InteropLibrary INTEROP = InteropLibrary.getUncached();
  static void check(boolean condition, String what) { if (!condition) throw new AssertionError(what); }
  static Object call(Function<String, NativeFunction> cells, String name, Object... args) throws Exception {
    return INTEROP.execute(cells.apply(name), args);
  }
  static void rejects(Function<String, NativeFunction> cells, String name, Object... args) throws Exception {
    try { call(cells, name, args); } catch (NativeCellException expected) { return; }
    throw new AssertionError(name + " accepted " + java.util.Arrays.toString(args));
  }
  interface Access { void run() throws Exception; }
  static void outOfBounds(Access access, String what) throws Exception {
    try { access.run(); } catch (com.oracle.truffle.api.interop.InvalidBufferOffsetException expected) { return; }
    throw new AssertionError(what + " was accepted");
  }
  /** What cells_pair computes from two C strings: their UTF-8 lengths and first bytes. */
  public static long pair(String left, String right) {
    byte[] l = left.getBytes(StandardCharsets.UTF_8), r = right.getBytes(StandardCharsets.UTF_8);
    return ((long) l.length << 32) | ((long) r.length << 16) | ((l.length == 0 ? 0 : l[0] & 0xff) << 8) | (r.length == 0 ? 0 : r[0] & 0xff);
  }
  /**
   * Two borrows with lengths of their own beside a pointer that passes through: the guest gives
   * the two buffers and the pointer, and the cell derives both lengths. {@code large} holds ones.
   */
  static void transfers(Function<String, NativeFunction> cells, byte[] large) throws Exception {
    try (Arena arena = Arena.ofConfined()) {
      MemorySegment input = arena.allocate(4);
      input.fill((byte) 5);
      MemorySegment output = arena.allocate(3);
      MemorySegment copied = arena.allocate(ValueLayout.JAVA_LONG);
      NativePointer count = NativePointer.of(copied.address());
      // Native memory is lent in place, both ways.
      copied.set(ValueLayout.JAVA_LONG, 0, -1);
      check(call(cells, "transfer", NativeBytes.of(input, false), NativeBytes.of(output, true), count).equals(15L), "transfer native");
      check(copied.get(ValueLayout.JAVA_LONG, 0) == 3 && output.get(ValueLayout.JAVA_BYTE, 2) == 5, "transfer wrote native memory in place");
      // The pointer beside the buffers keeps its own contract: here, nullable.
      check(call(cells, "transfer", NativeBytes.of(input, false), NativeBytes.of(output, true), NativePointer.NULL).equals(15L), "transfer without a count");
      check(call(cells, "transfer", NativeBytes.of(input, false), NativeBytes.of(output, true), NativeVoid.INSTANCE).equals(15L), "transfer with interop null");
      // Heap arrays, copied or pinned, in either position; a writable one is written back.
      byte[] small = new byte[5];
      check(call(cells, "transfer", new HeapBytes(new byte[] {1, 2, 3}, false), new HeapBytes(small, true), count).equals(6L), "transfer small heap");
      check(small[0] == 1 && small[2] == 3 && small[3] == 0 && copied.get(ValueLayout.JAVA_LONG, 0) == 3, "transfer wrote a copied array back");
      byte[] wide = new byte[large.length + 1];
      check(call(cells, "transfer", new HeapBytes(large, false), new HeapBytes(wide, true), count).equals((long) large.length), "transfer large heap");
      check(wide[0] == 1 && wide[large.length - 1] == 1 && wide[large.length] == 0 && copied.get(ValueLayout.JAVA_LONG, 0) == large.length, "transfer wrote a large array back");
      // Mixed kinds: a string or a heap array into native memory, native memory into a heap array.
      check(call(cells, "transfer", "abc", NativeBytes.of(output, true), count).equals(294L), "transfer string");
      check(output.get(ValueLayout.JAVA_BYTE, 0) == 'a' && output.get(ValueLayout.JAVA_BYTE, 2) == 'c', "transfer copied the string's bytes, unterminated");
      check(call(cells, "transfer", NativeBytes.of(input, false), new HeapBytes(small, true), NativePointer.NULL).equals(20L), "transfer native to heap");
      check(small[0] == 5 && small[3] == 5 && small[4] == 0, "transfer native to heap wrote back");
      // Either buffer may be empty.
      check(call(cells, "transfer", new HeapBytes(new byte[0], false), NativeBytes.of(output, true), count).equals(0L), "transfer empty input");
      check(copied.get(ValueLayout.JAVA_LONG, 0) == 0, "transfer empty input count");
      copied.set(ValueLayout.JAVA_LONG, 0, -1);
      check(call(cells, "transfer", NativeBytes.of(input, false), new HeapBytes(new byte[0], true), count).equals(0L), "transfer empty output");
      check(copied.get(ValueLayout.JAVA_LONG, 0) == 0, "transfer empty output count");
      // Both lengths are derived: the guest signature has the two buffers and the pointer only.
      rejects(cells, "transfer", NativeBytes.of(input, false), 4L, NativeBytes.of(output, true), 3, count);
      rejects(cells, "transfer", NativeBytes.of(input, false), NativeBytes.of(output, true));
      // Each buffer keeps its own access rules, and a bare pointer has no size to derive.
      rejects(cells, "transfer", NativeBytes.of(input, false), NativeBytes.of(output.asReadOnly(), true), count);
      rejects(cells, "transfer", NativeBytes.of(input, false), "text", count);
      rejects(cells, "transfer", NativePointer.of(input.address()), NativeBytes.of(output, true), count);
      rejects(cells, "transfer", NativeBytes.of(input, false), NativePointer.of(output.address()), count);
      rejects(cells, "transfer", NativeBytes.of(input, false), NativeBytes.of(output, true), 7);
      // A second borrow that fails releases the first: the next call's copies still work.
      rejects(cells, "transfer", "kept", 1, count);
      check(call(cells, "transfer", "ab", new HeapBytes(small, true), count).equals(195L), "transfer after a failed second borrow");
      check(small[0] == 'a' && small[1] == 'b' && small[2] == 5, "transfer after a failed second borrow wrote back");
    }
  }
  public static void run(Function<String, NativeFunction> cells) throws Exception {
    // Host Java null bypasses guest interop values; exercise the public host call API.
    for (Object[] sample : new Object[][] {
        {"add", 1, null}, {"widen", 0, 0, 0, null}, {"scale", null, 2},
        {"peek", null}, {"strlen", null}, {"checksum", null}, {"fill", null, 1},
        {"transfer", null, new HeapBytes(new byte[1], true), NativePointer.NULL},
        {"transfer", new HeapBytes(new byte[1], false), null, NativePointer.NULL}}) {
      String name = (String) sample[0];
      try {
        cells.apply(name).call(java.util.Arrays.copyOfRange(sample, 1, sample.length));
        throw new AssertionError(name + " accepted host null");
      } catch (NativeCellException expected) {
        check(expected.getMessage().contains("got null"), name + " host null diagnostic");
      }
    }
    check(cells.apply("rawLength").call((Object) null).equals(0L), "nullable host null");
    check(call(cells, "add", 40, 2).equals(42), "add");
    check(call(cells, "add", 40L, 2.0).equals(42), "add from long and double");
    rejects(cells, "add", 1);
    rejects(cells, "add", 1, "two");
    rejects(cells, "add", 1, 2147483648L);
    long widened = (long) call(cells, "widen", 255, 65535, 4294967295L, true);
    check(widened == (255L | (65535L << 8) | (4294967295L << 24) | (1L << 56)), "widen " + widened);
    rejects(cells, "widen", 256, 0, 0, true);
    rejects(cells, "widen", 0, 0, 0, 1);
    check(call(cells, "scale", 1.5, 2).equals(3.0), "scale");
    check(call(cells, "lowByte", 0x1ff).equals(255), "lowByte");
    check(call(cells, "sleepyAdd", 1L, 2).equals(3L), "sleepyAdd");
    try (Arena arena = Arena.ofConfined()) {
      MemorySegment cell = arena.allocate(ValueLayout.JAVA_LONG);
      NativePointer pointer = NativePointer.of(cell.address());
      check(call(cells, "poke", pointer, 7L) == NativeVoid.INSTANCE, "poke returns void");
      check(cell.get(ValueLayout.JAVA_LONG, 0) == 7, "poke wrote");
      check(call(cells, "peek", pointer).equals(7L), "peek");
      check(call(cells, "peek", NativeBytes.of(cell, true)).equals(7L), "peek through a buffer pointer");
      rejects(cells, "peek", NativeVoid.INSTANCE);
      rejects(cells, "peek", cell.address());
      MemorySegment text = arena.allocateFrom("abc");
      check(call(cells, "rawLength", text.address()).equals(3L), "raw pointer opt-in");
      check(call(cells, "rawLength", NativeVoid.INSTANCE).equals(0L), "nullable raw pointer");
      check(call(cells, "strlen", NativeBytes.of(text, false)).equals(3L), "strlen over native bytes");
      check(call(cells, "strlen", "héllo").equals(6L), "strlen encodes UTF-8");
      rejects(cells, "strlen", NativeVoid.INSTANCE);
      MemorySegment buffer = arena.allocate(8);
      check(call(cells, "fill", NativeBytes.of(buffer, true), 9) == NativeVoid.INSTANCE, "fill native");
      check(buffer.get(ValueLayout.JAVA_BYTE, 7) == 9, "fill wrote native memory in place");
      check(call(cells, "checksum", NativeBytes.of(buffer, false)).equals(72L), "checksum native");
      NativeBytes view = NativeBytes.of(buffer, true);
      INTEROP.writeBufferLong(view, java.nio.ByteOrder.LITTLE_ENDIAN, 0, 0x0102030405060708L);
      check(INTEROP.readBufferByte(view, 0) == 8 && INTEROP.readBufferByte(view, 7) == 1, "native byte reads");
      check(INTEROP.readBufferShort(view, java.nio.ByteOrder.BIG_ENDIAN, 0) == 0x0807, "native short big-endian");
      check(INTEROP.readBufferInt(view, java.nio.ByteOrder.LITTLE_ENDIAN, 4) == 0x01020304, "native int");
      INTEROP.writeBufferInt(view, java.nio.ByteOrder.BIG_ENDIAN, 0, 0x11223344);
      INTEROP.writeBufferShort(view, java.nio.ByteOrder.LITTLE_ENDIAN, 4, (short) 0x5566);
      INTEROP.writeBufferByte(view, 6, (byte) 0x77);
      check(INTEROP.readBufferLong(view, java.nio.ByteOrder.BIG_ENDIAN, 0) == 0x1122334466557701L, "native writes " + Long.toHexString(INTEROP.readBufferLong(view, java.nio.ByteOrder.BIG_ENDIAN, 0)));
      INTEROP.writeBufferDouble(view, java.nio.ByteOrder.LITTLE_ENDIAN, 0, 2.5);
      check(INTEROP.readBufferDouble(view, java.nio.ByteOrder.LITTLE_ENDIAN, 0) == 2.5, "native double");
      INTEROP.writeBufferFloat(view, java.nio.ByteOrder.BIG_ENDIAN, 4, 1.5f);
      check(INTEROP.readBufferFloat(view, java.nio.ByteOrder.BIG_ENDIAN, 4) == 1.5f, "native float");
      // Untrusted offsets and lengths never reach the image's word accesses.
      long before = INTEROP.readBufferLong(view, java.nio.ByteOrder.LITTLE_ENDIAN, 0);
      for (long offset : new long[] {-1, 8, 9, Long.MIN_VALUE, Long.MAX_VALUE, Long.MAX_VALUE - 7}) {
        outOfBounds(() -> INTEROP.readBufferByte(view, offset), "native byte read at " + offset);
        outOfBounds(() -> INTEROP.writeBufferByte(view, offset, (byte) 0), "native byte write at " + offset);
        outOfBounds(() -> INTEROP.readBufferLong(view, java.nio.ByteOrder.LITTLE_ENDIAN, offset), "native long read at " + offset);
      }
      outOfBounds(() -> INTEROP.readBufferLong(view, java.nio.ByteOrder.LITTLE_ENDIAN, 1), "native straddling long read");
      outOfBounds(() -> INTEROP.writeBufferInt(view, java.nio.ByteOrder.BIG_ENDIAN, 5, 0), "native straddling int write");
      outOfBounds(() -> INTEROP.readBuffer(view, 4, new byte[8], 0, 5), "native bulk read past the end");
      outOfBounds(() -> INTEROP.readBuffer(view, 0, new byte[8], 0, -1), "native bulk read of negative length");
      outOfBounds(() -> INTEROP.readBuffer(view, Long.MAX_VALUE, new byte[8], 0, 1), "native bulk read at a huge offset");
      check(INTEROP.readBufferLong(view, java.nio.ByteOrder.LITTLE_ENDIAN, 0) == before, "rejected writes left memory intact");
      rejects(cells, "fill", NativeBytes.of(buffer.asReadOnly(), true), 1);
    }
    // Two copied C strings in one call: both terminated, neither overwriting the other.
    check(call(cells, "pair", "héllo", "wörld!").equals(pair("héllo", "wörld!")), "pair of strings");
    check(call(cells, "pair", new HeapBytes("ab".getBytes(StandardCharsets.UTF_8), false), "xyz").equals(pair("ab", "xyz")), "pair heap and string");
    rejects(cells, "pair", "kept", 1);
    check(call(cells, "pair", "", "z").equals(pair("", "z")), "pair after a failed second borrow");
    check(call(cells, "checksum", new HeapBytes(new byte[] {1, 2, 3}, false)).equals(6L), "checksum heap");
    check(call(cells, "checksum", "abc").equals(294L), "checksum string");
    byte[] large = new byte[Borrow.pinThreshold() * 4];
    java.util.Arrays.fill(large, (byte) 1);
    check(call(cells, "checksum", new HeapBytes(large, false)).equals((long) large.length), "checksum pins or copies");
    // A truffle_buffer parameter is raw bytes beside other pointers: the guest passes the count,
    // nothing is terminated, and a large heap array is pinned in an image rather than copied.
    check(call(cells, "sumBytes", new HeapBytes(large, false), (long) large.length, NativePointer.NULL).equals((long) large.length), "sumBytes large heap");
    check(call(cells, "sumBytes", new HeapBytes(new byte[] {1, 2, 3}, false), 3L, NativePointer.NULL).equals(6L), "sumBytes small heap");
    check(call(cells, "sumBytes", "abc", 3L, NativePointer.NULL).equals(294L), "sumBytes string");
    try (Arena arena = Arena.ofConfined()) {
      MemorySegment raw = arena.allocate(4);
      raw.fill((byte) 5);
      MemorySegment bias = arena.allocate(8);
      bias.set(java.lang.foreign.ValueLayout.JAVA_LONG, 0, 100L);
      check(call(cells, "sumBytes", NativeBytes.of(raw, false), 4L, NativePointer.of(bias.address())).equals(120L), "sumBytes native with bias");
      check(call(cells, "sumBytes", NativePointer.of(raw.address()), 2L, NativePointer.NULL).equals(10L), "sumBytes bare pointer");
    }
    transfers(cells, large);
    check(call(cells, "fill", new HeapBytes(large, true), 7) == NativeVoid.INSTANCE, "fill heap");
    check(large[0] == 7 && large[large.length - 1] == 7, "fill wrote back");
    rejects(cells, "fill", new HeapBytes(large, false), 1);
    rejects(cells, "fill", "text", 1);
    rejects(cells, "fill", new HeapBytes(new byte[2], true), 256);
    check(call(cells, "counterLive").equals(0L), "no counters yet");
    Object counter = call(cells, "counterCreate", 5);
    check(counter instanceof NativeResource, "create returns an owner");
    check(call(cells, "counterNext", counter).equals(5L), "next");
    check(call(cells, "counterNext", counter).equals(6L), "next again");
    check(call(cells, "counterLive").equals(1L), "one live counter");
    check(call(cells, "counterDestroy", counter) == NativeVoid.INSTANCE, "destroy");
    check(call(cells, "counterLive").equals(0L), "destroyed");
    check(((NativeResource) counter).isClosed(), "closed");
    rejects(cells, "counterNext", counter);
    check(call(cells, "counterDestroy", counter) == NativeVoid.INSTANCE, "second destroy is a no-op");
    check(call(cells, "counterLive").equals(0L), "still destroyed once");
    check(call(cells, "counterCreate", -1) == NativeVoid.INSTANCE, "failed creation is null");
    rejects(cells, "counterDestroy", NativeVoid.INSTANCE);
    rejects(cells, "counterDestroy", NativePointer.of(16));
    // A create that takes pointers: another resource's handle and a pointer to plain memory.
    try (Arena arena = Arena.ofConfined()) {
      MemorySegment step = arena.allocate(8);
      step.set(ValueLayout.JAVA_LONG, 0, 5L);
      Object parent = call(cells, "counterCreate", 10);
      Object stepper = call(cells, "stepperCreate", parent, NativeBytes.of(step, false));
      check(stepper instanceof NativeResource r && r.type().equals("Stepper"), "stepper owner");
      check(call(cells, "counterLive").equals(2L), "counter and stepper live");
      check(call(cells, "stepperNext", stepper).equals(10L), "stepper reads its counter");
      check(call(cells, "counterNext", parent).equals(15L), "stepper advanced its counter");
      // The handle argument is typed by its pointer, not by resource: a closed parent, a null
      // and a failed creation are all reported, and nothing leaks.
      step.set(ValueLayout.JAVA_LONG, 0, 0L);
      check(call(cells, "stepperCreate", parent, NativeBytes.of(step, false)) == NativeVoid.INSTANCE, "failed stepper is null");
      rejects(cells, "stepperCreate", NativeVoid.INSTANCE, NativeBytes.of(step, false));
      rejects(cells, "stepperDestroy", parent);
      // An owner is taken only where its own resource is expected: a stepper's address read as
      // a counter, or the reverse, would be type confusion in native code.
      rejects(cells, "counterNext", stepper);
      rejects(cells, "stepperNext", parent);
      rejects(cells, "stepperCreate", stepper, NativeBytes.of(step, false));
      // The stepper keeps its counter open: closing the counter under it would leave the stepper
      // pointing at freed memory, so that is refused until the stepper is gone.
      rejects(cells, "counterDestroy", parent);
      check(call(cells, "counterLive").equals(2L), "parent survives a refused close");
      check(call(cells, "stepperNext", stepper).equals(16L), "stepper still usable");
      check(call(cells, "stepperDestroy", stepper) == NativeVoid.INSTANCE, "stepper destroyed");
      rejects(cells, "stepperNext", stepper);
      check(call(cells, "counterDestroy", parent) == NativeVoid.INSTANCE, "parent destroyed after its stepper");
      rejects(cells, "stepperCreate", parent, NativeBytes.of(step, false));
      check(call(cells, "counterLive").equals(0L), "both destroyed");
    }
    // Creates that report a status and write the resource through a parameter. The guest passes
    // a buffer for the struct, as a C caller would, and reads its fields from it afterwards; the
    // native side writes into memory the runtime owns and the buffer receives a copy.
    try (Arena arena = Arena.ofConfined()) {
      MemorySegment result = arena.allocate(24);
      Object out = NativeBytes.of(result, true);
      Object layout = call(cells, "layoutCompile", new HeapBytes(new byte[] {5, 1}, false), out);
      check(layout instanceof NativeResource r && r.type().equals("Layout"), "layout owner");
      check(result.get(ValueLayout.JAVA_LONG, 0) == 2, "count beside the handle");
      check(result.get(ValueLayout.JAVA_LONG, 16) == 4, "capacity beside the handle");
      check(call(cells, "layoutWidth", layout).equals(5L), "layout usable");
      check(call(cells, "counterLive").equals(1L), "one live layout");
      // Every listed status is success.
      Object second = call(cells, "layoutCompile", new HeapBytes(new byte[] {2}, false), out);
      check(call(cells, "layoutWidth", second).equals(2L), "status 2 also creates");
      check(call(cells, "layoutRelease", second) == NativeVoid.INSTANCE, "second released");
      // Any other status is an error carrying it, and nothing is created.
      try {
        call(cells, "layoutCompile", new HeapBytes(new byte[0], false), out);
        throw new AssertionError("a failed create returned");
      } catch (NativeStatusException failed) {
        check(failed.status() == 7, "status carried");
        check(result.get(ValueLayout.JAVA_LONG, 0) == 0, "the struct is copied on failure too");
        check(INTEROP.isException(failed), "an ordinary guest error");
        check(INTEROP.readMember(failed, "status").equals(7L), "status member");
        check(failed.getMessage().equals("layoutCompile created no resource: status 7"), failed.getMessage());
      }
      // Success without a handle is reported, not wrapped.
      try {
        call(cells, "layoutCompile", new HeapBytes(new byte[] {(byte) 0xff}, false), out);
        throw new AssertionError("a create without a handle returned an owner");
      } catch (NativeStatusException unexpected) {
        throw new AssertionError("no status to report", unexpected);
      } catch (NativeCellException missing) {
        check(missing.getMessage().equals("layoutCompile reported success but wrote no resource"), missing.getMessage());
      }
      check(call(cells, "counterLive").equals(1L), "failed creates leave nothing");
      rejects(cells, "layoutWidth", NativeVoid.INSTANCE);
      check(call(cells, "layoutRelease", layout) == NativeVoid.INSTANCE, "layout released");
      rejects(cells, "layoutWidth", layout);
      // A result initialized in place: the struct is the resource and release takes it back.
      MemorySegment view = arena.allocate(16);
      Object memory = NativeBytes.of(view, true);
      Object owned = call(cells, "viewInit", new HeapBytes(new byte[] {1, 2, 3}, false), memory);
      check(owned instanceof NativeResource r && r.type().equals("View"), "view owner");
      check(call(cells, "viewSum", owned).equals(6L), "view usable through its owner");
      check(view.get(ValueLayout.JAVA_LONG, 0) == 6, "the guest reads the struct's fields");
      check(view.get(ValueLayout.JAVA_LONG, 8) != 0, "native state copied with them");
      check(call(cells, "counterLive").equals(1L), "one live view");
      // The resource does not live in the guest's buffer, so nothing the guest does with that
      // buffer reaches it: overwriting the copy changes nothing, and passing the same buffer
      // again makes a second, independent resource instead of a second owner of the first.
      view.fill((byte) 0x5a);
      check(call(cells, "viewSum", owned).equals(6L), "the resource is out of the guest's reach");
      Object again = call(cells, "viewInit", new HeapBytes(new byte[] {7}, false), memory);
      check(call(cells, "viewSum", again).equals(7L) && call(cells, "viewSum", owned).equals(6L), "independent resources");
      check(call(cells, "counterLive").equals(2L), "two live views");
      check(call(cells, "viewRelease", again) == NativeVoid.INSTANCE, "second view released");
      // An owner is not memory for a new resource, and neither is a pointer without a size, a
      // read-only buffer, or one too small for the struct: the create is not called for those.
      rejects(cells, "viewInit", new HeapBytes(new byte[] {1}, false), owned);
      rejects(cells, "viewInit", new HeapBytes(new byte[] {1}, false), NativePointer.of(view.address()));
      rejects(cells, "viewInit", new HeapBytes(new byte[] {1}, false), NativeBytes.of(view, false));
      rejects(cells, "viewInit", new HeapBytes(new byte[] {1}, false), NativeBytes.of(view.asSlice(0, 15), true));
      check(call(cells, "counterLive").equals(1L), "refused creates create nothing");
      // A heap buffer takes the copy as well as native memory does.
      byte[] heapView = new byte[16];
      Object onHeap = call(cells, "viewInit", new HeapBytes(new byte[] {4, 4}, false), new HeapBytes(heapView, true));
      check(heapView[0] == 8, "struct copied into a heap buffer");
      check(call(cells, "viewRelease", onHeap) == NativeVoid.INSTANCE, "heap-buffered view released");
      try {
        call(cells, "viewInit", new HeapBytes(new byte[0], false), NativeBytes.of(arena.allocate(16), true));
        throw new AssertionError("a failed init returned");
      } catch (NativeStatusException failed) {
        check(failed.status() == -1, "negative status carried");
      }
      check(call(cells, "viewRelease", owned) == NativeVoid.INSTANCE, "view released");
      rejects(cells, "viewSum", owned);
      check(call(cells, "counterLive").equals(0L), "nothing left");
    }
    Object other = call(cells, "counterCreate", 1);
    INTEROP.invokeMember(other, "close");
    check(call(cells, "counterLive").equals(0L), "closed through the member");
    check(new String("ok".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8).equals("ok"), "sanity");
  }
}
