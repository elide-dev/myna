// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package dev.elide.myna.backend;

import dev.elide.myna.model.Names;
import dev.elide.myna.model.Seam;
import dev.elide.myna.model.Seam.*;
import dev.elide.myna.model.Seam.Module;
import dev.elide.myna.serialize.Fingerprint;
import dev.elide.myna.serialize.SeamJson;
import java.util.*;

public final class NativeImage implements Backend {
  public String filename(Module module) {
    return module.nativeClass() + ".java";
  }

  private static String javaType(Module m, Type type) {
    if (type instanceof FunctionPointer pointer) {
      Signature signature =
          m.callbacks().stream()
              .filter(c -> c.name().equals(pointer.signature()))
              .findFirst()
              .orElseThrow();
      return signature.javaType() == null ? signature.name() : signature.javaType();
    }
    return Types.nativeImage(type);
  }

  /** A pointer to a declared struct or opaque type: its interface, nested in the class. */
  private static boolean typed(Type type) {
    return type instanceof Pointer p && p.pointee() instanceof Named;
  }

  private static String carrier(Module m, Parameter p) {
    return javaType(m, p.type());
  }

  private static final Map<String, String> WORD_TYPES =
      Map.of(
          "IsolateThread", "org.graalvm.nativeimage",
          "PointerBase", "org.graalvm.word",
          "VoidPointer", "org.graalvm.nativeimage.c.type",
          "WordPointer", "org.graalvm.nativeimage.c.type");

  /** The carrier by its qualified name, for generated classes that import nothing. */
  static String qualifiedCarrier(Module m, Type type) {
    if (typed(type)) return m.nativeClass() + "." + Types.nativeImage(type);
    String c = Types.java(type);
    if (WORD_TYPES.containsKey(c)) return WORD_TYPES.get(c) + "." + c;
    return type instanceof Pointer ? "org.graalvm.nativeimage.c.type." + c : c;
  }

  private static void carrierImport(Set<String> imports, Type type) {
    if (type instanceof FunctionPointer || typed(type)) return; // nested in the generated class
    String c = Types.java(type);
    if (WORD_TYPES.containsKey(c)) imports.add(WORD_TYPES.get(c) + "." + c);
    else if (type instanceof Pointer) imports.add("org.graalvm.nativeimage.c.type." + c);
  }

  private static String params(Module m, List<Parameter> ps) {
    return String.join(", ", ps.stream().map(p -> carrier(m, p) + " " + p.name()).toList());
  }

  /** {@code @CConst} reaches the C headers Native Image writes for entry points. */
  private static boolean readOnlyPointer(Parameter p) {
    return p.type() instanceof Pointer
        && (p.access() == Access.READ || p.access() == Access.CSTRING);
  }

  private static boolean unsignedSmall(Type t) {
    return t == Scalar.U8 || t == Scalar.U16;
  }

  private static boolean small(Type t) {
    return t instanceof Scalar s && s != Scalar.VOID && s.bytes < 4;
  }

  /**
   * Native Image extends a narrow Java value by its Java (signed) type, so unsigned values leaving
   * Java travel as a zero-extended {@code int}; the C ABI reads the same low bits either way.
   */
  private static String widened(Type t, String value) {
    if (t == Scalar.U8) return value + " & 0xff";
    if (t == Scalar.U16) return value + " & 0xffff";
    if (t == Scalar.BOOL) return value + " ? 1 : 0";
    return value;
  }

  /** Only the imports a module uses, so checked-in output compiles cleanly under linters. */
  private static Set<String> imports(Module m) {
    var imports = new TreeSet<String>();
    // Every import's address is a linked CGlobalData symbol, read at run time.
    if (m.functions().stream().anyMatch(f -> f.direction() == Direction.IMPORT)) {
      imports.add("com.oracle.svm.guest.staging.c.CGlobalData");
      imports.add("com.oracle.svm.guest.staging.c.CGlobalDataFactory");
      imports.add("org.graalvm.nativeimage.c.function.CFunctionPointer");
    }
    if (m.functions().stream().anyMatch(Seam::nativeLeaf)) {
      imports.add("com.oracle.svm.shared.AlwaysInline");
      imports.add("com.oracle.svm.shared.Uninterruptible");
      imports.add("com.oracle.svm.guest.staging.c.CGlobalData");
      imports.add("com.oracle.svm.guest.staging.c.CGlobalDataFactory");
      imports.add("org.graalvm.nativeimage.c.function.CFunctionPointer");
      imports.add("org.graalvm.nativeimage.c.function.InvokeCFunctionPointer");
      imports.add("org.graalvm.nativeimage.c.function.CFunction");
    }
    imports.add("org.graalvm.nativeimage.c.CContext");
    imports.add("org.graalvm.nativeimage.c.constant.CConstant");
    if (m.constants().stream().anyMatch(c -> narrow(c.type())))
      imports.add("org.graalvm.nativeimage.c.struct.AllowNarrowingCast");
    if (m.nativeConfig() != null && m.nativeConfig().cLibrary() != null)
      imports.add("org.graalvm.nativeimage.c.function.CLibrary");
    if (!m.enums().isEmpty()) {
      imports.add("org.graalvm.nativeimage.c.constant.CEnum");
      imports.add("org.graalvm.nativeimage.c.constant.CEnumConstant");
      imports.add("org.graalvm.nativeimage.c.constant.CEnumLookup");
      imports.add("org.graalvm.nativeimage.c.constant.CEnumValue");
    }
    if (m.functions().stream().anyMatch(f -> !typedWrappers(f).isEmpty())
        || m.structs().stream()
            .flatMap(st -> st.fields().stream())
            .flatMap(f -> f.cases().stream())
            .anyMatch(c -> c.group().kind() == GroupKind.ENUM))
      imports.add("com.oracle.svm.shared.AlwaysInline");
    if (m.structs().stream()
        .flatMap(st -> st.fields().stream())
        .anyMatch(f -> bitfields(m, f) != null))
      imports.add("org.graalvm.nativeimage.c.struct.CBitfield");
    if (m.callbacks().stream().anyMatch(c -> c.javaType() == null)) {
      imports.add("org.graalvm.nativeimage.c.function.CFunctionPointer");
      imports.add("org.graalvm.nativeimage.c.function.InvokeCFunctionPointer");
    }
    if (!m.structs().isEmpty() || !m.opaqueTypes().isEmpty())
      imports.add("org.graalvm.word.PointerBase");
    if (!m.structs().isEmpty()) {
      imports.add("org.graalvm.nativeimage.c.struct.CStruct");
      imports.add("org.graalvm.nativeimage.c.struct.CField");
      imports.add("org.graalvm.nativeimage.c.struct.CFieldAddress");
    }
    for (Struct st : m.structs()) {
      var views = Views.of(m, st);
      if (!views.isEmpty()) imports.add("com.oracle.svm.shared.AlwaysInline");
      if (views.stream().anyMatch(v -> !(v.element() instanceof Named)))
        imports.add("org.graalvm.word.WordFactory");
      if (views.stream().anyMatch(Views::pointerElements))
        imports.add("org.graalvm.nativeimage.c.type.WordPointer");
    }
    for (Struct st : m.structs())
      for (Field f : st.fields()) {
        carrierImport(imports, f.type());
        carrierImport(imports, new Pointer(f.type()));
      }
    for (Signature c : m.callbacks()) {
      if (c.javaType() != null) continue;
      carrierImport(imports, c.returns());
      c.parameters().forEach(p -> carrierImport(imports, p.type()));
    }
    for (Function f : m.functions().stream().map(d -> ByValue.lower(m, d)).toList()) {
      if (f.direction() == Direction.IMPORT)
        imports.add("org.graalvm.nativeimage.c.function.CFunction");
      else {
        imports.add("org.graalvm.nativeimage.c.function.CEntryPoint");
        if (f.error() == ErrorConvention.NULL_SENTINEL) imports.add("org.graalvm.word.WordFactory");
        if (f.callback() != null)
          imports.add("org.graalvm.nativeimage.c.function.CEntryPointLiteral");
        if (f.parameters().stream().anyMatch(NativeImage::readOnlyPointer))
          imports.add("org.graalvm.nativeimage.c.type.CConst");
      }
      carrierImport(imports, f.returns());
      f.abiParameters().forEach(p -> carrierImport(imports, p.type()));
    }
    return imports;
  }

  /** Class literal for {@code CEntryPointLiteral.create}; nested callback types are qualified. */
  private static String classLiteral(Module m, Type type) {
    String carrier = javaType(m, type);
    return ((type instanceof FunctionPointer && !carrier.contains(".")) || typed(type)
            ? m.nativeClass() + "."
            : "")
        + carrier
        + ".class";
  }

  private static String leafType(Function f) {
    return f.name() + "_Leaf";
  }

  private static String leafParameters(Module m, Function f) {
    return String.join(
        ", ",
        f.abiParameters().stream()
            .map(p -> (unsignedSmall(p.type()) ? "int" : carrier(m, p)) + " " + p.name())
            .toList());
  }

  private static void leafBindings(Module m, StringBuilder out) {
    var leaves =
        m.functions().stream().map(f -> ByValue.lower(m, f)).filter(Seam::nativeLeaf).toList();
    var others =
        m.functions().stream()
            .filter(f -> f.direction() == Direction.IMPORT)
            .filter(f -> leaves.stream().noneMatch(l -> l.name().equals(f.name())))
            .toList();
    if (leaves.isEmpty() && others.isEmpty()) return;
    for (Function f : leaves) {
      out.append("    public interface ")
          .append(leafType(f))
          .append(" extends CFunctionPointer {\n")
          .append("        @AlwaysInline(\"Myna binding ABI 2 native leaf\")\n")
          .append(
              "        @Uninterruptible(reason = \"Bounded primitive/word native leaf; no Java"
                  + " reentry\", mayBeInlined = true)\n")
          .append(
              "        @InvokeCFunctionPointer(transition = CFunction.Transition.NO_TRANSITION)\n")
          .append("        ")
          .append(javaType(m, f.returns()))
          .append(" invoke(")
          .append(leafParameters(m, f))
          .append(");\n    }\n");
    }
    out.append("    /** Linker metadata only: initialized at image build time. */\n")
        .append("    private static final class MynaSymbols {\n");
    for (Function f : leaves)
      out.append("        static final CGlobalData<")
          .append(leafType(f))
          .append("> ")
          .append(f.name())
          .append(" = CGlobalDataFactory.forSymbol(")
          .append(SeamJson.quote(f.symbol()))
          .append(");\n");
    for (Function f : others)
      out.append("        static final CGlobalData<CFunctionPointer> ")
          .append(f.name())
          .append(" = CGlobalDataFactory.forSymbol(")
          .append(SeamJson.quote(f.symbol()))
          .append(");\n");
    // A by-value leaf calls its reference shim; its address is still the declared symbol, which a
    // native caller invokes with the declared signature. Seam identifiers have no '$'.
    for (Function f : m.functions())
      if (f.direction() == Direction.IMPORT && ByValue.applies(f) && Seam.nativeLeaf(f))
        out.append("        static final CGlobalData<CFunctionPointer> ")
            .append(f.name())
            .append("$declared = CGlobalDataFactory.forSymbol(")
            .append(SeamJson.quote(f.symbol()))
            .append(");\n");
    out.append("    }\n");
    // The linked address of each import, read at run time: never a value in the image heap.
    for (Function f : m.functions()) {
      if (f.direction() != Direction.IMPORT) continue;
      out.append("    /** The linked address of ")
          .append(f.symbol())
          .append(", for a symbol handed to native code rather than called. */\n")
          .append("    public static CFunctionPointer ")
          .append(f.name())
          .append("_address() {\n        return MynaSymbols.")
          .append(f.name())
          .append(ByValue.applies(f) && Seam.nativeLeaf(f) ? "$declared" : "")
          .append(".get();\n    }\n");
    }
    for (Function f : leaves)
      out.append("    /** Runtime address: never initialized in the build host. */\n")
          .append("    private static final class ")
          .append(f.name())
          .append("_Binding {\n")
          .append("        static final ")
          .append(leafType(f))
          .append(" ADDRESS = MynaSymbols.")
          .append(f.name())
          .append(".get();\n    }\n");
  }

  private static void leafWrapper(Module m, Function f, StringBuilder out) {
    out.append(
            "    /** Resolve before entering an uninterruptible caller; retain the linked library."
                + " */\n")
        .append("    @AlwaysInline(\"Myna native leaf address\")\n    public static ")
        .append(leafType(f))
        .append(' ')
        .append(f.name())
        .append("LeafPointer() {\n")
        .append("        return ")
        .append(f.name())
        .append("_Binding.ADDRESS;\n    }\n")
        .append("    @AlwaysInline(\"Myna native leaf call\")\n    public static ")
        .append(javaType(m, f.returns()))
        .append(' ')
        .append(f.name())
        .append('(')
        .append(params(m, f.abiParameters()))
        .append(") {\n        ")
        .append(f.returns() == Scalar.VOID ? "" : "return ")
        .append("MynaSymbols.")
        .append(f.name())
        .append(".get().invoke(")
        .append(
            String.join(
                ", ",
                f.abiParameters().stream()
                    .map(p -> unsignedSmall(p.type()) ? widened(p.type(), p.name()) : p.name())
                    .toList()))
        .append(");\n    }\n");
  }

  public String generate(Module m) {
    return generate(m, new Fingerprint(m));
  }

  public String generate(Module m, Fingerprint abi) {
    String fingerprint = abi.valueFor(m);
    var out =
        new StringBuilder(
            "// Generated by myna. Unsigned integer carriers preserve their raw bits.\n"
                + "package "
                + m.javaPackageName()
                + ";\n\n");
    for (String i : imports(m)) out.append("import ").append(i).append(";\n");
    out.append('\n');
    // The generated context supplies seam.h and delegates to the consumer's c_context, if any.
    out.append("@CContext(").append(m.directivesClass()).append(".class)\n");
    if (m.nativeConfig() != null) {
      NativeConfig config = m.nativeConfig();
      if (config.cLibrary() != null)
        out.append("@CLibrary(value = ")
            .append(SeamJson.quote(config.cLibrary()))
            .append(", requireStatic = ")
            .append(config.requireStatic())
            .append(")\n");
    }
    out.append("public final class ")
        .append(m.nativeClass())
        .append(" {\n    private ")
        .append(m.nativeClass())
        .append("() {}\n");
    out.append("    public static final String ABI_FINGERPRINT = ")
        .append(SeamJson.quote(fingerprint))
        .append(";\n");
    out.append("    public static final int BINDING_ABI_VERSION = ")
        .append(Seam.BINDING_ABI_VERSION)
        .append(";\n");
    constants(m, fingerprint, out);
    for (EnumGroup e : m.enums()) cEnum(m, e, out);
    leafBindings(m, out);
    for (String opaque : m.opaqueTypes())
      out.append("    public interface ").append(opaque).append(" extends PointerBase {}\n");
    for (Struct st : m.structs()) {
      struct(m, st, out);
      caseAccessors(m, st, out);
    }
    for (Signature c : m.callbacks()) {
      if (c.javaType() != null) continue;
      out.append("    public interface ")
          .append(c.name())
          .append(" extends CFunctionPointer {\n        @InvokeCFunctionPointer\n        ")
          .append(javaType(m, c.returns()))
          .append(" invoke(")
          .append(params(m, c.parameters()))
          .append(");\n    }\n");
    }
    if (m.functions().stream().anyMatch(f -> f.callback() != null)) {
      out.append(
          "    /** Entry-point addresses; CEntryPointLiteral.create is hosted-only, so initialize"
              + " this class at image build time. */\n"
              + "    public static final class Literals {\n        private Literals() {}\n");
      for (Function f : m.functions())
        if (f.callback() != null)
          out.append("        public static final CEntryPointLiteral<")
              .append(javaType(m, new FunctionPointer(f.callback())))
              .append("> ")
              .append(f.name())
              .append(" =\n            CEntryPointLiteral.create(")
              .append(m.nativeClass())
              .append(".class, ")
              .append(SeamJson.quote(f.name()))
              .append(
                  f.abiParameters().stream()
                      .map(p -> ", " + classLiteral(m, p.type()))
                      .reduce("", String::concat))
              .append(");\n");
      out.append("    }\n");
    }
    for (Function f : m.functions().stream().map(d -> ByValue.lower(m, d)).toList()) {
      typedWrappers(m, f, out);
      if (Seam.nativeLeaf(f)) {
        leafWrapper(m, f, out);
        continue;
      }
      boolean lowered =
          f.direction() == Direction.IMPORT
              && f.parameters().stream().anyMatch(p -> unsignedSmall(p.type()));
      if (lowered) {
        out.append("    public static ")
            .append(javaType(m, f.returns()))
            .append(' ')
            .append(f.name())
            .append('(')
            .append(
                String.join(
                    ", ",
                    f.parameters().stream().map(p -> carrier(m, p) + " " + p.name()).toList()))
            .append(") {\n        ")
            .append(f.returns() == Scalar.VOID ? "" : "return ")
            .append(f.name())
            .append("Native(")
            .append(
                String.join(
                    ", ",
                    f.parameters().stream()
                        .map(p -> unsignedSmall(p.type()) ? widened(p.type(), p.name()) : p.name())
                        .toList()))
            .append(");\n    }\n");
      }
      if (f.direction() == Direction.IMPORT)
        out.append("    @CFunction(value = ")
            .append(SeamJson.quote(f.symbol()))
            .append(lowered ? ")\n    private static native " : ")\n    public static native ");
      else
        out.append("    @CEntryPoint(name = ")
            .append(SeamJson.quote(f.symbol()))
            .append(f.include() == null ? "" : ", include = " + f.include() + ".class")
            .append(")\n    public static ");
      boolean widenReturn = f.direction() == Direction.EXPORT && small(f.returns());
      out.append(widenReturn ? "int" : javaType(m, f.returns()))
          .append(' ')
          .append(f.name())
          .append(lowered ? "Native" : "")
          .append('(');
      out.append(
          String.join(
              ", ",
              f.abiParameters().stream()
                  .map(p ->
                      (f.direction() == Direction.EXPORT && readOnlyPointer(p) ? "@CConst " : "")
                          + (lowered && unsignedSmall(p.type()) ? "int" : carrier(m, p))
                          + " "
                          + p.name())
                  .toList()));
      if (f.direction() == Direction.IMPORT) {
        out.append(");\n");
        continue;
      }
      out.append(") {\n");
      boolean translate = f.error() != ErrorConvention.ABORT;
      if (translate) out.append("        try {\n");
      out.append(translate ? "            " : "        ");
      if (f.returns() != Scalar.VOID) out.append("return ");
      String call =
          f.javaTarget()
              + "("
              + String.join(", ", f.parameters().stream().map(Parameter::name).toList())
              + ")";
      out.append(widenReturn ? widened(f.returns(), call) : call).append(";\n");
      if (translate) {
        out.append("        } catch (Throwable failure) {\n            return ");
        if (f.error() == ErrorConvention.NULL_SENTINEL) out.append("WordFactory.nullPointer()");
        else {
          out.append(f.sentinel());
          if (f.returns() == Scalar.I64) out.append('L');
        }
        out.append(";\n        }\n");
      }
      out.append("    }\n");
    }
    return out.append("}\n").toString();
  }

  /** A carrier narrower than C's {@code int}, the type of a narrow {@code INTn_C} macro. */
  private static boolean narrow(Scalar type) {
    return type.bytes < 4;
  }

  /**
   * The header check and one accessor per const, each read from {@code seam.h} at image build. SVM
   * replaces a {@code @CConstant} call with the value while parsing bytecode, so an accessor
   * compiles to the same immediate as the literal (tests/native_image_integration.py compares the
   * code). Narrow macros have type {@code int} in C and need {@code AllowNarrowingCast}; the value
   * keeps its raw bits, as the literal {@code (byte) 0xc8} does.
   */
  private static void constants(Module m, String fingerprint, StringBuilder out) {
    out.append(
            "    /**\n"
                + "     * The seam.h check. Only this generation's header defines the macro, and"
                + " the image build's\n"
                + "     * C query resolves every @CConstant of this class, so mixing generations"
                + " fails the build.\n"
                + "     * Never called; it costs the image nothing.\n"
                + "     */\n")
        .append("    @CConstant(")
        .append(SeamJson.quote(Names.seamAbiCheck(m, fingerprint)))
        .append(") static native int seamHeaderCheck();\n");
    for (Constant c : m.constants())
      out.append("    /** const ")
          .append(c.name())
          .append(": ")
          .append(c.type().text())
          .append(", from seam.h. */\n    @CConstant(")
          .append(SeamJson.quote(Names.cMacro(m, null, c.name())))
          .append(narrow(c.type()) ? ") @AllowNarrowingCast" : ")")
          .append(" public static native ")
          .append(Types.java(c.type()))
          .append(' ')
          .append(Names.lowerCamel(c.name()))
          .append("();\n");
  }

  /**
   * A record as SVM's typed pointer. SVM takes the offsets from {@code seam.h}, whose {@code
   * SEAM_ASSERT}s pin them to the descriptor's, and the interface works with {@code
   * StackValue.get(R.class)}, {@code SizeOf.get(R.class)} and typed signatures. Each accessor is
   * the single load or store at the field's offset that a raw {@code Pointer} access is, with the
   * same code (tests/native_image_integration.py measures every carrier); a {@code boolean} store
   * is one instruction shorter, and a narrow integer read ({@code i8}, {@code u8}, {@code char},
   * {@code i16}, {@code u16}) is one register extension longer, because SVM does not fold the
   * extension into a C-interface load.
   *
   * <p>Carriers are the same-width Java primitives as everywhere else, so unsigned and narrow
   * fields keep their raw bits. SVM's C-interface verifier checks only sizes by default
   * (equal-sized carriers need no {@code @AllowNarrowingCast}/{@code @AllowWideningCast}); its
   * signedness check is the debug option {@code -H:+VerifyCInterfaceSignedness}, off by default,
   * which would reject {@code u8}/{@code u16}/{@code u32}/{@code u64} carried as signed Java types.
   * A read sign-extends a narrow value into its Java type ({@code byte} 0xff reads as -1, as {@code
   * readByte} did), and a {@code boolean} reads its byte as zero or one and writes 0 or 1.
   */
  private static void struct(Module m, Struct st, StringBuilder out) {
    out.append("    @CStruct(value = ")
        .append(SeamJson.quote(st.name()))
        .append(", addStructKeyword = true)\n    public interface ")
        .append(st.name())
        .append(" extends PointerBase {\n        int SIZE = ")
        .append(st.size())
        .append(";\n        int ALIGNMENT = ")
        .append(st.alignment())
        .append(";\n");
    for (Field f : st.fields()) {
      String carrier = javaType(m, f.type());
      String field = "(" + SeamJson.quote(f.name()) + ") ";
      if (f.group() != null && f.group().kind() == GroupKind.FLAGS) {
        FlagSet set =
            m.flagSets().stream()
                .filter(g -> g.name().equals(f.group().name()))
                .findFirst()
                .orElseThrow();
        out.append("        /** flags ")
            .append(set.name())
            .append(": test with ")
            .append(m.constantsClass())
            .append('.')
            .append(set.name())
            .append('.')
            .append(
                Names.lowerCamel(
                    set.bits().isEmpty() ? set.ranges().get(0).name() : set.bits().get(0).name()))
            .append('(')
            .append(f.name())
            .append("()) */\n");
      }
      if (!f.cases().isEmpty())
        out.append("        /** ")
            .append(
                String.join(
                    "; ",
                    f.cases().stream()
                        .map(c ->
                            "when "
                                + c.tagField()
                                + "="
                                + c.value()
                                + (c.group().kind() == GroupKind.ENUM ? " enum=" : " flags=")
                                + c.group().name())
                        .toList()))
            .append(" */\n");
      out.append("        @CField")
          .append(field)
          .append(carrier)
          .append(' ')
          .append(f.name())
          .append("();\n        @CField")
          .append(field)
          .append("void ")
          .append(f.name())
          .append('(')
          .append(carrier)
          .append(" value);\n        @CFieldAddress")
          .append(field)
          .append(Types.nativeImage(new Pointer(f.type())))
          .append(" addressOf")
          .append(Character.toUpperCase(f.name().charAt(0)))
          .append(f.name().substring(1))
          .append("();\n");
      FlagSet bits = bitfields(m, f);
      if (bits != null) bitfieldAccessors(m, f, bits, carrier, out);
    }
    if (Views.elementStructs(m).contains(st.name()))
      out.append("        /** Element {@code index} of the C array this record starts. */\n")
          .append("        ")
          .append(st.name())
          .append(" addressOf(long index);\n");
    out.append("    }\n");
    for (Views v : Views.of(m, st)) viewAccessor(m, v, out);
  }

  /**
   * {@code <Struct>_<field>At(record, i)}: element {@code i} of a counted view, the element record
   * through its {@code addressOf(long)} or the scalar through its C pointer's {@code read}. SVM
   * rejects methods without C-interface annotations in a {@code @CStruct} interface, default and
   * static ones included, so the helper is a static method of the class. The bound is an {@code
   * assert}, which compiles out unless the image is built with assertions enabled; the helper is
   * always inlined, so it costs the pointer load and the scaled access.
   */
  private static void viewAccessor(Module m, Views v, StringBuilder out) {
    String record = "record." + v.pointer().name() + "()";
    String count = "record." + v.count().name() + "()";
    String bound =
        switch (v.countType()) {
          case U32 -> "i < Integer.toUnsignedLong(" + count + ")";
          case U16 -> "i < Short.toUnsignedLong(" + count + ")";
          case U8 -> "i < Byte.toUnsignedLong(" + count + ")";
          default -> "Long.compareUnsigned(i, " + count + ") < 0"; // u64, usize
        };
    String element;
    String access;
    if (v.element() instanceof Scalar e) {
      element = Types.java(e);
      access = record + ".read(WordFactory.signed(i))";
      if (e == Scalar.USIZE || e == Scalar.ISIZE) access += ".rawValue()";
    } else if (v.element() instanceof Pointer) {
      // The field's carrier is a plain word; read it as a word array of the element's interface.
      element = javaType(m, v.element());
      access = "((WordPointer) " + record + ").read(WordFactory.signed(i))";
    } else {
      element = javaType(m, v.pointer().type());
      access = record + ".addressOf(i)";
    }
    out.append("    /** Element i of ")
        .append(v.label())
        .append("; asserts 0 <= i < ")
        .append(v.count().name())
        .append(". Not for {@code @Uninterruptible} code; use {@code ")
        .append(access)
        .append("} there. */\n")
        .append("    @AlwaysInline(\"Myna counted view element\")\n    public static ")
        .append(element)
        .append(' ')
        .append(v.struct().name())
        .append('_')
        .append(v.pointer().name())
        .append("At(")
        .append(v.struct().name())
        .append(" record, long i) {\n        assert i >= 0L && ")
        .append(bound)
        .append(" : \"")
        .append(v.label())
        .append(": index out of bounds\";\n        return ")
        .append(access)
        .append(";\n    }\n");
  }

  /**
   * One static accessor per tagged field and enum its {@code when} lines name: the field as that
   * {@code @CEnum}, null for an unlisted value, like an import's {@code <name>Enum}. The tag is not
   * checked. Flag cases keep the raw {@code @CField}, tested with {@code <Class>Constants}.
   */
  private static void caseAccessors(Module m, Struct st, StringBuilder out) {
    var done = new HashSet<String>();
    for (Field f : st.fields())
      for (FieldCase c : f.cases()) {
        if (c.group().kind() != GroupKind.ENUM) continue;
        String accessor = Names.javaCaseAccessor(f.name(), c.group().name());
        if (!done.add(accessor)) continue;
        out.append("    /** {@code ")
            .append(st.name())
            .append('.')
            .append(f.name())
            .append("} as enum ")
            .append(c.group().name())
            .append(", its meaning when ")
            .append(
                String.join(
                    " or ",
                    f.cases().stream()
                        .filter(o -> o.group().equals(c.group()))
                        .map(o -> o.tagField() + " is " + o.value())
                        .toList()))
            .append(": null for an unlisted value. */\n")
            .append("    @AlwaysInline(\"Myna tagged field accessor\")\n    public static ")
            .append(c.group().name())
            .append(' ')
            .append(accessor)
            .append('(')
            .append(st.name())
            .append(" record) {\n");
        EnumGroup narrow = m.narrowed(c.group(), f.type());
        if (narrow == null)
          out.append("        return ")
              .append(c.group().name())
              .append(".of(record.")
              .append(f.name())
              .append("());\n    }\n");
        else
          // A raw value the enum's narrower carrier cannot hold is null, as an unlisted one is.
          out.append("        ")
              .append(javaType(m, f.type()))
              .append(" raw = record.")
              .append(f.name())
              .append("();\n        return ")
              .append(fitsCarrier(narrow.type()))
              .append(" ? ")
              .append(c.group().name())
              .append(".of((")
              .append(Types.java(narrow.type()))
              .append(") raw) : null;\n    }\n");
      }
  }

  /** True when the carrier holds {@code raw}, a wider field's value as its Java type. */
  private static String fitsCarrier(Scalar carrier) {
    return switch (carrier) {
      case U8 -> "(raw >>> 8) == 0";
      case U16 -> "(raw >>> 16) == 0";
      case U32 -> "(raw >>> 32) == 0";
      case I8 -> "raw == (byte) raw";
      case I16 -> "raw == (short) raw";
      default -> "raw == (int) raw";
    };
  }

  /** The {@code c_bitfields=true} flag set annotating a field, or null. */
  private static FlagSet bitfields(Module m, Field f) {
    if (f.group() == null || f.group().kind() != GroupKind.FLAGS) return null;
    return m.flagSets().stream()
        .filter(set -> set.name().equals(f.group().name()) && set.cBitfields())
        .findFirst()
        .orElse(null);
  }

  /**
   * Opt-in ({@code c_bitfields=true}) {@code @CBitfield} accessors over the {@code <field>Bits}
   * struct that {@code seam.h} unions with the field. They read and write the same bits as the
   * masks in {@code <Class>Constants}, at one shift more per read than a mask test (spec 7a.9).
   */
  private static void bitfieldAccessors(
      Module m, Field f, FlagSet set, String carrier, StringBuilder out) {
    for (FlagBit b : set.bits()) bitfieldAccessor(f, b.name(), "boolean", out);
    // A range is typed as <Class>Constants types it: the enum's Java carrier with enum=, else the
    // flag carrier's. SVM reads the unsigned bit-field zero-extended and narrows it to that type,
    // so a value with the type's top bit set reads negative, as the literals of an unsigned carrier
    // are; a write stores the value's low bits.
    for (FlagRange r : set.ranges()) {
      String type = carrier;
      if (r.enumName() != null)
        type = Types.java(enumGroup(m, new GroupRef(GroupKind.ENUM, r.enumName())).type());
      bitfieldAccessor(f, r.name(), type, out);
    }
  }

  private static void bitfieldAccessor(Field f, String member, String type, StringBuilder out) {
    String camel = Names.lowerCamel(member);
    String accessor = f.name() + Character.toUpperCase(camel.charAt(0)) + camel.substring(1);
    String annotation = "        @CBitfield(" + SeamJson.quote(f.name() + "Bits." + member) + ") ";
    out.append(annotation)
        .append(type)
        .append(' ')
        .append(accessor)
        .append("();\n")
        .append(annotation)
        .append("void ")
        .append(accessor)
        .append('(')
        .append(type)
        .append(" value);\n");
  }

  private static EnumGroup enumGroup(Module m, GroupRef ref) {
    if (ref == null || ref.kind() != GroupKind.ENUM) return null;
    return m.enums().stream().filter(e -> e.name().equals(ref.name())).findFirst().orElseThrow();
  }

  /**
   * An enum as a first-class C-interface type: SVM sources each value from {@code seam.h} through
   * {@code @CEnumConstant}, and consumer-written {@code @CFunction} signatures may use the type.
   * Myna's own wrappers use the literal {@code value} field, which folds to an immediate for a
   * constant enum (SVM's {@code cValue()} is a table load), and {@code of}, a switch with no image
   * heap table and no virtual call (SVM's {@code lookup} is both). Unlisted values map to null.
   */
  private static void cEnum(Module m, EnumGroup e, StringBuilder out) {
    String carrier = Types.java(e.type());
    out.append("    /** enum ")
        .append(e.name())
        .append(": ")
        .append(e.type().text())
        .append(". The raw carrier keeps unlisted values; {@code of} maps them to null. */\n")
        .append("    @CEnum(")
        .append(SeamJson.quote(Names.cTypedef(m, e.name())))
        .append(")\n    public enum ")
        .append(e.name())
        .append(" {\n");
    var values = e.values();
    for (int i = 0; i < values.size(); i++) {
      EnumValue v = values.get(i);
      out.append("        @CEnumConstant(")
          .append(SeamJson.quote(Names.cMacro(m, e.name(), v.name())))
          .append(") ")
          .append(v.name().toUpperCase(Locale.ROOT))
          .append('(')
          .append(Types.javaLiteral(e.type(), v.value(), false))
          .append(i + 1 < values.size() ? "),\n" : ");\n");
    }
    out.append(
            "        /** The literal value: a constant enum folds to an immediate, a variable one"
                + " is one field load. */\n")
        .append("        public final ")
        .append(carrier)
        .append(" value;\n        ")
        .append(e.name())
        .append('(')
        .append(carrier)
        .append(" value) {\n            this.value = value;\n        }\n")
        .append("        /** SVM's conversion, for consumer-written C-interface signatures. */\n")
        .append("        @CEnumValue public native ")
        .append(carrier)
        .append(" cValue();\n")
        .append("        @CEnumLookup public static native ")
        .append(e.name())
        .append(" lookup(")
        .append(carrier)
        .append(" value);\n")
        .append(
            "        /** A switch: no image-heap table, no virtual call. Null for an unlisted"
                + " value. */\n")
        .append("        public static ")
        .append(e.name())
        .append(" of(")
        .append(carrier)
        .append(" value) {\n");
    if (carrier.equals("long")) {
      for (EnumValue v : values)
        out.append("            if (value == ")
            .append(Types.javaLiteral(e.type(), v.value(), false))
            .append(") return ")
            .append(v.name().toUpperCase(Locale.ROOT))
            .append(";\n");
      out.append("            return null;\n");
    } else {
      out.append("            switch (value) {\n");
      for (EnumValue v : values)
        out.append("                case ")
            .append(Types.javaLiteral(e.type(), v.value(), false))
            .append(": return ")
            .append(v.name().toUpperCase(Locale.ROOT))
            .append(";\n");
      out.append("                default: return null;\n            }\n");
    }
    out.append("        }\n    }\n");
  }

  /**
   * The typed forms an import's {@code enum=} references add beside its raw binding: {@code
   * <name>Enum} for an enum return, and one overload taking the enum types for enum parameters.
   * Exports and callbacks keep raw carriers. Neither changes the import's transition: they call the
   * raw binding.
   */
  private static List<String> typedWrappers(Function f) {
    var forms = new ArrayList<String>();
    if (f.direction() != Direction.IMPORT) return forms;
    if (f.returnsGroup() != null && f.returnsGroup().kind() == GroupKind.ENUM) forms.add("return");
    if (f.parameters().stream()
        .anyMatch(p -> p.group() != null && p.group().kind() == GroupKind.ENUM))
      forms.add("parameters");
    return forms;
  }

  /**
   * The parameters of the public raw method the typed wrappers call: the lowered helper's logical
   * parameters when narrow unsigned values are widened behind it, the ABI parameters otherwise.
   */
  private static List<Parameter> publicParameters(Function f) {
    boolean lowered =
        !Seam.nativeLeaf(f)
            && f.direction() == Direction.IMPORT
            && f.parameters().stream().anyMatch(p -> unsignedSmall(p.type()));
    return lowered ? f.parameters() : f.abiParameters();
  }

  private static void typedWrappers(Module m, Function f, StringBuilder out) {
    var forms = typedWrappers(f);
    var ps = publicParameters(f);
    String args = String.join(", ", ps.stream().map(Parameter::name).toList());
    if (forms.contains("return")) {
      EnumGroup e = enumGroup(m, f.returnsGroup());
      out.append("    /** {@code ")
          .append(f.name())
          .append("} as enum ")
          .append(e.name())
          .append(": null for an unlisted value, which {@code ")
          .append(f.name())
          .append("} returns as is. */\n")
          .append("    @AlwaysInline(\"Myna typed enum wrapper\")\n    public static ")
          .append(e.name())
          .append(' ')
          .append(f.name())
          .append("Enum(")
          .append(params(m, ps))
          .append(") {\n        return ")
          .append(e.name())
          .append(".of(")
          .append(f.name())
          .append('(')
          .append(args)
          .append("));\n    }\n");
    }
    if (forms.contains("parameters")) {
      out.append("    @AlwaysInline(\"Myna typed enum overload\")\n    public static ")
          .append(javaType(m, f.returns()))
          .append(' ')
          .append(f.name())
          .append('(')
          .append(
              String.join(
                  ", ",
                  ps.stream()
                      .map(p ->
                          (enumGroup(m, p.group()) instanceof EnumGroup e
                                  ? e.name()
                                  : carrier(m, p))
                              + " "
                              + p.name())
                      .toList()))
          .append(") {\n        ")
          .append(f.returns() == Scalar.VOID ? "" : "return ")
          .append(f.name())
          .append('(')
          .append(
              String.join(
                  ", ",
                  ps.stream()
                      .map(p -> enumGroup(m, p.group()) != null ? p.name() + ".value" : p.name())
                      .toList()))
          .append(");\n    }\n");
    }
  }
}
