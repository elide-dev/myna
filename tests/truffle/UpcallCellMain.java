// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package bench.upcalls;

import java.lang.foreign.Arena;
import java.nio.file.Path;

/** The integration image runs the shared callback checks without the benchmark timer or NFI. */
public final class UpcallCellMain {
  public static void main(String[] args) throws Throwable {
    try (var arena = Arena.ofShared()) {
      var binding = new UpcallsFFM.LibraryBinding(Path.of(args[0]), arena);
      ReducerCellChecks.verify(binding, arena);
    }
    System.out.println("Truffle upcall cells passed worker delivery, context restoration and sentinels");
  }
}
