// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package dev.elide.myna;

import static org.junit.jupiter.api.Assertions.*;

import dev.elide.myna.frontend.Dsl;
import dev.elide.myna.model.Seam.*;
import dev.elide.myna.serialize.SeamJson;
import dev.elide.myna.validate.Validator;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.util.*;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;

/** Counted element views ({@code count=} on a pointer field), issue #133. */
class ViewsTest {
  private static final String HEAD = "module v target=x86_64-unknown-linux-gnu\n";
  private static final String ELEMENT =
      "struct E size=8 align=8\n  field x type=u64 offset=0\nend\n";

  private static String example() throws Exception {
    return Files.readString(Path.of("examples/views.seam"));
  }

  private static String reject(String body) {
    return assertThrows(
            IllegalArgumentException.class, () -> Validator.validate(Dsl.parse(HEAD + body), true))
        .getMessage();
  }

  private static String record(String fields) {
    return ELEMENT + "struct R size=24 align=8\n" + fields + "end\n";
  }

  @Test
  void parsesCountAndRecordsItInSeamJson() throws Exception {
    var m = Dsl.parse(example());
    var program = m.structs().stream().filter(s -> s.name().equals("Program")).findFirst();
    var fields = program.orElseThrow().fields();
    assertEquals("functionCount", fields.get(0).count());
    assertNull(fields.get(1).count());
    assertEquals("textLength", fields.get(2).count());
    assertEquals("rootCount", fields.get(5).count());
    assertEquals(new Pointer(new Pointer(new Named("Function"))), fields.get(5).type());
    assertNull(new Field("a", Scalar.U8, 0).count());
    String json = SeamJson.canonical(m);
    assertTrue(json.contains("\"offset\":0,\"count\":\"functionCount\"}"), json);
    assertTrue(json.contains("{\"name\":\"functionCount\",\"type\":\"u64\",\"offset\":8}"), json);
    // The relation is part of the fingerprint.
    var plain = Dsl.parse(example().replace(" count=functionCount", ""));
    assertNotEquals(SeamJson.fingerprint(m), SeamJson.fingerprint(plain));
  }

  @Test
  void goldenOutputs() throws Exception {
    var actual = Generator.generate(Dsl.parse(example()), true);
    try (var files = Files.list(Path.of("tests/golden/views"))) {
      assertTrue(files.findAny().isPresent());
    }
    for (var output : actual.entrySet())
      assertEquals(
          Files.readString(Path.of("tests/golden/views", output.getKey())),
          output.getValue(),
          output.getKey());
  }

  @Test
  void undeclaredViewsGenerateNothingNew() throws Exception {
    var outputs =
        Generator.generate(
            Dsl.parse(
                example()
                    .replace(" count=functionCount", "")
                    .replace(" count=textLength", "")
                    .replace(" count=rootCount", "")),
            true);
    assertFalse(outputs.get("seam.rs").contains("impl Program"));
    assertFalse(outputs.get("ViewsFFM.java").contains("Program_functions("));
    assertFalse(outputs.get("Views.java").contains("addressOf(long"));
    assertFalse(outputs.get("Views.java").contains("WordFactory"));
    assertFalse(outputs.get("seam.json").contains("\"count\""));
  }

  @Test
  void validatesTheCountedPointer() {
    String ok =
        "  field p type=ptr<E> offset=0 count=n\n  field n type=u32 offset=8\n"
            + "  field q type=ptr<u16> offset=16 count=m\n";
    assertThrows(
        IllegalArgumentException.class,
        () -> Validator.validate(Dsl.parse(HEAD + record(ok)), true),
        "u16 count m is missing");
    Validator.validate(
        Dsl.parse(
            HEAD
                + ELEMENT
                + "struct R size=32 align=8\n"
                + ok
                + "  field m type=u16 offset=24\nend\n"),
        true);
    assertEquals(
        "field R.n: count= requires a ptr<T> field, found u64",
        reject(record("  field n type=u64 offset=0 count=n\n")));
    assertEquals(
        "field R.p: count=n names no field of R",
        reject(record("  field p type=ptr<E> offset=0 count=n\n")));
    assertEquals(
        "field R.p: count field R.q must be u8, u16, u32, u64 or usize, found ptr<E>",
        reject(record("  field p type=ptr<E> offset=0 count=q\n  field q type=ptr<E> offset=8\n")));
    assertEquals(
        "field R.p: count field R.p must be u8, u16, u32, u64 or usize, found ptr<E>",
        reject(record("  field p type=ptr<E> offset=0 count=p\n")));
    assertEquals(
        "field R.p: count field R.n must be u8, u16, u32, u64 or usize, found f64",
        reject(record("  field p type=ptr<E> offset=0 count=n\n  field n type=f64 offset=8\n")));
    // A count is never negative: signed carriers, isize included, are rejected.
    for (String carrier : List.of("i8", "i16", "i32", "i64", "isize"))
      assertEquals(
          "field R.p: count field R.n must be u8, u16, u32, u64 or usize, found " + carrier,
          reject(
              record(
                  "  field p type=ptr<E> offset=0 count=n\n  field n type="
                      + carrier
                      + " offset=8\n")));
    assertEquals(
        "field R.p: count field R.n carries an enum= or flags= reference",
        reject(
            "enum K type=u64\n  value a = 0\nend\n"
                + record(
                    "  field p type=ptr<E> offset=0 count=n\n"
                        + "  field n type=u64 offset=8 enum=K\n")));
    assertEquals(
        "field R.p and field R.q: both name count=n",
        reject(
            record(
                "  field p type=ptr<E> offset=0 count=n\n  field n type=u64 offset=8\n"
                    + "  field q type=ptr<u8> offset=16 count=n\n")));
    assertEquals(
        "field R.p: count= needs elements of a known size, but O is opaque",
        reject(
            "opaque O\n"
                + record("  field p type=ptr<O> offset=0 count=n\n  field n type=u64 offset=8\n")));
    for (String element : List.of("void", "bool", "ptr<u8>", "ptr<ptr<E>>"))
      assertEquals(
          "field R.p: count= needs struct, integer/float or ptr<struct or opaque> elements, found"
              + " ptr<"
              + element
              + ">",
          reject(
              record(
                  "  field p type=ptr<"
                      + element
                      + "> offset=0 count=n\n  field n type=u64 offset=8\n")));
    for (String suffix : List.of("", "At", "_count", "_at"))
      assertEquals(
          "field R.p: accessor R_p" + suffix + " collides with function R_p" + suffix,
          reject(
              record("  field p type=ptr<E> offset=0 count=n\n  field n type=u64 offset=8\n")
                  + "import R_p"
                  + suffix
                  + " symbol=r_p return=void error=no_failure\nend\n"));
    // Two views' generated names may not meet: p's count accessor is p_count's view.
    assertEquals(
        "field R.p_count: accessor R_p_count collides with an accessor of field R.p",
        reject(
            ELEMENT
                + "struct R size=32 align=8\n  field p type=ptr<E> offset=0 count=n\n"
                + "  field n type=u64 offset=8\n  field p_count type=ptr<E> offset=16 count=m\n"
                + "  field m type=u64 offset=24\nend\n"));
    assertEquals(
        "field R.p: accessor R_pAt collides with function R_pAt",
        reject(
            record("  field p type=ptr<E> offset=0 count=n\n  field n type=u64 offset=8\n")
                + "import R_pAt symbol=r_p_at return=void error=no_failure\nend\n"));
    assertEquals(
        "field E.addressOf collides with the element accessor addressOf(long) that field R.p needs",
        reject(
            "struct E size=8 align=8\n  field addressOf type=u64 offset=0\nend\n"
                + "struct R size=16 align=8\n  field p type=ptr<E> offset=0 count=n\n"
                + "  field n type=u64 offset=8\nend\n"));
  }

  /** Every count carrier widens to the index type: signed ones as is, unsigned ones zero-extend. */
  @Test
  void countCarriersAndScalarElements() throws Exception {
    var out =
        Generator.generate(
            Dsl.parse(
                HEAD.replace("\n", " java_package=p java_class=C\n")
                    + "opaque O\n"
                    + "struct R size=64 align=8\n"
                    + "  field a type=ptr<usize> offset=0 count=an\n"
                    + "  field b type=ptr<char> offset=8 count=bn\n"
                    + "  field c type=ptr<f64> offset=16 count=cn\n"
                    + "  field d type=ptr<i32> offset=24 count=dn\n"
                    + "  field an type=u8 offset=32\n"
                    + "  field bn type=u16 offset=34\n"
                    + "  field cn type=u32 offset=36\n"
                    + "  field dn type=usize offset=40\n"
                    + "  field o type=ptr<ptr<O>> offset=48 count=on\n"
                    + "  field on type=u64 offset=56\n"
                    + "end\n"),
            true);
    String rust = out.get("seam.rs");
    assertTrue(rust.contains("pub unsafe fn a(&self) -> &[usize] {"), rust);
    assertTrue(rust.contains("pub unsafe fn b(&self) -> &[core::ffi::c_char] {"), rust);
    assertFalse(rust.contains(">= 0"), rust);
    assertTrue(rust.contains("let count = self.dn;\n"), rust);
    assertTrue(rust.contains("pub unsafe fn o(&self) -> &[*mut O] {"), rust);
    String ffm = out.get("CFFM.java");
    assertTrue(
        ffm.contains(
            "return view.getAtIndex(ValueLayout.ADDRESS, java.util.Objects.checkIndex(i,"
                + " view.byteSize() / 8L)).asReadOnly();"),
        ffm);
    assertTrue(
        ffm.contains("return R_o_at(R_o(record, owner), i, owner);")
            && ffm.contains("element.reinterpret(0L, owner, null)).asReadOnly();"),
        ffm);
    assertTrue(
        ffm.contains("return Byte.toUnsignedLong(record.get(ValueLayout.JAVA_BYTE, 32L));"), ffm);
    assertTrue(
        ffm.contains("return Short.toUnsignedLong(record.get(ValueLayout.JAVA_SHORT, 34L));"), ffm);
    assertTrue(ffm.contains("public static double R_c(MemorySegment record, long i) {"), ffm);
    assertTrue(ffm.contains("public static double R_c_at(MemorySegment view, long i) {"), ffm);
    // Only segment elements get an owner-scoped element form.
    assertFalse(ffm.contains("R_c(MemorySegment record, long i, Arena owner)"), ffm);
    assertTrue(ffm.contains("R_o(MemorySegment record, long i, Arena owner)"), ffm);
    // Narrow counts zero-extend, compiled and run: a u8 count of 0xff and a u16 count of 0xffff.
    String consumer =
        """
        package p;
        import java.lang.foreign.*;
        public final class Consumer {
          public static String run() {
            try (Arena arena = Arena.ofConfined()) {
              MemorySegment words = arena.allocate(ValueLayout.JAVA_LONG, 255);
              MemorySegment chars = arena.allocate(ValueLayout.JAVA_BYTE, 65535);
              chars.set(ValueLayout.JAVA_BYTE, 65534, (byte) 'z');
              MemorySegment r = arena.allocate(CFFM.R_LAYOUT);
              r.set(ValueLayout.ADDRESS, 0, words);
              r.set(ValueLayout.ADDRESS, 8, chars);
              r.set(ValueLayout.JAVA_BYTE, 32, (byte) 0xff);
              r.set(ValueLayout.JAVA_SHORT, 34, (short) 0xffff);
              return CFFM.R_a_count(r) + " " + CFFM.R_a(r).byteSize() + " " + CFFM.R_b_count(r)
                  + " " + (char) CFFM.R_b(r, 65534) + " " + CFFM.R_b_at(CFFM.R_b(r), 65534);
            }
          }
        }
        """;
    assertEquals("255 2040 65535 z 122", runOnHotSpot("p", "CFFM", out.get("CFFM.java"), consumer));
    String ni = out.get("C.java");
    assertTrue(ni.contains("return ((WordPointer) record.o()).read(WordFactory.signed(i));"), ni);
    assertTrue(ni.contains("public static O R_oAt(R record, long i) {"), ni);
    assertTrue(ni.contains("i < Byte.toUnsignedLong(record.an())"), ni);
    assertTrue(ni.contains("i < Short.toUnsignedLong(record.bn())"), ni);
    assertTrue(ni.contains("i < Integer.toUnsignedLong(record.cn())"), ni);
    assertTrue(ni.contains("Long.compareUnsigned(i, record.dn()) < 0"), ni);
    assertTrue(ni.contains("return record.a().read(WordFactory.signed(i)).rawValue();"), ni);
    assertTrue(ni.contains("public static byte R_bAt(R record, long i) {"), ni);
  }

  /**
   * The FFM accessors on HotSpot, over records in Java memory: the views have the counted size,
   * elements read through them, index {@code count} is an IndexOutOfBoundsException, and a null
   * pointer with a zero count is an empty view.
   */
  @Test
  void ffmViewsRunOnHotSpot() throws Exception {
    var module = Dsl.parse(example());
    String ffm = Generator.generate(module, true).get("ViewsFFM.java");
    String consumer =
        """
        package fixture.views;
        import java.lang.foreign.*;
        public final class Consumer {
          public static String run() {
            StringBuilder out = new StringBuilder();
            try (Arena arena = Arena.ofConfined()) {
              MemorySegment functions = arena.allocate(ViewsFFM.Function_LAYOUT, 2);
              for (int i = 0; i < 2; i++) {
                functions.set(ValueLayout.JAVA_INT, i * 16L, 10 + i);
                functions.set(ValueLayout.JAVA_LONG, i * 16L + 8, 0xfffffffffL + i);
              }
              MemorySegment text = arena.allocateFrom(ValueLayout.JAVA_SHORT, (short) 'h', (short) 'i', (short) 0xffff);
              MemorySegment program = arena.allocate(ViewsFFM.Program_LAYOUT);
              program.set(ValueLayout.ADDRESS, 0, functions);
              program.set(ValueLayout.JAVA_LONG, 8, 2L);
              program.set(ValueLayout.ADDRESS, 16, text);
              program.set(ValueLayout.JAVA_INT, 24, 3);
              MemorySegment roots = arena.allocate(ValueLayout.ADDRESS, 2);
              roots.setAtIndex(ValueLayout.ADDRESS, 0, functions.asSlice(16));
              roots.setAtIndex(ValueLayout.ADDRESS, 1, functions);
              program.set(ValueLayout.ADDRESS, 32, roots);
              program.set(ValueLayout.JAVA_LONG, 40, 2L);
              out.append(ViewsFFM.Program_functions(program).byteSize()).append(' ')
                  .append(ViewsFFM.Program_text(program).byteSize()).append(' ')
                  .append(ViewsFFM.Program_functions(program, 1).get(ValueLayout.JAVA_INT, 0)).append(' ')
                  .append(ViewsFFM.Program_functions(program, 1).byteSize()).append(' ')
                  .append((char) ViewsFFM.Program_text(program, 1)).append(' ')
                  .append(ViewsFFM.Program_text(program, 2)).append(' ')
                  .append(ViewsFFM.Program_roots(program).byteSize()).append(' ')
                  .append(ViewsFFM.Program_roots(program, 0).address() - functions.address()).append(' ')
                  .append(ViewsFFM.Program_roots(program, 1).address() == functions.address())
                  .append(ViewsFFM.Program_roots(program, 0).byteSize()).append(' ');
              out.append(outOfBounds(() -> ViewsFFM.Program_functions(program, 2)))
                  .append(outOfBounds(() -> ViewsFFM.Program_text(program, 3)))
                  .append(outOfBounds(() -> ViewsFFM.Program_text(program, -1)))
                  .append(outOfBounds(() -> ViewsFFM.Program_roots(program, 2))).append(' ');
              MemorySegment empty = arena.allocate(ViewsFFM.Program_LAYOUT);
              out.append(ViewsFFM.Program_functions(empty).byteSize())
                  .append(ViewsFFM.Program_text(empty).byteSize())
                  .append(outOfBounds(() -> ViewsFFM.Program_functions(empty, 0)))
                  .append(outOfBounds(() -> ViewsFFM.Program_text(empty, 0)))
                  .append(ViewsFFM.Program_roots(empty).byteSize())
                  .append(outOfBounds(() -> ViewsFFM.Program_roots(empty, 0)));
              // The count through the binding, and the loop helper over a view taken once.
              MemorySegment view = ViewsFFM.Program_functions(program);
              long ids = 0;
              for (long i = 0; i < ViewsFFM.Program_functions_count(program); i++)
                ids += ViewsFFM.Program_functions_at(view, i).get(ValueLayout.JAVA_INT, 0);
              MemorySegment textView = ViewsFFM.Program_text(program);
              MemorySegment rootView = ViewsFFM.Program_roots(program);
              out.append(' ').append(ids).append(ViewsFFM.Program_text_count(program))
                  .append((char) ViewsFFM.Program_text_at(textView, 1))
                  .append(ViewsFFM.Program_roots_count(program))
                  .append(ViewsFFM.Program_roots_at(rootView, 1).address() == functions.address())
                  .append(outOfBounds(() -> ViewsFFM.Program_functions_at(view, 2)))
                  .append(outOfBounds(() -> ViewsFFM.Program_functions_at(view, Long.MAX_VALUE / 16 + 1)))
                  .append(outOfBounds(() -> ViewsFFM.Program_text_at(textView, -1)))
                  .append(outOfBounds(() -> ViewsFFM.Program_functions_at(view, -1)));
              // Views of producer memory are read-only, whole or element.
              out.append(' ').append(view.isReadOnly()).append(textView.isReadOnly())
                  .append(rootView.isReadOnly())
                  .append(ViewsFFM.Program_functions(program, 1).isReadOnly())
                  .append(readOnly(() -> view.set(ValueLayout.JAVA_INT, 0, 1)))
                  .append(readOnly(() -> textView.set(ValueLayout.JAVA_SHORT, 0, (short) 1)))
                  .append(readOnly(() -> ViewsFFM.Program_functions(program, 0).set(ValueLayout.JAVA_INT, 0, 1)))
                  // Pointer-array elements too, in every form.
                  .append(ViewsFFM.Program_roots_at(rootView, 0).isReadOnly())
                  .append(ViewsFFM.Program_roots(program, 0).isReadOnly())
                  .append(ViewsFFM.Program_roots(program, 0, arena).isReadOnly())
                  .append(ViewsFFM.Program_roots_at(rootView, 0, arena).isReadOnly());
              // A null element stays null through the owner form.
              MemorySegment nullRoots = arena.allocate(ValueLayout.ADDRESS, 1).asReadOnly();
              out.append(ViewsFFM.Program_roots_at(nullRoots, 0, arena).address());
              // Owner-scoped views: alive while the owner is, then IllegalStateException.
              MemorySegment owned;
              MemorySegment ownedElement;
              MemorySegment ownedRoot;
              short ownedText;
              try (Arena owner = Arena.ofConfined()) {
                owned = ViewsFFM.Program_functions(program, owner);
                ownedElement = ViewsFFM.Program_functions(program, 1, owner);
                ownedRoot = ViewsFFM.Program_roots(program, 0, owner);
                MemorySegment ownedRootView = ViewsFFM.Program_roots(program, owner);
                ownedText = ViewsFFM.Program_text_at(ViewsFFM.Program_text(program, owner), 2);
                out.append(' ').append(owned.byteSize()).append(ownedElement.get(ValueLayout.JAVA_INT, 0))
                    .append(ownedRoot.address() - functions.address()).append(ownedText)
                    .append(ownedRoot.byteSize()).append(ownedRoot.get(ValueLayout.JAVA_INT, 0))
                    .append(ViewsFFM.Program_roots_at(ownedRootView, 1, owner).get(ValueLayout.JAVA_INT, 0))
                    .append(owned.isReadOnly()).append(ownedElement.isReadOnly()).append(ownedRoot.isReadOnly())
                    .append(readOnly(() -> owned.set(ValueLayout.JAVA_INT, 0, 1)));
              }
              out.append(closed(() -> owned.get(ValueLayout.JAVA_INT, 0)))
                  .append(closed(() -> ownedElement.get(ValueLayout.JAVA_INT, 0)))
                  .append(closed(() -> ownedRoot.get(ValueLayout.JAVA_INT, 0)));
              // Every bad count is one IndexOutOfBoundsException naming the view and its count.
              MemorySegment bad = arena.allocate(ViewsFFM.Program_LAYOUT);
              bad.set(ValueLayout.JAVA_LONG, 8, 2L);
              out.append('|').append(failure(() -> ViewsFFM.Program_functions(bad)))
                  .append('|').append(failure(() -> ViewsFFM.Program_functions(bad, 0)))
                  .append('|').append(failure(() -> ViewsFFM.Program_functions(bad, arena)))
                  .append('|').append(failure(() -> ViewsFFM.Program_functions(bad, 0, arena)));
              // A misaligned pointer is the same exception, naming the view.
              bad.set(ValueLayout.ADDRESS, 0, functions.asSlice(1));
              out.append('|').append(failure(() -> ViewsFFM.Program_functions(bad)).replaceAll("0x[0-9a-f]+", "0x?"));
              bad.set(ValueLayout.ADDRESS, 0, functions);
              // The exact boundary is a byte size the helper accepts (reinterpret then decides).
              bad.set(ValueLayout.JAVA_LONG, 8, Long.MAX_VALUE / 16);
              out.append('|').append(failure(() -> ViewsFFM.Program_functions(bad).byteSize()));
              bad.set(ValueLayout.JAVA_LONG, 8, -1L);
              out.append('|').append(failure(() -> ViewsFFM.Program_functions(bad)));
              bad.set(ValueLayout.JAVA_LONG, 8, Long.MAX_VALUE / 16 + 1);
              out.append('|').append(failure(() -> ViewsFFM.Program_functions(bad)))
                  .append('|').append(failure(() -> ViewsFFM.Program_functions(bad, 0, arena)));
              bad.set(ValueLayout.ADDRESS, 32, roots);
              bad.set(ValueLayout.JAVA_LONG, 40, Long.MAX_VALUE / 8 + 1);
              out.append('|').append(failure(() -> ViewsFFM.Program_roots(bad, 0)));
            }
            return out.toString();
          }
          private static boolean readOnly(Runnable write) {
            try {
              write.run();
              return false;
            } catch (IllegalArgumentException | UnsupportedOperationException expected) {
              return true;
            }
          }
          private static boolean closed(Runnable read) {
            try {
              read.run();
              return false;
            } catch (IllegalStateException expected) {
              return true;
            }
          }
          private static String failure(Runnable read) {
            try {
              read.run();
              return "no failure";
            } catch (RuntimeException e) {
              return e.getClass().getSimpleName() + ": " + e.getMessage();
            }
          }
          private static boolean outOfBounds(Runnable read) {
            try {
              read.run();
              return false;
            } catch (IndexOutOfBoundsException expected) {
              return true;
            }
          }
        }
        """;
    Object result = runOnHotSpot("fixture.views", "ViewsFFM", ffm, consumer);
    assertEquals(
        "32 6 11 16 i -1 16 16 true0 truetruetruetrue 00truetrue0true"
            + " 213i2truetruetruetruetrue truetruetruetruetruetruetruetruetruetruetrue0"
            + " 321116-1161110truetruetruetruetruetruetrue"
            + "|IndexOutOfBoundsException: Program.functions is null with functionCount 2"
            + "|IndexOutOfBoundsException: Program.functions is null with functionCount 2"
            + "|IndexOutOfBoundsException: Program.functions is null with functionCount 2"
            + "|IndexOutOfBoundsException: Program.functions is null with functionCount 2"
            + "|IndexOutOfBoundsException: Program.functions is misaligned: 0x? is not a"
            + " multiple of 8"
            + "|no failure"
            + "|IndexOutOfBoundsException: Program.functions: functionCount"
            + " 18446744073709551615 overflows a byte size"
            + "|IndexOutOfBoundsException: Program.functions: functionCount"
            + " 576460752303423488 overflows a byte size"
            + "|IndexOutOfBoundsException: Program.functions: functionCount"
            + " 576460752303423488 overflows a byte size"
            + "|IndexOutOfBoundsException: Program.roots: rootCount 1152921504606846976"
            + " overflows a byte size",
        result);
  }

  /** Compiles a generated FFM class beside a consumer and returns the consumer's {@code run()}. */
  private static Object runOnHotSpot(String pkg, String ffmClass, String ffm, String consumer)
      throws Exception {
    Path out = Files.createTempDirectory("myna-views");
    try {
      var diagnostics = new DiagnosticCollector<JavaFileObject>();
      boolean ok =
          ToolProvider.getSystemJavaCompiler()
              .getTask(
                  null,
                  null,
                  diagnostics,
                  List.of("-proc:none", "-d", out.toString()),
                  null,
                  List.of(source(pkg, ffmClass, ffm), source(pkg, "Consumer", consumer)))
              .call();
      assertTrue(ok, diagnostics.getDiagnostics().toString());
      try (var loader = new URLClassLoader(new URL[] {out.toUri().toURL()}, null)) {
        return loader.loadClass(pkg + ".Consumer").getMethod("run").invoke(null);
      }
    } finally {
      try (var walk = Files.walk(out)) {
        walk.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
      }
    }
  }

  private static JavaFileObject source(String pkg, String name, String text) {
    return new SimpleJavaFileObject(
        URI.create("string:///" + pkg.replace('.', '/') + "/" + name + ".java"),
        JavaFileObject.Kind.SOURCE) {
      @Override
      public CharSequence getCharContent(boolean ignoreEncodingErrors) {
        return text;
      }
    };
  }
}
