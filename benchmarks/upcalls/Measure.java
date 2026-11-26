// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package bench.upcalls;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.file.Path;

public final class Measure {
  private static volatile int hookCalls;

  public static void hook() {
    hookCalls++;
  }

  public static long reduce(long previous, long increment) {
    return previous + increment;
  }

  private static MemorySegment reducer(String kind, Arena arena) {
    return switch (kind) {
      case "static" -> UpcallsFFM.ReducerUpcalls.create(arena);
      case "dynamic" -> UpcallsFFM.ReducerUpcalls.create(Measure::reduce, -1, arena);
      case "bound" -> UpcallsFFM.ReducerUpcalls.createDirect(Measure::reduce, -1, arena);
      default -> throw new IllegalArgumentException(kind);
    };
  }

  public static void main(String[] args) throws Throwable {
    String workload = args[1];
    int count = Integer.parseInt(args[2]);
    try (var arena = Arena.ofShared()) {
      var binding = new UpcallsFFM.LibraryBinding(Path.of(args[0]), arena);
      if (workload.startsWith("guest-")) {
        Guest.measure(binding, arena, workload, count);
        return;
      }
      String kind = workload.substring(0, workload.indexOf('-'));
      var callback = switch (workload) {
        case "static-caller", "static-worker", "dynamic-caller", "dynamic-worker",
            "bound-caller", "bound-worker" -> reducer(kind, arena);
        default -> MemorySegment.NULL;
      };
      var hook = switch (workload) {
        case "dynamic-hook-caller", "dynamic-hook-worker" -> UpcallsFFM.HookUpcalls.create(Measure::hook, arena);
        case "bound-hook-caller", "bound-hook-worker" -> UpcallsFFM.HookUpcalls.createDirect(Measure::hook, arena);
        case "static-hook-caller", "static-hook-worker" -> UpcallsFFM.HookUpcalls.create(arena);
        default -> MemorySegment.NULL;
      };
      for (int round = -3; round < 12; round++) {
        long elapsed;
        if (workload.endsWith("-setup")) {
          long start = System.nanoTime();
          try (var registrations = Arena.ofShared()) {
            for (int i = 0; i < count; i++) {
              var stub = reducer(kind, registrations);
              if (stub.address() == 0) throw new AssertionError();
            }
          }
          elapsed = System.nanoTime() - start;
        } else if (workload.contains("-hook-")) {
          hookCalls = 0;
          elapsed = binding.measureHook(hook, workload.endsWith("-worker"), count);
          if (hookCalls != count) throw new AssertionError("missing hook calls");
        } else if (workload.equals("isolate-caller")) {
          elapsed = binding.measureIsolate(
              MemorySegment.ofAddress(Upcalls.Literals.reduceInIsolate.getFunctionPointer().rawValue()),
              MemorySegment.ofAddress(org.graalvm.nativeimage.CurrentIsolate.getCurrentThread().rawValue()),
              count);
        } else {
          elapsed = binding.measure(callback, workload.endsWith("-worker"), count);
        }
        if (round >= 0) System.out.println(elapsed);
      }
    }
  }
}
