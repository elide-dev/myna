// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package fixture.formats;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Drives the generated Java persisted-format code for {@code tests/formats/doc.seam}; prints the
 * same output as {@code tests/formats/check.rs} for the same commands.
 */
public final class FormatsMain {
  private FormatsMain() {}

  private static final int NO = DocNodes.NO_REF;

  private static String hex(byte[] b, int from, int length) {
    var out = new StringBuilder("x");
    for (int i = from; i < from + length; i++) out.append(String.format("%02x", b[i] & 0xff));
    return out.toString();
  }

  private static String hex(byte[] b) {
    return hex(b, 0, b.length);
  }

  private static String str(byte[] b, int id) {
    return hex(b, DocNodes.stringOffset(b, 0, id), DocNodes.stringLength(b, 0, id));
  }

  private static String opt(int index) {
    return index == NO ? "-" : Integer.toString(index);
  }

  private static String dump(byte[] b, int i) {
    return switch (DocNodes.tag(b, 0, i)) {
      case DocNodes.TAG_STYLE ->
          "Style face=" + str(b, DocNodes.Style_face_id(b, 0, i))
              + " flags=" + Integer.toUnsignedString(DocNodes.Style_flags(b, 0, i))
              + " align=" + (DocNodes.Style_align(b, 0, i) & 0xff)
              + " size=" + Long.toHexString(DocNodes.Style_size_bits(b, 0, i))
              + " parent=" + opt(DocNodes.Style_parent(b, 0, i));
      case DocNodes.TAG_PARAGRAPH -> {
        var spans = new ArrayList<String>();
        int at = DocNodes.Paragraph_spans_first(b, 0, i);
        for (int k = 0; k < DocNodes.Paragraph_spans_count(b, 0, i); k++) {
          spans.add(switch (DocNodes.Span_tag(b, at)) {
            case 0 -> "T:" + str(b, DocNodes.SpanText_text_id(b, 0, at));
            case 1 -> "B";
            default -> "S:" + DocNodes.SpanStyled_style(b, 0, at) + ":"
                + str(b, DocNodes.SpanStyled_text_id(b, 0, at)) + ":"
                + Integer.toHexString(Float.floatToRawIntBits(DocNodes.SpanStyled_scale(b, 0, at)));
          });
          at = DocNodes.Span_next(b, at);
        }
        var attrs = new ArrayList<String>();
        at = DocNodes.Paragraph_attrs_first(b, 0, i);
        for (int k = 0; k < DocNodes.Paragraph_attrs_count(b, 0, i); k++) {
          attrs.add(str(b, DocNodes.Attr_key_id(b, 0, at)) + ":" + opt(DocNodes.Attr_value(b, 0, at))
              + ":" + DocNodes.Attr_important(b, 0, at));
          if (!DocNodes.Attr_key(b, 0, at).equals(new String(b, DocNodes.stringOffset(b, 0, DocNodes.Attr_key_id(b, 0, at)),
              DocNodes.stringLength(b, 0, DocNodes.Attr_key_id(b, 0, at)), java.nio.charset.StandardCharsets.UTF_8)))
            throw new AssertionError("string accessor");
          at = DocNodes.Attr_next(at);
        }
        yield "Paragraph style=" + DocNodes.Paragraph_style(b, 0, i) + " spans=[" + String.join(",", spans)
            + "] attrs=[" + String.join(",", attrs) + "]";
      }
      case DocNodes.TAG_RAW_TEXT -> {
        var units = new ArrayList<String>();
        for (int k = 0; k < DocNodes.RawText_units_count(b, 0, i); k++)
          units.add(Integer.toHexString(DocNodes.RawText_units_at(b, 0, i, k) & 0xffff));
        yield "RawText units=[" + String.join(",", units) + "]";
      }
      case DocNodes.TAG_SECTION -> {
        var blocks = new ArrayList<String>();
        for (int k = 0; k < DocNodes.Section_blocks_count(b, 0, i); k++)
          blocks.add(Integer.toString(DocNodes.Section_blocks_at(b, 0, i, k)));
        yield "Section title=" + str(b, DocNodes.Section_title_id(b, 0, i)) + " id="
            + DocNodes.Section_title_id(b, 0, i) + " blocks=[" + String.join(",", blocks) + "]";
      }
      case DocNodes.TAG_DOCUMENT -> {
        var sections = new ArrayList<String>();
        for (int k = 0; k < DocNodes.Document_sections_count(b, 0, i); k++)
          sections.add(Integer.toString(DocNodes.Document_sections_at(b, 0, i, k)));
        yield "Document sections=[" + String.join(",", sections) + "] source="
            + hex(DocNodes.Document_source(b, 0, i)) + " meta=" + opt(DocNodes.Document_meta(b, 0, i));
      }
      case DocNodes.TAG_META -> {
        var samples = new ArrayList<String>();
        for (int k = 0; k < DocNodes.Meta_samples_count(b, 0, i); k++)
          samples.add(Long.toString(DocNodes.Meta_samples_at(b, 0, i, k)));
        yield "Meta a=" + DocNodes.Meta_a(b, 0, i)
            + " b=" + (DocNodes.Meta_b(b, 0, i) & 0xffff)
            + " c=" + DocNodes.Meta_c(b, 0, i)
            + " d=" + DocNodes.Meta_d(b, 0, i)
            + " e=" + DocNodes.Meta_e(b, 0, i)
            + " f=" + Long.toHexString(Double.doubleToRawLongBits(DocNodes.Meta_f(b, 0, i)))
            + " g=" + DocNodes.Meta_g(b, 0, i)
            + " samples=[" + String.join(",", samples) + "]"
            + " blob=" + hex(b, DocNodes.Meta_blob_offset(b, 0, i), DocNodes.Meta_blob_count(b, 0, i))
            + " marker=" + opt(DocNodes.Meta_marker(b, 0, i));
      }
      case DocNodes.TAG_MARKER -> "Marker";
      default -> throw new AssertionError("tag");
    };
  }

  // ------------------------------------------------------------------- golden values

  private static byte[] full(boolean orderB) {
    var w = new DocWriter();
    if (orderB) {
      w.string("unused");
      for (String s : List.of("title-2", "title-1", "ac", "ab", "k", "naïve", "café", "", "Body", "Serif"))
        w.string(s);
      int unused = w.string("dropped");
      w.style(unused, 0, (byte) 0, 9, NO);
    }
    int serif = w.string("Serif");
    int body = w.string("Body");
    int marker = w.marker();
    int sA, sB;
    if (orderB) {
      sB = w.style(serif, 0, (byte) 0, 2, NO);
      sA = w.style(serif, 0, (byte) 0, 1, NO);
    } else {
      sA = w.style(serif, 0, (byte) 0, 1, NO);
      sB = w.style(serif, 0, (byte) 0, 2, NO);
    }
    int sBase = w.style(serif, 1 | (5 << 4), (byte) 1, 0x4028_0000_0000_0000L, NO);
    int sChild = w.style(body, 2, (byte) 2, 3, sBase);
    int r1 = w.rawText(new short[] {0x48, (short) 0xD800, 0x69});
    int cafe = w.string("café");
    int naive = w.string("naïve");
    int k = w.string("k");
    int empty = w.string("");
    int p1 = w.paragraph(
        sChild,
        List.of(new DocWriter.Span.SpanText(cafe), new DocWriter.Span.SpanBreak(), new DocWriter.Span.SpanStyled(sA, naive, 1.5f)),
        List.of(new DocWriter.Attr(k, sB, true), new DocWriter.Attr(empty, NO, false)));
    int baseAgain = orderB ? w.style(serif, 1 | (5 << 4), (byte) 1, 0x4028_0000_0000_0000L, NO) : sBase;
    int ab = w.string("ab");
    int ac = w.string("ac");
    int p2 = w.paragraph(baseAgain, List.of(new DocWriter.Span.SpanText(ab)), List.of());
    int p3 = w.paragraph(sBase, List.of(new DocWriter.Span.SpanText(ac)), List.of());
    int t1 = w.string("title-1");
    int t2 = w.string("title-2");
    int sec1 = w.section(t1, new int[] {p1, r1});
    int sec2 = w.section(t2, new int[] {p2, p3});
    int meta = w.meta((byte) -5, (short) 65535, (short) -2, Integer.MIN_VALUE, Long.MAX_VALUE,
        Double.longBitsToDouble(0x7ff8_0000_0000_0001L), true, new long[] {-1, 0, 1}, new byte[] {(byte) 0xde, (byte) 0xad}, marker);
    int doc = w.document(new int[] {sec1, sec2}, "source\0ÿ".getBytes(java.nio.charset.StandardCharsets.ISO_8859_1), meta);
    return w.finishDocFile(doc, 7);
  }

  private static byte[] fragmentParagraph(boolean orderB) {
    var w = new DocWriter();
    int face = w.string("Serif");
    int style = w.style(face, 0, (byte) 0, 0, NO);
    if (orderB) style = w.style(face, 0, (byte) 0, 0, NO);
    int p = w.paragraph(style, List.of(new DocWriter.Span.SpanText(face), new DocWriter.Span.SpanBreak()), List.of());
    return w.finishDocFragment(p);
  }

  private static byte[] fragmentRawText(boolean orderB) {
    var w = new DocWriter();
    return w.finishDocFragment(w.rawText(new short[0]));
  }

  private static byte[] tiny(boolean orderB) {
    // At every DocTiny limit: 4 nodes, depth 3, work 6, 3 strings.
    var w = new DocWriter();
    int f, k, t;
    if (orderB) {
      t = w.string("t");
      k = w.string("k");
      f = w.string("f");
    } else {
      f = w.string("f");
      k = w.string("k");
      t = w.string("t");
    }
    int s1, s2;
    if (orderB) {
      s2 = w.style(f, 0, (byte) 0, 2, NO);
      s1 = w.style(f, 0, (byte) 0, 1, NO);
    } else {
      s1 = w.style(f, 0, (byte) 0, 1, NO);
      s2 = w.style(f, 0, (byte) 0, 2, NO);
    }
    int p = w.paragraph(s2, List.of(), List.of(new DocWriter.Attr(k, s1, false), new DocWriter.Attr(k, s1, false),
        new DocWriter.Attr(k, s1, false), new DocWriter.Attr(k, NO, false)));
    int section = w.section(t, new int[] {p});
    return w.finishDocTiny(section, 1);
  }

  // ------------------------------------------------------------------- round trip

  private static int[] ids(byte[] b, int at, int count, java.util.function.IntBinaryOperator item, int[] map) {
    int[] result = new int[count];
    for (int k = 0; k < count; k++) result[k] = map[item.applyAsInt(at, k)];
    return result;
  }

  /** Rebuilds every node through a fresh writer in index order; returns the writer and the id map. */
  private static int[] copy(byte[] b, DocWriter w) {
    int count = DocNodes.nodeCount(b, 0);
    int[] map = new int[count];
    for (int i = 0; i < count; i++) {
      final int n = i;
      map[i] = switch (DocNodes.tag(b, 0, i)) {
        case DocNodes.TAG_STYLE -> {
          int parent = DocNodes.Style_parent(b, 0, i);
          yield w.style(w.string(DocNodes.Style_face(b, 0, i)), DocNodes.Style_flags(b, 0, i), DocNodes.Style_align(b, 0, i),
              DocNodes.Style_size_bits(b, 0, i), parent == NO ? NO : map[parent]);
        }
        case DocNodes.TAG_PARAGRAPH -> {
          var spans = new ArrayList<DocWriter.Span>();
          int at = DocNodes.Paragraph_spans_first(b, 0, i);
          for (int k = 0; k < DocNodes.Paragraph_spans_count(b, 0, i); k++) {
            spans.add(switch (DocNodes.Span_tag(b, at)) {
              case 0 -> new DocWriter.Span.SpanText(w.string(DocNodes.SpanText_text(b, 0, at)));
              case 1 -> new DocWriter.Span.SpanBreak();
              default -> new DocWriter.Span.SpanStyled(map[DocNodes.SpanStyled_style(b, 0, at)],
                  w.string(DocNodes.SpanStyled_text(b, 0, at)), DocNodes.SpanStyled_scale(b, 0, at));
            });
            at = DocNodes.Span_next(b, at);
          }
          var attrs = new ArrayList<DocWriter.Attr>();
          at = DocNodes.Paragraph_attrs_first(b, 0, i);
          for (int k = 0; k < DocNodes.Paragraph_attrs_count(b, 0, i); k++) {
            int value = DocNodes.Attr_value(b, 0, at);
            attrs.add(new DocWriter.Attr(w.string(DocNodes.Attr_key(b, 0, at)), value == NO ? NO : map[value],
                DocNodes.Attr_important(b, 0, at)));
            at = DocNodes.Attr_next(at);
          }
          yield w.paragraph(map[DocNodes.Paragraph_style(b, 0, i)], spans, attrs);
        }
        case DocNodes.TAG_RAW_TEXT -> {
          short[] units = new short[DocNodes.RawText_units_count(b, 0, i)];
          for (int k = 0; k < units.length; k++) units[k] = DocNodes.RawText_units_at(b, 0, i, k);
          yield w.rawText(units);
        }
        case DocNodes.TAG_SECTION -> w.section(w.string(DocNodes.Section_title(b, 0, i)),
            ids(b, 0, DocNodes.Section_blocks_count(b, 0, i), (a, k) -> DocNodes.Section_blocks_at(b, 0, n, k), map));
        case DocNodes.TAG_DOCUMENT -> {
          int meta = DocNodes.Document_meta(b, 0, i);
          yield w.document(ids(b, 0, DocNodes.Document_sections_count(b, 0, i), (a, k) -> DocNodes.Document_sections_at(b, 0, n, k), map),
              DocNodes.Document_source(b, 0, i), meta == NO ? NO : map[meta]);
        }
        case DocNodes.TAG_META -> {
          long[] samples = new long[DocNodes.Meta_samples_count(b, 0, i)];
          for (int k = 0; k < samples.length; k++) samples[k] = DocNodes.Meta_samples_at(b, 0, i, k);
          int off = DocNodes.Meta_blob_offset(b, 0, i);
          byte[] blob = Arrays.copyOfRange(b, off, off + DocNodes.Meta_blob_count(b, 0, i));
          int marker = DocNodes.Meta_marker(b, 0, i);
          yield w.meta(DocNodes.Meta_a(b, 0, i), DocNodes.Meta_b(b, 0, i), DocNodes.Meta_c(b, 0, i), DocNodes.Meta_d(b, 0, i),
              DocNodes.Meta_e(b, 0, i), DocNodes.Meta_f(b, 0, i), DocNodes.Meta_g(b, 0, i), samples, blob, marker == NO ? NO : map[marker]);
        }
        default -> w.marker();
      };
    }
    return map;
  }

  private static void roundTrip(String name, byte[] b) {
    var w = new DocWriter();
    int[] map = copy(b, w);
    int root = map[map.length - 1];
    byte[] rebuilt;
    if (name.startsWith("docfile")) rebuilt = w.finishDocFile(root, DocFileReader.headerWord(b, 0));
    else if (name.startsWith("docfragment")) rebuilt = w.finishDocFragment(root);
    else rebuilt = w.finishDocTiny(root, DocTinyReader.headerWord(b, 0));
    if (!Arrays.equals(rebuilt, b)) throw new AssertionError(name + ": decoding and encoding again changed the bytes");
  }

  private interface Build {
    byte[] build(boolean orderB);
  }

  private static void golden(Path dir) throws Exception {
    var values = new java.util.LinkedHashMap<String, Build>();
    values.put("docfile-full", FormatsMain::full);
    values.put("docfragment-paragraph", FormatsMain::fragmentParagraph);
    values.put("docfragment-rawtext", FormatsMain::fragmentRawText);
    values.put("doctiny-section", FormatsMain::tiny);
    for (var e : values.entrySet()) {
      byte[] a = e.getValue().build(false);
      byte[] b = e.getValue().build(true);
      if (!Arrays.equals(a, b)) throw new AssertionError(e.getKey() + ": creation order changed the bytes");
      roundTrip(e.getKey(), a);
      Files.write(dir.resolve(e.getKey() + ".bin"), a);
    }
  }

  // ------------------------------------------------------------------- verdicts

  private static final String[] CLASSES = {"Unsupported", "Limit", "Malformed"};

  private static String show(int[] error) {
    if (error == null) return "OK";
    return "ERR " + DocNodes.errorName(error[0]) + " " + error[1] + " " + error[2] + " " + CLASSES[DocNodes.errorClass(error[0])];
  }

  private static void verdict(Path path, StringBuilder out) throws Exception {
    byte[] b = Files.readAllBytes(path);
    String name = path.getFileName().toString();
    int[] full, structural;
    if (name.startsWith("docfile")) {
      full = DocFileReader.check(b, 0, b.length, new int[] {7});
      structural = DocFileReader.checkStructural(b, 0, b.length, new int[] {7});
    } else if (name.startsWith("docfragment")) {
      full = DocFragmentReader.check(b, 0, b.length);
      structural = DocFragmentReader.checkStructural(b, 0, b.length);
    } else {
      full = DocTinyReader.check(b, 0, b.length, new int[] {1});
      structural = DocTinyReader.checkStructural(b, 0, b.length, new int[] {1});
    }
    out.append(name).append(" full=").append(show(full)).append(" structural=").append(show(structural)).append('\n');
    if (full == null)
      for (int i = 0; i < DocNodes.nodeCount(b, 0); i++) out.append("  #").append(i).append(' ').append(dump(b, i)).append('\n');
  }

  // ------------------------------------------------------------------- writer errors

  private interface Attempt {
    Object run();
  }

  private static String kind(Attempt a) {
    try {
      a.run();
      return "OK";
    } catch (DocException e) {
      return DocNodes.errorName(e.kind);
    }
  }

  private static void writerErrors(StringBuilder out) {
    var w = new DocWriter();
    int f = w.string("f");
    out.append("bad-enum ").append(kind(() -> w.style(f, 0, (byte) 3, 0, NO))).append('\n');
    out.append("bad-flags ").append(kind(() -> w.style(f, 0x100, (byte) 0, 0, NO))).append('\n');
    int s = w.style(f, 0, (byte) 0, 0, NO);
    var attrs = new ArrayList<DocWriter.Attr>();
    for (int i = 0; i < 256; i++) attrs.add(new DocWriter.Attr(f, NO, false));
    out.append("field-too-long ").append(kind(() -> w.paragraph(s, List.of(), attrs))).append('\n');

    var w2 = new DocWriter();
    int f2 = w2.string("f");
    int s1 = w2.style(f2, 0, (byte) 0, 1, NO);
    int s2 = w2.style(f2, 0, (byte) 0, 2, s1);
    int p2 = w2.paragraph(s2, List.of(), List.of());
    int section2 = w2.section(w2.string("t"), new int[] {p2});
    out.append("too-deep ").append(kind(() -> w2.finishDocTiny(section2, 1))).append('\n');

    var w3 = new DocWriter();
    int f3 = w3.string("f");
    int s3 = w3.style(f3, 0, (byte) 0, 0, NO);
    var attrs3 = new ArrayList<DocWriter.Attr>();
    for (int i = 0; i < 5; i++) attrs3.add(new DocWriter.Attr(f3, s3, false));
    int p3 = w3.paragraph(s3, List.of(), attrs3);
    out.append("too-much-work ").append(kind(() -> w3.finishDocTiny(p3, 1))).append('\n');

    var w4 = new DocWriter();
    var spans = new ArrayList<DocWriter.Span>();
    int first = -1;
    for (String x : List.of("a", "b", "c", "d")) {
      int id = w4.string(x);
      if (first < 0) first = id;
      spans.add(new DocWriter.Span.SpanText(id));
    }
    int p4 = w4.paragraph(w4.style(first, 0, (byte) 0, 0, NO), spans, List.of());
    out.append("too-many-strings ").append(kind(() -> w4.finishDocTiny(p4, 1))).append('\n');

    var w5 = new DocWriter();
    w5.string("f");
    int t5 = w5.string("t");
    int[] blocks = new int[4];
    for (int i = 0; i < 4; i++) blocks[i] = w5.rawText(new short[0]);
    int section5 = w5.section(t5, blocks);
    out.append("too-many-nodes ").append(kind(() -> w5.finishDocTiny(section5, 1))).append('\n');

    var w6 = new DocWriter();
    int r6 = w6.rawText(new short[300]);
    out.append("too-large ").append(kind(() -> w6.finishDocTiny(r6, 1))).append('\n');

    var w7 = new DocWriter();
    int section7 = w7.section(w7.string("t"), new int[0]);
    out.append("bad-root ").append(kind(() -> w7.finishDocFragment(section7))).append('\n');

    // Only Java can attempt these: Rust's types rule them out.
    var w8 = new DocWriter();
    int t8 = w8.string("t");
    int r8 = w8.rawText(new short[0]);
    w8.section(t8, new int[] {r8});
    out.append("java shared-unique ").append(kind(() -> w8.section(t8, new int[] {r8}))).append('\n');
    out.append("java shared-unique-one-parent ").append(kind(() -> w8.section(t8, new int[] {w8.rawText(new short[] {1}), 0}))).append('\n');
    int r9 = w8.rawText(new short[] {2});
    out.append("java shared-unique-twice ").append(kind(() -> w8.section(t8, new int[] {r9, r9}))).append('\n');
    out.append("java bad-ref-kind ").append(kind(() -> w8.paragraph(r8, List.of(), List.of()))).append('\n');
    out.append("java bad-ref ").append(kind(() -> w8.paragraph(1000, List.of(), List.of()))).append('\n');
    out.append("java bad-string ").append(kind(() -> w8.section(1000, new int[0]))).append('\n');
    out.append("java bad-utf8 ").append(kind(() -> w8.string("\ud800"))).append('\n');
  }

  public static void main(String[] args) throws Exception {
    var out = new StringBuilder();
    switch (args[0]) {
      case "golden" -> golden(Path.of(args[1]));
      case "verdict" -> {
        for (int i = 1; i < args.length; i++) verdict(Path.of(args[i]), out);
      }
      case "writer-errors" -> writerErrors(out);
      default -> throw new IllegalArgumentException(args[0]);
    }
    System.out.print(out);
  }
}
