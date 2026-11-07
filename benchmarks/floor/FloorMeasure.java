// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package bench;

/** AOT-only timing: identity and recurrence have distinct correctness oracles. */
final class FloorMeasure {
  static volatile long sink;
  static void run(String name, int count, Measure.Loop loop) throws Throwable {
    var output = new StringBuilder();
    for(int sample=-10; sample<10; sample++) {
      long seed=123456789L+sample;
      long expected=name.startsWith("identity-") ? seed : Measure.reference(count,seed);
      long start=System.nanoTime();
      long result=loop.run(count,seed);
      long elapsed=System.nanoTime()-start;
      sink=result;
      if(result!=expected) throw new AssertionError("checksum: "+name);
      if(sample>=0) output.append(String.format(java.util.Locale.ROOT,
          "{\"case\":\"%s\",\"sample\":%d,\"iterations\":%d,\"ns\":%d,\"checksum\":%d}%n",
          name,sample,count,elapsed,result));
    }
    java.nio.file.Files.writeString(java.nio.file.Path.of(System.getProperty("myna.bench.results")),output);
  }
}
