package dev.elide.myna.values;

import org.graalvm.word.Pointer;
import org.graalvm.word.WordFactory;

/**
 * Direct SVM loads from a native-owned value tape, with no per-node native calls.
 * A single reader object covers the whole tree. The native owner must keep the
 * descriptor (56 bytes for v3, 40 for v2, aligned to 8), nodes, and bytes live and immutable while
 * constructing this reader; nodes, bytes, region table, and referenced regions
 * must remain so for its entire use.
 * Extents must describe real allocations. This class does not retain the owner.
 */
public class NativeValueTape extends ValueTape {
  private final Pointer nodes;
  protected final Pointer bytes;
  private final Pointer regions;
  private final long regionCount;

  public NativeValueTape(Pointer descriptor) {
    this(checked(descriptor), descriptor.readLong(16), descriptor.readLong(32));
  }

  private NativeValueTape(Pointer descriptor, long nodeCount, long byteCount) {
    super(nodeCount, byteCount, descriptor.readInt(0));
    boolean v3 = descriptor.readInt(0) == ABI_VERSION;
    regions = v3 ? descriptor.readWord(40) : WordFactory.nullPointer();
    regionCount = v3 ? descriptor.readLong(48) : 0;
    if (regionCount < 0 || regionCount > 0xffffffffL || (regionCount > 0 && (regions.isNull() || (regions.rawValue() & 7) != 0)))
      throw new IllegalArgumentException("Invalid region table");
    nodes = descriptor.readWord(8);
    bytes = descriptor.readWord(24);
    if (nodes.isNull() || (nodeCount > 0 && (nodes.rawValue() & 7) != 0)
        || (byteCount > 0 && bytes.isNull()))
      throw new IllegalArgumentException("Invalid value storage pointer");
  }

  private static Pointer checked(Pointer descriptor) {
    if (descriptor.isNull() || (descriptor.rawValue() & 7) != 0)
      throw new IllegalArgumentException("Invalid value descriptor pointer");
    if ((descriptor.readInt(0) != ABI_VERSION && descriptor.readInt(0) != 2) || descriptor.readInt(4) != 0)
      throw new IllegalArgumentException("Unsupported value ABI version or flags");
    return descriptor;
  }

  @Override protected long regionLength(long region) {
    if (region == 0) return byteCount();
    if (region < 1 || region > regionCount) throw new IllegalArgumentException("Unknown text region");
    long len = regions.readLong(WordFactory.signed((region - 1) * 16 + 8));
    if (len < 0) throw new IllegalArgumentException("Invalid region length");
    return len;
  }
  protected final Pointer regionAddress(long region) {
    long len = regionLength(region);
    Pointer address = region == 0 ? bytes : regions.readWord(WordFactory.signed((region - 1) * 16));
    if (len > 0 && address.isNull()) throw new IllegalArgumentException("Null text region");
    return address;
  }
  @Override protected byte readRegionByte(long region, long offset) {
    return regionAddress(region).readByte(WordFactory.signed(offset));
  }
  @Override protected int readInt(long offset) { return nodes.readInt(WordFactory.signed(offset)); }
  @Override protected long readLong(long offset) { return nodes.readLong(WordFactory.signed(offset)); }
  @Override protected byte readByte(long offset) { return bytes.readByte(WordFactory.signed(offset)); }
}
