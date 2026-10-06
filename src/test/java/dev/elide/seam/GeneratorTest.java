package dev.elide.seam;

import static org.junit.jupiter.api.Assertions.*;

import dev.elide.seam.frontend.Dsl;
import dev.elide.seam.model.Seam.*;
import dev.elide.seam.serialize.SeamJson;
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
        output.get("SeamNative.java").contains("transition = CFunction.Transition.NO_TRANSITION"));
    assertFalse(output.get("seam.ll").contains("native_leaf"));
    assertTrue(output.get("seam.json").contains("native_leaf"));
    assertFalse(generate(VALID).get("SeamNative.java").contains("NO_TRANSITION"));
    assertFalse(
        generate(leaf.replace("native_leaf=true", "native_leaf=false"))
            .get("SeamNative.java")
            .contains("NO_TRANSITION"));
    String heuristic = leaf.replace("explicit_contract", "heuristic");
    assertThrows(IllegalArgumentException.class, () -> generate(heuristic));
    assertFalse(
        Generator.generate(Dsl.parse(heuristic), false)
            .get("SeamNative.java")
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
    String ffm = output.get("SeamFFM.java");
    assertTrue(ffm.contains("static final MethodHandle HANDLE"));
    assertTrue(ffm.contains("public static long inspect(MemorySegment value) throws Throwable"));
    assertTrue(ffm.contains("return (long) SeamFFM.inspect_Binding.HANDLE.invokeExact(value)"));
    assertFalse(ffm.contains("invokeWithArguments"));
    String feature = output.get("SeamForeignFeature.java");
    assertTrue(feature.contains("duringSetup"));
    assertTrue(
        feature.contains("RuntimeForeignAccess.registerForDowncall(SeamFFM.inspect_DESCRIPTOR)"));
    assertTrue(
        feature.contains(
            "initializeAtRunTime(\"dev.elide.seam.generated.SeamFFM$inspect_Binding\")"));
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
            .get("SeamFFM.java")
            .contains("of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG)"));
    assertTrue(output.get("SeamNative.java").contains("fixture.Ops.add(amount)"));
    assertTrue(output.get("SeamNative.java").contains("catch (Throwable failure)"));
    assertTrue(output.get("SeamNative.java").contains("return -1L;"));
  }

  @Test
  void errorAdaptations() {
    var nullResult =
        generate(
            MODULE
                + "export create return=ptr<void> java=fixture.Ops.create isolate=thread"
                + " error=null_sentinel\n"
                + "end\n");
    assertTrue(nullResult.get("SeamNative.java").contains("WordFactory.nullPointer()"));
    for (String returns : List.of("void", "i32")) {
      var abort =
          generate(
              MODULE
                  + "export perform return="
                  + returns
                  + " java=fixture.Ops.perform isolate=thread error=abort\nend\n");
      assertFalse(abort.get("SeamNative.java").contains("catch (Throwable"));
    }
    var intResult =
        generate(
            MODULE
                + "export create return=i32 java=fixture.Ops.create isolate=thread"
                + " error=integer_sentinel sentinel=-2147483648\n"
                + "end\n");
    assertTrue(intResult.get("SeamNative.java").contains("return -2147483648;"));
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
    assertTrue(output.get("SeamFFM.java").contains("MemoryLayout.paddingLayout(8)"));
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
    assertTrue(result.get("SeamNative.java").contains("return 9;"));
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
        VALID.replace("return=i64", "return=i8"),
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
