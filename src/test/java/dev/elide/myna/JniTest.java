// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package dev.elide.myna;

import static org.junit.jupiter.api.Assertions.*;

import dev.elide.myna.backend.JniAbi;
import dev.elide.myna.backend.StaticJni;
import dev.elide.myna.cli.Main;
import dev.elide.myna.frontend.Dsl;
import dev.elide.myna.model.Seam.*;
import java.io.*;
import java.nio.file.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JniTest {
  @TempDir Path temp;

  @Test
  void scalarCarriersAndImplicitArguments() {
    StringBuilder source = new StringBuilder("module jni_test target=aarch64-apple-darwin\n");
    for (Scalar s : Scalar.values()) {
      source
          .append("import call_")
          .append(s.text())
          .append(" return=")
          .append(s.text())
          .append(" error=no_failure\n");
      if (s != Scalar.VOID) source.append("param value type=").append(s.text()).append('\n');
      source.append("end\n");
    }
    var m = Dsl.parse(source.toString());
    var methods = JniAbi.methods(m);
    assertEquals(Scalar.values().length, methods.size());
    for (var method : methods) {
      assertEquals("JNIEnv *", method.arguments().getFirst().carrier().c());
      assertEquals("jclass", method.arguments().get(1).carrier().c());
      assertTrue(method.symbol().startsWith("Java_dev_elide_myna_generated_MynaNativeJNI_call_1"));
      assertTrue(method.descriptor().startsWith("("));
    }
    String c = StaticJni.generate(m).get("seam-jni.c");
    assertTrue(c.contains("JNI_OnLoad_jni_test_jni"));
    assertTrue(c.contains("return (jbyte)call_u8((uint8_t)arg0)"));
    assertTrue(c.contains("return (jboolean)call_bool((bool)arg0)"));
    String contracts = StaticJni.generate(m).get("seam-jni.ll");
    assertTrue(contracts.contains("i8 signext %arg0"));
    assertTrue(contracts.contains("i8 zeroext %arg0"));
    assertFalse(
        StaticJni.generate(m.withTarget("aarch64-unknown-linux-gnu"))
            .get("seam-jni.ll")
            .contains("signext"));
  }

  @Test
  void adaptersPreserveRawAbiAndDoNotCopyEffects() throws Exception {
    var m = Dsl.parse(Files.readString(Path.of("examples/ownership.seam")));
    var output = StaticJni.generate(m);
    String c = output.get("seam-jni.c"),
        java = output.get("NativeApiJNI.java"),
        llvm = output.get("seam-jni.ll");
    assertTrue(c.contains("return (jlong)(uintptr_t)ticket_create()"));
    assertTrue(c.contains("ticket_destroy((struct Ticket *)(uintptr_t)arg0)"));
    assertTrue(c.contains("GetDirectBufferAddress"));
    assertTrue(java.contains("arg0.slice()"));
    assertTrue(java.contains("isReadOnly()"));
    assertTrue(
        llvm.lines()
            .filter(l -> l.startsWith("declare") && l.contains("@Java_"))
            .noneMatch(
                l -> l.contains("nounwind") || l.contains("readonly") || l.contains("captures")));
    assertTrue(llvm.contains("@buffer_checksum(ptr captures(none) readonly"));
    assertTrue(
        JniAbi.methods(m).stream()
            .anyMatch(x -> x.descriptor().equals("(Ljava/nio/ByteBuffer;)J")));
    var buffer = Dsl.parse(Files.readString(Path.of("examples/buffer.seam")));
    c = StaticJni.generate(buffer).get("seam-jni.c");
    assertTrue(c.contains("*(struct Triple *)(uintptr_t)result = seam_rotate"));
    assertTrue(c.contains("*(struct Triple *)(uintptr_t)arg0"));
    assertFalse(
        JniAbi.methods(buffer).stream()
            .anyMatch(x -> x.implementation().direction() == Direction.EXPORT));
    String source = Files.readString(Path.of("examples/ownership.seam"));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            StaticJni.generate(
                Dsl.parse(source + "\nimport checksumBuffer return=void error=no_failure\nend\n")));
    assertThrows(
        IllegalArgumentException.class,
        () -> StaticJni.generate(Dsl.parse("module empty target=aarch64-apple-darwin\n")));
  }

  @Test
  void bufferAdaptersTakeOneByteBufferPerBorrow() {
    var m = Dsl.parse(OwnershipTest.TRANSFER);
    var methods = JniAbi.methods(m);
    // One raw method and one buffer adapter, whatever the number of borrows.
    assertEquals(
        List.of("transfer", "transferBuffer"), methods.stream().map(x -> x.name()).toList());
    var adapter = methods.getLast();
    assertEquals(2, adapter.borrows().size());
    assertTrue(methods.getFirst().borrows().isEmpty());
    // Both lengths are derived; the other pointers stay raw addresses.
    assertEquals("(JLjava/nio/ByteBuffer;Ljava/nio/ByteBuffer;JB)I", adapter.descriptor());
    assertEquals(
        "Java_dev_elide_myna_generated_ApiJNI_transferBuffer"
            + "__JLjava_nio_ByteBuffer_2Ljava_nio_ByteBuffer_2JB",
        adapter.symbol());
    var output = StaticJni.generate(m);
    String java = output.get("ApiJNI.java");
    assertTrue(
        java.contains(
            """
                public static native int transfer(long arg0, long arg1, long arg2, int arg3, long arg4, long arg5, byte arg6);
                private static native int transferBuffer(long arg0, java.nio.ByteBuffer arg1, java.nio.ByteBuffer arg4, long arg5, byte arg6);
                /** Borrows remaining bytes of each direct buffer; preserves their positions.
                 * Caller must keep any backing arena open throughout the call, including callbacks.
                 * Liveness/thread checks do not pin the arena against concurrent or reentrant closure. */
                public static int transfer(long arg0, java.nio.ByteBuffer arg1, java.nio.ByteBuffer arg4, long arg5, byte arg6) {
                    if (!arg1.isDirect()) throw new IllegalArgumentException("direct buffer required");
                    java.lang.foreign.MemorySegment segment1 = java.lang.foreign.MemorySegment.ofBuffer(arg1);
                    if (!segment1.scope().isAlive()) throw new IllegalStateException("closed buffer");
                    if (!segment1.isAccessibleBy(Thread.currentThread())) throw new WrongThreadException();
                    if (!arg4.isDirect()) throw new IllegalArgumentException("direct buffer required");
                    if (arg4.isReadOnly()) throw new IllegalArgumentException("writable buffer required");
                    java.lang.foreign.MemorySegment segment4 = java.lang.foreign.MemorySegment.ofBuffer(arg4);
                    if (!segment4.scope().isAlive()) throw new IllegalStateException("closed buffer");
                    if (!segment4.isAccessibleBy(Thread.currentThread())) throw new WrongThreadException();
                    return transferBuffer(arg0, arg1.slice(), arg4.slice(), arg5, arg6);
                }
            """),
        java);
    // Each buffer has locals of its own; a length that precedes its buffer still reads them.
    String c = output.get("seam-jni.c");
    assertTrue(
        c.contains(
            """
              jlong length1 = arg1 == NULL ? -1 : (*env)->GetDirectBufferCapacity(env, arg1);
              if ((*env)->ExceptionCheck(env)) return 0;
              void *pointer1 = arg1 == NULL ? NULL : (*env)->GetDirectBufferAddress(env, arg1);
              if ((*env)->ExceptionCheck(env)) return 0;
              if (length1 < 0 || (length1 != 0 && pointer1 == NULL)) {
            """),
        c);
    assertTrue(
        c.contains(
            """
              uint8_t empty1 = 0;
              if (length1 == 0) pointer1 = &empty1;
              jlong length4 = arg4 == NULL ? -1 : (*env)->GetDirectBufferCapacity(env, arg4);
            """),
        c);
    assertTrue(
        c.contains(
            """
              uint8_t empty4 = 0;
              if (length4 == 0) pointer4 = &empty4;
              return (jint)transfer((struct Session *)(uintptr_t)arg0, pointer1, (uint64_t)length1, (uint32_t)length4, pointer4, (uint32_t *)(uintptr_t)arg5, (uint8_t)arg6);
            }
            """),
        c);
    // The raw adapter is untouched by the borrows.
    assertTrue(
        c.contains(
            "  return (jint)transfer((struct Session *)(uintptr_t)arg0, (uint8_t *)(uintptr_t)arg1,"
                + " (uint64_t)arg2, (uint32_t)arg3, (uint8_t *)(uintptr_t)arg4, (uint32_t"
                + " *)(uintptr_t)arg5, (uint8_t)arg6);\n"),
        c);
    // A native symbol named like a buffer local is not hidden by it.
    String shadowed =
        StaticJni.generate(
                Dsl.parse(
                    OwnershipTest.TRANSFER.replace(
                        "import transfer return=i32", "import transfer symbol=length4 return=i32")))
            .get("seam-jni.c");
    assertTrue(shadowed.contains("  jlong length4_ = arg4 == NULL ? -1 :"), shadowed);
    assertTrue(shadowed.contains("(uint32_t)length4_, pointer4,"), shadowed);
    assertTrue(shadowed.contains("return (jint)length4((struct Session *)"), shadowed);
  }

  @Test
  void nullByteBuffersAreAbsentNullableBuffers() {
    var output = StaticJni.generate(Dsl.parse(OwnershipTest.DECODE));
    String java = output.get("ApiJNI.java");
    // Only a buffer that is present is checked and sliced; the non-nullable one is as before.
    assertTrue(
        java.contains(
            """
                public static long decode(java.nio.ByteBuffer arg0, java.nio.ByteBuffer arg2) {
                    if (!arg0.isDirect()) throw new IllegalArgumentException("direct buffer required");
                    java.lang.foreign.MemorySegment segment0 = java.lang.foreign.MemorySegment.ofBuffer(arg0);
                    if (!segment0.scope().isAlive()) throw new IllegalStateException("closed buffer");
                    if (!segment0.isAccessibleBy(Thread.currentThread())) throw new WrongThreadException();
                    if (arg2 != null) {
                        if (!arg2.isDirect()) throw new IllegalArgumentException("direct buffer required");
                        if (arg2.isReadOnly()) throw new IllegalArgumentException("writable buffer required");
                        java.lang.foreign.MemorySegment segment2 = java.lang.foreign.MemorySegment.ofBuffer(arg2);
                        if (!segment2.scope().isAlive()) throw new IllegalStateException("closed buffer");
                        if (!segment2.isAccessibleBy(Thread.currentThread())) throw new WrongThreadException();
                    }
                    return decodeBuffer(arg0.slice(), arg2 == null ? null : arg2.slice());
                }
            """),
        java);
    assertTrue(
        java.contains(
            """
                public static long tally(java.nio.ByteBuffer arg0) {
                    if (arg0 != null) {
                        if (!arg0.isDirect()) throw new IllegalArgumentException("direct buffer required");
                        java.lang.foreign.MemorySegment segment = java.lang.foreign.MemorySegment.ofBuffer(arg0);
                        if (!segment.scope().isAlive()) throw new IllegalStateException("closed buffer");
                        if (!segment.isAccessibleBy(Thread.currentThread())) throw new WrongThreadException();
                    }
                    return tallyBuffer(arg0 == null ? null : arg0.slice());
                }
            """),
        java);
    assertTrue(java.contains("        clearBuffer(arg1 == null ? null : arg1.slice());\n"), java);
    // The adapter keeps NULL and length zero for an absent buffer, where an invalid one is
    // rejected and an empty one gets storage; a null jobject for the other buffer is still invalid.
    String c = output.get("seam-jni.c");
    assertTrue(
        c.contains(
            """
              jlong length0 = arg0 == NULL ? -1 : (*env)->GetDirectBufferCapacity(env, arg0);
            """),
        c);
    assertTrue(
        c.contains(
            """
              uint8_t empty0 = 0;
              if (length0 == 0) pointer0 = &empty0;
              jlong length2 = arg2 == NULL ? 0 : (*env)->GetDirectBufferCapacity(env, arg2);
              if ((*env)->ExceptionCheck(env)) return 0;
              void *pointer2 = arg2 == NULL ? NULL : (*env)->GetDirectBufferAddress(env, arg2);
              if ((*env)->ExceptionCheck(env)) return 0;
              if (length2 < 0 || (length2 != 0 && pointer2 == NULL)) {
            """),
        c);
    assertTrue(
        c.contains(
            """
              uint8_t empty2 = 0;
              if (arg2 != NULL && length2 == 0) pointer2 = &empty2;
              return (jlong)decode(pointer0, (uint64_t)length0, pointer2, (uint32_t)length2);
            }
            """),
        c);
    assertTrue(
        c.contains(
            """
              uint8_t empty = 0;
              if (arg1 != NULL && length == 0) pointer = &empty;
              clear((int32_t)length, pointer);
            }
            """),
        c);
    // Nullability changes neither the method's name nor its descriptor.
    var required =
        JniAbi.methods(Dsl.parse(OwnershipTest.DECODE.replace("nullable=true", "nullable=false")));
    var optional = JniAbi.methods(Dsl.parse(OwnershipTest.DECODE));
    assertEquals(
        required.stream().map(JniAbi.Method::symbol).toList(),
        optional.stream().map(JniAbi.Method::symbol).toList());
  }

  @Test
  void cliMakesJniOptionalAndChecksItsOutputs() throws Exception {
    var out = new PrintStream(new ByteArrayOutputStream());
    Path destination = temp.resolve("generated");
    String[] args = {
      "generate", "examples/ownership.seam", "--out", destination.toString(), "--jni"
    };
    assertEquals(0, Main.run(args, out, out));
    assertTrue(Files.exists(destination.resolve("seam-jni.c")));
    assertEquals(
        0,
        Main.run(
            new String[] {
              "generate",
              "examples/ownership.seam",
              "--out",
              destination.toString(),
              "--jni",
              "--check"
            },
            out,
            out));
    assertEquals(
        2, Main.run(new String[] {"validate", "examples/ownership.seam", "--jni"}, out, out));
  }
}
