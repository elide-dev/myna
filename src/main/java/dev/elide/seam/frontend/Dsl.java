package dev.elide.seam.frontend;

import dev.elide.seam.model.Seam;
import dev.elide.seam.model.Seam.*;
import dev.elide.seam.model.Seam.Module;
import java.util.*;

/** Strict line-oriented frontend. Unknown options are errors, including misspelled contracts. */
public final class Dsl {
  private Dsl() {}

  public static Module parse(String source) {
    String name = null, target = null, javaPackage = null, javaClass = null;
    int abi = 1;
    var opaque = new ArrayList<String>();
    var structs = new ArrayList<Struct>();
    var functions = new ArrayList<Function>();
    String[] header = null;
    Map<String, String> options = null;
    var parameters = new ArrayList<Parameter>();
    var fields = new ArrayList<Field>();
    var lines = source.split("\\R");
    for (int i = 0; i < lines.length; i++) {
      String line = lines[i].split("#", 2)[0].strip();
      if (line.isEmpty()) continue;
      try {
        String[] words = line.split("\\s+");
        switch (words[0]) {
          case "module" -> {
            if (name != null
                || header != null
                || !functions.isEmpty()
                || !structs.isEmpty()
                || !opaque.isEmpty())
              throw new IllegalArgumentException("module must be declared once, first");
            require(words.length >= 2, "expected module name");
            name = words[1];
            var opts = options(words, 2);
            target = take(opts, "target", null);
            abi = Integer.parseInt(take(opts, "abi", "1"));
            javaPackage = take(opts, "java_package", null);
            javaClass = take(opts, "java_class", null);
            empty(opts);
          }
          case "opaque" -> {
            require(
                name != null && header == null && words.length == 2,
                "expected opaque NAME outside a block");
            opaque.add(words[1]);
          }
          case "struct", "import", "export" -> {
            require(
                name != null && header == null && words.length >= 2,
                "expected a named top-level block");
            header = words;
            options = options(words, 2);
            parameters.clear();
            fields.clear();
          }
          case "param" -> {
            require(
                header != null && !header[0].equals("struct") && words.length >= 3,
                "param requires a function block and a type");
            var opts = options(words, 2);
            var type = Seam.type(takeRequired(opts, "type"));
            boolean nullable = bool(take(opts, "nullable", "true"));
            var ownership =
                Ownership.valueOf(take(opts, "ownership", "unspecified").toUpperCase(Locale.ROOT));
            var access =
                Access.valueOf(take(opts, "access", "unspecified").toUpperCase(Locale.ROOT));
            parameters.add(new Parameter(words[1], type, nullable, ownership, access, facts(opts)));
          }
          case "field" -> {
            require(
                header != null && header[0].equals("struct") && words.length >= 3,
                "field requires a struct block");
            var opts = options(words, 2);
            fields.add(
                new Field(
                    words[1],
                    Seam.type(takeRequired(opts, "type")),
                    Long.parseLong(takeRequired(opts, "offset"))));
            empty(opts);
          }
          case "end" -> {
            require(header != null && words.length == 1, "unexpected end");
            if (header[0].equals("struct")) {
              structs.add(
                  new Struct(
                      header[1],
                      Long.parseLong(takeRequired(options, "size")),
                      Integer.parseInt(takeRequired(options, "align")),
                      fields));
              empty(options);
            } else {
              String symbol = take(options, "symbol", header[1]);
              var returns = Seam.type(takeRequired(options, "return"));
              var error =
                  ErrorConvention.valueOf(takeRequired(options, "error").toUpperCase(Locale.ROOT));
              String sentinel = take(options, "sentinel", null);
              if (sentinel != null && sentinel.matches("-?[0-9]+"))
                sentinel = new java.math.BigInteger(sentinel).toString();
              String javaTarget = take(options, "java", null);
              String include = take(options, "include", null);
              String isolate = take(options, "isolate", "none");
              require(
                  isolate.equals("none") || isolate.equals("thread"),
                  "isolate must be none or thread");
              String cc = take(options, "cc", "c");
              require(cc.equals("c"), "only cc=c is supported");
              functions.add(
                  new Function(
                      header[1],
                      symbol,
                      Direction.valueOf(header[0].toUpperCase(Locale.ROOT)),
                      returns,
                      parameters,
                      error,
                      sentinel,
                      javaTarget,
                      include,
                      isolate.equals("thread"),
                      facts(options)));
            }
            header = null;
            options = null;
          }
          default -> throw new IllegalArgumentException("unknown directive: " + words[0]);
        }
      } catch (IllegalArgumentException e) {
        throw new IllegalArgumentException("line " + (i + 1) + ": " + e.getMessage(), e);
      }
    }
    require(name != null, "missing module");
    require(header == null, "unterminated block");
    return new Module(name, abi, target, javaPackage, javaClass, opaque, structs, functions);
  }

  private static Map<String, String> options(String[] words, int start) {
    var result = new LinkedHashMap<String, String>();
    for (int i = start; i < words.length; i++) {
      String[] pair = words[i].split("=", 2);
      require(pair.length == 2 && !pair[1].isEmpty(), "expected key=value: " + words[i]);
      require(result.putIfAbsent(pair[0], pair[1]) == null, "duplicate option: " + pair[0]);
    }
    return result;
  }

  private static SortedMap<String, Fact> facts(Map<String, String> options) {
    var result = new TreeMap<String, Fact>();
    options.forEach(
        (key, value) -> {
          String[] pieces = value.split("@", -1);
          require(pieces.length == 2, "contract " + key + " requires VALUE@PROVENANCE");
          result.put(
              key,
              new Fact(
                  pieces[0],
                  Provenance.valueOf(pieces[1].toUpperCase(Locale.ROOT)),
                  "DSL contract"));
        });
    return result;
  }

  private static String take(Map<String, String> opts, String key, String fallback) {
    String value = opts.remove(key);
    return value == null ? fallback : value;
  }

  private static String takeRequired(Map<String, String> opts, String key) {
    String value = opts.remove(key);
    require(value != null, "missing " + key);
    return value;
  }

  private static void empty(Map<String, String> opts) {
    require(opts.isEmpty(), "unknown options: " + opts.keySet());
  }

  private static boolean bool(String value) {
    require(value.equals("true") || value.equals("false"), "expected true or false");
    return Boolean.parseBoolean(value);
  }

  private static void require(boolean test, String message) {
    if (!test) throw new IllegalArgumentException(message);
  }
}
