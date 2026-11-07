// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package bench;
import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.file.Path;
public final class Jvm {
  static native long step(long value);
  static final MethodHandle manual;
  static final MethodHandle generatedCritical;
  static final MethodHandle manualBoundCritical;
  static final MemorySegment address;
  static final MemorySegment leafAddress;
  static final Arena libraryArena = Arena.ofShared();
  static final MemorySegment libraryAddress;
  static final GeneratedFFM.LibraryBinding library;
  static final GeneratedFFM.CriticalLibraryBinding criticalLibrary;
  static final class Unbound {
    static final MethodHandle HANDLE = Linker.nativeLinker().downcallHandle(
        FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG));
  }
  static final class CriticalUnbound {
    static final MethodHandle HANDLE = Linker.nativeLinker().downcallHandle(
        FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG),
        Linker.Option.critical(false));
  }
  static {
    String path = System.getProperty("myna.bench.library");
    System.load(path);
    SymbolLookup lookup = SymbolLookup.loaderLookup();
    address = lookup.find("myna_bench_step").orElseThrow();
    leafAddress = lookup.find("myna_bench_leaf").orElseThrow();
    manual = Linker.nativeLinker().downcallHandle(
        address, FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG));
    generatedCritical = GeneratedFFM.leaf(lookup, true);
    criticalLibrary = new GeneratedFFM.CriticalLibraryBinding(lookup);
    manualBoundCritical = Linker.nativeLinker().downcallHandle(
        leafAddress, FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG),
        Linker.Option.critical(false));
    SymbolLookup explicit = SymbolLookup.libraryLookup(Path.of(path), libraryArena);
    libraryAddress = explicit.find("myna_bench_step").orElseThrow();
    library = new GeneratedFFM.LibraryBinding(explicit);
  }
  static long generatedFfm(int n, long x) throws Throwable {
    for (int i=0; i<n; ++i) x=GeneratedFFM.step(x); return x;
  }
  static long manualFfm(int n, long x) throws Throwable {
    for (int i=0; i<n; ++i) x=(long)Unbound.HANDLE.invokeExact(address, x); return x;
  }
  static long manualBoundFfm(int n, long x) throws Throwable {
    for (int i=0; i<n; ++i) x=(long)manual.invokeExact(x); return x;
  }
  static long generatedLibraryFfm(int n, long x) throws Throwable {
    for (int i=0; i<n; ++i) x=library.step(x); return x;
  }
  static long manualLibraryFfm(int n, long x) throws Throwable {
    for (int i=0; i<n; ++i) x=(long)Unbound.HANDLE.invokeExact(libraryAddress, x); return x;
  }
  static long generatedCriticalFfm(int n, long x) throws Throwable {
    for (int i=0; i<n; ++i) x=(long)generatedCritical.invokeExact(x); return x;
  }
  static long manualBoundCriticalFfm(int n, long x) throws Throwable {
    for (int i=0; i<n; ++i) x=(long)manualBoundCritical.invokeExact(x); return x;
  }
  static long manualUnboundCriticalFfm(int n, long x) throws Throwable {
    for (int i=0; i<n; ++i) x=(long)CriticalUnbound.HANDLE.invokeExact(leafAddress, x); return x;
  }
  static long generatedTypedCriticalFfm(int n, long x) throws Throwable {
    for (int i=0; i<n; ++i) x=criticalLibrary.leaf(x); return x;
  }
  static long jni(int n, long x) {
    for (int i=0; i<n; ++i) x=step(x); return x;
  }
  static final class GeneratedFfm implements Measure.Loop {
    public long run(int n, long x) throws Throwable { return generatedFfm(n, x); }
  }
  static final class ManualFfm implements Measure.Loop {
    public long run(int n, long x) throws Throwable { return manualFfm(n, x); }
  }
  static final class ManualBoundFfm implements Measure.Loop {
    public long run(int n, long x) throws Throwable { return manualBoundFfm(n, x); }
  }
  static final class GeneratedLibraryFfm implements Measure.Loop {
    public long run(int n, long x) throws Throwable { return generatedLibraryFfm(n, x); }
  }
  static final class ManualLibraryFfm implements Measure.Loop {
    public long run(int n, long x) throws Throwable { return manualLibraryFfm(n, x); }
  }
  static final class GeneratedCriticalFfm implements Measure.Loop {
    public long run(int n, long x) throws Throwable { return generatedCriticalFfm(n, x); }
  }
  static final class ManualBoundCriticalFfm implements Measure.Loop {
    public long run(int n, long x) throws Throwable { return manualBoundCriticalFfm(n, x); }
  }
  static final class ManualUnboundCriticalFfm implements Measure.Loop {
    public long run(int n, long x) throws Throwable { return manualUnboundCriticalFfm(n, x); }
  }
  static final class GeneratedTypedCriticalFfm implements Measure.Loop {
    public long run(int n, long x) throws Throwable { return generatedTypedCriticalFfm(n, x); }
  }
  static final class Jni implements Measure.Loop {
    public long run(int n, long x) { return jni(n, x); }
  }
  public static void main(String[] args) throws Throwable {
    String name = args[0]; int count = Integer.parseInt(args[1]);
    Measure.Loop loop = switch (name) {
      case "control" -> new Measure.ReferenceLoop();
      case "generated-ffm" -> new GeneratedFfm();
      case "manual-ffm" -> new ManualFfm();
      case "manual-bound-ffm" -> new ManualBoundFfm();
      case "generated-library-ffm" -> new GeneratedLibraryFfm();
      case "manual-library-ffm" -> new ManualLibraryFfm();
      case "generated-critical-ffm" -> new GeneratedCriticalFfm();
      case "manual-bound-critical-ffm" -> new ManualBoundCriticalFfm();
      case "manual-unbound-critical-ffm" -> new ManualUnboundCriticalFfm();
      case "generated-typed-critical-ffm" -> new GeneratedTypedCriticalFfm();
      case "jni" -> new Jni();
      default -> throw new IllegalArgumentException(name);
    };
    try {
      Measure.run(name, count, loop);
    } finally {
      libraryArena.close();
    }
  }
}
