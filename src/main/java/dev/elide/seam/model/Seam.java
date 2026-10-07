package dev.elide.seam.model;

import java.util.*;

/** Language-neutral, immutable ABI model. Source diagnostics are excluded from identity. */
public final class Seam {
  private Seam() {}

  public static final String VERSION = "0.3.0";

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

  public sealed interface Type permits Scalar, Pointer, Named, FunctionPointer, Isolate {
    String text();
  }

  public enum Scalar implements Type {
    VOID("void", 0),
    BOOL("bool", 1),
    /** C {@code char}: signed on x86-64 and Apple arm64, unsigned on AAPCS64 Linux. */
    CHAR("char", 1),
    I8("i8", 1),
    U8("u8", 1),
    I16("i16", 2),
    U16("u16", 2),
    I32("i32", 4),
    U32("u32", 4),
    I64("i64", 8),
    U64("u64", 8),
    /** Pointer-sized; every supported target is 64-bit. */
    USIZE("usize", 8),
    ISIZE("isize", 8),
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

  /** A C function pointer whose signature is a declared {@link Signature}. */
  public record FunctionPointer(String signature) implements Type {
    public String text() {
      return "fn<" + signature + ">";
    }
  }

  /** The Native Image isolate thread, carried through native code as an opaque pointer. */
  public enum Isolate implements Type {
    THREAD;

    public String text() {
      return "isolate_thread";
    }
  }

  public static Type type(String text) {
    for (Scalar s : Scalar.values()) if (s.text().equals(text)) return s;
    if (text.equals(Isolate.THREAD.text())) return Isolate.THREAD;
    if (text.startsWith("fn<") && text.endsWith(">"))
      return new FunctionPointer(text.substring(3, text.length() - 1));
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
      String callback,
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
              Isolate.THREAD,
              false,
              Ownership.BORROWED,
              Access.UNSPECIFIED,
              new TreeMap<>()));
      lowered.addAll(parameters);
      return List.copyOf(lowered);
    }
  }

  /** A callback signature, referenced as {@code fn<name>}. */
  public record Signature(String name, Type returns, List<Parameter> parameters) {
    public Signature {
      parameters = List.copyOf(parameters);
    }
  }

  public record Field(String name, Type type, long offset) {}

  public record Struct(String name, long size, int alignment, List<Field> fields) {
    public Struct {
      fields = List.copyOf(fields);
    }
  }

  /** Explicit lifecycle contract for a unique native allocation. Null creation means failure. */
  public record Resource(String name, String create, String destroy, boolean shared) {}

  /** A byte-buffer borrow valid only for the dynamic extent of one call. */
  public record BufferBorrow(String function, String parameter, String length) {}

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
      List<Signature> callbacks,
      List<Function> functions,
      List<Resource> resources,
      List<BufferBorrow> borrows) {
    public Module(
        String name,
        int abiVersion,
        String targetTriple,
        String javaPackage,
        String javaClass,
        List<String> opaqueTypes,
        List<Struct> structs,
        List<Function> functions) {
      this(
          name,
          abiVersion,
          targetTriple,
          javaPackage,
          javaClass,
          opaqueTypes,
          structs,
          List.of(),
          functions,
          List.of(),
          List.of());
    }

    public Module(
        String name,
        int abiVersion,
        String targetTriple,
        String javaPackage,
        String javaClass,
        List<String> opaqueTypes,
        List<Struct> structs,
        List<Signature> callbacks,
        List<Function> functions) {
      this(
          name,
          abiVersion,
          targetTriple,
          javaPackage,
          javaClass,
          opaqueTypes,
          structs,
          callbacks,
          functions,
          List.of(),
          List.of());
    }

    public Module {
      resources = resources.stream().sorted(Comparator.comparing(Resource::name)).toList();
      borrows = borrows.stream().sorted(Comparator.comparing(BufferBorrow::function)).toList();
      opaqueTypes = opaqueTypes.stream().sorted().toList();
      callbacks = callbacks.stream().sorted(Comparator.comparing(Signature::name)).toList();
      structs = structs.stream().sorted(Comparator.comparing(Struct::name)).toList();
      functions = functions.stream().sorted(Comparator.comparing(Function::symbol)).toList();
    }

    public Module withTarget(String target) {
      return new Module(
          name,
          abiVersion,
          target,
          javaPackage,
          javaClass,
          opaqueTypes,
          structs,
          callbacks,
          functions,
          resources,
          borrows);
    }

    public String javaPackageName() {
      return javaPackage == null ? DEFAULT_JAVA_PACKAGE : javaPackage;
    }

    /** The Native Image class; FFM and Feature classes take it as a prefix when it is named. */
    public String nativeClass() {
      return javaClass == null ? "SeamNative" : javaClass;
    }

    public String ownershipClass() {
      return javaClass == null ? "SeamOwnership" : javaClass + "Ownership";
    }

    public String ffmClass() {
      return javaClass == null ? "SeamFFM" : javaClass + "FFM";
    }

    public String foreignFeatureClass() {
      return javaClass == null ? "SeamForeignFeature" : javaClass + "ForeignFeature";
    }
  }
}
