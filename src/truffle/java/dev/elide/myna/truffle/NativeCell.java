// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package dev.elide.myna.truffle;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.nodes.Node;

/**
 * One native function, callable from guest code. Myna generates a subclass per binding: it owns the
 * coercions for the declared signature as child nodes, calls the module's existing generated
 * binding (a Native Image leaf, a transitioning {@code @CFunction} stub, or an FFM handle) and
 * converts the result. The cell adds no native-call machinery of its own, and it never changes a
 * binding's leaf status: how the call compiles is decided by the binding and the compiler.
 *
 * <p>A cell is a Truffle node: adopt it once, either in a language's own function object or through
 * {@link #function()}. Each adoption needs a fresh instance, because the coercions hold call-site
 * profiles.
 */
public abstract class NativeCell extends Node {
  private final String name;
  private final int arity;

  protected NativeCell(String name, int arity) {
    this.name = name;
    this.arity = arity;
  }

  /** The seam's logical function name. */
  public final String name() {
    return name;
  }

  /** The number of guest arguments: the seam's parameters, less lengths derived from borrows. */
  public final int arity() {
    return arity;
  }

  /**
   * Coerces guest values per the signature, calls the binding, and converts the result.
   *
   * @throws NativeCellException for wrong arity or an argument outside the declared carrier
   */
  public abstract Object execute(Object[] arguments);

  protected final void checkArity(Object[] arguments) {
    if (arguments.length != arity) {
      CompilerDirectives.transferToInterpreter();
      throw NativeCellException.arity(name, arity, arguments.length, this);
    }
  }

  /** Wraps this cell as an interop-executable object with its own call target. */
  public final NativeFunction function() {
    return NativeFunction.of(this);
  }
}
