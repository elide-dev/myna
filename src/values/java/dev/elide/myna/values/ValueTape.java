// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package dev.elide.myna.values;

import java.nio.charset.StandardCharsets;

/**
 * Read-only access to Myna value ABI v2, v3, and v4. Node indexes replace per-value wrappers. The
 * caller must keep the backing storage alive and immutable for every read. Implementations must
 * bounds-check their backing allocation at construction.
 */
public abstract class ValueTape {
  public static final int ABI_VERSION = 3;
  public static final int ABI_VERSION_V4 = 4;

  /** Descriptor flag permitting non-finite binary F64 values, not decimal tokens. */
  public static final int ALLOW_NON_FINITE = 1;

  public static final int NULL = 0,
      BOOL = 1,
      I64 = 2,
      U64 = 3,
      F64 = 4,
      STRING = 5,
      ARRAY = 6,
      OBJECT = 7,
      DECIMAL = 8,
      REF = 9;
  public static final int REGION_TEXT = 0x100, KIND_MASK = 0xff;
  public static final int NODE_SIZE = 24;
  private long nodeCount;
  private long byteCount;
  private int version;
  private int flags;

  protected ValueTape(long nodeCount, long byteCount) {
    this(nodeCount, byteCount, ABI_VERSION);
  }

  protected ValueTape(long nodeCount, long byteCount, int version) {
    this(nodeCount, byteCount, version, 0);
  }

  protected ValueTape(long nodeCount, long byteCount, int version, int flags) {
    resetDescriptor(nodeCount, byteCount, version, flags);
  }

  /** Thread-confined rebinding: validate all descriptor fields before committing. */
  protected final void resetDescriptor(long nodeCount, long byteCount, int version, int flags) {
    if (version != 2 && version != ABI_VERSION && version != ABI_VERSION_V4)
      throw new IllegalArgumentException("Unsupported value ABI");
    if ((flags & ~ALLOW_NON_FINITE) != 0 || (version == 2 && flags != 0))
      throw new IllegalArgumentException("Unsupported value descriptor flags");
    if (nodeCount <= 0 || nodeCount > Long.MAX_VALUE / NODE_SIZE || byteCount < 0)
      throw new IllegalArgumentException("Invalid value storage extent");
    this.version = version;
    this.flags = flags;
    this.nodeCount = nodeCount;
    this.byteCount = byteCount;
  }

  protected abstract int readInt(long nodeByteOffset);

  protected abstract long readLong(long nodeByteOffset);

  protected abstract byte readByte(long byteOffset);

  public final long nodeCount() {
    return nodeCount;
  }

  public final long byteCount() {
    return byteCount;
  }

  public final int flags() {
    return flags;
  }

  private long offset(long node) {
    if (node < 0 || node >= nodeCount) throw new IndexOutOfBoundsException("Node index");
    return node * NODE_SIZE;
  }

  public final int version() {
    return version;
  }

  /** Checked kind accessor with its own bounds and tag validation. */
  public final int kindAt(long node) {
    if (node < 0 || node >= nodeCount) throw new IndexOutOfBoundsException("Node index");
    int kind = readInt(node * NODE_SIZE) & KIND_MASK;
    if (kind > (version == 4 ? REF : version == 2 ? OBJECT : DECIMAL))
      throw new IllegalArgumentException("Unknown value tag");
    return kind;
  }

  /** Explicit reference access; never expands aliases or allocates a value tree. */
  public final long referenceTarget(long node) {
    require(node, REF);
    long target = bits(node);
    if (version != 4 || target < 0 || target >= node || next(target) > node)
      throw new IllegalArgumentException("Reference must target a completed earlier subtree");
    return target;
  }

  protected long directoryCount() {
    return 0;
  }

  protected int directoryHash(long index) {
    throw new IllegalArgumentException("No directory");
  }

  protected long directoryKeyOffset(long index) {
    throw new IllegalArgumentException("No directory");
  }

  protected int readMetadata(long node) {
    return 0;
  }

  public final int metadata(long node) {
    offset(node);
    return version == 4 ? readMetadata(node) : 0;
  }

  public final int tag(long node) {
    return storageTag(node) & KIND_MASK;
  }

  private int storageTag(long node) {
    int raw = readInt(offset(node));
    if (version == 2 && raw == STRING && length(node) > 16) return raw | REGION_TEXT;
    return raw;
  }

  public final boolean isInlineText(long node) {
    int raw = storageTag(node);
    return (raw == STRING || raw == DECIMAL) && length(node) <= 16;
  }

  protected long regionLength(long region) {
    if (region != 0) throw new IllegalArgumentException("Unknown text region");
    return byteCount;
  }

  protected byte readRegionByte(long region, long offset) {
    if (region != 0) throw new IllegalArgumentException("Unknown text region");
    return readByte(offset);
  }

  private long textRegion(long node) {
    return version == 2 ? 0 : bits(node) >>> 32;
  }

  public final long length(long node) {
    return Integer.toUnsignedLong(readInt(offset(node) + 4));
  }

  public final long bits(long node) {
    return readLong(offset(node) + 8);
  }

  public final long next(long node) {
    long at = offset(node);
    if (isInlineText(node)) return node + 1;
    long next = readLong(at + 16);
    if (next <= node || next > nodeCount)
      throw new IllegalArgumentException("Invalid subtree extent");
    return next;
  }

  private void require(long node, int expected) {
    if (tag(node) != expected) throw new IllegalArgumentException("Unexpected value type");
  }

  public final boolean booleanValue(long node) {
    require(node, BOOL);
    long value = bits(node);
    if (value != 0 && value != 1) throw new IllegalArgumentException("Invalid boolean");
    return value != 0;
  }

  public final long signedValue(long node) {
    require(node, I64);
    return bits(node);
  }

  /** Preserves all 64 bits; use Long.compareUnsigned/toUnsignedString as appropriate. */
  public final long unsignedBits(long node) {
    require(node, U64);
    return bits(node);
  }

  public final double doubleValue(long node) {
    require(node, F64);
    return Double.longBitsToDouble(bits(node));
  }

  private long stringOffset(long node) {
    require(node, STRING);
    return textOffset(node);
  }

  private long textOffset(long node) {
    long len = length(node);
    if (isInlineText(node)) return -1;
    int raw = storageTag(node);
    if (raw != (STRING | REGION_TEXT) && raw != (DECIMAL | REGION_TEXT))
      throw new IllegalArgumentException("Invalid text storage tag");
    long start = version == 2 ? bits(node) : bits(node) & 0xffffffffL;
    long extent = regionLength(textRegion(node));
    if (start < 0 || len > extent || start > extent - len)
      throw new IllegalArgumentException("Invalid text region extent");
    return start;
  }

  private byte textByte(long node, long start, long index) {
    if (start >= 0) return readRegionByte(textRegion(node), start + index);
    long word = readLong(offset(node) + 8 + (index >>> 3) * 8);
    return (byte) (word >>> ((index & 7) * 8));
  }

  public final byte stringByte(long node, long index) {
    long start = stringOffset(node);
    if (index < 0 || index >= length(node))
      throw new IndexOutOfBoundsException("String byte index");
    return textByte(node, start, index);
  }

  /** Copy into caller-owned reusable UTF-8 storage, without allocating. */
  public final int copyStringUtf8(long node, byte[] output, int outputOffset) {
    long start = stringOffset(node);
    int len = Math.toIntExact(length(node));
    java.util.Objects.checkFromIndexSize(outputOffset, len, output.length);
    if (start >= 0) copyRegionBytes(textRegion(node), start, output, outputOffset, len);
    else for (int i = 0; i < len; i++) output[outputOffset + i] = textByte(node, start, i);
    return len;
  }

  /** Allocates only when the caller explicitly asks for a Java String. */
  public final String stringValue(long node) {
    require(node, STRING);
    return textValue(node);
  }

  private String textValue(long node) {
    long start = textOffset(node);
    int len = Math.toIntExact(length(node));
    if (len == 0) return "";
    if (start >= 0) return decodeExternalUtf8(textRegion(node), start, len);
    byte[] data = new byte[len];
    for (int i = 0; i < len; i++) data[i] = textByte(node, start, i);
    return decodeUtf8(data);
  }

  protected String decodeExternalUtf8(long region, long start, int len) {
    byte[] data = new byte[len];
    copyRegionBytes(region, start, data, 0, len);
    return decodeUtf8(data);
  }

  /** Called only after source and destination extents have been checked. */
  protected void copyRegionBytes(long region, long start, byte[] output, int offset, int len) {
    for (int i = 0; i < len; i++) output[offset + i] = readRegionByte(region, start + i);
  }

  private static String decodeUtf8(byte[] data) {
    boolean ascii = true;
    for (byte value : data) {
      if (value < 0) {
        ascii = false;
        break;
      }
    }
    if (ascii) return new String(data, StandardCharsets.ISO_8859_1);
    // REPORT malformed input instead of silently substituting replacement chars.
    try {
      return StandardCharsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(data)).toString();
    } catch (java.nio.charset.CharacterCodingException error) {
      throw new IllegalArgumentException("Invalid UTF-8", error);
    }
  }

  /** Compare to a reusable UTF-8 key without allocating or decoding a String. */
  public final boolean stringEquals(long node, byte[] utf8) {
    long start = stringOffset(node);
    if (length(node) != utf8.length) return false;
    for (int i = 0; i < utf8.length; i++) if (textByte(node, start, i) != utf8[i]) return false;
    return true;
  }

  /** A prepared key avoids repacking short field names on every lookup. */
  public static final class Utf8Key {
    private final byte[] bytes;
    private final long low, high;
    private final int hash;

    private Utf8Key(byte[] bytes) {
      this.bytes = bytes;
      long lo = 0, hi = 0;
      for (int i = 0; i < Math.min(16, bytes.length); i++) {
        if (i < 8) lo |= (bytes[i] & 255L) << (i * 8);
        else hi |= (bytes[i] & 255L) << ((i - 8) * 8);
      }
      low = lo;
      high = hi;
      hash = hashUtf8(bytes);
    }

    public int byteLength() {
      return bytes.length;
    }

    public long lowWord() {
      return low;
    }

    public long highWord() {
      return high;
    }

    public int hash() {
      return hash;
    }

    public byte byteAt(int index) {
      return bytes[index];
    }

    public static Utf8Key of(String text) {
      try {
        var encoded = StandardCharsets.UTF_8.newEncoder().encode(java.nio.CharBuffer.wrap(text));
        byte[] bytes = new byte[encoded.remaining()];
        encoded.get(bytes);
        return new Utf8Key(bytes);
      } catch (java.nio.charset.CharacterCodingException error) {
        throw new IllegalArgumentException("Unpaired UTF-16 surrogate", error);
      }
    }
  }

  public static int hashUtf8(byte[] bytes) {
    int hash = 0x811c9dc5;
    for (byte b : bytes) hash = (hash ^ (b & 255)) * 0x01000193;
    return hash;
  }

  /** Prepared-key comparison including type/extent checks, without materialization. */
  public final boolean keyMatches(long node, Utf8Key key) {
    java.util.Objects.requireNonNull(key);
    long at = offset(node);
    if ((readInt(at) & KIND_MASK) != STRING)
      throw new IllegalArgumentException("Expected string key");
    if (Integer.toUnsignedLong(readInt(at + 4)) != key.bytes.length) return false;
    if (!isInlineText(node)) return stringEquals(node, key.bytes);
    return (readLong(at + 8) & mask(key.bytes.length)) == key.low
        && (readLong(at + 16) & mask(Math.max(0, key.bytes.length - 8))) == key.high;
  }

  private static long mask(int bytes) {
    return bytes >= 8 ? -1L : (1L << (bytes * 8)) - 1;
  }

  public final boolean stringEquals(long node, Utf8Key key) {
    require(node, STRING);
    int len = key.bytes.length;
    if (length(node) != len) return false;
    if (!isInlineText(node)) return stringEquals(node, key.bytes);
    long at = offset(node);
    return (readLong(at + 8) & mask(len)) == key.low
        && (readLong(at + 16) & mask(Math.max(0, len - 8))) == key.high;
  }

  private static final java.util.regex.Pattern JSON_NUMBER =
      java.util.regex.Pattern.compile("-?(?:0|[1-9][0-9]*)(?:\\.[0-9]+)?(?:[eE][+-]?[0-9]+)?");

  /** Exact token text; huge exponents remain representable without decimal allocation. */
  public final String numberText(long node) {
    require(node, DECIMAL);
    String text = textValue(node);
    if (!JSON_NUMBER.matcher(text).matches())
      throw new IllegalArgumentException("Invalid JSON number token");
    return text;
  }

  /** Explicit conversion; BigDecimal may reject exponents outside its own range. */
  public final java.math.BigDecimal decimalValue(long node) {
    return new java.math.BigDecimal(numberText(node));
  }

  public final long decimalLongExact(long node) {
    return decimalValue(node).longValueExact();
  }

  /** First child/key, or the container's end index if empty. Iterate with next(). */
  public final long first(long node) {
    int tag = tag(node);
    if (tag != ARRAY && tag != OBJECT) throw new IllegalArgumentException("Expected container");
    return node + 1;
  }

  /** Linear lookup, preserving first-match semantics for duplicate keys. */
  public final long find(long object, byte[] utf8) {
    java.util.Objects.requireNonNull(utf8);
    return findKey(object, utf8, null);
  }

  public final long find(long object, Utf8Key key) {
    return findKey(object, null, java.util.Objects.requireNonNull(key));
  }

  private long findKey(long object, byte[] utf8, Utf8Key keyValue) {
    require(object, OBJECT);
    long end = next(object);
    if (version == 4 && bits(object) != 0) {
      long start = bits(object) - 1, count = length(object);
      if (start < 0 || start > directoryCount() || count > directoryCount() - start)
        throw new IllegalArgumentException("Invalid directory extent");
      int hash = keyValue == null ? hashUtf8(utf8) : keyValue.hash;
      long low = start, high = start + count;
      while (low < high) {
        long mid = low + (high - low) / 2;
        if (Integer.compareUnsigned(directoryHash(mid), hash) < 0) low = mid + 1;
        else high = mid;
      }
      for (long entry = low; entry < start + count && directoryHash(entry) == hash; entry++) {
        long key = object + directoryKeyOffset(entry);
        if (key <= object
            || key >= end - 1
            || tag(key) != STRING
            || next(key) != key + 1
            || next(key + 1) > end) throw new IllegalArgumentException("Invalid directory entry");
        if (keyValue == null ? stringEquals(key, utf8) : keyMatches(key, keyValue)) return key + 1;
      }
      return -1;
    }
    for (long key = object + 1; key < end; ) {
      long value = next(key);
      if (tag(key) != STRING || value != key + 1 || value >= end)
        throw new IllegalArgumentException("Invalid object entry");
      long following = next(value);
      if (following > end) throw new IllegalArgumentException("Entry escapes object");
      if (keyValue == null ? stringEquals(key, utf8) : stringEquals(key, keyValue)) return value;
      key = following;
    }
    return -1;
  }

  /**
   * Optional O(nodes + number text bytes) structural and number validation. Memory extents must
   * already be trustworthy; this cannot validate arbitrary native pointers or their owner.
   * Rust-produced tapes need not repeat this scan on each crossing.
   */
  private void validateDirectory(long object) {
    long start = bits(object) - 1, count = length(object), end = next(object);
    if (start < 0 || start > directoryCount() || count > directoryCount() - start)
      throw new IllegalArgumentException("Invalid directory extent");
    for (long i = start; i < start + count; i++) {
      long key = object + directoryKeyOffset(i);
      if (key <= object || key >= end - 1 || tag(key) != STRING)
        throw new IllegalArgumentException("Invalid directory key");
      if (i > start) {
        int order = Integer.compareUnsigned(directoryHash(i - 1), directoryHash(i));
        if (order > 0 || (order == 0 && directoryKeyOffset(i - 1) >= directoryKeyOffset(i)))
          throw new IllegalArgumentException("Unsorted or duplicate directory entry");
      }
    }
    for (long key = object + 1; key < end; key = next(key + 1)) {
      if (tag(key) != STRING || key + 1 >= end || next(key + 1) > end)
        throw new IllegalArgumentException("Invalid object entry");
      int hash = 0x811c9dc5;
      for (long b = 0; b < length(key); b++)
        hash = (hash ^ (stringByte(key, b) & 255)) * 0x01000193;
      long low = start, high = start + count, relative = key - object;
      while (low < high) {
        long mid = low + (high - low) / 2;
        int order = Integer.compareUnsigned(directoryHash(mid), hash);
        if (order < 0 || (order == 0 && directoryKeyOffset(mid) < relative)) low = mid + 1;
        else high = mid;
      }
      if (low == start + count || directoryHash(low) != hash || directoryKeyOffset(low) != relative)
        throw new IllegalArgumentException("Directory does not match object keys");
    }
  }

  public final void validate() {
    if (next(0) != nodeCount) throw new IllegalArgumentException("Multiple root values");
    for (long node = 0; node < nodeCount; node++) {
      int tag = tag(node);
      long end = next(node), len = length(node), payload = bits(node);
      if (tag < NULL || tag > (version == 4 ? REF : DECIMAL) || (version == 2 && tag == DECIMAL))
        throw new IllegalArgumentException("Unknown value tag");
      int raw = storageTag(node);
      if (raw != tag && raw != (tag | REGION_TEXT))
        throw new IllegalArgumentException("Unknown storage flags");
      if ((raw & REGION_TEXT) != 0 && tag != STRING && tag != DECIMAL)
        throw new IllegalArgumentException("Non-text region reference");
      if (tag != ARRAY && tag != OBJECT) {
        if (end != node + 1 || (tag != STRING && tag != DECIMAL && len != 0))
          throw new IllegalArgumentException("Invalid scalar shape");
        switch (tag) {
          case NULL -> {
            if (payload != 0) throw new IllegalArgumentException("Invalid null");
          }
          case BOOL -> booleanValue(node);
          case F64 -> {
            if ((flags & ALLOW_NON_FINITE) == 0 && !Double.isFinite(doubleValue(node)))
              throw new IllegalArgumentException("Non-finite number");
          }
          case STRING -> stringOffset(node);
          case DECIMAL -> numberText(node);
          case REF -> referenceTarget(node);
          default -> {}
        }
      } else {
        if (payload != 0 && (version != 4 || tag != OBJECT))
          throw new IllegalArgumentException("Invalid container payload");
        if (payload != 0) validateDirectory(node);
        long cursor = node + 1, count = 0;
        while (cursor < end) {
          if (tag == OBJECT) {
            if (tag(cursor) != STRING || next(cursor) != cursor + 1)
              throw new IllegalArgumentException("Invalid object key");
            cursor++;
            if (cursor >= end) throw new IllegalArgumentException("Missing object value");
          }
          cursor = next(cursor);
          if (cursor > end) throw new IllegalArgumentException("Child escapes container");
          count++;
        }
        if (count != len) throw new IllegalArgumentException("Incorrect container length");
      }
    }
  }
}
