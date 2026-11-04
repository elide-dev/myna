package dev.elide.myna.values;

import org.graalvm.nativeimage.PinnedObject;
import org.graalvm.nativeimage.c.function.CFunction;
import org.graalvm.nativeimage.c.type.CShortPointer;
import org.graalvm.word.Pointer;
import org.graalvm.word.WordFactory;

/**
 * Optional simdutf transcoding for bulk string materialization. Link the Rust
 * myna-values bridge and the C++ runtime when using this class. Plain
 * NativeValueTape remains usable without native helper symbols.
 */
public final class SimdNativeValueTape extends NativeValueTape {
  /** Conservative crossover; tune with the native fixture on deployment hardware. */
  public static final int SIMD_THRESHOLD = 256;

  public SimdNativeValueTape(Pointer descriptor) { super(descriptor); }

  // Normal transition: input length is unbounded and may delay a safepoint.
  @CFunction("myna_utf8_to_utf16_v2")
  private static native long transcode(Pointer input, long length, CShortPointer output, long capacity);

  @Override protected String decodeExternalUtf8(long region, long start, int len) {
    if (len < SIMD_THRESHOLD) return super.decodeExternalUtf8(region, start, len);
    // One UTF-16 unit per UTF-8 byte is a safe bound. simdutf validates input
    // during conversion; neither pointer can escape this pinned interval.
    char[] chars = new char[len];
    long written;
    try (var pinned = PinnedObject.create(chars)) {
      written = transcode(regionAddress(region).add(WordFactory.signed(start)), len, pinned.addressOfArrayElement(0), len);
    }
    if (written < 0 || written > len) throw new IllegalArgumentException("Invalid UTF-8 or output extent");
    return new String(chars, 0, (int) written);
  }
}
