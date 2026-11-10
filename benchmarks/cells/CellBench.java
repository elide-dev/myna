// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package bench;

import bench.nodes.Bodies;
import dev.elide.myna.truffle.NativeFunction;
import java.util.Locale;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

/**
 * Single case per process: one runtime-compiled Truffle root runs the whole measured loop, as the
 * fork's CFunctionRuntimeCompilationTest does, and every measured sample must observe compiled code.
 */
public final class CellBench {
  static volatile long sink;
  static volatile Object canary;

  static long expected(String name, int count, long seed) {
    if (name.contains("-sum-")) {
      int size = Integer.parseInt(name.substring(name.lastIndexOf('-') + 1));
      long sum = 0;
      for (byte b : Bodies.pattern(size)) sum += b & 0xff;
      return seed + count * sum;
    }
    if (name.contains("-slab")) return seed + count * Bodies.SLAB_DELTA;
    if (name.endsWith("-text")) return seed + (long) count * Bodies.TEXT.length();
    long x = seed;
    for (int i = 0; i < count; i++) x = x * 1664525L + 1013904223L;
    return x;
  }

  public static void main(String[] args) throws Throwable {
    String name = args[0];
    int count = Integer.parseInt(args[1]);
    if (count <= 0 || count % 256 != 0) throw new IllegalArgumentException("count must be a positive multiple of 256");
    String library = System.getProperty("myna.bench.library");
    if (name.contains("ffm")) System.load(library);
    boolean nfi = name.startsWith("nfi-");
    var builder = (nfi ? Context.newBuilder("cellbench").allowNativeAccess(true) : Context.newBuilder())
        .allowExperimentalOptions(true)
        .option("engine.CompileImmediately", "true").option("engine.BackgroundCompilation", "false")
        .option("engine.MultiTier", "false").option("engine.CompilationFailureAction", "Throw")
        .option("engine.TraceCompilation", "true");
    var allocation = (com.sun.management.ThreadMXBean) java.lang.management.ManagementFactory.getThreadMXBean();
    if (!allocation.isThreadAllocatedMemorySupported()) throw new AssertionError("Current-thread Java heap allocation counter required");
    allocation.setThreadAllocatedMemoryEnabled(true);
    long threadId = Thread.currentThread().threadId();
    try (Context context = builder.build()) {
      context.enter();
      try {
        // NFI is an internal language: the benchmark language binds the symbol and hands it over.
        String spec = name.equals("nfi-text") ? "myna_bench_text|(STRING,SINT64):SINT64" : "myna_bench_leaf|(SINT64):SINT64";
        Object extra = nfi ? context.eval("cellbench", library + "|" + spec) : 0;
        NativeFunction function = Bodies.create(name).function();
        Value callable = context.asValue(function);
        if (!callable.canExecute()) throw new AssertionError("cell wrapper is not executable through the context");
        for (int i = 0; i < 10000; ++i) allocation.getThreadAllocatedBytes(threadId);
        long canaryStart = allocation.getThreadAllocatedBytes(threadId);
        canary = new byte[4096];
        long canaryBytes = allocation.getThreadAllocatedBytes(threadId) - canaryStart;
        if (canaryBytes < 4096) throw new AssertionError("Allocation counter canary failed");
        var output = new StringBuilder();
        int interpretedWarmups = 0;
        for (int sample = -10; sample < 10; ++sample) {
          long seed = 123456789L + sample;
          long expected = expected(name, count, seed);
          long allocatedBefore = allocation.getThreadAllocatedBytes(threadId);
          long start = System.nanoTime();
          long result = callable.execute(count, seed, extra).asLong();
          long elapsed = System.nanoTime() - start;
          long allocatedBytes = allocation.getThreadAllocatedBytes(threadId) - allocatedBefore;
          sink = result;
          if (result != expected) throw new AssertionError("checksum: " + name + " got " + result + " expected " + expected);
          boolean compiled = Bodies.lastCompiled;
          if (sample < 0 && !compiled) interpretedWarmups++;
          if (sample >= 0 && !compiled) throw new AssertionError("measured sample ran outside compiled code: " + name);
          if (sample >= 0) output.append(String.format(Locale.ROOT,
              "{\"case\":\"%s\",\"sample\":%d,\"iterations\":%d,\"ns\":%d,\"checksum\":%d,\"allocated_bytes\":%d,\"allocation_canary_bytes\":%d,\"compiled\":%b,\"interpreted_warmups\":%d}%n",
              name, sample, count, elapsed, result, allocatedBytes, canaryBytes, compiled, interpretedWarmups));
        }
        java.nio.file.Files.writeString(java.nio.file.Path.of(System.getProperty("myna.bench.results")), output);
      } finally {
        context.leave();
      }
    }
  }
}
