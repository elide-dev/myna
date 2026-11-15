// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package dev.elide.myna.model;

import java.math.BigInteger;
import java.util.*;

/** Language-neutral, immutable ABI model. Source diagnostics are excluded from identity. */
public final class Seam {
  private Seam() {}

  public static final String VERSION = "0.18.0";
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
      SortedMap<String, Fact> facts,
      GroupRef group) {
    public Parameter(
        String name,
        Type type,
        boolean nullable,
        Ownership ownership,
        Access access,
        SortedMap<String, Fact> facts) {
      this(name, type, nullable, ownership, access, facts, null);
    }

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
      SortedMap<String, Fact> facts,
      GroupRef returnsGroup) {
    public Function(
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
      this(
          name,
          symbol,
          direction,
          returns,
          parameters,
          error,
          sentinel,
          javaTarget,
          include,
          callback,
          isolateThread,
          facts,
          null);
    }

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
      String ffmSentinel,
      GroupRef returnsGroup) {
    public Signature(String name, Type returns, List<Parameter> parameters) {
      this(name, returns, parameters, null, null, null, null);
    }

    public Signature(String name, Type returns, List<Parameter> parameters, String javaType) {
      this(name, returns, parameters, javaType, null, null, null);
    }

    public Signature(
        String name,
        Type returns,
        List<Parameter> parameters,
        String javaType,
        String ffmTarget,
        String ffmSentinel) {
      this(name, returns, parameters, javaType, ffmTarget, ffmSentinel, null);
    }

    public Signature {
      parameters = List.copyOf(parameters);
    }
  }

  /** A struct field; {@code group} names the enum or flag set annotating it, or is null. */
  public record Field(String name, Type type, long offset, GroupRef group) {
    public Field(String name, Type type, long offset) {
      this(name, type, offset, null);
    }
  }

  /** A named integer constant: {@code const NAME type=u32 value=0x10}. */
  public record Constant(String name, Scalar type, BigInteger value) {}

  public record EnumValue(String name, BigInteger value) {}

  /** A closed list of named values over an integer carrier; values are sorted by number. */
  public record EnumGroup(String name, Scalar type, List<EnumValue> values) {
    public EnumGroup {
      values = values.stream().sorted(Comparator.comparing(EnumValue::value)).toList();
    }
  }

  public record FlagBit(String name, int bit) {}

  /**
   * A multi-bit field of a flag word, bits {@code low..high} inclusive: {@code range kind = 8..15}.
   * {@code enumName} names the enum its values belong to, or is null for a raw number.
   */
  public record FlagRange(String name, int low, int high, String enumName) {
    public int width() {
      return high - low + 1;
    }

    /** The range's bits in place: {@code ((1 << width) - 1) << low}. */
    public BigInteger mask() {
      return valueMask().shiftLeft(low);
    }

    /** The range's bits shifted down to bit 0: {@code (1 << width) - 1}. */
    public BigInteger valueMask() {
      return BigInteger.ONE.shiftLeft(width()).subtract(BigInteger.ONE);
    }
  }

  /**
   * A set of named bit indices over an integer carrier; bits are sorted by index and ranges by
   * their low bit. {@code cBitfields} opts the set into a generated {@code @CBitfield} accessor
   * struct.
   */
  public record FlagSet(
      String name, Scalar type, List<FlagBit> bits, boolean cBitfields, List<FlagRange> ranges) {
    public FlagSet {
      bits = bits.stream().sorted(Comparator.comparingInt(FlagBit::bit)).toList();
      ranges = ranges.stream().sorted(Comparator.comparingInt(FlagRange::low)).toList();
    }

    public FlagSet(String name, Scalar type, List<FlagBit> bits, boolean cBitfields) {
      this(name, type, bits, cBitfields, List.of());
    }

    public FlagSet(String name, Scalar type, List<FlagBit> bits) {
      this(name, type, bits, false);
    }

    /** Every declared bit: the bits' masks and the ranges' masks. */
    public BigInteger all() {
      BigInteger all = BigInteger.ZERO;
      for (FlagBit b : bits) all = all.setBit(b.bit());
      for (FlagRange r : ranges) all = all.or(r.mask());
      return all;
    }
  }

  public enum GroupKind {
    ENUM,
    FLAGS
  }

  /** A reference from a field, parameter or return to a declared enum or flag set. */
  public record GroupRef(GroupKind kind, String name) {}

  public record Struct(String name, long size, int alignment, List<Field> fields) {
    public Struct {
      fields = List.copyOf(fields);
    }
  }

  /**
   * Explicit lifecycle contract for a unique native allocation.
   *
   * <p>Without a {@code handle}, the create function returns the resource pointer and null means
   * failure. With one, the create function reports through its result and writes the resource where
   * the handle says: {@code PARAMETER.FIELD} names a pointer field of the struct that parameter
   * points to, and a bare {@code PARAMETER} says the struct it points to is the resource itself,
   * initialized in place. {@code success} lists the results that mean the resource exists. Only
   * Truffle cells project the handle forms; the other ownership APIs leave those functions to their
   * raw bindings.
   */
  public record Resource(
      String name,
      String create,
      String destroy,
      boolean shared,
      String handle,
      List<Long> success) {
    public Resource {
      success = List.copyOf(success);
    }

    /** The parameter the create function writes through, or null for a returned pointer. */
    public String handleParameter() {
      if (handle == null) return null;
      int dot = handle.indexOf('.');
      return dot < 0 ? handle : handle.substring(0, dot);
    }

    /**
     * The field holding the resource pointer, or null when the pointed-to struct is the resource.
     */
    public String handleField() {
      if (handle == null) return null;
      int dot = handle.indexOf('.');
      return dot < 0 ? null : handle.substring(dot + 1);
    }
  }

  /**
   * A byte-buffer borrow. A function may declare several, each with a buffer and a length parameter
   * of its own.
   *
   * @param resource null for {@code lifetime=call}, a borrow valid only for the dynamic extent of
   *     one call; otherwise the resource whose create the function is, for {@code
   *     lifetime=resource:NAME}: the create keeps the buffer until that resource is destroyed
   */
  public record BufferBorrow(String function, String parameter, String length, String resource) {
    /** Whether the borrow outlives the call, held by the resource the function creates. */
    public boolean held() {
      return resource != null;
    }
  }

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

  /**
   * Whether a Truffle cell lends bytes to this parameter (buffers, strings) rather than passing an
   * address: a byte pointer with a declared access.
   */
  public static boolean lendsBytes(Parameter parameter) {
    return parameter.type() instanceof Pointer p
        && (p.pointee() == Scalar.U8 || p.pointee() == Scalar.I8 || p.pointee() == Scalar.CHAR)
        && (parameter.access() == Access.READ
            || parameter.access() == Access.READ_WRITE
            || parameter.access() == Access.WRITE);
  }

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
      TruffleConfig truffleConfig,
      List<Constant> constants,
      List<EnumGroup> enums,
      List<FlagSet> flagSets) {
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
        NativeConfig nativeConfig,
        TruffleConfig truffleConfig) {
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
          truffleConfig,
          List.of(),
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
      borrows =
          borrows.stream()
              .sorted(
                  Comparator.comparing(BufferBorrow::function)
                      .thenComparing(BufferBorrow::parameter))
              .toList();
      opaqueTypes = opaqueTypes.stream().sorted().toList();
      callbacks = callbacks.stream().sorted(Comparator.comparing(Signature::name)).toList();
      structs = structs.stream().sorted(Comparator.comparing(Struct::name)).toList();
      functions = functions.stream().sorted(Comparator.comparing(Function::symbol)).toList();
      constants = constants.stream().sorted(Comparator.comparing(Constant::name)).toList();
      enums = enums.stream().sorted(Comparator.comparing(EnumGroup::name)).toList();
      flagSets = flagSets.stream().sorted(Comparator.comparing(FlagSet::name)).toList();
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
          truffleConfig,
          constants,
          enums,
          flagSets);
    }

    public String javaPackageName() {
      return javaPackage == null ? DEFAULT_JAVA_PACKAGE : javaPackage;
    }

    /** The Native Image class; FFM and Feature classes take it as a prefix when it is named. */
    public String nativeClass() {
      return javaClass == null ? "MynaNative" : javaClass;
    }

    /**
     * The resources every ownership API wraps: those whose create returns the pointer. The handle
     * forms are projected by Truffle cells only.
     */
    public List<Resource> returnedResources() {
      return resources.stream().filter(r -> r.handle() == null).toList();
    }

    /**
     * The borrows that call-scoped wrappers project: every borrow of a function none of whose
     * buffers a resource holds. A held buffer outlives the call, which only a Truffle cell's owner
     * can express, so the other backends leave such a function to its raw binding.
     */
    public List<BufferBorrow> callBorrows() {
      var holding = new HashSet<String>();
      for (BufferBorrow b : borrows) if (b.held()) holding.add(b.function());
      return borrows.stream().filter(b -> !holding.contains(b.function())).toList();
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

    /** The dependency-free class of literal constants, flag predicates and name functions. */
    public String constantsClass() {
      return nativeClass() + "Constants";
    }

    /** The {@code @CContext} directives class of the Native Image output. */
    public String directivesClass() {
      return nativeClass() + "Directives";
    }

    public String truffleClass() {
      return javaClass == null ? "MynaTruffle" : javaClass + "Truffle";
    }
  }
}
