package dev.elide.seam.model;

import java.util.*;

/** Language-neutral, immutable ABI model. Source diagnostics are excluded from identity. */
public final class Seam {
  private Seam() {}

  public static final String VERSION = "0.2.0";

  public enum Direction {
    IMPORT,
    EXPORT
  }

  public enum Ownership {
    BORROWED,
    OWNED_BY_CALLER,
    OWNED_BY_CALLEE,
    TRANSFERRED_TO_CALLEE,
    TRANSFERRED_TO_CALLER,
    UNSPECIFIED
  }

  public enum Access {
    NONE,
    READ,
    WRITE,
    READ_WRITE,
    UNSPECIFIED
  }

  public enum Provenance {
    EXPLICIT_CONTRACT,
    ABI_DERIVED,
    LANGUAGE_DERIVED,
    COMPILER_PROVEN,
    HEURISTIC
  }

  public enum ErrorConvention {
    NO_FAILURE,
    ABORT,
    INTEGER_SENTINEL,
    NULL_SENTINEL
  }

  public record Fact(String value, Provenance provenance, String explanation) {
    public boolean approved() {
      return provenance == Provenance.EXPLICIT_CONTRACT || provenance == Provenance.COMPILER_PROVEN;
    }

    public boolean enabled() {
      return !value.equals("false");
    }
  }

  public sealed interface Type permits Scalar, Pointer, Named {
    String text();
  }

  public enum Scalar implements Type {
    VOID("void", 0),
    I8("i8", 1),
    U8("u8", 1),
    I16("i16", 2),
    U16("u16", 2),
    I32("i32", 4),
    U32("u32", 4),
    I64("i64", 8),
    U64("u64", 8),
    F32("f32", 4),
    F64("f64", 8);
    private final String text;
    public final int bytes;

    Scalar(String text, int bytes) {
      this.text = text;
      this.bytes = bytes;
    }

    public String text() {
      return text;
    }
  }

  public record Pointer(Type pointee) implements Type {
    public String text() {
      return "ptr<" + pointee.text() + ">";
    }
  }

  public record Named(String name) implements Type {
    public String text() {
      return name;
    }
  }

  public static Type type(String text) {
    for (Scalar s : Scalar.values()) if (s.text().equals(text)) return s;
    if (text.startsWith("ptr<") && text.endsWith(">"))
      return new Pointer(type(text.substring(4, text.length() - 1)));
    if (!text.matches("[A-Za-z][A-Za-z0-9_]*"))
      throw new IllegalArgumentException("invalid type: " + text);
    return new Named(text);
  }

  public record Parameter(
      String name,
      Type type,
      boolean nullable,
      Ownership ownership,
      Access access,
      SortedMap<String, Fact> facts) {
    public Parameter {
      facts = Collections.unmodifiableSortedMap(new TreeMap<>(facts));
    }
  }

  public record Function(
      String name,
      String symbol,
      Direction direction,
      Type returns,
      List<Parameter> parameters,
      ErrorConvention error,
      String sentinel,
      String javaTarget,
      String include,
      boolean isolateThread,
      SortedMap<String, Fact> facts) {
    public Function {
      parameters = List.copyOf(parameters);
      facts = Collections.unmodifiableSortedMap(new TreeMap<>(facts));
    }

    /** The isolate argument is part of the public ABI, in every backend. */
    public List<Parameter> abiParameters() {
      if (!isolateThread) return parameters;
      var lowered = new ArrayList<Parameter>();
      lowered.add(
          new Parameter(
              "isolate_thread",
              new Pointer(Scalar.VOID),
              false,
              Ownership.BORROWED,
              Access.UNSPECIFIED,
              new TreeMap<>()));
      lowered.addAll(parameters);
      return List.copyOf(lowered);
    }
  }

  public record Field(String name, Type type, long offset) {}

  public record Struct(String name, long size, int alignment, List<Field> fields) {
    public Struct {
      fields = List.copyOf(fields);
    }
  }

  public static final String DEFAULT_JAVA_PACKAGE = "dev.elide.seam.generated";

  /** {@code javaPackage}/{@code javaClass} are null unless the descriptor names them. */
  public record Module(
      String name,
      int abiVersion,
      String targetTriple,
      String javaPackage,
      String javaClass,
      List<String> opaqueTypes,
      List<Struct> structs,
      List<Function> functions) {
    public Module {
      opaqueTypes = opaqueTypes.stream().sorted().toList();
      structs = structs.stream().sorted(Comparator.comparing(Struct::name)).toList();
      functions = functions.stream().sorted(Comparator.comparing(Function::symbol)).toList();
    }

    public Module withTarget(String target) {
      return new Module(
          name, abiVersion, target, javaPackage, javaClass, opaqueTypes, structs, functions);
    }

    public String javaPackageName() {
      return javaPackage == null ? DEFAULT_JAVA_PACKAGE : javaPackage;
    }

    /** The Native Image class; FFM and Feature classes take it as a prefix when it is named. */
    public String nativeClass() {
      return javaClass == null ? "SeamNative" : javaClass;
    }

    public String ffmClass() {
      return javaClass == null ? "SeamFFM" : javaClass + "FFM";
    }

    public String foreignFeatureClass() {
      return javaClass == null ? "SeamForeignFeature" : javaClass + "ForeignFeature";
    }
  }
}
