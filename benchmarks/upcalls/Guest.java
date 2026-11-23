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
  private static Guest registered;

  private final Context context;
  private final CallTarget target;
  private final boolean perCall;
  private volatile Throwable failure;

  private Guest(Context context, CallTarget target, boolean perCall) {
    this.context = context;
    this.target = target;
    this.perCall = perCall;
  }

  static final class MixNode extends Node {
    int execute(int value) {
      int rotated = Integer.rotateLeft(value, 5);
      return (value & 1) == 0 ? rotated + 17 : rotated ^ 0x5a5a;
    }
  }

  static final class CountingRoot extends RootNode {
    @Child private MixNode mix = new MixNode();
    CountingRoot(HookLanguage language) { super(language); }
    @Override public Object execute(VirtualFrame frame) {
      var guest = HookLanguage.CURRENT.get(this);
      if (CompilerDirectives.inCompiledCode()) guest.compiled++;
      guest.value = mix.execute(guest.value);
      guest.calls++;
      return null;
    }
  }

  public static void enter() { registered.enterContext(); }
  public static void leave() { registered.leaveContext(); }
  public static void run() { registered.callTarget(); }

  private void enterContext() {
    try { context.enter(); } catch (Throwable cause) { failure = cause; }
  }

  private void leaveContext() {
    try { context.leave(); } catch (Throwable cause) { failure = cause; }
  }

  private void callTarget() {
    try {
      if (perCall) context.enter();
      try { target.call(); } finally { if (perCall) context.leave(); }
    } catch (Throwable cause) { failure = cause; }
  }

  static void measure(UpcallsFFM.LibraryBinding binding, Arena arena, String name, int count) throws Throwable {
    try (var ctx = Context.newBuilder("upcallbench").allowExperimentalOptions(true)
        .option("engine.CompileImmediately", "true").option("engine.BackgroundCompilation", "false")
        .option("engine.MultiTier", "false").option("engine.CompilationFailureAction", "Throw").build()) {
      ctx.initialize("upcallbench");
      HookLanguage.State guest;
      CallTarget target;
      ctx.enter();
      try {
        guest = HookLanguage.CURRENT.get(null);
        target = guest.root.getCallTarget();
      } finally { ctx.leave(); }
      boolean perCall = name.contains("-each-");
      boolean bound = name.startsWith("guest-bound-");
      var receiver = new Guest(ctx, target, perCall);
      if (!bound) registered = receiver;
      var hook = bound ? UpcallsFFM.GuestHookUpcalls.createDirect(receiver::callTarget, arena)
          : UpcallsFFM.GuestHookUpcalls.create(arena);
      var enter = perCall ? MemorySegment.NULL : bound
          ? UpcallsFFM.EnterGuestUpcalls.createDirect(receiver::enterContext, arena)
          : UpcallsFFM.EnterGuestUpcalls.create(arena);
      var leave = perCall ? MemorySegment.NULL : bound
          ? UpcallsFFM.LeaveGuestUpcalls.createDirect(receiver::leaveContext, arena)
          : UpcallsFFM.LeaveGuestUpcalls.create(arena);
      int expected = 1;
      for (int i = 0; i < count; i++) {
        int rotated = Integer.rotateLeft(expected, 5);
        expected = (expected & 1) == 0 ? rotated + 17 : rotated ^ 0x5a5a;
      }
      for (int round = -3; round < 12; round++) {
        guest.calls = guest.compiled = 0;
        guest.value = 1;
        long elapsed = binding.measureGuest(hook, enter, leave, name.endsWith("-worker"), count);
        if (receiver.failure != null) throw new AssertionError("guest callback failed", receiver.failure);
        if (guest.value != expected || guest.calls != count || (round >= 0 && guest.compiled != count)) {
          throw new AssertionError("calls=" + guest.calls + " compiled=" + guest.compiled);
        }
        if (round >= 0) System.out.println(elapsed);
      }
      System.err.println("verified " + name + " calls=" + guest.calls + " compiled=" + guest.compiled
          + " checksum=" + guest.value + " timed_rounds=12");
    } finally { registered = null; }
  }
}
