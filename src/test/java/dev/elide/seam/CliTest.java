package dev.elide.seam;

import static org.junit.jupiter.api.Assertions.*;

import dev.elide.seam.cli.Main;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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
    assertTrue(run("--help").out().contains("svmgen apply"));
    assertEquals("0.4.0\n", run("--version").out());
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
            new String[] {"generate", "examples/buffer.seam"},
            new String[] {"apply"},
            new String[] {"apply", "--wrong", "x"},
            new String[] {"apply", "--contracts"},
            new String[] {"apply", "--input", "x", "--input", "y"})) {
      var result = run(args);
      assertEquals(2, result.code(), Arrays.toString(args));
      assertTrue(result.err().startsWith("svmgen:"));
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
  void validationFailureDoesNotWrite() throws Exception {
    Path input = temp.resolve("bad.seam"), out = temp.resolve("out");
    Files.writeString(
        input, GeneratorTest.VALID.replace("access=read", "access=read readonly=true@heuristic"));
    assertEquals(2, run("generate", input.toString(), "--out", out.toString()).code());
    assertFalse(Files.exists(out));
    assertEquals(0, run("generate", input.toString(), "--out", out.toString(), "--relaxed").code());
    assertFalse(Files.readString(out.resolve("seam.ll")).contains("readonly"));
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
