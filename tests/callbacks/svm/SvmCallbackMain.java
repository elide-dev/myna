// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package fixture.callbacks;

import com.oracle.svm.core.foreign.ForeignFunctionsRuntime;
import java.lang.constant.DirectMethodHandleDesc;
import java.lang.foreign.Arena;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class SvmCallbackMain {
  private record Selection(long address, DirectMethodHandleDesc target) {}

  public static void main(String[] args) throws Throwable {
    CallbackMain.main(args);
    try (var arena = Arena.ofShared()) {
      var binding = new CallbackApiFFM.LibraryBinding(Path.of(args[0]), arena);
      var selections = new ArrayList<Selection>();
      var runtime = ForeignFunctionsRuntime.singleton();
      runtime.setUsingSpecializedUpcallListener((address, target) -> selections.add(new Selection(address, target)));
      try {
        CallbackApiFFM.ReducerCallback target = (left, right) -> {
          if (left < 0) throw new IllegalArgumentException("negative input");
          return left + right;
        };
        var generic = CallbackApiFFM.ReducerUpcalls.create(target, -11L, arena);
        var direct = CallbackApiFFM.ReducerUpcalls.createDirect(target, -22L, arena);
        // SVM fires the listener only after patching a specific trampoline to its direct entry.
        var expected = CallbackApiFFM.ReducerUpcalls.DIRECT_HANDLE.describeConstable().orElseThrow();
        if (generic.address() == direct.address() ||
            !selections.equals(List.of(new Selection(direct.address(), (DirectMethodHandleDesc) expected)))) {
          throw new AssertionError("Unexpected direct upcall selection " + selections);
        }
        for (boolean worker : new boolean[] {false, true}) {
          if (binding.invoke(generic, worker, 20, 22) != 42 || binding.invoke(generic, worker, -1, 0) != -11 ||
              binding.invoke(direct, worker, 20, 22) != 42 || binding.invoke(direct, worker, -1, 0) != -22) {
            throw new AssertionError("Selected callback result or sentinel");
          }
        }
      } finally {
        runtime.setUsingSpecializedUpcallListener(null);
      }
    }
    System.out.println("Receiver-bound direct upcall selection passed");
  }
}
