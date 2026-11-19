// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package dev.elide.myna.model;

import dev.elide.myna.model.Persisted.*;
import dev.elide.myna.model.Seam.*;
import dev.elide.myna.model.Seam.Module;
import java.math.BigInteger;
import java.util.*;

/**
 * The resolved byte layout of one family of a validated module: tags, offsets, sizes, reference
 * targets and the per-format layout fingerprint. Every backend and the validator read layouts from
 * here, so they cannot disagree.
 */
public final class PersistedLayout {
  public final Module module;
  public final Family family;

  /** The family's nodes, sorted by tag. */
  public final List<Node> nodes;

  /** The family's elements, sorted by name. */
  public final List<Element> elements;

  public final List<Kind> kinds;
  public final List<Union> unions;

  /** The family's formats, sorted by name. */
  public final List<Format> formats;

  private PersistedLayout(Module module, Family family) {
    this.module = module;
    this.family = family;
    var d = module.persisted();
    String f = family.name();
    nodes =
        d.nodes().stream()
            .filter(n -> n.family().equals(f))
            .sorted(Comparator.comparingInt(Node::tag))
            .toList();
    elements = d.elements().stream().filter(e -> e.family().equals(f)).toList();
    kinds = d.kinds().stream().filter(k -> k.family().equals(f)).toList();
    unions = d.unions().stream().filter(u -> u.family().equals(f)).toList();
    formats = d.formats().stream().filter(x -> x.family().equals(f)).toList();
  }

  public static List<PersistedLayout> of(Module module) {
    return module.persisted().families().stream().map(f -> new PersistedLayout(module, f)).toList();
  }

  public static PersistedLayout of(Module module, String family) {
    for (Family f : module.persisted().families())
      if (f.name().equals(family)) return new PersistedLayout(module, f);
    throw new IllegalArgumentException("unknown family: " + family);
  }

  public Node node(String name) {
    for (Node n : nodes) if (n.name().equals(name)) return n;
    return null;
  }

  public Node nodeByTag(int tag) {
    for (Node n : nodes) if (n.tag() == tag) return n;
    return null;
  }

  public Element element(String name) {
    for (Element e : elements) if (e.name().equals(name)) return e;
    return null;
  }

  public Kind kind(String name) {
    for (Kind k : kinds) if (k.name().equals(name)) return k;
    return null;
  }

  public Union union(String name) {
    for (Union u : unions) if (u.name().equals(name)) return u;
    return null;
  }

  /** A union's elements, sorted by tag. */
  public List<Element> unionElements(Union u) {
    return u.elements().stream()
        .map(this::element)
        .sorted(Comparator.comparingInt(Element::tag))
        .toList();
  }

  /** The bytes a member occupies in the fixed part. */
  public static int width(Member m) {
    return m.kind() == MemberKind.FIELD ? scalarWidth(m.scalar()) : 4;
  }

  public static int scalarWidth(Scalar s) {
    return s == Scalar.BOOL ? 1 : s.bytes;
  }

  public static long pad4(long n) {
    return (n + 3) & ~3L;
  }

  /** The offset of each member's fixed slot from the start of its node or element. */
  public static int[] offsets(List<Member> members, int start) {
    int[] result = new int[members.size()];
    int at = start;
    for (int i = 0; i < members.size(); i++) {
      result[i] = at;
      at += width(members.get(i));
    }
    return result;
  }

  /** The node's tag word plus its fixed part, before the padding to 4. */
  public static int fixedEnd(Node n) {
    int at = 4;
    for (Member m : n.members()) at += width(m);
    return at;
  }

  /** An element's size: its tag word when tagged, its members, padded to 4. */
  public static int elementSize(Element e) {
    int at = e.tag() == null ? 0 : 4;
    for (Member m : e.members()) at += width(m);
    return (int) pad4(at);
  }

  /** The node tags a {@code node=} target admits, sorted. */
  public SortedSet<Integer> targets(String target) {
    var result = new TreeSet<Integer>();
    if (target.equals("any")) {
      for (Node n : nodes) result.add(n.tag());
      return result;
    }
    Node n = node(target);
    if (n != null) {
      result.add(n.tag());
      return result;
    }
    Kind k = kind(target);
    if (k != null) for (String name : k.nodes()) result.add(node(name).tag());
    return result;
  }

  /** The members that can hold node references: the node's own and those of its elements. */
  private List<Member> referenceMembers(Node n) {
    var result = new ArrayList<Member>();
    for (Member m : n.members()) {
      switch (m.kind()) {
        case REF, REFS -> result.add(m);
        case ELEMENT_LIST -> result.addAll(element(m.target()).members());
        case UNION_LIST -> {
          for (Element e : unionElements(union(m.target()))) result.addAll(e.members());
        }
        default -> {}
      }
    }
    return result.stream()
        .filter(m -> m.kind() == MemberKind.REF || m.kind() == MemberKind.REFS)
        .toList();
  }

  /** The node tags a format's root line admits, sorted. */
  public SortedSet<Integer> roots(Format f) {
    var result = new TreeSet<Integer>();
    for (String r : f.roots()) result.addAll(targets(r));
    return result;
  }

  /**
   * The node tags a format accepts: everything reachable from its roots through declared targets.
   */
  public SortedSet<Integer> accepted(Format f) {
    var result = new TreeSet<Integer>();
    var work = new ArrayDeque<>(roots(f));
    while (!work.isEmpty()) {
      int tag = work.pop();
      if (!result.add(tag)) continue;
      for (Member m : referenceMembers(nodeByTag(tag))) work.addAll(targets(m.target()));
    }
    return result;
  }

  public EnumGroup enumGroup(String name) {
    for (EnumGroup e : module.enums()) if (e.name().equals(name)) return e;
    return null;
  }

  public FlagSet flagSet(String name) {
    for (FlagSet f : module.flagSets()) if (f.name().equals(name)) return f;
    return null;
  }

  /** The canonical JSON the layout fingerprint hashes: numbers and fixed keys only, no names. */
  public String layoutJson(Format f) {
    var out = new StringBuilder("{\"wire\":").append(Persisted.WIRE_RULES);
    out.append(",\"magic\":").append(f.magic());
    out.append(",\"version\":").append(f.version());
    out.append(",\"header\":").append(f.headerWord() != null);
    out.append(",\"roots\":").append(numbers(roots(f)));
    out.append(",\"nodes\":[");
    boolean first = true;
    for (int tag : accepted(f)) {
      Node n = nodeByTag(tag);
      if (!first) out.append(',');
      first = false;
      out.append("{\"tag\":").append(tag).append(",\"sharing\":\"").append(n.sharing().text());
      out.append("\",\"members\":").append(membersJson(n.members())).append('}');
    }
    return out.append("]}").toString();
  }

  private String membersJson(List<Member> members) {
    var parts = new ArrayList<String>();
    for (Member m : members) {
      var out = new StringBuilder("{\"kind\":\"").append(m.kind().text()).append('"');
      if (m.scalar() != null) out.append(",\"scalar\":\"").append(m.scalar().text()).append('"');
      if (m.optional()) out.append(",\"optional\":true");
      if (m.max() != null) out.append(",\"max\":").append(m.max());
      switch (m.kind()) {
        case REF, REFS -> out.append(",\"targets\":").append(numbers(targets(m.target())));
        case ELEMENT_LIST ->
            out.append(",\"elements\":[").append(elementJson(element(m.target()))).append(']');
        case UNION_LIST -> {
          var each = new ArrayList<String>();
          for (Element e : unionElements(union(m.target()))) each.add(elementJson(e));
          out.append(",\"elements\":[").append(String.join(",", each)).append(']');
        }
        default -> {}
      }
      if (m.group() != null) {
        if (m.group().kind() == GroupKind.ENUM) {
          var values = new TreeSet<BigInteger>();
          for (EnumValue v : enumGroup(m.group().name()).values()) values.add(v.value());
          out.append(",\"values\":[");
          var text = new ArrayList<String>();
          for (BigInteger v : values) text.add(v.toString());
          out.append(String.join(",", text)).append(']');
        } else {
          out.append(",\"mask\":").append(flagSet(m.group().name()).all());
        }
      }
      parts.add(out.append('}').toString());
    }
    return "[" + String.join(",", parts) + "]";
  }

  private String elementJson(Element e) {
    return "{\"tag\":"
        + (e.tag() == null ? "null" : e.tag().toString())
        + ",\"members\":"
        + membersJson(e.members())
        + "}";
  }

  private static String numbers(Collection<Integer> values) {
    var text = new ArrayList<String>();
    for (int v : values) text.add(Integer.toString(v));
    return "[" + String.join(",", text) + "]";
  }

  /** The first 8 bytes of the SHA-256 of {@link #layoutJson}, as 16 lowercase hex digits. */
  public String fingerprint(Format f) {
    return dev.elide.myna.serialize.SeamJson.fingerprintOf(layoutJson(f)).substring(0, 16);
  }

  /** {@link #fingerprint} as the little-endian {@code u64} the file header carries. */
  public long fingerprintWord(Format f) {
    byte[] bytes = HexFormat.of().parseHex(fingerprint(f));
    long value = 0;
    for (int i = 7; i >= 0; i--) value = (value << 8) | (bytes[i] & 0xff);
    return value;
  }

  /** Lower snake case of an upper camel case name. */
  public static String snake(String camel) {
    return Names.lowerSnake(camel);
  }

  /**
   * Rust keywords, strict and reserved, through edition 2024. Formats emit only Rust and Java, so
   * these and {@link #JAVA_KEYWORDS} are the only keywords their names must avoid.
   */
  public static final Set<String> RUST_KEYWORDS =
      Set.of(
          ("as async await break const continue crate dyn else enum extern false fn for if impl in"
               + " let loop match mod move mut pub ref return self Self static struct super trait"
               + " true type unsafe use where while abstract become box do final macro override"
               + " priv typeof unsized virtual yield try gen")
              .split(" "));

  /** Java keywords, literals and the restricted identifiers a generated name could hit. */
  public static final Set<String> JAVA_KEYWORDS =
      Set.of(
          ("abstract assert boolean break byte case catch char class const continue default do"
                  + " double else enum extends final finally float for goto if implements import"
                  + " instanceof int interface long native new package private protected public"
                  + " return short static strictfp super switch synchronized this throw throws"
                  + " transient try void volatile while true false null var yield record sealed"
                  + " permits")
              .split(" "));

  /**
   * A Rust name derived from a declared type name (a family module or a writer method): {@code
   * Return} becomes {@code return_} rather than the keyword.
   */
  public static String rustEscape(String derived) {
    return RUST_KEYWORDS.contains(derived) ? derived + "_" : derived;
  }

  /** {@link #rustEscape} for Java: a node {@code Return} has the writer method {@code return_}. */
  public static String javaEscape(String derived) {
    return JAVA_KEYWORDS.contains(derived) ? derived + "_" : derived;
  }

  /** The Rust writer method of a node. */
  public static String rustWriterMethod(String node) {
    return rustEscape(snake(node));
  }

  /** The Java writer method of a node: its name with a lowercase first letter. */
  public static String javaWriterMethod(String node) {
    return javaEscape(Character.toLowerCase(node.charAt(0)) + node.substring(1));
  }

  /** The Rust module of a family. */
  public static String rustModule(String family) {
    return rustEscape(snake(family));
  }
}
