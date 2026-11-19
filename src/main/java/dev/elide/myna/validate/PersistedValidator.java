// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package dev.elide.myna.validate;

import dev.elide.myna.model.Names;
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

  /** Constants of a family's Rust module that a format's spec constant must not take. */
  private static final Set<String> RESERVED_RUST_CONSTANTS =
      Set.of(
          "WIRE_RULES",
          "NONE",
          "UNTAGGED",
          "NO_UNION",
          "REFERENCED",
          "K_FIELD",
          "K_BYTES",
          "K_STRING",
          "K_REF",
          "K_REFS",
          "K_SCALARS",
          "K_ELEMENTS",
          "K_UNION",
          "NODES",
          "ELEMENTS",
          "UNIONS");

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
    for (Family f : d.families())
      at(
          d,
          "family",
          f.name(),
          () -> {
            typeName(f.name(), "family");
            require(families.add(f.name()), "duplicate family: " + f.name());
            require(
                familySnakes.add(PersistedLayout.snake(f.name())),
                "family names collide in Rust: " + f.name());
          });
    // Every declaration names a declared family.
    for (Format f : d.formats())
      at(d, "format", f.name(), () -> family(families, f.family(), "format " + f.name()));
    for (Node n : d.nodes())
      at(d, "node", n.name(), () -> family(families, n.family(), "node " + n.name()));
    for (Element e : d.elements())
      at(d, "element", e.name(), () -> family(families, e.family(), "element " + e.name()));
    for (Kind k : d.kinds())
      at(d, "kind", k.name(), () -> family(families, k.family(), "kind " + k.name()));
    for (Union u : d.unions())
      at(d, "union", u.name(), () -> family(families, u.family(), "union " + u.name()));
    var magics = new HashMap<Long, String>();
    var javaNames = new HashMap<String, String>();
    for (Format f : d.formats())
      at(
          d,
          "format",
          f.name(),
          () -> {
            String other = magics.putIfAbsent(f.magic(), f.name());
            require(other == null, "formats " + other + " and " + f.name() + " share a magic");
          });
    for (PersistedLayout layout : PersistedLayout.of(module)) family(layout, javaNames);
  }

  /**
   * Runs {@code check} for one declaration ({@code what} is node, kind, ...), prefixing a failure
   * with the declaration's descriptor line, and with the declaration when the message does not
   * already name it.
   */
  private static void at(Declarations d, String what, String name, Runnable check) {
    try {
      check.run();
    } catch (IllegalArgumentException e) {
      String message = e.getMessage();
      if (message.startsWith("line ")) throw e;
      if (!message.contains(name)) message = what + " " + name + ": " + message;
      int line = d.line(what + " " + name);
      throw new IllegalArgumentException(line > 0 ? "line " + line + ": " + message : message, e);
    }
  }

  private static void family(PersistedLayout layout, Map<String, String> javaNames) {
    var d = layout.module.persisted();
    String family = layout.family.name();
    // Nodes, kinds, elements, unions and formats share one type namespace per family.
    var types = new HashSet<String>();
    for (Node n : layout.nodes) at(d, "node", n.name(), () -> declareType(types, n.name(), "node"));
    for (Kind k : layout.kinds) at(d, "kind", k.name(), () -> declareType(types, k.name(), "kind"));
    for (Element e : layout.elements)
      at(d, "element", e.name(), () -> declareType(types, e.name(), "element"));
    for (Union u : layout.unions)
      at(d, "union", u.name(), () -> declareType(types, u.name(), "union"));
    for (Format f : layout.formats)
      at(d, "format", f.name(), () -> declareType(types, f.name(), "format"));
    // Java classes are one package per module.
    at(
        d,
        "family",
        family,
        () -> {
          javaClass(javaNames, family + "Nodes", family);
          javaClass(javaNames, family + "Writer", family);
          javaClass(javaNames, family + "Exception", family);
        });
    var tags = new HashMap<Integer, String>();
    var writerMethods = new HashSet<>(RESERVED_WRITER_METHODS);
    var formatConstants = new HashSet<>(RESERVED_RUST_CONSTANTS);
    for (Format f : layout.formats)
      at(
          d,
          "format",
          f.name(),
          () -> {
            javaClass(javaNames, f.name() + "Reader", f.name());
            String finish = "finish_" + PersistedLayout.snake(f.name());
            require(writerMethods.add(finish), "writer method collides: " + finish);
            // The format's Rust spec constant shares the family module with the shared core's.
            String constant = Names.upperSnake(f.name());
            require(formatConstants.add(constant), "Rust constant collides: " + constant);
          });
    for (Node n : layout.nodes)
      at(
          d,
          "node",
          n.name(),
          () -> {
            require(
                n.tag() >= 1 && n.tag() <= 0xFFFF, "node " + n.name() + ": tag must be 1..65535");
            String other = tags.putIfAbsent(n.tag(), n.name());
            require(other == null, "nodes " + other + " and " + n.name() + " share tag " + n.tag());
            String method = PersistedLayout.rustWriterMethod(n.name());
            require(
                writerMethods.add(method),
                "node " + n.name() + ": writer method collides: " + method);
            members(layout, "node", n.name(), n.members(), true);
          });
    for (Element e : layout.elements)
      at(
          d,
          "element",
          e.name(),
          () -> {
            String where = "element " + e.name();
            if (e.tag() != null)
              require(e.tag() >= 0 && e.tag() <= 0xFFFF, where + ": tag must be 0..65535");
            else require(!e.members().isEmpty(), where + ": an untagged element needs a member");
            members(layout, "element", e.name(), e.members(), false);
          });
    for (Kind k : layout.kinds)
      at(
          d,
          "kind",
          k.name(),
          () -> {
            require(!k.nodes().isEmpty(), "kind " + k.name() + " lists no nodes");
            var seen = new HashSet<String>();
            for (String n : k.nodes()) {
              require(layout.node(n) != null, "kind " + k.name() + ": unknown node " + n);
              require(seen.add(n), "kind " + k.name() + " lists " + n + " twice");
            }
          });
    for (Union u : layout.unions)
      at(
          d,
          "union",
          u.name(),
          () -> {
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
                  "union "
                      + u.name()
                      + ": elements "
                      + other
                      + " and "
                      + name
                      + " share tag "
                      + e.tag());
            }
          });
    // An element is used tagged through unions or untagged through element=, never both.
    for (Node n : layout.nodes)
      for (Member m : n.members())
        if (m.kind() == MemberKind.ELEMENT_LIST) {
          Element e = layout.element(m.target());
          at(
              d,
              "node " + n.name() + " member",
              m.name(),
              () ->
                  require(
                      e.tag() == null,
                      "node " + n.name() + ": element " + e.name() + " is tagged; use union="));
        }
    for (Format f : layout.formats) at(d, "format", f.name(), () -> format(layout, f));
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
      lowerName(where + ": header word", f.headerWord());
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
      PersistedLayout layout, String what, String owner, List<Member> members, boolean node) {
    var d = layout.module.persisted();
    var generated = new HashSet<String>();
    for (Member m : members)
      at(
          d,
          what + " " + owner + " member",
          m.name(),
          () -> member(layout, what + " " + owner, m, node, generated));
  }

  private static void member(
      PersistedLayout layout, String where, Member m, boolean node, Set<String> generated) {
    {
      String name = m.name();
      require(
          name.matches(MEMBER_NAME),
          where + ": a member name is lowercase letters, digits and underscores: " + name);
      require(
          !RESERVED_MEMBERS.contains(name),
          where
              + ": member name "
              + name
              + " is taken by a generated accessor ("
              + String.join(", ", RESERVED_MEMBERS.stream().sorted().toList())
              + ")");
      lowerName(where + ": member", name);
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

  private static void family(Set<String> families, String family, String where) {
    require(families.contains(family), where + ": unknown family " + family);
  }

  private static void typeName(String name, String what) {
    require(
        name.matches(TYPE_NAME) && !RESERVED_TYPES.contains(name) && !name.endsWith("Id"),
        "invalid or reserved " + what + " name: " + name);
    // Derived names (a writer method, the family's Rust module) escape keywords, so only the name
    // itself must not be one; Self is the only upper camel case keyword and is reserved above.
  }

  /**
   * A member or header word, which Rust uses as written and Java in lower camel case: neither may
   * be a keyword of its language. Formats emit no C, so C and C++ keywords are allowed.
   */
  private static void lowerName(String where, String name) {
    require(
        !PersistedLayout.RUST_KEYWORDS.contains(name), where + " " + name + " is a Rust keyword");
    String java = Names.lowerCamel(name.toUpperCase(Locale.ROOT));
    require(
        !PersistedLayout.JAVA_KEYWORDS.contains(java),
        where + " " + name + " is the Java keyword " + java);
    require(
        !OBJECT_METHODS.contains(java),
        where + " " + name + " would override java.lang.Object." + java + "()");
  }

  /** Methods of {@code java.lang.Object} a Java accessor named after a member would override. */
  private static final Set<String> OBJECT_METHODS =
      Set.of(
          "getClass",
          "hashCode",
          "equals",
          "toString",
          "clone",
          "finalize",
          "wait",
          "notify",
          "notifyAll");

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
