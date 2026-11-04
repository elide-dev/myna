package bench;

/** Single case per process: no polymorphic callsite shares compilation profiles. */
final class Measure {
  interface Loop { long run(int count, long seed) throws Throwable; }
  static volatile long sink;
  static long reference(int count, long seed) {
    long x = seed;
    for (int i = 0; i < count; ++i) x = x * 1664525L + 1013904223L;
    return x;
  }
  static void run(String name, int count, Loop loop) throws Throwable {
    var output = new StringBuilder();
    long warmupUptime = 0;
    for (int sample = -10; sample < 10; ++sample) {
      if (sample == 0) warmupUptime = java.lang.management.ManagementFactory.getRuntimeMXBean().getUptime();
      long seed = 123456789L + sample;
      long expected = reference(count, seed);
      long start = System.nanoTime();
      long result = loop.run(count, seed);
      long elapsed = System.nanoTime() - start;
      sink = result;
      if (result != expected) throw new AssertionError("checksum: " + name);
      if (sample >= 0) output.append(String.format(java.util.Locale.ROOT,
          "{\"case\":\"%s\",\"sample\":%d,\"iterations\":%d,\"ns\":%d,\"checksum\":%d,\"warmup_uptime_ms\":%d}%n",
          name, sample, count, elapsed, result, warmupUptime));
    }
    java.nio.file.Files.writeString(java.nio.file.Path.of(System.getProperty("myna.bench.results")), output);
  }
}
