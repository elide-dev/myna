// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package dev.elide.myna;

import static org.junit.jupiter.api.Assertions.*;

import dev.elide.myna.frontend.Dsl;
import dev.elide.myna.model.Seam;
import dev.elide.myna.model.Seam.*;
import dev.elide.myna.serialize.SeamJson;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;

/** The generated Truffle cells: a projection of the same seam the other backends emit. */
class TruffleCellsTest {
  static final String MODULE = "module cells target=aarch64-apple-darwin truffle_cells=true\n";
  static final String ADD =
      "import add return=i32 error=no_failure\n"
          + "  param left type=i32\n"
          + "  param right type=i32\n"
          + "end\n";

  static SortedMap<String, String> generate(String source) {
    return Generator.generate(Dsl.parse(source), true);
  }

  static String example() throws Exception {
    return Files.readString(Path.of("examples/truffle.seam"));
  }

  @Test
  void cellsAreOptInAndFingerprinted() throws Exception {
    var plain = generate(MODULE.replace(" truffle_cells=true", "") + ADD);
    assertFalse(plain.containsKey("MynaTruffle.java"));
    assertFalse(plain.get("seam.json").contains("\"truffle\""));
    var cells = generate(MODULE + ADD);
    assertTrue(cells.containsKey("MynaTruffle.java"));
    assertTrue(
        cells
            .get("seam.json")
            .contains(
                "\"truffle\": {\"cells\":true,\"backend\":\"NATIVE\",\"coercion\":\"STRICT\","
                    + "\"transitions\":\"DIRECT\"}"));
    assertNotEquals(plain.get("seam.abi"), cells.get("seam.abi"));
    var off = generate(MODULE.replace("=true", "=false") + ADD);
    assertEquals(plain, off);
    var named =
        generate(
            MODULE.replace("module cells ", "module cells java_package=a.b java_class=Api ") + ADD);
    assertTrue(named.containsKey("ApiTruffle.java"));
    assertTrue(named.get("ApiTruffle.java").contains("return Api.add(value_left, value_right);"));
    assertTrue(named.get("ApiTruffle.java").contains("package a.b;"));
    // Options are parsed strictly and only with cells enabled.
    for (String bad :
        List.of(
            "truffle_backend=ffm",
            "truffle_coercion=wrap",
            "truffle_transitions=boundary",
            "truffle_cells=true truffle_backend=jni",
            "truffle_cells=true truffle_coercion=lenient",
            "truffle_cells=true truffle_transitions=inline",
            "truffle_cells=maybe"))
      assertThrows(
          IllegalArgumentException.class,
          () -> generate(MODULE.replace(" truffle_cells=true", " " + bad) + ADD),
          bad);
    var module = Dsl.parse(MODULE + ADD);
    assertEquals(
        new TruffleConfig(TruffleBackend.NATIVE, TruffleCoercion.STRICT, TruffleTransitions.DIRECT),
        module.truffleConfig());
    assertEquals(
        module.truffleConfig(), module.withTarget("x86_64-unknown-linux-gnu").truffleConfig());
    assertTrue(Seam.truffleCell(module, module.functions().getFirst()));
  }

  @Test
  void providerFactsStayOutOfLlvmContracts() {
    String plain =
        MODULE + "import f return=i64 error=no_failure nounwind=true@explicit_contract\nend\n";
    var baseline = generate(plain);
    for (String fact : List.of("native_leaf", "truffle_cell", "truffle_raw_pointers")) {
      for (String value : List.of("true", "false")) {
        var output =
            generate(
                plain.replace("nounwind=", fact + "=" + value + "@explicit_contract nounwind="));
        String llvm = output.get("seam.ll");
        assertTrue(llvm.contains("declare i64 @f() nounwind\n"), llvm);
        assertFalse(llvm.contains(fact), llvm);
        assertNotEquals(baseline.get("seam.abi"), output.get("seam.abi"));
      }
    }
  }

  @Test
  void nativeCellsCoerceCallAndConvert() throws Exception {
    String truffle = generate(example()).get("CellApiTruffle.java");
    assertTrue(truffle.contains("public static final class add_Cell extends NativeCell"));
    assertTrue(
        truffle.contains(
            "new IntegerCoercion(\"add\", \"left\", 32, false, CoercionPolicy.STRICT)"));
    assertTrue(truffle.contains("int value_left = (int) coerce_left.execute(arguments[0]);"));
    assertTrue(truffle.contains("return CellApi.add(value_left, value_right);"));
    // Unsigned and small carriers: raw bits in, numeric values out.
    assertTrue(
        truffle.contains(
            "new IntegerCoercion(\"widen\", \"small\", 8, true, CoercionPolicy.STRICT)"));
    assertTrue(truffle.contains("byte value_small = (byte) coerce_small.execute(arguments[0]);"));
    assertTrue(truffle.contains("return Carriers.u8(CellApi.lowByte(value_value));"));
    assertTrue(truffle.contains("boolean value_flag = coerce_flag.execute(arguments[3]);"));
    assertTrue(truffle.contains("float value_value = (float) coerce_value.execute(arguments[0]);"));
    assertTrue(truffle.contains("double value_factor = coerce_factor.execute(arguments[1]);"));
    // Pointers are opaque; raw numbers only where the binding opted in.
    assertTrue(truffle.contains("new PointerCoercion(\"peek\", \"cell\", false, false)"));
    assertTrue(truffle.contains("new PointerCoercion(\"rawLength\", \"text\", true, true)"));
    assertTrue(truffle.contains("return CellApi.peek(WordFactory.pointer(address_cell));"));
    assertTrue(truffle.contains("CellApi.poke(WordFactory.pointer(address_cell), value_value);"));
    assertTrue(truffle.contains("return Carriers.none();"));
    // C strings and call borrows lend bytes for exactly the call.
    assertTrue(truffle.contains("new BytesCoercion(\"strlen\", \"text\", false, false, false)"));
    assertTrue(truffle.contains("new BytesCoercion(\"checksum\", \"bytes\", false, false, true)"));
    assertTrue(truffle.contains("new BytesCoercion(\"fill\", \"bytes\", false, true, true)"));
    assertTrue(
        truffle.contains(
            "return CellApi.checksum(WordFactory.pointer(borrow_bytes.address()),"
                + " borrow_bytes.length());"));
    assertTrue(
        truffle.contains(
            "int length_bytes = Carriers.length32(borrow_bytes.length(), true, this);"));
    assertTrue(
        truffle.contains(
            "CellApi.fill(WordFactory.pointer(borrow_bytes.address()), length_bytes,"
                + " value_value);"));
    assertTrue(truffle.contains("super(\"fill\", 2);"), "length parameter is derived");
    assertTrue(truffle.contains("super(\"checksum\", 1);"));
    assertTrue(truffle.contains("borrow_bytes.release();"));
    String fill = truffle.substring(truffle.indexOf("class fill_Cell"));
    int coerce = fill.indexOf("byte value_value = (byte) coerce_value.execute(arguments[1]);");
    int borrow = fill.indexOf("Borrow borrow_bytes = coerce_bytes.execute(arguments[0]);");
    assertTrue(coerce > 0 && borrow > coerce, "scalars coerce before any borrow is acquired");
    // Ownership: owners in, owners out.
    assertTrue(truffle.contains("long created = CellApi.counterCreate(value_start).rawValue();"));
    assertTrue(truffle.contains("if (created == 0) return Carriers.none();"));
    assertTrue(
        truffle.contains(
            "return new NativeResource(\"Counter\", created, DESTRUCTOR,"
                + " Thread.currentThread());"));
    assertTrue(
        truffle.contains(
            "DESTRUCTOR = address -> CellApi.counterDestroy(WordFactory.pointer(address));"));
    assertTrue(
        truffle.contains(
            "if (!(arguments[0] instanceof NativeResource resource) ||"
                + " !resource.type().equals(\"Counter\"))"));
    assertTrue(truffle.contains("resource.close();"));
    assertTrue(truffle.contains("super(\"counterDestroy\", 1);"));
    // Opt-outs and registry.
    assertFalse(truffle.contains("swap_Cell"));
    assertTrue(truffle.contains("case \"strlen\" -> strlen();"));
    assertTrue(truffle.contains("public static final String[] CELLS = {\"add\", \"checksum\","));
    assertTrue(truffle.contains("/** Leaf import cells_add through the Native Image binding. */"));
    assertTrue(
        truffle.contains(
            "/** Transitioning import cells_sleepy_add through the Native Image binding. */"));
    assertFalse(truffle.contains("TruffleBoundary"));
    assertFalse(truffle.contains("MemorySegment"));
    // Cells take part in partial evaluation, so they initialize at build time; bindings keep their
    // run-time address classes.
    String properties =
        generate(example())
            .get(
                "META-INF/native-image/dev.elide.myna/fixture.cells.CellApi/native-image.properties");
    assertTrue(
        properties.contains(
            " --initialize-at-build-time=fixture.cells.CellApiTruffle,fixture.cells.CellApiTruffle$add_Cell,"),
        properties);
    assertTrue(properties.contains("fixture.cells.CellApiTruffle$widen_Cell\n"), properties);
    assertFalse(properties.contains("swap_Cell"));
    assertTrue(properties.contains("--initialize-at-run-time=fixture.cells.CellApi$add_Binding"));
    String transitionsOnly =
        generate(
                MODULE
                    + "import slow return=i64 error=no_failure"
                    + " native_leaf=false@explicit_contract\n"
                    + "end\n")
            .get(
                "META-INF/native-image/dev.elide.myna/dev.elide.myna.generated.MynaNative/native-image.properties");
    assertTrue(
        transitionsOnly.endsWith(
            "Args ="
                + " --initialize-at-build-time=dev.elide.myna.generated.MynaTruffle,dev.elide.myna.generated.MynaTruffle$slow_Cell\n"),
        transitionsOnly);
    assertEquals(
        Set.of(
            "META-INF/native-image/dev.elide.myna/fixture.cells.CellApi/native-image.properties",
            "CellApi.java",
            "CellApiFFM.java",
            "CellApiForeignFeature.java",
            "CellApiOwnership.java",
            "CellApiTruffle.java",
            "seam.abi",
            "seam.h",
            "seam.json",
            "seam.ll",
            "seam.rs"),
        generate(example()).keySet());
  }

  @Test
  void boundaryTransitionsKeepTransitioningCallsOutOfCompiledCode() throws Exception {
    String truffle =
        generate(
                example()
                    .replace(
                        "truffle_cells=true", "truffle_cells=true truffle_transitions=boundary"))
            .get("CellApiTruffle.java");
    assertTrue(
        truffle.contains(
            "@CompilerDirectives.TruffleBoundary\n"
                + "        private static long call(long a0, long a1) {\n"
                + "            return CellApi.sleepyAdd(a0, a1);"));
    assertTrue(truffle.contains("return call(value_left, value_right);"));
    assertTrue(
        truffle.contains(
            "private static long call(long a0) {\n"
                + "            return CellApi.counterCreate(a0).rawValue();"));
    assertTrue(truffle.contains("long created = call(value_start);"));
    assertTrue(truffle.contains("DESTRUCTOR = counterCreate_Cell::destroy;"));
    assertTrue(
        truffle.contains(
            "private static void destroy(long address) {\n"
                + "            CellApi.counterDestroy(WordFactory.pointer(address));"));
    // Leaves are untouched: they inline into compiled guest code on every fork.
    assertTrue(truffle.contains("return CellApi.add(value_left, value_right);"));
    assertFalse(truffle.contains("private static int call("));
    String pointers =
        generate(
                MODULE.replace(
                        "truffle_cells=true", "truffle_cells=true truffle_transitions=boundary")
                    + "opaque Handle\n"
                    + "import open return=ptr<Handle> error=no_failure"
                    + " native_leaf=false@explicit_contract\n"
                    + "  param name type=ptr<char> access=read\n"
                    + "  param mode type=ptr<void>\n"
                    + "  param flags type=u16\n"
                    + "end\n")
            .get("MynaTruffle.java");
    assertTrue(pointers.contains("private static long call(long a0, long a1, short a2) {"));
    assertTrue(
        pointers.contains(
            "return MynaNative.open(WordFactory.pointer(a0), WordFactory.pointer(a1),"
                + " a2).rawValue();"));
    assertTrue(
        pointers.contains(
            "return Carriers.pointer(call(borrow_name.address(), address_mode, value_flags));"));
  }

  @Test
  void ffmCellsCallTheFfmBindingsAndReportLinkageFailures() throws Exception {
    String truffle =
        generate(
                example()
                    .replace(
                        "truffle_cells=true",
                        "truffle_cells=true truffle_backend=ffm truffle_coercion=wrap"))
            .get("CellApiTruffle.java");
    assertTrue(truffle.contains("every cell calls CellApiFFM."));
    assertTrue(truffle.contains("return CellApiFFM.add(value_left, value_right);"));
    assertTrue(truffle.contains("} catch (Throwable failure) {"));
    assertTrue(truffle.contains("throw failed(failure);"));
    assertTrue(truffle.contains("CoercionPolicy.WRAP"));
    assertFalse(truffle.contains("CoercionPolicy.STRICT"));
    assertTrue(
        truffle.contains("CellApiFFM.poke(MemorySegment.ofAddress(address_cell), value_value);"));
    assertTrue(truffle.contains("long created = CellApiFFM.counterCreate(value_start).address();"));
    assertTrue(
        truffle.contains(
            "DESTRUCTOR = address -> { try {"
                + " CellApiFFM.counterDestroy(MemorySegment.ofAddress(address)); }"
                + " catch (Throwable failure) { throw failed(failure); } };"));
    assertTrue(truffle.contains("private static RuntimeException failed(Throwable failure) {"));
    assertTrue(
        truffle.contains(
            "return NativeCellException.of(null, \"native call failed: \", failure);"));
    assertFalse(truffle.contains("WordFactory"));
    assertFalse(
        truffle.contains("TruffleBoundary"), "boundaries apply to Native Image transitions only");
    String empty =
        generate(
                MODULE.replace("truffle_cells=true", "truffle_cells=true truffle_backend=ffm")
                    + "import f return=i32 error=no_failure truffle_cell=false@explicit_contract\n"
                    + "end\n")
            .get("MynaTruffle.java");
    assertFalse(empty.contains("interface Call"));
    assertTrue(empty.contains("public static final String[] CELLS = {};"));
  }

  @Test
  void returnCarriersCoverEveryScalar() {
    var source = new StringBuilder(MODULE);
    for (Scalar s : Scalar.values())
      source
          .append("import r_")
          .append(s.text())
          .append(" return=")
          .append(s.text())
          .append(" error=no_failure\nend\n");
    source.append("opaque Handle\nimport r_ptr return=ptr<Handle> error=no_failure\nend\n");
    source.append("callback Cb return=i32\nend\nimport r_fn return=fn<Cb> error=no_failure\nend\n");
    String truffle = generate(source.toString()).get("MynaTruffle.java");
    assertTrue(truffle.contains("MynaNative.r_void();\n            return Carriers.none();"));
    assertTrue(truffle.contains("return MynaNative.r_bool();"));
    assertTrue(truffle.contains("return (int) MynaNative.r_char();"));
    assertTrue(truffle.contains("return (int) MynaNative.r_i8();"));
    assertTrue(truffle.contains("return Carriers.u8(MynaNative.r_u8());"));
    assertTrue(truffle.contains("return (int) MynaNative.r_i16();"));
    assertTrue(truffle.contains("return Carriers.u16(MynaNative.r_u16());"));
    assertTrue(truffle.contains("return MynaNative.r_i32();"));
    assertTrue(truffle.contains("return Carriers.u32(MynaNative.r_u32());"));
    assertTrue(truffle.contains("return MynaNative.r_i64();"));
    assertTrue(truffle.contains("return MynaNative.r_u64();"));
    assertTrue(truffle.contains("return MynaNative.r_usize();"));
    assertTrue(truffle.contains("return MynaNative.r_isize();"));
    assertTrue(truffle.contains("return (double) MynaNative.r_f32();"));
    assertTrue(truffle.contains("return MynaNative.r_f64();"));
    assertTrue(truffle.contains("return Carriers.pointer(MynaNative.r_ptr().rawValue());"));
    assertTrue(truffle.contains("return Carriers.pointer(MynaNative.r_fn().rawValue());"));
    String ffm =
        generate(
                source
                    .toString()
                    .replace("truffle_cells=true", "truffle_cells=true truffle_backend=ffm"))
            .get("MynaTruffle.java");
    assertTrue(ffm.contains("return Carriers.pointer(MynaFFM.r_ptr().address());"));
    assertTrue(ffm.contains("MynaFFM.r_void();\n                return Carriers.none();"));
  }

  /**
   * The C {@code char} scalar is target-dependent (signed on x86-64 and Apple arm64, unsigned on
   * AAPCS64 Linux). The cell's {@code char} coercion's {@code unsigned} flag and the {@code char}
   * return conversion follow the module's resolved ABI class, instead of always emitting {@code (8,
   * false)} and a sign-extending {@code (int)} cast that broke AAPCS64 Linux.
   */
  @Test
  void charSignednessFollowsTargetAbi() {
    String source =
        MODULE
            + "import r_char return=char error=no_failure\nend\n"
            + "import p_char return=i32 error=no_failure\n"
            + "  param c type=char\n  param i type=i8\n  param u type=u8\nend\n";
    // Signed-char target (aarch64-apple-darwin): char coerces as i8, return sign-extends, and the
    // ABI descriptor records no charUnsigned field so existing fingerprints are unchanged.
    var signedOutputs = generate(source);
    String signed = signedOutputs.get("MynaTruffle.java");
    assertTrue(
        signed.contains("new IntegerCoercion(\"p_char\", \"c\", 8, false, CoercionPolicy.STRICT)"),
        "signed-char target: char coerces as i8 in [-128, 127]");
    assertTrue(
        signed.contains("new IntegerCoercion(\"p_char\", \"i\", 8, false, CoercionPolicy.STRICT)"),
        "i8 stays signed on a signed-char target");
    assertTrue(
        signed.contains("new IntegerCoercion(\"p_char\", \"u\", 8, true, CoercionPolicy.STRICT)"),
        "u8 stays unsigned on a signed-char target");
    assertTrue(
        signed.contains("return (int) MynaNative.r_char();"), "signed char return sign-extends");
    assertFalse(
        signedOutputs.get("seam.json").contains("\"charUnsigned\""),
        "no charUnsigned for signed-char");
    // Unsigned-char target (aarch64-unknown-linux-gnu, AAPCS64 Linux): char coerces as u8 in
    // [0, 255] — a guest passing 200 no longer raises — and the return is masked to its numeric
    // value via Carriers.u8, so a char returning 200 surfaces as 200 instead of -56. i8 and u8
    // are unaffected; the carrier stays a raw byte in both bindings.
    var unsignedSource =
        source.replace("target=aarch64-apple-darwin", "target=aarch64-unknown-linux-gnu");
    var unsignedOutputs = generate(unsignedSource);
    String unsigned = unsignedOutputs.get("MynaTruffle.java");
    assertTrue(
        unsigned.contains("new IntegerCoercion(\"p_char\", \"c\", 8, true, CoercionPolicy.STRICT)"),
        "AAPCS64 Linux: char coerces as u8 in [0, 255]");
    assertTrue(
        unsigned.contains(
            "new IntegerCoercion(\"p_char\", \"i\", 8, false, CoercionPolicy.STRICT)"),
        "i8 stays signed on AAPCS64 Linux");
    assertTrue(
        unsigned.contains("new IntegerCoercion(\"p_char\", \"u\", 8, true, CoercionPolicy.STRICT)"),
        "u8 stays unsigned on AAPCS64 Linux");
    assertTrue(
        unsigned.contains("byte value_c = (byte) coerce_c.execute(arguments[0]);"),
        "char still lowers to the raw byte carrier the binding expects");
    assertTrue(
        unsigned.contains("return Carriers.u8(MynaNative.r_char());"),
        "AAPCS64 Linux char return surfaces as 0..255, not sign-extended");
    assertFalse(
        unsigned.contains("return (int) MynaNative.r_char();"),
        "AAPCS64 Linux char return does not sign-extend");
    assertTrue(
        unsignedOutputs.get("seam.json").contains("\"charUnsigned\":true"),
        "charUnsigned recorded for AAPCS64 Linux");
    // The two ABI classes no longer share one fingerprint: charUnsigned is part of the ABI class.
    assertNotEquals(
        signedOutputs.get("seam.abi"),
        unsignedOutputs.get("seam.abi"),
        "AAPCS64 Linux has a distinct ABI fingerprint from the signed-char targets");
    // The same fix applies to FFM-backend cells, which call MynaFFM (same carrier, same masking).
    var unsignedFfm =
        generate(
                unsignedSource.replace(
                    "truffle_cells=true", "truffle_cells=true truffle_backend=ffm"))
            .get("MynaTruffle.java");
    assertTrue(
        unsignedFfm.contains(
            "new IntegerCoercion(\"p_char\", \"c\", 8, true, CoercionPolicy.STRICT)"),
        "FFM cells on AAPCS64 Linux: char coerces as u8");
    assertTrue(
        unsignedFfm.contains("return Carriers.u8(MynaFFM.r_char());"),
        "FFM cells on AAPCS64 Linux: char return masks to 0..255");
  }

  @Test
  void parameterShapesSelectCoercions() {
    String truffle =
        generate(
                MODULE
                    + "opaque Handle\n"
                    + "import shapes return=void error=no_failure\n"
                    + "  param text type=ptr<char> access=read\n"
                    + "  param raw type=ptr<u8>\n"
                    + "  param out type=ptr<u8> access=write nullable=false\n"
                    + "  param both type=ptr<i8> access=read_write\n"
                    + "  param words type=ptr<i64> access=read\n"
                    + "  param nested type=ptr<ptr<u8>>\n"
                    + "  param handle type=ptr<Handle> nullable=false\n"
                    + "  param size type=usize\n"
                    + "  param offset type=isize\n"
                    + "  param c type=char\n"
                    + "end\n")
            .get("MynaTruffle.java");
    assertTrue(truffle.contains("new BytesCoercion(\"shapes\", \"text\", true, false, false)"));
    assertTrue(
        truffle.contains("new PointerCoercion(\"shapes\", \"raw\", true, false)"),
        "unspecified access stays a pointer");
    assertTrue(truffle.contains("new BytesCoercion(\"shapes\", \"out\", false, true, false)"));
    assertTrue(truffle.contains("new BytesCoercion(\"shapes\", \"both\", true, true, false)"));
    assertTrue(truffle.contains("new PointerCoercion(\"shapes\", \"words\", true, false)"));
    assertTrue(truffle.contains("new PointerCoercion(\"shapes\", \"nested\", true, false)"));
    assertTrue(truffle.contains("new PointerCoercion(\"shapes\", \"handle\", false, false)"));
    assertTrue(
        truffle.contains(
            "new IntegerCoercion(\"shapes\", \"size\", 64, true, CoercionPolicy.STRICT)"));
    assertTrue(
        truffle.contains(
            "new IntegerCoercion(\"shapes\", \"offset\", 64, false, CoercionPolicy.STRICT)"));
    assertTrue(
        truffle.contains(
            "new IntegerCoercion(\"shapes\", \"c\", 8, false, CoercionPolicy.STRICT)"));
    assertTrue(truffle.contains("byte value_c = (byte) coerce_c.execute(arguments[9]);"));
    // Three borrows nest their releases in reverse order.
    int first = truffle.indexOf("Borrow borrow_text");
    int second = truffle.indexOf("Borrow borrow_out");
    int third = truffle.indexOf("Borrow borrow_both");
    assertTrue(first < second && second < third);
    assertTrue(
        truffle.contains(
            "                    } finally {\n"
                + "                        borrow_both.release();\n"
                + "                    }\n"
                + "                } finally {\n"
                + "                    borrow_out.release();\n"
                + "                }\n"
                + "            } finally {\n"
                + "                borrow_text.release();\n"
                + "            }"));
    String narrow =
        generate(
                MODULE
                    + "borrow sum bytes length=count lifetime=call\n"
                    + "import sum return=i64 error=no_failure nounwind=true@explicit_contract\n"
                    + "  param count type=i32\n"
                    + "  param bytes type=ptr<u8> nullable=false ownership=borrowed access=read"
                    + " nocapture=true@explicit_contract readonly=true@explicit_contract\n"
                    + "end\n")
            .get("MynaTruffle.java");
    assertTrue(
        narrow.contains(
            "int length_bytes = Carriers.length32(borrow_bytes.length(), false, this);"));
    assertTrue(
        narrow.contains(
            "MynaNative.sum(length_bytes, WordFactory.pointer(borrow_bytes.address()))"));
    assertTrue(
        narrow.contains("coerce_bytes.execute(arguments[0])"),
        "the buffer is the only guest argument");
  }

  @Test
  void validationRejectsShapesCellsCannotCarry() {
    String records =
        MODULE
            + "struct Pair size=16 align=8\n"
            + "  field a type=i64 offset=0\n"
            + "  field b type=i64 offset=8\n"
            + "end\n"
            + "import swap return=Pair error=no_failure\n"
            + "  param value type=Pair\n"
            + "end\n";
    var error = assertThrows(IllegalArgumentException.class, () -> generate(records));
    assertTrue(error.getMessage().contains("by-value records"), error.getMessage());
    assertTrue(error.getMessage().contains("truffle_cell=false@explicit_contract on swap"));
    assertTrue(
        generate(
                records.replace(
                    "error=no_failure\n  param value type=Pair",
                    "error=no_failure truffle_cell=false@explicit_contract\n"
                        + "  param value type=Pair"))
            .containsKey("MynaTruffle.java"));
    String callbacks =
        MODULE
            + "callback Visitor return=i32\n  param value type=i64\nend\n"
            + "import forEach return=i32 error=no_failure native_leaf=false@explicit_contract\n"
            + "  param visitor type=fn<Visitor>\nend\n";
    assertTrue(
        assertThrows(IllegalArgumentException.class, () -> generate(callbacks))
            .getMessage()
            .contains("callback parameters"));
    // Found by SeamFuzzTest: an isolate_thread parameter crashed the cell emitter with a
    // ClassCastException instead of being rejected with opt-out advice.
    String isolate =
        MODULE
            + "import poke return=void error=no_failure\n"
            + "  param value type=i64\n  param isolate type=isolate_thread\nend\n";
    var isolateError = assertThrows(IllegalArgumentException.class, () -> generate(isolate));
    assertTrue(
        isolateError.getMessage().contains("isolate_thread parameters"), isolateError.getMessage());
    assertTrue(isolateError.getMessage().contains("truffle_cell=false@explicit_contract on poke"));
    assertTrue(
        generate(
                isolate.replace(
                    "error=no_failure\n",
                    "error=no_failure truffle_cell=false@explicit_contract\n"))
            .containsKey("MynaTruffle.java"));
    String noreturn =
        MODULE
            + "import die return=void error=no_failure native_leaf=false@explicit_contract"
            + " noreturn=true@explicit_contract\n"
            + "end\n";
    assertTrue(
        assertThrows(IllegalArgumentException.class, () -> generate(noreturn))
            .getMessage()
            .contains("returning import"));
    assertThrows(
        IllegalArgumentException.class, () -> generate(MODULE + "opaque add_Cell\n" + ADD));
    assertThrows(
        IllegalArgumentException.class, () -> generate(MODULE + "opaque MynaTruffle\n" + ADD));
    assertThrows(
        IllegalArgumentException.class,
        () -> generate(MODULE + ADD.replace("add ", "MynaTruffle ")));
    // The facts need the module option, and apply to imports only.
    String plain = MODULE.replace(" truffle_cells=true", "");
    for (String fact : List.of("truffle_cell=false", "truffle_raw_pointers=true"))
      assertTrue(
          assertThrows(
                  IllegalArgumentException.class,
                  () ->
                      generate(
                          plain
                              + ADD.replace(
                                  "error=no_failure",
                                  "error=no_failure " + fact + "@explicit_contract")))
              .getMessage()
              .contains("requires module truffle_cells=true"));
    assertTrue(
        assertThrows(
                IllegalArgumentException.class,
                () ->
                    generate(
                        MODULE
                            + "export e return=i64 java=fixture.Ops.e isolate=thread error=abort"
                            + " truffle_cell=true@explicit_contract\nend\n"))
            .getMessage()
            .contains("imports only"));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            generate(
                MODULE
                    + ADD.replace(
                        "error=no_failure",
                        "error=no_failure truffle_cell=maybe@explicit_contract")));
    // Relaxed mode still refuses the unsupported shapes.
    assertThrows(
        IllegalArgumentException.class, () -> Generator.generate(Dsl.parse(records), false));
    // Exports are never cells, and a module with no eligible imports still gets its class.
    var exportsOnly =
        generate(
            MODULE + "export e return=i64 java=fixture.Ops.e isolate=thread error=abort\nend\n");
    assertTrue(exportsOnly.get("MynaTruffle.java").contains("CELLS = {};"));
    assertFalse(exportsOnly.get("MynaTruffle.java").contains("_Cell"));
  }

  @Test
  void truffleClassCollisionIsRejectedWithoutAnyCell() {
    String optedOut =
        ADD.replace("error=no_failure", "error=no_failure truffle_cell=false@explicit_contract");
    String named = MODULE.replace("module cells ", "module cells java_package=a.b java_class=Api ");
    var failure =
        assertThrows(
            IllegalArgumentException.class,
            () -> generate(named + "opaque ApiTruffle\n" + optedOut));
    assertTrue(failure.getMessage().contains("collides with generated Truffle class"));
    assertTrue(generate(named + optedOut).containsKey("ApiTruffle.java"));
  }

  @Test
  void ffmHelpersPassGuestErrorsThrough() {
    String ffm =
        generate(
                MODULE.replace("truffle_cells=true", "truffle_cells=true truffle_backend=ffm")
                    + ADD)
            .get("MynaTruffle.java");
    assertTrue(
        ffm.contains("if (failure instanceof AbstractTruffleException guest) return guest;"));
    assertTrue(ffm.contains("if (failure instanceof VirtualMachineError fatal) throw fatal;"));
    // The 32-bit length check runs before the handler, so its guest error is not rewrapped.
    String fill =
        generate(
                MODULE.replace("truffle_cells=true", "truffle_cells=true truffle_backend=ffm")
                    + "borrow fill bytes length=count lifetime=call\n"
                    + "import fill return=void error=no_failure nounwind=true@explicit_contract\n"
                    + "  param bytes type=ptr<u8> nullable=false ownership=borrowed"
                    + " access=read_write nocapture=true@explicit_contract\n"
                    + "  param count type=u32\n"
                    + "end\n")
            .get("MynaTruffle.java");
    assertTrue(
        fill.contains(
            "int length_bytes = Carriers.length32(borrow_bytes.length(), true, this);\n"
                + "                try {\n"
                + "                   "
                + " MynaFFM.fill(MemorySegment.ofAddress(borrow_bytes.address()), length_bytes);"));
  }

  @Test
  void goldenOutputs() throws Exception {
    var actual = generate(example());
    for (var output : actual.entrySet())
      assertEquals(
          Files.readString(Path.of("tests/golden/truffle", output.getKey())),
          output.getValue(),
          output.getKey());
    assertEquals(SeamJson.fingerprint(Dsl.parse(example())) + "\n", actual.get("seam.abi"));
  }
}
