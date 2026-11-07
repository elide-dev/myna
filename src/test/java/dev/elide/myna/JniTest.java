// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package dev.elide.myna;

import static org.junit.jupiter.api.Assertions.*;

import dev.elide.myna.backend.JniAbi;
import dev.elide.myna.backend.StaticJni;
import dev.elide.myna.cli.Main;
import dev.elide.myna.frontend.Dsl;
import dev.elide.myna.model.Seam.*;
import java.io.*;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JniTest {
  @TempDir Path temp;

  @Test
  void scalarCarriersAndImplicitArguments() {
    StringBuilder source = new StringBuilder("module jni_test target=aarch64-apple-darwin\n");
    for (Scalar s : Scalar.values()) {
      source
          .append("import call_")
          .append(s.text())
          .append(" return=")
          .append(s.text())
          .append(" error=no_failure\n");
      if (s != Scalar.VOID) source.append("param value type=").append(s.text()).append('\n');
      source.append("end\n");
    }
    var m = Dsl.parse(source.toString());
    var methods = JniAbi.methods(m);
    assertEquals(Scalar.values().length, methods.size());
    for (var method : methods) {
      assertEquals("JNIEnv *", method.arguments().getFirst().carrier().c());
      assertEquals("jclass", method.arguments().get(1).carrier().c());
      assertTrue(method.symbol().startsWith("Java_dev_elide_myna_generated_MynaNativeJNI_call_1"));
      assertTrue(method.descriptor().startsWith("("));
    }
    String c = StaticJni.generate(m).get("seam-jni.c");
    assertTrue(c.contains("JNI_OnLoad_jni_test_jni"));
    assertTrue(c.contains("return (jbyte)call_u8((uint8_t)arg0)"));
    assertTrue(c.contains("return (jboolean)call_bool((bool)arg0)"));
    String contracts = StaticJni.generate(m).get("seam-jni.ll");
    assertTrue(contracts.contains("i8 signext %arg0"));
    assertTrue(contracts.contains("i8 zeroext %arg0"));
    assertFalse(
        StaticJni.generate(m.withTarget("aarch64-unknown-linux-gnu"))
            .get("seam-jni.ll")
            .contains("signext"));
  }

  @Test
  void adaptersPreserveRawAbiAndDoNotCopyEffects() throws Exception {
    var m = Dsl.parse(Files.readString(Path.of("examples/ownership.seam")));
    var output = StaticJni.generate(m);
    String c = output.get("seam-jni.c"),
        java = output.get("NativeApiJNI.java"),
        llvm = output.get("seam-jni.ll");
    assertTrue(c.contains("return (jlong)(uintptr_t)ticket_create()"));
    assertTrue(c.contains("ticket_destroy((struct Ticket *)(uintptr_t)arg0)"));
    assertTrue(c.contains("GetDirectBufferAddress"));
    assertTrue(java.contains("arg0.slice()"));
    assertTrue(java.contains("isReadOnly()"));
    assertTrue(
        llvm.lines()
            .filter(l -> l.startsWith("declare") && l.contains("@Java_"))
            .noneMatch(
                l -> l.contains("nounwind") || l.contains("readonly") || l.contains("captures")));
    assertTrue(llvm.contains("@buffer_checksum(ptr captures(none) readonly"));
    assertTrue(
        JniAbi.methods(m).stream()
            .anyMatch(x -> x.descriptor().equals("(Ljava/nio/ByteBuffer;)J")));
    var buffer = Dsl.parse(Files.readString(Path.of("examples/buffer.seam")));
    c = StaticJni.generate(buffer).get("seam-jni.c");
    assertTrue(c.contains("*(struct Triple *)(uintptr_t)result = seam_rotate"));
    assertTrue(c.contains("*(struct Triple *)(uintptr_t)arg0"));
    assertFalse(
        JniAbi.methods(buffer).stream()
            .anyMatch(x -> x.implementation().direction() == Direction.EXPORT));
    String source = Files.readString(Path.of("examples/ownership.seam"));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            StaticJni.generate(
                Dsl.parse(source + "\nimport checksumBuffer return=void error=no_failure\nend\n")));
    assertThrows(
        IllegalArgumentException.class,
        () -> StaticJni.generate(Dsl.parse("module empty target=aarch64-apple-darwin\n")));
  }

  @Test
  void cliMakesJniOptionalAndChecksItsOutputs() throws Exception {
    var out = new PrintStream(new ByteArrayOutputStream());
    Path destination = temp.resolve("generated");
    String[] args = {
      "generate", "examples/ownership.seam", "--out", destination.toString(), "--jni"
    };
    assertEquals(0, Main.run(args, out, out));
    assertTrue(Files.exists(destination.resolve("seam-jni.c")));
    assertEquals(
        0,
        Main.run(
            new String[] {
              "generate",
              "examples/ownership.seam",
              "--out",
              destination.toString(),
              "--jni",
              "--check"
            },
            out,
            out));
    assertEquals(
        2, Main.run(new String[] {"validate", "examples/ownership.seam", "--jni"}, out, out));
  }
}
