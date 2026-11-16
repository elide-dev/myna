// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package dev.elide.myna;

import static org.junit.jupiter.api.Assertions.*;

import dev.elide.myna.backend.StaticJni;
import dev.elide.myna.frontend.Dsl;
import dev.elide.myna.model.Seam;
import dev.elide.myna.model.Seam.*;
import dev.elide.myna.serialize.SeamJson;
import dev.elide.myna.validate.Validator;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class GeneratorTest {
  static final String MODULE = "module sample target=aarch64-apple-darwin\n";
  static final String FUNCTION = "import inspect return=i64 error=no_failure\n";
  static final String PARAM =
      "param value type=ptr<i64> nullable=false ownership=borrowed access=read\n";
  static final String VALID = MODULE + FUNCTION + PARAM + "end\n";

  static SortedMap<String, String> generate(String source) {
    return Generator.generate(Dsl.parse(source), true);
  }

  @Test
  void nativeLeafIsTheDefaultWithAnExplicitNormalTransitionOptOut() {
    String leaf =
        VALID.replace("error=no_failure", "error=no_failure native_leaf=true@explicit_contract");
    var output = generate(leaf);
    assertTrue(
        output.get("MynaNative.java").contains("transition = CFunction.Transition.NO_TRANSITION"));
    assertFalse(output.get("seam.ll").contains("native_leaf"));
    assertTrue(output.get("seam.json").contains("native_leaf"));
    assertTrue(generate(VALID).get("MynaNative.java").contains("NO_TRANSITION"));
    assertTrue(generate(VALID).get("seam.json").contains("\"bindingAbiVersion\": 2"));
    assertTrue(generate(VALID).get("seam.json").contains("\"nativeCallPolicy\":\"INLINE_LEAF\""));
    assertFalse(
        generate(leaf.replace("native_leaf=true", "native_leaf=false"))
            .get("MynaNative.java")
            .contains("NO_TRANSITION"));
    // The FFM static entry follows the same contract: leaves skip the transition as critical
    // downcalls, and an explicit normal transition keeps the transitioning adapter.
    assertTrue(
        output
            .get("MynaFFM.java")
            .contains(
                "MynaFFM.inspect_CriticalDowncall.HANDLE.invokeExact(MemorySegment.ofAddress("));
    String normal =
        generate(leaf.replace("native_leaf=true", "native_leaf=false")).get("MynaFFM.java");
    assertTrue(
        normal.contains("MynaFFM.inspect_Downcall.HANDLE.invokeExact(MemorySegment.ofAddress("));
    assertFalse(normal.contains("inspect_CriticalDowncall"));
    String heuristic = leaf.replace("explicit_contract", "heuristic");
    assertThrows(IllegalArgumentException.class, () -> generate(heuristic));
    assertFalse(
        Generator.generate(Dsl.parse(heuristic), false)
            .get("MynaNative.java")
            .contains("NO_TRANSITION"));
    assertThrows(
        IllegalArgumentException.class,
        () -> generate(leaf.replace("return=i64", "return=void noreturn=true@explicit_contract")));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            generate(
                MODULE
                    + "export test return=i64 error=integer_sentinel sentinel=-1"
                    + " java=fixture.Ops.call isolate=thread native_leaf=true@explicit_contract\n"
                    + "end\n"));
    assertNotEquals(generate(VALID).get("seam.abi"), output.get("seam.abi"));
  }

  @Test
  void javaNamingSelectsPackageClassesAndFilenames() {
    String named =
        VALID.replace(
            "module sample ", "module sample java_package=dev.example.io java_class=IoNatives ");
    var output = generate(named);
    assertEquals(
        Set.of(
            "META-INF/native-image/dev.elide.myna/dev.example.io.IoNatives/native-image.properties",
            "IoNatives.java",
            "IoNativesDirectives.java",
            "IoNativesFFM.java",
            "IoNativesForeignFeature.java",
            "seam.abi",
            "seam.h",
            "seam.json",
            "seam.ll",
            "seam.rs"),
        output.keySet());
    String nativeImage = output.get("IoNatives.java");
    assertTrue(nativeImage.contains("package dev.example.io;"));
    assertTrue(nativeImage.contains("public final class IoNatives {"));
    assertTrue(nativeImage.contains("@CContext(IoNativesDirectives.class)"));
    assertTrue(
        output
            .get("IoNativesDirectives.java")
            .contains("package dev.example.io;\n\n/**\n * The C context of {@link IoNatives}:"));
    assertTrue(output.get("IoNativesFFM.java").contains("IoNativesFFM.inspect_Downcall.HANDLE"));
    assertTrue(
        output
            .get("IoNativesForeignFeature.java")
            .contains("\"dev.example.io.IoNativesFFM$inspect_Binding\""));
    assertTrue(output.get("seam.json").contains("\"java\": {\"package\":\"dev.example.io\""));
    assertNotEquals(generate(VALID).get("seam.abi"), output.get("seam.abi"));
    assertFalse(generate(VALID).get("seam.json").contains("\"java\""));
    for (String bad : List.of("java_package=dev..io", "java_package=dev.fn", "java_class=1x"))
      assertThrows(
          IllegalArgumentException.class,
          () -> generate(VALID.replace("module sample ", "module sample " + bad + " ")));
  }

  @Test
  void rustPinsOnlyImportImplementations() {
    String rust =
        generate(
                VALID
                    + "export sum symbol=seam_sum return=i64 java=fixture.Ops.sum isolate=thread"
                    + " error=integer_sentinel sentinel=-1\nend\n")
            .get("seam.rs");
    assertTrue(rust.contains("let _: unsafe extern \"C\" fn(*const i64) -> i64 = inspect;"));
    assertFalse(rust.contains("= seam_sum;"));
    String exportsOnly =
        generate(
                MODULE
                    + "export sum return=i64 java=fixture.Ops.sum isolate=thread"
                    + " error=abort\nend\n")
            .get("seam.rs");
    assertFalse(exportsOnly.contains("assert_implementations"));
  }

  @Test
  void exportsCarryIncludePredicatesAndTypedCarriers() {
    String source =
        MODULE
            + "opaque Handle\n"
            + "export open symbol=x_open return=i64 java=fixture.Ops.open isolate=thread"
            + " error=abort include=fixture.ImageOnly\n"
            + "  param path type=ptr<u8> nullable=false access=read\n"
            + "  param argv type=ptr<ptr<u8>>\n"
            + "  param any type=ptr<void>\n"
            + "  param ints type=ptr<i32>\n"
            + "  param handle type=ptr<Handle>\n"
            + "end\n";
    var output = generate(source);
    String ni = output.get("MynaNative.java");
    assertTrue(
        ni.contains("@CEntryPoint(name = \"x_open\", include = fixture.ImageOnly.class)"), ni);
    assertTrue(
        ni.contains(
            "open(IsolateThread isolate_thread, @CConst CCharPointer path, CCharPointerPointer"
                + " argv, VoidPointer any, CIntPointer ints, Handle handle)"),
        ni);
    assertTrue(ni.contains("return fixture.Ops.open(path, argv, any, ints, handle);"));
    assertFalse(ni.contains("WordFactory"));
    assertFalse(ni.contains("CFunction"));
    assertTrue(output.get("seam.json").contains("\"include\":\"fixture.ImageOnly\""));
    assertNotEquals(
        output.get("seam.abi"),
        generate(source.replace(" include=fixture.ImageOnly", "")).get("seam.abi"));
    assertThrows(
        IllegalArgumentException.class,
        () -> generate(source.replace("include=fixture.ImageOnly", "include=ImageOnly")));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            generate(
                VALID.replace("error=no_failure", "error=no_failure include=fixture.ImageOnly")));
  }

  @Test
  void smallValuesExtendPerTargetAndLeaveJavaZeroExtended() {
    String source =
        "module small target=TARGET\n"
            + "import put return=u8 error=no_failure\n  param a type=i8\n  param b type=u8\n"
            + "  param c type=u16\n  param d type=bool\nend\n"
            + "export get symbol=x_get return=u16 java=fixture.Ops.get isolate=thread error=abort\n"
            + "  param a type=i16\nend\n";
    for (String target :
        List.of("x86_64-unknown-linux-gnu", "x86_64-apple-darwin", "aarch64-apple-darwin")) {
      String ll = generate(source.replace("TARGET", target)).get("seam.ll");
      assertTrue(
          ll.contains(
              "declare i8 @put(i8 signext %a, i8 zeroext %b, i16 zeroext %c, i1 zeroext %d)"),
          ll);
    }
    var output = generate(source.replace("TARGET", "aarch64-unknown-linux-gnu"));
    assertTrue(output.get("seam.ll").contains("declare i8 @put(i8 %a, i8 %b, i16 %c, i1 %d)"));
    String ni = output.get("MynaNative.java");
    assertTrue(ni.contains("public static byte put(byte a, byte b, short c, boolean d) {"), ni);
    assertTrue(ni.contains("return MynaSymbols.put.get().invoke(a, b & 0xff, c & 0xffff, d);"), ni);
    assertTrue(ni.contains("byte invoke(byte a, int b, int c, boolean d);"), ni);
    assertTrue(ni.contains("public static int get(IsolateThread isolate_thread, short a) {"), ni);
    assertTrue(ni.contains("return fixture.Ops.get(a) & 0xffff;"), ni);
    String rust = output.get("seam.rs");
    assertTrue(rust.contains("pub fn put(a: i8, b: u8, c: u16, d: bool) -> u8;"), rust);
    assertTrue(
        output.get("seam.h").contains("uint8_t put(int8_t a, uint8_t b, uint16_t c, bool d);"));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            generate(
                source.replace("TARGET", "x86_64-unknown-linux-gnu")
                    + "import putNative return=void error=no_failure\nend\n"));
  }

  @Test
  void ffmZeroExtendsUnsignedSmallParametersButNotReturnsOrSigned() {
    String source =
        "module small target=aarch64-apple-darwin\n"
            + "import put return=u8 error=no_failure\n"
            + "  param a type=i8\n  param b type=u8\n  param c type=u16\n  param d type=bool\n"
            + "  param e type=i32\n  param f type=u32\nend\n"
            + "export get symbol=x_get return=u16 java=fixture.Ops.get isolate=thread error=abort\n"
            + "  param a type=i16\nend\n";
    String ffm = generate(source).get("MynaFFM.java");
    // Unsigned u8/u16 parameters are zero-extended to a JAVA_INT slot and masked at the downcall;
    // the public wrapper keeps the typed byte/short carriers, mirroring NativeImage.widened.
    assertTrue(
        ffm.contains(
            "put_DESCRIPTOR = FunctionDescriptor.of(ValueLayout.JAVA_BYTE,"
                + " ValueLayout.JAVA_BYTE, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT,"
                + " ValueLayout.JAVA_BOOLEAN, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT)"),
        ffm);
    assertTrue(
        ffm.contains(
            "public static byte put(byte a, byte b, short c, boolean d, int e, int f)"
                + " throws Throwable {"),
        ffm);
    assertTrue(
        ffm.contains(
            "invokeExact(MemorySegment.ofAddress(MynaFFM.put_Binding.ADDRESS), a, b & 0xff, c &"
                + " 0xffff, d, e, f)"),
        ffm);
    // Signed (i8/i16), bool, 32-bit parameters, isolate, and returns are NOT widened or masked.
    assertTrue(
        ffm.contains(
            "get_DESCRIPTOR = FunctionDescriptor.of(ValueLayout.JAVA_SHORT,"
                + " ValueLayout.ADDRESS, ValueLayout.JAVA_SHORT)"),
        ffm);
    assertTrue(
        ffm.contains(
            "public static short get(MemorySegment isolate_thread, short a) throws Throwable {"),
        ffm);
    assertTrue(
        ffm.contains(
            "invokeExact(MemorySegment.ofAddress(MynaFFM.get_Binding.ADDRESS), isolate_thread, a)"),
        ffm);
    assertFalse(ffm.contains("a & 0xff"), ffm);
    assertFalse(ffm.contains("a & 0xffff"), ffm);
    assertFalse(ffm.contains("JAVA_CHAR"), ffm);
  }

  @Test
  void wordsAndCCharsMapToTargetTypes() {
    var output =
        generate(
            MODULE
                + "import lookup return=usize error=no_failure\n"
                + "  param name type=ptr<char> access=read\n  param out type=ptr<usize>\n"
                + "  param delta type=isize\n  param tag type=char\nend\n");
    assertTrue(
        output
            .get("seam.rs")
            .contains(
                "pub fn lookup(name: *const core::ffi::c_char, out: *mut usize, delta: isize, tag:"
                    + " core::ffi::c_char) -> usize;"),
        output.get("seam.rs"));
    assertTrue(
        output
            .get("seam.h")
            .contains("size_t lookup(char const * name, size_t * out, ptrdiff_t delta, char tag);"),
        output.get("seam.h"));
    String ni = output.get("MynaNative.java");
    assertTrue(
        ni.contains("long lookup(CCharPointer name, WordPointer out, long delta, byte tag) {"), ni);
    assertTrue(ni.contains("import org.graalvm.nativeimage.c.type.WordPointer;"));
    assertTrue(
        output
            .get("seam.ll")
            .contains("declare i64 @lookup(ptr %name, ptr %out, i64 %delta, i8 signext %tag)"));
    assertTrue(output.get("seam.rs").contains("pub type LookupFn = unsafe extern \"C\" fn("));
  }

  @Test
  void callbacksAndIsolateThreadsAreValidated() {
    String base =
        MODULE
            + "callback Sink return=i32\n"
            + "  param isolate type=isolate_thread\n"
            + "  param n type=i64\n"
            + "end\n"
            + "export sink return=i32 java=fixture.Ops.sink isolate=thread error=abort"
            + " callback=Sink\n"
            + "  param n type=i64\n"
            + "end\n"
            + "import run return=void error=no_failure\n"
            + "  param cb type=fn<Sink>\n"
            + "  param isolate type=isolate_thread\n"
            + "end\n";
    var output = generate(base);
    String ni = output.get("MynaNative.java");
    assertTrue(ni.contains("public interface Sink extends CFunctionPointer {"), ni);
    assertTrue(ni.contains("int invoke(IsolateThread isolate, long n);"), ni);
    assertTrue(
        ni.contains(
            "CEntryPointLiteral.create(MynaNative.class, \"sink\", IsolateThread.class,"
                + " long.class)"),
        ni);
    assertTrue(
        output.get("seam.h").contains("typedef int32_t (*Sink)(void * isolate, int64_t n);"));
    assertTrue(output.get("seam.json").contains("\"callback\":\"Sink\""));
    for (String bad :
        List.of(
            base.replace("callback=Sink", "callback=Missing"),
            base.replace("export sink return=i32", "export sink return=i64"),
            base.replace("param cb type=fn<Sink>", "param cb type=fn<Missing>"),
            base.replace("param n type=i64\nend\nexport", "param n type=u8\nend\nexport"),
            base.replace("import run return=void", "import run return=isolate_thread"),
            base.replace(
                "import run return=void error=no_failure",
                "import run return=void error=no_failure callback=Sink"),
            base + "struct Holder size=8 align=8\n  field t type=isolate_thread offset=0\nend\n",
            base + "opaque SinkFn\n",
            base.replace("param cb type=fn<Sink>", "param cb type=ptr<fn<Sink>>"),
            base.replace("param cb type=fn<Sink>", "param cb type=ptr<Sink>"),
            base + "struct Holder size=8 align=8\n  field cb type=ptr<Sink> offset=0\nend\n",
            base.replace("callback Sink", "callback i64"),
            base
                + "import collide symbol=Sink return=i32 error=no_failure\n"
                + "  param n type=i64\n"
                + "end\n",
            base
                + "export Sink return=i32 java=fixture.Ops.sink isolate=thread error=abort\n"
                + "  param n type=i64\nend\n"))
      assertThrows(IllegalArgumentException.class, () -> generate(bad), bad);
  }

  @Test
  void reservedLiteralsHolderNameIsNotUsableByUserTypes() {
    String base =
        MODULE
            + "callback Visitor return=i32\n  param isolate type=isolate_thread\n"
            + "  param code type=i32\nend\n"
            + "export visit symbol=visit return=i32 java=fixture.Ops.visit isolate=thread"
            + " error=abort callback=Visitor\n  param code type=i32\nend\n";
    String ni =
        generate(base + "struct Box size=8 align=8\n  field value type=i64 offset=0\nend\n")
            .get("MynaNative.java");
    assertEquals(
        1, java.util.regex.Pattern.compile("class Literals \\{").matcher(ni).results().count(), ni);
    for (String bad :
        List.of(
            base + "struct Literals size=8 align=8\n  field value type=i64 offset=0\nend\n",
            base.replace("callback Visitor", "callback Literals")
                .replace("callback=Visitor", "callback=Literals"),
            base + "opaque Literals\n"))
      assertThrows(IllegalArgumentException.class, () -> generate(bad), bad);
  }

  @Test
  void byValueRecordsLowerOnlyAtTheNativeImageBoundary() {
    String base =
        MODULE
            + "opaque Handle\n"
            + "struct Pt size=8 align=4\n"
            + "  field x type=i32 offset=0\n"
            + "  field y type=i32 offset=4\n"
            + "end\n"
            + "import flip symbol=pt_flip return=Pt error=no_failure\n"
            + "  param p type=Pt\n"
            + "  param k type=i32\n"
            + "end\n"
            + "export norm symbol=pt_norm return=i32 java=fixture.Ops.norm isolate=thread"
            + " error=abort\n"
            + "  param p type=Pt\n"
            + "end\n";
    var output = generate(base);
    String ni = output.get("MynaNative.java");
    assertTrue(ni.contains("CGlobalDataFactory.forSymbol(\"pt_flip_myna_ref\")"), ni);
    assertTrue(ni.contains("public static void flip(Pt p, int k, Pt result) {"), ni);
    assertTrue(ni.contains("@CEntryPoint(name = \"pt_norm_myna_ref\")"), ni);
    assertTrue(ni.contains("return fixture.Ops.norm(p);"), ni);
    assertTrue(ni.contains("        int SIZE = 8;\n"), ni);
    assertTrue(ni.contains("@CField(\"y\") int y();"), ni);
    String rust = output.get("seam.rs");
    assertTrue(rust.contains("pub fn flip(p: Pt, k: i32) -> Pt;"), rust);
    assertTrue(
        rust.contains(
            "pub fn normRef(isolate_thread: *mut core::ffi::c_void, p: *const Pt) -> i32;"),
        rust);
    assertTrue(
        rust.contains("pub unsafe fn norm(isolate_thread: *mut core::ffi::c_void, p: Pt) -> i32 {"),
        rust);
    assertTrue(rust.contains("#[derive(Clone, Copy)]"));
    assertTrue(
        rust.contains(
            "pub unsafe extern \"C\" fn pt_flip_myna_ref(p: *const $($seam)::+::Pt, k: i32, result:"
                + " *mut $($seam)::+::Pt)"),
        rust);
    assertTrue(rust.contains("unsafe { result.write(pt_flip(p.read(), k)) }"), rust);
    String ll = output.get("seam.ll");
    assertFalse(ll.contains("@pt_flip("), ll);
    assertTrue(
        ll.contains(
            "declare void @pt_flip_myna_ref(ptr align 4 dereferenceable(8) captures(none) nonnull"
                + " readonly %p, i32 %k, ptr align 4 dereferenceable(8) captures(none) nonnull"
                + " writeonly %result)"),
        ll);
    String h = output.get("seam.h");
    // By-value IMPORT in seam.h: declares both the by-value implementation (called by C/Rust/FFM)
    // AND the Native Image reference form `<symbol>_myna_ref` the image binds. Without the
    // reference-form declaration, a C provider implementing only the by-value form compiles
    // cleanly but leaves the image's `pt_flip_myna_ref` symbol undefined, failing at first use.
    assertTrue(h.contains("struct Pt pt_flip(struct Pt p, int32_t k);"), h);
    assertTrue(
        h.contains("void pt_flip_myna_ref(struct Pt const * p, int32_t k, struct Pt * result);"),
        h);
    assertTrue(
        h.contains("Native Image binds pt_flip_myna_ref; the C provider must also define it"), h);
    // The by-value prototype precedes the reference-form declaration so the C contract reads
    // "implement this, plus the binding the image resolves."
    assertTrue(h.indexOf("struct Pt pt_flip(") < h.indexOf("void pt_flip_myna_ref("), h);
    // EXPORT keeps its two forms: the binding prototype and the by-value inline wrapper.
    assertTrue(
        h.contains("int32_t pt_norm_myna_ref(void * isolate_thread, struct Pt const * p);"), h);
    assertTrue(
        h.contains("static inline int32_t pt_norm(void * isolate_thread, struct Pt p) {"), h);
    // A by-value IMPORT with a record RETURN lowers the return to `void` plus a trailing
    // `T *result`; the `pt_flip` case above already exercises that. A by-value IMPORT taking a
    // record but returning a scalar must lower only the record parameter, with no `result`,
    // mirroring the export-side `pt_norm` form.
    String scalarReturn =
        generate(
                MODULE
                    + "struct Pt size=8 align=4\n"
                    + "  field x type=i32 offset=0\n"
                    + "  field y type=i32 offset=4\n"
                    + "end\n"
                    + "import probe symbol=pt_probe return=i32 error=no_failure\n"
                    + "  param p type=Pt\n"
                    + "end\n")
            .get("seam.h");
    assertTrue(
        scalarReturn.contains("int32_t pt_probe_myna_ref(struct Pt const * p);"), scalarReturn);
    assertTrue(scalarReturn.contains("int32_t pt_probe(struct Pt p);"), scalarReturn);
    assertFalse(
        scalarReturn.contains("pt_probe_myna_ref(struct Pt const * p, struct Pt"), scalarReturn);
    // Scalar IMPORTS (no record) gain no `_myna_ref` declaration: nothing to lower.
    String scalar =
        generate(MODULE + "import scalar return=i32 error=no_failure\n  param v type=i32\nend\n")
            .get("seam.h");
    assertTrue(scalar.contains("int32_t scalar(int32_t v);"), scalar);
    assertFalse(scalar.contains("scalar_myna_ref"), scalar);
    assertFalse(output.get("MynaFFM.java").contains("public static Pt flip"));
    assertTrue(
        output
            .get("MynaFFM.java")
            .contains("flip(SegmentAllocator allocator, MemorySegment p, int k)"));
    for (String bad :
        List.of(
            base.replace("return=Pt error=no_failure", "return=Handle error=no_failure"),
            base.replace("param k type=i32", "param result type=i32"),
            base + "callback Cb return=void\n  param p type=Pt\nend\n",
            base + "struct Outer size=8 align=4\n  field inner type=Pt offset=0\nend\n",
            base + "import clash symbol=pt_flip_myna_ref return=void error=no_failure\nend\n"))
      assertThrows(IllegalArgumentException.class, () -> generate(bad), bad);
  }

  @Test
  void byValueImportStructsMustUseNaturalLayout() {
    // The JDK FFM linker (downcallHandle) rejects by-value struct layouts that are not the
    // natural C ABI; imports keep the real by-value layout, so non-canonical structs would throw
    // ExceptionInInitializerError at first use. Exports are lowered to the _myna_ref pointer form
    // and never feed a struct layout to the linker, so gaps there remain valid (as in C/Rust).
    String dense =
        "struct Pt size=8 align=4\n  field x type=i32 offset=0\n  field y type=i32 offset=4\nend\n";
    String denseImport =
        "import flip symbol=pt_flip return=Pt error=no_failure\n  param p type=Pt\nend\n";
    var ok = generate(MODULE + dense + denseImport);
    assertTrue(
        ok.get("MynaFFM.java").contains("FunctionDescriptor.of(Pt_LAYOUT, Pt_LAYOUT)"),
        ok.get("MynaFFM.java"));
    // Natural trailing alignment padding is canonical and accepted by the linker.
    String mix =
        "struct Mix size=16 align=8\n"
            + "  field a type=i64 offset=0\n"
            + "  field b type=i32 offset=8\n"
            + "end\n";
    var okMix =
        generate(
            MODULE + mix + "import takeMix return=i64 error=no_failure\n  param m type=Mix\nend\n");
    assertTrue(
        okMix.get("MynaFFM.java").contains("MemoryLayout.paddingLayout(4)"),
        okMix.get("MynaFFM.java"));
    // Reserved interior gap (u32 at 0 and 8, leaving a 4-byte hole) in a by-value import
    // parameter -> rejected.
    String gap =
        "struct Gap size=12 align=4\n"
            + "  field kind type=u32 offset=0\n"
            + "  field flags type=u32 offset=8\n"
            + "end\n";
    var gapParam =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                generate(
                    MODULE
                        + gap
                        + "import takeGap return=void error=no_failure\n"
                        + "  param m type=Gap\n"
                        + "end\n"));
    assertTrue(gapParam.getMessage().contains("Gap"), gapParam.getMessage());
    assertTrue(gapParam.getMessage().contains("natural C layout"), gapParam.getMessage());
    // Reserved interior gap reached via a by-value import RETURN is rejected too.
    assertThrows(
        IllegalArgumentException.class,
        () -> generate(MODULE + gap + "import makeGap return=Gap error=no_failure\nend\n"));
    // Over-declared trailing size in a by-value import -> rejected.
    String over =
        "struct Over size=16 align=4\n"
            + "  field a type=i32 offset=0\n"
            + "  field b type=i32 offset=4\n"
            + "end\n";
    assertThrows(
        IllegalArgumentException.class,
        () ->
            generate(
                MODULE
                    + over
                    + "import takeOver return=void error=no_failure\n  param o type=Over\nend\n"));
    // Leading gap (first field not at offset 0) in a by-value import -> rejected.
    String lead = "struct Lead size=16 align=8\n  field a type=i64 offset=8\nend\n";
    assertThrows(
        IllegalArgumentException.class,
        () ->
            generate(
                MODULE
                    + lead
                    + "import takeLead return=void error=no_failure\n  param l type=Lead\nend\n"));
    // The same non-canonical struct used by value only in an EXPORT is accepted: FFM lowers it to
    // the _myna_ref pointer form (no by-value layout reaches the linker), and C/Rust still honor
    // the declared gap with explicit padding fields.
    var overExport =
        generate(
            MODULE
                + over
                + "export scaleOver symbol=seam_scale_over return=Over java=fixture.Ops.scale"
                + " isolate=thread error=abort\n  param o type=Over\nend\n");
    assertTrue(
        overExport.get("MynaFFM.java").contains("seam_scale_over_myna_ref"),
        overExport.get("MynaFFM.java"));
    assertTrue(
        overExport.get("MynaFFM.java").contains("MemoryLayout.paddingLayout(8)"),
        overExport.get("MynaFFM.java"));
    assertTrue(
        overExport.get("seam.rs").contains("seamPadding0: [u8; 8]"), overExport.get("seam.rs"));
    // A non-canonical struct never used by value in an import is accepted and still emits its
    // declared trailing padding in FFM (C/Rust support for explicit gaps is preserved).
    var unused =
        generate(
            MODULE
                + over
                + "import probe return=i64 error=no_failure\n  param v type=ptr<i64> nullable=false"
                + " ownership=borrowed access=read\nend\n");
    assertTrue(
        unused.get("MynaFFM.java").contains("MemoryLayout.paddingLayout(8)"),
        unused.get("MynaFFM.java"));
    // A struct used by value in BOTH an import and an export is rejected because of the import.
    assertThrows(
        IllegalArgumentException.class,
        () ->
            generate(
                MODULE
                    + over
                    + "import takeOver return=void error=no_failure\n  param o type=Over\nend\n"
                    + "export scaleOver symbol=seam_scale_over return=Over java=fixture.Ops.scale"
                    + " isolate=thread error=abort\n  param o type=Over\nend\n"));
  }

  @Test
  void byValueExportRefIdentifierMustNotCollide() {
    String struct =
        "struct Pair size=16 align=8\n"
            + "  field left type=i64 offset=0\n"
            + "  field right type=i32 offset=8\n"
            + "end\n";
    String byValueScale =
        "export scale symbol=seam_scale return=Pair java=fixture.Ops.scale isolate=thread"
            + " error=abort\n  param value type=Pair\nend\n";
    String byValueScaleRef =
        "export scaleRef symbol=seam_scaleref return=Pair java=fixture.Ops.scaleref isolate=thread"
            + " error=abort\n  param value type=Pair\nend\n";
    String scalarImportScaleRef =
        "import scaleRef return=i64 error=no_failure\n  param v type=ptr<i64> nullable=false"
            + " ownership=borrowed access=read\nend\n";
    // Extern-block duplicate: by-value export's synthesised `pub fn scaleRef` vs an import of that
    // name.
    assertThrows(
        IllegalArgumentException.class,
        () -> generate(MODULE + struct + byValueScale + scalarImportScaleRef),
        "import named <name>Ref must clash with a by-value export's synthesised extern");
    // Module-level duplicate: by-value export of `<name>Ref` wrapper vs another export's
    // synthesised extern.
    assertThrows(
        IllegalArgumentException.class,
        () -> generate(MODULE + struct + byValueScale + byValueScaleRef),
        "by-value export of <name>Ref must clash with another by-value export's synthesised"
            + " extern");
    // A by-value import named scaleRef synthesises no Ref extern, so it must remain valid.
    String byValueImportScaleRef =
        "import scaleRef symbol=seam_scaleref return=Pair error=no_failure\n"
            + "  param value type=Pair\n"
            + "end\n";
    assertTrue(
        generate(MODULE + struct + byValueImportScaleRef)
            .get("seam.rs")
            .contains("pub fn scaleRef("),
        "a by-value import keeps its own extern name and is not rejected");
    // An unrelated `Ref`-suffixed name does not trigger the guard.
    assertDoesNotThrow(() ->
        generate(
            MODULE
                + struct
                + byValueScale
                + "import spreadRef return=i64 error=no_failure\n  param v type=ptr<i64>"
                + " nullable=false ownership=borrowed access=read\nend\n"));
    // Happy path: a single by-value export emits exactly one `pub fn scaleRef` extern item.
    String rust = generate(MODULE + struct + byValueScale).get("seam.rs");
    assertEquals(
        1,
        java.util.regex.Pattern.compile("pub fn scaleRef\\(").matcher(rust).results().count(),
        "by-value export enumerates its synthesised extern exactly once");
  }

  @Test
  void rustAliasCollisionsAcrossFunctionsAreRejected() {
    // The dynamic-resolution `<Name>Fn` alias capitalises the function's first letter, so two
    // functions whose names differ only in that case (parseX / ParseX) both map to ParseXFn and
    // would emit duplicate `pub type ParseXFn` items that rustc rejects with E0428.
    String importPair =
        MODULE
            + "import parseX symbol=myna_parse_x_lower return=i64 error=no_failure\n"
            + "  param x type=i64\n"
            + "end\n"
            + "import ParseX symbol=myna_parse_x_upper return=i64 error=no_failure\n"
            + "  param x type=i64\n"
            + "end\n";
    var collision =
        assertThrows(
            IllegalArgumentException.class,
            () -> generate(importPair),
            "case-colliding imports must be rejected");
    assertEquals(
        "Rust alias ParseXFn collides with another function's alias", collision.getMessage());
    // Exports lower through the isolate-thread path but keep the same name derivation, so they
    // collide too.
    String exportPair =
        MODULE
            + "export parseX symbol=myna_parse_x_lower return=i64 java=fixture.Ops.px"
            + " isolate=thread error=integer_sentinel sentinel=-1\n"
            + "  param x type=i64\n"
            + "end\n"
            + "export ParseX symbol=myna_parse_x_upper return=i64 java=fixture.Ops.pX"
            + " isolate=thread error=integer_sentinel sentinel=-1\n"
            + "  param x type=i64\n"
            + "end\n";
    assertThrows(
        IllegalArgumentException.class,
        () -> generate(exportPair),
        "case-colliding exports must be rejected");
    // The alias set is shared across directions in one module.
    String mixedPair =
        MODULE
            + "import parseX symbol=myna_parse_x_lower return=i64 error=no_failure\n"
            + "  param x type=i64\n"
            + "end\n"
            + "export ParseX symbol=myna_parse_x_upper return=i64 java=fixture.Ops.pX"
            + " isolate=thread error=integer_sentinel sentinel=-1\n"
            + "  param x type=i64\n"
            + "end\n";
    assertThrows(
        IllegalArgumentException.class,
        () -> generate(mixedPair),
        "a mixed-direction alias collision must be rejected");
    // The check is case-sensitive on the derived alias: parseX / PARSEX map to ParseXFn / PARSEXFn,
    // both distinct, so both must remain valid (guards against an over-eager case-insensitive fix).
    String caseSensitive =
        MODULE
            + "import parseX symbol=myna_parse_x return=i64 error=no_failure\n"
            + "  param x type=i64\n"
            + "end\n"
            + "import PARSEX symbol=myna_parse_upper return=i64 error=no_failure\n"
            + "  param x type=i64\n"
            + "end\n";
    var rust = generate(caseSensitive).get("seam.rs");
    assertTrue(rust.contains("pub type ParseXFn ="), "parseX emits ParseXFn");
    assertTrue(
        rust.contains("pub type PARSEXFn ="), "PARSEX emits PARSEXFn, distinct from ParseXFn");
  }

  /**
   * Issue #61: one platform-neutral binding must accept every ABI-class-mate target's library.
   * Target ABI classes split on {@code char} signedness: AAPCS64 Linux (aarch64-unknown-linux-gnu)
   * is its own class because {@code char} is unsigned there, while x86-64 and Apple arm64 share one
   * signed-char LP64 little-endian class. Within a class every target produces the same fingerprint
   * and byte-identical platform-neutral outputs; only {@code seam.ll} names the triple.
   */
  @Test
  void sameAbiClassTargetsShareFingerprintsAndPlatformNeutralOutputs() throws Exception {
    Map<TargetAbi, List<String>> byClass = new LinkedHashMap<>();
    for (String t : Validator.TARGETS)
      byClass.computeIfAbsent(Seam.targetAbi(t), k -> new ArrayList<>()).add(t);
    assertEquals(2, byClass.size(), "signed-char and unsigned-char LP64-little ABI classes");
    for (String example :
        List.of("examples/buffer.seam", "examples/ownership.seam", "examples/truffle.seam")) {
      var base = Dsl.parse(Files.readString(Path.of(example)));
      var signedFingerprint = SeamJson.fingerprint(base.withTarget("x86_64-unknown-linux-gnu"));
      var aarch64LinuxFingerprint =
          SeamJson.fingerprint(base.withTarget("aarch64-unknown-linux-gnu"));
      assertNotEquals(
          signedFingerprint,
          aarch64LinuxFingerprint,
          example + ": AAPCS64 Linux has a different ABI fingerprint (char is unsigned)");
      for (var entry : byClass.entrySet()) {
        var abi = entry.getKey();
        var targets = entry.getValue();
        var referenceTarget = targets.getFirst();
        var reference = Generator.generate(base.withTarget(referenceTarget), true);
        var referenceJni = StaticJni.generate(base.withTarget(referenceTarget));
        var abiJson =
            "\"targetAbi\": {\"dataModel\":\"LP64\",\"byteOrder\":\"little\""
                + (abi.charUnsigned() ? ",\"charUnsigned\":true" : "")
                + "}";
        assertTrue(reference.get("seam.json").contains(abiJson), example + " reference abi json");
        for (String target : targets) {
          var module = base.withTarget(target);
          var outputs = Generator.generate(module, true);
          assertEquals(reference.keySet(), outputs.keySet(), target);
          for (var output : outputs.entrySet()) {
            if (output.getKey().equals("seam.ll")) {
              assertTrue(output.getValue().contains("target triple = \"" + target + "\""), target);
              assertTrue(output.getValue().contains(reference.get("seam.abi").strip()), target);
            } else
              assertEquals(
                  reference.get(output.getKey()),
                  output.getValue(),
                  example + " " + target + " " + output.getKey());
          }
          assertFalse(outputs.get("seam.json").contains(target), target);
          assertTrue(outputs.get("seam.json").contains(abiJson), target);
          assertEquals(
              SeamJson.fingerprint(module) + "\n",
              reference.get("seam.abi"),
              "fingerprint is the descriptor hash for " + target);
          for (var output : StaticJni.generate(module).entrySet())
            if (!output.getKey().endsWith(".ll"))
              assertEquals(referenceJni.get(output.getKey()), output.getValue(), output.getKey());
        }
      }
    }
    assertThrows(IllegalArgumentException.class, () -> Seam.targetAbi("riscv64-unknown-linux-gnu"));
    assertThrows(IllegalArgumentException.class, () -> Seam.targetAbi(null));
    assertEquals("LP64, little-endian", Seam.targetAbi("aarch64-apple-darwin").toString());
    assertEquals(
        "LP64, little-endian, unsigned char",
        Seam.targetAbi("aarch64-unknown-linux-gnu").toString());
  }

  @Test
  void goldenOutputs() throws Exception {
    var actual = generate(Files.readString(Path.of("examples/buffer.seam")));
    for (var output : actual.entrySet())
      assertEquals(
          Files.readString(Path.of("tests/golden", output.getKey())),
          output.getValue(),
          output.getKey());
  }

  /**
   * A generation fingerprints the module once and hands that fingerprint to every backend; each
   * backend used on its own computes it and must produce the same text.
   */
  @Test
  void standaloneBackendsMatchOneGeneration() throws Exception {
    for (String example : List.of("examples/truffle.seam", "examples/constants.seam")) {
      var module = Dsl.parse(Files.readString(Path.of(example)));
      var generated = Generator.generate(module, true);
      var fingerprint = new dev.elide.myna.serialize.Fingerprint(module);
      Validator.validate(module, true, fingerprint);
      assertEquals(SeamJson.canonical(module), fingerprint.json());
      assertEquals(SeamJson.fingerprint(module) + "\n", generated.get("seam.abi"));
      String abi = fingerprint.value();
      for (dev.elide.myna.backend.Backend backend :
          List.of(
              new dev.elide.myna.backend.CHeader(),
              new dev.elide.myna.backend.RustFfi(),
              new dev.elide.myna.backend.JavaFfm(),
              new dev.elide.myna.backend.NativeImage(),
              new dev.elide.myna.backend.NativeImageDirectives(),
              new dev.elide.myna.backend.NativeImageProperties(),
              new dev.elide.myna.backend.Llvm(),
              new dev.elide.myna.backend.TruffleCells())) {
        String expected = generated.get(backend.filename(module));
        assertEquals(expected, backend.generate(module), example + " " + backend.filename(module));
        assertEquals(
            expected, backend.generate(module, abi), example + " " + backend.filename(module));
      }
    }
  }

  @Test
  void constantsGoldenOutputs() throws Exception {
    var actual = generate(Files.readString(Path.of("examples/constants.seam")));
    assertFalse(actual.isEmpty());
    try (var files = Files.list(Path.of("tests/golden/constants"))) {
      assertTrue(files.findAny().isPresent());
    }
    for (var output : actual.entrySet())
      assertEquals(
          Files.readString(Path.of("tests/golden/constants", output.getKey())),
          output.getValue(),
          output.getKey());
  }

  @Test
  void determinismIgnoresCommentsWhitespaceAndDeclarationOrder() {
    String a =
        MODULE
            + "opaque Zebra\nopaque Alpha\n"
            + FUNCTION
            + PARAM
            + "end\nimport beta return=void error=no_failure\nend\n";
    String b =
        "# comment\n  "
            + MODULE
            + "opaque Alpha\nopaque Zebra\nimport beta return=void error=no_failure\nend\n  "
            + FUNCTION
            + "  "
            + PARAM
            + "end # trailing\n";
    assertEquals(generate(a), generate(b));
  }

  @Test
  void abiFingerprintChangesWithSignatureAndContractsButNotSameLayoutTarget() {
    String original = generate(VALID).get("seam.abi");
    assertEquals(original, generate(VALID.replace("aarch64", "x86_64")).get("seam.abi"));
    for (String change :
        List.of(
            VALID.replace("return=i64", "return=i32"),
            VALID.replace("access=read", "access=read readonly=true@explicit_contract")))
      assertNotEquals(original, generate(change).get("seam.abi"));
  }

  @Test
  void ffmCallsitesAreTypedConstantAndAotRegistered() {
    var output = generate(VALID);
    String ffm = output.get("MynaFFM.java");
    assertTrue(ffm.contains("static final MethodHandle HANDLE"));
    assertTrue(ffm.contains("public static long inspect(MemorySegment value) throws Throwable"));
    assertTrue(
        ffm.contains(
            "return (long)"
                + " MynaFFM.inspect_CriticalDowncall.HANDLE.invokeExact("
                + "MemorySegment.ofAddress(MynaFFM.inspect_Binding.ADDRESS), value)"));
    assertTrue(ffm.contains("static final long ADDRESS = SymbolLookup.loaderLookup()"));
    assertFalse(
        ffm.contains("MynaFFM.inspect_Downcall.HANDLE.invokeExact(MemorySegment.ofAddress("));
    assertTrue(ffm.contains("downcallHandle(MynaFFM.inspect_DESCRIPTOR)"));
    assertFalse(ffm.contains("invokeWithArguments"));
    String feature = output.get("MynaForeignFeature.java");
    assertTrue(feature.contains("duringSetup"));
    assertTrue(feature.contains("initializeAtBuildTime(\"dev.elide.myna.generated.MynaFFM\");"));
    assertTrue(
        feature.contains(
            "initializeAtBuildTime(\"dev.elide.myna.generated.MynaFFM$inspect_Downcall\")"));
    assertTrue(
        feature.contains("RuntimeForeignAccess.registerForDowncall(MynaFFM.inspect_DESCRIPTOR)"));
    assertTrue(
        feature.contains(
            "initializeAtRunTime(\"dev.elide.myna.generated.MynaFFM$inspect_Binding\")"));
  }

  @Test
  void absenceOfContractsDoesNotInferAttributes() {
    String llvm = generate(VALID).get("seam.ll");
    for (String attr :
        List.of("noalias", "captures(", "readonly", "memory(", "nonnull", "nounwind"))
      assertFalse(llvm.contains(attr), attr);
  }

  @Test
  void strictRejectsAndRelaxedOmitsUnapprovedFacts() {
    for (String provenance : List.of("heuristic", "language_derived", "abi_derived")) {
      var module =
          Dsl.parse(VALID.replace("access=read", "access=read readonly=true@" + provenance));
      assertThrows(IllegalArgumentException.class, () -> Generator.generate(module, true));
      var output = Generator.generate(module, false);
      assertFalse(output.get("seam.ll").contains("readonly"));
      assertTrue(output.get("seam.json").contains(provenance.toUpperCase(Locale.ROOT)));
    }
  }

  @Test
  void explicitAndProvenContractsAreEmittedWithProvenance() {
    for (String provenance : List.of("explicit_contract", "compiler_proven")) {
      var output =
          generate(
              VALID.replace(
                  "access=read",
                  "access=read nonnull=true@"
                      + provenance
                      + " nocapture=true@"
                      + provenance
                      + " readonly=true@"
                      + provenance
                      + " align=8@"
                      + provenance
                      + " dereferenceable=8@"
                      + provenance
                      + " noalias=true@"
                      + provenance));
      for (String attr :
          List.of(
              "nonnull", "captures(none)", "readonly", "align 8", "dereferenceable(8)", "noalias"))
        assertTrue(output.get("seam.ll").contains(attr), attr);
      assertTrue(output.get("seam.json").contains(provenance.toUpperCase(Locale.ROOT)));
    }
  }

  @Test
  void falseFactsAreRetainedButNotEmitted() {
    var output = generate(VALID.replace("access=read", "access=read noalias=false@heuristic"));
    assertFalse(output.get("seam.ll").contains("noalias"));
    assertTrue(output.get("seam.json").contains("HEURISTIC"));
  }

  @Test
  void exportedIsolateAndExceptionTranslationAgreeAcrossBackends() {
    var output =
        generate(
            MODULE
                + "export add return=i64 java=fixture.Ops.add isolate=thread error=integer_sentinel"
                + " sentinel=-1 nounwind=true@explicit_contract\n"
                + "param amount type=i64\n"
                + "end\n");
    assertTrue(output.get("seam.h").contains("add(void * isolate_thread, int64_t amount)"));
    assertTrue(output.get("seam.rs").contains("isolate_thread: *mut core::ffi::c_void"));
    assertTrue(output.get("seam.ll").contains("@add(ptr %isolate_thread, i64 %amount) nounwind"));
    assertTrue(
        output
            .get("MynaFFM.java")
            .contains("of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG)"));
    assertTrue(output.get("MynaNative.java").contains("fixture.Ops.add(amount)"));
    assertTrue(output.get("MynaNative.java").contains("catch (Throwable failure)"));
    assertTrue(output.get("MynaNative.java").contains("return -1L;"));
  }

  @Test
  void errorAdaptations() {
    var nullResult =
        generate(
            MODULE
                + "export create return=ptr<void> java=fixture.Ops.create isolate=thread"
                + " error=null_sentinel\n"
                + "end\n");
    assertTrue(nullResult.get("MynaNative.java").contains("WordFactory.nullPointer()"));
    for (String returns : List.of("void", "i32")) {
      var abort =
          generate(
              MODULE
                  + "export perform return="
                  + returns
                  + " java=fixture.Ops.perform isolate=thread error=abort\nend\n");
      assertFalse(abort.get("MynaNative.java").contains("catch (Throwable"));
    }
    var intResult =
        generate(
            MODULE
                + "export create return=i32 java=fixture.Ops.create isolate=thread"
                + " error=integer_sentinel sentinel=-2147483648\n"
                + "end\n");
    assertTrue(intResult.get("MynaNative.java").contains("return -2147483648;"));
  }

  @Test
  void everyScalarAndNestedPointerLowersInRecords() {
    String source =
        MODULE
            + "opaque Handle\n"
            + "struct AllTypes size=72 align=8\n"
            + "field a type=i8 offset=0\n"
            + "field b type=u8 offset=1\n"
            + "field c type=i16 offset=2\n"
            + "field d type=u16 offset=4\n"
            + "field e type=i32 offset=8\n"
            + "field f type=u32 offset=12\n"
            + "field g type=i64 offset=16\n"
            + "field h type=u64 offset=24\n"
            + "field i type=f32 offset=32\n"
            + "field j type=f64 offset=40\n"
            + "field k type=ptr<Handle> offset=48\n"
            + "field l type=ptr<ptr<u8>> offset=56\n"
            + "end\n"
            + "import mutate return=ptr<Handle> error=no_failure\n"
            + "param output type=ptr<ptr<u8>> access=write writeonly=true@explicit_contract\n"
            + "end\n";
    var output = generate(source);
    assertTrue(output.get("seam.h").contains("uint8_t * * output"));
    assertTrue(output.get("seam.rs").contains("*mut *mut u8"));
    assertTrue(output.get("MynaFFM.java").contains("MemoryLayout.paddingLayout(8)"));
    assertTrue(output.get("seam.ll").contains("ptr writeonly"));
  }

  @Test
  void nativeImageRecordsAreCStructs() {
    String source =
        MODULE
            + "opaque Handle\n"
            + "struct Rec size=80 align=8\n"
            + "field signed64 type=i64 offset=0\n"
            + "field unsigned64 type=u64 offset=8\n"
            + "field size type=usize offset=16\n"
            + "field offset type=isize offset=24\n"
            + "field ratio type=f64 offset=32\n"
            + "field next type=ptr<Rec> offset=40\n"
            + "field handle type=ptr<Handle> offset=48\n"
            + "field signed32 type=i32 offset=56\n"
            + "field unsigned32 type=u32 offset=60\n"
            + "field scale type=f32 offset=64\n"
            + "field signed16 type=i16 offset=68\n"
            + "field unsigned16 type=u16 offset=70\n"
            + "field flag type=bool offset=72\n"
            + "field letter type=char offset=73\n"
            + "field signed8 type=i8 offset=74\n"
            + "field unsigned8 type=u8 offset=75\n"
            + "end\n"
            + "import first return=i64 error=no_failure\n"
            + "param rec type=ptr<Rec>\n"
            + "end\n";
    String ni = generate(source).get("MynaNative.java");
    String expected =
        """
            @CStruct(value = "Rec", addStructKeyword = true)
            public interface Rec extends PointerBase {
                int SIZE = 80;
                int ALIGNMENT = 8;
                @CField("signed64") long signed64();
                @CField("signed64") void signed64(long value);
                @CFieldAddress("signed64") CLongPointer addressOfSigned64();
                @CField("unsigned64") long unsigned64();
                @CField("unsigned64") void unsigned64(long value);
                @CFieldAddress("unsigned64") CLongPointer addressOfUnsigned64();
                @CField("size") long size();
                @CField("size") void size(long value);
                @CFieldAddress("size") WordPointer addressOfSize();
                @CField("offset") long offset();
                @CField("offset") void offset(long value);
                @CFieldAddress("offset") WordPointer addressOfOffset();
                @CField("ratio") double ratio();
                @CField("ratio") void ratio(double value);
                @CFieldAddress("ratio") CDoublePointer addressOfRatio();
                @CField("next") Rec next();
                @CField("next") void next(Rec value);
                @CFieldAddress("next") PointerBase addressOfNext();
                @CField("handle") Handle handle();
                @CField("handle") void handle(Handle value);
                @CFieldAddress("handle") PointerBase addressOfHandle();
                @CField("signed32") int signed32();
                @CField("signed32") void signed32(int value);
                @CFieldAddress("signed32") CIntPointer addressOfSigned32();
                @CField("unsigned32") int unsigned32();
                @CField("unsigned32") void unsigned32(int value);
                @CFieldAddress("unsigned32") CIntPointer addressOfUnsigned32();
                @CField("scale") float scale();
                @CField("scale") void scale(float value);
                @CFieldAddress("scale") CFloatPointer addressOfScale();
                @CField("signed16") short signed16();
                @CField("signed16") void signed16(short value);
                @CFieldAddress("signed16") CShortPointer addressOfSigned16();
                @CField("unsigned16") short unsigned16();
                @CField("unsigned16") void unsigned16(short value);
                @CFieldAddress("unsigned16") CShortPointer addressOfUnsigned16();
                @CField("flag") boolean flag();
                @CField("flag") void flag(boolean value);
                @CFieldAddress("flag") CCharPointer addressOfFlag();
                @CField("letter") byte letter();
                @CField("letter") void letter(byte value);
                @CFieldAddress("letter") CCharPointer addressOfLetter();
                @CField("signed8") byte signed8();
                @CField("signed8") void signed8(byte value);
                @CFieldAddress("signed8") CCharPointer addressOfSigned8();
                @CField("unsigned8") byte unsigned8();
                @CField("unsigned8") void unsigned8(byte value);
                @CFieldAddress("unsigned8") CCharPointer addressOfUnsigned8();
            }
        """;
    assertTrue(ni.contains(expected), ni);
    assertTrue(ni.contains("    public interface Handle extends PointerBase {}\n"), ni);
    for (String i :
        List.of(
            "org.graalvm.nativeimage.c.struct.CStruct",
            "org.graalvm.nativeimage.c.struct.CField",
            "org.graalvm.nativeimage.c.struct.CFieldAddress"))
      assertTrue(ni.contains("import " + i + ";\n"), i);
    // Records are typed pointers: no static accessor classes, no raw offset reads.
    assertFalse(ni.contains("static final class Rec"), ni);
    assertFalse(ni.contains("((Pointer) record)"), ni);
    assertFalse(ni.contains("import org.graalvm.word.Pointer;"), ni);
    assertTrue(ni.contains(" long first(Rec rec)"), ni);
    // Accessor names may not redeclare a word method or another field's address accessor.
    for (String method : List.of("isNull", "isNonNull", "rawValue", "equal", "notEqual"))
      assertEquals(
          "field collides with a Native Image word method: " + method,
          assertThrows(
                  IllegalArgumentException.class,
                  () -> generate(source.replace("unsigned8 type=u8", method + " type=u8")))
              .getMessage());
    assertEquals(
        "field collides with the Native Image address accessor addressOfNext of next",
        assertThrows(
                IllegalArgumentException.class,
                () -> generate(source.replace("unsigned8 type=u8", "addressOfNext type=u8")))
            .getMessage());
    // Fields x and X would both generate addressOfX.
    assertEquals(
        "field collides with the Native Image address accessor addressOfSigned8 of Signed8",
        assertThrows(
                IllegalArgumentException.class,
                () -> generate(source.replace("unsigned8 type=u8", "Signed8 type=u8")))
            .getMessage());
    // Declared types are nested in the Native Image class: they may not shadow it or a name it
    // imports or declares.
    for (String type : List.of("PointerBase", "CStruct", "CField", "CCharPointer", "String")) {
      for (String shadow :
          List.of(
              source.replace("opaque Handle", "opaque " + type),
              source
                  .replace("struct Rec", "struct " + type)
                  .replace("ptr<Rec>", "ptr<" + type + ">")))
        assertEquals(
            "type shadows generated name: " + type,
            assertThrows(IllegalArgumentException.class, () -> generate(shadow)).getMessage());
    }
    String named = source.replace(MODULE.strip(), MODULE.strip() + " java_class=Handle");
    assertEquals(
        "type shadows generated name: Handle",
        assertThrows(IllegalArgumentException.class, () -> generate(named)).getMessage());
    assertTrue(
        generate(source.replace("opaque Handle", "opaque Pointer").replace("<Handle>", "<Pointer>"))
            .get("MynaNative.java")
            .contains("public interface Pointer extends PointerBase {}"));
  }

  @Test
  void typedOpaquePointers() {
    String source =
        MODULE
            + "opaque Handle\n"
            + "struct Pair size=16 align=8\n"
            + "field left type=i64 offset=0\n"
            + "field right type=ptr<ptr<u8>> offset=8\n"
            + "end\n"
            + "callback Visit return=i32\n"
            + "param handle type=ptr<Handle>\n"
            + "param pair type=ptr<Pair>\n"
            + "end\n"
            + "import open return=ptr<Handle> error=no_failure"
            + " native_leaf=false@explicit_contract\n"
            + "param name type=ptr<u8>\n"
            + "end\n"
            + "import peek return=ptr<Pair> error=no_failure\n"
            + "param handle type=ptr<Handle>\n"
            + "param names type=ptr<ptr<Pair>>\n"
            + "end\n"
            + "export visit symbol=x_visit return=i32 java=fixture.Ops.visit isolate=thread"
            + " error=abort\n"
            + "param handle type=ptr<Handle>\n"
            + "param pair type=ptr<Pair> access=read\n"
            + "end\n";
    String ni = generate(source).get("MynaNative.java");
    assertTrue(ni.contains("    public interface Handle extends PointerBase {}\n"), ni);
    // A transitioning @CFunction returning ptr<Opaque> returns the opaque interface.
    assertTrue(
        ni.contains(
            "    @CFunction(value = \"open\")\n"
                + "    public static native Handle open(CCharPointer name);\n"),
        ni);
    // Default leaves carry the same types; ptr<ptr<…>> keeps today's carriers.
    assertTrue(ni.contains("Pair invoke(Handle handle, PointerBase names);"), ni);
    assertTrue(ni.contains("public static Pair peek(Handle handle, PointerBase names) {"), ni);
    assertTrue(ni.contains("int invoke(Handle handle, Pair pair);"), ni);
    assertTrue(ni.contains("Handle handle, @CConst Pair pair) {"), ni);
    assertTrue(ni.contains("@CField(\"right\") CCharPointerPointer right();"), ni);
  }

  @Test
  void scalarFunctionCarriers() {
    for (String type : List.of("i32", "u32", "i64", "u64", "f32", "f64")) {
      assertEquals(
          10,
          generate(
                  MODULE
                      + "import identity return="
                      + type
                      + " error=no_failure\nparam value type="
                      + type
                      + "\nend\n")
              .size());
    }
    assertTrue(
        generate(
                MODULE
                    + "import halt return=void error=no_failure native_leaf=false@explicit_contract"
                    + " noreturn=true@explicit_contract nofree=true@explicit_contract\n"
                    + "end\n")
            .get("seam.ll")
            .contains("nofree noreturn"));
  }

  @Test
  void numericSentinelsAreNormalizedAndHugeOffsetsFailCleanly() {
    var result =
        generate(
            MODULE
                + "export f return=i32 error=integer_sentinel sentinel=09 isolate=thread"
                + " java=fixture.Ops.f\n"
                + "end\n");
    assertTrue(result.get("MynaNative.java").contains("return 9;"));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            generate(
                MODULE
                    + "struct X size=8 align=8\n"
                    + "field a type=i64 offset=9223372036854775800\n"
                    + "end\n"));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            generate(
                MODULE
                    + "export f return=void error=abort isolate=thread java=fixture.Ops.class\n"
                    + "end\n"));
  }

  @Test
  void jsonEscapingAndImmutability() {
    assertEquals("\"\"", SeamJson.quote(""));
    assertEquals("\"caf\u00e9 \ud83d\ude80\"", SeamJson.quote("caf\u00e9 \ud83d\ude80"));
    assertEquals("\"\\n\"", SeamJson.quote("\n"));
    assertEquals("\"\\\\\"", SeamJson.quote("\\"));
    assertEquals("\"a\\\"\\\\\\n\\r\\t\\u0001\"", SeamJson.quote("a\"\\\n\r\t\u0001"));
    assertEquals("null", SeamJson.quote(null));
    var module = Dsl.parse(VALID);
    assertThrows(UnsupportedOperationException.class, () -> module.functions().clear());
    assertThrows(
        UnsupportedOperationException.class,
        () -> module.functions().getFirst().parameters().getFirst().facts().clear());
  }

  static Stream<String> invalidInputs() {
    return Stream.of(
        "",
        "unknown thing",
        "module",
        MODULE + MODULE,
        MODULE + "opaque",
        MODULE + "opaque X extra",
        MODULE + "import",
        MODULE + "param p type=i64",
        MODULE + "field a type=i32 offset=0",
        MODULE + "end",
        MODULE + FUNCTION,
        MODULE + FUNCTION + "end extra",
        MODULE + FUNCTION + "struct X size=8 align=8",
        MODULE + "import f return=i32 error=no_failure cc=fast\nend",
        MODULE + "import f return=i32 error=no_failure isolate=bad\nend",
        MODULE + "import f return=i32 return=i64 error=no_failure\nend",
        MODULE + "import f return= error=no_failure\nend",
        MODULE + "import f foo\nend",
        MODULE + "import f error=no_failure\nend",
        MODULE + FUNCTION + "param p type=ptr<i8> nullable=maybe\nend",
        MODULE + FUNCTION + "param p type=ptr<\nend",
        MODULE + FUNCTION + "param p type=ptr<i8> nonnull=true\nend",
        MODULE + FUNCTION + "param p type=ptr<i8> nonnull=true@bogus\nend",
        MODULE + FUNCTION + "param p type=i64 foo=true@explicit_contract\nend",
        MODULE + "opaque X\nopaque X",
        MODULE + "opaque i32",
        MODULE + "struct X size=8 align=8\nend",
        MODULE + "struct X size=8 align=8 typo=1\nfield a type=i64 offset=0\nend",
        VALID.replace("target=aarch64-apple-darwin", "target=unknown"),
        VALID.replace("target=aarch64-apple-darwin", ""),
        VALID.replace("sample target", "sample abi=0 target"),
        VALID.replace("sample", "class"),
        VALID.replace("value type", "_reserved type"),
        VALID.replace("ptr<i64>", "ptr<Unknown>"),
        VALID.replace("ptr<i64>", "void"),
        VALID.replace("ptr<i64>", "ptr<ptr<Unknown>>"),
        VALID.replace("return=i64", "return=Record"),
        MODULE + FUNCTION + PARAM + PARAM + "end",
        VALID + FUNCTION + PARAM + "end",
        VALID + "import other symbol=inspect return=i64 error=no_failure\nend",
        VALID
            .replace("access=read", "access=read nonnull=true@explicit_contract")
            .replace("nullable=false", "nullable=true"),
        VALID.replace(
            "access=read",
            "access=read readonly=true@explicit_contract writeonly=true@explicit_contract"),
        VALID.replace("access=read", "access=write readonly=true@explicit_contract"),
        VALID.replace("access=read", "access=read writeonly=true@explicit_contract"),
        VALID.replace("access=read", "access=read align=3@explicit_contract"),
        VALID.replace("access=read", "access=read align=0@explicit_contract"),
        VALID.replace("access=read", "access=read dereferenceable=0@explicit_contract"),
        VALID
            .replace("access=read", "access=read dereferenceable=8@explicit_contract")
            .replace("nullable=false", "nullable=true"),
        VALID.replace("access=read", "access=read nonnull=maybe@explicit_contract"),
        VALID.replace(
            "ownership=borrowed",
            "ownership=transferred_to_callee nocapture=true@explicit_contract"),
        VALID.replace("ownership=borrowed", "ownership=transferred_to_caller"),
        VALID.replace("ptr<i64>", "i64"),
        MODULE + FUNCTION + "param x type=i64 nonnull=true@explicit_contract\nend",
        MODULE + FUNCTION + "param x type=i64 ownership=borrowed\nend",
        VALID.replace(
            "error=no_failure",
            "error=no_failure noreturn=true@explicit_contract willreturn=true@explicit_contract"),
        VALID.replace("error=no_failure", "error=no_failure noreturn=true@explicit_contract"),
        VALID.replace("error=no_failure", "error=abort"),
        VALID.replace("error=no_failure", "error=no_failure java=fixture.Ops.inspect"),
        VALID.replace("error=no_failure", "error=no_failure isolate=thread"),
        VALID.replace("error=no_failure", "error=no_failure sentinel=-1"),
        VALID.replace("import inspect", "export inspect"),
        MODULE + "export f return=i64 error=no_failure isolate=thread java=fixture.Ops.f\nend",
        MODULE + "export f return=i64 error=abort isolate=thread java=invalid\nend",
        MODULE
            + "export f return=i64 error=abort isolate=thread java=fixture.Ops.f"
            + " noreturn=true@explicit_contract\n"
            + "end",
        MODULE + "export f return=i64 error=null_sentinel isolate=thread java=fixture.Ops.f\nend",
        MODULE
            + "export f return=f64 error=integer_sentinel sentinel=-1 isolate=thread"
            + " java=fixture.Ops.f\n"
            + "end",
        MODULE
            + "export f return=i32 error=integer_sentinel isolate=thread java=fixture.Ops.f\nend",
        MODULE
            + "export f return=i32 error=integer_sentinel sentinel=2147483648 isolate=thread"
            + " java=fixture.Ops.f\n"
            + "end",
        MODULE
            + "export f return=i32 error=integer_sentinel sentinel=bad isolate=thread"
            + " java=fixture.Ops.f\n"
            + "end",
        MODULE
            + "struct X size=8 align=8\nfield a type=i64 offset=0\nfield b type=i32 offset=4\nend",
        MODULE + "struct X size=8 align=4\nfield a type=i64 offset=0\nend",
        MODULE + "struct X size=4 align=8\nfield a type=i64 offset=0\nend",
        MODULE + "struct X size=8 align=8\nfield a type=i32 offset=1\nend",
        MODULE
            + "struct X size=8 align=4\nfield a type=i32 offset=0\nfield a type=i32 offset=4\nend",
        MODULE + "struct X size=0 align=8\nfield a type=i64 offset=0\nend",
        MODULE + "opaque X\nstruct X size=8 align=8\nfield a type=i64 offset=0\nend",
        MODULE + "struct i64 size=8 align=8\nfield a type=i64 offset=0\nend",
        MODULE + "import box return=i64 error=no_failure\nend",
        MODULE + "import f symbol=box return=i64 error=no_failure\nend",
        MODULE + "import f return=i64 error=no_failure\nparam gen type=i64\nend",
        MODULE + "opaque box",
        MODULE + "struct unsized size=8 align=8\nfield a type=i64 offset=0\nend",
        MODULE + "callback become return=void\nend");
  }

  static Stream<Arguments> rustReservedKeywordsAndPositions() {
    return Stream.of("box", "become", "override", "priv", "typeof", "unsized", "gen")
        .flatMap(keyword ->
            Stream.of(
                Arguments.of(
                    "module-name", keyword, "module " + keyword + " target=aarch64-apple-darwin\n"),
                Arguments.of(
                    "function-name",
                    keyword,
                    MODULE + "import " + keyword + " return=i64 error=no_failure\n" + "end\n"),
                Arguments.of(
                    "function-symbol",
                    keyword,
                    MODULE
                        + "import f symbol="
                        + keyword
                        + " return=i64 error=no_failure\n"
                        + "end\n"),
                Arguments.of(
                    "parameter-name",
                    keyword,
                    MODULE
                        + "import f return=i64 error=no_failure\n"
                        + "param "
                        + keyword
                        + " type=i64\n"
                        + "end\n"),
                Arguments.of("opaque-name", keyword, MODULE + "opaque " + keyword + "\n"),
                Arguments.of(
                    "struct-name",
                    keyword,
                    MODULE
                        + "struct "
                        + keyword
                        + " size=8 align=8\n"
                        + "field a type=i64 offset=0\n"
                        + "end\n"),
                Arguments.of(
                    "callback-name",
                    keyword,
                    MODULE + "callback " + keyword + " return=void\n" + "end\n"),
                Arguments.of(
                    "field-name",
                    keyword,
                    MODULE
                        + "struct S size=8 align=8\n"
                        + "field "
                        + keyword
                        + " type=i64 offset=0\n"
                        + "end\n"),
                Arguments.of(
                    "java-package-segment",
                    keyword,
                    MODULE.replace(
                            "module sample target=aarch64-apple-darwin",
                            "module sample java_package=fixture."
                                + keyword
                                + " target=aarch64-apple-darwin")
                        + "import f return=i64 error=no_failure\n"
                        + "end\n"),
                Arguments.of(
                    "java-class-name",
                    keyword,
                    MODULE.replace(
                            "module sample target=aarch64-apple-darwin",
                            "module sample java_class=" + keyword + " target=aarch64-apple-darwin")
                        + "import f return=i64 error=no_failure\n"
                        + "end\n")));
  }

  @ParameterizedTest(name = "{0}={1}")
  @MethodSource("rustReservedKeywordsAndPositions")
  void rejectsRustReservedKeywordsAsIdentifiers(String position, String keyword, String source) {
    var error = assertThrows(IllegalArgumentException.class, () -> generate(source), source);
    assertEquals("invalid or reserved identifier: " + keyword, error.getMessage());
  }

  @Test
  void validRustMethodNamesAreStillAccepted() {
    String source = MODULE + "import resize return=i64 error=no_failure\n" + "end\n";
    var output = assertDoesNotThrow(() -> generate(source), source);
    assertTrue(output.get("seam.rs").contains("pub fn resize() -> i64"));
  }

  @ParameterizedTest
  @MethodSource("invalidInputs")
  void rejectsInvalidInput(String source) {
    assertThrows(IllegalArgumentException.class, () -> generate(source), source);
  }

  @Test
  void relaxedNeverPermitsUnprovenNoAlias() {
    var module = Dsl.parse(VALID.replace("access=read", "access=read noalias=true@heuristic"));
    assertThrows(IllegalArgumentException.class, () -> Generator.generate(module, false));
  }

  static final String PROPERTIES =
      "META-INF/native-image/dev.elide.myna/dev.elide.myna.generated.MynaNative/"
          + "native-image.properties";

  @Test
  void nativeImageClassAlwaysHasTheGeneratedContextAndConstantAccessors() {
    String constants =
        MODULE
            + "const ANALYSIS_MAGIC type=u32 value=0x3141514b\n"
            + "const LEVEL type=u8 value=200\n"
            + "const DEPTH type=i16 value=-2\n"
            + "const LIMIT type=u64 value=0xffffffffffffffff\n"
            + "const FLOOR type=i64 value=-9223372036854775808\n"
            + "const SPAN type=usize value=4096\n"
            + FUNCTION
            + PARAM
            + "end\n";
    String java = generate(constants).get("MynaNative.java");
    assertTrue(java.contains("import org.graalvm.nativeimage.c.CContext;\n"), java);
    assertTrue(java.contains("import org.graalvm.nativeimage.c.constant.CConstant;\n"), java);
    assertTrue(
        java.contains("import org.graalvm.nativeimage.c.struct.AllowNarrowingCast;\n"), java);
    assertTrue(java.contains("\n@CContext(MynaNativeDirectives.class)\npublic final class"), java);
    // The seam.h check: a macro only this generation's header defines, resolved by the C query.
    var outputs = generate(constants);
    String check = "SAMPLE_SEAM_ABI_" + outputs.get("seam.abi").strip();
    assertTrue(
        java.contains("    @CConstant(\"" + check + "\") static native int seamHeaderCheck();\n"),
        java);
    assertTrue(
        outputs.get("seam.h").contains("\n#define " + check + " 1\n"), outputs.get("seam.h"));
    assertTrue(
        outputs.get("seam.h").contains("\n#define SAMPLE_SEAM_ABI \""), outputs.get("seam.h"));
    // Nothing calls it: wrappers, leaf or not, are unchanged by the check.
    assertEquals(1, java.split("seamHeaderCheck", -1).length - 1, java);
    // Each const reads its macro from seam.h; SVM replaces the call with the value at parse time.
    assertTrue(
        java.contains(
            "    @CConstant(\"SAMPLE_ANALYSIS_MAGIC\") public static native int"
                + " analysisMagic();\n"),
        java);
    // A narrow const's macro has type int in C (UINT8_C(200) is 200), so SVM must narrow it.
    assertTrue(
        java.contains(
            "    @CConstant(\"SAMPLE_LEVEL\") @AllowNarrowingCast public static native byte"
                + " level();\n"),
        java);
    assertTrue(
        java.contains(
            "    @CConstant(\"SAMPLE_DEPTH\") @AllowNarrowingCast public static native short"
                + " depth();\n"),
        java);
    assertTrue(
        java.contains("    @CConstant(\"SAMPLE_LIMIT\") public static native long limit();\n"),
        java);
    assertTrue(
        java.contains("    @CConstant(\"SAMPLE_FLOOR\") public static native long floor();\n"),
        java);
    assertTrue(
        java.contains("    @CConstant(\"SAMPLE_SPAN\") public static native long span();\n"), java);
    // Without constants or a consumer context the class still names the generated context and
    // checks the header; nothing needs narrowing.
    String plain = generate(VALID).get("MynaNative.java");
    assertTrue(
        plain.contains("\n@CContext(MynaNativeDirectives.class)\npublic final class"), plain);
    assertTrue(plain.contains("static native int seamHeaderCheck();"), plain);
    assertFalse(plain.contains("seamHeaderAbi"), plain);
    assertFalse(plain.contains("AllowNarrowingCast"), plain);
    // A consumer context is delegated to, never the annotation value.
    String bemo =
        generate(MODULE.replace("\n", " java_package=a.b c_context=a.b.Ctx\n") + FUNCTION + "end\n")
            .get("MynaNative.java");
    assertTrue(bemo.contains("\n@CContext(MynaNativeDirectives.class)\npublic final class"), bemo);
    assertFalse(bemo.contains("a.b.Ctx"), bemo);
  }

  /** The string constants of a generated directives class, decoded, in declaration order. */
  static List<String> headerPieces(String directives) {
    var pieces = new ArrayList<String>();
    var declaration =
        java.util.regex.Pattern.compile(
            "private static final String HEADER_(\\d+) =(.*?);\n", java.util.regex.Pattern.DOTALL);
    var literal = java.util.regex.Pattern.compile("\"((?:[^\"\\\\]|\\\\.)*)\"");
    var matcher = declaration.matcher(directives);
    while (matcher.find()) {
      assertEquals(String.valueOf(pieces.size()), matcher.group(1));
      var piece = new StringBuilder();
      var parts = literal.matcher(matcher.group(2));
      while (parts.find()) piece.append(unescape(parts.group(1)));
      pieces.add(piece.toString());
    }
    return pieces;
  }

  static String unescape(String body) {
    var out = new StringBuilder();
    for (int i = 0; i < body.length(); i++) {
      char c = body.charAt(i);
      if (c != '\\') {
        out.append(c);
        continue;
      }
      char e = body.charAt(++i);
      switch (e) {
        case 'n' -> out.append('\n');
        case 'r' -> out.append('\r');
        case 't' -> out.append('\t');
        case '"', '\\' -> out.append(e);
        case 'u' -> {
          out.append((char) Integer.parseInt(body.substring(i + 1, i + 5), 16));
          i += 4;
        }
        default -> {
          int end = i;
          while (end < body.length()
              && end < i + 3
              && body.charAt(end) >= '0'
              && body.charAt(end) <= '7') end++;
          assertTrue(end > i, "unknown escape \\" + e);
          out.append((char) Integer.parseInt(body.substring(i, end), 8));
          i = end - 1;
        }
      }
    }
    return out.toString();
  }

  @Test
  void directivesSupplyTheHeaderAndAreNoFeature() {
    var output = generate(VALID);
    String directives = output.get("MynaNativeDirectives.java");
    assertNotNull(directives, output.keySet().toString());
    // Everything qualified, so no declared or consumer name can shadow an import.
    assertFalse(directives.contains("\nimport "), directives);
    assertTrue(
        directives.contains(
            "public final class MynaNativeDirectives implements"
                + " org.graalvm.nativeimage.c.CContext.Directives {\n"),
        directives);
    assertTrue(directives.contains("    public MynaNativeDirectives() {}\n"), directives);
    assertTrue(directives.endsWith("    }\n}\n"), directives);
    assertEquals(List.of(output.get("seam.h")), headerPieces(directives));
    // Written once per builder JVM into a directory the builder removes when it exits.
    assertTrue(directives.contains("createTempDirectory(\"myna-seam\")"), directives);
    assertTrue(directives.contains("Runtime.getRuntime().addShutdownHook("), directives);
    assertTrue(
        directives.contains("        return java.util.List.of(Header.INCLUDE);\n"), directives);
    // The check is the bindings' fingerprinted @CConstant, not a hosted Feature: nothing here reads
    // the bindings, and an image without the Native Image class never touches this class.
    for (String gone :
        List.of("Feature", "beforeAnalysis", "ABI_FINGERPRINT", "seamHeaderAbi", "MynaNative\""))
      assertFalse(directives.contains(gone), gone);
    // Without a consumer context nothing is delegated.
    assertFalse(directives.contains("Directives context ="), directives);
    assertFalse(directives.contains("return context."), directives);
    // The check needs no flag: the generated properties register no Feature.
    assertFalse(output.get(PROPERTIES).contains("--features"), output.get(PROPERTIES));
    assertFalse(output.get(PROPERTIES).contains("Directives"), output.get(PROPERTIES));
  }

  @Test
  void directivesDelegateToTheConsumerContext() throws Exception {
    var output = generate(Files.readString(Path.of("tests/bemo/bemo.seam")));
    String directives = output.get("BemoImportsDirectives.java");
    assertTrue(
        directives.contains(
            "    private final org.graalvm.nativeimage.c.CContext.Directives context =\n"
                + "        new fixture.bemo.BemoDirectives();\n"),
        directives);
    for (String method :
        List.of(
            "isInConfiguration",
            "getMacroDefinitions",
            "getOptions",
            "getLibraries",
            "getLibraryPaths"))
      assertTrue(directives.contains("        return context." + method + "();\n"), method);
    // The consumer's headers first, then seam.h.
    assertTrue(
        directives.contains(
            "        var headers = new java.util.ArrayList<String>(context.getHeaderFiles());\n"
                + "        headers.add(Header.INCLUDE);\n"
                + "        return headers;\n"),
        directives);
    assertEquals(List.of(output.get("seam.h")), headerPieces(directives));
  }

  @Test
  void directivesSplitLargeHeaders() {
    var source = new StringBuilder(MODULE);
    for (int i = 0; i < 160; i++) {
      source.append("struct Record").append(i).append(" size=64 align=8\n");
      for (int f = 0; f < 8; f++)
        source
            .append("field value")
            .append(f)
            .append(" type=i64 offset=")
            .append(f * 8)
            .append('\n');
      source.append("end\n");
    }
    var output = generate(source.toString());
    String header = output.get("seam.h");
    assertTrue(header.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 64 * 1024);
    var pieces = headerPieces(output.get("MynaNativeDirectives.java"));
    assertTrue(pieces.size() >= 4, String.valueOf(pieces.size()));
    for (String piece : pieces)
      assertTrue(
          piece.getBytes(java.nio.charset.StandardCharsets.UTF_8).length <= 16 * 1024,
          String.valueOf(piece.length()));
    assertEquals(header, String.join("", pieces));
  }

  @Test
  void headerPiecesKeepCharactersWholeAndLiteralsSafe() {
    String smile = new String(Character.toChars(0x1F600));
    String line = smile.repeat(5000) + "\n"; // 20000 UTF-8 bytes in one line
    String text = "a\"b\\c\\u000a\r\t\u0001\u007fé\n" + line + "tail";
    var pieces = dev.elide.myna.backend.NativeImageDirectives.pieces(text, 16 * 1024);
    assertEquals(text, String.join("", pieces));
    for (String piece : pieces) {
      assertTrue(piece.getBytes(java.nio.charset.StandardCharsets.UTF_8).length <= 16 * 1024);
      assertFalse(Character.isLowSurrogate(piece.charAt(0)), "split inside a surrogate pair");
      assertFalse(Character.isHighSurrogate(piece.charAt(piece.length() - 1)));
    }
    String literal = dev.elide.myna.backend.NativeImageDirectives.literal(text);
    assertTrue(literal.chars().allMatch(c -> c >= 0x20 && c < 0x7f), literal);
    assertEquals(text, unescape(literal.substring(1, literal.length() - 1)));
    // javac translates unicode escapes (a `u` after an odd run of backslashes) before lexing; none
    // may spell a quote, a backslash or a line terminator.
    var escapes = new ArrayList<String>();
    for (int i = 0, run = 0; i < literal.length(); i++) {
      char c = literal.charAt(i);
      if (c == 'u' && run % 2 == 1) escapes.add(literal.substring(i + 1, i + 5));
      run = c == '\\' ? run + 1 : 0;
    }
    assertEquals(List.of("00e9", "d83d", "de00"), escapes.subList(0, 3));
    for (String unsafe : List.of("000a", "000d", "0022", "005c"))
      assertFalse(escapes.contains(unsafe), unsafe);
    assertEquals(List.of(""), dev.elide.myna.backend.NativeImageDirectives.pieces("", 16));
  }

  /**
   * {@code enum=}/{@code flags=} references add typed forms beside the raw bindings and never
   * change a binding's transition: the leaf interfaces, symbols and {@code @CFunction} declarations
   * are the same with and without them, in the Native Image class, and so is the AOT FFM and
   * Truffle leaf choice.
   */
  @Test
  void enumReferencesNeverChangeTransition() throws Exception {
    String typed = Files.readString(Path.of("examples/constants.seam"));
    String raw = typed.replaceAll("(?m)^ *when .*\n", "").replaceAll(" (enum|flags)=[A-Za-z]+", "");
    assertNotEquals(typed, raw);
    var withRefs = generate(typed);
    var without = generate(raw);
    for (Function f : Dsl.parse(typed).functions())
      assertEquals(
          Seam.nativeLeaf(f),
          Seam.nativeLeaf(
              Dsl.parse(raw).functions().stream()
                  .filter(g -> g.name().equals(f.name()))
                  .findFirst()
                  .orElseThrow()),
          f.name());
    java.util.function.Function<String, List<String>> transitions =
        text ->
            text.lines()
                .filter(line ->
                    line.contains("CFunction")
                        || line.contains("Transition")
                        || line.contains("_Leaf")
                        || line.contains("forSymbol")
                        || line.contains("Uninterruptible"))
                .toList();
    assertEquals(
        transitions.apply(without.get("Consts.java")),
        transitions.apply(withRefs.get("Consts.java")));
    assertTrue(withRefs.get("Consts.java").contains("statusEnum("));
    for (String file : List.of("ConstsFFM.java", "ConstsTruffle.java"))
      assertEquals(
          without.get(file).lines().filter(GeneratorTest::bindingLine).toList(),
          withRefs.get(file).lines().filter(GeneratorTest::bindingLine).toList(),
          file);
  }

  /** Lines that describe a binding, not the descriptor's fingerprint or a comment. */
  private static boolean bindingLine(String line) {
    return !line.strip().startsWith("//") && !line.contains("ABI_FINGERPRINT");
  }
}
