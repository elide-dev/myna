package dev.elide.seam;

import static org.junit.jupiter.api.Assertions.*;

import dev.elide.seam.frontend.Dsl;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class NativeConfigurationTest {
  private static String source() throws Exception {
    return Files.readString(Path.of("tests/bemo/bemo.seam"));
  }

  @Test
  void configuresLinkageAndUsesExternalCallbackType() throws Exception {
    var m = Dsl.parse(source());
    var outputs = Generator.generate(m, true);
    String java = outputs.get("BemoImports.java");
    assertTrue(java.contains("@CContext(fixture.bemo.BemoDirectives.class)"));
    assertTrue(java.contains("@CLibrary(value = \"bemofixture\", requireStatic = true)"));
    assertTrue(java.contains("fixture.bemo.HandwrittenSink callback"));
    assertFalse(java.contains("interface Sink"));
    assertTrue(java.contains("fixture.bemo.HandwrittenSink callback(PointerBase record)"));
    assertFalse(java.contains("import org.graalvm.nativeimage.c.function.InvokeCFunctionPointer"));
    assertFalse(java.contains("class Literals"));
    assertTrue(outputs.get("seam.h").contains("Sink"));
    assertTrue(outputs.get("seam.rs").contains("Sink"));
    assertTrue(outputs.get("seam.json").contains("fixture.bemo.HandwrittenSink"));
    assertEquals(m.nativeConfig(), m.withTarget("x86_64-unknown-linux-gnu").nativeConfig());
    assertNotEquals(
        outputs.get("seam.abi"),
        Generator.generate(
                Dsl.parse(source().replace("c_library_static=true", "c_library_static=false")),
                true)
            .get("seam.abi"));
  }

  @Test
  void externalTypesWorkInReturnsAndEntryPointLiterals() throws Exception {
    String source =
        source()
            + "\n"
            + "import getSink return=fn<Sink> error=no_failure\n"
            + "end\n"
            + "export receive return=i64 error=abort java=fixture.bemo.BemoMain.receive"
            + " isolate=thread callback=Sink\n"
            + "param value type=i64\n"
            + "end\n";
    String java = Generator.generate(Dsl.parse(source), true).get("BemoImports.java");
    assertTrue(java.contains("CEntryPointLiteral<fixture.bemo.HandwrittenSink>"));
    assertTrue(java.contains("fixture.bemo.HandwrittenSink getSink()"));
  }

  @Test
  void contextAndLibraryCanBeIndependent() throws Exception {
    String contextOnly = source().replace(" c_library=bemofixture c_library_static=true", "");
    String java = Generator.generate(Dsl.parse(contextOnly), true).get("BemoImports.java");
    assertTrue(java.contains("@CContext"));
    assertFalse(java.contains("@CLibrary"));
    java =
        Generator.generate(
                Dsl.parse(
                    source()
                        .replace(" c_context=fixture.bemo.BemoDirectives", "")
                        .replace(" c_library_static=true", "")),
                true)
            .get("BemoImports.java");
    assertFalse(java.contains("@CContext"));
    assertTrue(java.contains("requireStatic = false"));
  }

  @Test
  void linkageWithoutImportsIsRejected() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            Generator.generate(
                Dsl.parse("module empty target=aarch64-apple-darwin c_library=bemo\n"), true));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "c_context=fixture.bemo.BemoDirectives|c_context=Unqualified",
        "c_context=fixture.bemo.BemoDirectives|c_context=fixture..Invalid",
        "c_library=bemofixture|c_library=../bad",
        "c_library=bemofixture |",
        "c_library_static=true|c_library_static=yes",
        "java_type=fixture.bemo.HandwrittenSink|java_type=Missing",
        "java_type=fixture.bemo.HandwrittenSink|java_type=fixture.class"
      })
  void rejectsMalformedConfiguration(String mutation) throws Exception {
    var parts = mutation.split("\\|", -1);
    String changed = source().replace(parts[0], parts[1]);
    assertThrows(
        IllegalArgumentException.class, () -> Generator.generate(Dsl.parse(changed), true));
  }
}
