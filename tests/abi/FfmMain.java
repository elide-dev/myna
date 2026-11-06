import dev.elide.myna.generated.MynaFFM;
import dev.elide.myna.generated.MynaNativeJNI;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.util.concurrent.atomic.AtomicReference;

/** The same generated, typed FFM callsite runs on a JVM and as an AOT Native Image. */
public final class FfmMain {
  static volatile byte[] allocation;
  @FunctionalInterface interface Action { void run() throws Throwable; }

  private static void rejects(Class<? extends Throwable> type, Action action) throws Throwable {
    try { action.run(); } catch (Throwable failure) {
      if (type.isInstance(failure)) return;
      throw failure;
    }
    throw new AssertionError("expected " + type.getName());
  }

  private static long collections() {
    return java.lang.management.ManagementFactory.getGarbageCollectorMXBeans().stream()
        .mapToLong(bean -> Math.max(0, bean.getCollectionCount())).sum();
  }

  private static void criticalScopesAndGc(MethodHandle critical, MethodHandle normal,
      MemorySegment value, String libraryPath) throws Throwable {
    var heap = MemorySegment.ofArray(new long[] {42L});
    rejects(IllegalArgumentException.class, () -> { long ignored = (long) critical.invokeExact(heap); });
    var closedArena = Arena.ofConfined();
    var closed = closedArena.allocate(ValueLayout.JAVA_LONG);
    closedArena.close();
    rejects(IllegalStateException.class, () -> { long ignored = (long) critical.invokeExact(closed); });
    var failure = new AtomicReference<Throwable>();
    var otherThread = new Thread(() -> {
      try {
        rejects(WrongThreadException.class, () -> { long ignored = (long) critical.invokeExact(value); });
      } catch (Throwable error) { failure.set(error); }
    });
    otherThread.start(); otherThread.join();
    if (failure.get() != null) throw failure.get();

    MethodHandle scoped;
    try (var library = Arena.ofConfined(); var shared = Arena.ofShared()) {
      scoped = MynaFFM.inspect(SymbolLookup.libraryLookup(java.nio.file.Path.of(libraryPath), library), true);
      var accessible = shared.allocateFrom(ValueLayout.JAVA_LONG, 42L);
      if ((long) scoped.invokeExact(accessible) != 42) throw new AssertionError("scoped critical call");
      // The argument is accessible by both threads; only the symbol's arena is confined.
      var libraryThread = new Thread(() -> {
        try {
          rejects(WrongThreadException.class, () -> { long ignored = (long) scoped.invokeExact(accessible); });
        } catch (Throwable error) { failure.set(error); }
      });
      libraryThread.start(); libraryThread.join();
      if (failure.get() != null) throw failure.get();
    }
    // FFM must reject the closed symbol scope before entering native code.
    rejects(IllegalStateException.class, () -> { long ignored = (long) scoped.invokeExact(value); });

    long before = collections();
    var collector = new Thread(() -> {
      try {
        for (int i = 0; i < 128; i++) {
          allocation = new byte[65536];
          if (i % 8 == 0) System.gc();
        }
      } catch (Throwable error) { failure.set(error); }
    });
    collector.start();
    for (long i = 0; i < 1_000_000 || collector.isAlive(); i++) {
      value.set(ValueLayout.JAVA_LONG, 0, i);
      if ((long) critical.invokeExact(value) != i || (long) normal.invokeExact(value) != i)
        throw new AssertionError("normal/critical FFM under GC");
    }
    collector.join();
    if (failure.get() != null) throw failure.get();
    if (collections() <= before) throw new AssertionError("no GC observed during FFM stress");
    value.set(ValueLayout.JAVA_LONG, 0, 42L);
  }

  public static void main(String[] args) throws Throwable {
    System.load(args[0]);
    try (var arena = Arena.ofConfined()) {
      var value = arena.allocate(ValueLayout.JAVA_LONG);
      value.set(ValueLayout.JAVA_LONG, 0, 42L);
      for (int i = 0; i < 100; i++) {
        if (MynaFFM.inspect(value) != 42) throw new AssertionError("typed FFM result");
      }
      if (MynaFFM.reducer().address() == 0) throw new AssertionError("no-argument pointer return");
      var independentlyBound = MynaFFM.inspect(java.lang.foreign.SymbolLookup.loaderLookup());
      if ((long) independentlyBound.invokeExact(value) != 42)
        throw new AssertionError("SymbolLookup factory compatibility");
      var critical = MynaFFM.inspect(java.lang.foreign.SymbolLookup.loaderLookup(), true);
      if ((long) critical.invokeExact(value) != 42) throw new AssertionError("critical leaf");
      var normal = MynaFFM.inspect(java.lang.foreign.SymbolLookup.loaderLookup(), false);
      if ((long) normal.invokeExact(value) != 42) throw new AssertionError("normal downcall");
      criticalScopesAndGc(critical, normal, value, args[0]);
      try {
        MynaFFM.forEach(java.lang.foreign.SymbolLookup.loaderLookup(), true);
        throw new AssertionError("accepted critical callback-capable import");
      } catch (IllegalArgumentException expected) { }
      // Records by value: FFM lowers the C ABI itself, unlike Native Image @CFunction.
      var triple = arena.allocate(MynaFFM.Triple_LAYOUT);
      triple.set(ValueLayout.JAVA_LONG, MynaFFM.Triple_a_OFFSET, 1L);
      triple.set(ValueLayout.JAVA_LONG, MynaFFM.Triple_b_OFFSET, 2L);
      triple.set(ValueLayout.JAVA_LONG, MynaFFM.Triple_c_OFFSET, 3L);
      var rotated = MynaFFM.rotate(arena, triple, 1);
      if (rotated.get(ValueLayout.JAVA_LONG, MynaFFM.Triple_a_OFFSET) != 2
          || rotated.get(ValueLayout.JAVA_LONG, MynaFFM.Triple_c_OFFSET) != 1)
        throw new AssertionError("by-value FFM record");
      var criticalRotate = MynaFFM.rotate(SymbolLookup.loaderLookup(), true);
      var criticalRotated = (MemorySegment) criticalRotate.invokeExact((SegmentAllocator) arena, triple, 1);
      if (criticalRotated.get(ValueLayout.JAVA_LONG, MynaFFM.Triple_a_OFFSET) != 2
          || criticalRotated.get(ValueLayout.JAVA_LONG, MynaFFM.Triple_b_OFFSET) != 3
          || criticalRotated.get(ValueLayout.JAVA_LONG, MynaFFM.Triple_c_OFFSET) != 1)
        throw new AssertionError("critical memory-class record");
      var pair = arena.allocate(MynaFFM.Pair_LAYOUT);
      pair.set(ValueLayout.JAVA_LONG, MynaFFM.Pair_left_OFFSET, 7L);
      pair.set(ValueLayout.JAVA_INT, MynaFFM.Pair_right_OFFSET, 19);
      var criticalSwap = MynaFFM.swapPair(SymbolLookup.loaderLookup(), true);
      var swapped = (MemorySegment) criticalSwap.invokeExact((SegmentAllocator) arena, pair);
      if (swapped.get(ValueLayout.JAVA_LONG, MynaFFM.Pair_left_OFFSET) != 19
          || swapped.get(ValueLayout.JAVA_INT, MynaFFM.Pair_right_OFFSET) != 7)
        throw new AssertionError("critical register-class record");
      // Sub-32-bit unsigned parameters must arrive zero-extended (zeroext), not sign-extended;
      // an optimized callee trusts the caller's extension and reads the full register.
      if (MynaFFM.widen((byte) 0, (byte) 200, (short) 0, (short) 0, false) != 200)
        throw new AssertionError("ffm u8 zero-extend");
      if (MynaFFM.widen((byte) 0, (byte) 0, (short) 0, (short) 0xFFFF, false) != 65535)
        throw new AssertionError("ffm u16 zero-extend");
      if (MynaFFM.widen((byte) -1, (byte) 255, (short) -2, (short) 65535, true) != 66787)
        throw new AssertionError("ffm narrow aggregate");
      // Factory handles expose ABI carriers: unsigned byte/short arguments widen to int.
      var criticalWiden = MynaFFM.widen(SymbolLookup.loaderLookup(), true);
      if ((int) criticalWiden.invokeExact((byte) 0, 200, (short) 0, 0, false) != 200
          || (int) criticalWiden.invokeExact((byte) 0, 0, (short) 0, 65535, false) != 65535
          || (int) criticalWiden.invokeExact((byte) -1, 255, (short) -2, 65535, true) != 66787)
        throw new AssertionError("critical narrow parameter extension");
      var criticalUnsigned = MynaFFM.lowUnsigned(SymbolLookup.loaderLookup(), true);
      var criticalSigned = MynaFFM.lowSigned(SymbolLookup.loaderLookup(), true);
      if ((byte) criticalUnsigned.invokeExact(200) != (byte) 200
          || (byte) criticalSigned.invokeExact(-100) != -100)
        throw new AssertionError("critical narrow return carriers");
      var binding = new MynaFFM.LibraryBinding(java.nio.file.Path.of(args[0]), arena);
      if (binding.inspect(value) != 42 || binding.reducer().address() == 0)
        throw new AssertionError("explicit binding pointer carriers");
      if (binding.widen((byte) -1, (byte) 255, (short) -2, (short) 65535, true) != 66787)
        throw new AssertionError("explicit binding unsigned carriers");
      if (binding.rotate(arena, triple, 1).get(ValueLayout.JAVA_LONG, MynaFFM.Triple_a_OFFSET) != 2)
        throw new AssertionError("explicit binding by-value record");
      var criticalBinding = new MynaFFM.CriticalLibraryBinding(java.nio.file.Path.of(args[0]), arena);
      if (criticalBinding.inspect(value) != 42
          || criticalBinding.widen((byte) -1, (byte) 255, (short) -2, (short) 65535, true) != 66787
          || criticalBinding.rotate(arena, triple, 1).get(ValueLayout.JAVA_LONG, MynaFFM.Triple_a_OFFSET) != 2)
        throw new AssertionError("typed critical binding ABI carriers");
      if (args.length > 1) {
      if (MynaNativeJNI.inspect(value.address()) != 42
          || MynaNativeJNI.widen((byte) -1, (byte) 255, (short) -2, (short) 65535, true) != 66787
          || MynaNativeJNI.lowUnsigned(200) != (byte) 200 || MynaNativeJNI.lowSigned(-100) != -100)
        throw new AssertionError("JNI narrow carriers");
      var jniRotated = arena.allocate(MynaFFM.Triple_LAYOUT);
      MynaNativeJNI.rotate(triple.address(), 1, jniRotated.address());
      if (jniRotated.get(ValueLayout.JAVA_LONG, MynaFFM.Triple_a_OFFSET) != 2)
        throw new AssertionError("JNI aggregate adapter");
      }
    }
    System.out.println("AOT FFM -> Rust passed");
  }
}
