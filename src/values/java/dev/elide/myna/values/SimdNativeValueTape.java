package dev.elide.myna.values;

import org.graalvm.nativeimage.PinnedObject;
import org.graalvm.nativeimage.StackValue;
import org.graalvm.nativeimage.c.function.CFunction;
import org.graalvm.nativeimage.c.type.CShortPointer;
import org.graalvm.word.Pointer;
import org.graalvm.word.WordFactory;

/**
 * Optional simdutf transcoding for bulk string materialization. Link the Rust myna-values bridge
 * and the C++ runtime when using this class. Plain NativeValueTape remains usable without native
 * helper symbols.
 */
public final class SimdNativeValueTape extends NativeValueTape {
  /** Conservative crossover; tune with the native fixture on deployment hardware. */
  public static final int SIMD_THRESHOLD = 256;

  private final int simdThreshold;

  public SimdNativeValueTape(Pointer descriptor) {
    this(descriptor, SIMD_THRESHOLD);
  }

  /** Select a crossover in UTF-8 bytes; zero uses SIMD for every nonempty external string. */
  public SimdNativeValueTape(Pointer descriptor, int threshold) {
    super(descriptor);
    if (threshold < 0) throw new IllegalArgumentException("Negative SIMD threshold");
    simdThreshold = threshold;
  }

  public int simdThreshold() {
    return simdThreshold;
  }

  // Normal transition: input length is unbounded and may delay a safepoint.
  @CFunction("myna_utf8_to_utf16_v2")
  private static native long transcode(
      Pointer input, long length, CShortPointer output, long capacity);

  @CFunction("myna_tape_text_v1")
  private static native long batchText(
      Pointer descriptor, Pointer output, long capacity, Pointer table, long tableCapacity);

  /** Required caller-buffer size for batch text conversion. ABI 3 and 4 only. */
  public long batchTextBytes() {
    Pointer descriptor = StackValue.get(88);
    writeDescriptor(descriptor);
    long result = batchText(descriptor, WordFactory.nullPointer(), 0, WordFactory.nullPointer(), 0);
    if (result < 0) throw new IllegalArgumentException("Invalid tape text: " + result);
    return result;
  }

  /**
   * Convert every string/key and decimal token into caller-owned packed storage. Each node uses
   * three longs in table: byte offset, code-unit length, and packed native-endian encoding/reserved
   * fields. Use batchEncoding to read the latter. Encoding 0 is non-text, 1 Latin-1, 2 UTF-16LE.
   * References are not expanded. Errors leave both arrays unchanged. No output buffer allocation
   * occurs here.
   */
  public long copyBatchText(byte[] output, long[] table) {
    java.util.Objects.requireNonNull(output);
    java.util.Objects.requireNonNull(table);
    if (nodeCount() > table.length / 3) throw new IllegalArgumentException("Batch table too small");
    Pointer descriptor = StackValue.get(88);
    writeDescriptor(descriptor);
    long result;
    try (var out = PinnedObject.create(output);
        var entries = PinnedObject.create(table)) {
      result =
          batchText(
              descriptor,
              output.length == 0 ? WordFactory.nullPointer() : out.addressOfArrayElement(0),
              output.length,
              entries.addressOfArrayElement(0),
              table.length / 3);
    }
    if (result < 0) throw new IllegalArgumentException("Invalid batch text or capacity: " + result);
    return result;
  }

  public static int batchEncoding(long packed) {
    return java.nio.ByteOrder.nativeOrder() == java.nio.ByteOrder.LITTLE_ENDIAN
        ? (int) packed
        : (int) (packed >>> 32);
  }

  @Override
  protected String decodeExternalUtf8(long region, long start, int len) {
    if (len == 0 || len < simdThreshold) return super.decodeExternalUtf8(region, start, len);
    // One UTF-16 unit per UTF-8 byte is a safe bound. simdutf validates input
    // during conversion; neither pointer can escape this pinned interval.
    char[] chars = new char[len];
    long written;
    try (var pinned = PinnedObject.create(chars)) {
      written =
          transcode(
              regionAddress(region).add(WordFactory.signed(start)),
              len,
              pinned.addressOfArrayElement(0),
              len);
    }
    if (written < 0 || written > len)
      throw new IllegalArgumentException("Invalid UTF-8 or output extent");
    return new String(chars, 0, (int) written);
  }
}
