// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package dev.elide.myna;

import static org.junit.jupiter.api.Assertions.*;

import dev.elide.myna.backend.StaticJni;
import dev.elide.myna.frontend.Dsl;
import dev.elide.myna.serialize.SeamJson;
import java.nio.file.*;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class OwnershipTest {
  static String source() throws Exception {
    return Files.readString(Path.of("examples/ownership.seam"));
  }

  @Test
  void generatesConsumerPolicyAndRustOwners() throws Exception {
    var module = Dsl.parse(source());
    var output = Generator.generate(module, true);
    String java = output.get("NativeApiOwnership.java"), rust = output.get("seam.rs");
    assertTrue(java.contains("interface ResourceFactory<T>"));
    assertTrue(java.contains("interface NativeBuffer"));
    assertFalse(java.contains("Cleaner"));
    assertFalse(java.contains("AutoCloseable"));
    assertTrue(java.contains("factory.adopt(address, DESTRUCTOR, null)"));
    assertTrue(java.contains("DESTRUCTOR.destroy(address)"));
    assertTrue(java.contains(".acquire(true)"));
    assertTrue(java.contains(".release(false)"));
    assertTrue(rust.contains("unsafe impl core::marker::Send for Ticket"));
    assertTrue(rust.contains("unsafe impl core::marker::Sync for Ticket"));
    assertTrue(rust.contains("impl core::ops::Drop for Ticket"));
    assertTrue(rust.contains("TicketBorrow<'owner>"));
    assertTrue(rust.contains("core::mem::ManuallyDrop"));
    assertTrue(rust.contains("checksum(bytes: &[u8]) -> u64"));
    assertTrue(rust.contains("increment(bytes: &mut [u8])"));
    assertTrue(SeamJson.canonical(module).contains("\"threading\":\"shared\""));
    assertTrue(
        SeamJson.canonical(module.withTarget("x86_64-unknown-linux-gnu")).contains("\"borrows\""));
    assertEquals(module.resources(), module.withTarget("x86_64-unknown-linux-gnu").resources());
    var confined =
        Generator.generate(
            Dsl.parse(source().replace("threading=shared", "threading=confined")), true);
    assertFalse(confined.get("seam.rs").contains("unsafe impl core::marker::Send"));
    assertTrue(
        confined
            .get("NativeApiOwnership.java")
            .contains("factory.adopt(address, DESTRUCTOR, java.lang.Thread.currentThread())"));
    assertNotEquals(output.get("seam.abi"), confined.get("seam.abi"));
    assertFalse(output.get("seam.ll").contains("noalias"));
  }

  @Test
  void scalarConstructorsAndIntegerLengths() throws Exception {
    for (String length : new String[] {"u32", "i32", "i64"}) {
      var output =
          Generator.generate(
              Dsl.parse(
                  source()
                      .replace("param count type=u64", "param count type=" + length)
                      .replace(
                          "import ticketCreate symbol=ticket_create return=ptr<Ticket>"
                              + " error=no_failure nounwind=true@explicit_contract"
                              + " native_leaf=false@explicit_contract\n"
                              + "end",
                          "import ticketCreate symbol=ticket_create return=ptr<Ticket>"
                              + " error=no_failure nounwind=true@explicit_contract"
                              + " native_leaf=false@explicit_contract\n"
                              + "param size type=u64\n"
                              + "end")),
              true);
      assertTrue(output.get("seam.rs").contains("pub fn create(size: u64)"));
      assertTrue(
          output
              .get("NativeApiOwnership.java")
              .contains("create(ResourceFactory<T> factory, long arg0)"));
      if (!length.equals("i64"))
        assertTrue(
            output.get("NativeApiOwnership.java").contains("buffer length exceeds ABI range"));
    }
  }

  @Test
  void ownershipAndCallbackMetadataSurviveTargetOverride() throws Exception {
    String source =
        source()
            + "\n"
            + "callback Sink return=i32\n"
            + "param thread type=isolate_thread\n"
            + "param value type=usize\n"
            + "end\n"
            + "import runCallback return=void error=no_failure\n"
            + "param cb type=fn<Sink>\n"
            + "end\n";
    var original = Dsl.parse(source);
    var retargeted = original.withTarget("x86_64-unknown-linux-gnu");
    assertEquals(original.callbacks(), retargeted.callbacks());
    assertEquals(original.resources(), retargeted.resources());
    assertEquals(original.borrows(), retargeted.borrows());
    var output = Generator.generate(retargeted, true);
    assertTrue(output.get("seam.json").contains("\"callbacks\""));
    assertTrue(output.containsKey("NativeApiOwnership.java"));
    assertTrue(output.get("seam.rs").contains("TicketBorrow<'owner>"));
    assertTrue(output.get("NativeApi.java").contains("interface Sink"));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "representation=native|representation=java",
        "threading=shared|threading=guess",
        "lifetime=call|lifetime=owner",
        "create=ticketCreate|create=missing",
        "destroy=ticketDestroy|destroy=ticketCreate",
        "resource Ticket|resource Missing",
        "return=ptr<Ticket>|return=i64",
        "ownership=transferred_to_callee|ownership=borrowed",
        "access=read_write\nend\n\n#|access=read\nend\n\n#",
        "borrow checksum bytes|borrow absent bytes",
        "borrow checksum bytes|borrow checksum missing",
        "length=count|length=bytes",
        "type=u64|type=f64",
        "nocapture=true@explicit_contract|nocapture=false@explicit_contract",
        "readonly=true@explicit_contract|readonly=false@explicit_contract",
        "ownership=borrowed|ownership=owned_by_caller",
        "access=read nocapture|access=write nocapture",
        "readonly=true@explicit_contract|readonly=true@heuristic",
        "type=ptr<u8>|type=ptr<i64>",
        "nullable=false ownership=borrowed|ownership=borrowed",
        "nullable=false ownership=borrowed|nullable=true ownership=borrowed",
        "readonly=true@explicit_contract|readonly=true@explicit_contract"
            + " noalias=true@explicit_contract",
        "resource Ticket representation|resource Ticket representation=native threading=shared"
            + " create=ticketCreate destroy=ticketDestroy\n"
            + "resource Ticket representation",
        "borrow checksum bytes length=count lifetime=call|borrow checksum bytes length=count"
            + " lifetime=call\n"
            + "borrow checksum bytes length=count lifetime=call",
        "nounwind=true@explicit_contract|nounwind=true@heuristic"
      })
  void rejectsUnsafeOrUnsupportedContractsEvenRelaxed(String mutation) throws Exception {
    String[] parts = mutation.split("\\|", -1);
    String original = source();
    assertTrue(original.contains(parts[0]), "mutation must affect fixture");
    String changed = original.replace(parts[0], parts[1]);
    assertThrows(
        IllegalArgumentException.class, () -> Generator.generate(Dsl.parse(changed), false));
  }

  /** Two buffers with lengths of their own, one length ahead of its buffer, beside pointers. */
  static final String TRANSFER =
      "module transfers target=aarch64-apple-darwin java_class=Api\n"
          + "opaque Session\n"
          + "borrow transfer output length=capacity lifetime=call\n"
          + "borrow transfer input length=count lifetime=call\n"
          + "import transfer return=i32 error=no_failure nounwind=true@explicit_contract\n"
          + "  param session type=ptr<Session> nullable=false ownership=borrowed"
          + " access=read_write\n"
          + "  param input type=ptr<u8> nullable=false ownership=borrowed access=read"
          + " nocapture=true@explicit_contract readonly=true@explicit_contract\n"
          + "  param count type=u64\n"
          + "  param capacity type=u32\n"
          + "  param output type=ptr<u8> nullable=false ownership=borrowed access=read_write"
          + " nocapture=true@explicit_contract\n"
          + "  param written type=ptr<u32> nullable=true ownership=borrowed access=write\n"
          + "  param flags type=u8\n"
          + "end\n";

  @Test
  void severalBuffersAcquireInParameterOrderAndReleaseInReverse() {
    String java = Generator.generate(Dsl.parse(TRANSFER), true).get("ApiOwnership.java");
    // Other pointers keep the raw binding's carriers; every buffer is acquired and validated in
    // parameter order and released in reverse, each in a finally of its own.
    assertTrue(
        java.contains(
            """
                public static int transfer(org.graalvm.word.PointerBase arg0, NativeBuffer arg1, NativeBuffer arg4, org.graalvm.nativeimage.c.type.CIntPointer arg5, byte arg6) {
                    arg1.acquire(false);
                    try {
                        long length1 = arg1.byteSize();
                        long address1 = arg1.address();
                        if (length1 < 0 || (address1 == 0 && length1 != 0)) throw new IllegalArgumentException("invalid buffer extent");
                        org.graalvm.nativeimage.c.type.CCharPointer pointer1 = address1 == 0
                            ? org.graalvm.nativeimage.StackValue.get(1) : org.graalvm.word.WordFactory.pointer(address1);
                        arg4.acquire(true);
                        try {
                            long length4 = arg4.byteSize();
                            if (length4 > 4294967295L) throw new IllegalArgumentException("buffer length exceeds ABI range");
                            long address4 = arg4.address();
                            if (length4 < 0 || (address4 == 0 && length4 != 0)) throw new IllegalArgumentException("invalid buffer extent");
                            org.graalvm.nativeimage.c.type.CCharPointer pointer4 = address4 == 0
                                ? org.graalvm.nativeimage.StackValue.get(1) : org.graalvm.word.WordFactory.pointer(address4);
                            return Api.transfer(arg0, pointer1, length1, (int) length4, pointer4, arg5, arg6);
                        } finally {
                            arg4.release(true);
                            java.lang.ref.Reference.reachabilityFence(arg4);
                        }
                    } finally {
                        arg1.release(false);
                        java.lang.ref.Reference.reachabilityFence(arg1);
                    }
                }
            """),
        java);
    // Arrays: non-empty ones are pinned in turn and unpinned in reverse; empty ones get the dummy.
    assertTrue(
        java.contains(
            """
                /** Array convenience path; caller prevents conflicting concurrent array access. */
                public static int transfer(org.graalvm.word.PointerBase arg0, byte[] arg1, byte[] arg4, org.graalvm.nativeimage.c.type.CIntPointer arg5, byte arg6) {
                    long length1 = arg1.length;
                    long length4 = arg4.length;
                    org.graalvm.nativeimage.PinnedObject pin1 = length1 == 0 ? null : org.graalvm.nativeimage.PinnedObject.create(arg1);
                    try {
                        org.graalvm.nativeimage.c.type.CCharPointer pointer1 = pin1 == null
                            ? org.graalvm.nativeimage.StackValue.get(1) : pin1.addressOfArrayElement(0);
                        org.graalvm.nativeimage.PinnedObject pin4 = length4 == 0 ? null : org.graalvm.nativeimage.PinnedObject.create(arg4);
                        try {
                            org.graalvm.nativeimage.c.type.CCharPointer pointer4 = pin4 == null
                                ? org.graalvm.nativeimage.StackValue.get(1) : pin4.addressOfArrayElement(0);
                            return Api.transfer(arg0, pointer1, length1, (int) length4, pointer4, arg5, arg6);
                        } finally {
                            if (pin4 != null) pin4.close();
                        }
                    } finally {
                        if (pin1 != null) pin1.close();
                    }
                }
            """),
        java);
    // Plain try/finally only: these wrappers may sit on partially evaluated paths (#87).
    assertFalse(java.contains("try ("), java);
    assertFalse(java.contains("addSuppressed"), java);
    // A void result needs no early return when several arrays share one call site.
    String none =
        Generator.generate(Dsl.parse(TRANSFER.replace("return=i32", "return=void")), true)
            .get("ApiOwnership.java");
    assertTrue(
        none.contains(
            "                Api.transfer(arg0, pointer1, length1, (int) length4, pointer4, arg5,"
                + " arg6);\n"
                + "            } finally {\n"
                + "                if (pin4 != null) pin4.close();\n"),
        none);
    assertFalse(none.contains("return"), none);
  }

  @Test
  void oneBufferBesidePointersKeepsTheSingleBufferWrapper() throws Exception {
    // The pointer the old rules rejected now passes through, in the binding's own carrier, and
    // the wrapper is otherwise what a single buffer has always produced.
    var before = Generator.generate(Dsl.parse(source()), true);
    var after =
        Generator.generate(
            Dsl.parse(
                source()
                    .replace(
                        "param count type=u64\nend\nborrow increment",
                        "param count type=u64\n"
                            + "  param extra type=ptr<void>\n"
                            + "  param words type=ptr<usize>\n"
                            + "  param names type=ptr<ptr<char>>\n"
                            + "  param ticket type=ptr<Ticket>\n"
                            + "end\nborrow increment")),
            true);
    String java = after.get("NativeApiOwnership.java");
    String signature =
        "(NativeBuffer arg0, org.graalvm.nativeimage.c.type.VoidPointer arg2,"
            + " org.graalvm.nativeimage.c.type.WordPointer arg3,"
            + " org.graalvm.nativeimage.c.type.CCharPointerPointer arg4,"
            + " org.graalvm.word.PointerBase arg5) {\n";
    assertTrue(java.contains("    public static long checksum" + signature), java);
    assertTrue(
        java.contains("return NativeApi.checksum(pointer, length, arg2, arg3, arg4, arg5);"), java);
    assertEquals(
        before.get("NativeApiOwnership.java"),
        java.replace(signature, "(NativeBuffer arg0) {\n")
            .replace(
                "(byte[] arg0, org.graalvm.nativeimage.c.type.VoidPointer arg2,"
                    + " org.graalvm.nativeimage.c.type.WordPointer arg3,"
                    + " org.graalvm.nativeimage.c.type.CCharPointerPointer arg4,"
                    + " org.graalvm.word.PointerBase arg5) {\n",
                "(byte[] arg0) {\n")
            .replace("(pointer, length, arg2, arg3, arg4, arg5);", "(pointer, length);"));
  }

  @Test
  void rustWrappersAreUnsafeOnlyWhenTheyPassPointers() throws Exception {
    String rust = Generator.generate(Dsl.parse(TRANSFER), true).get("seam.rs");
    assertTrue(
        rust.contains(
            """
                /// # Safety
                /// Every pointer other than a borrowed buffer must satisfy the raw function's requirements.
                pub unsafe fn transfer(session: *mut super::Session, input: &[u8], output: &mut [u8], written: *mut u32, flags: u8) -> i32 {
                    unsafe { super::transfer(session, input.as_ptr(), input.len().try_into().expect("buffer length exceeds ABI range"), output.len().try_into().expect("buffer length exceeds ABI range"), output.as_mut_ptr(), written, flags) }
                }
            """),
        rust);
    // Scalars and buffers alone: the slices vouch for everything, so the wrapper stays safe.
    String scalars =
        Generator.generate(
                Dsl.parse(
                    TRANSFER
                        .replace(
                            "  param session type=ptr<Session> nullable=false ownership=borrowed"
                                + " access=read_write\n",
                            "")
                        .replace(
                            "  param written type=ptr<u32> nullable=true ownership=borrowed"
                                + " access=write\n",
                            "")),
                true)
            .get("seam.rs");
    assertTrue(
        scalars.contains(
            "    pub fn transfer(input: &[u8], output: &mut [u8], flags: u8) -> i32 {\n"
                + "        unsafe { super::transfer(input.as_ptr(), "),
        scalars);
    assertFalse(scalars.contains("# Safety"), scalars);
    // One buffer beside a pointer is unsafe for the same reason; without it nothing changes.
    String example = Generator.generate(Dsl.parse(source()), true).get("seam.rs");
    assertTrue(example.contains("    pub fn checksum(bytes: &[u8]) -> u64 {\n"), example);
    assertFalse(example.contains("pub unsafe fn checksum"), example);
    String pointer =
        Generator.generate(
                Dsl.parse(
                    source()
                        .replace(
                            "param count type=u64\nend\nborrow increment",
                            "param count type=u64\n  param seed type=ptr<u64> access=read\n"
                                + "end\nborrow increment")),
                true)
            .get("seam.rs");
    assertTrue(
        pointer.contains(
            "    pub unsafe fn checksum(bytes: &[u8], seed: *const u64) -> u64 {\n"
                + "        unsafe { super::checksum(bytes.as_ptr(), bytes.len().try_into()"
                + ".expect(\"buffer length exceeds ABI range\"), seed) }\n"),
        pointer);
    assertTrue(pointer.contains("    pub fn increment(bytes: &mut [u8]) {\n"), pointer);
  }

  @Test
  void writeOnlyBuffersAreWritableInEveryWrapper() throws Exception {
    String writeOnly = TRANSFER.replace("access=read_write nocapture", "access=write nocapture");
    assertNotEquals(TRANSFER, writeOnly);
    var expected = Generator.generate(Dsl.parse(TRANSFER), true);
    var actual = Generator.generate(Dsl.parse(writeOnly), true);
    // The declared access is part of the descriptor, and so of the fingerprint.
    assertTrue(actual.get("seam.json").contains("\"access\":\"WRITE\""));
    String was = expected.get("seam.abi").strip();
    String now = actual.get("seam.abi").strip();
    assertNotEquals(was, now);
    // Every wrapper treats the buffer exactly as it treats a read/write one: nothing relies on
    // the callee not reading it. Only the fingerprint constant differs.
    for (String file : List.of("ApiOwnership.java", "ApiFFM.java", "Api.java"))
      assertEquals(expected.get(file), actual.get(file).replace(now, was), file);
    String rust = actual.get("seam.rs");
    assertEquals(
        expected.get("seam.rs").substring(expected.get("seam.rs").indexOf("pub mod ownership")),
        rust.substring(rust.indexOf("pub mod ownership")));
    assertTrue(rust.contains("output: &mut [u8]"), rust);
    assertTrue(rust.contains("output.as_mut_ptr()"), rust);
    String java = actual.get("ApiOwnership.java");
    assertTrue(java.contains("arg4.acquire(true);"), java);
    assertTrue(java.contains("arg4.release(true);"), java);
    assertTrue(
        actual
            .get("ApiFFM.java")
            .contains(
                "if (arg4.isReadOnly()) throw new IllegalArgumentException(\"writable segment"
                    + " required\");"));
    var jniExpected = StaticJni.generate(Dsl.parse(TRANSFER));
    var jni = StaticJni.generate(Dsl.parse(writeOnly));
    for (String file : List.of("ApiJNI.java", "seam-jni.c", "seam-jni.h", "seam-jni.rs"))
      assertEquals(jniExpected.get(file), jni.get(file).replace(now, was), file);
    assertTrue(
        jni.get("ApiJNI.java")
            .contains(
                "if (arg4.isReadOnly()) throw new IllegalArgumentException(\"writable buffer"
                    + " required\");"));
    // Cells: a writable borrow, written back when it was copied.
    String module = " java_class=Api";
    var cellsExpected =
        Generator.generate(
            Dsl.parse(TRANSFER.replace(module, module + " truffle_cells=true")), true);
    var cells =
        Generator.generate(
            Dsl.parse(writeOnly.replace(module, module + " truffle_cells=true")), true);
    assertEquals(
        cellsExpected.get("ApiTruffle.java"),
        cells
            .get("ApiTruffle.java")
            .replace(cells.get("seam.abi").strip(), cellsExpected.get("seam.abi").strip()));
    assertTrue(
        cells
            .get("ApiTruffle.java")
            .contains("new BytesCoercion(\"transfer\", \"output\", false, true, true);"));
    // An approved writeonly fact is an optimizer contract of its own; the wrappers ignore it.
    var declared =
        Generator.generate(
            Dsl.parse(
                writeOnly.replace(
                    "access=write nocapture=true@explicit_contract",
                    "access=write nocapture=true@explicit_contract"
                        + " writeonly=true@explicit_contract")),
            true);
    assertEquals(
        java, declared.get("ApiOwnership.java"), "the writeonly fact does not change a wrapper");
    assertTrue(declared.get("seam.ll").contains("writeonly"), declared.get("seam.ll"));
    assertFalse(actual.get("seam.ll").contains("writeonly"), actual.get("seam.ll"));
    // One write-only buffer on its own, beside scalars: a safe Rust fn over a mutable slice.
    String single =
        Generator.generate(
                Dsl.parse(
                    source().replace("access=read_write nocapture", "access=write nocapture")),
                true)
            .get("seam.rs");
    assertTrue(single.contains("    pub fn increment(bytes: &mut [u8]) {\n"), single);
  }

  @Test
  void borrowDeclarationOrderDoesNotChangeTheDescriptor() {
    var declared = Dsl.parse(TRANSFER);
    var swapped =
        Dsl.parse(
            TRANSFER.replace(
                "borrow transfer output length=capacity lifetime=call\n"
                    + "borrow transfer input length=count lifetime=call\n",
                "borrow transfer input length=count lifetime=call\n"
                    + "borrow transfer output length=capacity lifetime=call\n"));
    assertEquals(declared.borrows(), swapped.borrows());
    assertEquals(SeamJson.fingerprint(declared), SeamJson.fingerprint(swapped));
    assertEquals(Generator.generate(declared, true), Generator.generate(swapped, true));
    assertTrue(
        SeamJson.canonical(declared)
            .contains(
                "\"borrows\": [{\"function\":\"transfer\",\"parameter\":\"input\","
                    + "\"length\":\"count\",\"lifetime\":\"call\"},"
                    + "{\"function\":\"transfer\",\"parameter\":\"output\","
                    + "\"length\":\"capacity\",\"lifetime\":\"call\"}]"),
        SeamJson.canonical(declared));
  }

  /** Creates that take pointers: a record and a C string, then a parent resource's handle. */
  static final String CREATES =
      "module creates target=aarch64-apple-darwin java_class=Api\n"
          + "opaque Session\n"
          + "opaque Stream\n"
          + "struct Limits size=16 align=8\n"
          + "  field max_bytes type=u64 offset=0\n"
          + "  field max_depth type=u64 offset=8\n"
          + "end\n"
          + "resource Session representation=native threading=shared create=sessionCreate"
          + " destroy=sessionDestroy\n"
          + "resource Stream representation=native threading=confined create=streamCreate"
          + " destroy=streamDestroy\n"
          + "import sessionCreate symbol=session_create return=ptr<Session> error=no_failure"
          + " nounwind=true@explicit_contract native_leaf=false@explicit_contract\n"
          + "  param limits type=ptr<Limits> nullable=false ownership=borrowed access=read\n"
          + "  param name type=ptr<char> nullable=false ownership=borrowed access=read\n"
          + "end\n"
          + "import sessionDestroy symbol=session_destroy return=void error=no_failure"
          + " nounwind=true@explicit_contract native_leaf=false@explicit_contract\n"
          + "  param session type=ptr<Session> nullable=false ownership=transferred_to_callee"
          + " access=read_write\n"
          + "end\n"
          + "import streamCreate symbol=stream_create return=ptr<Stream> error=no_failure"
          + " nounwind=true@explicit_contract native_leaf=false@explicit_contract\n"
          + "  param session type=ptr<Session> nullable=false ownership=borrowed"
          + " access=read_write\n"
          + "  param max_bytes type=u64\n"
          + "  param batch type=u16\n"
          + "end\n"
          + "import streamDestroy symbol=stream_destroy return=void error=no_failure"
          + " nounwind=true@explicit_contract native_leaf=false@explicit_contract\n"
          + "  param stream type=ptr<Stream> nullable=false ownership=transferred_to_callee"
          + " access=read_write\n"
          + "end\n";

  @Test
  void resourceCreatesPassPointerArgumentsThrough() throws Exception {
    var output = Generator.generate(Dsl.parse(CREATES), true);
    // Native Image: the raw binding's word carriers, by their qualified names.
    String java = output.get("ApiOwnership.java");
    assertTrue(
        java.contains(
            """
                    public static <T> T create(ResourceFactory<T> factory, org.graalvm.word.PointerBase arg0, org.graalvm.nativeimage.c.type.CCharPointer arg1) {
                        java.util.Objects.requireNonNull(factory);
                        long address = Api.sessionCreate(arg0, arg1).rawValue();
                        if (address == 0) return null;
                        return adoptUnsafe(address, factory);
                    }
            """),
        java);
    assertTrue(
        java.contains(
            """
                    public static <T> T create(ResourceFactory<T> factory, org.graalvm.word.PointerBase arg0, long arg1, short arg2) {
                        java.util.Objects.requireNonNull(factory);
                        long address = Api.streamCreate(arg0, arg1, arg2).rawValue();
            """),
        java);
    assertTrue(java.contains("factory.adopt(address, DESTRUCTOR, null)"), java);
    assertTrue(
        java.contains("factory.adopt(address, DESTRUCTOR, java.lang.Thread.currentThread())"),
        java);
    // FFM: pointers are the raw binding's segments.
    String ffm = output.get("ApiFFM.java");
    assertTrue(
        ffm.contains(
            "        public static <T> T create(LibraryBinding binding, ResourceFactory<T> factory,"
                + " MemorySegment arg0, MemorySegment arg1) throws Throwable {\n"
                + "            java.util.Objects.requireNonNull(factory);\n"
                + "            MemorySegment address = binding.sessionCreate(arg0, arg1);\n"),
        ffm);
    assertTrue(
        ffm.contains(
            "        public static <T> T create(LibraryBinding binding, ResourceFactory<T> factory,"
                + " MemorySegment arg0, long arg1, short arg2) throws Throwable {\n"
                + "            java.util.Objects.requireNonNull(factory);\n"
                + "            MemorySegment address = binding.streamCreate(arg0, arg1, arg2);\n"),
        ffm);
    // Rust: nothing vouches for a pointer argument or for how long the resource needs it.
    String rust = output.get("seam.rs");
    assertTrue(
        rust.contains(
            """
                    /// # Safety
                    /// Every pointer argument must satisfy the raw function's requirements, for as long
                    /// as the created resource depends on it.
                    pub unsafe fn create(limits: *const super::Limits, name: *const core::ffi::c_char) -> Option<Self> {
                        core::ptr::NonNull::new(unsafe { super::sessionCreate(limits, name) }).map(|pointer| Self { pointer, _thread: core::marker::PhantomData })
                    }
            """),
        rust);
    assertTrue(
        rust.contains(
            """
                    /// # Safety
                    /// Every pointer argument must satisfy the raw function's requirements, for as long
                    /// as the created resource depends on it.
                    pub unsafe fn create(session: *mut super::Session, max_bytes: u64, batch: u16) -> Option<Self> {
                        core::ptr::NonNull::new(unsafe { super::streamCreate(session, max_bytes, batch) }).map(|pointer| Self { pointer, _thread: core::marker::PhantomData })
                    }
            """),
        rust);
    // A create with scalar arguments only is the safe fn it has always been.
    String scalars =
        Generator.generate(
                Dsl.parse(
                    CREATES.replace(
                        "  param session type=ptr<Session> nullable=false ownership=borrowed"
                            + " access=read_write\n  param max_bytes",
                        "  param max_bytes")),
                true)
            .get("seam.rs");
    assertTrue(
        scalars.contains(
            "    impl Stream {\n"
                + "        pub fn create(max_bytes: u64, batch: u16) -> Option<Self> {\n"
                + "            core::ptr::NonNull::new(unsafe { super::streamCreate(max_bytes,"
                + " batch) })"),
        scalars);
    String example = Generator.generate(Dsl.parse(source()), true).get("seam.rs");
    assertTrue(example.contains("        pub fn create() -> Option<Self> {\n"), example);
    assertFalse(example.contains("pub unsafe fn create"), example);
    // Relaxed mode accepts the same shapes.
    assertEquals(output, Generator.generate(Dsl.parse(CREATES), false));
  }

  static Stream<Arguments> createRejections() {
    String limits =
        "  param limits type=ptr<Limits> nullable=false ownership=borrowed access=read\n";
    return Stream.of(
        // Arguments with no pass-through carrier in every backend.
        Arguments.of(
            limits,
            "  param limits type=Limits\n",
            "resource create takes only scalar and pointer arguments: sessionCreate limits"),
        Arguments.of(
            limits,
            "  param visit type=fn<Visit>\n",
            "resource create takes only scalar and pointer arguments: sessionCreate visit"),
        // The handle is the result: null means failure, and nothing else is a create.
        Arguments.of(
            "return=ptr<Session>", "return=i32", "resource create must return ptr<resource>"),
        Arguments.of(
            "return=ptr<Session>",
            "return=ptr<Stream>",
            "resource create must return ptr<resource>"),
        Arguments.of(
            "return=ptr<Session>",
            "return=ptr<ptr<Session>>",
            "resource create must return ptr<resource>"),
        // A create returns a pointer, so none of its byte pointers can be a call borrow.
        Arguments.of(
            "import sessionCreate",
            "borrow sessionCreate name length=limits lifetime=call\nimport sessionCreate",
            "call borrow cannot return a pointer"),
        // Out-parameter handles and in-place results are not a form of resource.
        Arguments.of(
            "create=sessionCreate destroy=sessionDestroy",
            "create=sessionCreate destroy=sessionDestroy out=handle",
            "unknown options: [out]"));
  }

  @ParameterizedTest
  @MethodSource("createRejections")
  void rejectsCreateShapesWithTheirReason(String from, String to, String message) {
    String valid =
        CREATES.replace("opaque Session\n", "opaque Session\ncallback Visit return=void\nend\n");
    assertDoesNotThrow(() -> Generator.generate(Dsl.parse(valid), true));
    assertTrue(valid.contains(from), "mutation must affect fixture");
    String changed = valid.replace(from, to);
    for (boolean strict : new boolean[] {false, true}) {
      var error =
          assertThrows(
              IllegalArgumentException.class,
              () -> Generator.generate(Dsl.parse(changed), strict),
              to);
      // The parser prefixes its own errors with the source line; validation errors have none.
      assertEquals(message, error.getMessage().replaceFirst("^line \\d+: ", ""));
    }
  }

  /** Creates that report a status and write their resource through a parameter. */
  static final String HANDLES =
      "module handles target=aarch64-apple-darwin java_class=Api truffle_cells=true\n"
          + "opaque Layout\n"
          + "struct LayoutResult size=24 align=8\n"
          + "  field count type=u64 offset=0\n"
          + "  field data type=ptr<Layout> offset=8\n"
          + "  field capacity type=u64 offset=16\n"
          + "end\n"
          + "resource Layout representation=native threading=confined create=layoutCompile"
          + " destroy=layoutRelease handle=result.data success=0,2\n"
          + "borrow layoutCompile schema length=length lifetime=call\n"
          + "import layoutCompile return=i32 error=no_failure nounwind=true@explicit_contract"
          + " native_leaf=false@explicit_contract\n"
          + "  param schema type=ptr<u8> nullable=false ownership=borrowed access=read"
          + " nocapture=true@explicit_contract readonly=true@explicit_contract\n"
          + "  param length type=u64\n"
          + "  param result type=ptr<LayoutResult> nullable=false ownership=borrowed access=write\n"
          + "end\n"
          + "import layoutRelease return=void error=no_failure nounwind=true@explicit_contract"
          + " native_leaf=false@explicit_contract\n"
          + "  param layout type=ptr<Layout> nullable=false ownership=transferred_to_callee"
          + " access=read_write\n"
          + "end\n"
          + "struct View size=16 align=8\n"
          + "  field sum type=i64 offset=0\n"
          + "  field scratch type=ptr<u8> offset=8\n"
          + "end\n"
          + "resource View representation=native threading=shared create=viewInit"
          + " destroy=viewRelease handle=view\n"
          + "import viewInit return=void error=no_failure nounwind=true@explicit_contract"
          + " native_leaf=false@explicit_contract\n"
          + "  param seed type=i64\n"
          + "  param view type=ptr<View> nullable=false ownership=borrowed access=read_write\n"
          + "end\n"
          + "import viewRelease return=void error=no_failure nounwind=true@explicit_contract"
          + " native_leaf=false@explicit_contract\n"
          + "  param view type=ptr<View> nullable=false ownership=transferred_to_callee"
          + " access=read_write\n"
          + "end\n";

  /**
   * The handle forms are a contract only Truffle cells project: the other ownership APIs leave
   * those creates to their raw bindings, and the descriptor records the form.
   */
  @Test
  void handleResourcesAreProjectedByCellsOnly() {
    var output = Generator.generate(Dsl.parse(HANDLES), true);
    assertTrue(output.get("ApiTruffle.java").contains("class layoutCompile_Cell"));
    // The borrow on the create still gets its wrappers; the resource gets no owner type.
    assertTrue(output.get("ApiOwnership.java").contains("layoutCompile(NativeBuffer arg0"));
    for (String file : List.of("ApiOwnership.java", "ApiFFM.java", "seam.rs")) {
      assertFalse(output.get(file).contains("class Layout "), file);
      assertFalse(output.get(file).contains("ResourceFactory"), file);
      assertFalse(output.get(file).contains("impl core::ops::Drop for"), file);
    }
    String json = output.get("seam.json");
    assertTrue(json.contains("\"handle\":\"result.data\",\"success\":[0,2]"), json);
    assertTrue(json.contains("\"handle\":\"view\",\"success\":[]"), json);
    // The form is part of the contract, so it is part of the fingerprint.
    assertNotEquals(
        output.get("seam.abi"),
        Generator.generate(Dsl.parse(HANDLES.replace("success=0,2", "success=0")), true)
            .get("seam.abi"));
    // A module with only handle resources has nothing for the ownership class to hold.
    String bare = HANDLES.replace("borrow layoutCompile schema length=length lifetime=call\n", "");
    assertFalse(Generator.generate(Dsl.parse(bare), true).containsKey("ApiOwnership.java"));
  }

  static Stream<Arguments> handleRejections() {
    String cellsOnly =
        "a resource handle is generated for Truffle cells only; layoutCompile needs one: Layout"
            + " handle=result.data";
    String pointer = "resource handle must name a non-null pointer parameter with write access: ";
    String result =
        "  param result type=ptr<LayoutResult> nullable=false ownership=borrowed access=write\n";
    return Stream.of(
        Arguments.of(" truffle_cells=true", "", cellsOnly),
        Arguments.of(
            "import layoutCompile return=i32 error=no_failure",
            "import layoutCompile return=i32 error=no_failure truffle_cell=false@explicit_contract",
            cellsOnly),
        Arguments.of(
            "handle=result.data",
            "handle=result.data.more",
            "resource handle is PARAMETER or PARAMETER.FIELD: Layout handle=result.data.more"),
        Arguments.of(
            "handle=result.data",
            "handle=.data",
            "resource handle is PARAMETER or PARAMETER.FIELD: Layout handle=.data"),
        Arguments.of(
            "handle=result.data",
            "handle=result.",
            "resource handle is PARAMETER or PARAMETER.FIELD: Layout handle=result."),
        Arguments.of(
            "handle=result.data", "handle=absent.data", pointer + "Layout handle=absent.data"),
        Arguments.of(
            "handle=result.data", "handle=length.data", pointer + "Layout handle=length.data"),
        Arguments.of(
            result,
            result.replace("access=write", "access=read"),
            pointer + "Layout handle=result.data"),
        Arguments.of(
            result,
            result.replace("nullable=false", "nullable=true"),
            pointer + "Layout handle=result.data"),
        Arguments.of(
            result,
            result.replace("ptr<LayoutResult>", "ptr<u64>"),
            "resource handle field requires a pointer to a struct: Layout handle=result.data"),
        Arguments.of(
            "handle=result.data",
            "handle=result.count",
            "resource handle field must be a ptr<resource> field of LayoutResult: Layout"
                + " handle=result.count"),
        Arguments.of(
            "handle=result.data",
            "handle=result.absent",
            "resource handle field must be a ptr<resource> field of LayoutResult: Layout"
                + " handle=result.absent"),
        Arguments.of(
            " success=0,2",
            "",
            "a resource create with a handle must list the results that mean success: Layout"
                + " handle=result.data"),
        Arguments.of(
            "import layoutCompile return=i32",
            "import layoutCompile return=f64",
            "a resource create with a handle returns an integer status or void: Layout"
                + " handle=result.data"),
        Arguments.of(
            "import layoutCompile return=i32",
            "import layoutCompile return=bool",
            "a resource create with a handle returns an integer status or void: Layout"
                + " handle=result.data"),
        Arguments.of(
            "destroy=viewRelease handle=view",
            "destroy=viewRelease handle=view success=0",
            "a create that returns void has no result to compare with success: View handle=view"),
        // In place, the resource is the struct the parameter points to.
        Arguments.of(
            "struct View size=16 align=8\n  field sum type=i64 offset=0\n"
                + "  field scratch type=ptr<u8> offset=8\nend\n",
            "opaque View\n",
            "a resource initialized in place must name a struct: View"),
        Arguments.of(
            "  param view type=ptr<View> nullable=false ownership=borrowed access=read_write\n",
            "  param view type=ptr<LayoutResult> nullable=false ownership=borrowed"
                + " access=read_write\n",
            "a resource initialized in place is what its handle parameter points to: View"
                + " handle=view"),
        // Through a field, the resource is still an opaque allocation.
        Arguments.of(
            "resource Layout representation",
            "resource LayoutResult representation",
            "resource must name an opaque type: LayoutResult"),
        Arguments.of("success=0,2", "success=0,two", "success lists integer results: 0,two"));
  }

  @ParameterizedTest
  @MethodSource("handleRejections")
  void rejectsHandleShapesWithTheirReason(String from, String to, String message) {
    assertDoesNotThrow(() -> Generator.generate(Dsl.parse(HANDLES), true));
    assertTrue(HANDLES.contains(from), "mutation must affect fixture");
    String changed = HANDLES.replace(from, to);
    var error =
        assertThrows(
            IllegalArgumentException.class, () -> Generator.generate(Dsl.parse(changed), true), to);
    assertEquals(message, error.getMessage().replaceFirst("^line \\d+: ", ""));
  }

  /** A returned-pointer resource has no result to list as success. */
  @Test
  void successBelongsToHandleCreates() {
    String source =
        CREATES.replace(
            "create=sessionCreate destroy=sessionDestroy",
            "create=sessionCreate destroy=sessionDestroy success=0");
    assertEquals(
        "success applies to a resource create with a handle",
        assertThrows(
                IllegalArgumentException.class, () -> Generator.generate(Dsl.parse(source), true))
            .getMessage());
  }

  static Stream<Arguments> borrowRejections() {
    String output =
        "  param output type=ptr<u8> nullable=false ownership=borrowed access=read_write"
            + " nocapture=true@explicit_contract\n";
    String outputBorrow = "borrow transfer output length=capacity lifetime=call\n";
    return Stream.of(
        // What a function's borrows may not share.
        Arguments.of(
            outputBorrow, outputBorrow + outputBorrow, "duplicate borrow: transfer output"),
        Arguments.of(
            "length=capacity",
            "length=count",
            "borrows cannot share a length parameter: transfer count"),
        Arguments.of(
            "length=capacity",
            "length=input",
            "call buffer cannot be another borrow's length: transfer input"),
        Arguments.of(
            "length=capacity",
            "length=output",
            "borrow must identify distinct buffer and length parameters"),
        Arguments.of(
            "length=capacity",
            "length=absent",
            "borrow must identify distinct buffer and length parameters"),
        // Out of scope: a result that could point into a borrowed buffer.
        Arguments.of("return=i32", "return=ptr<Session>", "call borrow cannot return a pointer"),
        Arguments.of("return=i32", "return=ptr<u8>", "call borrow cannot return a pointer"),
        // Parameters with no pass-through carrier in every backend.
        Arguments.of(
            "  param flags type=u8\n",
            "  param visit type=fn<Visit>\n",
            "call buffer wrappers pass only scalars and pointers beside their buffers:"
                + " transfer visit"),
        Arguments.of(
            "  param flags type=u8\n",
            "  param pair type=Pair\n",
            "call buffer wrappers pass only scalars and pointers beside their buffers:"
                + " transfer pair"),
        // Every per-buffer rule applies to each buffer.
        Arguments.of(
            output,
            output.replace("nullable=false", "nullable=true"),
            "call buffer requires non-null ptr<u8>"),
        Arguments.of(
            output, output.replace("ptr<u8>", "ptr<i8>"), "call buffer requires borrowed ptr<u8>"),
        Arguments.of(
            output,
            output.replace("ownership=borrowed", "ownership=owned_by_caller"),
            "call buffer requires borrowed ptr<u8>"),
        Arguments.of(
            output,
            output.replace("access=read_write", "access=none"),
            "call buffer requires read, write or read_write access"),
        Arguments.of(
            output,
            output.replace(" access=read_write", ""),
            "call buffer requires read, write or read_write access"),
        Arguments.of(
            output,
            output.replace("access=read_write", "access=write readonly=true@explicit_contract"),
            "readonly contradicts access"),
        Arguments.of(
            output,
            output.replace(" nocapture=true@explicit_contract", ""),
            "call buffer requires approved nocapture=true"),
        Arguments.of(
            output,
            output.replace("access=read_write", "access=read"),
            "shared call buffer requires approved readonly=true"),
        Arguments.of(
            output,
            output.replace("\n", " noalias=true@explicit_contract\n"),
            "call buffer cannot impose alignment, extent, or noalias preconditions"),
        Arguments.of(
            "param capacity type=u32",
            "param capacity type=u16",
            "buffer length must be a 32/64-bit integer (bytes)"),
        Arguments.of(
            " nounwind=true@explicit_contract\n",
            "\n",
            "ownership wrappers require approved nounwind=true"),
        Arguments.of(
            "return=i32 error=no_failure nounwind=true@explicit_contract\n",
            "return=void error=no_failure nounwind=true@explicit_contract"
                + " noreturn=true@explicit_contract native_leaf=false@explicit_contract\n",
            "ownership wrappers require returning no_failure native imports"));
  }

  @ParameterizedTest
  @MethodSource("borrowRejections")
  void rejectsBorrowShapesWithTheirReason(String from, String to, String message) {
    String declarations =
        "callback Visit return=void\nend\n"
            + "struct Pair size=16 align=8\n"
            + "  field a type=i64 offset=0\n"
            + "  field b type=i64 offset=8\n"
            + "end\n";
    String valid = TRANSFER.replace("opaque Session\n", "opaque Session\n" + declarations);
    assertDoesNotThrow(() -> Generator.generate(Dsl.parse(valid), true));
    assertTrue(valid.contains(from), "mutation must affect fixture");
    String changed = valid.replace(from, to);
    for (boolean strict : new boolean[] {false, true}) {
      var error =
          assertThrows(
              IllegalArgumentException.class,
              () -> Generator.generate(Dsl.parse(changed), strict),
              to);
      assertEquals(message, error.getMessage());
    }
  }

  @Test
  void rejectsNullableBorrowBufferInBothModes() throws Exception {
    assertTrue(
        source().contains("nullable=false ownership=borrowed"),
        "fixture must hold nullable=false on a borrow buffer");
    String message = "call buffer requires non-null ptr<u8>";

    // DSL default is nullable=true: omitting nullable=false silently yields nullable=true.
    String defaulted = source().replace("nullable=false ownership=borrowed", "ownership=borrowed");
    var relaxedDefault =
        assertThrows(
            IllegalArgumentException.class,
            () -> Generator.generate(Dsl.parse(defaulted), false),
            "relaxed mode must reject defaulted-nullable borrow buffer");
    assertEquals(message, relaxedDefault.getMessage());
    var strictDefault =
        assertThrows(
            IllegalArgumentException.class,
            () -> Generator.generate(Dsl.parse(defaulted), true),
            "strict mode must reject defaulted-nullable borrow buffer");
    assertEquals(message, strictDefault.getMessage());

    // Explicit nullable=true must also be rejected.
    String explicit =
        source().replace("nullable=false ownership=borrowed", "nullable=true ownership=borrowed");
    var relaxedExplicit =
        assertThrows(
            IllegalArgumentException.class,
            () -> Generator.generate(Dsl.parse(explicit), false),
            "relaxed mode must reject explicit nullable=true borrow buffer");
    assertEquals(message, relaxedExplicit.getMessage());
    var strictExplicit =
        assertThrows(
            IllegalArgumentException.class,
            () -> Generator.generate(Dsl.parse(explicit), true),
            "strict mode must reject explicit nullable=true borrow buffer");
    assertEquals(message, strictExplicit.getMessage());

    // Control: the original non-nullable fixture still generates in both modes.
    assertDoesNotThrow(() -> Generator.generate(Dsl.parse(source()), false));
    assertDoesNotThrow(() -> Generator.generate(Dsl.parse(source()), true));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "NativeBuffer",
        "NativeDestructor",
        "ResourceFactory",
        "java",
        "org",
        "core",
        "Option",
        "Drop",
        "Send",
        "Sync"
      })
  void rejectsReservedNativeClassNamesInOwnershipScope(String reserved) throws Exception {
    assertTrue(source().contains("java_class=NativeApi"), "fixture must name a java_class");
    String changed = source().replace("java_class=NativeApi", "java_class=" + reserved);
    var relaxed =
        assertThrows(
            IllegalArgumentException.class,
            () -> Generator.generate(Dsl.parse(changed), false),
            "relaxed mode must reject reserved java_class=" + reserved);
    assertEquals("reserved native/ownership class name", relaxed.getMessage());
    var strict =
        assertThrows(
            IllegalArgumentException.class,
            () -> Generator.generate(Dsl.parse(changed), true),
            "strict mode must reject reserved java_class=" + reserved);
    assertEquals("reserved native/ownership class name", strict.getMessage());
  }

  @ParameterizedTest
  @CsvSource({
    "Drop, reserved resource name",
    "Send, reserved resource name",
    "Sync, reserved resource name",
    "Option, type shadows generated name: Option",
    "core, type shadows generated name: core"
  })
  void rejectsReservedResourceNamesInOwnershipScope(String reserved, String message)
      throws Exception {
    assertTrue(source().contains("opaque Ticket"), "fixture must name a resource Ticket");
    String changed = source().replace("Ticket", reserved);
    // Globally reserved type names are rejected before ownership-specific validation.
    var relaxed =
        assertThrows(
            IllegalArgumentException.class,
            () -> Generator.generate(Dsl.parse(changed), false),
            "relaxed mode must reject reserved resource name=" + reserved);
    assertEquals(message, relaxed.getMessage());
    var strict =
        assertThrows(
            IllegalArgumentException.class,
            () -> Generator.generate(Dsl.parse(changed), true),
            "strict mode must reject reserved resource name=" + reserved);
    assertEquals(message, strict.getMessage());
  }

  @Test
  void ownershipTraitsAreEmittedFullyQualified() throws Exception {
    String rust = Generator.generate(Dsl.parse(source()), true).get("seam.rs");
    assertTrue(rust.contains("impl core::ops::Drop for Ticket"));
    assertTrue(rust.contains("unsafe impl core::marker::Send for Ticket"));
    assertTrue(rust.contains("unsafe impl core::marker::Sync for Ticket"));
    assertFalse(rust.contains("impl Drop for Ticket"));
    assertFalse(rust.contains("unsafe impl Send for Ticket"));
    assertFalse(rust.contains("unsafe impl Sync for Ticket"));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "opaque ownership",
        "struct ownership size=8 align=8\nfield a type=i64 offset=0\nend",
        "callback ownership return=void\nend"
      })
  void rejectsOwnershipNameReservedForWrappers(String declaration) throws Exception {
    String source = source() + "\n" + declaration + "\n";
    var relaxed =
        assertThrows(
            IllegalArgumentException.class,
            () -> Generator.generate(Dsl.parse(source), false),
            "relaxed mode must reject reserved name for: " + declaration);
    assertEquals("ownership is reserved when wrappers are generated", relaxed.getMessage());
    var strict =
        assertThrows(
            IllegalArgumentException.class,
            () -> Generator.generate(Dsl.parse(source), true),
            "strict mode must reject reserved name for: " + declaration);
    assertEquals("ownership is reserved when wrappers are generated", strict.getMessage());
  }

  @Test
  void reservedNameGuardSkipsBareModulesWithoutOwnership() {
    String bare =
        "module demo target=aarch64-apple-darwin java_class=java\n"
            + "import inspect return=i64 error=no_failure\n"
            + "  param value type=ptr<i64> nullable=false ownership=borrowed access=read\n"
            + "end\n";
    assertDoesNotThrow(() -> Generator.generate(Dsl.parse(bare), false));
    assertDoesNotThrow(() -> Generator.generate(Dsl.parse(bare), true));
    assertTrue(Generator.generate(Dsl.parse(bare), true).containsKey("java.java"));
  }

  @Test
  void reservedNameGuardAllowsCallbackOwnershipInBareModules() {
    String bare =
        "module demo target=aarch64-apple-darwin\n"
            + "callback ownership return=void\nend\n"
            + "import inspect return=i64 error=no_failure\n"
            + "  param value type=ptr<i64> nullable=false ownership=borrowed access=read\n"
            + "end\n";
    var relaxed = assertDoesNotThrow(() -> Generator.generate(Dsl.parse(bare), false));
    assertDoesNotThrow(() -> Generator.generate(Dsl.parse(bare), true));
    assertTrue(relaxed.get("seam.rs").contains("pub type ownership = "));
    assertFalse(relaxed.get("seam.rs").contains("pub mod ownership"));
  }

  static Stream<Arguments> coreAndOptionAcrossDeclaredKinds() {
    return Stream.of(
        Arguments.of("opaque", "core"),
        Arguments.of("opaque", "Option"),
        Arguments.of("struct", "core"),
        Arguments.of("struct", "Option"),
        Arguments.of("callback", "core"),
        Arguments.of("callback", "Option"));
  }

  static String reservedDeclaration(String kind, String name) {
    return switch (kind) {
      case "opaque" -> "opaque " + name;
      case "struct" -> "struct " + name + " size=8 align=8\nfield a type=i64 offset=0\nend";
      case "callback" -> "callback " + name + " return=void\nend";
      default -> throw new IllegalArgumentException(kind);
    };
  }

  @ParameterizedTest
  @MethodSource("coreAndOptionAcrossDeclaredKinds")
  void rejectsCoreOrOptionDeclaredTypesInBareModules(String kind, String reserved) {
    String source =
        "module demo target=aarch64-apple-darwin\n"
            + reservedDeclaration(kind, reserved)
            + "\nimport inspect return=i64 error=no_failure\n"
            + "  param value type=ptr<i64> nullable=false ownership=borrowed access=read\n"
            + "end\n";
    String message = "type shadows generated name: " + reserved;
    var relaxed =
        assertThrows(
            IllegalArgumentException.class,
            () -> Generator.generate(Dsl.parse(source), false),
            "relaxed mode must reject " + kind + " '" + reserved + "'");
    assertEquals(message, relaxed.getMessage());
    var strict =
        assertThrows(
            IllegalArgumentException.class,
            () -> Generator.generate(Dsl.parse(source), true),
            "strict mode must reject " + kind + " '" + reserved + "'");
    assertEquals(message, strict.getMessage());
  }

  @ParameterizedTest
  @ValueSource(strings = {"NativeBuffer", "NativeDestructor", "ResourceFactory", "java", "org"})
  void keepsOwnershipReservedNamesAllowedForDeclaredTypesInBareModules(String name) {
    String source =
        "module demo target=aarch64-apple-darwin\n"
            + "opaque "
            + name
            + "\n"
            + "import inspect return=i64 error=no_failure\n"
            + "  param value type=ptr<i64> nullable=false ownership=borrowed access=read\n"
            + "end\n";
    assertDoesNotThrow(() -> Generator.generate(Dsl.parse(source), false), "relaxed: " + name);
    assertDoesNotThrow(() -> Generator.generate(Dsl.parse(source), true), "strict: " + name);
  }
}
