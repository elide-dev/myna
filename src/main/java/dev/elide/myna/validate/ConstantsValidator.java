// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package dev.elide.myna.validate;

import dev.elide.myna.model.Names;
import dev.elide.myna.model.Seam;
import dev.elide.myna.model.Seam.*;
import dev.elide.myna.model.Seam.Module;
import java.math.BigInteger;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Validates named constants, enums and flag sets, the references that annotate fields, parameters
 * and returns with them, and every name the generators derive from them, even in relaxed mode.
 */
final class ConstantsValidator {
  private ConstantsValidator() {}

  private static final Pattern MEMBER = Pattern.compile("[a-z][a-z0-9_]*");
  private static final Set<String> RESERVED_BITS = Set.of("all", "none");

  static void validate(Module m) {
    TargetAbi abi = Seam.targetAbi(m.targetTriple());
    for (Constant c : m.constants()) carrier("const " + c.name(), c.type());
    for (EnumGroup e : m.enums()) carrier("enum " + e.name(), e.type());
    for (FlagSet f : m.flagSets()) carrier("flags " + f.name(), f.type());

    var functionSymbols = new HashSet<String>();
    for (Function f : m.functions()) functionSymbols.add(f.symbol());
    groupNames(m, functionSymbols);

    var constants = new HashSet<String>();
    for (Constant c : m.constants()) {
      Validator.identifier(c.name());
      require(constants.add(c.name()), "duplicate constant: " + c.name());
      String where = "const " + c.name();
      require(
          !functionSymbols.contains(c.name()),
          where + ": name collides with function symbol " + c.name());
      require(
          Ranges.fits(c.type(), c.value(), abi),
          where + ": value " + c.value() + " does not fit " + c.type().text());
    }
    accessors(m);
    for (EnumGroup e : m.enums()) enumValues(e, abi);
    for (FlagSet f : m.flagSets()) flagBits(f);

    references(m);
    bitfieldAccessors(m);
    cNames(m, functionSymbols);
  }

  private static boolean integer(Scalar s) {
    return switch (s) {
      case I8, U8, I16, U16, I32, U32, I64, U64, ISIZE, USIZE -> true;
      default -> false;
    };
  }

  private static boolean unsigned(Scalar s) {
    return switch (s) {
      case U8, U16, U32, U64, USIZE -> true;
      default -> false;
    };
  }

  private static void carrier(String where, Scalar type) {
    require(integer(type), where + ": carrier must be an integer scalar, found " + type.text());
  }

  private static void groupNames(Module m, Set<String> functionSymbols) {
    var declared = new HashSet<String>(m.opaqueTypes());
    m.structs().forEach(s -> declared.add(s.name()));
    m.callbacks().forEach(s -> declared.add(s.name()));
    var groups = new ArrayList<String[]>();
    m.enums().forEach(e -> groups.add(new String[] {"enum " + e.name(), e.name()}));
    m.flagSets().forEach(f -> groups.add(new String[] {"flags " + f.name(), f.name()}));
    // Duplicate, scalar, reserved and generated-name checks of the group names themselves run in
    // Validator, where the groups join the declared types.
    for (String[] group : groups) {
      String name = group[1];
      declared.add(name);
      require(
          !functionSymbols.contains(name),
          group[0] + ": name collides with function symbol " + name);
      require(!name.equals(m.ffmClass()), "type collides with generated FFM class " + m.ffmClass());
    }
    for (String[] group : groups)
      require(
          !declared.contains(group[1] + "Names"),
          group[0] + ": generated name " + group[1] + "Names collides with a declared type");
    require(
        !declared.contains(m.directivesClass()),
        "type collides with generated directives class " + m.directivesClass());
    if (!groups.isEmpty() || !m.constants().isEmpty())
      require(
          !declared.contains(m.constantsClass()),
          "type collides with generated constants class " + m.constantsClass());
    for (EnumGroup e : m.enums())
      require(!e.values().isEmpty(), "enum " + e.name() + ": declares no values");
    for (FlagSet f : m.flagSets())
      require(!f.bits().isEmpty(), "flags " + f.name() + ": declares no bits");
  }

  /**
   * Java keywords and literals, the instance methods of {@code Object} (a static method may not
   * hide one), and the header-fingerprint accessor: names a const accessor cannot take.
   */
  private static final Set<String> RESERVED_ACCESSORS =
      Set.of(
          ("abstract assert boolean break byte case catch char class const continue default do"
                  + " double else enum extends final finally float for goto if implements import"
                  + " instanceof int interface long native new package private protected public"
                  + " return short static strictfp super switch synchronized this throw throws"
                  + " transient try void volatile while true false null getClass hashCode equals"
                  + " toString clone finalize notify notifyAll wait seamHeaderAbi")
              .split(" "));

  /**
   * Each const is a static {@code @CConstant} accessor of the Native Image class, named {@code
   * lowerCamel(NAME)}: it must be a legal method name and the class's only method of that name.
   */
  private static void accessors(Module m) {
    var methods = new HashMap<String, String>();
    for (Function f : m.functions()) {
      methods.putIfAbsent(f.name(), "function " + f.name());
      methods.putIfAbsent(f.name() + "Native", "function " + f.name());
      if (Seam.nativeLeaf(f)) methods.putIfAbsent(f.name() + "LeafPointer", "function " + f.name());
      // The typed form of an import whose return names an enum.
      if (f.direction() == Direction.IMPORT
          && f.returnsGroup() != null
          && f.returnsGroup().kind() == GroupKind.ENUM)
        methods.putIfAbsent(f.name() + "Enum", "typed enum wrapper of function " + f.name());
    }
    for (Constant c : m.constants()) {
      String accessor = Names.lowerCamel(c.name());
      String where = "const " + c.name() + ": Native Image accessor " + accessor;
      require(!RESERVED_ACCESSORS.contains(accessor), where + " is a reserved Java name");
      String other = methods.putIfAbsent(accessor, "const " + c.name());
      require(other == null, where + " collides with " + other);
    }
  }

  private static void enumValues(EnumGroup e, TargetAbi abi) {
    String where = "enum " + e.name() + ": ";
    var names = new HashSet<String>();
    var byValue = new HashMap<BigInteger, String>();
    for (EnumValue v : e.values()) {
      require(MEMBER.matcher(v.name()).matches(), where + "invalid value name: " + v.name());
      require(names.add(v.name()), where + "duplicate value name: " + v.name());
      require(
          Ranges.fits(e.type(), v.value(), abi),
          where + "value " + v.name() + " = " + v.value() + " does not fit " + e.type().text());
      String other = byValue.putIfAbsent(v.value(), v.name());
      require(
          other == null, where + "values " + other + " and " + v.name() + " are both " + v.value());
    }
  }

  private static void flagBits(FlagSet f) {
    String where = "flags " + f.name() + ": ";
    int width = f.type().bytes * 8;
    var names = new HashSet<String>();
    var byBit = new HashMap<Integer, String>();
    for (FlagBit b : f.bits()) {
      require(MEMBER.matcher(b.name()).matches(), where + "invalid bit name: " + b.name());
      require(!RESERVED_BITS.contains(b.name()), where + "bit name is reserved: " + b.name());
      require(names.add(b.name()), where + "duplicate bit name: " + b.name());
      require(
          b.bit() < width,
          where
              + "bit "
              + b.name()
              + " = "
              + b.bit()
              + " is outside "
              + f.type().text()
              + " (bits 0.."
              + (width - 1)
              + ")");
      String other = byBit.putIfAbsent(b.bit(), b.name());
      require(
          other == null, where + "bits " + other + " and " + b.name() + " both use bit " + b.bit());
      // Each bit is also a static predicate of <Class>Constants.<Flags>, named lowerCamel(bit).
      String predicate = Names.lowerCamel(b.name());
      require(
          !RESERVED_ACCESSORS.contains(predicate) && !predicate.equals("hasUnknown"),
          where + "bit " + b.name() + ": predicate " + predicate + " is a reserved Java name");
    }
    require(
        !f.cBitfields() || unsigned(f.type()),
        where + "c_bitfields requires an unsigned carrier, found " + f.type().text());
  }

  // ---- references ----

  private static void references(Module m) {
    for (Struct s : m.structs())
      for (Field f : s.fields())
        reference(m, "field " + s.name() + "." + f.name(), f.group(), f.type(), "field's type");
    for (Function f : m.functions()) {
      String owner = f.direction().name().toLowerCase(Locale.ROOT) + " " + f.name();
      reference(m, owner, f.returnsGroup(), f.returns(), "return type");
      for (Parameter p : f.parameters())
        reference(m, "param " + f.name() + "." + p.name(), p.group(), p.type(), "parameter's type");
    }
    for (Signature s : m.callbacks()) {
      reference(m, "callback " + s.name(), s.returnsGroup(), s.returns(), "return type");
      for (Parameter p : s.parameters())
        reference(m, "param " + s.name() + "." + p.name(), p.group(), p.type(), "parameter's type");
    }
    var typedEnum = new HashSet<String>();
    for (Function f : m.functions())
      if (f.direction() == Direction.IMPORT
          && f.returnsGroup() != null
          && f.returnsGroup().kind() == GroupKind.ENUM) typedEnum.add(f.name());
    for (Function f : m.functions())
      for (String base : typedEnum)
        if (f.name().equals(base + "Enum"))
          throw new IllegalArgumentException(
              "logical name collides with the typed enum wrapper of " + base + ": " + f.name());
  }

  private static void reference(Module m, String where, GroupRef ref, Type type, String what) {
    if (ref == null) return;
    boolean isEnum = ref.kind() == GroupKind.ENUM;
    String option = (isEnum ? "enum=" : "flags=") + ref.name();
    Scalar carrier = null;
    if (isEnum) {
      for (EnumGroup e : m.enums()) if (e.name().equals(ref.name())) carrier = e.type();
    } else {
      for (FlagSet f : m.flagSets()) if (f.name().equals(ref.name())) carrier = f.type();
    }
    if (carrier == null) {
      boolean otherKind =
          isEnum
              ? m.flagSets().stream().anyMatch(f -> f.name().equals(ref.name()))
              : m.enums().stream().anyMatch(e -> e.name().equals(ref.name()));
      if (otherKind)
        throw new IllegalArgumentException(
            where
                + ": "
                + option
                + (isEnum ? " names a flag set; use flags=" : " names an enum; use enum=")
                + ref.name());
      throw new IllegalArgumentException(
          where + ": undeclared " + (isEnum ? "enum" : "flag set") + ": " + ref.name());
    }
    require(
        type == carrier,
        where
            + ": "
            + option
            + " has carrier "
            + carrier.text()
            + ", but the "
            + what
            + " is "
            + type.text());
  }

  /** Accessor names of {@code @CBitfield} structs may not collide with the struct's other names. */
  private static void bitfieldAccessors(Module m) {
    for (Struct s : m.structs()) {
      var accessors = new HashMap<String, String>();
      for (Field f : s.fields()) accessors.put(f.name(), "field " + f.name());
      for (Field f : s.fields()) {
        if (f.group() == null || f.group().kind() != GroupKind.FLAGS) continue;
        for (FlagSet set : m.flagSets()) {
          if (!set.name().equals(f.group().name()) || !set.cBitfields()) continue;
          for (FlagBit b : set.bits()) {
            String camel = Names.lowerCamel(b.name());
            String accessor =
                f.name() + Character.toUpperCase(camel.charAt(0)) + camel.substring(1);
            String other =
                accessors.putIfAbsent(
                    accessor, "accessor of field " + f.name() + " bit " + b.name());
            require(
                other == null,
                "struct "
                    + s.name()
                    + ": accessor "
                    + accessor
                    + " of field "
                    + f.name()
                    + " bit "
                    + b.name()
                    + " collides with "
                    + other);
          }
        }
      }
    }
  }

  // ---- generated C names ----

  private static void cNames(Module m, Set<String> functionSymbols) {
    String upper = m.name().toUpperCase(Locale.ROOT);
    var reserved = Set.of(upper + "_SEAM_ABI", upper + "_BINDING_ABI_VERSION", upper + "_SEAM_H");
    var claimed = new HashMap<String, String>();
    for (String symbol : new TreeSet<>(functionSymbols))
      claimed.put(symbol, "function symbol " + symbol);
    for (Constant c : m.constants()) {
      String name = Names.cMacro(m, null, c.name());
      String where = "const " + c.name();
      require(
          !reserved.contains(name),
          where + ": C name " + name + " is reserved by the generated header");
      claim(claimed, name, where);
    }
    for (EnumGroup e : m.enums()) {
      for (EnumValue v : e.values()) {
        String name = Names.cMacro(m, e.name(), v.name());
        String where = "enum " + e.name() + " value " + v.name();
        require(
            !reserved.contains(name),
            where + ": C name " + name + " is reserved by the generated header");
        claim(claimed, name, where);
      }
      claim(claimed, Names.cTypedef(m, e.name()), "enum " + e.name() + " typedef");
      claim(claimed, Names.cNameFunction(m, e.name()), "enum " + e.name() + " name function");
    }
    for (FlagSet f : m.flagSets()) {
      for (FlagBit b : f.bits()) {
        String name = Names.cMacro(m, f.name(), b.name());
        String where = "flags " + f.name() + " bit " + b.name();
        require(
            !reserved.contains(name),
            where + ": C name " + name + " is reserved by the generated header");
        claim(claimed, name, where);
      }
      claim(claimed, Names.cMacro(m, f.name(), "ALL"), "flags " + f.name() + " _ALL");
      claim(claimed, Names.cTypedef(m, f.name()), "flags " + f.name() + " typedef");
      if (f.cBitfields())
        claim(claimed, Names.cBitsStruct(m, f.name()), "flags " + f.name() + " bits struct");
    }
  }

  private static void claim(Map<String, String> claimed, String name, String owner) {
    String other = claimed.putIfAbsent(name, owner);
    require(other == null, "C name " + name + " is generated by both " + other + " and " + owner);
  }

  private static void require(boolean test, String message) {
    if (!test) throw new IllegalArgumentException(message);
  }
}
