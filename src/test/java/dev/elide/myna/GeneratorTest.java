package dev.elide.myna;

import static org.junit.jupiter.api.Assertions.*;

import dev.elide.myna.frontend.Dsl;
import dev.elide.myna.model.Seam.*;
import dev.elide.myna.serialize.SeamJson;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
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
  void nativeLeafElidesOnlyApprovedImportTransitions() {
    String leaf =
        VALID.replace("error=no_failure", "error=no_failure native_leaf=true@explicit_contract");
    var output = generate(leaf);
    assertTrue(
        output.get("MynaNative.java").contains("transition = CFunction.Transition.NO_TRANSITION"));
    assertFalse(output.get("seam.ll").contains("native_leaf"));
    assertTrue(output.get("seam.json").contains("native_leaf"));
    assertFalse(generate(VALID).get("MynaNative.java").contains("NO_TRANSITION"));
    assertFalse(
        generate(leaf.replace("native_leaf=true", "native_leaf=false"))
            .get("MynaNative.java")
            .contains("NO_TRANSITION"));
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
            "IoNatives.java",
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
    assertTrue(output.get("IoNativesFFM.java").contains("IoNativesFFM.inspect_Binding.HANDLE"));
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
            + "export open symbol=x_open return=i64 java=fixture.Ops.open isolate=thread error=abort"
            + " include=fixture.ImageOnly\n"
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
            "open(IsolateThread isolate_thread, @CConst CCharPointer path, CCharPointerPointer argv,"
                + " VoidPointer any, CIntPointer ints, PointerBase handle)"),
        ni);
    assertTrue(ni.contains("return fixture.Ops.open(path, argv, any, ints, handle);"));
    assertFalse(ni.contains("WordFactory"));
    assertFalse(ni.contains("CFunction"));
    assertTrue(output.get("seam.json").contains("\"include\":\"fixture.ImageOnly\""));
    assertNotEquals(
        output.get("seam.abi"), generate(source.replace(" include=fixture.ImageOnly", "")).get("seam.abi"));
    assertThrows(
        IllegalArgumentException.class,
        () -> generate(source.replace("include=fixture.ImageOnly", "include=ImageOnly")));
    assertThrows(
        IllegalArgumentException.class,
        () -> generate(VALID.replace("error=no_failure", "error=no_failure include=fixture.ImageOnly")));
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
          ll.contains("declare i8 @put(i8 signext %a, i8 zeroext %b, i16 zeroext %c, i1 zeroext %d)"),
          ll);
    }
    var output = generate(source.replace("TARGET", "aarch64-unknown-linux-gnu"));
    assertTrue(output.get("seam.ll").contains("declare i8 @put(i8 %a, i8 %b, i16 %c, i1 %d)"));
    String ni = output.get("MynaNative.java");
    assertTrue(ni.contains("public static byte put(byte a, byte b, short c, boolean d) {"), ni);
    assertTrue(ni.contains("return putNative(a, b & 0xff, c & 0xffff, d);"), ni);
    assertTrue(ni.contains("private static native byte putNative(byte a, int b, int c, boolean d);"), ni);
    assertTrue(ni.contains("public static int get(IsolateThread isolate_thread, short a) {"), ni);
    assertTrue(ni.contains("return fixture.Ops.get(a) & 0xffff;"), ni);
    String rust = output.get("seam.rs");
    assertTrue(rust.contains("pub fn put(a: i8, b: u8, c: u16, d: bool) -> u8;"), rust);
    assertTrue(output.get("seam.h").contains("uint8_t put(int8_t a, uint8_t b, uint16_t c, bool d);"));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            generate(
                source.replace("TARGET", "x86_64-unknown-linux-gnu")
                    + "import putNative return=void error=no_failure\nend\n"));
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
        output.get("seam.rs").contains(
            "pub fn lookup(name: *const core::ffi::c_char, out: *mut usize, delta: isize, tag:"
                + " core::ffi::c_char) -> usize;"),
        output.get("seam.rs"));
    assertTrue(
        output.get("seam.h").contains(
            "size_t lookup(char const * name, size_t * out, ptrdiff_t delta, char tag);"),
        output.get("seam.h"));
    String ni = output.get("MynaNative.java");
    assertTrue(
        ni.contains("long lookup(CCharPointer name, WordPointer out, long delta, byte tag);"), ni);
    assertTrue(ni.contains("import org.graalvm.nativeimage.c.type.WordPointer;"));
    assertTrue(output.get("seam.ll").contains("declare i64 @lookup(ptr %name, ptr %out, i64 %delta, i8 signext %tag)"));
    assertTrue(output.get("seam.rs").contains("pub type LookupFn = unsafe extern \"C\" fn("));
  }

  @Test
  void callbacksAndIsolateThreadsAreValidated() {
    String base =
        MODULE
            + "callback Sink return=i32\n  param isolate type=isolate_thread\n  param n type=i64\nend\n"
            + "export sink return=i32 java=fixture.Ops.sink isolate=thread error=abort callback=Sink\n"
            + "  param n type=i64\nend\n"
            + "import run return=void error=no_failure\n  param cb type=fn<Sink>\n"
            + "  param isolate type=isolate_thread\nend\n";
    var output = generate(base);
    String ni = output.get("MynaNative.java");
    assertTrue(ni.contains("public interface Sink extends CFunctionPointer {"), ni);
    assertTrue(ni.contains("int invoke(IsolateThread isolate, long n);"), ni);
    assertTrue(
        ni.contains("CEntryPointLiteral.create(MynaNative.class, \"sink\", IsolateThread.class, long.class)"),
        ni);
    assertTrue(output.get("seam.h").contains("typedef int32_t (*Sink)(void * isolate, int64_t n);"));
    assertTrue(output.get("seam.json").contains("\"callback\":\"Sink\""));
    for (String bad :
        List.of(
            base.replace("callback=Sink", "callback=Missing"),
            base.replace("export sink return=i32", "export sink return=i64"),
            base.replace("param cb type=fn<Sink>", "param cb type=fn<Missing>"),
            base.replace("param n type=i64\nend\nexport", "param n type=u8\nend\nexport"),
            base.replace("import run return=void", "import run return=isolate_thread"),
            base.replace("import run return=void error=no_failure", "import run return=void error=no_failure callback=Sink"),
            base + "struct Holder size=8 align=8\n  field t type=isolate_thread offset=0\nend\n",
            base + "opaque SinkFn\n",
            base.replace("param cb type=fn<Sink>", "param cb type=ptr<fn<Sink>>")))
      assertThrows(IllegalArgumentException.class, () -> generate(bad), bad);
  }

  @Test
  void byValueRecordsLowerOnlyAtTheNativeImageBoundary() {
    String base =
        MODULE
            + "opaque Handle\n"
            + "struct Pt size=8 align=4\n  field x type=i32 offset=0\n  field y type=i32 offset=4\nend\n"
            + "import flip symbol=pt_flip return=Pt error=no_failure\n  param p type=Pt\n  param k type=i32\nend\n"
            + "export norm symbol=pt_norm return=i32 java=fixture.Ops.norm isolate=thread error=abort\n"
            + "  param p type=Pt\nend\n";
    var output = generate(base);
    String ni = output.get("MynaNative.java");
    assertTrue(ni.contains("@CFunction(value = \"pt_flip_myna_ref\")"), ni);
    assertTrue(ni.contains("public static native void flip(PointerBase p, int k, PointerBase result);"), ni);
    assertTrue(ni.contains("@CEntryPoint(name = \"pt_norm_myna_ref\")"), ni);
    assertTrue(ni.contains("return fixture.Ops.norm(p);"), ni);
    assertTrue(ni.contains("public static final int SIZE = 8;"), ni);
    assertTrue(ni.contains("((Pointer) record).readInt(4)"), ni);
    String rust = output.get("seam.rs");
    assertTrue(rust.contains("pub fn flip(p: Pt, k: i32) -> Pt;"), rust);
    assertTrue(rust.contains("pub fn normRef(isolate_thread: *mut core::ffi::c_void, p: *const Pt) -> i32;"), rust);
    assertTrue(rust.contains("pub unsafe fn norm(isolate_thread: *mut core::ffi::c_void, p: Pt) -> i32 {"), rust);
    assertTrue(rust.contains("#[derive(Clone, Copy)]"));
    assertTrue(rust.contains("pub unsafe extern \"C\" fn pt_flip_myna_ref(p: *const $($seam)::+::Pt, k: i32, result: *mut $($seam)::+::Pt)"), rust);
    assertTrue(rust.contains("unsafe { result.write(pt_flip(p.read(), k)) }"), rust);
    String ll = output.get("seam.ll");
    assertFalse(ll.contains("@pt_flip("), ll);
    assertTrue(
        ll.contains(
            "declare void @pt_flip_myna_ref(ptr align 4 dereferenceable(8) captures(none) nonnull readonly %p, i32 %k, ptr align 4 dereferenceable(8) captures(none) nonnull writeonly %result)"),
        ll);
    String h = output.get("seam.h");
    assertTrue(h.contains("struct Pt pt_flip(struct Pt p, int32_t k);"), h);
    assertTrue(h.contains("static inline int32_t pt_norm(void * isolate_thread, struct Pt p) {"), h);
    assertFalse(output.get("MynaFFM.java").contains("public static Pt flip"));
    assertTrue(output.get("MynaFFM.java").contains("flip(SegmentAllocator allocator, MemorySegment p, int k)"));
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
  void goldenOutputs() throws Exception {
    var actual = generate(Files.readString(Path.of("examples/buffer.seam")));
    for (var output : actual.entrySet())
      assertEquals(
          Files.readString(Path.of("tests/golden", output.getKey())),
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
  void abiFingerprintChangesWithTargetSignatureAndContracts() {
    String original = generate(VALID).get("seam.abi");
    for (String change :
        List.of(
            VALID.replace("aarch64", "x86_64"),
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
    assertTrue(ffm.contains("return (long) MynaFFM.inspect_Binding.HANDLE.invokeExact(value)"));
    assertFalse(ffm.contains("invokeWithArguments"));
    String feature = output.get("MynaForeignFeature.java");
    assertTrue(feature.contains("duringSetup"));
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
  void scalarFunctionCarriers() {
    for (String type : List.of("i32", "u32", "i64", "u64", "f32", "f64")) {
      assertEquals(
          8,
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
                    + "import halt return=void error=no_failure noreturn=true@explicit_contract"
                    + " nofree=true@explicit_contract\n"
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
        MODULE + "struct i64 size=8 align=8\nfield a type=i64 offset=0\nend");
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
}
