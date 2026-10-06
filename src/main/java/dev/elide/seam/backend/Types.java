package dev.elide.seam.backend;

import dev.elide.seam.model.Seam.*;

final class Types {
  private Types() {}

  static String c(Type type) {
    if (type instanceof Pointer p) return c(p.pointee()) + " *";
    if (type instanceof Named n) return "struct " + n.name();
    Scalar s = (Scalar) type;
    return switch (s) {
      case VOID -> "void";
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
    if (type instanceof Pointer p) return "*mut " + rust(p.pointee());
    if (type instanceof Named n) return n.name();
    return type == Scalar.VOID ? "core::ffi::c_void" : type.text();
  }

  static String rust(Parameter p) {
    if (p.type() instanceof Pointer ptr)
      return (p.access() == Access.READ ? "*const " : "*mut ") + rust(ptr.pointee());
    return rust(p.type());
  }

  static String java(Type type) {
    if (type instanceof Pointer) return "PointerBase";
    return switch ((Scalar) type) {
      case VOID -> "void";
      case I8, U8 -> "byte";
      case I16, U16 -> "short";
      case I32, U32 -> "int";
      case I64, U64 -> "long";
      case F32 -> "float";
      case F64 -> "double";
    };
  }

  static String ffmCarrier(Type type) {
    return type instanceof Pointer ? "MemorySegment" : java(type);
  }

  static String layout(Type type) {
    if (type instanceof Pointer) return "ValueLayout.ADDRESS";
    return switch ((Scalar) type) {
      case I8, U8 -> "ValueLayout.JAVA_BYTE";
      case I16, U16 -> "ValueLayout.JAVA_SHORT";
      case I32, U32 -> "ValueLayout.JAVA_INT";
      case I64, U64 -> "ValueLayout.JAVA_LONG";
      case F32 -> "ValueLayout.JAVA_FLOAT";
      case F64 -> "ValueLayout.JAVA_DOUBLE";
      case VOID -> throw new IllegalArgumentException("void has no FFM layout");
    };
  }

  static String llvm(Type type) {
    if (type instanceof Pointer) return "ptr";
    return switch ((Scalar) type) {
      case VOID -> "void";
      case F32 -> "float";
      case F64 -> "double";
      default -> "i" + ((Scalar) type).bytes * 8;
    };
  }
}
