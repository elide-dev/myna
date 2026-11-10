// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package dev.elide.myna.model;

import java.util.*;

/** Language-neutral, immutable ABI model. Source diagnostics are excluded from identity. */
public final class Seam {
  private Seam() {}

  public static final String VERSION = "0.16.0";
  public static final int BINDING_ABI_VERSION = 2;

  /**
   * The target properties a seam's layouts and platform-neutral outputs depend on. Struct sizes,
   * alignments and offsets, scalar widths and Java carriers follow from these alone; the triple
   * itself only selects target-specific LLVM contracts (sub-word argument extension, by-value
   * lowering), which are not part of the fingerprint. {@code charUnsigned} is part of the ABI class
   * because the C {@code char} signedness is target-dependent (signed on x86-64 and Apple arm64,
   * unsigned on AAPCS64 Linux) and the Truffle cells' {@code char} coercion and result conversion
   * must observe it; the property is serialized into the canonical descriptor so equal classes
   * produce equal fingerprints and byte-identical platform-neutral outputs.
   */
  public record TargetAbi(String dataModel, String byteOrder, boolean charUnsigned) {
    public TargetAbi(String dataModel, String byteOrder) {
      this(dataModel, byteOrder, false);
    }

    @Override
    public String toString() {
      return dataModel + ", " + byteOrder + "-endian" + (charUnsigned ? ", unsigned char" : "");
    }
  }

  private static final TargetAbi LP64_LITTLE = new TargetAbi("LP64", "little");

  /** AAPCS64 Linux: LP64, little-endian, {@code char} is unsigned. */
  private static final TargetAbi LP64_LITTLE_AAPCS64 = new TargetAbi("LP64", "little", true);

  /** Supported triples and their resolved ABI class. Equal classes share fingerprints. */
  public static final Map<String, TargetAbi> TARGETS =
      Map.of(
          "aarch64-apple-darwin", LP64_LITTLE,
          "x86_64-apple-darwin", LP64_LITTLE,
          "aarch64-unknown-linux-gnu", LP64_LITTLE_AAPCS64,
          "x86_64-unknown-linux-gnu", LP64_LITTLE);

  public static TargetAbi targetAbi(String triple) {
    TargetAbi abi = triple == null ? null : TARGETS.get(triple);
    if (abi == null) throw new IllegalArgumentException("unsupported target: " + triple);
    return abi;
  }

  /** Binding ABI 2 makes bounded, nonblocking, non-reentrant native imports the default. */
  public static boolean nativeLeaf(Function function) {
    if (function.direction() != Direction.IMPORT) return false;
    Fact fact = function.facts().get("native_leaf");
    return fact == null || (fact.enabled() && fact.approved());
  }

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
  public record Signature(
      String name,
      Type returns,
      List<Parameter> parameters,
      String javaType,
      String ffmTarget,
      String ffmSentinel) {
    public Signature(String name, Type returns, List<Parameter> parameters) {
      this(name, returns, parameters, null, null, null);
    }

    public Signature(String name, Type returns, List<Parameter> parameters, String javaType) {
      this(name, returns, parameters, javaType, null, null);
    }

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

  /** Optional Native Image build integration, without dependencies on hosted SDK classes. */
  public record NativeConfig(String cContext, String cLibrary, boolean requireStatic) {}

  /** Which generated binding a Truffle cell calls. */
  public enum TruffleBackend {
    /** The Native Image bindings: inlined leaves, or the transitioning stub. */
    NATIVE,
    /** The FFM bindings, for consumers that also run on HotSpot. */
    FFM
  }

  /** How cells treat integer arguments outside a carrier's range. */
  public enum TruffleCoercion {
    STRICT,
    WRAP
  }

  /** How a cell over a transitioning Native Image binding reaches its AOT stub. */
  public enum TruffleTransitions {
    /** Compiled guest code calls the stub directly; needs elide-dev/graal#196 (25.5.5-elide.26). */
    DIRECT,
    /** The call sits behind a Truffle boundary, which any fork compiles, at a boundary's cost. */
    BOUNDARY
  }

  /** Opt-in Truffle cell generation for a module's imports. */
  public record TruffleConfig(
      TruffleBackend backend, TruffleCoercion coercion, TruffleTransitions transitions) {}

  /** Whether a module import gets a Truffle cell: every import does unless it opts out. */
  public static boolean truffleCell(Module module, Function function) {
    if (module.truffleConfig() == null || function.direction() != Direction.IMPORT) return false;
    Fact fact = function.facts().get("truffle_cell");
    return fact == null || fact.enabled();
  }

  public static final String DEFAULT_JAVA_PACKAGE = "dev.elide.myna.generated";

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
      List<BufferBorrow> borrows,
      NativeConfig nativeConfig,
      TruffleConfig truffleConfig) {
    public Module(
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
        List<BufferBorrow> borrows,
        NativeConfig nativeConfig) {
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
          resources,
          borrows,
          nativeConfig,
          null);
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
        List<Function> functions,
        List<Resource> resources,
        List<BufferBorrow> borrows) {
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
          resources,
          borrows,
          null,
          null);
    }

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
          borrows,
          nativeConfig,
          truffleConfig);
    }

    public String javaPackageName() {
      return javaPackage == null ? DEFAULT_JAVA_PACKAGE : javaPackage;
    }

    /** The Native Image class; FFM and Feature classes take it as a prefix when it is named. */
    public String nativeClass() {
      return javaClass == null ? "MynaNative" : javaClass;
    }

    public String ownershipClass() {
      return javaClass == null ? "MynaOwnership" : javaClass + "Ownership";
    }

    public String ffmClass() {
      return javaClass == null ? "MynaFFM" : javaClass + "FFM";
    }

    public String foreignFeatureClass() {
      return javaClass == null ? "MynaForeignFeature" : javaClass + "ForeignFeature";
    }

    public String truffleClass() {
      return javaClass == null ? "MynaTruffle" : javaClass + "Truffle";
    }
  }
}
