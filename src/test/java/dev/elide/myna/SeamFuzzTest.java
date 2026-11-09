// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package dev.elide.myna;

import static org.junit.jupiter.api.Assertions.*;

import com.sun.source.util.JavacTask;
import dev.elide.myna.frontend.Dsl;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.SortedMap;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;

/**
 * Seeded mutation of the checked-in example seams through the parser, Validator and every backend.
 * Each mutant must either be rejected with {@link IllegalArgumentException} or generate output in
 * which adversarial option text (quotes, backslashes, comment delimiters, non-ASCII) cannot break
 * out: generated Java must parse, and C and Rust comment and string delimiters must balance.
 */
class SeamFuzzTest {
  private static final long SEED = 1729;
  private static final int MUTANTS = 1500;
  private static final String[] HOSTILE = {
    "\"",
    "\\",
    "*/",
    "/*",
    "//",
    "\"x\";",
    "a*/b",
    "x\\\"y",
    "${x}",
    "é",
    " ",
    "0x10",
    "-1",
    "99999999999999999999",
    "-9223372036854775809",
    "1e3",
    "a.b.c",
    "java.lang.String",
    "_",
    "__x",
    "class",
    "true@explicit_contract",
    "false@heuristic",
    "ptr<ptr<i8>>",
    "ptr<",
    "i64",
    "u8",
    "void",
    "ptr<void>",
    "fn",
    "end",
    "=",
    "a=b",
    "@",
    "x@",
    "{",
    "}",
    ";",
    ")"
  };

  private static List<String> seeds() throws IOException {
    var seeds = new ArrayList<String>();
    try (var files = Files.list(Path.of("examples"))) {
      for (Path path : files.filter(p -> p.toString().endsWith(".seam")).sorted().toList())
        seeds.add(Files.readString(path));
    }
    assertFalse(seeds.isEmpty(), "example seams");
    return seeds;
  }

  private static String mutate(String source, List<String> corpus, Random random) {
    var lines = new ArrayList<>(List.of(source.split("\n", -1)));
    int edits = 1 + random.nextInt(3);
    for (int edit = 0; edit < edits && !lines.isEmpty(); edit++) {
      int index = random.nextInt(lines.size());
      var words = new ArrayList<>(List.of(lines.get(index).strip().split("\\s+")));
      int word = random.nextInt(words.size());
      String current = words.get(word);
      String hostile = HOSTILE[random.nextInt(HOSTILE.length)];
      switch (random.nextInt(8)) {
        case 0 -> words.remove(word);
        case 1 -> words.add(word, current);
        case 2 -> words.set(word, hostile);
        case 3 -> {
          int equals = current.indexOf('=');
          // Keep the key, replace the value: reaches option-specific validation.
          words.set(
              word, equals < 0 ? current + hostile : current.substring(0, equals + 1) + hostile);
        }
        case 4 -> words.set(word, current + hostile);
        case 5 -> lines.remove(index);
        case 6 -> lines.add(index, corpus.get(random.nextInt(corpus.size())));
        default -> {
          int equals = current.indexOf('=');
          if (equals > 0) words.set(word, current.substring(0, equals + 1) + current + hostile);
        }
      }
      if (index < lines.size() && random.nextInt(8) != 5) lines.set(index, String.join(" ", words));
    }
    return String.join("\n", lines);
  }

  @Test
  void everyMutantIsRejectedOrGeneratesWellFormedSources() throws IOException {
    var seeds = seeds();
    var corpus = new ArrayList<String>();
    for (String seed : seeds)
      for (String line : seed.split("\n")) if (!line.isBlank()) corpus.add(line);
    var random = new Random(SEED);
    int accepted = 0;
    for (int i = 0; i < MUTANTS; i++) {
      String source = mutate(seeds.get(random.nextInt(seeds.size())), corpus, random);
      SortedMap<String, String> outputs;
      try {
        outputs = Generator.generate(Dsl.parse(source), random.nextBoolean());
      } catch (IllegalArgumentException rejected) {
        continue;
      } catch (RuntimeException | StackOverflowError crash) {
        throw new AssertionError("mutant " + i + " crashed the generator:\n" + source, crash);
      }
      accepted++;
      for (Map.Entry<String, String> output : outputs.entrySet())
        wellFormed(output.getKey(), output.getValue(), i, source);
    }
    // The campaign must reach backends, not only the parser's rejection paths.
    assertTrue(accepted > MUTANTS / 20, "accepted mutants: " + accepted);
  }

  private static void wellFormed(String name, String text, int mutant, String source) {
    String where = "mutant " + mutant + " output " + name + "\n" + source;
    if (name.endsWith(".java")) parsesAsJava(name, text, where);
    if (name.endsWith(".h") || name.endsWith(".rs") || name.endsWith(".java")) {
      assertEquals(count(text, "/*"), count(text, "*/"), "unbalanced block comments in " + where);
      for (String line : text.split("\n")) {
        String code = line.contains("//") ? line.substring(0, line.indexOf("//")) : line;
        if (code.strip().startsWith("*")) continue;
        assertEquals(
            0,
            count(code.replace("\\\\", "").replace("\\\"", ""), "\"") % 2,
            "unbalanced string literal `" + line + "` in " + where);
      }
    }
  }

  private static int count(String text, String needle) {
    int count = 0;
    for (int at = text.indexOf(needle); at >= 0; at = text.indexOf(needle, at + needle.length()))
      count++;
    return count;
  }

  private static void parsesAsJava(String name, String text, String where) {
    var compiler = ToolProvider.getSystemJavaCompiler();
    assertNotNull(compiler, "javac is required to parse generated Java");
    var diagnostics = new DiagnosticCollector<JavaFileObject>();
    var file =
        new SimpleJavaFileObject(URI.create("string:///" + name), JavaFileObject.Kind.SOURCE) {
          @Override
          public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return text;
          }
        };
    var task =
        (JavacTask)
            compiler.getTask(null, null, diagnostics, List.of("-proc:none"), null, List.of(file));
    try {
      task.parse();
    } catch (IOException e) {
      throw new AssertionError(e);
    }
    for (var diagnostic : diagnostics.getDiagnostics())
      assertNotEquals(
          Diagnostic.Kind.ERROR, diagnostic.getKind(), diagnostic + " in generated " + where);
  }
}
