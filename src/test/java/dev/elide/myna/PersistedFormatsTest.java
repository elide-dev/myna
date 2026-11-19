// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package dev.elide.myna;

import static org.junit.jupiter.api.Assertions.*;

import dev.elide.myna.frontend.Dsl;
import dev.elide.myna.model.Persisted;
import dev.elide.myna.model.PersistedLayout;
import dev.elide.myna.model.Seam.Module;
import dev.elide.myna.serialize.SeamJson;
import dev.elide.myna.validate.Validator;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;

/**
 * Persisted formats (myna#167): descriptor parsing and validation, the layout fingerprint and the
 * generated files. {@code tests/formats_integration.py} compiles and runs the generated code.
 */
class PersistedFormatsTest {
  private static final String HEAD = "module m target=x86_64-unknown-linux-gnu\n";

  /** A small family with every member kind; formats are drafts unless a test pins them. */
  private static final String BASE =
      """
      enum E type=u8
        value a = 0
        value b = 1
      end
      flags G type=u32
        bit x = 0
      end
      family F
      format Fmt family=F version=1 magic=0x1 draft=true
        limit max_bytes=1024
        limit max_nodes=16
        limit max_strings=16
        limit max_depth=8
        limit max_work=64
        header cap
        root Root
      end
      element El family=F
        string s
      end
      element Tg family=F tag=0
        field v type=u8 enum=E
      end
      union U family=F elements=Tg
      kind K family=F nodes=Leaf
      node Leaf family=F tag=1 sharing=interned
        field f type=u32 flags=G
      end
      node Root family=F tag=2 sharing=unique
        ref leaf node=Leaf
        refs leaves node=K max=4
        list els element=El
        list us union=U
        list xs type=u16
        field data type=bytes
        string name
        ref other node=any optional=true
      end
      """;

  private static Module parse(String body) {
    return Dsl.parse(HEAD + body);
  }

  private static String reject(String body) {
    return assertThrows(IllegalArgumentException.class, () -> Validator.validate(parse(body), true))
        .getMessage();
  }

  /** {@link #BASE} with one replacement, which must apply. */
  private static String base(String from, String to) {
    assertTrue(BASE.contains(from), from);
    return BASE.replace(from, to);
  }

  private static String fingerprint(String body) {
    Module m = parse(body);
    Validator.validate(m, true);
    var layout = PersistedLayout.of(m, m.persisted().families().getFirst().name());
    return layout.fingerprint(layout.formats.getFirst());
  }

  @Test
  void generatedFilesMatchGolden() throws Exception {
    var outputs =
        Generator.generate(Dsl.parse(Files.readString(Path.of("tests/formats/doc.seam"))), true);
    var golden = new TreeSet<String>();
    try (var files = Files.list(Path.of("tests/golden/formats"))) {
      files.forEach(f -> golden.add(f.getFileName().toString()));
    }
    for (String name : golden)
      assertEquals(
          Files.readString(Path.of("tests/golden/formats", name)), outputs.get(name), name);
    var formats = new TreeSet<String>();
    for (String name : outputs.keySet())
      if (name.equals("formats.rs") || name.startsWith("Doc")) formats.add(name);
    formats.add("seam.json");
    assertEquals(golden, formats);
  }

  @Test
  void otherShapesGenerate() throws Exception {
    // tests/formats_integration.py compiles these; here they only need to generate.
    var outputs =
        Generator.generate(Dsl.parse(Files.readString(Path.of("tests/formats/shapes.seam"))), true);
    String rust = outputs.get("formats.rs");
    assertTrue(rust.contains("let leaf: LeavesId = leaf.into();"), rust);
    assertTrue(rust.contains("pub struct OwnersId(u32);"), rust);
    assertTrue(
        rust.contains("#[derive(Clone, Copy, Debug, PartialEq, Eq)]\n        pub struct LeavesId"),
        rust);
    assertTrue(rust.contains("pub fn root(&self) -> AnyNode<'a>"), rust);
    assertTrue(outputs.get("ShapesWriter.java").contains("($x.w() & 0xffffL) != 65535L"));
    assertTrue(outputs.containsKey("OtherReader.java"));
  }

  @Test
  void declarationsAreEmptyOnlyWithoutAnyDeclaration() {
    var family = List.of(new Persisted.Family("F"));
    var format =
        List.of(
            new Persisted.Format(
                "X",
                "F",
                1,
                1,
                null,
                true,
                new Persisted.Limits(64, 1, 1, 1, 1),
                null,
                List.of("N")));
    var node = List.of(new Persisted.Node("N", "F", 1, Persisted.Sharing.UNIQUE, List.of()));
    var element = List.of(new Persisted.Element("E", "F", 0, List.of()));
    var kind = List.of(new Persisted.Kind("K", "F", List.of("N")));
    var union = List.of(new Persisted.Union("U", "F", List.of("E")));
    List<Persisted.Family> noFamily = List.of();
    List<Persisted.Format> noFormat = List.of();
    List<Persisted.Node> noNode = List.of();
    List<Persisted.Element> noElement = List.of();
    List<Persisted.Kind> noKind = List.of();
    List<Persisted.Union> noUnion = List.of();
    assertFalse(
        new Persisted.Declarations(family, noFormat, noNode, noElement, noKind, noUnion).isEmpty());
    assertFalse(
        new Persisted.Declarations(noFamily, format, noNode, noElement, noKind, noUnion).isEmpty());
    assertFalse(
        new Persisted.Declarations(noFamily, noFormat, node, noElement, noKind, noUnion).isEmpty());
    assertFalse(
        new Persisted.Declarations(noFamily, noFormat, noNode, element, noKind, noUnion).isEmpty());
    assertFalse(
        new Persisted.Declarations(noFamily, noFormat, noNode, noElement, kind, noUnion).isEmpty());
    assertFalse(
        new Persisted.Declarations(noFamily, noFormat, noNode, noElement, noKind, union).isEmpty());
  }

  @Test
  void modulesWithoutFamiliesGenerateNothingNew() {
    var outputs = Generator.generate(parse("opaque Buffer\n"), true);
    assertFalse(outputs.containsKey("formats.rs"));
    assertFalse(outputs.get("seam.json").contains("families"));
    assertTrue(Persisted.Declarations.EMPTY.isEmpty());
  }

  @Test
  void canonicalJsonRecordsFamiliesAndFormats() {
    String json = SeamJson.canonical(parse(BASE));
    assertTrue(json.contains("\"families\": [{\"name\":\"F\""), json);
    assertTrue(json.contains("\"sharing\":\"interned\""), json);
    assertTrue(json.contains("\"kind\":\"union\""), json);
    assertTrue(json.contains("\"headerWord\":\"cap\""), json);
    assertTrue(json.contains("\"elements\":[{\"name\":\"El\",\"members\""), json);
    assertTrue(json.contains("{\"name\":\"Tg\",\"tag\":0,"), json);
    assertTrue(json.contains("\"target\":\"any\",\"optional\":true"), json);
    assertTrue(json.contains("\"kind\":\"refs\",\"target\":\"K\",\"max\":4"), json);
  }

  @Test
  void layoutFingerprintIgnoresNamesLimitsAndOtherFormats() {
    String fp = fingerprint(BASE);
    // Renaming anything keeps files on disk valid.
    String renamed =
        BASE.replace("family F", "family Renamed")
            .replace("family=F", "family=Renamed")
            .replace("Fmt", "Fmt2")
            .replace("Leaf", "Leaf2")
            .replace("ref leaf", "ref leaf2")
            .replace("string name", "string title")
            .replace("header cap", "header flag")
            .replace("value a = 0", "value zero = 0")
            .replace("bit x = 0", "bit y = 0")
            .replace("element El", "element Elem")
            .replace("element=El", "element=Elem")
            .replace("union U family", "union U2 family")
            .replace("union=U", "union=U2");
    assertEquals(fp, fingerprint(renamed));
    assertEquals(fp, fingerprint(BASE.replace("limit max_work=64", "limit max_work=65")));
    assertEquals(fp, fingerprint(BASE.replace("limit max_bytes=1024", "limit max_bytes=2048")));
    // A node that only another format reaches (`any` would reach it too).
    String noAny = base("node=any", "node=Leaf");
    assertEquals(
        fingerprint(noAny),
        fingerprint(
            noAny
                + """
                node Extra family=F tag=9 sharing=interned
                end
                format Zother family=F version=1 magic=0x2 draft=true
                  limit max_bytes=1024
                  limit max_nodes=16
                  limit max_strings=16
                  limit max_depth=8
                  limit max_work=64
                  root Extra
                end
                """));
    // Anything that changes the accepted bytes changes it.
    for (String[] edit :
        new String[][] {
          {"version=1", "version=2"},
          {"magic=0x1", "magic=0x3"},
          {"  header cap\n", ""},
          {"sharing=interned", "sharing=unique"},
          {"refs leaves node=K max=4", "refs leaves node=K max=5"},
          {"value b = 1", "value b = 2"},
          {"bit x = 0", "bit x = 1"},
          {"ref other node=any optional=true", "ref other node=Leaf optional=true"},
          {"ref other node=any optional=true", "ref other node=any"},
          {"  string name\n", ""},
          {"list xs type=u16", "list xs type=u32"},
          {"element Tg family=F tag=0", "element Tg family=F tag=1"},
        }) {
      assertNotEquals(fp, fingerprint(base(edit[0], edit[1])), Arrays.toString(edit));
    }
    // Member order is wire order.
    assertNotEquals(
        fp,
        fingerprint(
            base("  string name\n  ref other", "  ref other")
                .replace("optional=true\nend", "optional=true\n  string name\nend")));
  }

  @Test
  void pinnedFingerprints() {
    String fp = fingerprint(BASE);
    Validator.validate(parse(base("draft=true", "pinned=" + fp)), true);
    Validator.validate(parse(base("draft=true", "draft=true pinned=" + fp)), true);
    String mismatch = reject(base("draft=true", "pinned=0123456789abcdef"));
    assertTrue(mismatch.contains("layout fingerprint is " + fp), mismatch);
    assertTrue(mismatch.contains("bump version="), mismatch);
    String missing = reject(base(" draft=true", ""));
    assertTrue(missing.contains("add pinned=" + fp), missing);
    assertTrue(reject(base("draft=true", "pinned=ABC")).contains("16 lowercase hex"));
  }

  @Test
  void layoutQueries() {
    Module m = parse(BASE);
    var layout = PersistedLayout.of(m, "F");
    assertNull(layout.node("Missing"));
    assertNull(layout.nodeByTag(99));
    assertNull(layout.element("Missing"));
    assertNull(layout.kind("Missing"));
    assertNull(layout.union("Missing"));
    assertNull(layout.enumGroup("Missing"));
    assertNull(layout.flagSet("Missing"));
    assertTrue(layout.targets("Missing").isEmpty());
    assertEquals(Set.of(1, 2), layout.targets("any"));
    assertEquals(Set.of(1), layout.targets("K"));
    assertEquals(Set.of(1, 2), layout.accepted(layout.formats.getFirst()));
    assertThrows(IllegalArgumentException.class, () -> PersistedLayout.of(m, "Missing"));
    // Root: tag, reserved, leaf, leaves count, els count, us count, xs count, data length, name,
    // other.
    assertEquals(4 + 4 * 8, PersistedLayout.fixedEnd(layout.node("Root")));
    // A tag word and a u8, padded.
    assertEquals(8, PersistedLayout.elementSize(layout.element("Tg")));
    assertEquals(4, PersistedLayout.elementSize(layout.element("El")));
    assertEquals(8, PersistedLayout.pad4(5));
    assertEquals(0xfedcba9876543210L, Long.reverseBytes(0x1032547698badcfeL));
    long word = layout.fingerprintWord(layout.formats.getFirst());
    String hex = layout.fingerprint(layout.formats.getFirst());
    assertEquals(Long.parseUnsignedLong(hex, 16), Long.reverseBytes(word));
  }

  @Test
  void parserRejections() {
    String[][] cases = {
      {"family\n", "expected family NAME outside a block"},
      {"kind K\n", "missing family"},
      {"kind K family=F\n", "missing nodes"},
      {"union U family=F\n", "missing elements"},
      {"kind K family=F nodes=A,,B\n", "empty name in list"},
      {"limit max_bytes=1\n", "limit requires a format block"},
      {"header cap\n", "header requires a format block"},
      {"root A\n", "root requires a format block"},
      {"string s\n", "string requires a node or element block"},
      {"node N family=F tag=1 sharing=unique\n  string\nend\n", "expected: string NAME"},
      {
        "node N family=F tag=1 sharing=unique\n  field x type=u8 offset=0\nend\n",
        "offset= is assigned"
      },
      {"node N family=F tag=1 sharing=unique\n  list x max=1\nend\n", "exactly one of type="},
      {
        "node N family=F tag=1 sharing=unique\n  list x type=u8 element=E\nend\n",
        "exactly one of type="
      },
      {"node N family=F tag=1 sharing=unique\n  field x type=u7\nend\n", "unknown scalar type: u7"},
      {"node N family=F tag=70000 sharing=unique\nend\n", "tag must fit u16"},
      {"node N family=F tag=-1 sharing=unique\nend\n", "unsigned 32-bit"},
      {"node N family=F tag=0x100000000 sharing=unique\nend\n", "unsigned 32-bit"},
      {"node N family=F tag=1 sharing=shared\nend\n", "sharing is interned or unique"},
      {"node N family=F tag=1\nend\n", "missing sharing"},
      {"element El family=F tag=1 extra=1\nend\n", "unknown options"},
      {
        "node N family=F tag=1 sharing=unique\n  refs r node=N optional=true\nend\n",
        "unknown options"
      },
    };
    for (String[] c : cases) {
      String message =
          assertThrows(IllegalArgumentException.class, () -> parse(c[0]), c[0]).getMessage();
      assertTrue(message.contains(c[1]), c[0] + " -> " + message);
    }
    String[][] formats = {
      {"  limit max_bytes=1 max_nodes=2\n", "expected: limit KEY=VALUE"},
      {"  limit max_bytes=1\n  limit max_bytes=2\n", "duplicate limit"},
      {"  limit max_other=1\n", "unknown limit: max_other"},
      {"  header\n", "expected: header NAME"},
      {"  header a\n  header b\n", "at most one header word"},
      {"  root\n", "expected: root NODE"},
      {"  root A\n  root B\n", "declares its roots once"},
    };
    for (String[] c : formats) {
      String body = "format X family=F version=1 magic=1\n" + c[0] + "end\n";
      String message =
          assertThrows(IllegalArgumentException.class, () -> parse(body), body).getMessage();
      assertTrue(message.contains(c[1]), body + " -> " + message);
    }
    String limits = "format X family=F version=1 magic=1\n  root A\nend\n";
    assertTrue(
        assertThrows(IllegalArgumentException.class, () -> parse(limits))
            .getMessage()
            .contains("missing limit max_bytes"));
    String roots =
        "format X family=F version=1 magic=1\n  limit max_bytes=1\n  limit max_nodes=1\n"
            + "  limit max_strings=1\n  limit max_depth=1\n  limit max_work=1\nend\n";
    assertTrue(
        assertThrows(IllegalArgumentException.class, () -> parse(roots))
            .getMessage()
            .contains("requires a root line"));
  }

  @Test
  void validatorRejections() {
    String[][] cases = {
      {"family F", "family f", "invalid or reserved family name: f"},
      {"family F", "family F\nfamily F", "duplicate family: F"},
      {"family F", "family F\nfamily AB\nfamily Ab", "family names collide in Rust"},
      {"format Fmt family=F", "format Fmt family=Q", "format Fmt: unknown family Q"},
      {"node Leaf family=F", "node Leaf family=Q", "node Leaf: unknown family Q"},
      {"element El family=F", "element El family=Q", "element El: unknown family Q"},
      {"kind K family=F", "kind K family=Q", "kind K: unknown family Q"},
      {"union U family=F", "union U family=Q", "union U: unknown family Q"},
      {"kind K family=F", "kind Leaf family=F", "duplicate name in family: Leaf"},
      {"kind K family=F", "kind KId family=F", "invalid or reserved kind name: KId"},
      {"union U family=F", "union Raw family=F", "invalid or reserved union name: Raw"},
      {"tag=1 sharing=interned", "tag=0 sharing=interned", "tag must be 1..65535"},
      {"tag=1 sharing=interned", "tag=2 sharing=interned", "nodes Leaf and Root share tag 2"},
      {"node Leaf family", "node Default family", "writer method collides: default"},
      {"node Leaf family", "node Int family", "invalid or reserved identifier: int"},
      {
        "element El family=F\n  string s\nend",
        "element El family=F\nend",
        "an untagged element needs a member"
      },
      {
        "  string s\nend\nelement Tg",
        "  refs r node=Leaf\nend\nelement Tg",
        "an element holds only field, string and ref"
      },
      {"kind K family=F nodes=Leaf", "kind K family=F nodes=Nope", "kind K: unknown node Nope"},
      {"kind K family=F nodes=Leaf", "kind K family=F nodes=Leaf,Leaf", "kind K lists Leaf twice"},
      {
        "union U family=F elements=Tg",
        "union U family=F elements=Nope",
        "union U: unknown element Nope"
      },
      {"union U family=F elements=Tg", "union U family=F elements=Tg,Tg", "union U lists Tg twice"},
      {"union U family=F elements=Tg", "union U family=F elements=El", "element El has no tag"},
      {"list els element=El", "list els element=Tg", "element Tg is tagged; use union="},
      {"version=1 magic=0x1", "version=0 magic=0x1", "version must be positive"},
      {"max_bytes=1024", "max_bytes=43", "max_bytes must be 44..2147483639"},
      {"max_nodes=16", "max_nodes=0", "max_nodes must be 1..2147483647"},
      {"max_strings=16", "max_strings=4294967295", "max_strings must be at most 2147483647"},
      {"max_depth=8", "max_depth=32768", "max_depth must be 1..32767"},
      {"max_work=64", "max_work=0", "max_work must be 1..2147483646"},
      {"header cap", "header Cap", "invalid or reserved header word name: Cap"},
      {"header cap", "header root", "invalid or reserved header word name: root"},
      {"header cap", "header int", "invalid or reserved identifier: int"},
      {"  root Root\n", "  root Nope\n", "root Nope is not a node or kind of family F"},
      {"  root Root\n", "  root Root,Root\n", "root Root is listed twice"},
      {"string name", "string Name", "invalid or reserved member name: Name"},
      {"string name", "string index", "invalid or reserved member name: index"},
      {"string name", "string int", "invalid or reserved identifier: int"},
      {"string name", "string name\n  field name_id type=u8", "collides with generated name_id"},
      {"field f type=u32 flags=G", "field f type=char", "unsupported field type char"},
      {
        "field f type=u32 flags=G",
        "field f type=bool enum=E",
        "enum= and flags= need an integer field"
      },
      {"field f type=u32 flags=G", "field f type=u32 enum=Nope", "unknown enum Nope"},
      {"field f type=u32 flags=G", "field f type=u32 enum=E", "enum E is carried as u8"},
      {"field f type=u32 flags=G", "field f type=u32 flags=Nope", "unknown flags Nope"},
      {"field f type=u32 flags=G", "field f type=u8 flags=G", "flags G are carried as u32"},
      {"ref leaf node=Leaf", "ref leaf node=Nope", "node= names no node or kind of family F: Nope"},
      {"list xs type=u16", "list xs type=f32", "list type must be an integer scalar"},
      {"list els element=El", "list els element=Nope", "unknown element Nope"},
      {"list us union=U", "list us union=Nope", "unknown union Nope"},
      {"refs leaves node=K max=4", "refs leaves node=K max=0", "max must be 1..2147483647"},
      {
        "format Fmt family=F version=1 magic=0x1",
        "format Fmt family=F version=1 magic=0x1 draft=true\n"
            + "  limit max_bytes=1024\n"
            + "  limit max_nodes=16\n"
            + "  limit max_strings=16\n"
            + "  limit max_depth=8\n"
            + "  limit max_work=64\n"
            + "  root Root\n"
            + "end\n"
            + "format Two family=F version=1 magic=0x1",
        "formats Fmt and Two share a magic"
      },
    };
    for (String[] c : cases) {
      String message = reject(base(c[0], c[1]));
      assertTrue(message.contains(c[2]), c[1] + " -> " + message);
    }
    // Generated Java classes are one package: two families' formats cannot share a name.
    String twoFamilies =
        BASE
            + """
            family H
            node Hn family=H tag=1 sharing=interned
            end
            format Fmt family=H version=1 magic=0x9 draft=true
              limit max_bytes=1024
              limit max_nodes=16
              limit max_strings=16
              limit max_depth=8
              limit max_work=64
              root Hn
            end
            """;
    assertTrue(reject(twoFamilies).contains("generated Java class FmtReader is claimed twice"));
  }
}
