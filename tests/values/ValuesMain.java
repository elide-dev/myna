package fixture.values;

import dev.elide.myna.values.NativeValueTape;
import dev.elide.myna.values.SimdNativeValueTape;
import dev.elide.myna.values.ValueTape;
import org.graalvm.nativeimage.CurrentIsolate;
import org.graalvm.word.Pointer;
import org.graalvm.word.PointerBase;

public final class ValuesMain {
  static java.util.function.Consumer<NativeValueTape> compiledCheck;
  private static volatile String materialized;
  private static ValueTape.Utf8Key key(String text) { return ValueTape.Utf8Key.of(text); }
  private static final ValueTape.Utf8Key ID = key("id"), NAME = key("name"), ACTIVE = key("active"),
      SIGNED = key("signed"), RATIO = key("ratio"), ABSENT = key("absent"), NESTED = key("nested"),
      MISSING = key("missing");
  private static void check(boolean condition) {
    if (!condition) throw new IllegalStateException("Incorrect Rust/SVM value mapping");
  }
  private static SimdNativeValueTape javaBench, simdBench;
  public static long consume(PointerBase descriptor, int mode) {
    if (mode >= 10 && mode < 100) {
      SimdNativeValueTape reader;
      if (mode % 2 == 0) {
        if (javaBench == null) javaBench = new SimdNativeValueTape((Pointer) descriptor, Integer.MAX_VALUE);
        reader = javaBench;
      } else {
        if (simdBench == null) simdBench = new SimdNativeValueTape((Pointer) descriptor, 0);
        reader = simdBench;
      }
      reader.rebind((Pointer) descriptor);
      String value = reader.stringValue(0);
      materialized = value;
      return value.length();
    }
    var tape = new NativeValueTape((Pointer) descriptor);
    if (mode == 9) {
      tape.validate();
      check(tape.version() == 4 && tape.nodeCount() == 9 && tape.metadata(1) == 42);
      check(tape.referenceTarget(8) == 1);
      check(tape.find(1, key("costarring")) == 3);
      check(tape.find(1, key("liquid")) == 5);
      check(tape.kindAt(2) == ValueTape.STRING && tape.keyMatches(2, key("costarring")));
      var simd = new SimdNativeValueTape((Pointer) descriptor, 0);
      check(simd.simdThreshold() == 0);
      check(simd.stringValue(3).equals("café"));
      long size = simd.batchTextBytes();
      byte[] packed = new byte[Math.toIntExact(size)];
      long[] entries = new long[Math.toIntExact(tape.nodeCount() * 3)];
      check(simd.copyBatchText(packed, entries) == size);
      for (int i = 0; i < tape.nodeCount(); i++) {
        int encoding = SimdNativeValueTape.batchEncoding(entries[i * 3 + 2]);
        if (tape.tag(i) != ValueTape.STRING) { check(encoding == 0); continue; }
        var charset = encoding == 1 ? java.nio.charset.StandardCharsets.ISO_8859_1
            : java.nio.charset.StandardCharsets.UTF_16LE;
        String text = new String(packed, (int) entries[i * 3], (int) entries[i * 3 + 1] * encoding, charset);
        check(text.equals(tape.stringValue(i)));
      }
      boolean rejected = false;
      try { simd.copyBatchText(new byte[0], entries); } catch (IllegalArgumentException expected) { rejected = true; }
      check(rejected);
      // A failed descriptor bind must preserve all old fields.
      Pointer invalid = org.graalvm.nativeimage.StackValue.get(88);
      for (int offset = 0; offset < 88; offset += 8) invalid.writeLong(offset, ((Pointer) descriptor).readLong(offset));
      for (long[] mutation : new long[][]{{0, 99}, {4, 2}, {8, 0}, {16, -1}, {24, 0},
          {48, -1}, {56, 0}, {64, -1}, {72, 0}, {80, 1}}) {
        for (int offset = 0; offset < 88; offset += 8) invalid.writeLong(offset, ((Pointer) descriptor).readLong(offset));
        int offset = (int) mutation[0];
        if (offset == 0 || offset == 4) invalid.writeInt(offset, (int) mutation[1]);
        else invalid.writeLong(offset, mutation[1]);
        // This fixture has no external bytes; make a null byte pointer invalid explicitly.
        if (offset == 24) invalid.writeLong(32, 1);
        rejected = false;
        try { tape.rebind(invalid); } catch (IllegalArgumentException expected) { rejected = true; }
        check(rejected && tape.nodeCount() == 9 && tape.metadata(1) == 42);
      }
      Pointer scalar = org.graalvm.nativeimage.StackValue.get(24);
      scalar.writeLong(0, 0); scalar.writeLong(8, 0); scalar.writeLong(16, 1);
      for (int offset = 0; offset < 88; offset += 8) invalid.writeLong(offset, 0);
      invalid.writeInt(0, 3); invalid.writeWord(8, scalar); invalid.writeLong(16, 1);
      tape.rebind(invalid);
      check(tape.nodeCount() == 1 && tape.kindAt(0) == ValueTape.NULL && tape.metadata(0) == 0);
      tape.rebind((Pointer) descriptor); tape.validate();
      check(tape.stringValue(tape.find(1, key("liquid"))).equals("🦀"));
      rejected = false;
      try { new SimdNativeValueTape((Pointer) descriptor, -1); } catch (IllegalArgumentException expected) { rejected = true; }
      check(rejected);
      if (compiledCheck != null) {
        try { compiledCheck.accept(tape); } catch (Throwable failure) { failure.printStackTrace(); throw failure; }
      }
      return 9;
    }
    if (mode >= 5 && mode <= 8) {
      for (ValueTape reader : new ValueTape[] {tape, new SimdNativeValueTape((Pointer) descriptor)}) {
        reader.validate();
        check(reader.flags() == ValueTape.ALLOW_NON_FINITE);
        check(reader.doubleValue(1) == Double.POSITIVE_INFINITY);
        check(reader.doubleValue(2) == Double.NEGATIVE_INFINITY);
        check(Double.isNaN(reader.doubleValue(3)));
        check(reader.bits(3) == 0x7ff8000000000042L);
      }
      return 5;
    }
    if (mode == 100) {
      tape.validate();
      check(tape.length(0) == 6 && tape.byteCount() == 0);
      check(tape.stringValue(tape.find(0, NAME)).equals("C++ 🦀"));
      check(tape.stringValue(tape.find(0, key("escaped"))).equals("line\nend"));
      check(tape.numberText(tape.find(0, key("count"))).equals("18446744073709551615"));
      check(tape.numberText(tape.find(0, key("exact"))).equals("1.234567890123456789012345678901"));
      check(tape.numberText(tape.find(0, key("tiny"))).equals("1e-999999999999"));
      return 100;
    }
    if (mode == 2) {
      int rejected = 0;
      try { tape.stringValue(0); } catch (IllegalArgumentException expected) { rejected++; }
      try { new SimdNativeValueTape((Pointer) descriptor).stringValue(0); }
      catch (IllegalArgumentException expected) { rejected++; }
      check(rejected == 2);
      return 9;
    }
    if (mode == 0) {
      tape.validate();
      check(ValueApi.ValueView.SIZE == 56 && ValueApi.ValueNode.SIZE == 24);
      check(ValueApi.ValueNode.tag(ValueApi.ValueView.nodes(descriptor)) == ValueTape.OBJECT);
      check(ValueApi.ValueView.nodeCount(descriptor) == tape.nodeCount());
      check(ValueApi.ValueView.byteCount(descriptor) == tape.byteCount());
      check(tape.tag(0) == ValueTape.OBJECT && tape.length(0) == 8);
      check(tape.unsignedBits(tape.find(0, ID)) == -1L);
      long name = tape.find(0, NAME);
      check(tape.stringValue(name).equals("Myna\0🦀"));
      check(tape.stringEquals(name, key("Myna\0🦀")));
      check(tape.stringByte(name, 4) == 0);
      check(tape.booleanValue(tape.find(0, ACTIVE)));
      check(tape.signedValue(tape.find(0, SIGNED)) == Long.MIN_VALUE);
      check(Double.doubleToRawLongBits(tape.doubleValue(tape.find(0, RATIO))) == Long.MIN_VALUE);
      check(tape.tag(tape.find(0, ABSENT)) == ValueTape.NULL);
      check(tape.find(0, MISSING) == -1);
      var simd = new SimdNativeValueTape((Pointer) descriptor);
      String[] expected = {"", "1234567", "12345678", "123456789", "123456789abcdef", "123456789abcdef0", "123456789abcdef01", "🦀🦀🦀🦀", "Myna🦀\0".repeat(128)};
      long text = tape.find(0, ValueTape.Utf8Key.of("text"));
      long cursor = tape.first(text);
      for (String value : expected) {
        check(tape.stringValue(cursor).equals(value));
        check(simd.stringValue(cursor).equals(value));
        check(tape.stringEquals(cursor, ValueTape.Utf8Key.of(value)));
        cursor = tape.next(cursor);
      }
      check(cursor == tape.next(text));
    }
    long nested = tape.find(0, NESTED), sum = 0;
    for (long array = tape.first(nested); array < tape.next(nested); array = tape.next(array)) {
      for (long value = tape.first(array); value < tape.next(array); value = tape.next(value))
        sum += tape.signedValue(value);
    }
    return sum;
  }
  public static void main(String[] args) {
    var callback = ValueApi.Literals.consume.getFunctionPointer();
    var thread = CurrentIsolate.getCurrentThread();
    check(ValueApi.cppDrive(callback, thread) == 100);
    check(ValueApi.drive(callback, thread, 16, 0) == 96);
    check(ValueApi.drive(callback, thread, 1, 2) == 9);
    check(ValueApi.drive(callback, thread, 1, 3) == -999); // Reject ABI v1.
    check(ValueApi.drive(callback, thread, 1, 4) == -999); // Reject unknown flags.
    check(ValueApi.drive(callback, thread, 1, 5) == 5); // Opt-in YAML floats, both native readers.
    check(ValueApi.drive(callback, thread, 1, 6) == -999); // Non-finite without the flag.
    check(ValueApi.drive(callback, thread, 1, 7) == -999); // v2 cannot use the capability.
    check(ValueApi.drive(callback, thread, 1, 8) == -999); // Known flag plus an unknown bit.
    check(ValueApi.drive(callback, thread, 1, 9) == 9);
    check(ValueApi.drive(callback, thread, 0, 0) == 0);
    check(ValueApi.drive(callback, thread, 1000, 1) == 6000);
    if (args.length != 0 && args[0].equals("strings")) {
      int iterations = args.length > 1 ? Integer.parseInt(args[1]) : 10000;
      if (iterations <= 0) throw new IllegalArgumentException("positive iterations required");
      for (int mode = 10; mode < 58; mode++) {
        int which = (mode - 10) / 2;
        int size = new int[]{16, 32, 64, 128, 256, 512, 1024, 4096}[which % 8];
        int units = which / 8 == 1 ? size / 2 : size;
        check(ValueApi.drive(callback, thread, 1000, mode) == 1000L * units);
        long start = System.nanoTime();
        check(ValueApi.drive(callback, thread, iterations, mode) == (long) iterations * units);
        long elapsed = System.nanoTime() - start;
        System.out.println("materialize " + (new String[]{"ASCII ", "Unicode ", "Escaped "}[which / 8]) + size
            + " bytes " + (mode % 2 == 0 ? "Java" : "SIMD reader") + ": "
            + ((double) elapsed / iterations) + " ns/string (includes callback and volatile sink)");
      }
    } else if (args.length != 0) {
      int iterations = Integer.parseInt(args[0]);
      if (iterations <= 0) throw new IllegalArgumentException("positive iterations required");
      long start = System.nanoTime();
      long result = ValueApi.drive(callback, thread, iterations, 1);
      long elapsed = System.nanoTime() - start;
      check(result == 6L * iterations);
      System.out.println("encode + Rust-to-SVM entry + field lookup + nested sum: "
          + ((double) elapsed / iterations) + " ns/object (local smoke measurement)");
    }
    System.out.println("Serde Rust -> SVM value mapping passed");
  }
}
