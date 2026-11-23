// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package bench.upcalls;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.nodes.Node;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import org.graalvm.polyglot.Context;

public final class Guest {
  private static Context context;
  private static CallTarget target;
  private static boolean perCall;
  private static volatile int compiled;
  private static volatile int calls;
  private static volatile Throwable failure;

  private static volatile int state;

  static final class MixNode extends Node {
    int execute(int value) {
      int rotated = Integer.rotateLeft(value, 5);
      return (value & 1) == 0 ? rotated + 17 : rotated ^ 0x5a5a;
    }
  }

  static final class CountingRoot extends RootNode {
    @Child private MixNode mix = new MixNode();
    CountingRoot() { super(null); }
    @Override public Object execute(VirtualFrame frame) {
      if (CompilerDirectives.inCompiledCode()) compiled++;
      state = mix.execute(state);
      calls++;
      return null;
    }
  }

  public static void enter() {
    try { context.enter(); } catch (Throwable cause) { failure = cause; }
  }

  public static void leave() {
    try { context.leave(); } catch (Throwable cause) { failure = cause; }
  }

  public static void run() {
    try {
      if (perCall) context.enter();
      try { target.call(); } finally { if (perCall) context.leave(); }
    } catch (Throwable cause) { failure = cause; }
  }

  static void measure(UpcallsFFM.LibraryBinding binding, Arena arena, String name, int count) throws Throwable {
    try (var ctx = Context.newBuilder().allowExperimentalOptions(true)
        .option("engine.CompileImmediately", "true").option("engine.BackgroundCompilation", "false")
        .option("engine.MultiTier", "false").option("engine.CompilationFailureAction", "Throw").build()) {
      context = ctx;
      ctx.enter();
      try { target = new CountingRoot().getCallTarget(); } finally { ctx.leave(); }
      perCall = name.contains("-each-");
      var hook = UpcallsFFM.GuestHookUpcalls.create(arena);
      var enter = perCall ? MemorySegment.NULL : UpcallsFFM.EnterGuestUpcalls.create(arena);
      var leave = perCall ? MemorySegment.NULL : UpcallsFFM.LeaveGuestUpcalls.create(arena);
      int expected = 1;
      for (int i = 0; i < count; i++) {
        int rotated = Integer.rotateLeft(expected, 5);
        expected = (expected & 1) == 0 ? rotated + 17 : rotated ^ 0x5a5a;
      }
      for (int round = -3; round < 12; round++) {
        calls = compiled = 0;
        state = 1;
        long elapsed = binding.measureGuest(hook, enter, leave, name.endsWith("-worker"), count);
        if (failure != null) throw new AssertionError("guest callback failed", failure);
        if (state != expected || calls != count || (round >= 0 && compiled != count)) {
          throw new AssertionError("calls=" + calls + " compiled=" + compiled);
        }
        if (round >= 0) System.out.println(elapsed);
      }
    }
  }
}
