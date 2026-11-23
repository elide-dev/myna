// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package dev.elide.myna;

import static org.junit.jupiter.api.Assertions.*;

import dev.elide.myna.frontend.Dsl;
import dev.elide.myna.serialize.Fingerprint;
import java.util.List;
import org.junit.jupiter.api.Test;

class BindingAbiTest {
  private static final String MODULE = "module bindings target=aarch64-apple-darwin\n";
  private static final String CALL =
      "import read symbol=native_read return=i64 error=no_failure\nparam value type=i64\nend\n";

  @Test
  void callbackTypedefsFollowDependencies() {
    String outer = "callback Alpha return=i32\nparam f type=fn<Beta>\nend\n";
    String inner = "callback Beta return=i32\nend\n";
    var output = Generator.generate(Dsl.parse(MODULE + outer + inner), true).get("seam.h");
    assertTrue(output.indexOf("(*Beta)") >= 0);
    assertTrue(output.indexOf("(*Alpha)") > output.indexOf("(*Beta)"));
    for (String cycle :
        List.of(
            outer.replace("fn<Beta>", "fn<Alpha>"),
            outer + inner.replace("end", "param g type=fn<Alpha>\nend")))
      assertTrue(
          assertThrows(
                  IllegalArgumentException.class,
                  () -> Generator.generate(Dsl.parse(MODULE + cycle), true))
              .getMessage()
              .contains("cyclic callback"));
  }

  @Test
  void variadicSelectorPreventsSafeLifecycleAndBorrowWrappers() {
    String call =
        "import configure return=void variadic_after=op error=no_failure\n"
            + "param op type=i32\nparam value type=i32\nend\n";
    String resource = "opaque Ticket\nresource Ticket representation=native threading=shared ";
    for (String contract :
        List.of(
            "borrow configure value length=op lifetime=call\n",
            resource + "create=configure destroy=release\n",
            resource + "create=acquire destroy=configure\n")) {
      var invalid = Dsl.parse(MODULE + contract + call);
      var error =
          assertThrows(
              IllegalArgumentException.class,
              () -> Generator.generate(invalid, true, new Fingerprint(invalid), true));
      assertEquals(
          "variadic imports cannot back safe ownership or borrow wrappers", error.getMessage());
    }
    for (String source :
        List.of(
            MODULE.replace(" target=", " rust_implementation=trait target=") + call,
            MODULE + call.replace("import configure", "export configure"))) {
      var invalid = Dsl.parse(source);
      var error =
          assertThrows(
              IllegalArgumentException.class,
              () -> Generator.generate(invalid, true, new Fingerprint(invalid), true));
      assertTrue(
          error.getMessage().contains("variadic_after requires a native import"),
          error.getMessage());
    }
  }

  @Test
  void sharedVariadicSymbolsRequireCompatibleFixedCarriers() {
    String header = MODULE + "enum Mode type=i32\nvalue active = 1\nend\n";
    String first =
        "import first symbol=dispatch return=void variadic_after=op error=no_failure\n"
            + "param buffer type=ptr<u8> access=read\n"
            + "param op type=i32 enum=Mode\n"
            + "param value type=i64\n"
            + "end\n";
    String second =
        first.replace("import first", "import other").replace("value type=i64", "value type=f64");
    var module = Dsl.parse(header + first + second.replace("param buffer", "param data"));
    String rust = Generator.generate(module, true, new Fingerprint(module), true).get("seam.rs");
    assertTrue(rust.contains("OtherFn = unsafe extern \"C\" fn(*const u8, Mode, ...)"));
    for (String conflict :
        List.of(
            second.replace("access=read", "access=write"),
            second.replace(" enum=Mode", ""),
            second.replace("after=op", "after=buffer").replace(" enum=Mode", ""),
            second.replace("return=void", "return=i32"),
            second.replace("variadic_after=op", ""))) {
      var invalid = Dsl.parse(header + first + conflict);
      var error =
          assertThrows(
              IllegalArgumentException.class,
              () -> Generator.generate(invalid, true, new Fingerprint(invalid), true));
      assertTrue(
          error.getMessage().contains("duplicate linker symbol: dispatch"), error.getMessage());
    }
  }

  @Test
  void variadicBoundaryIsFingerprintedAndRequiresRustGeneration() {
    String declaration =
        "import configure return=i32 variadic_after=op error=no_failure\n"
            + "param op type=i32\nparam value type=i32\nparam result type=ptr<i32>\nend\n";
    var module = Dsl.parse(MODULE + declaration);
    var abi = new Fingerprint(module);
    var generated = Generator.generate(module, true, abi, true);
    assertTrue(abi.json().contains("\"variadicAfter\":\"op\""));
    assertNotEquals(
        abi.value(),
        new Fingerprint(Dsl.parse(MODULE + declaration.replace("after=op", "after=value")))
            .value());
    assertThrows(IllegalArgumentException.class, () -> Generator.generate(module, true));
    assertTrue(generated.get("seam.rs").contains("ConfigureFn = unsafe extern \"C\" fn(i32, ...)"));
    assertFalse(generated.containsKey("seam.ll"));
    for (String bad :
        List.of(
            declaration.replace("after=op", "after=missing"),
            declaration.replace("after=op", "after=result"),
            declaration.replace("value type=i32", "value type=f32"),
            declaration
                + declaration
                    .replace("import configure", "import other symbol=configure")
                    .replace("op type=i32", "op type=i64"))) {
      var invalid = Dsl.parse(MODULE + bad);
      assertThrows(
          IllegalArgumentException.class,
          () -> Generator.generate(invalid, true, new Fingerprint(invalid), true));
    }
  }

  @Test
  void defaultIsInlineableAndOptOutRetainsNormalTransitions() {
    var fast = Generator.generate(Dsl.parse(MODULE + CALL), true);
    String source = fast.get("MynaNative.java");
    assertTrue(source.contains("@AlwaysInline"));
    assertTrue(source.contains("mayBeInlined = true"));
    assertTrue(
        source.contains(
            "@InvokeCFunctionPointer(transition = CFunction.Transition.NO_TRANSITION)"));
    assertTrue(source.contains("CGlobalDataFactory.forSymbol(\"native_read\")"));
    assertTrue(source.contains("return MynaSymbols.read.get().invoke(value);"));
    assertTrue(source.contains("public static read_Leaf readLeafPointer()"));
    assertFalse(source.contains("@CFunction(value"));
    assertTrue(fast.get("seam.json").contains("\"schemaVersion\": 3"));
    assertTrue(fast.get("seam.json").contains("\"bindingAbiVersion\": 2"));
    assertTrue(fast.get("seam.h").contains("BINDINGS_BINDING_ABI_VERSION 2"));
    assertTrue(fast.get("seam.rs").contains("BINDING_ABI_VERSION: u32 = 2"));
    var normal =
        Generator.generate(
            Dsl.parse(
                (MODULE + CALL)
                    .replace(
                        "error=no_failure",
                        "error=no_failure native_leaf=false@explicit_contract")),
            true);
    assertTrue(normal.get("MynaNative.java").contains("@CFunction(value = \"native_read\")"));
    assertFalse(normal.get("MynaNative.java").contains("NO_TRANSITION"));
    assertTrue(normal.get("seam.json").contains("\"nativeCallPolicy\":\"TRANSITION\""));
    assertNotEquals(fast.get("seam.abi"), normal.get("seam.abi"));
    // Without leaves only the linker metadata (each import's address symbol) initializes at build
    // time; no runtime address class exists.
    String normalConfig =
        normal.entrySet().stream()
            .filter(e -> e.getKey().endsWith(".properties"))
            .findFirst()
            .orElseThrow()
            .getValue();
    assertTrue(normalConfig.contains("$MynaSymbols"), normalConfig);
    assertFalse(normalConfig.contains("_Binding"), normalConfig);
    assertTrue(
        normal.get("MynaNative.java").contains("public static CFunctionPointer read_address() {"));
  }

  @Test
  void linkerMetadataAndEachRuntimeAddressHaveSeparateInitialization() {
    var output = Generator.generate(Dsl.parse(MODULE + CALL + CALL.replace("read", "other")), true);
    String source = output.get("MynaNative.java");
    assertTrue(source.contains("class read_Binding"));
    assertTrue(source.contains("class other_Binding"));
    assertFalse(source.contains("class MynaBindings"));
    String config =
        output.entrySet().stream()
            .filter(e -> e.getKey().endsWith(".properties"))
            .findFirst()
            .orElseThrow()
            .getValue();
    assertTrue(
        config.contains(
            "--initialize-at-build-time=dev.elide.myna.generated.MynaNative,dev.elide.myna.generated.MynaNative$MynaSymbols"));
    assertTrue(
        config.contains(
            "--initialize-at-run-time=dev.elide.myna.generated.MynaNative$other_Binding,dev.elide.myna.generated.MynaNative$read_Binding"));
    assertTrue(config.contains("JavaArgs = --add-exports="));
  }

  @Test
  void callbackExportRegistersLiteralsAtBuildTimeWithLeaves() {
    String source =
        MODULE
            + "callback Visitor return=i32\n  param isolate type=isolate_thread\n"
            + "  param value type=i64\nend\n"
            + "import read symbol=native_read return=i64 error=no_failure\n"
            + "  param value type=i64\nend\n"
            + "export visit symbol=native_visit return=i32 java=fixture.Ops.visit isolate=thread"
            + " error=abort callback=Visitor\n  param value type=i64\nend\n";
    var output = Generator.generate(Dsl.parse(source), true);
    assertTrue(output.get("MynaNative.java").contains("class Literals"));
    String config =
        output.entrySet().stream()
            .filter(e -> e.getKey().endsWith(".properties"))
            .findFirst()
            .orElseThrow()
            .getValue();
    assertTrue(
        config.contains(
            "--initialize-at-build-time=dev.elide.myna.generated.MynaNative,dev.elide.myna.generated.MynaNative$MynaSymbols,dev.elide.myna.generated.MynaNative$Literals"),
        config);
    assertTrue(
        config.contains(
            "--initialize-at-run-time=dev.elide.myna.generated.MynaNative$read_Binding"),
        config);
    assertTrue(config.contains("JavaArgs = --add-exports="));
    String noCallback =
        MODULE
            + "import read symbol=native_read return=i64 error=no_failure\n"
            + "  param value type=i64\nend\n";
    String noLiterals =
        Generator.generate(Dsl.parse(noCallback), true).entrySet().stream()
            .filter(e -> e.getKey().endsWith(".properties"))
            .findFirst()
            .orElseThrow()
            .getValue();
    assertFalse(noLiterals.contains("$Literals"), noLiterals);
  }

  @Test
  void callbackExportWithoutLeavesInitializesOnlyLiteralsAtBuildTime() {
    String source =
        MODULE
            + "callback Visitor return=i32\n  param isolate type=isolate_thread\n"
            + "  param value type=i64\nend\n"
            + "export visit symbol=native_visit return=i32 java=fixture.Ops.visit isolate=thread"
            + " error=abort callback=Visitor\n  param value type=i64\nend\n"
            + "import slow return=i64 error=no_failure native_leaf=false@explicit_contract"
            + "\nend\n";
    var output = Generator.generate(Dsl.parse(source), true);
    String ni = output.get("MynaNative.java");
    assertTrue(ni.contains("class Literals"), ni);
    // The import's address symbol is linker metadata too; still no runtime address class.
    assertTrue(ni.contains("static final CGlobalData<CFunctionPointer> slow"), ni);
    String config =
        output.entrySet().stream()
            .filter(e -> e.getKey().endsWith(".properties"))
            .findFirst()
            .orElseThrow()
            .getValue();
    assertTrue(
        config.contains("$MynaSymbols,dev.elide.myna.generated.MynaNative$Literals"), config);
    assertFalse(config.contains("_Binding"), config);
    assertFalse(config.contains("--initialize-at-run-time"), config);
  }

  @Test
  void rejectsLeafNameCollisionsAndNonreturningDefault() {
    for (String extra :
        List.of(
            "opaque read_Leaf\n",
            "opaque read_Binding\n",
            "callback read_Leaf return=i64\nend\n",
            "callback read_Binding return=i64\nend\n",
            "opaque MynaSymbols\n",
            "import readLeafPointer return=void error=no_failure\nend\n"))
      assertThrows(
          IllegalArgumentException.class,
          () -> Generator.generate(Dsl.parse(MODULE + CALL + extra), true));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            Generator.generate(
                Dsl.parse(
                    MODULE
                        + "import halt return=void error=no_failure"
                        + " noreturn=true@explicit_contract\n"
                        + "end\n"),
                true));
  }

  @Test
  void optOutStillZeroExtendsNarrowUnsignedArguments() {
    var output =
        Generator.generate(
            Dsl.parse(
                MODULE
                    + "import put return=void error=no_failure"
                    + " native_leaf=false@explicit_contract\n"
                    + "param a type=u8\n"
                    + "param b type=u16\n"
                    + "end\n"),
            true);
    String source = output.get("MynaNative.java");
    assertTrue(source.contains("putNative(a & 0xff, b & 0xffff);"));
    assertTrue(source.contains("private static native void putNative(int a, int b);"));
    String returning =
        Generator.generate(
                Dsl.parse(
                    MODULE
                        + "import put return=u8 error=no_failure"
                        + " native_leaf=false@explicit_contract\n"
                        + "param a type=u8\n"
                        + "param b type=i64\n"
                        + "end\n"),
                true)
            .get("MynaNative.java");
    assertTrue(returning.contains("return putNative(a & 0xff, b);"));
    assertTrue(returning.contains("private static native byte putNative(int a, long b);"));
  }
}
