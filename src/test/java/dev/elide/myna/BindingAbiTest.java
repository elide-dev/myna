// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package dev.elide.myna;

import static org.junit.jupiter.api.Assertions.*;

import dev.elide.myna.frontend.Dsl;
import java.util.List;
import org.junit.jupiter.api.Test;

class BindingAbiTest {
  private static final String MODULE = "module bindings target=aarch64-apple-darwin\n";
  private static final String CALL =
      "import read symbol=native_read return=i64 error=no_failure\nparam value type=i64\nend\n";

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
    assertFalse(
        normal.entrySet().stream()
            .filter(e -> e.getKey().endsWith(".properties"))
            .findFirst()
            .orElseThrow()
            .getValue()
            .contains("Args ="));
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
