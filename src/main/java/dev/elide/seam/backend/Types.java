package dev.elide.seam.backend;

import dev.elide.seam.model.Seam.*;

final class Types {
  private Types() {}

  static String c(Type type) {
    if (type instanceof FunctionPointer f) return f.signature();
    if (type == Isolate.THREAD) return "void *";
    if (type instanceof Pointer p) return c(p.pointee()) + " *";
    if (type instanceof Named n) return "struct " + n.name();
    Scalar s = (Scalar) type;
    return switch (s) {
      case VOID -> "void";
      case BOOL -> "bool";
      case CHAR -> "char";
      case USIZE -> "size_t";
      case ISIZE -> "ptrdiff_t";
      case F32 -> "float";
      case F64 -> "double";
      default -> (s.text().startsWith("u") ? "uint" : "int") + s.bytes * 8 + "_t";
    };
  }

  static String c(Parameter p) {
    if (p.type() instanceof Pointer ptr && p.access() == Access.READ)
      return c(ptr.pointee()) + " const *";
    return c(p.type());
  }

  static String rust(Type type) {
    if (type instanceof FunctionPointer f) return f.signature();
    if (type == Isolate.THREAD) return "*mut core::ffi::c_void";
    if (type instanceof Pointer p) return "*mut " + rust(p.pointee());
    if (type instanceof Named n) return n.name();
    if (type == Scalar.VOID) return "core::ffi::c_void";
    if (type == Scalar.CHAR) return "core::ffi::c_char";
    return type.text();
  }

  /** Rust type with declared names qualified by {@code q}, for macros expanded elsewhere. */
  static String rust(Type type, String q) {
    if (type instanceof FunctionPointer f) return q + f.signature();
    if (type instanceof Named n) return q + n.name();
    if (type instanceof Pointer p) return "*mut " + rust(p.pointee(), q);
    return rust(type);
  }

  static String rust(Parameter p, String q) {
    if (p.type() instanceof Pointer ptr)
      return (p.access() == Access.READ ? "*const " : "*mut ") + rust(ptr.pointee(), q);
    return rust(p.type(), q);
  }

  static String rust(Parameter p) {
    if (p.type() instanceof Pointer ptr)
      return (p.access() == Access.READ ? "*const " : "*mut ") + rust(ptr.pointee());
    return rust(p.type());
  }

  /** Native Image word carriers; typed C pointers where the pointee has one. */
  static String java(Type type) {
    if (type instanceof FunctionPointer f) return f.signature();
    if (type == Isolate.THREAD) return "IsolateThread";
    if (type instanceof Pointer p) {
      if (p.pointee() instanceof Pointer inner && inner.pointee() instanceof Scalar s)
        if (s == Scalar.I8 || s == Scalar.U8 || s == Scalar.CHAR) return "CCharPointerPointer";
      if (!(p.pointee() instanceof Scalar s)) return "PointerBase";
      return switch (s) {
        case VOID -> "VoidPointer";
        case BOOL, CHAR, I8, U8 -> "CCharPointer";
        case USIZE, ISIZE -> "WordPointer";
        case I16, U16 -> "CShortPointer";
        case I32, U32 -> "CIntPointer";
        case I64, U64 -> "CLongPointer";
        case F32 -> "CFloatPointer";
        case F64 -> "CDoublePointer";
      };
    }
    return switch ((Scalar) type) {
      case VOID -> "void";
      case BOOL -> "boolean";
      case CHAR, I8, U8 -> "byte";
      case I16, U16 -> "short";
      case I32, U32 -> "int";
      case I64, U64, USIZE, ISIZE -> "long";
      case F32 -> "float";
      case F64 -> "double";
    };
  }

  static String ffmCarrier(Type type) {
    return type instanceof Scalar ? java(type) : "MemorySegment";
  }

  static String layout(Type type) {
    if (type instanceof Named n) return n.name() + "_LAYOUT"; // by value; declared in the FFM class
    if (!(type instanceof Scalar)) return "ValueLayout.ADDRESS";
    return switch ((Scalar) type) {
      case BOOL -> "ValueLayout.JAVA_BOOLEAN";
      case CHAR, I8, U8 -> "ValueLayout.JAVA_BYTE";
      case I16, U16 -> "ValueLayout.JAVA_SHORT";
      case I32, U32 -> "ValueLayout.JAVA_INT";
      case I64, U64, USIZE, ISIZE -> "ValueLayout.JAVA_LONG";
      case F32 -> "ValueLayout.JAVA_FLOAT";
      case F64 -> "ValueLayout.JAVA_DOUBLE";
      case VOID -> throw new IllegalArgumentException("void has no FFM layout");
    };
  }

  static String llvm(Type type) {
    if (!(type instanceof Scalar)) return "ptr";
    return switch ((Scalar) type) {
      case VOID -> "void";
      case BOOL -> "i1";
      case F32 -> "float";
      case F64 -> "double";
      default -> "i" + ((Scalar) type).bytes * 8;
    };
  }

  /**
   * C ABI extension of a sub-32-bit parameter, as clang and rustc both emit it: extended on x86-64
   * and Apple arm64, left to the callee on AAPCS64 Linux. Returns are not described: the producers
   * disagree on x86-64 Linux (clang extends, rustc does not).
   */
  static String extension(Type type, String target) {
    if (!(type instanceof Scalar s) || s.bytes >= 4 || s == Scalar.VOID) return "";
    if (target.equals("aarch64-unknown-linux-gnu")) return "";
    return switch (s) {
      case CHAR, I8, I16 -> "signext";
      default -> "zeroext";
    };
  }
}
