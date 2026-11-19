// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package dev.elide.myna.validate;

import dev.elide.myna.model.Seam;
import dev.elide.myna.model.Seam.*;
import dev.elide.myna.model.Seam.Module;
import dev.elide.myna.serialize.Fingerprint;
import java.math.BigInteger;
import java.util.*;
import java.util.regex.Pattern;

/** Rejects unsupported ABI shapes before any output is written. */
public final class Validator {
  private Validator() {}

  public static final Set<String> TARGETS = Seam.TARGETS.keySet();
  private static final Set<String> PARAM_FACTS =
      Set.of(
          "nonnull",
          "nocapture",
          "readonly",
          "writeonly",
          "noalias",
          "align",
          "dereferenceable",
          "truffle_buffer");
  private static final Set<String> FUNCTION_FACTS =
      Set.of(
          "nounwind",
          "noreturn",
          "willreturn",
          "nofree",
          "native_leaf",
          "truffle_cell",
          "truffle_raw_pointers");
  private static final Pattern IDENTIFIER = Pattern.compile("[a-zA-Z][a-zA-Z0-9_]*");

  /**
   * Methods a Native Image record inherits from {@code PointerBase}: a field accessor of the same
   * name would redeclare or overload a word operation.
   */
  private static final Set<String> WORD_METHODS =
      Set.of("isNull", "isNonNull", "rawValue", "equal", "notEqual");

  /**
   * Every declared type is a nested type of the Native Image class, so it must not shadow a name
   * that class imports or uses unqualified. (Its own name, and the {@code <function>_Leaf} and
   * {@code <function>_Binding} classes it declares, are checked where they are known.)
   */
  static final Set<String> NATIVE_IMAGE_NAMES =
      Set.of(
          ("AlwaysInline Uninterruptible CGlobalData CGlobalDataFactory CFunction CFunctionPointer"
               + " InvokeCFunctionPointer CEntryPoint CEntryPointLiteral CContext CLibrary CConst"
               + " CStruct CField CFieldAddress CConstant AllowNarrowingCast PointerBase CEnum"
               + " CEnumConstant CEnumValue CEnumLookup CBitfield WordFactory IsolateThread"
               + " VoidPointer WordPointer CCharPointer CCharPointerPointer CShortPointer"
               + " CIntPointer CLongPointer CFloatPointer CDoublePointer String Throwable")
              .split(" "));

  private static final Set<String> KEYWORDS =
      Set.of(
          ("abstract assert auto boolean break byte case catch char class const continue default do"
               + " double else enum extern final finally float fn for goto if impl import in int"
               + " interface let long loop match mod move native new package private protected pub"
               + " public record ref return self short signed sizeof static strictfp struct super"
               + " switch synchronized this throw throws trait transient true false try type"
               + " typedef union unsigned unsafe use var void volatile while yield async await dyn"
               + " where null instanceof sealed permits non_sealed module requires exports opens"
               + " box become override priv typeof unsized gen uses provides with transitive crate"
               + " as Self macro macro_rules alignas alignof constexpr consteval constinit delete"
               + " friend inline namespace operator noexcept nullptr template typename virtual"
               + " wchar_t dynamic_cast static_cast reinterpret_cast const_cast isolate_thread"
               + " __padding ABI_FINGERPRINT BINDING_ABI_VERSION MynaSymbols Literals MynaFFM"
               + " MynaNative MynaForeignFeature MynaTruffle LibraryBinding CriticalLibraryBinding"
               + " getClass hashCode equals toString clone finalize wait notify notifyAll")
              .split(" "));

  /**
   * Names that declared types (opaque, struct, callback) may not take because they are emitted as
   * crate-root Rust type names beside generated code that depends on them: {@code core} backs the
   * unconditional {@code core::mem::...} ABI assertions, and {@code Option} backs the {@code
   * Option<...>} callback alias and the {@code Option<Self>} ownership API. Function and parameter
   * names live in Rust's value namespace and do not collide, so they stay governed by {@link
   * #identifier(String)}.
   */
  static final Set<String> RESERVED_TYPE_NAMES = Set.of("core", "Option");

  public static void validate(Module module, boolean strict) {
    validate(module, strict, new Fingerprint(module));
  }

  /**
   * {@link #validate(Module, boolean)} given the generation's fingerprint of exactly {@code
   * module}, which validation reads only when a name it checks derives from it. Throws if {@code
   * fingerprint} describes another module instance.
   */
  public static void validate(Module module, boolean strict, Fingerprint fingerprint) {
    fingerprint.requireFor(module);
    identifier(module.name());
    if (module.javaPackage() != null)
      for (String segment : module.javaPackage().split("\\.", -1)) identifier(segment);
    if (module.javaClass() != null) {
      identifier(module.javaClass());
      // The class imports these names, so it cannot itself take one.
      require(
          !NATIVE_IMAGE_NAMES.contains(module.javaClass()),
          "java_class shadows generated name: " + module.javaClass());
    }
    if (module.nativeConfig() != null) {
      var config = module.nativeConfig();
      if (config.cContext() != null) {
        qualifiedType(config.cContext());
        // The generated context delegates to this one; naming itself would recurse.
        String generated = module.javaPackageName() + "." + module.directivesClass();
        require(
            !config.cContext().equals(generated),
            "c_context names the generated directives class " + generated);
      }
      if (config.cLibrary() != null)
        require(
            config.cLibrary().matches("[A-Za-z0-9_][A-Za-z0-9_.+-]*"), "invalid C library name");
      require(
          !config.requireStatic() || config.cLibrary() != null,
          "static linkage requires a C library");
      require(
          module.functions().stream().anyMatch(f -> f.direction() == Direction.IMPORT),
          "native linkage configuration requires imports");
    }
    require(module.abiVersion() > 0, "abi must be positive");
    require(
        module.targetTriple() != null && TARGETS.contains(module.targetTriple()),
        "unsupported or missing target; supported: " + new TreeSet<>(TARGETS));
    Set<String> types = new HashSet<>();
    for (String opaque : module.opaqueTypes()) {
      identifier(opaque);
      require(types.add(opaque), "duplicate type: " + opaque);
      require(!isScalarName(opaque), "type shadows scalar: " + opaque);
      require(!RESERVED_TYPE_NAMES.contains(opaque), "type shadows generated name: " + opaque);
      nativeImageType(module, opaque);
    }
    for (Struct struct : module.structs()) {
      identifier(struct.name());
      require(types.add(struct.name()), "duplicate type: " + struct.name());
      require(!isScalarName(struct.name()), "type shadows scalar: " + struct.name());
      require(
          !RESERVED_TYPE_NAMES.contains(struct.name()),
          "type shadows generated name: " + struct.name());
      nativeImageType(module, struct.name());
    }
    // Enum and flag-set names are types of the generated code, so every generated-name check
    // below sees them; they are not pointees, so references use `pointable`.
    Set<String> groups = new HashSet<>();
    for (String group :
        java.util.stream.Stream.concat(
                module.enums().stream().map(EnumGroup::name),
                module.flagSets().stream().map(FlagSet::name))
            .toList()) {
      identifier(group);
      require(types.add(group), "duplicate type: " + group);
      require(!isScalarName(group), "type shadows scalar: " + group);
      require(!RESERVED_TYPE_NAMES.contains(group), "type shadows generated name: " + group);
      nativeImageType(module, group);
      groups.add(group);
    }
    Set<String> signatures = new HashSet<>();
    for (Signature signature : module.callbacks()) {
      identifier(signature.name());
      if (signature.javaType() != null) qualifiedType(signature.javaType());
      require(
          (signature.ffmTarget() == null) == (signature.ffmSentinel() == null),
          "FFM callback target and sentinel must be declared together");
      if (signature.ffmTarget() != null) {
        qualifiedType(signature.ffmTarget());
        require(
            signature.returns() == Scalar.I32 || signature.returns() == Scalar.I64,
            "static FFM callbacks require an i32/i64 status return");
        try {
          if (!Ranges.fits(
              (Scalar) signature.returns(),
              new BigInteger(signature.ffmSentinel()),
              Seam.targetAbi(module.targetTriple()))) throw new NumberFormatException();
        } catch (NumberFormatException invalid) {
          throw new IllegalArgumentException("FFM callback sentinel is outside its carrier range");
        }
      }
      require(types.add(signature.name()), "duplicate type: " + signature.name());
      require(!isScalarName(signature.name()), "type shadows scalar: " + signature.name());
      require(
          !RESERVED_TYPE_NAMES.contains(signature.name()),
          "type shadows generated name: " + signature.name());
      nativeImageType(module, signature.name());
      signatures.add(signature.name());
    }
    Set<String> pointable = types;
    if (!groups.isEmpty()) {
      pointable = new HashSet<>(types);
      pointable.removeAll(groups);
    }
    for (Signature signature : module.callbacks()) {
      for (String generated : List.of(signature.name() + "Callback", signature.name() + "Upcalls"))
        require(
            !types.contains(generated) && !module.ffmClass().equals(generated),
            "type collides with generated FFM callback helper: " + generated);
    }
    // The JDK FFM linker rejects by-value struct layouts that are not the natural C ABI, so
    // imports (which keep the real by-value layout, unlike _myna_ref-lowered exports) need
    // canonical structs or the generated <clinit> downcallHandle throws
    // ExceptionInInitializerError.
    // The Truffle class is generated whenever cells are configured, even with no cell in it.
    if (module.truffleConfig() != null)
      require(
          !types.contains(module.truffleClass()),
          "type collides with generated Truffle class " + module.truffleClass());
    Set<String> byValueImportStructs = new HashSet<>();
    for (Function function : module.functions()) {
      if (function.direction() != Direction.IMPORT) continue;
      if (function.returns() instanceof Named n) byValueImportStructs.add(n.name());
      for (Parameter p : function.parameters())
        if (p.type() instanceof Named n) byValueImportStructs.add(n.name());
    }
    for (Struct struct : module.structs()) {
      require(!struct.fields().isEmpty(), "empty structs have no portable C ABI");
      require(struct.size() > 0 && struct.size() <= Integer.MAX_VALUE, "invalid struct size");
      long end = 0;
      int naturalAlignment = 1;
      Set<String> fields = new HashSet<>();
      boolean addressNames = false;
      for (Field field : struct.fields()) {
        identifier(field.name());
        require(fields.add(field.name()), "duplicate field: " + field.name());
        addressNames |=
            Character.isUpperCase(field.name().charAt(0)) || field.name().startsWith("addressOf");
        require(
            !WORD_METHODS.contains(field.name()),
            "field collides with a Native Image word method: " + field.name());
        type(field.type(), pointable, signatures, false);
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
      // The record's Native Image interface: one getter/setter name per field, one address
      // accessor per field, all distinct (fields x and X would both make addressOfX). Only a field
      // that starts upper-case or with addressOf can make such a collision.
      if (addressNames) {
        Set<String> members = new HashSet<>(fields);
        for (Field field : struct.fields()) {
          String address =
              "addressOf"
                  + Character.toUpperCase(field.name().charAt(0))
                  + field.name().substring(1);
          require(
              members.add(address),
              "field collides with the Native Image address accessor "
                  + address
                  + " of "
                  + field.name());
        }
      }
      require(
          struct.alignment() == naturalAlignment,
          "v1 requires natural struct alignment: " + struct.name());
      require(
          struct.size() >= end && struct.size() % struct.alignment() == 0,
          "incomplete struct layout: " + struct.name());
      require(
          !byValueImportStructs.contains(struct.name()) || canonical(struct),
          "by-value import struct must match the natural C layout (the FFM linker rejects reserved"
              + " gaps and over-declared sizes): "
              + struct.name());
    }
    for (Signature signature : module.callbacks()) {
      type(signature.returns(), pointable, signatures, true);
      require(signature.returns() != Isolate.THREAD, "isolate_thread is a parameter type");
      Set<String> params = new HashSet<>();
      for (Parameter parameter : signature.parameters()) {
        identifier(parameter.name());
        require(params.add(parameter.name()), "duplicate parameter: " + parameter.name());
        type(parameter.type(), pointable, signatures, false);
        require(parameter.facts().isEmpty(), "contracts on callback parameters are not supported");
        // Native Image invokes with signed Java carriers; see NativeImage#widened.
        require(
            parameter.type() != Scalar.U8 && parameter.type() != Scalar.U16,
            "unsigned sub-32-bit callback parameters are not supported; use u32");
      }
    }
    Set<String> symbols = new HashSet<>(),
        logicalNames = new HashSet<>(),
        rustAliases = new HashSet<>();
    for (Function function : module.functions()) {
      identifier(function.name());
      identifier(function.symbol());
      require(logicalNames.add(function.name()), "duplicate logical name: " + function.name());
      if (function.returns() instanceof Named
          || function.parameters().stream().anyMatch(p -> p.type() instanceof Named)) {
        require(
            function.parameters().stream().noneMatch(p -> p.name().equals("result"))
                || !(function.returns() instanceof Named),
            "parameter name 'result' is reserved for by-value record returns");
        require(function.callback() == null, "by-value records cannot cross a callback");
        require(
            module.functions().stream()
                .noneMatch(o -> o.symbol().equals(function.symbol() + "_myna_ref")),
            "symbol collides with the by-value reference form of " + function.name());
        if (function.direction() == Direction.EXPORT)
          require(
              module.functions().stream().noneMatch(o -> o.name().equals(function.name() + "Ref")),
              "logical name collides with the Rust reference form of " + function.name());
      }
      String rustAlias =
          Character.toUpperCase(function.name().charAt(0)) + function.name().substring(1) + "Fn";
      require(
          !types.contains(rustAlias), "Rust alias " + rustAlias + " collides with a declared type");
      require(
          rustAliases.add(rustAlias),
          "Rust alias " + rustAlias + " collides with another function's alias");
      require(symbols.add(function.symbol()), "duplicate linker symbol: " + function.symbol());
      require(
          !signatures.contains(function.symbol()),
          "symbol collides with callback: " + function.symbol());
      record(module, function.returns());
      if (!(function.returns() instanceof Named))
        type(function.returns(), pointable, signatures, true);
      require(function.returns() != Isolate.THREAD, "isolate_thread is a parameter type");
      functionScalar(function.returns());
      facts(function.facts(), FUNCTION_FACTS, strict);
      for (String suffix : List.of("_Downcall", "_CriticalDowncall")) {
        String generated = function.name() + suffix;
        // All callback signatures are already present in types.
        require(
            !types.contains(generated),
            "type collides with generated FFM adapter for " + function.name());
      }
      if (Seam.nativeLeaf(function)) {
        require(
            !enabled(function.facts(), "noreturn"),
            "noreturn requires native_leaf=false in binding ABI 2");
        require(
            !types.contains(function.name() + "_Leaf")
                && !signatures.contains(function.name() + "_Leaf")
                && !types.contains(function.name() + "_Binding")
                && !signatures.contains(function.name() + "_Binding"),
            "type collides with generated native leaf binding for " + function.name());
        require(
            module.functions().stream()
                .noneMatch(other -> other.name().equals(function.name() + "LeafPointer")),
            "function collides with generated native leaf pointer accessor for " + function.name());
      }
      require(
          !enabled(function.facts(), "native_leaf")
              || (function.direction() == Direction.IMPORT
                  && !enabled(function.facts(), "noreturn")),
          "native_leaf requires a returning native import");
      truffle(module, function, types, signatures);
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
                  .orElseThrow(() ->
                      new IllegalArgumentException("unknown callback: " + function.callback()));
          require(
              signature.returns().equals(function.returns())
                  && signature.parameters().stream()
                      .map(Parameter::type)
                      .toList()
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
        require(
            Ranges.fits((Scalar) function.returns(), value, Seam.targetAbi(module.targetTriple())),
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
        if (!(parameter.type() instanceof Named))
          type(parameter.type(), pointable, signatures, false);
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
          long n = positive("align", align.value());
          require(n <= (1L << 29) && (n & (n - 1)) == 0, "align must be a power of two <= 2^29");
        }
        Fact deref = parameter.facts().get("dereferenceable");
        if (deref != null) {
          positive("dereferenceable", deref.value());
          require(!parameter.nullable(), "dereferenceable implies non-null on supported targets");
        }
        Fact alias = parameter.facts().get("noalias");
        require(
            alias == null || !alias.enabled() || alias.approved(),
            "noalias requires explicit_contract or compiler_proven");
      }
    }
    ConstantsValidator.validate(module, fingerprint);
    OwnershipValidator.validate(module);
    ViewsValidator.validate(module);
    PersistedValidator.validate(module);
  }

  /**
   * Cells call existing bindings with primitive, pointer and byte-buffer arguments. Records by
   * value and callbacks need language glue, so a module that opts into cells must opt those imports
   * out.
   */
  private static void truffle(
      Module module, Function function, Set<String> types, Set<String> signatures) {
    boolean configured = module.truffleConfig() != null;
    for (String fact : List.of("truffle_cell", "truffle_raw_pointers"))
      require(
          !function.facts().containsKey(fact) || configured,
          fact + " requires module truffle_cells=true");
    require(
        !function.facts().containsKey("truffle_cell") || function.direction() == Direction.IMPORT,
        "truffle_cell applies to imports only");
    // The Truffle class's constants() returns the guest-visible constants object.
    require(
        !configured
            || !function.name().equals("constants")
            || !Seam.truffleCell(module, function)
            || (module.constants().isEmpty()
                && module.enums().isEmpty()
                && module.flagSets().isEmpty()),
        "Truffle cell constants collides with the generated constants() accessor");
    for (Parameter parameter : function.parameters()) {
      Fact buffer = parameter.facts().get("truffle_buffer");
      if (buffer == null) continue;
      require(configured, "truffle_buffer requires module truffle_cells=true");
      require(
          function.direction() == Direction.IMPORT
              && Seam.lendsBytes(parameter)
              && module.borrows().stream()
                  .noneMatch(b ->
                      b.function().equals(function.name())
                          && b.parameter().equals(parameter.name())),
          "truffle_buffer applies to an import's byte pointer with read or write access that is"
              + " not already a borrow: "
              + parameter.name());
      require(
          !buffer.enabled() || buffer.approved(),
          "truffle_buffer requires explicit_contract or compiler_proven");
    }
    if (!Seam.truffleCell(module, function)) return;
    String advice = "; opt out with truffle_cell=false@explicit_contract on " + function.name();
    require(
        !(function.returns() instanceof Named)
            && function.parameters().stream().noneMatch(p -> p.type() instanceof Named),
        "Truffle cells do not support by-value records" + advice);
    require(
        function.parameters().stream().noneMatch(p -> p.type() instanceof FunctionPointer),
        "Truffle cells do not support callback parameters" + advice);
    require(
        function.parameters().stream().noneMatch(p -> p.type() instanceof Isolate),
        "Truffle cells do not support isolate_thread parameters" + advice);
    require(
        !enabled(function.facts(), "noreturn"),
        "Truffle cells require a returning import" + advice);
    String cell = function.name() + "_Cell";
    require(
        !types.contains(cell) && !signatures.contains(cell),
        "type collides with generated Truffle cell for " + function.name());
  }

  private static void qualifiedType(String name) {
    require(name.contains("."), "Java type must be fully qualified: " + name);
    for (String part : name.split("\\.", -1)) identifier(part);
  }

  static boolean isScalarName(String name) {
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
      if (p.pointee() instanceof Named n) {
        require(names.contains(n.name()), "unknown pointee: " + n.name());
        require(
            !signatures.contains(n.name()),
            "pointers to callbacks are not supported; use fn<" + n.name() + ">");
      } else if (p.pointee() instanceof Pointer) type(p.pointee(), names, signatures, false);
    } else if (type instanceof FunctionPointer f)
      require(signatures.contains(f.signature()), "unknown callback: " + f.signature());
    else if (type instanceof Named)
      throw new IllegalArgumentException(
          "by-value records are function parameters and returns only; use ptr<T>");
    else require(allowVoid || type != Scalar.VOID, "void parameter or field");
  }

  private static void facts(Map<String, Fact> facts, Set<String> allowed, boolean strict) {
    facts.forEach((key, fact) -> {
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

  /**
   * Natural C layout: every field sits at its naturally aligned offset (no reserved interior gap)
   * and the struct size is the minimal multiple of alignment covering the fields (no trailing
   * slack). The JDK FFM linker requires this shape for by-value struct layouts.
   */
  private static boolean canonical(Struct struct) {
    long offset = 0;
    for (Field field : struct.fields()) {
      int size = size(field.type());
      long expected = alignUp(offset, size);
      if (field.offset() != expected) return false;
      offset = expected + size;
    }
    return struct.size() == alignUp(offset, struct.alignment());
  }

  private static long alignUp(long value, int align) {
    return (value + align - 1) / align * align;
  }

  private static long positive(String name, String text) {
    long value;
    try {
      value = Long.parseLong(text);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(name + " requires a positive integer: " + text, e);
    }
    require(value > 0, name + " requires a positive integer: " + text);
    return value;
  }

  private static void nativeImageType(Module module, String name) {
    require(
        !name.equals(module.nativeClass()) && !NATIVE_IMAGE_NAMES.contains(name),
        "type shadows generated name: " + name);
  }

  static void identifier(String name) {
    require(
        name != null
            && IDENTIFIER.matcher(name).matches()
            && !KEYWORDS.contains(name)
            && !name.startsWith("seamPadding"),
        "invalid or reserved identifier: " + name);
  }

  private static void require(boolean test, String message) {
    if (!test) throw new IllegalArgumentException(message);
  }
}
