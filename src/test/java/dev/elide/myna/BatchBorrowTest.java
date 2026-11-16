// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package dev.elide.myna;

import static org.junit.jupiter.api.Assertions.*;

import dev.elide.myna.backend.StaticJni;
import dev.elide.myna.frontend.Dsl;
import dev.elide.myna.model.Seam.BufferBorrow;
import dev.elide.myna.serialize.SeamJson;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Batch borrows: an array of buffers with a parallel array of lengths and a count (#132). */
class BatchBorrowTest {
  private static final String ARRAY =
      " nullable=true ownership=borrowed access=read nocapture=true@explicit_contract"
          + " readonly=true@explicit_contract\n";

  static final String BATCH =
      "module api target=aarch64-apple-darwin java_class=Api\n"
          + "borrow digest sources lengths=lengths count=count lifetime=call\n"
          + "import digest symbol=api_digest return=u64 error=no_failure"
          + " nounwind=true@explicit_contract native_leaf=false@explicit_contract\n"
          + "  param sources type=ptr<ptr<u8>>"
          + ARRAY
          + "  param lengths type=ptr<u64>"
          + ARRAY
          + "  param count type=u64\n"
          + "end\n"
          + "borrow merge key length=keyLength lifetime=call\n"
          + "borrow merge inputs lengths=inputLengths count=inputCount lifetime=call\n"
          + "borrow merge deps lengths=depLengths count=depCount lifetime=call"
          + " elements_nullable=true\n"
          + "import merge symbol=api_merge return=u64 error=no_failure"
          + " nounwind=true@explicit_contract native_leaf=false@explicit_contract\n"
          + "  param key type=ptr<u8> nullable=false ownership=borrowed access=read"
          + " nocapture=true@explicit_contract readonly=true@explicit_contract\n"
          + "  param keyLength type=u32\n"
          + "  param inputs type=ptr<ptr<u8>>"
          + ARRAY
          + "  param inputLengths type=ptr<u64>"
          + ARRAY
          + "  param inputCount type=u64\n"
          + "  param deps type=ptr<ptr<u8>>"
          + ARRAY
          + "  param depLengths type=ptr<u32>"
          + ARRAY
          + "  param depCount type=i32\n"
          + "end\n";

  @Test
  void batchesAreSerializedParsedAndFingerprinted() {
    var module = Dsl.parse(BATCH);
    assertEquals(
        BufferBorrow.batch("digest", "sources", "lengths", "count", false),
        module.borrows().getFirst());
    assertTrue(
        module
            .borrows()
            .contains(BufferBorrow.batch("merge", "deps", "depLengths", "depCount", true)));
    String json = SeamJson.canonical(module);
    assertTrue(
        json.contains(
            "{\"function\":\"digest\",\"parameter\":\"sources\",\"lengths\":\"lengths\","
                + "\"count\":\"count\",\"elementsNullable\":false,\"lifetime\":\"call\"}"),
        json);
    assertTrue(json.contains("\"elementsNullable\":true"), json);
    // Single borrows keep their form.
    assertTrue(
        json.contains(
            "{\"function\":\"merge\",\"parameter\":\"key\",\"length\":\"keyLength\","
                + "\"lifetime\":\"call\"}"),
        json);
    var nullable = Dsl.parse(BATCH.replace(" elements_nullable=true", ""));
    assertNotEquals(SeamJson.fingerprint(module), SeamJson.fingerprint(nullable));
    // The raw declarations do not change: a batch adds no C or LLVM facts of its own.
    var raw =
        Dsl.parse(
            BATCH
                .replaceAll("borrow [a-z]+ [a-zA-Z]+ lengths=[^\n]*\n", "")
                .replace("borrow merge key length=keyLength lifetime=call\n", ""));
    var output = Generator.generate(module, true);
    var rawOutput = Generator.generate(raw, true);
    // Only the fingerprint, which covers the borrows, differs.
    assertEquals(
        rawOutput.get("seam.h").replace(SeamJson.fingerprint(raw), "FP"),
        output.get("seam.h").replace(SeamJson.fingerprint(module), "FP"));
    String ll = output.get("seam.ll");
    assertTrue(
        ll.contains(
            "declare i64 @api_digest(ptr captures(none) readonly %sources, ptr captures(none)"
                + " readonly %lengths, i64 %count) nounwind"),
        ll);
    assertFalse(ll.contains("dereferenceable"), ll);
  }

  @Test
  void wrappersTakeArraysOfBuffers() {
    var output = Generator.generate(Dsl.parse(BATCH), true);
    // Rust: slices of slices, lent through lend_batch, and the implementation side's views.
    String rust = output.get("seam.rs");
    assertTrue(
        rust.contains(
            """
                pub fn digest(sources: &[&[u8]]) -> u64 {
                    let count: u64 = sources.len().try_into().expect("batch count exceeds ABI range");
                    lend_batch::<_, u64, _>(sources, present, |sources, lengths| unsafe { super::digest(sources, lengths, count) })
                }
            """),
        rust);
    assertTrue(
        rust.contains(
            "    pub fn merge(key: &[u8], inputs: &[&[u8]], deps: &[Option<&[u8]>]) -> u64 {\n"),
        rust);
    assertTrue(rust.contains("lend_batch::<_, u32, _>(deps, optional, |deps, depLengths|"), rust);
    assertTrue(rust.contains("let depCount: i32 = deps.len().try_into()"), rust);
    assertTrue(rust.contains("pub struct BufferBatch<'a, L: BatchLength = u64> {"), rust);
    assertTrue(rust.contains("pub struct OptionalBufferBatch<'a, L: BatchLength = u64> {"), rust);
    assertTrue(rust.contains("extern crate alloc;"), rust);
    // Native Image: NativeBuffer[] and byte[][] overloads over the shared lending helpers.
    String java = output.get("ApiOwnership.java");
    assertTrue(java.contains("public static long digest(NativeBuffer[] arg0) {"), java);
    assertTrue(java.contains("public static long digest(byte[][] arg0) {"), java);
    assertTrue(
        java.contains(
            "public static long merge(NativeBuffer arg0, NativeBuffer[] arg2, NativeBuffer[]"
                + " arg5)"),
        java);
    assertTrue(java.contains("lendBatch(arg5, arrays5, count5, false, 4294967295L, true);"), java);
    assertTrue(java.contains("pinBatch(arg2, pins2, arrays2, count2, true, false);"), java);
    assertTrue(java.contains("private static void releaseBatch("), java);
    assertTrue(java.contains(": org.graalvm.nativeimage.StackValue.get(128);"), java);
    // FFM: an array of native segments, lent through the arena.
    String ffm = output.get("ApiFFM.java");
    assertTrue(ffm.contains("public long digest(MemorySegment[] arg0) throws Throwable {"), ffm);
    assertTrue(
        ffm.contains(
            "MemorySegment arrays5 = count5 == 0 ? MemorySegment.NULL : lendBatch(arg5, empty,"
                + " false, 4294967295L, true);"),
        ffm);
    assertTrue(ffm.contains("java.lang.ref.Reference.reachabilityFence(arg2);"), ffm);
    assertTrue(ffm.contains("private static MemorySegment lendBatch("), ffm);
  }

  @Test
  void staticJniKeepsTheRawBindingOfABatch() {
    var jni = StaticJni.generate(Dsl.parse(BATCH));
    assertFalse(jni.toString().contains("digestBuffer"), jni.toString());
    assertFalse(jni.toString().contains("mergeBuffer"), jni.toString());
  }

  @Test
  void singleBorrowOutputIsUnchanged() {
    // A descriptor without batches generates no batch helpers.
    String single = BATCH.substring(0, BATCH.indexOf("borrow merge"));
    var noBatch =
        single
            .replace("borrow digest sources lengths=lengths count=count lifetime=call\n", "")
            .replace("ptr<ptr<u8>>", "ptr<u8>");
    var output = Generator.generate(Dsl.parse(noBatch), true);
    output.values().forEach(text -> assertFalse(text.contains("BufferBatch"), text));
    output.values().forEach(text -> assertFalse(text.contains("lendBatch"), text));
  }

  static Stream<Arguments> batchRejections() {
    return Stream.of(
        Arguments.of(
            "param sources type=ptr<ptr<u8>>",
            "param sources type=ptr<u8>",
            "batch sources must be ptr<ptr<u8>>: digest sources"),
        Arguments.of(
            "param lengths type=ptr<u64>",
            "param lengths type=ptr<u16>",
            "batch lengths must point to u32, u64, i32 or i64: digest lengths"),
        Arguments.of(
            "param count type=u64",
            "param count type=u8",
            "batch count must be a 32/64-bit integer: digest count"),
        Arguments.of(
            "count=count lifetime=call",
            "count=absent lifetime=call",
            "a batch borrow names three distinct parameters of digest: digest sources"
                + " lengths=lengths count=absent"),
        Arguments.of(
            "count=count lifetime=call",
            "count=lengths lifetime=call",
            "a batch borrow's sources, lengths and count belong to it alone: digest lengths"),
        Arguments.of(
            "borrow merge key length=keyLength",
            "borrow merge key length=inputCount",
            "a batch borrow's sources, lengths and count belong to it alone: merge inputCount is"
                + " also borrowed by merge key"),
        Arguments.of(
            "borrow merge deps lengths=depLengths count=depCount",
            "borrow merge deps lengths=inputLengths count=depCount",
            "a batch borrow's sources, lengths and count belong to it alone: merge inputLengths"),
        Arguments.of(
            "param sources type=ptr<ptr<u8>> nullable=true",
            "param sources type=ptr<ptr<u8>> nullable=false",
            "a batch array is nullable=true, null for the empty batch: digest sources"),
        Arguments.of(
            "param lengths type=ptr<u64> nullable=true ownership=borrowed access=read"
                + " nocapture=true@explicit_contract readonly=true@explicit_contract",
            "param lengths type=ptr<u64> nullable=true ownership=borrowed access=read_write"
                + " nocapture=true@explicit_contract",
            "a batch array is a borrowed read-only pointer (access=read): digest lengths"),
        Arguments.of(
            "param lengths type=ptr<u64> nullable=true ownership=borrowed access=read"
                + " nocapture=true@explicit_contract",
            "param lengths type=ptr<u64> nullable=true ownership=borrowed access=read",
            "a batch array requires approved nocapture=true and readonly=true: digest lengths"),
        Arguments.of(
            "param sources type=ptr<ptr<u8>> nullable=true ownership=borrowed access=read",
            "param sources type=ptr<ptr<u8>> nullable=true ownership=borrowed access=read"
                + " align=8@explicit_contract",
            "a batch array cannot impose extent, alignment, noalias or nonnull preconditions:"
                + " digest sources"),
        Arguments.of(
            "borrow digest sources",
            "opaque BufferBatch\nborrow digest sources",
            "BufferBatch is reserved for the views of a batch borrow: digest sources"
                + " lengths=lengths count=count"),
        Arguments.of(
            "import digest symbol=api_digest return=u64",
            "import digest symbol=api_digest return=ptr<u8>",
            "call borrow cannot return a pointer: digest sources"));
  }

  @ParameterizedTest
  @MethodSource("batchRejections")
  void rejectsBatchShapesWithTheirReason(String from, String to, String message) {
    assertTrue(BATCH.contains(from), "mutation must affect fixture: " + from);
    var module = Dsl.parse(BATCH.replaceFirst(java.util.regex.Pattern.quote(from), to));
    assertEquals(
        message,
        assertThrows(IllegalArgumentException.class, () -> Generator.generate(module, true))
            .getMessage());
  }

  @Test
  void parserRejectsHeldAndMalformedBatches() {
    assertEquals(
        "line 2: a batch borrow has lifetime=call: digest sources",
        assertThrows(
                IllegalArgumentException.class,
                () ->
                    Dsl.parse(
                        BATCH.replace(
                            "count=count lifetime=call", "count=count lifetime=resource:Doc")))
            .getMessage());
    assertThrows(
        IllegalArgumentException.class,
        () -> Dsl.parse(BATCH.replace("elements_nullable=true", "elements_nullable=yes")));
    assertThrows(
        IllegalArgumentException.class,
        () -> Dsl.parse(BATCH.replace("lengths=lengths count=count", "lengths=lengths")));
  }

  @Test
  void cellsAreRejectedForBatchesUntilSupported() {
    String cells = BATCH.replace("java_class=Api", "java_class=Api truffle_cells=true");
    String message =
        assertThrows(
                IllegalArgumentException.class, () -> Generator.generate(Dsl.parse(cells), true))
            .getMessage();
    assertTrue(message.contains("truffle_cell=false@explicit_contract"), message);
  }
}
