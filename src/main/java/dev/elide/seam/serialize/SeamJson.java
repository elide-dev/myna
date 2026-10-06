package dev.elide.seam.serialize;

import dev.elide.seam.model.Seam;
import dev.elide.seam.model.Seam.*;
import dev.elide.seam.model.Seam.Module;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

/** Canonical serialization with explicit provenance; no reflection or host-dependent values. */
public final class SeamJson {
  private SeamJson() {}

  public static String canonical(Module m) {
    return "{\n  \"schemaVersion\": 1,\n  \"generatorVersion\": "
        + quote(Seam.VERSION)
        + ",\n  \"name\": "
        + quote(m.name())
        + ",\n  \"abiVersion\": "
        + m.abiVersion()
        + ",\n  \"targetTriple\": "
        + quote(m.targetTriple())
        + ",\n  \"opaqueTypes\": "
        + array(m.opaqueTypes().stream().map(SeamJson::quote).toList())
        + ",\n  \"structs\": "
        + array(
            m.structs().stream()
                .map(
                    s ->
                        "{\"name\":"
                            + quote(s.name())
                            + ",\"size\":"
                            + s.size()
                            + ",\"alignment\":"
                            + s.alignment()
                            + ",\"fields\":"
                            + array(
                                s.fields().stream()
                                    .map(
                                        f ->
                                            "{\"name\":"
                                                + quote(f.name())
                                                + ",\"type\":"
                                                + quote(f.type().text())
                                                + ",\"offset\":"
                                                + f.offset()
                                                + "}")
                                    .toList())
                            + "}")
                .toList())
        + ",\n  \"functions\": "
        + array(m.functions().stream().map(SeamJson::function).toList())
        + "\n}\n";
  }

  private static String function(Function f) {
    return "{\"logicalName\":"
        + quote(f.name())
        + ",\"symbolName\":"
        + quote(f.symbol())
        + ",\"direction\":"
        + quote(f.direction().name())
        + ",\"callingConvention\":\"C\",\"returnType\":"
        + quote(f.returns().text())
        + ",\"errorConvention\":"
        + quote(f.error().name())
        + ",\"sentinel\":"
        + quote(f.sentinel())
        + ",\"javaTarget\":"
        + quote(f.javaTarget())
        + ",\"isolateThread\":"
        + f.isolateThread()
        + ",\"parameters\":"
        + array(f.parameters().stream().map(SeamJson::parameter).toList())
        + ",\"abiParameters\":"
        + array(f.abiParameters().stream().map(SeamJson::parameter).toList())
        + ",\"facts\":"
        + facts(f.facts())
        + "}";
  }

  private static String parameter(Parameter p) {
    return "{\"name\":"
        + quote(p.name())
        + ",\"type\":"
        + quote(p.type().text())
        + ",\"nullable\":"
        + p.nullable()
        + ",\"ownership\":"
        + quote(p.ownership().name())
        + ",\"access\":"
        + quote(p.access().name())
        + ",\"facts\":"
        + facts(p.facts())
        + "}";
  }

  private static String facts(Map<String, Fact> facts) {
    var pieces = new ArrayList<String>();
    facts.forEach(
        (name, fact) ->
            pieces.add(
                quote(name)
                    + ":{\"value\":"
                    + quote(fact.value())
                    + ",\"provenance\":"
                    + quote(fact.provenance().name())
                    + ",\"explanation\":"
                    + quote(fact.explanation())
                    + "}"));
    return "{" + String.join(",", pieces) + "}";
  }

  public static String fingerprint(Module module) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256")
                  .digest(canonical(module).getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new AssertionError(e);
    }
  }

  private static String array(List<String> pieces) {
    return "[" + String.join(",", pieces) + "]";
  }

  public static String quote(String value) {
    if (value == null) return "null";
    var out = new StringBuilder("\"");
    for (char c : value.toCharArray()) {
      switch (c) {
        case '"' -> out.append("\\\"");
        case '\\' -> out.append("\\\\");
        case '\n' -> out.append("\\n");
        case '\r' -> out.append("\\r");
        case '\t' -> out.append("\\t");
        default -> {
          if (c < 32) out.append(String.format(Locale.ROOT, "\\u%04x", (int) c));
          else out.append(c);
        }
      }
    }
    return out.append('"').toString();
  }
}
