// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package dev.elide.myna;

import static org.junit.jupiter.api.Assertions.*;

import dev.elide.myna.frontend.Dsl;
import dev.elide.myna.model.Names;
import dev.elide.myna.model.Seam;
import dev.elide.myna.model.Seam.*;
import dev.elide.myna.model.Seam.Module;
import dev.elide.myna.serialize.SeamJson;
import dev.elide.myna.validate.Ranges;
import dev.elide.myna.validate.Validator;
import java.math.BigInteger;
import java.nio.file.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

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

  static final String HEAD = "module komodo_frontend target=x86_64-unknown-linux-gnu\n";
  static final String CELLS =
      "module komodo_frontend target=x86_64-unknown-linux-gnu java_class=Cells"
          + " truffle_cells=true\n";

  static String invalid(String head, String body) {
    return assertThrows(
            IllegalArgumentException.class, () -> Validator.validate(Dsl.parse(head + body), true))
        .getMessage();
  }

  static final String FL = "flags FunctionFlags type=u32\n bit exported = 0\nend\n";
  static final String ST = "enum Status type=i32\n value ok = 0\nend\n";
  static final String IMP = "import status symbol=s return=i32 error=no_failure";
  static final String NOFAIL = " return=void error=no_failure\nend";

  static Stream<Arguments> rejects() {
    return Stream.of(
        Arguments.of(
            "enum Status type=f32\n value ok = 0\nend\n",
            "enum Status: carrier must be an integer scalar, found f32"),
        Arguments.of(
            "enum Status type=void\n value ok = 0\nend\n",
            "enum Status: carrier must be an integer scalar, found void"),
        Arguments.of(
            "enum Status type=bool\n value ok = 0\nend\n",
            "enum Status: carrier must be an integer scalar, found bool"),
        Arguments.of(
            "enum Status type=char\n value ok = 0\nend\n",
            "enum Status: carrier must be an integer scalar, found char"),
        Arguments.of(
            "const X type=f32 value=1\n", "const X: carrier must be an integer scalar, found f32"),
        Arguments.of(
            "flags F type=bool\n bit a = 0\nend\n",
            "flags F: carrier must be an integer scalar, found bool"),
        Arguments.of("enum Status type=i32\nend\n", "enum Status: declares no values"),
        Arguments.of(
            "flags FunctionFlags type=u32\nend\n", "flags FunctionFlags: declares no bits"),
        Arguments.of(ST + "enum Status type=i32\n value x = 1\nend\n", "duplicate type: Status"),
        Arguments.of("opaque Status\n" + ST, "duplicate type: Status"),
        Arguments.of(ST + "flags Status type=u32\n bit a = 0\nend\n", "duplicate type: Status"),
        Arguments.of("enum u32 type=i32\n value ok = 0\nend\n", "type shadows scalar: u32"),
        Arguments.of(
            "enum core type=i32\n value ok = 0\nend\n", "type shadows generated name: core"),
        Arguments.of(
            "flags Option type=u32\n bit a = 0\nend\n", "type shadows generated name: Option"),
        Arguments.of(
            ST + "import Status symbol=Status" + NOFAIL + "\n",
            "enum Status: name collides with function symbol Status"),
        Arguments.of(
            FL + "import FunctionFlags symbol=FunctionFlags" + NOFAIL + "\n",
            "flags FunctionFlags: name collides with function symbol FunctionFlags"),
        Arguments.of("const A type=u8 value=1\nconst A type=u8 value=2\n", "duplicate constant: A"),
        Arguments.of(
            "const X type=u8 value=1\nimport q symbol=X" + NOFAIL + "\n",
            "const X: name collides with function symbol X"),
        Arguments.of(
            "const ANALYSIS_MAGIC type=u32 value=4294967296\n",
            "const ANALYSIS_MAGIC: value 4294967296 does not fit u32"),
        Arguments.of(
            "const ANALYSIS_MAGIC type=i8 value=-129\n",
            "const ANALYSIS_MAGIC: value -129 does not fit i8"),
        Arguments.of(
            "const BINDING_ABI_VERSION type=u8 value=1\n",
            "invalid or reserved identifier: BINDING_ABI_VERSION"),
        Arguments.of(
            "enum Status type=i32\n value ok = 0\n value ok = 1\nend\n",
            "enum Status: duplicate value name: ok"),
        Arguments.of(
            "enum Status type=i32\n value ok = 0\n value success = 0\nend\n",
            "enum Status: values ok and success are both 0"),
        Arguments.of(
            "enum Status type=i32\n value internal_error = 2147483648\nend\n",
            "enum Status: value internal_error = 2147483648 does not fit i32"),
        Arguments.of(
            "enum Status type=i32\n value Ok = 0\nend\n", "enum Status: invalid value name: Ok"),
        Arguments.of(
            "flags FunctionFlags type=u32\n bit exported = 0\n bit exported = 1\nend\n",
            "flags FunctionFlags: duplicate bit name: exported"),
        Arguments.of(
            "flags FunctionFlags type=u32\n bit exported = 0\n bit public = 0\nend\n",
            "flags FunctionFlags: bits exported and public both use bit 0"),
        Arguments.of(
            "flags FunctionFlags type=u32\n bit external = 32\nend\n",
            "flags FunctionFlags: bit external = 32 is outside u32 (bits 0..31)"),
        Arguments.of(
            "flags FunctionFlags type=u32\n bit all = 0\nend\n",
            "flags FunctionFlags: bit name is reserved: all"),
        Arguments.of(
            "flags FunctionFlags type=u32\n bit none = 0\nend\n",
            "flags FunctionFlags: bit name is reserved: none"),
        Arguments.of(
            "flags FunctionFlags type=u32\n bit Mixed = 0\nend\n",
            "flags FunctionFlags: invalid bit name: Mixed"),
        Arguments.of(
            FL
                + "struct IrFunction size=4 align=4\n"
                + " field flags type=u32 offset=0 flags=FunctionFlagz\nend\n",
            "field IrFunction.flags: undeclared flag set: FunctionFlagz"),
        Arguments.of(ST + IMP + " enum=Statuz\nend\n", "import status: undeclared enum: Statuz"),
        Arguments.of(
            "import compile symbol=c return=void error=no_failure\n"
                + " param mode type=i32 enum=Modez\n"
                + "end\n",
            "param compile.mode: undeclared enum: Modez"),
        Arguments.of(
            FL + IMP + " enum=FunctionFlags\nend\n",
            "import status: enum=FunctionFlags names a flag set; use flags=FunctionFlags"),
        Arguments.of(
            ST + IMP + " flags=Status\nend\n",
            "import status: flags=Status names an enum; use enum=Status"),
        Arguments.of(
            FL
                + "struct IrFunction size=8 align=8\n"
                + " field flags type=u64 offset=0 flags=FunctionFlags\nend\n",
            "field IrFunction.flags: flags=FunctionFlags has carrier u32, but the field's type is"
                + " u64"),
        Arguments.of(
            FL
                + "struct IrFunction size=8 align=8\n"
                + " field flags type=ptr<u32> offset=0 flags=FunctionFlags\nend\n",
            "field IrFunction.flags: flags=FunctionFlags has carrier u32, but the field's type is"
                + " ptr<u32>"),
        Arguments.of(
            ST + IMP + " enum=Status\n param p type=ptr<i32> enum=Status\nend\n",
            "param status.p: enum=Status has carrier i32, but the parameter's type is ptr<i32>"),
        Arguments.of(
            ST + "import status symbol=s return=u8 error=no_failure enum=Status\nend\n",
            "import status: enum=Status has carrier i32, but the return type is u8"),
        // References on export and callback headers.
        Arguments.of(
            ST
                + "export run symbol=r return=i32 java=a.B.run isolate=thread error=abort"
                + " enum=Statuz\nend\n",
            "export run: undeclared enum: Statuz"),
        Arguments.of(
            FL
                + "export run symbol=r return=i32 java=a.B.run isolate=thread error=abort"
                + " enum=FunctionFlags\nend\n",
            "export run: enum=FunctionFlags names a flag set; use flags=FunctionFlags"),
        Arguments.of(
            FL
                + "export run symbol=r return=u64 java=a.B.run isolate=thread error=abort"
                + " flags=FunctionFlags\nend\n",
            "export run: flags=FunctionFlags has carrier u32, but the return type is u64"),
        Arguments.of(
            "export run symbol=r return=i32 java=a.B.run isolate=thread error=abort\n"
                + " param mode type=i32 enum=Modez\nend\n",
            "param run.mode: undeclared enum: Modez"),
        Arguments.of(
            "callback Cb return=i32 enum=Statuz\nend\n", "callback Cb: undeclared enum: Statuz"),
        Arguments.of(
            ST + "callback Cb return=u8 enum=Status\nend\n",
            "callback Cb: enum=Status has carrier i32, but the return type is u8"),
        Arguments.of(
            ST + "callback Cb return=void\n param x type=i32 flags=Status\nend\n",
            "param Cb.x: flags=Status names an enum; use enum=Status"),
        // Generated C names.
        Arguments.of(
            "const STATUS_OK type=i32 value=1\n" + ST,
            "C name KOMODO_FRONTEND_STATUS_OK is generated by both const STATUS_OK and enum Status"
                + " value ok"),
        Arguments.of(
            "flags Status2 type=u8\n bit a = 0\nend\nconst STATUS2_ALL type=u8 value=1\n",
            "C name KOMODO_FRONTEND_STATUS2_ALL is generated by both const STATUS2_ALL and flags"
                + " Status2 _ALL"),
        Arguments.of(
            "const SEAM_ABI type=u8 value=1\n",
            "const SEAM_ABI: C name KOMODO_FRONTEND_SEAM_ABI is reserved by the generated header"),
        Arguments.of(
            "const SEAM_H type=u8 value=1\n",
            "const SEAM_H: C name KOMODO_FRONTEND_SEAM_H is reserved by the generated header"),
        Arguments.of(
            "enum Binding type=i32\n value abi_version = 0\nend\n",
            "enum Binding value abi_version: C name KOMODO_FRONTEND_BINDING_ABI_VERSION is reserved"
                + " by the generated header"),
        Arguments.of(
            "enum Seam type=i32\n value abi = 0\nend\n",
            "enum Seam value abi: C name KOMODO_FRONTEND_SEAM_ABI is reserved by the generated"
                + " header"),
        Arguments.of(
            "flags Binding type=u32\n bit abi_version = 0\nend\n",
            "flags Binding bit abi_version: C name KOMODO_FRONTEND_BINDING_ABI_VERSION is reserved"
                + " by the generated header"),
        Arguments.of(
            "enum A type=i32\n value b_t = 0\nend\nenum A_B type=i32\n value t = 0\nend\n",
            "C name KOMODO_FRONTEND_A_B_T is generated by both enum A value b_t and enum A_B value"
                + " t"),
        Arguments.of(
            ST + "import q symbol=komodo_frontend_status_t" + NOFAIL + "\n",
            "C name komodo_frontend_status_t is generated by both function symbol"
                + " komodo_frontend_status_t and enum Status typedef"),
        Arguments.of(
            ST + "import q symbol=komodo_frontend_status_name" + NOFAIL + "\n",
            "C name komodo_frontend_status_name is generated by both function symbol"
                + " komodo_frontend_status_name and enum Status name function"),
        Arguments.of(
            "flags F type=u8 c_bitfields=true\n bit a = 0\nend\n"
                + "import q symbol=komodo_frontend_f_bits"
                + NOFAIL
                + "\n",
            "C name komodo_frontend_f_bits is generated by both function symbol"
                + " komodo_frontend_f_bits and flags F bits struct"),
        Arguments.of(
            "flags F type=i32 c_bitfields=true\n bit a = 0\nend\n",
            "flags F: c_bitfields requires an unsigned carrier, found i32"),
        Arguments.of(
            ST + IMP + " enum=Status\nend\nimport statusEnum symbol=se" + NOFAIL + "\n",
            "logical name collides with the typed enum wrapper of status: statusEnum"),
        // Generated Java names.
        Arguments.of(
            "opaque MynaNativeConstants\nconst A type=u8 value=1\n",
            "type collides with generated constants class MynaNativeConstants"),
        Arguments.of(
            "opaque MynaNativeDirectives\nconst A type=u8 value=1\n",
            "type collides with generated directives class MynaNativeDirectives"),
        Arguments.of(
            "enum MynaNativeConstants type=i32\n value ok = 0\nend\n",
            "type collides with generated constants class MynaNativeConstants"),
        Arguments.of(
            "opaque StatusNames\n" + ST,
            "enum Status: generated name StatusNames collides with a declared type"),
        Arguments.of(
            ST + "enum StatusNames type=i32\n value ok = 0\nend\n",
            "enum Status: generated name StatusNames collides with a declared type"),
        Arguments.of(
            "opaque MynaNativeDirectives\n",
            "type collides with generated directives class MynaNativeDirectives"),
        // Groups are types of the generated code.
        Arguments.of(
            "enum FooFn type=i32\n value ok = 0\nend\n" + "import foo symbol=f" + NOFAIL + "\n",
            "Rust alias FooFn collides with a declared type"),
        Arguments.of(
            "enum bar_Downcall type=i32\n value ok = 0\nend\n"
                + "import bar symbol=b"
                + NOFAIL
                + "\n",
            "type collides with generated FFM adapter for bar"),
        Arguments.of(
            "enum bar_CriticalDowncall type=i32\n value ok = 0\nend\n"
                + "import bar symbol=b"
                + NOFAIL
                + "\n",
            "type collides with generated FFM adapter for bar"),
        Arguments.of(
            "enum foo_Leaf type=i32\n value ok = 0\nend\n" + "import foo symbol=f" + NOFAIL + "\n",
            "type collides with generated native leaf binding for foo"),
        Arguments.of(
            "enum foo_Binding type=i32\n value ok = 0\nend\n"
                + "import foo symbol=f"
                + NOFAIL
                + "\n",
            "type collides with generated native leaf binding for foo"),
        Arguments.of(
            "enum XCallback type=i32\n value ok = 0\nend\n" + "callback X return=void\nend\n",
            "type collides with generated FFM callback helper: XCallback"),
        Arguments.of(
            "enum XUpcalls type=i32\n value ok = 0\nend\n" + "callback X return=void\nend\n",
            "type collides with generated FFM callback helper: XUpcalls"),
        Arguments.of(
            "flags XCallback type=u8\n bit a = 0\nend\ncallback X return=void\nend\n",
            "type collides with generated FFM callback helper: XCallback"),
        Arguments.of("callback Status return=void\nend\n" + ST, "duplicate type: Status"),
        Arguments.of(
            ST + "struct S size=8 align=8\n field p type=ptr<Status> offset=0\nend\n",
            "unknown pointee: Status"),
        // Bitfield accessors.
        Arguments.of(
            "flags FunctionFlags type=u32 c_bitfields=true\n bit exported = 0\nend\n"
                + "struct IrFunction size=8 align=4\n"
                + " field flags type=u32 offset=0 flags=FunctionFlags\n"
                + " field flagsExported type=u32 offset=4\nend\n",
            "struct IrFunction: accessor flagsExported of field flags bit exported collides with"
                + " field flagsExported"),
        Arguments.of(
            "flags F type=u8 c_bitfields=true\n bit a_b = 0\n bit a__b = 1\nend\n"
                + "struct S size=1 align=1\n field f type=u8 offset=0 flags=F\nend\n",
            "struct S: accessor fAB of field f bit a__b collides with accessor of field f bit"
                + " a_b"));
  }

  @ParameterizedTest
  @MethodSource("rejects")
  void rejects(String body, String message) {
    assertEquals(message, invalid(HEAD, body));
  }

  @Test
  void groupsCollideWithTheFfmClass() {
    assertEquals(
        "type collides with generated FFM class CellsFFM",
        invalid(CELLS, "enum CellsFFM type=i32\n value ok = 0\nend\n"));
  }

  static Stream<Arguments> rejectsWithCells() {
    return Stream.of(
        Arguments.of(
            "enum CellsTruffle type=i32\n value ok = 0\nend\n",
            "type collides with generated Truffle class CellsTruffle"),
        Arguments.of(
            "enum status_Cell type=i32\n value ok = 0\nend\n" + IMP + "\nend\n",
            "type collides with generated Truffle cell for status"));
  }

  @ParameterizedTest
  @MethodSource("rejectsWithCells")
  void rejectsWithCells(String body, String message) {
    assertEquals(message, invalid(CELLS, body));
  }

  @Test
  void referencesAcceptMatchingCarriers() {
    var m =
        Dsl.parse(
            HEAD + ST + FL + IMP + " enum=Status\n param f type=u32 flags=FunctionFlags\nend\n");
    Validator.validate(m, true);
    Validator.validate(
        Dsl.parse(
            HEAD
                + "flags F type=u8 c_bitfields=true\n bit a = 0\n bit b_c = 1\nend\n"
                + "struct S size=1 align=1\n field f type=u8 offset=0 flags=F\nend\n"),
        true);
  }

  @Test
  void examplePasses() throws Exception {
    Validator.validate(Dsl.parse(Files.readString(Path.of("examples/constants.seam"))), true);
  }

  @Test
  void bothOptionsAreRejectedOnEveryDeclaration() {
    for (String line :
        new String[] {
          "import f symbol=f return=i32 error=no_failure enum=E flags=F\nend\n",
          "export f symbol=f return=i32 java=a.B.f isolate=thread error=abort enum=E flags=F\n"
              + "end\n",
          "callback Cb return=i32 enum=E flags=F\nend\n",
          "import f symbol=f return=void error=no_failure\n param p type=i32 enum=E flags=F\nend\n",
          "struct S size=4 align=4\n field a type=i32 offset=0 enum=E flags=F\nend\n"
        }) {
      var message = parseError(line);
      assertTrue(message.endsWith("enum= and flags= are mutually exclusive"), message);
      assertTrue(message.startsWith("line "), message);
    }
  }

  @Test
  void paramIsRejectedInsideGroupBlocks() {
    assertEquals(
        "line 3: param requires a function block and a type",
        parseError("enum E type=i32\n param p type=i32\nend\n"));
    assertEquals(
        "line 3: param requires a function block and a type",
        parseError("flags F type=u8\n param p type=u8\nend\n"));
  }

  static final String BASE =
      "module m target=x86_64-unknown-linux-gnu\n"
          + "enum E type=i32\n value a = 1\n value b = 0x10\nend\n"
          + "flags F type=u32\n bit x = 0\n bit y = 1\nend\n"
          + "const C type=u32 value=5\n"
          + "struct S size=8 align=4\n field f type=u32 offset=0 flags=F\n"
          + " field e type=i32 offset=4 enum=E\nend\n";

  static String abi(String source) {
    return SeamJson.fingerprint(Dsl.parse(source));
  }

  @Test
  void fingerprintTracksValues() throws Exception {
    assertEquals(
        Files.readString(Path.of("tests/golden/seam.abi")).trim(),
        abi(Files.readString(Path.of("examples/buffer.seam"))));
    assertEquals(
        Files.readString(Path.of("tests/golden/truffle/seam.abi")).trim(),
        abi(Files.readString(Path.of("examples/truffle.seam"))));
    String base = abi(BASE);
    assertEquals(base, abi(BASE.replace("value b = 0x10", "value b = 16")));
    assertEquals(
        base,
        abi(BASE.replace(" value a = 1\n value b = 0x10\n", " value b = 0x10\n value a = 1\n")));
    for (String[] change :
        new String[][] {
          {"value=5", "value=6"},
          {"const C", "const D"},
          {"value a = 1", "value a = 2"},
          {"value a", "value z"},
          {"bit y = 1", "bit y = 2"},
          {"bit y", "bit z"},
          {"flags F type=u32", "flags F type=u64"},
          {"enum E type=i32", "enum E type=i64"},
          {"flags F type=u32", "flags F type=u32 c_bitfields=true"},
          {"offset=0 flags=F", "offset=0"},
          {"offset=4 enum=E", "offset=4"},
        }) {
      String changed = BASE.replace(change[0], change[1]);
      assertNotEquals(BASE, changed, change[0]);
      assertNotEquals(base, abi(changed), change[0]);
    }
  }

  @Test
  void canonicalShape() throws Exception {
    String json =
        SeamJson.canonical(Dsl.parse(Files.readString(Path.of("examples/constants.seam"))));
    assertTrue(
        json.contains(
            "\"constants\":"
                + " [{\"name\":\"FLOOR\",\"type\":\"i64\",\"value\":\"-9223372036854775808\"},"
                + "{\"name\":\"LIMIT\",\"type\":\"u64\",\"value\":\"18446744073709551615\"},"
                + "{\"name\":\"MAGIC\",\"type\":\"u32\",\"value\":\"826364235\"}]"),
        json);
    assertTrue(
        json.contains(
            "{\"name\":\"Status\",\"type\":\"i32\",\"values\":[{\"name\":\"null_result\",\"value\":\"-1\"},"
                + "{\"name\":\"ok\",\"value\":\"0\"},{\"name\":\"internal_error\",\"value\":\"3\"},"
                + "{\"name\":\"syntax_error\",\"value\":\"16\"}]}"),
        json);
    assertTrue(
        json.contains(
            "\"flagSets\":"
                + " [{\"name\":\"Perms\",\"type\":\"u32\",\"bits\":[{\"name\":\"read\",\"bit\":0},"
                + "{\"name\":\"write\",\"bit\":1},{\"name\":\"sticky\",\"bit\":31}]},"
                + "{\"name\":\"Wide\",\"type\":\"u64\",\"cBitfields\":true,\"bits\":"),
        json);
    assertEquals(1, json.split("\"cBitfields\"", -1).length - 1);
    assertTrue(
        json.contains("{\"name\":\"perms\",\"type\":\"u32\",\"offset\":4,\"flags\":\"Perms\"}"));
    assertTrue(
        json.contains("{\"name\":\"status\",\"type\":\"i32\",\"offset\":0,\"enum\":\"Status\"}"));
    assertTrue(json.contains(",\"facts\":{},\"returnEnum\":\"Status\""));
    assertTrue(json.contains(",\"facts\":{},\"enum\":\"Level\"}"));
    assertTrue(json.indexOf("\"functions\":") < json.indexOf("\"constants\":"));
    assertTrue(
        json.indexOf("\"flagSets\":") < json.indexOf("\"resources\":")
            || !json.contains("\"resources\":"));
  }

  @Test
  void flagReferencesUseFlagsKeys() {
    String json =
        SeamJson.canonical(
            Dsl.parse(
                BASE
                    + "import g symbol=g return=u32 error=no_failure flags=F\n"
                    + " param p type=u32 flags=F\n"
                    + "end\n"
                    + "callback Cb return=u32 flags=F\n"
                    + "end\n"));
    assertEquals(2, json.split("\"returnFlags\":\"F\"", -1).length - 1);
    assertTrue(json.contains("\"flags\":\"F\"}"));
    assertFalse(json.contains("returnEnum"));
  }

  @Test
  void withoutDeclarationsJsonHasNoNewSections() {
    String json = SeamJson.canonical(Dsl.parse("module m target=x86_64-unknown-linux-gnu\n"));
    assertFalse(json.contains("constants"));
    assertFalse(json.contains("enums"));
    assertFalse(json.contains("flagSets"));
  }

  private static String header(String source) {
    return dev.elide.myna.Generator.generate(Dsl.parse(source), true).get("seam.h");
  }

  @Test
  void cHeaderRendersExtremes() throws Exception {
    String h = header(Files.readString(Path.of("examples/constants.seam")));
    for (String line :
        new String[] {
          "#define CONSTANTS_MAGIC UINT32_C(0x3141514b)",
          "#define CONSTANTS_LIMIT UINT64_C(0xffffffffffffffff)",
          "#define CONSTANTS_FLOOR (-INT64_C(9223372036854775807) - 1)",
          "#define CONSTANTS_STATUS_NULL_RESULT (INT32_C(-1))",
          "#define CONSTANTS_STATUS_SYNTAX_ERROR INT32_C(16)",
          "#define CONSTANTS_LEVEL_HIGH UINT8_C(255)",
          "#define CONSTANTS_PERMS_STICKY UINT32_C(0x80000000)",
          "#define CONSTANTS_PERMS_ALL UINT32_C(0x80000003)",
          "#define CONSTANTS_WIDE_LAST UINT64_C(0x8000000000000000)",
          "typedef int32_t constants_status_t;",
          "typedef uint8_t constants_level_t;",
          "static inline const char *constants_status_name(int32_t value) {",
          "    case CONSTANTS_STATUS_NULL_RESULT: return \"null_result\";",
          "    default: return NULL;",
          "constants_status_t consts_status(struct Record const * view);",
          "void consts_set_level(constants_level_t level, struct Session const * session);",
          "typedef constants_status_t (*Listener)(void * isolate);",
          "  constants_status_t status;",
          "  constants_perms_t perms;",
          "struct constants_wide_bits {\n"
              + "  uint64_t first : 1;\n"
              + "  uint64_t : 62;\n"
              + "  uint64_t last : 1;\n"
              + "};",
          "  union {\n"
              + "    constants_wide_t wide;\n"
              + "    struct constants_wide_bits wideBits;\n"
              + "  };",
          "SEAM_ASSERT(offsetof(struct Record, wide) == 8, \"field offset\");",
        }) assertTrue(h.contains(line), line);
    assertFalse(h.contains("constants_perms_bits"));
    assertFalse(h.contains("typedef enum"));
    assertFalse(h.contains("enum {"));
  }

  @Test
  void cHeaderSignedMasksAndMinimums() {
    String h =
        header(
            "module m target=x86_64-unknown-linux-gnu\n"
                + "flags F type=i32\n bit top = 31\n bit low = 0\nend\n"
                + "enum E type=i8\n value lo = -128\n value a = 1\nend\n");
    assertTrue(h.contains("#define M_F_TOP (-INT32_C(2147483647) - 1)"), h);
    assertTrue(h.contains("#define M_F_LOW INT32_C(0x1)"), h);
    assertTrue(h.contains("#define M_F_ALL (INT32_C(-2147483647))"), h);
    assertTrue(h.contains("#define M_E_LO (-INT8_C(127) - 1)"), h);
  }

  @Test
  void cHeaderWithoutDeclarationsHasNoConstantsSection() {
    assertFalse(header("module m target=x86_64-unknown-linux-gnu\n").contains("typedef"));
  }

  @Test
  void rustEmitsTransparentNewtypesAndLiterals() throws Exception {
    var m = Dsl.parse(Files.readString(Path.of("examples/constants.seam")));
    String rust = new dev.elide.myna.backend.RustFfi().generate(m);
    assertTrue(rust.contains("pub const FLOOR: i64 = -9223372036854775808;"));
    assertTrue(rust.contains("pub const LIMIT: u64 = 0xffffffffffffffff;"));
    assertTrue(
        rust.contains(
            "#[repr(transparent)]\n"
                + "#[derive(Clone, Copy, PartialEq, Eq, Hash, Debug)]\n"
                + "pub struct Status(pub i32);"));
    assertTrue(rust.contains("pub const STICKY: Self = Self(0x80000000);"));
    assertTrue(rust.contains("Self(self.0 * on as u64)"));
    assertTrue(rust.contains("pub fn status(view: *const Record) -> Status;"));
    assertTrue(rust.contains("pub status: Status,"));
    assertTrue(
        rust.contains("pub type SetLevelFn = unsafe extern \"C\" fn(Level, *const Session);"));
  }
}
