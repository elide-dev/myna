// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package bench.upcalls;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.TruffleContext;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import java.lang.foreign.Arena;
import org.graalvm.polyglot.Context;

/** Untimed checks across the actual C callback ABI in the benchmark image. */
final class ReducerCellChecks {
  private static final long SENTINEL = -7001L;

  private static final class CheckedRoot extends RootNode {
    private final HookLanguage.State owner;
    private final long bias;

    CheckedRoot(HookLanguage.State owner, long bias) {
      super(null);
      this.owner = owner;
      this.bias = bias;
    }

    @Override public Object execute(VirtualFrame frame) {
      if (HookLanguage.CURRENT.get(this) != owner) throw new AssertionError("wrong guest context");
      long left = (long) frame.getArguments()[0];
      long right = (long) frame.getArguments()[1];
      if (left == 0) return Integer.valueOf(42);
      if (left == 1) throw new IllegalArgumentException("guest rejected input");
      return (left ^ right) + bias;
    }
  }

  private record Guest(TruffleContext context, CallTarget target) {
    static Guest get(Context context, long bias) {
      context.initialize("upcallbench");
      context.enter();
      try {
        var state = HookLanguage.CURRENT.get(null);
        return new Guest(state.env.getContext(), new CheckedRoot(state, bias).getCallTarget());
      } finally {
        context.leave();
      }
    }
  }

  private static void expect(long expected, long actual) {
    if (expected != actual) throw new AssertionError("expected " + expected + ", got " + actual);
  }

  static void verify(UpcallsFFM.LibraryBinding binding, Arena arena) throws Throwable {
    try (var first = Context.create("upcallbench"); var second = Context.create("upcallbench")) {
      var a = Guest.get(first, 19L);
      var b = Guest.get(second, -23L);
      var ca = UpcallsTruffle.ReducerUpcall.bind(a.context(), a.target(), SENTINEL, arena);
      var cb = UpcallsTruffle.ReducerUpcall.bind(b.context(), b.target(), SENTINEL, arena);
      long left = Long.MIN_VALUE + 7;
      long right = 0x10000_0003L;
      long mixed = left ^ right;
      expect(mixed + 19, binding.checkReducer(ca, left, right, true));
      expect(mixed - 23, binding.checkReducer(cb, left, right, true));
      second.enter();
      try {
        // Re-enter the first engine and restore the second even if guest conversion fails.
        expect(mixed + 19, binding.checkReducer(ca, left, right, false));
        expect(SENTINEL, binding.checkReducer(ca, 0, right, false));
        expect(SENTINEL, binding.checkReducer(ca, 1, right, false));
        if (!b.context().isEntered()) throw new AssertionError("did not restore caller context");
        var entered = UpcallsTruffle.ReducerUpcall.bindEntered(b.target(), SENTINEL, arena);
        expect(mixed - 23, binding.checkReducer(entered, left, right, false));
      } finally {
        second.leave();
      }
    }
  }
}
