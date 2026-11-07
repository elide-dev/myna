// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package dev.elide.myna.backend;

import dev.elide.myna.model.Seam.*;
import dev.elide.myna.model.Seam.Module;
import java.util.*;

/** Scoped upcalls; no Java exception is allowed to escape a generated callback boundary. */
final class FfmCallbacks {
  private FfmCallbacks() {}

  static void generate(Module m, StringBuilder out) {
    for (Signature s : m.callbacks()) {
      String name = s.name(), carrier = Types.ffmCarrier(s.returns());
      boolean unsignedReturn = s.returns() == Scalar.U8 || s.returns() == Scalar.U16;
      String abiCarrier = unsignedReturn ? "int" : carrier;
      boolean returns = s.returns() != Scalar.VOID;
      var params = new ArrayList<String>();
      var args = new ArrayList<String>();
      var classes = new ArrayList<String>();
      for (int i = 0; i < s.parameters().size(); i++) {
        Type t = s.parameters().get(i).type();
        params.add(Types.ffmCarrier(t) + " arg" + i);
        args.add("arg" + i);
        classes.add(Types.ffmCarrier(t) + ".class");
      }
      var layouts = new ArrayList<String>();
      // C callers trust narrow unsigned return values to be zero-extended. FFM's signed
      // byte/short carriers sign-extend, so project the same register through an int adapter.
      if (returns) layouts.add(unsignedReturn ? "ValueLayout.JAVA_INT" : Types.layout(s.returns()));
      s.parameters().forEach(p -> layouts.add(Types.layout(p.type())));
      out.append("    public static final FunctionDescriptor ")
          .append(name)
          .append("_CALLBACK_DESCRIPTOR = FunctionDescriptor.")
          .append(returns ? "of(" : "ofVoid(")
          .append(String.join(", ", layouts))
          .append(");\n")
          .append("    @FunctionalInterface public interface ")
          .append(name)
          .append("Callback {\n")
          .append("        ")
          .append(carrier)
          .append(" invoke(")
          .append(String.join(", ", params))
          .append(") throws Throwable;\n    }\n")
          .append("    public static final class ")
          .append(name)
          .append("Upcalls {\n")
          .append("        private ")
          .append(name)
          .append("Upcalls() {}\n")
          .append("        private static final MethodHandle HANDLE;\n");
      if (s.ffmTarget() != null)
        out.append("        public static final MethodHandle STATIC_HANDLE;\n");
      out.append("        static {\n            try {\n")
          .append("                HANDLE = java.lang.invoke.MethodHandles.lookup().findStatic(")
          .append(name)
          .append("Upcalls.class, \"invoke\", java.lang.invoke.MethodType.methodType(")
          .append(abiCarrier)
          .append(".class, ")
          .append(name)
          .append("Callback.class")
          .append(returns ? ", " + carrier + ".class" : "")
          .append(classes.isEmpty() ? "" : ", " + String.join(", ", classes))
          .append("));\n");
      if (s.ffmTarget() != null)
        out.append(
                "                STATIC_HANDLE ="
                    + " java.lang.invoke.MethodHandles.lookup().findStatic(")
            .append(name)
            .append("Upcalls.class, \"invokeStatic\", java.lang.invoke.MethodType.methodType(")
            .append(carrier)
            .append(".class")
            .append(classes.isEmpty() ? "" : ", " + String.join(", ", classes))
            .append("));\n");
      out.append(
              "            } catch (ReflectiveOperationException failure) { throw new"
                  + " ExceptionInInitializerError(failure); }\n"
                  + "        }\n")
          .append("        private static ")
          .append(abiCarrier)
          .append(" invoke(")
          .append(name)
          .append("Callback target")
          .append(returns ? ", " + carrier + " sentinel" : "")
          .append(params.isEmpty() ? "" : ", " + String.join(", ", params))
          .append(") {\n")
          .append("            try { ")
          .append(returns ? "return " : "")
          .append("target.invoke(")
          .append(String.join(", ", args))
          .append(')')
          .append(returnExtension(s.returns()))
          .append("; }\n")
          .append("            catch (Throwable failure) { ")
          .append(
              returns
                  ? "return sentinel" + returnExtension(s.returns()) + ";"
                  : "/* void callback: discard failure */")
          .append(" }\n        }\n")
          .append(
              "        /** Caller owns arena; keep it alive until native code has stopped using the"
                  + " stub.\n")
          .append(
              "         * Use a shared arena for native threads. Exceptions return the supplied"
                  + " sentinel.\n")
          .append(
              "         * A void callback discards exceptions; catch inside the callback to report"
                  + " them. */\n")
          .append("        public static MemorySegment create(")
          .append(name)
          .append("Callback target")
          .append(returns ? ", " + carrier + " sentinel" : "")
          .append(", Arena arena) {\n")
          .append("            java.util.Objects.requireNonNull(target);\n");
      if (returns && carrier.equals("MemorySegment"))
        out.append("            java.util.Objects.requireNonNull(sentinel);\n");
      out.append(
              "            return"
                  + " Linker.nativeLinker().upcallStub(java.lang.invoke.MethodHandles.insertArguments(HANDLE,"
                  + " 0, target")
          .append(returns ? ", sentinel" : "")
          .append("), ")
          .append(name)
          .append("_CALLBACK_DESCRIPTOR, arena);\n        }\n");
      if (s.ffmTarget() != null) {
        out.append("        private static ")
            .append(carrier)
            .append(" invokeStatic(")
            .append(String.join(", ", params))
            .append(") {\n            try { return ")
            .append(s.ffmTarget())
            .append('(')
            .append(String.join(", ", args))
            .append("); }\n")
            .append("            catch (Throwable failure) { return ")
            .append(Long.toString(Long.parseLong(s.ffmSentinel())))
            .append(s.returns() == Scalar.I64 ? "L" : "")
            .append("; }\n        }\n")
            .append("        public static MemorySegment create(Arena arena) {\n")
            .append("            return Linker.nativeLinker().upcallStub(STATIC_HANDLE, ")
            .append(name)
            .append("_CALLBACK_DESCRIPTOR, arena);\n        }\n");
      }
      out.append("    }\n");
    }
  }

  private static String returnExtension(Type type) {
    if (type == Scalar.U8) return " & 0xff";
    if (type == Scalar.U16) return " & 0xffff";
    return "";
  }
}
