// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package dev.elide.myna.truffle;

import static org.junit.jupiter.api.Assertions.*;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleContext;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import org.graalvm.polyglot.Context;
import org.junit.jupiter.api.Test;

class UpcallTargetTest {
  @TruffleLanguage.Registration(id = "myna-upcall-test", name = "Myna upcall test")
  public static final class GuestLanguage extends TruffleLanguage<TruffleLanguage.Env> {
    static final ContextReference<Env> CURRENT = ContextReference.create(GuestLanguage.class);

    @Override
    protected Env createContext(Env env) {
      return env;
    }
  }

  private static Context openContext() {
    return Context.newBuilder("myna-upcall-test")
        .option("engine.WarnInterpreterOnly", "false")
        .build();
  }

  private record Guest(TruffleContext context, CallTarget target) {
    static Guest create(Context host) {
      host.initialize("myna-upcall-test");
      host.enter();
      try {
        var owner = GuestLanguage.CURRENT.get(null).getContext();
        var root =
            new RootNode(null) {
              @Override
              public Object execute(VirtualFrame frame) {
                assertSame(owner, GuestLanguage.CURRENT.get(this).getContext());
                long left = (long) frame.getArguments()[0];
                long right = (long) frame.getArguments()[1];
                if (left == 0) return null;
                if (left == 1) return Integer.valueOf(1);
                if (left == 2) throw new IllegalArgumentException("guest rejected input");
                if (left == 3) {
                  // Tools traversing the guest stack see the adapter as an internal frame.
                  RootNode caller =
                      Truffle.getRuntime()
                          .iterateFrames(
                              instance -> ((RootCallTarget) instance.getCallTarget()).getRootNode(),
                              1);
                  assertNotNull(caller);
                  assertEquals("Myna i64 upcall", caller.getName());
                  assertTrue(caller.isInternal());
                }
                return left ^ right;
              }
            };
        return new Guest(owner, root.getCallTarget());
      } finally {
        host.leave();
      }
    }
  }

  @Test
  void enteredTargetDeliversSignedCarriersAndRequiresALongResult() {
    try (var host = openContext()) {
      var guest = Guest.create(host);
      host.enter();
      try {
        var callback = UpcallTarget.entered(guest.target());
        long high = 0x1_0000_0005L;
        assertEquals(Long.MIN_VALUE ^ high, callback.callLong(Long.MIN_VALUE, high));
        assertEquals(3L ^ high, callback.callLong(3L, high));
        assertThrows(NullPointerException.class, () -> callback.callLong(0L, high));
        assertThrows(ClassCastException.class, () -> callback.callLong(1L, high));
        var failure =
            assertThrows(IllegalArgumentException.class, () -> callback.callLong(2L, high));
        assertEquals("guest rejected input", failure.getMessage());
        assertTrue(guest.context().isEntered());
      } finally {
        host.leave();
      }
    }
  }

  @Test
  void enteringTargetRestoresThePreviousContextEvenOnFailure() {
    try (var outerHost = openContext();
        var callbackHost = openContext()) {
      var outer = Guest.create(outerHost);
      var guest = Guest.create(callbackHost);
      outerHost.enter();
      try {
        assertThrows(
            NullPointerException.class, () -> UpcallTarget.entering(guest.context(), null));
        assertTrue(outer.context().isEntered());
        var callback = UpcallTarget.entering(guest.context(), guest.target());
        assertTrue(outer.context().isEntered());
        assertEquals(4L ^ Long.MIN_VALUE, callback.callLong(4L, Long.MIN_VALUE));
        assertTrue(outer.context().isEntered());
        var failure = assertThrows(IllegalArgumentException.class, () -> callback.callLong(2L, 0L));
        assertEquals("guest rejected input", failure.getMessage());
        assertTrue(outer.context().isEntered());
        assertFalse(guest.context().isEntered());
      } finally {
        outerHost.leave();
      }
    }
  }
}
