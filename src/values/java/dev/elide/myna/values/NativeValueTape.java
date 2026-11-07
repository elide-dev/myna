package dev.elide.myna.values;

import org.graalvm.word.Pointer;
import org.graalvm.word.WordFactory;

/**
 * Direct SVM loads from a native-owned value tape, with no per-node native calls. A single reader
 * object covers the whole tree and may be rebound between messages. This reader is thread-confined.
 * The native owner must keep the descriptor (88 bytes for v4, 56 for v3, 40 for v2, aligned to 8),
 * nodes, and bytes live and immutable while constructing this reader; nodes, bytes, region table,
 * and referenced regions must remain so for its entire use. Extents must describe real allocations.
 * This class does not retain the owner.
 */
public class NativeValueTape extends ValueTape {
  private Pointer nodes;
  protected Pointer bytes;
  private Pointer regions;
  private long regionCount;

  private Pointer directory, metadata;
  private long directorySize, metadataSize;

  // rebind and resetDescriptor are final, perform only checked field stores, and
  // never invoke a subclass or publish this. javac cannot inspect the inherited body.
  @SuppressWarnings("this-escape")
  public NativeValueTape(Pointer descriptor) {
    super(1, 0);
    rebind(descriptor);
  }

  /**
   * Reuse this thread-confined reader. The previous owner can be released after successful
   * rebinding. Invalid descriptor fields leave the previous binding intact. The new allocations
   * must already be live and have trustworthy extents. This checks the descriptor, not every node;
   * validate() is an explicit scan.
   */
  public final void rebind(Pointer descriptor) {
    if (descriptor.isNull() || (descriptor.rawValue() & 7) != 0)
      throw new IllegalArgumentException("Invalid value descriptor pointer");
    int version = descriptor.readInt(0), flags = descriptor.readInt(4);
    if (version != 2 && version != 3 && version != 4)
      throw new IllegalArgumentException("Unsupported value ABI version");
    long nodeCount = descriptor.readLong(16), byteCount = descriptor.readLong(32);
    Pointer newNodes = descriptor.readWord(8), newBytes = descriptor.readWord(24);
    Pointer newRegions = version >= 3 ? descriptor.readWord(40) : WordFactory.nullPointer();
    long newRegionCount = version >= 3 ? descriptor.readLong(48) : 0;
    if (newRegionCount < 0
        || newRegionCount > 0xffffffffL
        || (newRegionCount > 0 && (newRegions.isNull() || (newRegions.rawValue() & 7) != 0)))
      throw new IllegalArgumentException("Invalid region table");
    if (newNodes.isNull() || (newNodes.rawValue() & 7) != 0 || (byteCount > 0 && newBytes.isNull()))
      throw new IllegalArgumentException("Invalid value storage pointer");
    Pointer newDirectory = version == 4 ? descriptor.readWord(56) : WordFactory.nullPointer();
    long newDirectorySize = version == 4 ? descriptor.readLong(64) : 0;
    Pointer newMetadata = version == 4 ? descriptor.readWord(72) : WordFactory.nullPointer();
    long newMetadataSize = version == 4 ? descriptor.readLong(80) : 0;
    if (newDirectorySize < 0
        || newDirectorySize > Long.MAX_VALUE / 8
        || (newDirectorySize > 0 && (newDirectory.isNull() || (newDirectory.rawValue() & 3) != 0))
        || (newMetadataSize != 0
            && (newMetadataSize != nodeCount
                || newMetadata.isNull()
                || (newMetadata.rawValue() & 3) != 0)))
      throw new IllegalArgumentException("Invalid ABI 4 extensions");
    resetDescriptor(nodeCount, byteCount, version, flags);
    nodes = newNodes;
    bytes = newBytes;
    regions = newRegions;
    regionCount = newRegionCount;
    directory = newDirectory;
    directorySize = newDirectorySize;
    metadata = newMetadata;
    metadataSize = newMetadataSize;
  }

  /** Write a temporary descriptor from the current binding; no owner descriptor is retained. */
  protected final void writeDescriptor(Pointer target) {
    target.writeInt(0, version());
    target.writeInt(4, flags());
    target.writeWord(8, nodes);
    target.writeLong(16, nodeCount());
    target.writeWord(24, bytes);
    target.writeLong(32, byteCount());
    target.writeWord(40, regions);
    target.writeLong(48, regionCount);
    target.writeWord(56, directory);
    target.writeLong(64, directorySize);
    target.writeWord(72, metadata);
    target.writeLong(80, metadataSize);
  }

  @Override
  protected long directoryCount() {
    return directorySize;
  }

  @Override
  protected int directoryHash(long index) {
    return directory.readInt(WordFactory.signed(index * 8));
  }

  @Override
  protected long directoryKeyOffset(long index) {
    return Integer.toUnsignedLong(directory.readInt(WordFactory.signed(index * 8 + 4)));
  }

  @Override
  protected int readMetadata(long node) {
    return metadataSize == 0 ? 0 : metadata.readInt(WordFactory.signed(node * 4));
  }

  @Override
  protected long regionLength(long region) {
    if (region == 0) return byteCount();
    if (region < 1 || region > regionCount)
      throw new IllegalArgumentException("Unknown text region");
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

  @Override
  protected void copyRegionBytes(long region, long start, byte[] output, int offset, int len) {
    Pointer source = regionAddress(region).add(WordFactory.signed(start));
    for (int i = 0; i < len; i++) output[offset + i] = source.readByte(i);
  }

  @Override
  protected byte readRegionByte(long region, long offset) {
    return regionAddress(region).readByte(WordFactory.signed(offset));
  }

  @Override
  protected int readInt(long offset) {
    return nodes.readInt(WordFactory.signed(offset));
  }

  @Override
  protected long readLong(long offset) {
    return nodes.readLong(WordFactory.signed(offset));
  }

  @Override
  protected byte readByte(long offset) {
    return bytes.readByte(WordFactory.signed(offset));
  }
}
