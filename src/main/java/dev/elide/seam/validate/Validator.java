package dev.elide.seam.validate;

import dev.elide.seam.model.Seam.*;
import dev.elide.seam.model.Seam.Module;
import java.math.BigInteger;
import java.util.*;

/** Rejects unsupported ABI shapes before any output is written. */
public final class Validator {
  private Validator() {}

  public static final Set<String> TARGETS =
      Set.of(
          "aarch64-apple-darwin",
          "x86_64-apple-darwin",
          "aarch64-unknown-linux-gnu",
          "x86_64-unknown-linux-gnu");
  private static final Set<String> PARAM_FACTS =
      Set.of(
          "nonnull", "nocapture", "readonly", "writeonly", "noalias", "align", "dereferenceable");
  private static final Set<String> FUNCTION_FACTS =
      Set.of("nounwind", "noreturn", "willreturn", "nofree", "native_leaf");
  private static final Set<String> KEYWORDS =
      Set.of(
          ("abstract assert auto boolean break byte case catch char class const continue default do"
               + " double else enum extern final finally float fn for goto if impl import in int"
               + " interface let long loop match mod move native new package private protected pub"
               + " public record ref return self short signed sizeof static strictfp struct super"
               + " switch synchronized this throw throws trait transient true false try type"
               + " typedef union unsigned unsafe use var void volatile while yield async await dyn"
               + " where null instanceof sealed permits non_sealed module requires exports opens"
               + " uses provides with transitive crate as Self macro macro_rules alignas alignof"
               + " constexpr consteval constinit delete friend inline namespace operator noexcept"
               + " nullptr template typename virtual wchar_t dynamic_cast static_cast"
               + " reinterpret_cast const_cast isolate_thread __padding ABI_FINGERPRINT SeamFFM"
               + " SeamNative SeamForeignFeature getClass hashCode equals toString clone finalize"
               + " wait notify notifyAll")
              .split(" "));

  public static void validate(Module module, boolean strict) {
    identifier(module.name());
    if (module.javaPackage() != null)
      for (String segment : module.javaPackage().split("\\.", -1)) identifier(segment);
    if (module.javaClass() != null) identifier(module.javaClass());
    require(module.abiVersion() > 0, "abi must be positive");
    require(
        module.targetTriple() != null && TARGETS.contains(module.targetTriple()),
        "unsupported or missing target; supported: " + new TreeSet<>(TARGETS));
    Set<String> types = new HashSet<>();
    for (String opaque : module.opaqueTypes()) {
      identifier(opaque);
      require(types.add(opaque), "duplicate type: " + opaque);
      require(!isScalarName(opaque), "type shadows scalar: " + opaque);
    }
    for (Struct struct : module.structs()) {
      identifier(struct.name());
      require(types.add(struct.name()), "duplicate type: " + struct.name());
      require(!isScalarName(struct.name()), "type shadows scalar: " + struct.name());
    }
    for (Struct struct : module.structs()) {
      require(!struct.fields().isEmpty(), "empty structs have no portable C ABI");
      require(struct.size() > 0 && struct.size() <= Integer.MAX_VALUE, "invalid struct size");
      long end = 0;
      int naturalAlignment = 1;
      Set<String> fields = new HashSet<>();
      for (Field field : struct.fields()) {
        identifier(field.name());
        require(fields.add(field.name()), "duplicate field: " + field.name());
        type(
            field.type(),
            types,
            new HashSet<>(module.callbacks().stream().map(Signature::name).toList()),
            false);
        require(field.type() != Isolate.THREAD, "isolate_thread is a parameter type");
        int size = size(field.type());
        int align = size;
        naturalAlignment = Math.max(naturalAlignment, align);
        require(
            field.offset() >= end && field.offset() % align == 0,
            "overlapping or misaligned field: " + field.name());
        require(
            field.offset() <= struct.size() - size, "field exceeds struct size: " + field.name());
        end = field.offset() + size;
      }
      require(
          struct.alignment() == naturalAlignment,
          "v1 requires natural struct alignment: " + struct.name());
      require(
          struct.size() >= end && struct.size() % struct.alignment() == 0,
          "incomplete struct layout: " + struct.name());
    }
    Set<String> signatures = new HashSet<>();
    for (Signature signature : module.callbacks()) {
      identifier(signature.name());
      require(types.add(signature.name()), "duplicate type: " + signature.name());
      require(!isScalarName(signature.name()), "type shadows scalar: " + signature.name());
      signatures.add(signature.name());
    }
    for (Signature signature : module.callbacks()) {
      type(signature.returns(), types, signatures, true);
      require(signature.returns() != Isolate.THREAD, "isolate_thread is a parameter type");
      Set<String> params = new HashSet<>();
      for (Parameter parameter : signature.parameters()) {
        identifier(parameter.name());
        require(params.add(parameter.name()), "duplicate parameter: " + parameter.name());
        type(parameter.type(), types, signatures, false);
        require(parameter.facts().isEmpty(), "contracts on callback parameters are not supported");
        // Native Image invokes with signed Java carriers; see NativeImage#widened.
        require(
            parameter.type() != Scalar.U8 && parameter.type() != Scalar.U16,
            "unsigned sub-32-bit callback parameters are not supported; use u32");
      }
    }
    Set<String> symbols = new HashSet<>(), logicalNames = new HashSet<>();
    for (Function function : module.functions()) {
      identifier(function.name());
      identifier(function.symbol());
      require(logicalNames.add(function.name()), "duplicate logical name: " + function.name());
      if (function.returns() instanceof Named || function.parameters().stream().anyMatch(p -> p.type() instanceof Named)) {
        require(
            function.parameters().stream().noneMatch(p -> p.name().equals("result"))
                || !(function.returns() instanceof Named),
            "parameter name 'result' is reserved for by-value record returns");
        require(function.callback() == null, "by-value records cannot cross a callback");
        require(
            module.functions().stream().noneMatch(o -> o.symbol().equals(function.symbol() + "_svmgen_ref")),
            "symbol collides with the by-value reference form of " + function.name());
      }
      String rustAlias = Character.toUpperCase(function.name().charAt(0)) + function.name().substring(1) + "Fn";
      require(!types.contains(rustAlias), "Rust alias " + rustAlias + " collides with a declared type");
      require(symbols.add(function.symbol()), "duplicate linker symbol: " + function.symbol());
      record(module, function.returns());
      if (!(function.returns() instanceof Named)) type(function.returns(), types, signatures, true);
      require(function.returns() != Isolate.THREAD, "isolate_thread is a parameter type");
      functionScalar(function.returns());
      facts(function.facts(), FUNCTION_FACTS, strict);
      require(
          !enabled(function.facts(), "native_leaf")
              || (function.direction() == Direction.IMPORT
                  && !enabled(function.facts(), "noreturn")),
          "native_leaf requires a returning native import");
      require(
          !(enabled(function.facts(), "noreturn") && enabled(function.facts(), "willreturn")),
          "noreturn contradicts willreturn");
      // An ordinary returning Java bridge cannot honor noreturn.
      require(
          !enabled(function.facts(), "noreturn") || function.direction() == Direction.IMPORT,
          "noreturn export adaptation is not supported");
      require(
          !enabled(function.facts(), "noreturn") || function.returns() == Scalar.VOID,
          "noreturn requires void return in v1");
      require(
          function.isolateThread() == (function.direction() == Direction.EXPORT),
          "exports require isolate=thread; imports require isolate=none");
      if (function.direction() == Direction.EXPORT) {
        require(
            function.javaTarget() != null
                && function.javaTarget().matches("(?:[a-zA-Z][\\w]*\\.)+[a-zA-Z][\\w]*"),
            "export requires java=qualified.Class.method");
        for (String part : function.javaTarget().split("\\.")) identifier(part);
        if (function.callback() != null) {
          var signature =
              module.callbacks().stream()
                  .filter(c -> c.name().equals(function.callback()))
                  .findFirst()
                  .orElseThrow(
                      () -> new IllegalArgumentException("unknown callback: " + function.callback()));
          require(
              signature.returns().equals(function.returns())
                  && signature.parameters().stream().map(Parameter::type).toList()
                      .equals(function.abiParameters().stream().map(Parameter::type).toList()),
              "export " + function.name() + " does not match callback " + signature.name());
        }
        if (function.include() != null) {
          require(
              function.include().matches("(?:[a-zA-Z][\\w]*\\.)+[a-zA-Z][\\w]*"),
              "include requires a qualified BooleanSupplier class");
          for (String part : function.include().split("\\.")) identifier(part);
        }
        require(
            function.error() != ErrorConvention.NO_FAILURE,
            "exports must translate exceptions: abort, integer_sentinel, or null_sentinel");
      } else {
        require(function.javaTarget() == null, "java target is only valid on exports");
        require(function.include() == null, "include is only valid on exports");
        require(function.callback() == null, "callback is only valid on exports");
        if (function.parameters().stream()
            .anyMatch(p -> p.type() == Scalar.U8 || p.type() == Scalar.U16))
          require(
              module.functions().stream()
                  .noneMatch(other -> other.name().equals(function.name() + "Native")),
              "logical name collides with the lowered Java helper " + function.name() + "Native");
        require(
            function.error() == ErrorConvention.NO_FAILURE,
            "imports describe an existing non-throwing C ABI; use error=no_failure");
      }
      if (function.error() == ErrorConvention.INTEGER_SENTINEL) {
        require(
            function.returns() == Scalar.I32 || function.returns() == Scalar.I64,
            "integer_sentinel requires i32 or i64");
        require(
            function.sentinel() != null && function.sentinel().matches("-?[0-9]+"),
            "missing or invalid sentinel");
        BigInteger value = new BigInteger(function.sentinel());
        int bits = ((Scalar) function.returns()).bytes * 8;
        require(
            value.compareTo(BigInteger.ONE.shiftLeft(bits - 1).negate()) >= 0
                && value.compareTo(BigInteger.ONE.shiftLeft(bits - 1).subtract(BigInteger.ONE))
                    <= 0,
            "sentinel out of range");
      } else require(function.sentinel() == null, "sentinel only applies to integer_sentinel");
      require(
          function.error() != ErrorConvention.NULL_SENTINEL
              || function.returns() instanceof Pointer,
          "null_sentinel requires pointer return");
      Set<String> params = new HashSet<>();
      for (Parameter parameter : function.parameters()) {
        identifier(parameter.name());
        require(params.add(parameter.name()), "duplicate parameter: " + parameter.name());
        record(module, parameter.type());
        if (!(parameter.type() instanceof Named)) type(parameter.type(), types, signatures, false);
        functionScalar(parameter.type());
        facts(parameter.facts(), PARAM_FACTS, strict);
        boolean pointer = parameter.type() instanceof Pointer;
        require(
            pointer || parameter.facts().isEmpty(),
            "pointer attributes on scalar: " + parameter.name());
        require(pointer || parameter.access() == Access.UNSPECIFIED, "access intent on scalar");
        require(pointer || parameter.ownership() == Ownership.UNSPECIFIED, "ownership on scalar");
        require(
            !enabled(parameter.facts(), "nonnull") || !parameter.nullable(),
            "nonnull contradicts nullable=true");
        require(
            !(enabled(parameter.facts(), "readonly") && enabled(parameter.facts(), "writeonly")),
            "readonly contradicts writeonly");
        require(
            !enabled(parameter.facts(), "readonly")
                || (parameter.access() != Access.WRITE && parameter.access() != Access.READ_WRITE),
            "readonly contradicts access");
        require(
            !enabled(parameter.facts(), "writeonly")
                || (parameter.access() != Access.READ && parameter.access() != Access.READ_WRITE),
            "writeonly contradicts access");
        require(
            !enabled(parameter.facts(), "nocapture")
                || (parameter.ownership() != Ownership.TRANSFERRED_TO_CALLEE
                    && parameter.ownership() != Ownership.OWNED_BY_CALLEE),
            "nocapture contradicts ownership");
        require(
            parameter.ownership() != Ownership.TRANSFERRED_TO_CALLER,
            "ownership transfer to caller must be modeled on a return; unsupported in v1");
        Fact align = parameter.facts().get("align");
        if (align != null) {
          long n = positive(align.value());
          require(n <= (1L << 29) && (n & (n - 1)) == 0, "align must be a power of two <= 2^29");
        }
        Fact deref = parameter.facts().get("dereferenceable");
        if (deref != null) {
          positive(deref.value());
          require(!parameter.nullable(), "dereferenceable implies non-null on supported targets");
        }
        Fact alias = parameter.facts().get("noalias");
        require(
            alias == null || !alias.enabled() || alias.approved(),
            "noalias requires explicit_contract or compiler_proven");
      }
    }
    OwnershipValidator.validate(module);
  }

  private static boolean isScalarName(String name) {
    for (Scalar scalar : Scalar.values()) if (scalar.text().equals(name)) return true;
    return false;
  }

  /** Small values cross with each target's extension rule ({@code Types.extension}). */
  private static void functionScalar(Type type) {}

  /** By-value records are function values only, and only declared structs (never opaque). */
  private static void record(Module module, Type type) {
    if (type instanceof Named n)
      require(
          module.structs().stream().anyMatch(st -> st.name().equals(n.name())),
          "by-value type must be a declared struct: " + n.name());
  }

  private static void type(
      Type type, Set<String> names, Set<String> signatures, boolean allowVoid) {
    if (type instanceof Pointer p) {
      require(
          !(p.pointee() instanceof FunctionPointer) && p.pointee() != Isolate.THREAD,
          "pointers to function pointers or isolates are not supported");
      if (p.pointee() instanceof Named n)
        require(names.contains(n.name()), "unknown pointee: " + n.name());
      else if (p.pointee() instanceof Pointer) type(p.pointee(), names, signatures, false);
    } else if (type instanceof FunctionPointer f)
      require(signatures.contains(f.signature()), "unknown callback: " + f.signature());
    else if (type instanceof Named)
      throw new IllegalArgumentException("by-value records are function parameters and returns only; use ptr<T>");
    else require(allowVoid || type != Scalar.VOID, "void parameter or field");
  }

  private static void facts(Map<String, Fact> facts, Set<String> allowed, boolean strict) {
    facts.forEach(
        (key, fact) -> {
          require(allowed.contains(key), "unknown or unsupported contract: " + key);
          if (!key.equals("align") && !key.equals("dereferenceable"))
            require(
                fact.value().equals("true") || fact.value().equals("false"),
                "boolean contract required for " + key);
          require(
              !strict || !fact.enabled() || fact.approved(),
              "unapproved provenance for " + key + ": " + fact.provenance());
        });
  }

  public static boolean enabled(Map<String, Fact> facts, String key) {
    Fact fact = facts.get(key);
    return fact != null && fact.enabled();
  }

  public static int size(Type type) {
    return type instanceof Scalar s ? s.bytes : 8;
  }

  private static long positive(String text) {
    long value = Long.parseLong(text);
    require(value > 0, "expected positive integer");
    return value;
  }

  private static void identifier(String name) {
    require(
        name != null
            && name.matches("[a-zA-Z][a-zA-Z0-9_]*")
            && !KEYWORDS.contains(name)
            && !name.startsWith("seamPadding"),
        "invalid or reserved identifier: " + name);
  }

  private static void require(boolean test, String message) {
    if (!test) throw new IllegalArgumentException(message);
  }
}
