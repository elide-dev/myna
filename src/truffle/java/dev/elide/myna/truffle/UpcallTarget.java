// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package dev.elide.myna.truffle;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.TruffleContext;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.RootNode;
import java.util.Objects;

/** A signed-i64 guest callback bound to one registration and one language calling convention. */
public final class UpcallTarget {
  private final TruffleContext context;
  private final RootCallTarget entry;

  private UpcallTarget(TruffleContext context, CallTarget guest) {
    this.context = context;
    this.entry = new EntryRoot(Objects.requireNonNull(guest, "guest")).getCallTarget();
  }

  /**
   * Binds a guest target while its context is entered. The native producer must also arrange for
   * that context to be entered on each calling thread until the callback returns.
   */
  public static UpcallTarget entered(CallTarget guest) {
    return new UpcallTarget(null, guest);
  }

  /**
   * Binds a guest target in its owning context and enters that context for each callback. The
   * context's languages determine whether a producer may enter it concurrently from other threads.
   */
  public static UpcallTarget entering(TruffleContext context, CallTarget guest) {
    var previous = Objects.requireNonNull(context, "context").enter(null);
    try {
      return new UpcallTarget(context, guest);
    } finally {
      context.leave(null, previous);
    }
  }

  /**
   * Enters the adapter from the native callback with the guest's signed-i64 carriers. The guest
   * target must already bind any closure state its language requires and return a {@link Long}.
   */
  public long callLong(Object... arguments) {
    if (context == null) return (long) entry.call(arguments);
    var previous = context.enter(null);
    try {
      return (long) entry.call(arguments);
    } finally {
      context.leave(null, previous);
    }
  }

  private static final class EntryRoot extends RootNode {
    @Child private DirectCallNode guest;

    EntryRoot(CallTarget guest) {
      super(null);
      this.guest = DirectCallNode.create(guest);
    }

    @Override
    public Object execute(VirtualFrame frame) {
      var result = guest.call(frame.getArguments());
      return Objects.requireNonNull((Long) result, "signed i64 callback result");
    }

    @Override
    public String getName() {
      return "Myna i64 upcall";
    }

    @Override
    public boolean isInternal() {
      return true;
    }
  }
}
