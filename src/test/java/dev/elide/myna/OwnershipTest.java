// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package dev.elide.myna;

import static org.junit.jupiter.api.Assertions.*;

import dev.elide.myna.frontend.Dsl;
import dev.elide.myna.serialize.SeamJson;
import java.nio.file.*;
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
            output.replace("access=read_write", "access=write"),
            "call buffer requires read or read_write access"),
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
