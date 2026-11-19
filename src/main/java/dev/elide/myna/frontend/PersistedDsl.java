// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package dev.elide.myna.frontend;

import static dev.elide.myna.frontend.Dsl.*;

import dev.elide.myna.model.Persisted;
import dev.elide.myna.model.Seam.Scalar;
import java.math.BigInteger;
import java.util.*;

/**
 * The persisted-format directives of {@link Dsl} (myna#167), kept out of {@link Dsl#parse} so
 * descriptors without formats run the parser's original code. {@code Dsl} creates one on the first
 * such directive.
 */
final class PersistedDsl {
  /** The one-line directives {@link #line} handles. */
  static final Set<String> DIRECTIVES =
      Set.of("family", "kind", "union", "limit", "header", "root", "string", "ref", "refs", "list");

  private final List<Persisted.Family> families = new ArrayList<>();
  private final List<Persisted.Format> formats = new ArrayList<>();
  private final List<Persisted.Node> nodes = new ArrayList<>();
  private final List<Persisted.Element> elements = new ArrayList<>();
  private final List<Persisted.Kind> kinds = new ArrayList<>();
  private final List<Persisted.Union> unions = new ArrayList<>();
  private List<Persisted.Member> members = new ArrayList<>();
  private Map<String, Long> limits = new LinkedHashMap<>();
  private List<String> roots = new ArrayList<>();
  private String headerWord;

  /** The source line of each declaration and member; see {@link Persisted.Declarations#lines}. */
  private final Map<String, Integer> lines = new HashMap<>();

  /** A {@code format}, {@code node} or {@code element} block starts on {@code line}. */
  void open(String[] header, int line) {
    lines.putIfAbsent(header[0] + " " + header[1], line);
    members = new ArrayList<>();
    limits = new LinkedHashMap<>();
    roots = new ArrayList<>();
    headerWord = null;
  }

  /**
   * One persisted-format line, source line {@code line}; {@code header} is the open block's header
   * line, or null.
   */
  void line(String[] words, String[] header, int line) {
    if (words.length >= 2) {
      if (header == null) lines.putIfAbsent(words[0] + " " + words[1], line);
      else if (header[0].equals("node") || header[0].equals("element"))
        lines.putIfAbsent(header[0] + " " + header[1] + " member " + words[1], line);
    }
    switch (words[0]) {
      case "family" -> {
        require(header == null && words.length == 2, "expected family NAME outside a block");
        families.add(new Persisted.Family(words[1]));
      }
      case "kind", "union" -> {
        require(
            header == null && words.length >= 2,
            "expected " + words[0] + " NAME family=FAMILY outside a block");
        var opts = options(words, 2);
        String family = takeRequired(opts, "family");
        if (words[0].equals("kind"))
          kinds.add(new Persisted.Kind(words[1], family, names(takeRequired(opts, "nodes"))));
        else
          unions.add(new Persisted.Union(words[1], family, names(takeRequired(opts, "elements"))));
        empty(opts);
      }
      case "limit" -> {
        require(header != null && header[0].equals("format"), "limit requires a format block");
        require(words.length == 2, "expected: limit KEY=VALUE");
        var opts = options(words, 1);
        var entry = opts.entrySet().iterator().next();
        require(
            limits.putIfAbsent(entry.getKey(), unsigned(entry.getValue())) == null,
            "duplicate limit: " + entry.getKey());
      }
      case "header" -> {
        require(header != null && header[0].equals("format"), "header requires a format block");
        require(words.length == 2, "expected: header NAME");
        require(headerWord == null, "a format declares at most one header word");
        headerWord = words[1];
      }
      case "root" -> {
        require(header != null && header[0].equals("format"), "root requires a format block");
        require(words.length == 2, "expected: root NODE[,NODE...]");
        require(roots.isEmpty(), "a format declares its roots once");
        roots.addAll(names(words[1]));
      }
      default -> {
        require(
            header != null && (header[0].equals("node") || header[0].equals("element")),
            words[0] + " requires a node or element block");
        require(words.length >= 2, "expected: " + words[0] + " NAME");
        members.add(member(words));
      }
    }
  }

  /** The {@code end} of a {@code format}, {@code node} or {@code element} block. */
  void end(String[] header, Map<String, String> options) {
    switch (header[0]) {
      case "format" -> formats.add(format(header[1], options, limits, headerWord, roots));
      case "node" -> {
        String family = takeRequired(options, "family");
        int tag = tag(takeRequired(options, "tag"));
        String sharing = takeRequired(options, "sharing");
        require(
            sharing.equals("interned") || sharing.equals("unique"),
            "sharing is interned or unique: " + sharing);
        nodes.add(
            new Persisted.Node(
                header[1],
                family,
                tag,
                Persisted.Sharing.valueOf(sharing.toUpperCase(Locale.ROOT)),
                members));
        empty(options);
      }
      default -> {
        String family = takeRequired(options, "family");
        String tag = take(options, "tag", null);
        elements.add(
            new Persisted.Element(header[1], family, tag == null ? null : tag(tag), members));
        empty(options);
      }
    }
  }

  Persisted.Declarations declarations() {
    return new Persisted.Declarations(families, formats, nodes, elements, kinds, unions, lines);
  }

  static Persisted.Format format(
      String name,
      Map<String, String> options,
      Map<String, Long> limits,
      String headerWord,
      List<String> roots) {
    String family = takeRequired(options, "family");
    long version = unsigned(takeRequired(options, "version"));
    long magic = unsigned(takeRequired(options, "magic"));
    String pinned = take(options, "pinned", null);
    boolean draft = bool(take(options, "draft", "false"));
    empty(options);
    for (String key : limits.keySet()) require(LIMITS.contains(key), "unknown limit: " + key);
    for (String key : LIMITS) require(limits.containsKey(key), "missing limit " + key);
    require(!roots.isEmpty(), "format " + name + " requires a root line");
    return new Persisted.Format(
        name,
        family,
        version,
        magic,
        pinned,
        draft,
        new Persisted.Limits(
            limits.get("max_bytes"),
            limits.get("max_nodes"),
            limits.get("max_strings"),
            limits.get("max_depth"),
            limits.get("max_work")),
        headerWord,
        roots);
  }

  static final List<String> LIMITS =
      List.of("max_bytes", "max_nodes", "max_strings", "max_depth", "max_work");

  /** One member line of a node or element block: field, string, ref, refs or list. */
  static Persisted.Member member(String[] words) {
    var opts = options(words, 2);
    String memberName = words[1];
    Persisted.Member result;
    switch (words[0]) {
      case "field" -> {
        String type = takeRequired(opts, "type");
        require(!opts.containsKey("offset"), "offset= is assigned by Myna in a node or element");
        if (type.equals("bytes")) {
          result =
              new Persisted.Member(
                  memberName, Persisted.MemberKind.BYTES, null, null, null, false, max(opts));
        } else {
          var scalar = scalar(type);
          result =
              new Persisted.Member(
                  memberName, Persisted.MemberKind.FIELD, scalar, group(opts), null, false, null);
        }
      }
      case "string" ->
          result =
              new Persisted.Member(
                  memberName, Persisted.MemberKind.STRING, null, null, null, false, null);
      case "ref" -> {
        String target = takeRequired(opts, "node");
        boolean optional = bool(take(opts, "optional", "false"));
        result =
            new Persisted.Member(
                memberName, Persisted.MemberKind.REF, null, null, target, optional, null);
      }
      case "refs" ->
          result =
              new Persisted.Member(
                  memberName,
                  Persisted.MemberKind.REFS,
                  null,
                  null,
                  takeRequired(opts, "node"),
                  false,
                  max(opts));
      default -> {
        String type = take(opts, "type", null),
            element = take(opts, "element", null),
            union = take(opts, "union", null);
        require(
            (type != null ? 1 : 0) + (element != null ? 1 : 0) + (union != null ? 1 : 0) == 1,
            "list takes exactly one of type=, element= and union=");
        if (type != null)
          result =
              new Persisted.Member(
                  memberName,
                  Persisted.MemberKind.SCALAR_LIST,
                  scalar(type),
                  null,
                  null,
                  false,
                  max(opts));
        else
          result =
              new Persisted.Member(
                  memberName,
                  element != null
                      ? Persisted.MemberKind.ELEMENT_LIST
                      : Persisted.MemberKind.UNION_LIST,
                  null,
                  null,
                  element != null ? element : union,
                  false,
                  max(opts));
      }
    }
    empty(opts);
    return result;
  }

  static Scalar scalar(String text) {
    for (Scalar s : Scalar.values()) if (s.text().equals(text)) return s;
    throw new IllegalArgumentException("unknown scalar type: " + text);
  }

  static Long max(Map<String, String> opts) {
    String max = take(opts, "max", null);
    return max == null ? null : unsigned(max);
  }

  static int tag(String text) {
    long tag = unsigned(text);
    require(tag <= 0xFFFF, "tag must fit u16: " + text);
    return (int) tag;
  }

  /** A non-negative integer of at most 32 bits, decimal or {@code 0x} hex. */
  static long unsigned(String text) {
    BigInteger value = integer(text);
    require(
        value.signum() >= 0 && value.bitLength() <= 32,
        "expected an unsigned 32-bit integer: " + text);
    return value.longValueExact();
  }

  static List<String> names(String text) {
    var result = List.of(text.split(",", -1));
    for (String n : result) require(!n.isEmpty(), "empty name in list: " + text);
    return result;
  }
}
