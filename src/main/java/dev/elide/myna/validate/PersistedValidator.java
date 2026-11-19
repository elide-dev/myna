// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package dev.elide.myna.validate;

import dev.elide.myna.model.Persisted.*;
import dev.elide.myna.model.PersistedLayout;
import dev.elide.myna.model.Seam.*;
import dev.elide.myna.model.Seam.Module;
import java.util.*;

/** Validates persisted-format declarations (myna#167). */
final class PersistedValidator {
  private PersistedValidator() {}

  private static final String TYPE_NAME = "[A-Z][A-Za-z0-9]*";
  private static final String MEMBER_NAME = "[a-z][a-z0-9_]*";

  /**
   * Type names generated beside the family's own declarations in its Rust module and Java classes,
   * and standard names those files rely on.
   */
  private static final Set<String> RESERVED_TYPES =
      Set.of(
          "Raw",
          "Tag",
          "ErrorKind",
          "Class",
          "FormatError",
          "WriteError",
          "Peek",
          "AnyNode",
          "Elements",
          "Self",
          "Option",
          "Result",
          "Vec",
          "String",
          "Box",
          "Some",
          "None",
          "Ok",
          "Err",
          "Object",
          "Integer",
          "Long",
          "Short",
          "Byte",
          "Boolean",
          "Float",
          "Double",
          "Character",
          "Math",
          "Arrays",
          "System",
          "Override",
          "Iterator",
          "Writer",
          "Reader",
          "Nodes",
          "Exception",
          "RuntimeException",
          "IllegalArgumentException",
          "BTreeMap",
          "HashMap",
          "Map",
          "List",
          "ArrayList",
          "StandardCharsets",
          "VarHandle",
          "MethodHandles",
          "ByteOrder");

  /** Member names a generated view, element or writer already uses. */
  private static final Set<String> RESERVED_MEMBERS =
      Set.of("index", "tag", "at", "raw", "from_raw", "payloads", "bytes", "next", "size");

  /** Method names of a generated per-format Rust type. */
  private static final Set<String> RESERVED_HEADER_WORDS =
      Set.of("validate", "validate_structural", "peek", "root", "bytes", "header_word", "raw");

  /** Rust writer methods beside the per-node ones. */
  private static final Set<String> RESERVED_WRITER_METHODS = Set.of("new", "string", "default");

  private static final Set<Scalar> FIELD_SCALARS =
      EnumSet.of(
          Scalar.U8,
          Scalar.I8,
          Scalar.U16,
          Scalar.I16,
          Scalar.U32,
          Scalar.I32,
          Scalar.U64,
          Scalar.I64,
          Scalar.F32,
          Scalar.F64,
          Scalar.BOOL);

  private static final Set<Scalar> LIST_SCALARS =
      EnumSet.of(
          Scalar.U8,
          Scalar.I8,
          Scalar.U16,
          Scalar.I16,
          Scalar.U32,
          Scalar.I32,
          Scalar.U64,
          Scalar.I64);

  static void validate(Module module) {
    var d = module.persisted();
    if (d.isEmpty()) return;
    var families = new HashSet<String>();
    var familySnakes = new HashSet<String>();
    for (Family f : d.families()) {
      typeName(f.name(), "family");
      require(families.add(f.name()), "duplicate family: " + f.name());
      require(
          familySnakes.add(PersistedLayout.snake(f.name())),
          "family names collide in Rust: " + f.name());
    }
    // Every declaration names a declared family.
    for (Format f : d.formats()) family(families, f.family(), "format " + f.name());
    for (Node n : d.nodes()) family(families, n.family(), "node " + n.name());
    for (Element e : d.elements()) family(families, e.family(), "element " + e.name());
    for (Kind k : d.kinds()) family(families, k.family(), "kind " + k.name());
    for (Union u : d.unions()) family(families, u.family(), "union " + u.name());
    var magics = new HashMap<Long, String>();
    var javaNames = new HashMap<String, String>();
    for (Format f : d.formats()) {
      String other = magics.putIfAbsent(f.magic(), f.name());
      require(other == null, "formats " + other + " and " + f.name() + " share a magic");
    }
    for (PersistedLayout layout : PersistedLayout.of(module)) family(layout, javaNames);
  }

  private static void family(PersistedLayout layout, Map<String, String> javaNames) {
    String family = layout.family.name();
    // Nodes, kinds, elements, unions and formats share one type namespace per family.
    var types = new HashSet<String>();
    for (Node n : layout.nodes) declareType(types, n.name(), "node");
    for (Kind k : layout.kinds) declareType(types, k.name(), "kind");
    for (Element e : layout.elements) declareType(types, e.name(), "element");
    for (Union u : layout.unions) declareType(types, u.name(), "union");
    for (Format f : layout.formats) declareType(types, f.name(), "format");
    // Java classes are one package per module.
    javaClass(javaNames, family + "Nodes", family);
    javaClass(javaNames, family + "Writer", family);
    javaClass(javaNames, family + "Exception", family);
    for (Format f : layout.formats) javaClass(javaNames, f.name() + "Reader", f.name());

    var tags = new HashMap<Integer, String>();
    var writerMethods = new HashSet<>(RESERVED_WRITER_METHODS);
    for (Format f : layout.formats) writerMethods.add("finish_" + PersistedLayout.snake(f.name()));
    for (Node n : layout.nodes) {
      require(n.tag() >= 1 && n.tag() <= 0xFFFF, "node " + n.name() + ": tag must be 1..65535");
      String other = tags.putIfAbsent(n.tag(), n.name());
      require(other == null, "nodes " + other + " and " + n.name() + " share tag " + n.tag());
      String method = PersistedLayout.snake(n.name());
      require(
          writerMethods.add(method), "node " + n.name() + ": writer method collides: " + method);
      Validator.identifier(javaMethod(n.name()));
      members(layout, "node " + n.name(), n.members(), true);
    }
    for (Element e : layout.elements) {
      String where = "element " + e.name();
      if (e.tag() != null)
        require(e.tag() >= 0 && e.tag() <= 0xFFFF, where + ": tag must be 0..65535");
      else require(!e.members().isEmpty(), where + ": an untagged element needs a member");
      members(layout, where, e.members(), false);
    }
    for (Kind k : layout.kinds) {
      require(!k.nodes().isEmpty(), "kind " + k.name() + " lists no nodes");
      var seen = new HashSet<String>();
      for (String n : k.nodes()) {
        require(layout.node(n) != null, "kind " + k.name() + ": unknown node " + n);
        require(seen.add(n), "kind " + k.name() + " lists " + n + " twice");
      }
    }
    for (Union u : layout.unions) {
      require(!u.elements().isEmpty(), "union " + u.name() + " lists no elements");
      var seen = new HashMap<Integer, String>();
      var names = new HashSet<String>();
      for (String name : u.elements()) {
        Element e = layout.element(name);
        require(e != null, "union " + u.name() + ": unknown element " + name);
        require(names.add(name), "union " + u.name() + " lists " + name + " twice");
        require(e.tag() != null, "union " + u.name() + ": element " + name + " has no tag");
        String other = seen.putIfAbsent(e.tag(), name);
        require(
            other == null,
            "union " + u.name() + ": elements " + other + " and " + name + " share tag " + e.tag());
      }
    }
    // An element is used tagged through unions or untagged through element=, never both.
    for (Node n : layout.nodes)
      for (Member m : n.members())
        if (m.kind() == MemberKind.ELEMENT_LIST) {
          Element e = layout.element(m.target());
          require(
              e.tag() == null,
              "node " + n.name() + ": element " + e.name() + " is tagged; use union=");
        }
    for (Format f : layout.formats) format(layout, f);
  }

  private static void format(PersistedLayout layout, Format f) {
    String where = "format " + f.name();
    require(f.version() >= 1, where + ": version must be positive");
    var l = f.limits();
    require(
        l.maxBytes() >= 44 && l.maxBytes() <= Integer.MAX_VALUE - 8,
        where + ": max_bytes must be 44..2147483639");
    require(
        l.maxNodes() >= 1 && l.maxNodes() <= Integer.MAX_VALUE,
        where + ": max_nodes must be 1..2147483647");
    require(
        l.maxStrings() <= Integer.MAX_VALUE, where + ": max_strings must be at most 2147483647");
    require(l.maxDepth() >= 1 && l.maxDepth() <= 32767, where + ": max_depth must be 1..32767");
    require(
        l.maxWork() >= 1 && l.maxWork() <= Integer.MAX_VALUE - 1,
        where + ": max_work must be 1..2147483646");
    if (f.headerWord() != null) {
      require(
          f.headerWord().matches(MEMBER_NAME) && !RESERVED_HEADER_WORDS.contains(f.headerWord()),
          where + ": invalid or reserved header word name: " + f.headerWord());
      Validator.identifier(f.headerWord());
    }
    var roots = new HashSet<String>();
    for (String r : f.roots()) {
      require(
          layout.node(r) != null || layout.kind(r) != null,
          where + ": root " + r + " is not a node or kind of family " + layout.family.name());
      require(roots.add(r), where + ": root " + r + " is listed twice");
    }
    String computed = layout.fingerprint(f);
    if (f.pinned() != null) {
      require(f.pinned().matches("[0-9a-f]{16}"), where + ": pinned= is 16 lowercase hex digits");
      require(
          f.pinned().equals(computed),
          where
              + ": layout fingerprint is "
              + computed
              + " but pinned="
              + f.pinned()
              + "; the byte layout changed, so bump version= and update pinned=");
    } else
      require(
          f.draft(),
          where
              + ": layout fingerprint is "
              + computed
              + "; add pinned="
              + computed
              + " (or draft=true while the layout is still changing)");
  }

  private static void members(
      PersistedLayout layout, String where, List<Member> members, boolean node) {
    var generated = new HashSet<String>();
    for (Member m : members) {
      String name = m.name();
      require(
          name.matches(MEMBER_NAME) && !RESERVED_MEMBERS.contains(name),
          where + ": invalid or reserved member name: " + name);
      Validator.identifier(name);
      for (String g : generatedNames(m))
        require(generated.add(g), where + ": member " + name + " collides with generated " + g);
      String at = where + " member " + name;
      if (!node)
        require(
            m.kind() == MemberKind.FIELD
                || m.kind() == MemberKind.STRING
                || m.kind() == MemberKind.REF,
            at + ": an element holds only field, string and ref members");
      switch (m.kind()) {
        case FIELD -> {
          require(
              FIELD_SCALARS.contains(m.scalar()),
              at + ": unsupported field type " + m.scalar().text());
          if (m.group() != null) {
            require(
                m.scalar() != Scalar.BOOL && m.scalar() != Scalar.F32 && m.scalar() != Scalar.F64,
                at + ": enum= and flags= need an integer field");
            if (m.group().kind() == GroupKind.ENUM) {
              EnumGroup e = layout.enumGroup(m.group().name());
              require(e != null, at + ": unknown enum " + m.group().name());
              require(
                  e.type() == m.scalar(),
                  at + ": enum " + e.name() + " is carried as " + e.type().text());
            } else {
              FlagSet fs = layout.flagSet(m.group().name());
              require(fs != null, at + ": unknown flags " + m.group().name());
              require(
                  fs.type() == m.scalar(),
                  at + ": flags " + fs.name() + " are carried as " + fs.type().text());
            }
          }
        }
        case REF, REFS ->
            require(
                m.target().equals("any")
                    || layout.node(m.target()) != null
                    || layout.kind(m.target()) != null,
                at
                    + ": node= names no node or kind of family "
                    + layout.family.name()
                    + ": "
                    + m.target());
        case SCALAR_LIST ->
            require(
                LIST_SCALARS.contains(m.scalar()), at + ": list type must be an integer scalar");
        case ELEMENT_LIST ->
            require(layout.element(m.target()) != null, at + ": unknown element " + m.target());
        case UNION_LIST ->
            require(layout.union(m.target()) != null, at + ": unknown union " + m.target());
        default -> {}
      }
      if (m.max() != null)
        require(m.max() >= 1 && m.max() <= Integer.MAX_VALUE, at + ": max must be 1..2147483647");
    }
  }

  /** The accessor names a member generates in Rust and Java, beside the views' own. */
  private static List<String> generatedNames(Member m) {
    String n = m.name();
    return switch (m.kind()) {
      case FIELD, BYTES, REF -> List.of(n);
      case STRING -> List.of(n, n + "_id");
      case REFS, ELEMENT_LIST, UNION_LIST -> List.of(n, n + "_count", n + "_at", n + "_first");
      case SCALAR_LIST -> List.of(n, n + "_count", n + "_at", n + "_offset", n + "_raw");
    };
  }

  /** The Java writer method for a node: its name with a lowercase first letter. */
  static String javaMethod(String node) {
    return Character.toLowerCase(node.charAt(0)) + node.substring(1);
  }

  private static void family(Set<String> families, String family, String where) {
    require(families.contains(family), where + ": unknown family " + family);
  }

  private static void typeName(String name, String what) {
    require(
        name.matches(TYPE_NAME) && !RESERVED_TYPES.contains(name) && !name.endsWith("Id"),
        "invalid or reserved " + what + " name: " + name);
    Validator.identifier(name);
  }

  private static void declareType(Set<String> types, String name, String what) {
    typeName(name, what);
    require(types.add(name), "duplicate name in family: " + name);
  }

  private static void javaClass(Map<String, String> classes, String name, String owner) {
    String other = classes.putIfAbsent(name, owner);
    require(other == null, "generated Java class " + name + " is claimed twice");
  }

  private static void require(boolean test, String message) {
    if (!test) throw new IllegalArgumentException(message);
  }
}
