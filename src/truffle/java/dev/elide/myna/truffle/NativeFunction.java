// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package dev.elide.myna.truffle;

import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.IndirectCallNode;

/**
 * An interop-executable native function: a cell behind a call target, callable from any Truffle
 * language through {@link InteropLibrary#execute}. The call costs one interop dispatch, which
 * partial evaluation inlines for a monomorphic call site. Languages that want the cell as one of
 * their own function objects use {@link #callTarget()} or the cell directly.
 */
@ExportLibrary(InteropLibrary.class)
public final class NativeFunction implements TruffleObject {
  final RootCallTarget target;
  private final String name;
  private final int arity;

  private NativeFunction(RootCallTarget target, String name, int arity) {
    this.target = target;
    this.name = name;
    this.arity = arity;
  }

  /** Adopts the cell under a fresh root. */
  public static NativeFunction of(NativeCell cell) {
    return new NativeFunction(
        new NativeCellRootNode(cell).getCallTarget(), cell.name(), cell.arity());
  }

  public RootCallTarget callTarget() {
    return target;
  }

  public String name() {
    return name;
  }

  public int arity() {
    return arity;
  }

  /** Calls from host code, outside any guest frame. */
  public Object call(Object... arguments) {
    return target.call(arguments);
  }

  @ExportMessage
  boolean isExecutable() {
    return true;
  }

  @ExportMessage
  boolean hasExecutableName() {
    return true;
  }

  @ExportMessage
  Object getExecutableName() {
    return name;
  }

  @ExportMessage
  static final class Execute {
    @Specialization(guards = "receiver == cached", limit = "3")
    static Object direct(
        NativeFunction receiver,
        Object[] arguments,
        @Cached("receiver") NativeFunction cached,
        @Cached("create(cached.target)") DirectCallNode call) {
      return call.call(arguments);
    }

    @Specialization(replaces = "direct")
    static Object indirect(
        NativeFunction receiver, Object[] arguments, @Cached IndirectCallNode call) {
      return call.call(receiver.target, arguments);
    }
  }

  @Override
  public String toString() {
    return "NativeFunction " + name + "/" + arity;
  }
}
