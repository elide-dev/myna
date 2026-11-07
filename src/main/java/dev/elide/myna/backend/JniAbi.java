package dev.elide.myna.backend;

import dev.elide.myna.model.Seam.*;
import dev.elide.myna.model.Seam.Module;
import java.util.*;

/** Concrete JNI signatures, separate from the producer-neutral C ABI and its optimizer facts. */
public final class JniAbi {
  private JniAbi() {}

  public record Carrier(String java, String c, String llvm, String descriptor) {
    public String llvmArgument(String target) {
      Type scalar =
          switch (c) {
            case "jboolean" -> Scalar.U8;
            case "jbyte" -> Scalar.I8;
            case "jshort" -> Scalar.I16;
            default -> Scalar.I64;
          };
      String extension = Types.extension(scalar, target);
      return llvm + (extension.isEmpty() ? "" : " " + extension);
    }

    public String rust() {
      return switch (c) {
        case "void" -> "()";
        case "jboolean" -> "u8";
        case "jbyte" -> "i8";
        case "jshort" -> "i16";
        case "jint" -> "i32";
        case "jlong" -> "i64";
        case "jfloat" -> "f32";
        case "jdouble" -> "f64";
        default -> "*mut core::ffi::c_void";
      };
    }
  }

  public record Argument(String name, Carrier carrier) {}

  public record Method(
      Function implementation,
      String name,
      String symbol,
      Carrier returns,
      List<Argument> arguments,
      BufferBorrow borrow) {
    public Method {
      arguments = List.copyOf(arguments);
    }

    public String descriptor() {
      return "("
          + String.join("", arguments.stream().skip(2).map(a -> a.carrier().descriptor()).toList())
          + ")"
          + returns.descriptor();
    }
  }

  public static String className(Module m) {
    return m.nativeClass() + "JNI";
  }

  public static String library(Module m) {
    return m.name() + "_jni";
  }

  public static Carrier carrier(Type type) {
    if (!(type instanceof Scalar s)) return new Carrier("long", "jlong", "i64", "J");
    return switch (s) {
      case VOID -> new Carrier("void", "void", "void", "V");
      case BOOL -> new Carrier("boolean", "jboolean", "i8", "Z");
      case CHAR, I8, U8 -> new Carrier("byte", "jbyte", "i8", "B");
      case I16, U16 -> new Carrier("short", "jshort", "i16", "S");
      case I32, U32 -> new Carrier("int", "jint", "i32", "I");
      case I64, U64, ISIZE, USIZE -> new Carrier("long", "jlong", "i64", "J");
      case F32 -> new Carrier("float", "jfloat", "float", "F");
      case F64 -> new Carrier("double", "jdouble", "double", "D");
    };
  }

  public static List<Method> methods(Module m) {
    var result = new ArrayList<Method>();
    for (Function f : m.functions()) {
      if (f.direction() != Direction.IMPORT) continue;
      result.add(lower(m, f, null));
      for (BufferBorrow b : m.borrows()) {
        if (b.function().equals(f.name())) {
          if (m.functions().stream().anyMatch(other -> other.name().equals(f.name() + "Buffer")))
            throw new IllegalArgumentException(
                "JNI buffer adapter name collision: " + f.name() + "Buffer");
          result.add(lower(m, f, b));
        }
      }
    }
    return List.copyOf(result);
  }

  private static Method lower(Module m, Function f, BufferBorrow borrow) {
    var args = new ArrayList<Argument>();
    args.add(new Argument("env", new Carrier("", "JNIEnv *", "ptr", "")));
    args.add(new Argument("clazz", new Carrier("", "jclass", "ptr", "")));
    int i = 0;
    for (Parameter p : f.abiParameters()) {
      String name = "arg" + i++;
      if (borrow != null && p.name().equals(borrow.length())) continue;
      args.add(
          new Argument(
              name,
              borrow != null && p.name().equals(borrow.parameter())
                  ? new Carrier("java.nio.ByteBuffer", "jobject", "ptr", "Ljava/nio/ByteBuffer;")
                  : carrier(p.type())));
    }
    if (f.returns() instanceof Named)
      args.add(new Argument("result", carrier(new Pointer(f.returns()))));
    String name = f.name() + (borrow == null ? "" : "Buffer");
    // Long JNI names also distinguish a raw native method from a Java buffer overload.
    String descriptor =
        String.join("", args.stream().skip(2).map(a -> a.carrier().descriptor()).toList());
    String symbol =
        "Java_"
            + mangle(m.javaPackageName() + "." + className(m))
            + "_"
            + mangle(name)
            + "__"
            + mangle(descriptor);
    return new Method(
        f,
        name,
        symbol,
        carrier(f.returns() instanceof Named ? Scalar.VOID : f.returns()),
        args,
        borrow);
  }

  private static String mangle(String value) {
    return value
        .replace("_", "_1")
        .replace(";", "_2")
        .replace("[", "_3")
        .replace('.', '_')
        .replace('/', '_');
  }
}
