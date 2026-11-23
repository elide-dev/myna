// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package fixture.callbacks;

import java.lang.foreign.Arena;
import java.nio.file.Path;

public final class GenericCallbackMain {
  public static void main(String[] args) throws Throwable {
    try (var arena = Arena.ofShared()) {
      var binding = new CallbackApiFFM.LibraryBinding(Path.of(args[0]), arena);
      var generic = CallbackApiFFM.ReducerUpcalls.create((left, right) -> left - right, -1L, arena);
      var fixed = CallbackApiFFM.ReducerUpcalls.create(arena);
      if (binding.invoke(generic, false, 20, 22) != -2 || binding.invoke(fixed, false, 20, 22) != 42) {
        throw new AssertionError("Generic and fixed callbacks");
      }
    }
  }
}
