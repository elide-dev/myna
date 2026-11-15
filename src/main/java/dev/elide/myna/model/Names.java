// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package dev.elide.myna.model;

import dev.elide.myna.model.Seam.Module;
import java.util.Locale;

/** Generated names for constants, enums and flag sets, shared by every backend. */
public final class Names {
  private Names() {}

  /** {@code FunctionFlags} becomes {@code FUNCTION_FLAGS}. */
  public static String upperSnake(String camel) {
    return lowerSnake(camel).toUpperCase(Locale.ROOT);
  }

  /** {@code FunctionFlags} becomes {@code function_flags}; acronym runs stay together. */
  public static String lowerSnake(String camel) {
    var out = new StringBuilder();
    for (int i = 0; i < camel.length(); i++) {
      char c = camel.charAt(i);
      if (Character.isUpperCase(c) && i > 0) {
        char prev = camel.charAt(i - 1);
        boolean nextLower = i + 1 < camel.length() && Character.isLowerCase(camel.charAt(i + 1));
        if (Character.isLowerCase(prev) || Character.isDigit(prev) || (nextLower && prev != '_'))
          out.append('_');
      }
      out.append(Character.toLowerCase(c));
    }
    return out.toString();
  }

  /** {@code ANALYSIS_MAGIC} becomes {@code analysisMagic}. */
  public static String lowerCamel(String upperSnake) {
    var out = new StringBuilder();
    boolean up = false;
    for (char c : upperSnake.toLowerCase(Locale.ROOT).toCharArray()) {
      if (c == '_') up = true;
      else {
        out.append(up && out.length() > 0 ? Character.toUpperCase(c) : c);
        up = false;
      }
    }
    return out.toString();
  }

  private static String prefix(Module m) {
    return m.name().toUpperCase(Locale.ROOT);
  }

  /** The C macro of a constant ({@code group == null}) or of a group's value or bit. */
  public static String cMacro(Module m, String group, String name) {
    String base = prefix(m) + "_";
    return base + (group == null ? "" : upperSnake(group) + "_") + name.toUpperCase(Locale.ROOT);
  }

  /** The C carrier typedef of a group: {@code komodo_frontend_status_t}. */
  public static String cTypedef(Module m, String group) {
    return m.name().toLowerCase(Locale.ROOT) + "_" + lowerSnake(group) + "_t";
  }

  /** The C name function of a group: {@code komodo_frontend_status_name}. */
  public static String cNameFunction(Module m, String group) {
    return m.name().toLowerCase(Locale.ROOT) + "_" + lowerSnake(group) + "_name";
  }

  /** The C bitfield struct of a flag set: {@code komodo_frontend_function_flags_bits}. */
  public static String cBitsStruct(Module m, String group) {
    return m.name().toLowerCase(Locale.ROOT) + "_" + lowerSnake(group) + "_bits";
  }
}
