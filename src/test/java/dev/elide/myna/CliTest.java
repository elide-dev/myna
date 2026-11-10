// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package dev.elide.myna;

import static org.junit.jupiter.api.Assertions.*;

import dev.elide.myna.cli.Main;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CliTest {
  @TempDir Path temp;

  record Result(int code, String out, String err) {}

  Result run(String... args) {
    var out = new ByteArrayOutputStream();
    var err = new ByteArrayOutputStream();
    int code = Main.run(args, new PrintStream(out), new PrintStream(err));
    return new Result(code, out.toString(), err.toString());
  }

  @Test
  void helpVersionAndBadUsage() {
    assertEquals(0, run().code());
    assertTrue(run("--help").out().contains("myna apply"));
    assertEquals("0.17.0\n", run("--version").out());
    for (String[] args :
        List.of(
            new String[] {"bad"},
            new String[] {"generate"},
            new String[] {"generate", "missing"},
            new String[] {"generate", "examples/buffer.seam", "--out"},
            new String[] {"generate", "examples/buffer.seam", "--out", "--check"},
            new String[] {"generate", "examples/buffer.seam", "--unknown"},
            new String[] {"generate", "examples/buffer.seam", "--check", "--check"},
            new String[] {"validate", "examples/buffer.seam", "--check"},
            new String[] {"validate", "examples/buffer.seam", "--targets"},
            new String[] {"generate", "examples/buffer.seam", "--targets", "x86_64-apple-darwin"},
            new String[] {
              "validate",
              "examples/buffer.seam",
              "--target",
              "x86_64-apple-darwin",
              "--targets",
              "x86_64-apple-darwin"
            },
            new String[] {
              "validate", "examples/buffer.seam", "--targets", "x86_64-apple-darwin", "--check"
            },
            new String[] {
              "validate",
              "examples/buffer.seam",
              "--targets",
              "x86_64-apple-darwin,x86_64-apple-darwin"
            },
            new String[] {
              "validate", "examples/buffer.seam", "--targets", "x86_64-apple-darwin,riscv64-linux"
            },
            new String[] {"validate", "examples/buffer.seam", "--targets", "x86_64-apple-darwin,"},
            new String[] {"generate", "examples/buffer.seam"},
            new String[] {"apply"},
            new String[] {"apply", "--wrong", "x"},
            new String[] {"apply", "--contracts"},
            new String[] {"apply", "--input", "x", "--input", "y"})) {
      var result = run(args);
      assertEquals(2, result.code(), Arrays.toString(args));
      assertTrue(result.err().startsWith("myna:"));
    }
  }

  @Test
  void generateValidateCheckAndOverwrite() throws Exception {
    Path out = temp.resolve("generated");
    assertEquals(0, run("validate", "examples/buffer.seam").code());
    assertEquals(0, run("generate", "examples/buffer.seam", "--out", out.toString()).code());
    assertEquals(
        0, run("generate", "examples/buffer.seam", "--out", out.toString(), "--check").code());
    Files.writeString(out.resolve("seam.h"), "drift");
    assertEquals(
        1, run("generate", "examples/buffer.seam", "--out", out.toString(), "--check").code());
    assertEquals("drift", Files.readString(out.resolve("seam.h")));
    assertEquals(
        0,
        run(
                "generate",
                "examples/buffer.seam",
                "--out",
                out.toString(),
                "--target",
                "x86_64-unknown-linux-gnu")
            .code());
    assertTrue(Files.readString(out.resolve("seam.ll")).contains("x86_64-unknown-linux-gnu"));
    Files.delete(out.resolve("seam.abi"));
    assertEquals(
        1, run("generate", "examples/buffer.seam", "--out", out.toString(), "--check").code());
  }

  @Test
  void validateTargetsRequiresOneFingerprint() {
    // The three signed-char targets (x86-64 Linux, Apple arm64, x86-64 Apple) share one ABI
    // fingerprint; aarch64-unknown-linux-gnu (AAPCS64 Linux, unsigned char) is its own ABI
    // class and now shares no fingerprint with the signed-char targets.
    var signed =
        run(
            "validate",
            "examples/buffer.seam",
            "--targets",
            "x86_64-unknown-linux-gnu,aarch64-apple-darwin,x86_64-apple-darwin");
    assertEquals(0, signed.code(), signed.err());
    for (String target :
        List.of("x86_64-unknown-linux-gnu", "aarch64-apple-darwin", "x86_64-apple-darwin"))
      assertEquals(
          signed.out(), run("validate", "examples/buffer.seam", "--target", target).out(), target);
    assertTrue(signed.out().matches("valid buffer [0-9a-f]{64}\n"), signed.out());
    var crossClass =
        run(
            "validate",
            "examples/buffer.seam",
            "--targets",
            "x86_64-unknown-linux-gnu,aarch64-unknown-linux-gnu");
    assertEquals(2, crossClass.code(), crossClass.err());
    assertTrue(crossClass.err().startsWith("myna:"), crossClass.err());
    assertTrue(crossClass.err().contains("different ABI fingerprints"), crossClass.err());
    var diverged =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                Main.sharedFingerprint(
                    new TreeMap<>(
                        Map.of(
                            "aa", List.of("x86_64-unknown-linux-gnu (LP64, little-endian)"),
                            "bb",
                                List.of(
                                    "aarch64-unknown-linux-gnu (LP64, little-endian,"
                                        + " unsigned char)")))));
    assertEquals(
        "targets have different ABI fingerprints: aa for x86_64-unknown-linux-gnu (LP64,"
            + " little-endian); bb for aarch64-unknown-linux-gnu (LP64, little-endian,"
            + " unsigned char)",
        diverged.getMessage());
  }

  @Test
  void validationFailureDoesNotWrite() throws Exception {
    Path input = temp.resolve("bad.seam"), out = temp.resolve("out");
    Files.writeString(
        input, GeneratorTest.VALID.replace("access=read", "access=read readonly=true@heuristic"));
    assertEquals(2, run("generate", input.toString(), "--out", out.toString()).code());
    assertFalse(Files.exists(out));
    assertEquals(0, run("generate", input.toString(), "--out", out.toString(), "--relaxed").code());
    assertFalse(Files.readString(out.resolve("seam.ll")).contains("readonly"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"box", "become", "override", "priv", "typeof", "unsized", "gen"})
  void reservedRustKeywordsAreRejectedBeforeAnyOutput(String keyword) throws Exception {
    Path input = temp.resolve("reserved-" + keyword + ".seam");
    Path out = temp.resolve("out-" + keyword);
    Files.writeString(
        input,
        "module demo target=aarch64-apple-darwin\n"
            + "import "
            + keyword
            + " return=i64 error=no_failure\n"
            + "  param become type=u64\n"
            + "end\n");
    var validate = run("validate", input.toString());
    assertEquals(2, validate.code(), keyword);
    assertFalse(validate.out().contains("valid demo"), keyword);
    assertTrue(validate.err().contains("invalid or reserved identifier: " + keyword), keyword);
    assertEquals(2, run("generate", input.toString(), "--out", out.toString()).code(), keyword);
    assertFalse(Files.exists(out), keyword);
  }

  @Test
  void generatedMetadataIsNestedAndCannotEscapeTheOutputDirectory() throws Exception {
    Main.write(
        Map.of("META-INF/native-image/example/native-image.properties", "Args = test\n"), temp);
    assertEquals(
        "Args = test\n",
        Files.readString(temp.resolve("META-INF/native-image/example/native-image.properties")));
    assertThrows(
        IllegalArgumentException.class, () -> Main.write(Map.of("../escaped", "bad"), temp));
  }

  @Test
  void applyForwardsArgumentsWithoutShellExpansion() {
    var args =
        new String[] {
          "apply",
          "--contracts",
          "$(echo bad)",
          "--input",
          "input space.o",
          "--output",
          "out.o",
          "--llvm-tool",
          "fake tool"
        };
    var out = new PrintStream(new ByteArrayOutputStream());
    Main.CommandRunner success =
        (command, stream) -> {
          assertEquals(
              List.of(
                  "fake tool",
                  "--contracts",
                  "$(echo bad)",
                  "--input",
                  "input space.o",
                  "--output",
                  "out.o"),
              command);
          return 0;
        };
    assertEquals(0, Main.run(args, out, out, success));
    assertEquals(2, Main.run(args, out, out, (c, o) -> 2));
    assertEquals(
        2,
        Main.run(
            args,
            out,
            out,
            (c, o) -> {
              throw new IOException("missing tool");
            }));
    try {
      assertEquals(
          130,
          Main.run(
              args,
              out,
              out,
              (c, o) -> {
                throw new InterruptedException();
              }));
      assertTrue(Thread.currentThread().isInterrupted());
    } finally {
      Thread.interrupted();
    }
  }
}
