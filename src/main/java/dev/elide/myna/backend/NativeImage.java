// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package dev.elide.myna.backend;

import dev.elide.myna.model.Seam;
import dev.elide.myna.model.Seam.*;
import dev.elide.myna.model.Seam.Module;
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
    return Types.java(type);
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
  static String qualifiedCarrier(Type type) {
    String c = Types.java(type);
    if (WORD_TYPES.containsKey(c)) return WORD_TYPES.get(c) + "." + c;
    return type instanceof Pointer ? "org.graalvm.nativeimage.c.type." + c : c;
  }

  private static void carrierImport(Set<String> imports, Type type) {
    if (type instanceof FunctionPointer) return; // nested in the generated class
    String c = Types.java(type);
    if (WORD_TYPES.containsKey(c)) imports.add(WORD_TYPES.get(c) + "." + c);
    else if (type instanceof Pointer) imports.add("org.graalvm.nativeimage.c.type." + c);
  }

  private static String params(Module m, List<Parameter> ps) {
    return String.join(", ", ps.stream().map(p -> carrier(m, p) + " " + p.name()).toList());
  }

  /** {@code @CConst} reaches the C headers Native Image writes for entry points. */
  private static boolean readOnlyPointer(Parameter p) {
    return p.type() instanceof Pointer && p.access() == Access.READ;
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
    if (m.functions().stream().anyMatch(Seam::nativeLeaf)) {
      imports.add("com.oracle.svm.shared.AlwaysInline");
      imports.add("com.oracle.svm.shared.Uninterruptible");
      imports.add("com.oracle.svm.guest.staging.c.CGlobalData");
      imports.add("com.oracle.svm.guest.staging.c.CGlobalDataFactory");
      imports.add("org.graalvm.nativeimage.c.function.CFunctionPointer");
      imports.add("org.graalvm.nativeimage.c.function.InvokeCFunctionPointer");
      imports.add("org.graalvm.nativeimage.c.function.CFunction");
    }
    if (m.nativeConfig() != null) {
      if (m.nativeConfig().cContext() != null) imports.add("org.graalvm.nativeimage.c.CContext");
      if (m.nativeConfig().cLibrary() != null)
        imports.add("org.graalvm.nativeimage.c.function.CLibrary");
    }
    if (m.callbacks().stream().anyMatch(c -> c.javaType() == null)) {
      imports.add("org.graalvm.nativeimage.c.function.CFunctionPointer");
      imports.add("org.graalvm.nativeimage.c.function.InvokeCFunctionPointer");
    }
    if (!m.structs().isEmpty()) {
      imports.add("org.graalvm.word.Pointer");
      imports.add("org.graalvm.word.PointerBase");
    }
    for (Struct st : m.structs()) st.fields().forEach(f -> carrierImport(imports, f.type()));
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
    return (type instanceof FunctionPointer && !carrier.contains(".") ? m.nativeClass() + "." : "")
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
    if (leaves.isEmpty()) return;
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
    out.append("    }\n");
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
    var out =
        new StringBuilder(
            "// Generated by myna. Unsigned integer carriers preserve their raw bits.\n"
                + "package "
                + m.javaPackageName()
                + ";\n\n");
    for (String i : imports(m)) out.append("import ").append(i).append(";\n");
    out.append('\n');
    if (m.nativeConfig() != null) {
      NativeConfig config = m.nativeConfig();
      if (config.cContext() != null)
        out.append("@CContext(").append(config.cContext()).append(".class)\n");
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
        .append(SeamJson.quote(SeamJson.fingerprint(m)))
        .append(";\n");
    out.append("    public static final int BINDING_ABI_VERSION = ")
        .append(Seam.BINDING_ABI_VERSION)
        .append(";\n");
    leafBindings(m, out);
    for (Struct st : m.structs()) accessors(m, st, out);
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

  /**
   * Field access at the descriptor's offsets, for records in Java-managed memory (StackValue,
   * UnmanagedMemory) and for the reference form of by-value calls.
   */
  private static void accessors(Module m, Struct st, StringBuilder out) {
    out.append("    public static final class ")
        .append(st.name())
        .append(" {\n        private ")
        .append(st.name())
        .append("() {}\n        public static final int SIZE = ")
        .append(st.size())
        .append(";\n        public static final int ALIGNMENT = ")
        .append(st.alignment())
        .append(";\n");
    for (Field f : st.fields()) {
      String carrier = javaType(m, f.type());
      String at = "((Pointer) record)";
      String get, set;
      if (f.type() instanceof Scalar s)
        switch (s) {
          case BOOL -> {
            get = at + ".readByte(" + f.offset() + ") != 0";
            set = at + ".writeByte(" + f.offset() + ", (byte) (value ? 1 : 0))";
          }
          default -> {
            String kind =
                switch (s) {
                  case CHAR, I8, U8 -> "Byte";
                  case I16, U16 -> "Short";
                  case I32, U32 -> "Int";
                  case F32 -> "Float";
                  case F64 -> "Double";
                  default -> "Long";
                };
            get = at + ".read" + kind + "(" + f.offset() + ")";
            set = at + ".write" + kind + "(" + f.offset() + ", value)";
          }
        }
      else {
        get = at + ".readWord(" + f.offset() + ")";
        set = at + ".writeWord(" + f.offset() + ", value)";
      }
      out.append("        public static ")
          .append(carrier)
          .append(' ')
          .append(f.name())
          .append("(PointerBase record) {\n            return ")
          .append(get)
          .append(";\n        }\n        public static void ")
          .append(f.name())
          .append("(PointerBase record, ")
          .append(carrier)
          .append(" value) {\n            ")
          .append(set)
          .append(";\n        }\n");
    }
    out.append("    }\n");
  }
}
