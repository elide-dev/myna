package bench;

/** Single case per process: no polymorphic callsite shares compilation profiles. */
final class Measure {
  interface Loop { long run(int count, long seed) throws Throwable; }
  static final class ReferenceLoop implements Loop {
    public long run(int count, long seed) { return reference(count, seed); }
  }
  static volatile long sink;
  static volatile Object allocationCanary;
  static long reference(int count, long seed) {
    long x = seed;
    for (int i = 0; i < count; ++i) x = x * 1664525L + 1013904223L;
    return x;
  }
  static void run(String name, int count, Loop loop) throws Throwable {
    var output = new StringBuilder();
    var allocation = (com.sun.management.ThreadMXBean)
        java.lang.management.ManagementFactory.getThreadMXBean();
    if (!allocation.isThreadAllocatedMemorySupported())
      throw new AssertionError("Current-thread Java heap allocation counter required");
    allocation.setThreadAllocatedMemoryEnabled(true);
    long threadId = Thread.currentThread().threadId();
    for (int i=0; i<10000; ++i) allocation.getThreadAllocatedBytes(threadId);
    long canaryStart = allocation.getThreadAllocatedBytes(threadId);
    allocationCanary = new byte[4096];
    long canaryBytes = allocation.getThreadAllocatedBytes(threadId) - canaryStart;
    if (canaryBytes < 4096) throw new AssertionError("Allocation counter canary failed");
    allocationCanary = null;
    boolean zeroAllocation = Boolean.getBoolean("myna.bench.zero-allocation");
    long warmupUptime = 0;
    for (int sample = -10; sample < 10; ++sample) {
      if (sample == 0) warmupUptime = java.lang.management.ManagementFactory.getRuntimeMXBean().getUptime();
      long seed = 123456789L + sample;
      long expected = reference(count, seed);
      long allocatedBefore = allocation.getThreadAllocatedBytes(threadId);
      long start = System.nanoTime();
      long result = loop.run(count, seed);
      long elapsed = System.nanoTime() - start;
      long allocatedBytes = allocation.getThreadAllocatedBytes(threadId) - allocatedBefore;
      sink = result;
      if (result != expected) throw new AssertionError("checksum: " + name);
      if (allocatedBytes < 0) throw new AssertionError("Invalid allocation counter");
      if (sample >= 0 && zeroAllocation && allocatedBytes != 0)
        throw new AssertionError("Java heap allocation budget exceeded: " + name + " allocated=" + allocatedBytes);
      if (sample >= 0) output.append(String.format(java.util.Locale.ROOT,
          "{\"case\":\"%s\",\"sample\":%d,\"iterations\":%d,\"ns\":%d,\"checksum\":%d,\"warmup_uptime_ms\":%d,\"allocated_bytes\":%d,\"allocation_canary_bytes\":%d}%n",
          name, sample, count, elapsed, result, warmupUptime, allocatedBytes, canaryBytes));
    }
    java.nio.file.Files.writeString(java.nio.file.Path.of(System.getProperty("myna.bench.results")), output);
  }
}
