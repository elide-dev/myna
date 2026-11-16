// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package dev.elide.myna.validate;

import dev.elide.myna.model.Names;
import dev.elide.myna.model.Seam;
import dev.elide.myna.model.Seam.*;
import dev.elide.myna.model.Seam.Module;
import dev.elide.myna.serialize.SeamJson;
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
    // Truffle's constants() object has one member per const and per group, in one namespace.
    if (m.truffleConfig() != null) {
      for (EnumGroup e : m.enums())
        require(
            !constants.contains(e.name()),
            "enum "
                + e.name()
                + ": name collides with const "
                + e.name()
                + " in Truffle constants()");
      for (FlagSet f : m.flagSets())
        require(
            !constants.contains(f.name()),
            "flags "
                + f.name()
                + ": name collides with const "
                + f.name()
                + " in Truffle constants()");
    }
    accessors(m);
    for (EnumGroup e : m.enums()) enumValues(e, abi);
    for (FlagSet f : m.flagSets()) flagBits(f);
    for (FlagSet f : m.flagSets()) flagRanges(m, f);

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
      require(
          !f.bits().isEmpty() || !f.ranges().isEmpty(), "flags " + f.name() + ": declares no bits");
  }

  /**
   * Java keywords and literals, the instance methods of {@code Object} (a static method may not
   * hide one), and the seam.h check: names a const accessor cannot take.
   */
  private static final Set<String> RESERVED_ACCESSORS =
      Set.of(
          ("abstract assert boolean break byte case catch char class const continue default do"
                  + " double else enum extends final finally float for goto if implements import"
                  + " instanceof int interface long native new package private protected public"
                  + " return short static strictfp super switch synchronized this throw throws"
                  + " transient try void volatile while true false null getClass hashCode equals"
                  + " toString clone finalize notify notifyAll wait seamHeaderCheck")
              .split(" "));

  /**
   * Each const is a static {@code @CConstant} accessor of the Native Image class, named {@code
   * lowerCamel(NAME)}: it must be a legal method name and the class's only method of that name.
   */
  private static void accessors(Module m) {
    var methods = new HashMap<String, String>();
    for (Function f : m.functions()) {
      require(
          !f.name().equals("seamHeaderCheck"),
          "function seamHeaderCheck: name collides with the generated seam.h check of the Native"
              + " Image class");
      methods.putIfAbsent(f.name(), "function " + f.name());
      methods.putIfAbsent(f.name() + "Native", "function " + f.name());
      if (Seam.nativeLeaf(f)) methods.putIfAbsent(f.name() + "LeafPointer", "function " + f.name());
      // The typed form of an import whose return names an enum.
      if (f.direction() == Direction.IMPORT
          && f.returnsGroup() != null
          && f.returnsGroup().kind() == GroupKind.ENUM)
        methods.putIfAbsent(f.name() + "Enum", "typed enum wrapper of function " + f.name());
    }
    // Native Image enum case accessors: one per record, field and enum; overloads across records.
    var cases = new HashMap<String, String>();
    for (Struct s : m.structs())
      for (Field f : s.fields())
        for (FieldCase c : f.cases()) {
          if (c.group().kind() != GroupKind.ENUM) continue;
          String accessor = Names.javaCaseAccessor(f.name(), c.group().name());
          String owner = "field " + s.name() + "." + f.name() + " enum " + c.group().name();
          String other = cases.putIfAbsent(accessor + "(" + s.name() + ")", owner);
          require(
              other == null || other.equals(owner),
              owner + ": Native Image accessor " + accessor + " collides with " + other);
          other = methods.get(accessor);
          require(
              other == null || other.startsWith("field "),
              owner + ": Native Image accessor " + accessor + " collides with " + other);
          methods.putIfAbsent(accessor, owner);
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
          b.bit() >= 0 && b.bit() < width,
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

  /**
   * Methods of every Rust flag newtype, which a range's getter may not take: its inherent methods,
   * and those of its derives ({@code Clone}, {@code PartialEq}, {@code Eq}, {@code Hash}, {@code
   * Debug}) and operator impls ({@code BitOr}, {@code BitAnd}). An inherent method of the same name
   * would shadow the trait method in method-call syntax.
   */
  private static final Set<String> RUST_FLAG_METHODS =
      Set.of(
          "contains",
          "intersects",
          "union",
          "when",
          "clone",
          "clone_from",
          "eq",
          "ne",
          "assert_receiver_is_total_eq",
          "hash",
          "hash_slice",
          "fmt",
          "bitor",
          "bitand");

  /**
   * Bit ranges: inside the carrier, disjoint from every bit and other range, with an enum whose
   * carrier holds every value of the width and whose values fit it. Bit and range names, and each
   * range's derived {@code <name>_shift} and {@code <name>_mask} constants, share one namespace;
   * each range's getter and {@code with} setter must be free method names in Java and Rust.
   */
  private static void flagRanges(Module m, FlagSet f) {
    if (f.ranges().isEmpty()) return;
    String where = "flags " + f.name() + ": ";
    int width = f.type().bytes * 8;
    // Owner of each member name: bits, ranges, and the ranges' derived constants.
    var members = new HashMap<String, String>();
    for (FlagBit b : f.bits()) members.put(b.name(), "bit " + b.name());
    var javaMethods = new HashMap<String, String>();
    for (FlagBit b : f.bits())
      javaMethods.putIfAbsent(Names.lowerCamel(b.name()), "predicate of bit " + b.name());
    javaMethods.put("hasUnknown", "hasUnknown");
    var rustMethods = new HashMap<String, String>();
    for (String method : RUST_FLAG_METHODS) rustMethods.put(method, "the flag method " + method);
    for (FlagRange r : f.ranges()) {
      String range = "range " + r.name() + " = " + r.low() + ".." + r.high();
      require(MEMBER.matcher(r.name()).matches(), where + "invalid range name: " + r.name());
      require(!RESERVED_BITS.contains(r.name()), where + "range name is reserved: " + r.name());
      Validator.identifier(r.name());
      require(r.low() <= r.high(), where + range + " is empty (LOW > HIGH)");
      require(
          r.low() >= 0 && r.high() < width,
          where + range + " is outside " + f.type().text() + " (bits 0.." + (width - 1) + ")");
      // C, Rust and Java spell the derived constants NAME_SHIFT and NAME_MASK.
      for (String member : List.of(r.name(), r.name() + "_shift", r.name() + "_mask")) {
        String other = members.putIfAbsent(member, "range " + r.name());
        require(
            other == null,
            where + other + " and range " + r.name() + " both use the name " + member);
      }
      String getter = Names.javaRangeGetter(r.name());
      String setter = Names.javaRangeSetter(r.name());
      require(
          !RESERVED_ACCESSORS.contains(getter),
          where + "range " + r.name() + ": getter " + getter + " is a reserved Java name");
      for (String method : List.of(getter, setter)) {
        String other = javaMethods.putIfAbsent(method, "range " + r.name());
        require(
            other == null,
            where + "range " + r.name() + ": Java method " + method + " collides with " + other);
      }
      for (String method : List.of(r.name(), "with_" + r.name())) {
        String other = rustMethods.putIfAbsent(method, "range " + r.name());
        require(
            other == null,
            where + "range " + r.name() + ": Rust method " + method + " collides with " + other);
      }
      for (FlagBit b : f.bits())
        require(
            b.bit() < r.low() || b.bit() > r.high(),
            where + range + " and bit " + b.name() + " = " + b.bit() + " overlap");
      if (r.enumName() != null) rangeEnum(m, where + range, r);
    }
    for (int i = 1; i < f.ranges().size(); i++) {
      FlagRange a = f.ranges().get(i - 1), b = f.ranges().get(i);
      require(
          b.low() > a.high(),
          where
              + "ranges "
              + a.name()
              + " = "
              + a.low()
              + ".."
              + a.high()
              + " and "
              + b.name()
              + " = "
              + b.low()
              + ".."
              + b.high()
              + " overlap");
    }
  }

  private static void rangeEnum(Module m, String where, FlagRange r) {
    EnumGroup e =
        m.enums().stream().filter(g -> g.name().equals(r.enumName())).findFirst().orElse(null);
    if (e == null) {
      boolean flagSet = m.flagSets().stream().anyMatch(g -> g.name().equals(r.enumName()));
      throw new IllegalArgumentException(
          where
              + ": "
              + (flagSet
                  ? "enum=" + r.enumName() + " names a flag set"
                  : "undeclared enum: " + r.enumName()));
    }
    // The getter returns the enum's carrier: it must hold every value of the range's width.
    int carrierBits = e.type().bytes * 8 - (unsigned(e.type()) ? 0 : 1);
    require(
        carrierBits >= r.width(),
        where
            + ": enum="
            + e.name()
            + " has carrier "
            + e.type().text()
            + ", too narrow for "
            + r.width()
            + " bits");
    for (EnumValue v : e.values())
      require(
          v.value().signum() >= 0 && v.value().bitLength() <= r.width(),
          where
              + ": enum="
              + e.name()
              + " value "
              + v.name()
              + " = "
              + v.value()
              + " does not fit "
              + r.width()
              + " bits");
  }

  // ---- references ----

  private static void references(Module m) {
    for (Struct s : m.structs())
      for (Field f : s.fields()) {
        reference(m, "field " + s.name() + "." + f.name(), f.group(), f.type(), "field's type");
        cases(m, s, f);
      }
    caseAccessors(m);
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

  /**
   * {@code when} lines: one tag field of the same struct that carries {@code enum=}, one of its
   * values per line and at most one line per value, each naming a group whose carrier is the
   * field's type.
   */
  private static void cases(Module m, Struct s, Field f) {
    if (f.cases().isEmpty()) return;
    String where = "field " + s.name() + "." + f.name();
    require(f.group() == null, where + ": when lines and enum=/flags= are mutually exclusive");
    String tagName = f.cases().get(0).tagField();
    var values = new HashSet<String>();
    for (FieldCase c : f.cases()) {
      String when = where + " when " + c.tagField() + "=" + c.value();
      require(
          c.tagField().equals(tagName),
          where + ": when lines name tag fields " + tagName + " and " + c.tagField() + "; use one");
      Field tag =
          s.fields().stream().filter(x -> x.name().equals(c.tagField())).findFirst().orElse(null);
      require(tag != null, when + ": struct " + s.name() + " has no field " + c.tagField());
      require(
          tag.group() != null && tag.group().kind() == GroupKind.ENUM,
          when + ": tag field " + c.tagField() + " carries no enum=");
      EnumGroup e =
          m.enums().stream()
              .filter(g -> g.name().equals(tag.group().name()))
              .findFirst()
              .orElse(null);
      require(e != null, when + ": undeclared enum: " + tag.group().name());
      require(
          e.values().stream().anyMatch(v -> v.name().equals(c.value())),
          when + ": enum " + e.name() + " declares no value " + c.value());
      require(values.add(c.value()), where + ": two when lines for " + tagName + "=" + c.value());
      reference(m, when, c.group(), f.type(), "field's type");
    }
  }

  /**
   * The per-case accessors: Rust {@code <field>_as_<group>()} on the record, one per field and
   * group, and Native Image {@code <field>As<Group>(record)} for enum cases. Neither may collide
   * with another accessor of the record or, in Java, with another method of the class.
   */
  private static void caseAccessors(Module m) {
    for (Struct s : m.structs()) {
      var rust = new HashMap<String, String>();
      // A counted pointer's Rust view is an inherent method named after the field.
      for (Field f : s.fields())
        if (f.count() != null) rust.put(f.name(), "counted field " + f.name());
      for (Field f : s.fields())
        for (FieldCase c : f.cases()) {
          String owner = "field " + f.name() + " group " + c.group().name();
          String other =
              rust.putIfAbsent(Names.rustCaseAccessor(f.name(), c.group().name()), owner);
          require(
              other == null || other.equals(owner),
              "struct "
                  + s.name()
                  + ": Rust accessor "
                  + Names.rustCaseAccessor(f.name(), c.group().name())
                  + " of "
                  + owner
                  + " collides with the accessor of "
                  + other);
        }
    }
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
      for (Field f : s.fields()) {
        accessors.put(f.name(), "field " + f.name());
        accessors.put(
            "addressOf" + Character.toUpperCase(f.name().charAt(0)) + f.name().substring(1),
            "address accessor of field " + f.name());
      }
      for (Field f : s.fields()) {
        if (f.group() == null || f.group().kind() != GroupKind.FLAGS) continue;
        for (FlagSet set : m.flagSets()) {
          if (!set.name().equals(f.group().name()) || !set.cBitfields()) continue;
          var members = new ArrayList<String>();
          for (FlagBit b : set.bits()) members.add("bit " + b.name());
          for (FlagRange r : set.ranges()) members.add("range " + r.name());
          for (String member : members) {
            String camel = Names.lowerCamel(member.substring(member.indexOf(' ') + 1));
            String accessor =
                f.name() + Character.toUpperCase(camel.charAt(0)) + camel.substring(1);
            String other =
                accessors.putIfAbsent(accessor, "accessor of field " + f.name() + " " + member);
            require(
                other == null,
                "struct "
                    + s.name()
                    + ": accessor "
                    + accessor
                    + " of field "
                    + f.name()
                    + " "
                    + member
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
    var reserved =
        Set.of(
            upper + "_SEAM_ABI",
            Names.seamAbiCheck(m, SeamJson.fingerprint(m)),
            upper + "_BINDING_ABI_VERSION",
            upper + "_SEAM_H");
    var claimed = new HashMap<String, String>();
    for (String symbol : new TreeSet<>(functionSymbols))
      claimed.put(symbol, "function symbol " + symbol);
    // Callback names are C typedefs in the same ordinary-identifier namespace as the generated
    // macros, typedefs and name functions.
    for (Signature c : m.callbacks()) claim(claimed, c.name(), "callback " + c.name());
    // Opaque and record names are struct tags, the namespace of the generated bits structs.
    var tags = new HashMap<String, String>();
    for (String name : m.opaqueTypes()) claim(tags, name, "opaque " + name);
    for (Struct s : m.structs()) claim(tags, s.name(), "struct " + s.name());
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
      for (FlagRange r : f.ranges()) {
        String where = "flags " + f.name() + " range " + r.name();
        for (String suffix : List.of("_shift", "_mask")) {
          String name = Names.cMacro(m, f.name(), r.name() + suffix);
          require(
              !reserved.contains(name),
              where + ": C name " + name + " is reserved by the generated header");
          claim(claimed, name, where + suffix.toUpperCase(Locale.ROOT));
        }
        claim(claimed, Names.cFlagFunction(m, f.name(), r.name()), where + " getter");
        claim(claimed, Names.cFlagFunction(m, f.name(), "with_" + r.name()), where + " setter");
      }
      claim(claimed, Names.cMacro(m, f.name(), "ALL"), "flags " + f.name() + " _ALL");
      claim(claimed, Names.cTypedef(m, f.name()), "flags " + f.name() + " typedef");
      if (f.cBitfields()) {
        String bits = Names.cBitsStruct(m, f.name());
        claim(claimed, bits, "flags " + f.name() + " bits struct");
        claim(tags, bits, "flags " + f.name() + " bits struct");
      }
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
