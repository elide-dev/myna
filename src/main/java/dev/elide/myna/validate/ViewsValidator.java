// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package dev.elide.myna.validate;

import dev.elide.myna.model.Seam.*;
import dev.elide.myna.model.Seam.Module;
import java.util.*;

/**
 * Validates counted element views: {@code count=FIELD} on a {@code ptr<T>} struct field, and the
 * accessor names the generators derive from them, even in relaxed mode.
 */
final class ViewsValidator {
  private ViewsValidator() {}

  static void validate(Module m) {
    var structs = new HashMap<String, Struct>();
    for (Struct s : m.structs()) structs.put(s.name(), s);
    var functions = new HashSet<String>();
    for (Function f : m.functions()) functions.add(f.name());
    var elements = new TreeMap<String, String>();
    var accessors = new HashMap<String, String>();
    for (Struct s : m.structs()) {
      var counted = new HashMap<String, String>();
      for (Field f : s.fields()) {
        if (f.count() == null) continue;
        String where = "field " + s.name() + "." + f.name();
        require(
            f.type() instanceof Pointer,
            where + ": count= requires a ptr<T> field, found " + f.type().text());
        Type element = ((Pointer) f.type()).pointee();
        if (element instanceof Named n) {
          if (!structs.containsKey(n.name()))
            throw new IllegalArgumentException(
                where + ": count= needs elements of a known size, but " + n.name() + " is opaque");
          elements.putIfAbsent(n.name(), where);
        } else
          require(
              (element instanceof Scalar e && e != Scalar.VOID && e != Scalar.BOOL)
                  || (element instanceof Pointer p && p.pointee() instanceof Named),
              where
                  + ": count= needs struct, integer/float or ptr<struct or opaque> elements, found "
                  + f.type().text());
        Field count =
            s.fields().stream().filter(c -> c.name().equals(f.count())).findFirst().orElse(null);
        require(count != null, where + ": count=" + f.count() + " names no field of " + s.name());
        String countWhere = s.name() + "." + count.name();
        require(
            count.type() instanceof Scalar c && unsigned(c),
            where
                + ": count field "
                + countWhere
                + " must be u8, u16, u32, u64 or usize, found "
                + count.type().text());
        require(
            count.group() == null,
            where + ": count field " + countWhere + " carries an enum= or flags= reference");
        String other = counted.putIfAbsent(count.name(), f.name());
        require(
            other == null,
            "field "
                + s.name()
                + "."
                + other
                + " and field "
                + s.name()
                + "."
                + f.name()
                + ": both name count="
                + count.name());
        // The FFM view, owner, element, count and loop accessors, and the Native Image element
        // helper, are static methods of the generated classes beside the functions' own and each
        // other's: a field `a_count` must not meet field `a`'s count accessor.
        String base = s.name() + "_" + f.name();
        for (String accessor : List.of(base, base + "At", base + "_count", base + "_at")) {
          require(
              !functions.contains(accessor),
              where + ": accessor " + accessor + " collides with function " + accessor);
          String owner = accessors.putIfAbsent(accessor, where);
          require(
              owner == null,
              where + ": accessor " + accessor + " collides with an accessor of " + owner);
        }
      }
    }
    // A struct that is the element of a view gains Native Image's indexed addressOf(long).
    for (var entry : elements.entrySet())
      for (Field f : structs.get(entry.getKey()).fields())
        require(
            !f.name().equals("addressOf"),
            "field "
                + entry.getKey()
                + ".addressOf collides with the element accessor addressOf(long) that "
                + entry.getValue()
                + " needs");
  }

  /** A count is never negative: only unsigned carriers count elements. */
  private static boolean unsigned(Scalar s) {
    return switch (s) {
      case U8, U16, U32, U64, USIZE -> true;
      default -> false;
    };
  }

  private static void require(boolean condition, String message) {
    if (!condition) throw new IllegalArgumentException(message);
  }
}
