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

  /**
   * The header check macro, {@code BUFFER_SEAM_ABI_<fingerprint>}: {@code seam.h} defines it as 1
   * and the Native Image class reads it as a C constant. Bindings and a header from different
   * generations name different macros, so the image build's C query fails.
   */
  public static String seamAbiCheck(Module m, String fingerprint) {
    return prefix(m) + "_SEAM_ABI_" + fingerprint;
  }

  /** The C carrier typedef of a group: {@code komodo_frontend_status_t}. */
  public static String cTypedef(Module m, String group) {
    return m.name().toLowerCase(Locale.ROOT) + "_" + lowerSnake(group) + "_t";
  }

  /** The C name function of a group: {@code komodo_frontend_status_name}. */
  public static String cNameFunction(Module m, String group) {
    return m.name().toLowerCase(Locale.ROOT) + "_" + lowerSnake(group) + "_name";
  }

  /**
   * A C helper function of a flag set's member: the range getter {@code
   * komodo_frontend_class_flags_kind} and setter {@code komodo_frontend_class_flags_with_kind}.
   */
  public static String cFlagFunction(Module m, String group, String member) {
    return m.name().toLowerCase(Locale.ROOT) + "_" + lowerSnake(group) + "_" + member;
  }

  /** The Java getter of a flag set's range: {@code kind}, {@code typeCode}. */
  public static String javaRangeGetter(String range) {
    return lowerCamel(range);
  }

  /** The Java {@code with} setter of a flag set's range: {@code withKind}, {@code withTypeCode}. */
  public static String javaRangeSetter(String range) {
    String getter = javaRangeGetter(range);
    return "with" + Character.toUpperCase(getter.charAt(0)) + getter.substring(1);
  }

  /** The Rust accessor of a tagged field's case: {@code operand_as_binary_op}. */
  public static String rustCaseAccessor(String field, String group) {
    return field + "_as_" + lowerSnake(group);
  }

  /** The Native Image accessor of a tagged field's enum case: {@code operandAsBinaryOp}. */
  public static String javaCaseAccessor(String field, String group) {
    return field + "As" + group;
  }

  /** The C bitfield struct of a flag set: {@code komodo_frontend_function_flags_bits}. */
  public static String cBitsStruct(Module m, String group) {
    return m.name().toLowerCase(Locale.ROOT) + "_" + lowerSnake(group) + "_bits";
  }
}
