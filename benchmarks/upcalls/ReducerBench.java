// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package bench.upcalls;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.Source;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.ref.Reference;
import org.graalvm.polyglot.Context;

/** Same native loop and same guest reducer for a manual bridge, generated upcall cell and NFI. */
final class ReducerBench {
  static final class ReducerRoot extends RootNode {
    ReducerRoot(HookLanguage language) { super(language); }

    @Override public Object execute(VirtualFrame frame) {
      var guest = HookLanguage.CURRENT.get(this);
      Object[] operands = frame.getArguments();
      long result = (Long) operands[0] + (Long) operands[1];
      if (CompilerDirectives.inCompiledCode()) guest.compiled++;
      guest.calls++;
      guest.lastReduction = result;
      return result;
    }
  }

  @ExportLibrary(InteropLibrary.class)
  static final class NfiExecutable implements TruffleObject {
    final CallTarget target;

    NfiExecutable(CallTarget target) { this.target = target; }

    @ExportMessage boolean isExecutable() { return true; }

    @ExportMessage Object execute(Object[] arguments) {
      return target.call(arguments);
    }
  }

  private static Object nfiClosure(HookLanguage.State guest, CallTarget target) throws Exception {
    var source = Source.newBuilder("nfi", "(SINT64,SINT64):SINT64", "reducer.nfi").internal(true).build();
    Object signature = guest.env.parseInternal(source).call();
    return InteropLibrary.getUncached().invokeMember(signature, "createClosure", new NfiExecutable(target));
  }

  static void measure(UpcallsFFM.LibraryBinding binding, Arena arena, String name, int count) throws Throwable {
    if (count < 1) throw new IllegalArgumentException("Expected a positive reducer event count");
    int tail = count % 8;
    long expected = 0x1_0000_0031L + 36L * (count / 8) + tail * (tail + 1L) / 2;
    try (var ctx = Context.newBuilder("upcallbench").allowNativeAccess(true).allowExperimentalOptions(true)
        .option("engine.CompileImmediately", "true").option("engine.BackgroundCompilation", "false")
        .option("engine.MultiTier", "false").option("engine.CompilationFailureAction", "Throw").build()) {
      ctx.initialize("upcallbench");
      HookLanguage.State guest;
      CallTarget target;
      Object owner = null;
      MemorySegment callback;
      ctx.enter();
      try {
        guest = HookLanguage.CURRENT.get(null);
        target = guest.reducer.getCallTarget();
        switch (name) {
          case "reducer-bound-caller", "reducer-bound-worker" ->
              callback = UpcallsFFM.ReducerUpcalls.createDirect(
                  (previous, increment) -> (Long) target.call(previous, increment), -1L, arena);
          case "reducer-cell-caller", "reducer-cell-worker" ->
              callback = UpcallsTruffle.ReducerUpcall.bindEntered(target, -1L, arena);
          case "reducer-nfi-caller", "reducer-nfi-worker" -> {
            owner = nfiClosure(guest, target);
            callback = MemorySegment.ofAddress(InteropLibrary.getUncached().asPointer(owner));
          }
          default -> throw new IllegalArgumentException(name);
        }
      } finally { ctx.leave(); }
      // The generated void-hook ABI cannot propagate Java exceptions to C. Check them after each join.
      var receiver = new Guest.Bound(ctx, target, false);
      var enter = UpcallsFFM.EnterGuestUpcalls.createDirect(receiver::enterContext, arena);
      var leave = UpcallsFFM.LeaveGuestUpcalls.createDirect(receiver::leaveContext, arena);
      try {
        for (int round = -3; round < 12; round++) {
          guest.calls = guest.compiled = 0;
          guest.lastReduction = 0;
          long elapsed = binding.measureGuestReducer(callback, enter, leave, name.endsWith("-worker"), count);
          receiver.checkFailure();
          if (elapsed < 0 || guest.calls != count || guest.lastReduction != expected
              || (round >= 0 && guest.compiled != count)) {
            throw new AssertionError(name + " calls=" + guest.calls + " compiled=" + guest.compiled
                + " checksum=" + guest.lastReduction + " elapsed=" + elapsed);
          }
          if (round >= 0) System.out.println(elapsed);
        }
      } finally {
        // The raw code pointer stays callable only while its libffi closure remains strongly reachable.
        Reference.reachabilityFence(owner);
      }
      System.err.println("verified " + name + " calls=" + guest.calls + " compiled=" + guest.compiled
          + " checksum=" + guest.lastReduction + " timed_rounds=12");
    }
  }
}
