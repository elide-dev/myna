// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package dev.elide.myna.serialize;

import dev.elide.myna.model.Seam;
import dev.elide.myna.model.Seam.*;
import dev.elide.myna.model.Seam.Module;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

/** Canonical serialization with explicit provenance; no reflection or host-dependent values. */
public final class SeamJson {
  private SeamJson() {}

  /**
   * The target is recorded as its resolved ABI class, not its triple, so targets with identical
   * layouts produce identical descriptors, fingerprints and platform-neutral outputs.
   */
  public static String canonical(Module m) {
    TargetAbi abi = Seam.targetAbi(m.targetTriple());
    return "{\n  \"schemaVersion\": 3,\n  \"bindingAbiVersion\": 2,\n  \"generatorVersion\": "
        + quote(Seam.VERSION)
        + ",\n  \"name\": "
        + quote(m.name())
        + ",\n  \"abiVersion\": "
        + m.abiVersion()
        + ",\n  \"targetAbi\": {\"dataModel\":"
        + quote(abi.dataModel())
        + ",\"byteOrder\":"
        + quote(abi.byteOrder())
        + (abi.charUnsigned() ? ",\"charUnsigned\":true" : "")
        + "}"
        + java(m)
        + nativeConfig(m)
        + truffle(m)
        + ",\n  \"opaqueTypes\": "
        + array(m.opaqueTypes().stream().map(SeamJson::quote).toList())
        + ",\n  \"structs\": "
        + array(
            m.structs().stream()
                .map(s ->
                    "{\"name\":"
                        + quote(s.name())
                        + ",\"size\":"
                        + s.size()
                        + ",\"alignment\":"
                        + s.alignment()
                        + ",\"fields\":"
                        + array(
                            s.fields().stream()
                                .map(f ->
                                    "{\"name\":"
                                        + quote(f.name())
                                        + ",\"type\":"
                                        + quote(f.type().text())
                                        + ",\"offset\":"
                                        + f.offset()
                                        + reference(f.group(), "")
                                        + (f.count() == null
                                            ? ""
                                            : ",\"count\":" + quote(f.count()))
                                        + cases(f)
                                        + "}")
                                .toList())
                        + "}")
                .toList())
        + ",\n  \"callbacks\": "
        + array(
            m.callbacks().stream()
                .map(c ->
                    "{\"name\":"
                        + quote(c.name())
                        + (c.javaType() == null ? "" : ",\"javaType\":" + quote(c.javaType()))
                        + (c.ffmTarget() == null
                            ? ""
                            : ",\"ffmTarget\":"
                                + quote(c.ffmTarget())
                                + ",\"ffmSentinel\":"
                                + quote(c.ffmSentinel()))
                        + ",\"returnType\":"
                        + quote(c.returns().text())
                        + ",\"parameters\":"
                        + array(c.parameters().stream().map(SeamJson::parameter).toList())
                        + reference(c.returnsGroup(), "return")
                        + "}")
                .toList())
        + ",\n  \"functions\": "
        + array(m.functions().stream().map(SeamJson::function).toList())
        + groups(m)
        + ownership(m)
        + "\n}\n";
  }

  private static String nativeConfig(Module m) {
    NativeConfig c = m.nativeConfig();
    if (c == null) return "";
    return ",\n  \"nativeConfig\": {\"cContext\":"
        + quote(c.cContext())
        + ",\"cLibrary\":"
        + quote(c.cLibrary())
        + ",\"requireStatic\":"
        + c.requireStatic()
        + "}";
  }

  /** Omitted unless cells are requested, so existing descriptors keep their fingerprints. */
  private static String truffle(Module m) {
    TruffleConfig c = m.truffleConfig();
    if (c == null) return "";
    return ",\n  \"truffle\": {\"cells\":true,\"backend\":"
        + quote(c.backend().name())
        + ",\"coercion\":"
        + quote(c.coercion().name())
        + ",\"transitions\":"
        + quote(c.transitions().name())
        + "}";
  }

  private static String ownership(Module m) {
    String result = "";
    if (!m.resources().isEmpty())
      result +=
          ",\n  \"resources\": "
              + array(
                  m.resources().stream()
                      .map(r ->
                          "{\"name\":"
                              + quote(r.name())
                              + ",\"representation\":\"native\",\"threading\":"
                              + quote(r.shared() ? "shared" : "confined")
                              // One constructor keeps its string form; several are a list.
                              + ",\"create\":"
                              + (r.creates().size() == 1
                                  ? quote(r.creates().getFirst())
                                  : "["
                                      + String.join(
                                          ",", r.creates().stream().map(SeamJson::quote).toList())
                                      + "]")
                              + ",\"destroy\":"
                              + quote(r.destroy())
                              + (r.handle() == null
                                  ? ""
                                  : ",\"handle\":"
                                      + quote(r.handle())
                                      + ",\"success\":["
                                      + String.join(
                                          ",", r.success().stream().map(String::valueOf).toList())
                                      + "]")
                              + "}")
                      .toList());
    if (!m.borrows().isEmpty())
      result +=
          ",\n  \"borrows\": "
              + array(
                  m.borrows().stream()
                      .map(b ->
                          "{\"function\":"
                              + quote(b.function())
                              + ",\"parameter\":"
                              + quote(b.parameter())
                              // A batch names its arrays and count; single buffers keep their
                              // form.
                              + (b.batch()
                                  ? ",\"lengths\":"
                                      + quote(b.length())
                                      + ",\"count\":"
                                      + quote(b.count())
                                      + ",\"elementsNullable\":"
                                      + b.elementsNullable()
                                  : ",\"length\":" + quote(b.length()))
                              // A held borrow names its resource; call borrows keep their form.
                              + (b.held()
                                  ? ",\"lifetime\":\"resource\",\"resource\":" + quote(b.resource())
                                  : ",\"lifetime\":\"call\"")
                              + "}")
                      .toList());
    return result;
  }

  /** Omitted unless named, so descriptors without Java naming keep their fingerprints. */
  private static String java(Module m) {
    if (m.javaPackage() == null && m.javaClass() == null) return "";
    return ",\n  \"java\": {\"package\":"
        + quote(m.javaPackageName())
        + ",\"class\":"
        + quote(m.nativeClass())
        + "}";
  }

  private static String function(Function f) {
    return "{\"logicalName\":"
        + quote(f.name())
        + ",\"symbolName\":"
        + quote(f.symbol())
        + ",\"direction\":"
        + quote(f.direction().name())
        + (f.direction() == Direction.IMPORT
            ? ",\"nativeCallPolicy\":" + quote(Seam.nativeLeaf(f) ? "INLINE_LEAF" : "TRANSITION")
            : "")
        + ",\"callingConvention\":\"C\",\"returnType\":"
        + quote(f.returns().text())
        + ",\"errorConvention\":"
        + quote(f.error().name())
        + ",\"sentinel\":"
        + quote(f.sentinel())
        + ",\"javaTarget\":"
        + quote(f.javaTarget())
        + (f.include() == null ? "" : ",\"include\":" + quote(f.include()))
        + (f.callback() == null ? "" : ",\"callback\":" + quote(f.callback()))
        + ",\"isolateThread\":"
        + f.isolateThread()
        + ",\"parameters\":"
        + array(f.parameters().stream().map(SeamJson::parameter).toList())
        + ",\"abiParameters\":"
        + array(f.abiParameters().stream().map(SeamJson::parameter).toList())
        + ",\"facts\":"
        + facts(f.facts())
        + reference(f.returnsGroup(), "return")
        + "}";
  }

  /** Constants, enums and flag sets; each section is omitted when empty. Values are decimal. */
  private static String groups(Module m) {
    String result = "";
    if (!m.constants().isEmpty())
      result +=
          ",\n  \"constants\": "
              + array(
                  m.constants().stream()
                      .map(c ->
                          "{\"name\":"
                              + quote(c.name())
                              + ",\"type\":"
                              + quote(c.type().text())
                              + ",\"value\":"
                              + quote(c.value().toString())
                              + "}")
                      .toList());
    if (!m.enums().isEmpty())
      result +=
          ",\n  \"enums\": "
              + array(
                  m.enums().stream()
                      .map(e ->
                          "{\"name\":"
                              + quote(e.name())
                              + ",\"type\":"
                              + quote(e.type().text())
                              + ",\"values\":"
                              + array(
                                  e.values().stream()
                                      .map(v ->
                                          "{\"name\":"
                                              + quote(v.name())
                                              + ",\"value\":"
                                              + quote(v.value().toString())
                                              + "}")
                                      .toList())
                              + "}")
                      .toList());
    if (!m.flagSets().isEmpty())
      result +=
          ",\n  \"flagSets\": "
              + array(
                  m.flagSets().stream()
                      .map(f ->
                          "{\"name\":"
                              + quote(f.name())
                              + ",\"type\":"
                              + quote(f.type().text())
                              + (f.cBitfields() ? ",\"cBitfields\":true" : "")
                              + ",\"bits\":"
                              + array(
                                  f.bits().stream()
                                      .map(b ->
                                          "{\"name\":"
                                              + quote(b.name())
                                              + ",\"bit\":"
                                              + b.bit()
                                              + "}")
                                      .toList())
                              + ranges(f)
                              + "}")
                      .toList());
    return result;
  }

  /** A field's tag cases, omitted when it has none so other fields keep their fingerprints. */
  private static String cases(Field f) {
    if (f.cases().isEmpty()) return "";
    return ",\"cases\":"
        + array(
            f.cases().stream()
                .map(c ->
                    "{\"tag\":"
                        + quote(c.tagField())
                        + ",\"value\":"
                        + quote(c.value())
                        + reference(c.group(), "")
                        + "}")
                .toList());
  }

  /** A flag set's bit ranges, omitted when it has none so older sets keep their fingerprints. */
  private static String ranges(FlagSet f) {
    if (f.ranges().isEmpty()) return "";
    return ",\"ranges\":"
        + array(
            f.ranges().stream()
                .map(r ->
                    "{\"name\":"
                        + quote(r.name())
                        + ",\"low\":"
                        + r.low()
                        + ",\"high\":"
                        + r.high()
                        + (r.enumName() == null ? "" : ",\"enum\":" + quote(r.enumName()))
                        + "}")
                .toList());
  }

  /** Appended only when present: {@code "enum"}/{@code "flags"}, or {@code "returnEnum"} etc. */
  private static String reference(GroupRef group, String prefix) {
    if (group == null) return "";
    String key = group.kind() == GroupKind.ENUM ? "Enum" : "Flags";
    key = prefix.isEmpty() ? key.toLowerCase(Locale.ROOT) : prefix + key;
    return ",\"" + key + "\":" + quote(group.name());
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
        + reference(p.group(), "")
        + "}";
  }

  private static String facts(Map<String, Fact> facts) {
    var pieces = new ArrayList<String>();
    facts.forEach((name, fact) ->
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
    return fingerprintOf(canonical(module));
  }

  /** The fingerprint of an already serialized {@link #canonical(Module) canonical} descriptor. */
  public static String fingerprintOf(String canonical) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256")
                  .digest(canonical.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new AssertionError(e);
    }
  }

  private static String array(List<String> pieces) {
    return "[" + String.join(",", pieces) + "]";
  }

  public static String quote(String value) {
    if (value == null) return "null";
    boolean escaped = false;
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      if (c < 32 || c == '"' || c == '\\') {
        escaped = true;
        break;
      }
    }
    if (!escaped) return "\"" + value + "\"";
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
