package dev.elide.seam.cli;

import dev.elide.seam.Generator;
import dev.elide.seam.frontend.Dsl;
import dev.elide.seam.model.Seam;
import dev.elide.seam.model.Seam.Module;
import dev.elide.seam.serialize.SeamJson;
import dev.elide.seam.validate.Validator;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public final class Main {
  private Main() {}

  private static final String HELP =
      """
      svmgen 0.2.0 — Native Image / Rust ABI generator
      Usage:
        svmgen generate INPUT.seam --out DIRECTORY [--target TRIPLE] [--relaxed] [--check]
        svmgen validate INPUT.seam [--target TRIPLE] [--relaxed]
        svmgen apply --contracts seam.ll --input caller.o --output caller.seam.o [--llvm-tool PATH]
        svmgen --help | --version
      apply rewrites LLVM 23 bitcode objects or archive members using svmgen-llvm.
      --relaxed omits unapproved optimizer facts; unknown/contradictory contracts still fail.
      --check verifies generated files without modifying them.
      """;

  public static void main(String[] args) {
    System.exit(run(args, System.out, System.err));
  }

  @FunctionalInterface
  public interface CommandRunner {
    int run(List<String> command, PrintStream out) throws IOException, InterruptedException;
  }

  public static int run(String[] args, PrintStream out, PrintStream err) {
    return run(args, out, err, Main::execute);
  }

  public static int run(String[] args, PrintStream out, PrintStream err, CommandRunner runner) {
    try {
      if (args.length == 1 && args[0].equals("--version")) {
        out.println(Seam.VERSION);
        return 0;
      }
      if (args.length == 0 || (args.length == 1 && args[0].equals("--help"))) {
        out.print(HELP);
        return 0;
      }
      if (args[0].equals("apply")) return apply(args, out, err, runner);
      if (!args[0].equals("generate") && !args[0].equals("validate"))
        throw new IllegalArgumentException("unknown command: " + args[0]);
      if (args.length < 2) throw new IllegalArgumentException("missing INPUT.seam");
      Path input = Path.of(args[1]), destination = null;
      String target = null;
      boolean strict = true, check = false;
      Set<String> seen = new HashSet<>();
      for (int i = 2; i < args.length; i++) {
        if (!seen.add(args[i])) throw new IllegalArgumentException("duplicate option: " + args[i]);
        switch (args[i]) {
          case "--out" -> {
            requireValue(args, i);
            destination = Path.of(args[++i]);
          }
          case "--target" -> {
            requireValue(args, i);
            target = args[++i];
          }
          case "--relaxed" -> strict = false;
          case "--check" -> check = true;
          default -> throw new IllegalArgumentException("unknown option: " + args[i]);
        }
      }
      Module module = Dsl.parse(Files.readString(input));
      if (target != null) module = module.withTarget(target);
      Validator.validate(module, strict);
      if (args[0].equals("validate")) {
        if (destination != null || check)
          throw new IllegalArgumentException("validate does not accept --out/--check");
        out.println("valid " + module.name() + " " + SeamJson.fingerprint(module));
        return 0;
      }
      if (destination == null)
        throw new IllegalArgumentException("generate requires --out DIRECTORY");
      var outputs = Generator.generate(module, strict);
      if (check) {
        for (var item : outputs.entrySet()) {
          Path file = destination.resolve(item.getKey());
          if (!Files.isRegularFile(file) || !Files.readString(file).equals(item.getValue())) {
            err.println("generated artifact differs: " + file);
            return 1;
          }
        }
      } else write(outputs, destination);
      out.println(
          (check ? "verified " : "generated ")
              + module.name()
              + " "
              + SeamJson.fingerprint(module));
      return 0;
    } catch (IllegalArgumentException | IOException e) {
      err.println("svmgen: " + e.getMessage());
      return 2;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      err.println("svmgen: interrupted");
      return 130;
    }
  }

  public static void write(Map<String, String> outputs, Path destination) throws IOException {
    Files.createDirectories(destination);
    // Complete parsing and validation happens before entering this method. Replace files
    // atomically.
    for (var item : outputs.entrySet()) {
      Path temporary = Files.createTempFile(destination, ".svmgen-", ".tmp");
      try {
        Files.writeString(temporary, item.getValue());
        try {
          Files.move(
              temporary,
              destination.resolve(item.getKey()),
              StandardCopyOption.ATOMIC_MOVE,
              StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
          Files.move(
              temporary, destination.resolve(item.getKey()), StandardCopyOption.REPLACE_EXISTING);
        }
      } finally {
        Files.deleteIfExists(temporary);
      }
    }
  }

  private static int apply(String[] args, PrintStream out, PrintStream err, CommandRunner runner)
      throws IOException, InterruptedException {
    Map<String, String> options = new HashMap<>();
    for (int i = 1; i < args.length; i += 2) {
      if (!Set.of("--contracts", "--input", "--output", "--llvm-tool").contains(args[i]))
        throw new IllegalArgumentException("unknown apply option: " + args[i]);
      requireValue(args, i);
      if (options.putIfAbsent(args[i], args[i + 1]) != null)
        throw new IllegalArgumentException("duplicate option: " + args[i]);
    }
    for (String required : List.of("--contracts", "--input", "--output"))
      if (!options.containsKey(required))
        throw new IllegalArgumentException("apply requires " + required);
    var command = new ArrayList<String>();
    command.add(options.getOrDefault("--llvm-tool", "svmgen-llvm"));
    for (String key : List.of("--contracts", "--input", "--output")) {
      command.add(key);
      command.add(options.get(key));
    }
    int code = runner.run(List.copyOf(command), out);
    if (code != 0) err.println("svmgen: LLVM contract application failed");
    return code;
  }

  private static int execute(List<String> command, PrintStream out)
      throws IOException, InterruptedException {
    Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
    try {
      process.getInputStream().transferTo(out);
      return process.waitFor();
    } finally {
      if (process.isAlive()) process.destroyForcibly();
    }
  }

  private static void requireValue(String[] args, int i) {
    if (i + 1 == args.length || args[i + 1].startsWith("--"))
      throw new IllegalArgumentException("missing value for " + args[i]);
  }
}
