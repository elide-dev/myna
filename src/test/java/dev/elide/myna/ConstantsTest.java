// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package dev.elide.myna;

import static org.junit.jupiter.api.Assertions.*;

import dev.elide.myna.frontend.Dsl;
import dev.elide.myna.model.Names;
import dev.elide.myna.model.Seam;
import dev.elide.myna.model.Seam.*;
import dev.elide.myna.model.Seam.Module;
import dev.elide.myna.validate.Ranges;
import java.math.BigInteger;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ConstantsTest {
  static final TargetAbi X86 = Seam.targetAbi("x86_64-unknown-linux-gnu");
  static final TargetAbi AARCH64 = Seam.targetAbi("aarch64-unknown-linux-gnu");

  @ParameterizedTest
  @CsvSource({
    "I8,-128,127",
    "U8,0,255",
    "I16,-32768,32767",
    "U16,0,65535",
    "I32,-2147483648,2147483647",
    "U32,0,4294967295",
    "I64,-9223372036854775808,9223372036854775807",
    "U64,0,18446744073709551615",
    "ISIZE,-9223372036854775808,9223372036854775807",
    "USIZE,0,18446744073709551615"
  })
  void rangesMatchCarriers(Scalar scalar, String min, String max) {
    var lo = new BigInteger(min);
    var hi = new BigInteger(max);
    assertEquals(lo, Ranges.min(scalar, X86));
    assertEquals(hi, Ranges.max(scalar, X86));
    assertTrue(Ranges.fits(scalar, lo, X86));
    assertTrue(Ranges.fits(scalar, hi, X86));
    assertFalse(Ranges.fits(scalar, lo.subtract(BigInteger.ONE), X86));
    assertFalse(Ranges.fits(scalar, hi.add(BigInteger.ONE), X86));
  }

  @Test
  void charFollowsTargetSignedness() {
    assertEquals(BigInteger.valueOf(-128), Ranges.min(Scalar.CHAR, X86));
    assertEquals(BigInteger.valueOf(127), Ranges.max(Scalar.CHAR, X86));
    assertEquals(BigInteger.ZERO, Ranges.min(Scalar.CHAR, AARCH64));
    assertEquals(BigInteger.valueOf(255), Ranges.max(Scalar.CHAR, AARCH64));
  }

  @Test
  void parsesDeclarationsAndReferences() throws Exception {
    var m = Dsl.parse(Files.readString(Path.of("examples/constants.seam")));
    assertEquals(
        java.util.List.of("FLOOR", "LIMIT", "MAGIC"),
        m.constants().stream().map(Constant::name).toList());
    var magic = m.constants().get(2);
    assertEquals(Scalar.U32, magic.type());
    assertEquals(BigInteger.valueOf(0x3141514bL), magic.value());
    assertEquals(new BigInteger("18446744073709551615"), m.constants().get(1).value());
    assertEquals(new BigInteger("-9223372036854775808"), m.constants().get(0).value());
    assertEquals(
        java.util.List.of("Level", "Status"), m.enums().stream().map(EnumGroup::name).toList());
    var status = m.enums().get(1);
    assertEquals(Scalar.I32, status.type());
    assertEquals(
        java.util.List.of(
            new EnumValue("null_result", BigInteger.valueOf(-1)),
            new EnumValue("ok", BigInteger.ZERO),
            new EnumValue("internal_error", BigInteger.valueOf(3)),
            new EnumValue("syntax_error", BigInteger.valueOf(16))),
        status.values());
    assertEquals(
        java.util.List.of("Perms", "Wide"), m.flagSets().stream().map(FlagSet::name).toList());
    assertFalse(m.flagSets().get(0).cBitfields());
    assertEquals(
        java.util.List.of(
            new FlagBit("read", 0), new FlagBit("write", 1), new FlagBit("sticky", 31)),
        m.flagSets().get(0).bits());
    assertTrue(m.flagSets().get(1).cBitfields());
    assertEquals(63, m.flagSets().get(1).bits().get(1).bit());

    var record = m.structs().get(0);
    assertEquals(new GroupRef(GroupKind.ENUM, "Status"), record.fields().get(0).group());
    assertEquals(new GroupRef(GroupKind.FLAGS, "Perms"), record.fields().get(1).group());
    assertEquals(new GroupRef(GroupKind.FLAGS, "Wide"), record.fields().get(2).group());
    assertNull(record.fields().get(3).group());
    assertEquals(new GroupRef(GroupKind.ENUM, "Status"), m.callbacks().get(0).returnsGroup());

    var leaf = function(m, "status");
    assertEquals(new GroupRef(GroupKind.ENUM, "Status"), leaf.returnsGroup());
    assertTrue(leaf.facts().isEmpty(), "enum= must not become a fact");
    assertTrue(Seam.nativeLeaf(leaf));
    var setLevel = function(m, "setLevel");
    assertEquals(new GroupRef(GroupKind.ENUM, "Level"), setLevel.parameters().get(0).group());
    assertTrue(setLevel.parameters().get(0).facts().isEmpty());
    var transition = function(m, "blockingStatus");
    assertEquals(new GroupRef(GroupKind.ENUM, "Status"), transition.returnsGroup());
    assertFalse(Seam.nativeLeaf(transition));
    assertEquals(1, transition.facts().size());

    assertEquals("ConstsConstants", m.constantsClass());
    assertEquals("ConstsDirectives", m.directivesClass());
  }

  @Test
  void namesFollowTheSpecExamples() {
    var m = Dsl.parse("module komodo_frontend target=x86_64-unknown-linux-gnu\n");
    assertEquals("FUNCTION_FLAGS", Names.upperSnake("FunctionFlags"));
    assertEquals("function_flags", Names.lowerSnake("FunctionFlags"));
    assertEquals("analysisMagic", Names.lowerCamel("ANALYSIS_MAGIC"));
    assertEquals("KOMODO_FRONTEND_STATUS_OK", Names.cMacro(m, "Status", "ok"));
    assertEquals("KOMODO_FRONTEND_ANALYSIS_MAGIC", Names.cMacro(m, null, "ANALYSIS_MAGIC"));
    assertEquals("komodo_frontend_status_t", Names.cTypedef(m, "Status"));
    assertEquals("komodo_frontend_status_name", Names.cNameFunction(m, "Status"));
    assertEquals("komodo_frontend_function_flags_bits", Names.cBitsStruct(m, "FunctionFlags"));
  }

  @Test
  void acronymsStayTogetherInSnakeCase() {
    assertEquals("http_status", Names.lowerSnake("HTTPStatus"));
    assertEquals("utf8_mode", Names.lowerSnake("Utf8Mode"));
    assertEquals("a_bc", Names.lowerSnake("A_Bc"));
  }

  @Test
  void rangesRejectNonIntegerCarriers() {
    assertThrows(IllegalArgumentException.class, () -> Ranges.max(Scalar.F32, X86));
  }

  @Test
  void compatConstructorsLeaveReferencesEmpty() {
    var fn =
        new Function(
            "f",
            "f",
            Direction.IMPORT,
            Scalar.VOID,
            java.util.List.of(),
            ErrorConvention.NO_FAILURE,
            null,
            null,
            null,
            null,
            false,
            new java.util.TreeMap<>());
    assertNull(fn.returnsGroup());
    var callback = new Signature("cb", Scalar.VOID, java.util.List.of(), null, null, null);
    assertNull(callback.returnsGroup());
    assertNull(new Field("a", Scalar.U8, 0).group());
    assertFalse(new FlagSet("F", Scalar.U8, java.util.List.of(new FlagBit("a", 0))).cBitfields());
  }

  @Test
  void hexAndDecimalAreEqual() {
    assertEquals(Dsl.integer("16"), Dsl.integer("0x10"));
    assertEquals(Dsl.integer("16"), Dsl.integer("0X10"));
    assertEquals(BigInteger.valueOf(-5), Dsl.integer("-5"));
  }

  @Test
  void reorderedDeclarationsAreCanonical() {
    String a = "module m target=x86_64-unknown-linux-gnu\n";
    var one =
        Dsl.parse(
            a
                + "enum E type=i32\n value b = 2\n value a = 1\nend\nconst B type=u8 value=1\n"
                + "const A type=u8 value=2\nflags F type=u8\n bit y = 3\n bit x = 1\nend\n");
    assertEquals("A", one.constants().get(0).name());
    assertEquals("a", one.enums().get(0).values().get(0).name());
    assertEquals("x", one.flagSets().get(0).bits().get(0).name());
  }

  static Function function(Module m, String name) {
    return m.functions().stream().filter(f -> f.name().equals(name)).findFirst().orElseThrow();
  }

  static String parseError(String body) {
    return assertThrows(
            IllegalArgumentException.class,
            () -> Dsl.parse("module m target=x86_64-unknown-linux-gnu\n" + body))
        .getMessage();
  }

  @Test
  void parserNegatives() {
    assertEquals(
        "line 3: expected: value NAME = INTEGER",
        parseError("enum E type=i32\n value ok 0\nend\n"));
    assertEquals(
        "line 4: expected: value NAME = INTEGER",
        parseError("enum E type=i32\nvalue a = 1\n value ok = 0 1\nend\n"));
    assertEquals("line 2: value requires an enum block", parseError("value ok = 0\n"));
    assertEquals(
        "line 3: value requires an enum block",
        parseError("flags F type=u8\n value ok = 0\nend\n"));
    assertEquals(
        "line 3: invalid integer: 0xZZ", parseError("enum E type=i32\n value ok = 0xZZ\nend\n"));
    assertEquals(
        "line 3: invalid integer: -0x1", parseError("enum E type=i32\n value ok = -0x1\nend\n"));
    assertEquals("unterminated block", parseError("enum E type=i32\n value ok = 0\n"));
    assertEquals(
        "line 2: unknown options: [extra]", parseError("const A type=u8 value=1 extra=2\n"));
    assertEquals("line 2: missing value", parseError("const A type=u8\n"));
    assertEquals("line 2: invalid integer: 0xZZ", parseError("const A type=u8 value=0xZZ\n"));
    assertEquals(
        "line 3: unknown options: [c_bitfields]",
        parseError("enum E type=i32 c_bitfields=true\nend\n"));
    assertEquals(
        "line 3: bit requires a flags block", parseError("enum E type=i32\n bit a = 1\nend\n"));
    assertEquals(
        "line 3: expected: bit NAME = INDEX", parseError("flags F type=u8\n bit a 1\nend\n"));
    assertEquals(
        "line 3: enum= and flags= are mutually exclusive",
        parseError("struct S size=4 align=4\n field a type=u32 offset=0 enum=E flags=F\nend\n"));
  }
}
