// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package dev.elide.myna.model;

import dev.elide.myna.model.Seam.GroupRef;
import dev.elide.myna.model.Seam.Scalar;
import java.util.*;

/**
 * Persisted formats (myna#167): versioned byte layouts of tagged nodes with backward references,
 * declared as families of nodes and the formats over them. See {@code docs/formats.md}.
 */
public final class Persisted {
  private Persisted() {}

  /** Bumped only when Myna changes how a descriptor maps to bytes; part of every layout hash. */
  public static final int WIRE_RULES = 1;

  /** The fixed header size in bytes. */
  public static final int HEADER_BYTES = 40;

  /** The encoded absent optional reference. */
  public static final long NO_REF = 0xFFFF_FFFFL;

  public record Family(String name) {}

  /** The five required format limits. */
  public record Limits(
      long maxBytes, long maxNodes, long maxStrings, long maxDepth, long maxWork) {}

  public record Format(
      String name,
      String family,
      long version,
      long magic,
      String pinned,
      boolean draft,
      Limits limits,
      String headerWord,
      List<String> roots) {
    public Format {
      roots = List.copyOf(roots);
    }
  }

  public enum Sharing {
    INTERNED,
    UNIQUE;

    public String text() {
      return name().toLowerCase(Locale.ROOT);
    }
  }

  public enum MemberKind {
    /** A scalar in the fixed part. */
    FIELD("field"),
    /** A length-prefixed byte payload. */
    BYTES("bytes"),
    /** A string-table id. */
    STRING("string"),
    /** One backward node reference. */
    REF("ref"),
    /** A counted list of backward node references. */
    REFS("refs"),
    /** A counted list of scalars. */
    SCALAR_LIST("list"),
    /** A counted list of untagged inline elements. */
    ELEMENT_LIST("elements"),
    /** A counted list of tagged inline elements. */
    UNION_LIST("union");

    private final String text;

    MemberKind(String text) {
      this.text = text;
    }

    public String text() {
      return text;
    }

    /** Whether the member has a variable payload after the fixed part. */
    public boolean variable() {
      return this == BYTES
          || this == REFS
          || this == SCALAR_LIST
          || this == ELEMENT_LIST
          || this == UNION_LIST;
    }
  }

  /**
   * One member of a node or element, in wire order. {@code scalar} is the field or list scalar;
   * {@code target} names the {@code node=} target (a node, a kind or {@code any}), the element of
   * {@code element=} or the union of {@code union=}; {@code max} bounds a count or byte length.
   */
  public record Member(
      String name,
      MemberKind kind,
      Scalar scalar,
      GroupRef group,
      String target,
      boolean optional,
      Long max) {}

  public record Node(String name, String family, int tag, Sharing sharing, List<Member> members) {
    public Node {
      members = List.copyOf(members);
    }
  }

  /**
   * An inline element; {@code tag} is null for an element used untagged through {@code element=}.
   */
  public record Element(String name, String family, Integer tag, List<Member> members) {
    public Element {
      members = List.copyOf(members);
    }
  }

  public record Kind(String name, String family, List<String> nodes) {
    public Kind {
      nodes = List.copyOf(nodes);
    }
  }

  public record Union(String name, String family, List<String> elements) {
    public Union {
      elements = List.copyOf(elements);
    }
  }

  /** Every persisted-format declaration of a module, each list sorted by name. */
  public record Declarations(
      List<Family> families,
      List<Format> formats,
      List<Node> nodes,
      List<Element> elements,
      List<Kind> kinds,
      List<Union> unions) {
    public static final Declarations EMPTY =
        new Declarations(List.of(), List.of(), List.of(), List.of(), List.of(), List.of());

    public Declarations {
      families = sorted(families, Comparator.comparing(Family::name));
      formats = sorted(formats, Comparator.comparing(Format::name));
      nodes = sorted(nodes, Comparator.comparing(Node::name));
      elements = sorted(elements, Comparator.comparing(Element::name));
      kinds = sorted(kinds, Comparator.comparing(Kind::name));
      unions = sorted(unions, Comparator.comparing(Union::name));
    }

    /**
     * {@code items} sorted and unmodifiable; most modules declare none, so that path allocates
     * nothing.
     */
    private static <T> List<T> sorted(List<T> items, Comparator<T> order) {
      if (items.isEmpty()) return List.of();
      var copy = new ArrayList<>(items);
      copy.sort(order);
      return List.copyOf(copy);
    }

    public boolean isEmpty() {
      return families.isEmpty()
          && formats.isEmpty()
          && nodes.isEmpty()
          && elements.isEmpty()
          && kinds.isEmpty()
          && unions.isEmpty();
    }
  }
}
