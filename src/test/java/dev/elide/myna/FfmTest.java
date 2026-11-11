// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package dev.elide.myna;

import static org.junit.jupiter.api.Assertions.*;

import dev.elide.myna.frontend.Dsl;
import java.nio.file.*;
import org.junit.jupiter.api.Test;

class FfmTest {
  private static final String MODULE = "module callbacks target=aarch64-apple-darwin\n";

  private static java.util.Map<String, String> generate(String source) {
    return Generator.generate(Dsl.parse(source), true);
  }

  @Test
  void typedCriticalBindingsOnlyExposeLeaves() {
    var output =
        generate(
            MODULE
                + "import leaf return=i64 error=no_failure\n"
                + "param value type=i64\n"
                + "end\n"
                + "import slow return=void error=no_failure native_leaf=false@explicit_contract\n"
                + "end\n");
    String ffm = output.get("MynaFFM.java");
    String critical =
        ffm.substring(ffm.indexOf("public static final class CriticalLibraryBinding"));
    assertTrue(critical.contains("leaf_CriticalDowncall.HANDLE.invokeExact"));
    assertFalse(critical.contains("slow"));
    assertTrue(output.get("MynaForeignFeature.java").contains("MynaFFM$leaf_CriticalDowncall"));
    assertThrows(
        IllegalArgumentException.class, () -> generate(MODULE + "opaque CriticalLibraryBinding\n"));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            generate(
                MODULE
                    + "opaque leaf_CriticalDowncall\n"
                    + "import leaf return=i64 error=no_failure\n"
                    + "end\n"));
  }

  @Test
  void recordAllocatorAvoidsParameterNames() {
    var output =
        generate(
            MODULE
                + "struct Result size=8 align=8\n"
                + "field value type=i64 offset=0\n"
                + "end\n"
                + "import transform return=Result error=no_failure\n"
                + "param allocator type=Result\n"
                + "param allocator_ type=i32\n"
                + "end\n");
    assertTrue(
        output
            .get("MynaFFM.java")
            .contains(
                "transform(SegmentAllocator allocator__, MemorySegment allocator, int"
                    + " allocator_)"));
  }

  @Test
  void staticTargetsAndExceptionPolicies() throws Exception {
    var out = generate(Files.readString(Path.of("tests/callbacks/callbacks.seam")));
    assertTrue(
        out.get("CallbackApiFFM.java")
            .contains("return fixture.callbacks.CallbackOps.reduce(arg0, arg1)"));
    assertTrue(
        out.get("CallbackApiFFM.java").contains("catch (Throwable failure) { return -1L; }"));
    assertTrue(
        out.get("CallbackApiForeignFeature.java")
            .contains("registerForDirectUpcall(CallbackApiFFM.ReducerUpcalls.STATIC_HANDLE"));
    var scalar =
        generate(
            MODULE
                + "callback Status return=i32 ffm_java=fixture.Ops.status ffm_sentinel=09\nend\n");
    assertTrue(scalar.get("MynaFFM.java").contains("catch (Throwable failure) { return 9; }"));
    var types =
        generate(
            MODULE
                + "callback Done return=void\nend\n"
                + "callback Pointer return=ptr<u8>\nparam p type=ptr<u8>\nend\n");
    assertTrue(types.get("MynaFFM.java").contains("void callback: discard failure"));
    assertTrue(types.get("MynaFFM.java").contains("java.util.Objects.requireNonNull(sentinel)"));
    assertTrue(types.get("MynaFFM.java").contains("FunctionDescriptor.ofVoid()"));
  }

  @Test
  void rejectsIncompleteAndInvalidStaticPolicies() {
    assertThrows(
        IllegalArgumentException.class,
        () -> generate(MODULE + "opaque StatusUpcalls\ncallback Status return=i32\nend\n"));
    for (String header :
        new String[] {
          "return=i32 ffm_java=fixture.Ops.call",
          "return=i32 ffm_sentinel=-1",
          "return=f32 ffm_java=fixture.Ops.call ffm_sentinel=-1",
          "return=i32 ffm_java=fixture.Ops.call ffm_sentinel=2147483648",
          "return=i64 ffm_java=fixture.Ops.call ffm_sentinel=9223372036854775808"
        })
      assertThrows(
          IllegalArgumentException.class,
          () -> generate(MODULE + "callback Status " + header + "\nend\n"));
  }

  @Test
  void unsignedUpcallReturnsUseExplicitlyExtendedAbiCarriers() {
    String ffm =
        generate(
                MODULE
                    + "callback ByteResult return=u8\nend\n"
                    + "callback ShortResult return=u16\nend\n")
            .get("MynaFFM.java");
    assertTrue(
        ffm.contains(
            "ByteResult_CALLBACK_DESCRIPTOR = FunctionDescriptor.of(ValueLayout.JAVA_INT)"));
    assertTrue(
        ffm.contains(
            "ShortResult_CALLBACK_DESCRIPTOR = FunctionDescriptor.of(ValueLayout.JAVA_INT)"));
    assertTrue(ffm.contains("private static int invoke(ByteResultCallback target, byte sentinel)"));
    assertTrue(ffm.contains("return target.invoke() & 0xff;"));
    assertTrue(ffm.contains("return sentinel & 0xff;"));
    assertTrue(
        ffm.contains("private static int invoke(ShortResultCallback target, short sentinel)"));
    assertTrue(ffm.contains("return target.invoke() & 0xffff;"));
    assertTrue(ffm.contains("return sentinel & 0xffff;"));
  }

  @Test
  void segmentWrappersValidateEveryBuffer() throws Exception {
    String ffm = generate(OwnershipTest.TRANSFER).get("ApiFFM.java");
    // Every segment gets the single-buffer checks; a pointer beside them passes through.
    String wrapper =
        """
                /** Borrows each entire native segment for this call; lengths are in bytes. */
                public int transfer(MemorySegment arg0, MemorySegment arg1, MemorySegment arg4, MemorySegment arg5, byte arg6) throws Throwable {
                    if (!arg1.isNative()) throw new IllegalArgumentException("native segment required");
                    if (!arg1.scope().isAlive()) throw new IllegalStateException("closed buffer");
                    if (!arg1.isAccessibleBy(Thread.currentThread())) throw new WrongThreadException();
                    long length1 = arg1.byteSize();
                    if (length1 != 0 && arg1.address() == 0) throw new IllegalArgumentException("null buffer address");
                    if (!arg4.isNative()) throw new IllegalArgumentException("native segment required");
                    if (arg4.isReadOnly()) throw new IllegalArgumentException("writable segment required");
                    if (!arg4.scope().isAlive()) throw new IllegalStateException("closed buffer");
                    if (!arg4.isAccessibleBy(Thread.currentThread())) throw new WrongThreadException();
                    long length4 = arg4.byteSize();
                    if (length4 != 0 && arg4.address() == 0) throw new IllegalArgumentException("null buffer address");
                    if (length4 > 4294967295L) throw new IllegalArgumentException("buffer length exceeds ABI range");
                    if (length1 != 0 && length4 != 0) {
                        MemorySegment pointer1 = arg1;
                        MemorySegment pointer4 = arg4;
                        return this.transfer(arg0, pointer1, length1, (int) length4, pointer4, arg5, arg6);
                    }
                    try (Arena empty = Arena.ofConfined()) {
                        MemorySegment pointer1 = length1 != 0 ? arg1 : empty.allocate(1);
                        MemorySegment pointer4 = length4 != 0 ? arg4 : empty.allocate(1);
                        return this.transfer(arg0, pointer1, length1, (int) length4, pointer4, arg5, arg6);
                    }
                }
        """;
    assertTrue(ffm.contains(wrapper), ffm);
    // A leaf gets the wrapper on the critical binding as well.
    assertEquals(2, ffm.split(java.util.regex.Pattern.quote(wrapper), -1).length - 1, ffm);
    String none =
        generate(OwnershipTest.TRANSFER.replace("return=i32", "return=void")).get("ApiFFM.java");
    assertTrue(
        none.contains(
            "                this.transfer(arg0, pointer1, length1, (int) length4, pointer4, arg5,"
                + " arg6);\n"
                + "                return;\n"
                + "            }\n"
                + "            try (Arena empty = Arena.ofConfined()) {\n"),
        none);
    // One buffer beside a pointer keeps the single-buffer wrapper.
    String single =
        generate(
                Files.readString(Path.of("examples/ownership.seam"))
                    .replace(
                        "param count type=u64\nend\nborrow increment",
                        "param count type=u64\n  param seed type=ptr<u64> access=read\n"
                            + "end\nborrow increment"))
            .get("NativeApiFFM.java");
    assertTrue(
        single.contains(
            "        /** Borrows the entire native segment for this call; length is in bytes. */\n"
                + "        public long checksum(MemorySegment arg0, MemorySegment arg2) throws"
                + " Throwable {\n"),
        single);
    assertTrue(
        single.contains(
            "                MemorySegment pointer = empty.allocate(1);\n"
                + "                return this.checksum(pointer, length, arg2);\n"),
        single);
  }

  @Test
  void nullSegmentsAreAbsentNullableBuffers() {
    String ffm = generate(OwnershipTest.DECODE).get("ApiFFM.java");
    // An absent buffer (null, or the null address with length zero) is not validated and passes
    // MemorySegment.NULL; only a buffer that is present and empty needs temporary storage.
    String wrapper =
        """
                /** Borrows each entire native segment for this call; lengths are in bytes. */
                public long decode(MemorySegment arg0, MemorySegment arg2) throws Throwable {
                    if (!arg0.isNative()) throw new IllegalArgumentException("native segment required");
                    if (!arg0.scope().isAlive()) throw new IllegalStateException("closed buffer");
                    if (!arg0.isAccessibleBy(Thread.currentThread())) throw new WrongThreadException();
                    long length0 = arg0.byteSize();
                    if (length0 != 0 && arg0.address() == 0) throw new IllegalArgumentException("null buffer address");
                    boolean absent2 = arg2 == null || (arg2.isNative() && arg2.address() == 0 && arg2.byteSize() == 0);
                    long length2 = 0;
                    if (!absent2) {
                        if (!arg2.isNative()) throw new IllegalArgumentException("native segment required");
                        if (arg2.isReadOnly()) throw new IllegalArgumentException("writable segment required");
                        if (!arg2.scope().isAlive()) throw new IllegalStateException("closed buffer");
                        if (!arg2.isAccessibleBy(Thread.currentThread())) throw new WrongThreadException();
                        length2 = arg2.byteSize();
                        if (length2 != 0 && arg2.address() == 0) throw new IllegalArgumentException("null buffer address");
                        if (length2 > 4294967295L) throw new IllegalArgumentException("buffer length exceeds ABI range");
                    }
                    if (length0 != 0 && (absent2 || length2 != 0)) {
                        MemorySegment pointer0 = arg0;
                        MemorySegment pointer2 = absent2 ? MemorySegment.NULL : arg2;
                        return this.decode(pointer0, length0, pointer2, (int) length2);
                    }
                    try (Arena empty = Arena.ofConfined()) {
                        MemorySegment pointer0 = length0 != 0 ? arg0 : empty.allocate(1);
                        MemorySegment pointer2 = absent2 ? MemorySegment.NULL : length2 != 0 ? arg2 : empty.allocate(1);
                        return this.decode(pointer0, length0, pointer2, (int) length2);
                    }
                }
        """;
    assertTrue(ffm.contains(wrapper), ffm);
    // A leaf gets the wrapper on the critical binding as well.
    assertEquals(2, ffm.split(java.util.regex.Pattern.quote(wrapper), -1).length - 1, ffm);
    // One buffer: the temporary path is reached only by a buffer that is present and empty.
    assertTrue(
        ffm.contains(
            """
                    public long tally(MemorySegment arg0) throws Throwable {
                        boolean absent = arg0 == null || (arg0.isNative() && arg0.address() == 0 && arg0.byteSize() == 0);
                        long length = 0;
                        if (!absent) {
                            if (!arg0.isNative()) throw new IllegalArgumentException("native segment required");
                            if (!arg0.scope().isAlive()) throw new IllegalStateException("closed buffer");
                            if (!arg0.isAccessibleBy(Thread.currentThread())) throw new WrongThreadException();
                            length = arg0.byteSize();
                            if (length != 0 && arg0.address() == 0) throw new IllegalArgumentException("null buffer address");
                        }
                        if (absent || length != 0) {
                            MemorySegment pointer = absent ? MemorySegment.NULL : arg0;
                            return this.tally(pointer, length);
                        }
                        try (Arena empty = Arena.ofConfined()) {
                            MemorySegment pointer = empty.allocate(1);
                            return this.tally(pointer, length);
                        }
                    }
            """),
        ffm);
    // A void result, with a signed 32-bit length checked only for a buffer that is present.
    assertTrue(
        ffm.contains(
            """
                            length = arg1.byteSize();
                            if (length != 0 && arg1.address() == 0) throw new IllegalArgumentException("null buffer address");
                            if (length > 2147483647L) throw new IllegalArgumentException("buffer length exceeds ABI range");
                        }
                        if (absent || length != 0) {
                            MemorySegment pointer = absent ? MemorySegment.NULL : arg1;
                            this.clear((int) length, pointer);
                            return;
                        }
            """),
        ffm);
    // Declared non-null, the same buffers get the wrappers they have always had.
    String required =
        generate(
                OwnershipTest.DECODE
                    .replace("nullable=true", "nullable=false")
                    .replace(
                        "type=ptr<u8> ownership=borrowed",
                        "type=ptr<u8> nullable=false ownership=borrowed"))
            .get("ApiFFM.java");
    assertFalse(required.contains("absent"), required);
    assertFalse(required.contains("MemorySegment.NULL"), required);
    assertTrue(required.contains("            if (length0 != 0 && length2 != 0) {\n"), required);
  }

  @Test
  void resourceFactoriesHaveExplicitDestructors() throws Exception {
    String source = Files.readString(Path.of("examples/ownership.seam"));
    String ffm =
        generate(
                source
                    .replace("threading=shared", "threading=confined")
                    .replace(
                        "import ticketCreate symbol=ticket_create return=ptr<Ticket>"
                            + " error=no_failure nounwind=true@explicit_contract"
                            + " native_leaf=false@explicit_contract\n"
                            + "end",
                        "import ticketCreate symbol=ticket_create return=ptr<Ticket>"
                            + " error=no_failure nounwind=true@explicit_contract"
                            + " native_leaf=false@explicit_contract\n"
                            + "param size type=i64\n"
                            + "end"))
            .get("NativeApiFFM.java");
    assertTrue(ffm.contains("binding.ticketCreate(arg0)"));
    assertTrue(ffm.contains("binding::ticketDestroy, Thread.currentThread()"));
    assertTrue(ffm.contains("failure.addSuppressed(cleanup)"));
    assertThrows(
        IllegalArgumentException.class, () -> generate(source.replace("Ticket", "MemorySegment")));
    assertThrows(
        IllegalArgumentException.class, () -> generate(source.replace("Ticket", "Linker")));
    assertThrows(
        IllegalArgumentException.class, () -> generate(source.replace("Ticket", "NativeApiFFM")));
  }
}
