// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package dev.elide.myna.backend;

import dev.elide.myna.model.Seam.*;
import dev.elide.myna.model.Seam.Module;
import java.util.*;

/**
 * Counted element views: a {@code ptr<T>} field whose {@code count=} names the integer field of the
 * same record counting the elements it points to. A null pointer is an empty view and requires a
 * zero count; otherwise the pointer addresses {@code count} consecutive elements, valid for as long
 * as the record is.
 */
record Views(
    Struct struct,
    Field pointer,
    Field count,
    Type element,
    long elementSize,
    long elementAlignment,
    long pointeeSize) {
  /** The views of one record, in field order; empty when no field declares {@code count=}. */
  static List<Views> of(Module m, Struct s) {
    var views = new ArrayList<Views>();
    for (Field f : s.fields()) {
      if (f.count() == null) continue;
      Field count =
          s.fields().stream().filter(c -> c.name().equals(f.count())).findFirst().orElseThrow();
      Type element = ((Pointer) f.type()).pointee();
      Struct record = struct(m, element);
      long size =
          record != null
              ? record.size()
              : element instanceof Scalar e ? e.bytes : 8; // a ptr<T> element is one word
      long alignment = record != null ? record.alignment() : size;
      // A pointer array's pointee: a struct's size, or 0 for an opaque type.
      Struct pointee = element instanceof Pointer p ? struct(m, p.pointee()) : null;
      views.add(
          new Views(s, f, count, element, size, alignment, pointee == null ? 0 : pointee.size()));
    }
    return views;
  }

  private static Struct struct(Module m, Type type) {
    if (!(type instanceof Named n)) return null;
    return m.structs().stream().filter(e -> e.name().equals(n.name())).findFirst().orElse(null);
  }

  /** Records that are the element type of some view. */
  static Set<String> elementStructs(Module m) {
    var names = new HashSet<String>();
    for (Struct s : m.structs())
      for (Views v : of(m, s)) if (v.element() instanceof Named n) names.add(n.name());
    return names;
  }

  Scalar countType() {
    return (Scalar) count.type();
  }

  /** A {@code ptr<ptr<T>>} view: its elements are pointers to a struct or opaque {@code T}. */
  boolean pointerElements() {
    return element instanceof Pointer;
  }

  /** {@code Struct.field}, for messages. */
  String label() {
    return struct.name() + "." + pointer.name();
  }
}
