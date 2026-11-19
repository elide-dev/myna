// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package dev.elide.myna.frontend;

import dev.elide.myna.model.Persisted;
import dev.elide.myna.model.Seam;
import dev.elide.myna.model.Seam.*;
import dev.elide.myna.model.Seam.Module;
import java.math.BigInteger;
import java.util.*;

/** Strict line-oriented frontend. Unknown options are errors, including misspelled contracts. */
public final class Dsl {
  private Dsl() {}

  public static Module parse(String source) {
    String name = null, target = null, javaPackage = null, javaClass = null;
    int abi = 1;
    boolean abiDeclared = false;
    boolean rustImplementation = false;
    NativeConfig nativeConfig = null;
    TruffleConfig truffleConfig = null;
    var opaque = new ArrayList<String>();
    var structs = new ArrayList<Struct>();
    var functions = new ArrayList<Function>();
    var callbacks = new ArrayList<Signature>();
    var resources = new ArrayList<Resource>();
    var borrows = new ArrayList<BufferBorrow>();
    var constants = new ArrayList<Constant>();
    var enums = new ArrayList<EnumGroup>();
    var flagSets = new ArrayList<FlagSet>();
    var enumValues = new ArrayList<EnumValue>();
    var flagBits = new ArrayList<FlagBit>();
    var flagRanges = new ArrayList<FlagRange>();
    String[] header = null;
    Map<String, String> options = null;
    var parameters = new ArrayList<Parameter>();
    var fields = new ArrayList<Field>();
    PersistedDsl persisted = null; // created by the first persisted-format directive
    var lines = source.split("\\R");
    String previous = null; // the directive of the previous non-blank line

    for (int i = 0; i < lines.length; i++) {
      String line = lines[i].split("#", 2)[0].strip();
      if (line.isEmpty()) continue;
      try {
        String[] words = line.split("\\s+");
        String directive = previous;
        previous = words[0];
        switch (words[0]) {
          case "module" -> {
            if (name != null
                || header != null
                || !functions.isEmpty()
                || !callbacks.isEmpty()
                || !structs.isEmpty()
                || !opaque.isEmpty())
              throw new IllegalArgumentException("module must be declared once, first");
            require(words.length >= 2, "expected module name");
            name = words[1];
            var opts = options(words, 2);
            target = take(opts, "target", null);
            abiDeclared = opts.containsKey("abi");
            abi = Integer.parseInt(take(opts, "abi", "1"));
            javaPackage = take(opts, "java_package", null);
            javaClass = take(opts, "java_class", null);
            String context = take(opts, "c_context", null);
            String library = take(opts, "c_library", null);
            String requireStatic = take(opts, "c_library_static", null);
            require(
                requireStatic == null || library != null, "c_library_static requires c_library");
            if (context != null || library != null)
              nativeConfig =
                  new NativeConfig(context, library, requireStatic != null && bool(requireStatic));
            String implementation = take(opts, "rust_implementation", null);
            require(
                implementation == null || implementation.equals("trait"),
                "rust_implementation is trait: " + implementation);
            rustImplementation = implementation != null;
            String cells = take(opts, "truffle_cells", null);
            String backend = take(opts, "truffle_backend", "native");
            String coercion = take(opts, "truffle_coercion", "strict");
            String transitions = take(opts, "truffle_transitions", "direct");
            boolean enabled = cells != null && bool(cells);
            require(
                enabled
                    || (backend.equals("native")
                        && coercion.equals("strict")
                        && transitions.equals("direct")),
                "truffle_backend, truffle_coercion and truffle_transitions require"
                    + " truffle_cells=true");
            if (enabled)
              truffleConfig =
                  new TruffleConfig(
                      option(TruffleBackend.class, "truffle_backend", backend),
                      option(TruffleCoercion.class, "truffle_coercion", coercion),
                      option(TruffleTransitions.class, "truffle_transitions", transitions));
            empty(opts);
          }
          case "resource" -> {
            require(
                name != null && header == null && words.length >= 2,
                "resource requires a top-level name");
            var opts = options(words, 2);
            require(
                takeRequired(opts, "representation").equals("native"),
                "only representation=native is supported");
            String threading = takeRequired(opts, "threading");
            require(
                threading.equals("confined") || threading.equals("shared"),
                "threading must be confined or shared");
            String create = takeRequired(opts, "create");
            String destroy = takeRequired(opts, "destroy");
            String handle = take(opts, "handle", null);
            var success = new ArrayList<Long>();
            String statuses = take(opts, "success", null);
            if (statuses != null)
              for (String status : statuses.split(",", -1)) {
                try {
                  success.add(Long.parseLong(status));
                } catch (NumberFormatException e) {
                  throw new IllegalArgumentException("success lists integer results: " + statuses);
                }
              }
            resources.add(
                new Resource(
                    words[1],
                    // Several constructors are a comma-separated list, each an import's name.
                    List.of(create.split(",", -1)),
                    destroy,
                    threading.equals("shared"),
                    handle,
                    success));
            empty(opts);
          }
          case "borrow" -> {
            require(
                name != null && header == null && words.length >= 3,
                "borrow requires FUNCTION PARAMETER");
            var opts = options(words, 3);
            // lifetime=call ends with the call; lifetime=resource:NAME lasts until the resource
            // the function creates is destroyed.
            String lifetime = takeRequired(opts, "lifetime");
            String holder = null;
            if (lifetime.startsWith("resource:")) holder = lifetime.substring("resource:".length());
            require(
                lifetime.equals("call") || (holder != null && !holder.isEmpty()),
                "lifetime is call or resource:RESOURCE: " + lifetime);
            // A batch names an array of lengths and a count; a single buffer names its length.
            if (opts.containsKey("lengths") || opts.containsKey("count")) {
              String elements = opts.remove("elements_nullable");
              require(
                  elements == null || elements.equals("true") || elements.equals("false"),
                  "elements_nullable is true or false: " + elements);
              require(
                  holder == null, "a batch borrow has lifetime=call: " + words[1] + " " + words[2]);
              borrows.add(
                  BufferBorrow.batch(
                      words[1],
                      words[2],
                      takeRequired(opts, "lengths"),
                      takeRequired(opts, "count"),
                      "true".equals(elements)));
            } else
              borrows.add(
                  new BufferBorrow(words[1], words[2], takeRequired(opts, "length"), holder));
            empty(opts);
          }
          case "opaque" -> {
            require(
                name != null && header == null && words.length == 2,
                "expected opaque NAME outside a block");
            opaque.add(words[1]);
          }
          case "const" -> {
            require(
                name != null && header == null && words.length >= 2,
                "expected const NAME type=INT value=INTEGER outside a block");
            var opts = options(words, 2);
            var type = integerScalar(takeRequired(opts, "type"));
            constants.add(new Constant(words[1], type, integer(takeRequired(opts, "value"))));
            empty(opts);
          }
          case "struct", "import", "export", "callback", "enum", "flags" -> {
            require(
                name != null && header == null && words.length >= 2,
                "expected a named top-level block");
            header = words;
            options = options(words, 2);
            parameters.clear();
            fields.clear();
            enumValues.clear();
            flagBits.clear();
            flagRanges.clear();
          }
          case "format", "node", "element" -> {
            require(
                name != null && header == null && words.length >= 2,
                "expected a named top-level block");
            header = words;
            options = options(words, 2);
            if (persisted == null) persisted = new PersistedDsl();
            persisted.open(words, i + 1);
          }
          case "value" -> {
            require(header != null && header[0].equals("enum"), "value requires an enum block");
            require(words.length == 4 && words[2].equals("="), "expected: value NAME = INTEGER");
            enumValues.add(new EnumValue(words[1], integer(words[3])));
          }
          case "bit" -> {
            require(header != null && header[0].equals("flags"), "bit requires a flags block");
            require(words.length == 4 && words[2].equals("="), "expected: bit NAME = INDEX");
            require(words[3].matches("[0-9]{1,9}"), "invalid bit index: " + words[3]);
            flagBits.add(new FlagBit(words[1], Integer.parseInt(words[3])));
          }
          case "range" -> {
            require(header != null && header[0].equals("flags"), "range requires a flags block");
            require(
                words.length >= 4 && words[2].equals("="),
                "expected: range NAME = LOW..HIGH [enum=ENUM]");
            require(
                words[3].matches("[0-9]{1,9}\\.\\.[0-9]{1,9}"), "invalid bit range: " + words[3]);
            String[] bounds = words[3].split("\\.\\.");
            var opts = options(words, 4);
            String enumName = take(opts, "enum", null);
            empty(opts);
            flagRanges.add(
                new FlagRange(
                    words[1], Integer.parseInt(bounds[0]), Integer.parseInt(bounds[1]), enumName));
          }
          case "param" -> {
            require(
                header != null
                    && (header[0].equals("import")
                        || header[0].equals("export")
                        || header[0].equals("callback"))
                    && words.length >= 3,
                "param requires a function block and a type");
            var opts = options(words, 2);
            var type = Seam.type(takeRequired(opts, "type"));
            boolean nullable = bool(take(opts, "nullable", "true"));
            var ownership =
                Ownership.valueOf(take(opts, "ownership", "unspecified").toUpperCase(Locale.ROOT));
            var access =
                Access.valueOf(take(opts, "access", "unspecified").toUpperCase(Locale.ROOT));
            var group = group(opts);
            parameters.add(
                new Parameter(words[1], type, nullable, ownership, access, facts(opts), group));
          }
          case "field" -> {
            if (persisted != null
                && header != null
                && (header[0].equals("node") || header[0].equals("element"))
                && words.length >= 3) {
              persisted.line(words, header, i + 1);
              continue;
            }
            require(
                header != null && header[0].equals("struct") && words.length >= 3,
                "field requires a struct block");
            var opts = options(words, 2);
            var type = Seam.type(takeRequired(opts, "type"));
            long offset = Long.parseLong(takeRequired(opts, "offset"));
            fields.add(new Field(words[1], type, offset, group(opts), take(opts, "count", null)));
            empty(opts);
          }
          case "when" -> {
            require(
                header != null
                    && header[0].equals("struct")
                    && ("field".equals(directive) || "when".equals(directive)),
                "when must directly follow a field or another when");
            String[] tag = words.length >= 2 ? words[1].split("=", 2) : new String[0];
            require(
                tag.length == 2 && !tag[0].isEmpty() && !tag[1].isEmpty(),
                "expected: when FIELD=VALUE enum=ENUM|flags=FLAGS");
            var opts = options(words, 2);
            var group = group(opts);
            require(group != null, "when requires enum= or flags=");
            empty(opts);
            fields.add(fields.removeLast().withCase(new FieldCase(tag[0], tag[1], group)));
          }
          case "end" -> {
            require(header != null && words.length == 1, "unexpected end");
            if (header[0].equals("callback")) {
              callbacks.add(
                  new Signature(
                      header[1],
                      Seam.type(takeRequired(options, "return")),
                      parameters,
                      take(options, "java_type", null),
                      take(options, "ffm_java", null),
                      take(options, "ffm_sentinel", null),
                      group(options)));
              empty(options);
            } else if (header[0].equals("enum")) {
              enums.add(
                  new EnumGroup(
                      header[1], integerScalar(takeRequired(options, "type")), enumValues));
              empty(options);
            } else if (header[0].equals("flags")) {
              var type = integerScalar(takeRequired(options, "type"));
              boolean bitfields = bool(take(options, "c_bitfields", "false"));
              flagSets.add(new FlagSet(header[1], type, flagBits, bitfields, flagRanges));
              empty(options);
            } else if (header[0].equals("format")
                || header[0].equals("node")
                || header[0].equals("element")) {
              persisted.end(header, options);
            } else if (header[0].equals("struct")) {
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
              String callback = take(options, "callback", null);
              String isolate = take(options, "isolate", "none");
              require(
                  isolate.equals("none") || isolate.equals("thread"),
                  "isolate must be none or thread");
              String cc = take(options, "cc", "c");
              require(cc.equals("c"), "only cc=c is supported");
              var returnsGroup = group(options);
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
                      callback,
                      isolate.equals("thread"),
                      facts(options),
                      returnsGroup));
            }
            header = null;
            options = null;
          }
          default -> {
            require(PersistedDsl.DIRECTIVES.contains(words[0]), "unknown directive: " + words[0]);
            require(name != null, "missing module");
            if (persisted == null) persisted = new PersistedDsl();
            persisted.line(words, header, i + 1);
          }
        }
      } catch (IllegalArgumentException e) {
        throw new IllegalArgumentException("line " + (i + 1) + ": " + e.getMessage(), e);
      }
    }
    require(name != null, "missing module");
    require(header == null, "unterminated block");
    return new Module(
        name,
        abi,
        target,
        javaPackage,
        javaClass,
        opaque,
        structs,
        callbacks,
        functions,
        resources,
        borrows,
        nativeConfig,
        truffleConfig,
        constants,
        enums,
        flagSets,
        abiDeclared,
        rustImplementation,
        persisted == null ? Persisted.Declarations.EMPTY : persisted.declarations());
  }

  /** Decimal or {@code 0x} hexadecimal; hex is a magnitude, so only decimal is negative. */
  public static BigInteger integer(String text) {
    if (text.matches("-?[0-9]+")) return new BigInteger(text);
    if (text.matches("0[xX][0-9a-fA-F]+")) return new BigInteger(text.substring(2), 16);
    throw new IllegalArgumentException("invalid integer: " + text);
  }

  private static Scalar integerScalar(String text) {
    // Any scalar parses; the validator rejects carriers that are not integers.
    if (Seam.type(text) instanceof Scalar s) return s;
    throw new IllegalArgumentException("carrier must be an integer scalar, found " + text);
  }

  /** Takes {@code enum=} or {@code flags=}, before the remaining options become facts. */
  static GroupRef group(Map<String, String> opts) {
    String enumName = take(opts, "enum", null), flagsName = take(opts, "flags", null);
    require(enumName == null || flagsName == null, "enum= and flags= are mutually exclusive");
    if (enumName != null) return new GroupRef(GroupKind.ENUM, enumName);
    return flagsName == null ? null : new GroupRef(GroupKind.FLAGS, flagsName);
  }

  static Map<String, String> options(String[] words, int start) {
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
    options.forEach((key, value) -> {
      String[] pieces = value.split("@", -1);
      require(pieces.length == 2, "contract " + key + " requires VALUE@PROVENANCE");
      result.put(
          key,
          new Fact(
              pieces[0], Provenance.valueOf(pieces[1].toUpperCase(Locale.ROOT)), "DSL contract"));
    });
    return result;
  }

  private static <E extends Enum<E>> E option(Class<E> type, String key, String value) {
    try {
      return Enum.valueOf(type, value.toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("invalid " + key + ": " + value, e);
    }
  }

  static String take(Map<String, String> opts, String key, String fallback) {
    String value = opts.remove(key);
    return value == null ? fallback : value;
  }

  static String takeRequired(Map<String, String> opts, String key) {
    String value = opts.remove(key);
    require(value != null, "missing " + key);
    return value;
  }

  static void empty(Map<String, String> opts) {
    require(opts.isEmpty(), "unknown options: " + opts.keySet());
  }

  static boolean bool(String value) {
    require(value.equals("true") || value.equals("false"), "expected true or false");
    return Boolean.parseBoolean(value);
  }

  static void require(boolean test, String message) {
    if (!test) throw new IllegalArgumentException(message);
  }
}
