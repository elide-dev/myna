// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package dev.elide.myna.backend;

import dev.elide.myna.model.Seam.*;
import dev.elide.myna.model.Seam.Module;
import java.util.*;

/**
 * The call borrows of one function: which parameters are byte buffers and which carry their
 * lengths. Every other parameter passes through a wrapper in the backend's ordinary carrier.
 */
record CallBorrows(Function function, List<BufferBorrow> borrows) {
  CallBorrows {
    borrows = List.copyOf(borrows);
  }

  /** Every function with call borrows only, in the module's borrow order (by function name). */
  static List<CallBorrows> of(Module m) {
    var names = new LinkedHashSet<String>();
    m.callBorrows().forEach(b -> names.add(b.function()));
    var result = new ArrayList<CallBorrows>();
    for (String name : names)
      result.add(
          of(
              m,
              m.functions().stream().filter(f -> f.name().equals(name)).findFirst().orElseThrow()));
    return result;
  }

  /** The borrows declared for {@code f}; none for a function without one. */
  static CallBorrows of(Module m, Function f) {
    return new CallBorrows(
        f, m.borrows().stream().filter(b -> b.function().equals(f.name())).toList());
  }

  /** The borrow whose buffer is {@code p}, or null. */
  BufferBorrow buffer(Parameter p) {
    for (BufferBorrow b : borrows) if (b.parameter().equals(p.name())) return b;
    return null;
  }

  /** The borrow whose length is {@code p}, or null. */
  BufferBorrow length(Parameter p) {
    for (BufferBorrow b : borrows) if (b.length().equals(p.name())) return b;
    return null;
  }

  private Parameter parameter(String name) {
    return function.parameters().stream()
        .filter(p -> p.name().equals(name))
        .findFirst()
        .orElseThrow();
  }

  /**
   * Whether the callee may write the buffer. Write-only access counts exactly like read/write: a
   * wrapper never assumes the callee does not read the bytes it is given.
   */
  boolean writable(BufferBorrow b) {
    return parameter(b.parameter()).access() != Access.READ;
  }

  /**
   * Whether the buffer may be absent. Every wrapper then takes its own spelling of "no buffer" and
   * passes the null pointer with length zero; a buffer that is present, empty or not, is lent
   * exactly as a non-nullable one is.
   */
  boolean nullable(BufferBorrow b) {
    return parameter(b.parameter()).nullable();
  }

  /** Whether the length travels in a 32-bit carrier, so the byte count needs a range check. */
  boolean narrow(BufferBorrow b) {
    Type type = parameter(b.length()).type();
    return type == Scalar.I32 || type == Scalar.U32;
  }

  /** The largest byte count a 32-bit length carries, as a Java {@code long} literal. */
  String maxLength(BufferBorrow b) {
    return parameter(b.length()).type() == Scalar.I32 ? "2147483647L" : "4294967295L";
  }

  /**
   * Distinguishes one buffer's generated locals: nothing for a function's only buffer, which keeps
   * single-buffer wrappers as they were, and the buffer's parameter position otherwise.
   */
  String suffix(BufferBorrow b) {
    if (borrows.size() == 1) return "";
    return String.valueOf(function.parameters().indexOf(parameter(b.parameter())));
  }

  /** Whether a parameter other than a buffer is a pointer, which the wrapper cannot vouch for. */
  boolean passesPointers() {
    return function.parameters().stream()
        .anyMatch(p -> buffer(p) == null && p.type() instanceof Pointer);
  }
}
