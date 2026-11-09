// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package dev.elide.myna.truffle;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.UnexpectedResultException;

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

  /**
   * {@link #execute} for a cell whose guest result is a {@code long}: {@code i64}, {@code u64},
   * {@code isize}, {@code usize} and {@code u32}. Generated cells with such a result override it
   * without the checked exception, and their {@link #execute} boxes its value, so a language node
   * holding the concrete cell type gets the primitive with no box on any path. This default unboxes
   * {@link #execute}.
   *
   * @throws UnexpectedResultException carrying the result when it is not a {@code Long}
   */
  public long executeLong(Object[] arguments) throws UnexpectedResultException {
    Object result = execute(arguments);
    if (result instanceof Long value) return value;
    throw unexpected(result);
  }

  /**
   * {@link #execute} for an {@code int} guest result: {@code i8}, {@code i16}, {@code i32}, {@code
   * u8}, {@code u16} and {@code char}. See {@link #executeLong}.
   */
  public int executeInt(Object[] arguments) throws UnexpectedResultException {
    Object result = execute(arguments);
    if (result instanceof Integer value) return value;
    throw unexpected(result);
  }

  /** {@link #execute} for a {@code double} guest result: {@code f32} and {@code f64}. */
  public double executeDouble(Object[] arguments) throws UnexpectedResultException {
    Object result = execute(arguments);
    if (result instanceof Double value) return value;
    throw unexpected(result);
  }

  /** {@link #execute} for a {@code bool} guest result. */
  public boolean executeBoolean(Object[] arguments) throws UnexpectedResultException {
    Object result = execute(arguments);
    if (result instanceof Boolean value) return value;
    throw unexpected(result);
  }

  private static UnexpectedResultException unexpected(Object result) {
    CompilerDirectives.transferToInterpreter();
    return new UnexpectedResultException(result);
  }

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
