// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package dev.elide.myna.backend;

import dev.elide.myna.model.Seam.*;
import dev.elide.myna.model.Seam.Module;
import java.util.*;

/** Scoped upcalls; no Java exception is allowed to escape a generated callback boundary. */
final class FfmCallbacks {
  private FfmCallbacks() {}

  static void generate(Module m, JavaFfm.StructLayouts structs, StringBuilder out) {
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
      String captures = returns ? "target, sentinel" : "target";
      String captureParams =
          name + "Callback target" + (returns ? ", " + carrier + " sentinel" : "");
      String parameterClasses = classes.isEmpty() ? "" : ", " + String.join(", ", classes);
      var layouts = new ArrayList<String>();
      // C callers trust narrow unsigned return values to be zero-extended. FFM's signed
      // byte/short carriers sign-extend, so project the same register through an int adapter.
      if (returns)
        layouts.add(unsignedReturn ? "ValueLayout.JAVA_INT" : structs.value(s.returns()));
      // A ptr<S> argument arrives sized to S, as an import's result does.
      s.parameters().forEach(p -> layouts.add(structs.value(p.type())));
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
          .append("        private final ")
          .append(name)
          .append("Callback target;\n");
      if (returns) out.append("        private final ").append(carrier).append(" sentinel;\n");
      out.append("        private ")
          .append(name)
          .append("Upcalls(")
          .append(captureParams)
          .append(") {\n            this.target = target;\n");
      if (returns) out.append("            this.sentinel = sentinel;\n");
      out.append("        }\n        private static final MethodHandle HANDLE;\n")
          .append("        static final MethodHandle DIRECT_HANDLE;\n");
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
          .append(parameterClasses)
          .append("));\n")
          .append(
              "                DIRECT_HANDLE ="
                  + " java.lang.invoke.MethodHandles.lookup().findVirtual(")
          .append(name)
          .append("Upcalls.class, \"invoke\", java.lang.invoke.MethodType.methodType(")
          .append(abiCarrier)
          .append(".class")
          .append(parameterClasses)
          .append("));\n");
      if (s.ffmTarget() != null)
        out.append(
                "                STATIC_HANDLE ="
                    + " java.lang.invoke.MethodHandles.lookup().findStatic(")
            .append(name)
            .append("Upcalls.class, \"invokeStatic\", java.lang.invoke.MethodType.methodType(")
            .append(carrier)
            .append(".class")
            .append(parameterClasses)
            .append("));\n");
      out.append(
              "            } catch (ReflectiveOperationException failure) { throw new"
                  + " ExceptionInInitializerError(failure); }\n"
                  + "        }\n")
          .append("        private ")
          .append(abiCarrier)
          .append(" invoke(")
          .append(String.join(", ", params))
          .append(") {\n            ")
          .append(returns ? "return " : "")
          .append("invoke(")
          .append(captures)
          .append(args.isEmpty() ? "" : ", " + String.join(", ", args))
          .append(");\n        }\n")
          .append("        private static ")
          .append(abiCarrier)
          .append(" invoke(")
          .append(captureParams)
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
                  + " them. */\n");
      boolean segmentSentinel = returns && carrier.equals("MemorySegment");
      appendFactory(
          out,
          name,
          "create",
          captureParams,
          segmentSentinel,
          "java.lang.invoke.MethodHandles.insertArguments(HANDLE, 0, " + captures + ")");
      // A single bound receiver lets Native Image specialize the trampoline for this adapter.
      out.append(
          "        /** Specializes repeated Native Image calls to this receiver. Uses the same"
              + " arena and exception policy as create. */\n");
      appendFactory(
          out,
          name,
          "createDirect",
          captureParams,
          segmentSentinel,
          "DIRECT_HANDLE.bindTo(new " + name + "Upcalls(" + captures + "))");
      if (s.ffmTarget() != null) {
        out.append("        private static ")
            .append(carrier)
            .append(" invokeStatic(")
            .append(String.join(", ", params))
            .append(") {\n            try { ")
            .append(returns ? "return " : "")
            .append(s.ffmTarget())
            .append('(')
            .append(String.join(", ", args))
            .append("); }\n")
            .append("            catch (Throwable failure) { ")
            .append(
                returns
                    ? "return "
                        + Long.parseLong(s.ffmSentinel())
                        + (s.returns() == Scalar.I64 ? "L;" : ";")
                    : "/* void callback: discard failure */")
            .append(" }\n        }\n")
            .append("        public static MemorySegment create(Arena arena) {\n")
            .append("            return Linker.nativeLinker().upcallStub(STATIC_HANDLE, ")
            .append(name)
            .append("_CALLBACK_DESCRIPTOR, arena);\n        }\n");
      }
      out.append("    }\n");
    }
  }

  private static void appendFactory(
      StringBuilder out,
      String name,
      String factory,
      String captures,
      boolean segmentSentinel,
      String handle) {
    out.append("        public static MemorySegment ")
        .append(factory)
        .append('(')
        .append(captures)
        .append(", Arena arena) {\n")
        .append("            java.util.Objects.requireNonNull(target);\n");
    if (segmentSentinel) out.append("            java.util.Objects.requireNonNull(sentinel);\n");
    out.append("            return Linker.nativeLinker().upcallStub(")
        .append(handle)
        .append(", ")
        .append(name)
        .append("_CALLBACK_DESCRIPTOR, arena);\n        }\n");
  }

  private static String returnExtension(Type type) {
    if (type == Scalar.U8) return " & 0xff";
    if (type == Scalar.U16) return " & 0xffff";
    return "";
  }
}
