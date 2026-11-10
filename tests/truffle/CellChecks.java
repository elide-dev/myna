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
  public static void run(Function<String, NativeFunction> cells) throws Exception {
    // Host Java null bypasses guest interop values; exercise the public host call API.
    for (Object[] sample : new Object[][] {
        {"add", 1, null}, {"widen", 0, 0, 0, null}, {"scale", null, 2},
        {"peek", null}, {"strlen", null}, {"checksum", null}, {"fill", null, 1}}) {
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
    Object other = call(cells, "counterCreate", 1);
    INTEROP.invokeMember(other, "close");
    check(call(cells, "counterLive").equals(0L), "closed through the member");
    check(new String("ok".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8).equals("ok"), "sanity");
  }
}
