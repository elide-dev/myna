// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package fixture.fast;
import java.lang.foreign.Arena;
import java.lang.foreign.ValueLayout;
import org.graalvm.word.WordFactory;
public final class FastMain {
  static volatile byte[] allocation;
  public static void main(String[] args) throws Exception {
    if(FastApi.BINDING_ABI_VERSION!=2) throw new AssertionError("binding ABI");
    // The ordinary default must work before any cached pointer is requested.
    if(FastApi.increment(40)!=41) throw new AssertionError("default leaf first use");
    try (Arena arena = Arena.ofConfined()) {
      var value = arena.allocate(8, 4);
      var result = arena.allocate(8, 4);
      value.set(ValueLayout.JAVA_INT, 0, 7);
      value.set(ValueLayout.JAVA_INT, 4, 35);
      FastApi.flip(WordFactory.pointer(value.address()), WordFactory.pointer(result.address()));
      if (result.get(ValueLayout.JAVA_INT, 0) != 35 || result.get(ValueLayout.JAVA_INT, 4) != 7)
        throw new AssertionError("C record import reference return");
      if (FastApi.total(WordFactory.pointer(result.address())) != 42)
        throw new AssertionError("C record import scalar return");
    }
    var pointer=FastApi.incrementLeafPointer();
    var failure=new java.util.concurrent.atomic.AtomicReference<Throwable>();
    long before=java.lang.management.ManagementFactory.getGarbageCollectorMXBeans().stream()
        .mapToLong(b -> Math.max(0,b.getCollectionCount())).sum();
    Thread collector=new Thread(() -> {
      try {
        for(int i=0;i<128;i++) { allocation=new byte[65536]; if(i%8==0) System.gc(); }
      } catch(Throwable t) { failure.set(t); }
    });
    collector.start();
    long x=0,y=0,i=0;
    while(i<1_000_000 || collector.isAlive()) {
      x=FastApi.increment(x); y=pointer.invoke(y); i++;
      if(x!=i || y!=i) throw new AssertionError("default leaf under GC");
    }
    collector.join();
    if(failure.get()!=null) throw new AssertionError(failure.get());
    long after=java.lang.management.ManagementFactory.getGarbageCollectorMXBeans().stream()
        .mapToLong(b -> Math.max(0,b.getCollectionCount())).sum();
    if(after<=before) throw new AssertionError("No GC observed");
    System.out.println("Binding ABI 2 default leaf, static linkage, lazy symbols and GC passed");
  }
}
