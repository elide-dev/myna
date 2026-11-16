// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package dev.elide.myna;

import static org.junit.jupiter.api.Assertions.*;

import com.sun.source.tree.*;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreeScanner;
import dev.elide.myna.frontend.Dsl;
import dev.elide.myna.model.Names;
import dev.elide.myna.model.Seam;
import dev.elide.myna.model.Seam.*;
import dev.elide.myna.model.Seam.Module;
import dev.elide.myna.serialize.SeamJson;
import dev.elide.myna.validate.Ranges;
import dev.elide.myna.validate.Validator;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;
import javax.lang.model.element.Modifier;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
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
        java.util.List.of("BinaryOp", "Kind", "Level", "NodeTag", "Status", "UnaryOp"),
        m.enums().stream().map(EnumGroup::name).toList());
    var status = m.enums().get(4);
    assertEquals(Scalar.I32, status.type());
    assertEquals(
        java.util.List.of(
            new EnumValue("null_result", BigInteger.valueOf(-1)),
            new EnumValue("ok", BigInteger.ZERO),
            new EnumValue("internal_error", BigInteger.valueOf(3)),
            new EnumValue("syntax_error", BigInteger.valueOf(16))),
        status.values());
    assertEquals(
        java.util.List.of("Octet", "Packed", "Perms", "Wide", "WriteFlags"),
        m.flagSets().stream().map(FlagSet::name).toList());
    assertEquals(
        java.util.List.of(
            new FlagRange("kind", 8, 15, "Kind"), new FlagRange("depth", 16, 19, null)),
        m.flagSets().get(1).ranges());
    assertFalse(m.flagSets().get(2).cBitfields());
    assertEquals(
        java.util.List.of(
            new FlagBit("read", 0), new FlagBit("write", 1), new FlagBit("sticky", 31)),
        m.flagSets().get(2).bits());
    assertTrue(m.flagSets().get(2).ranges().isEmpty());
    assertTrue(m.flagSets().get(3).cBitfields());
    assertEquals(63, m.flagSets().get(3).bits().get(1).bit());

    var record = m.structs().stream().filter(s -> s.name().equals("Record")).findFirst().get();
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
            "enum Status type=i32\n value ok = 0\nend\nconst STATUS_ENUM type=i32 value=1\n"
                + IMP
                + " enum=Status\nend\n",
            "const STATUS_ENUM: Native Image accessor statusEnum collides with typed enum wrapper"
                + " of function status"),
        Arguments.of(
            "enum Status type=i32\n value ok = 0\nend\n"
                + IMP
                + " enum=Status\n"
                + " param x type=i32\n"
                + "end\n"
                + "import statusEnum symbol=t return=i32 error=no_failure\n"
                + " param x type=i32\n"
                + "end\n",
            "logical name collides with the typed enum wrapper of status: statusEnum"),
        Arguments.of(
            "flags F type=u8 c_bitfields=true\n bit raw = 0\nend\n"
                + "struct S size=2 align=1\n field addressOf type=u8 offset=0 flags=F\n"
                + " field raw type=u8 offset=1\nend\n",
            "struct S: accessor addressOfRaw of field addressOf bit raw collides with address"
                + " accessor of field raw"),
        Arguments.of(
            "flags F type=u32\n bit int = 0\nend\n",
            "flags F: bit int: predicate int is a reserved Java name"),
        Arguments.of(
            "flags F type=u32\n bit has_unknown = 0\nend\n",
            "flags F: bit has_unknown: predicate hasUnknown is a reserved Java name"),
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
        // Each const is also a @CConstant accessor method of the Native Image class.
        Arguments.of(
            "const CLASS type=u32 value=1\n",
            "const CLASS: Native Image accessor class is a reserved Java name"),
        Arguments.of(
            "const HASH_CODE type=u32 value=1\n",
            "const HASH_CODE: Native Image accessor hashCode is a reserved Java name"),
        Arguments.of(
            "const SEAM_HEADER_CHECK type=u32 value=1\n",
            "const SEAM_HEADER_CHECK: Native Image accessor seamHeaderCheck is a reserved Java"
                + " name"),
        Arguments.of(
            "import seamHeaderCheck symbol=s return=i32 error=no_failure\nend\n",
            "function seamHeaderCheck: name collides with the generated seam.h check of the Native"
                + " Image class"),
        Arguments.of(
            "const A_B type=u32 value=1\nconst A__B type=u32 value=2\n",
            "const A__B: Native Image accessor aB collides with const A_B"),
        Arguments.of(
            "const STATUS type=u32 value=1\n" + IMP + "\nend\n",
            "const STATUS: Native Image accessor status collides with function status"),
        Arguments.of(
            "const STATUS_LEAF_POINTER type=u32 value=1\n" + IMP + "\nend\n",
            "const STATUS_LEAF_POINTER: Native Image accessor statusLeafPointer collides with"
                + " function status"),
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
        // Callback names are C typedefs beside the generated names.
        Arguments.of(
            ST + "callback komodo_frontend_status_t return=void\nend\n",
            "C name komodo_frontend_status_t is generated by both callback"
                + " komodo_frontend_status_t and enum Status typedef"),
        Arguments.of(
            ST + "callback komodo_frontend_status_name return=void\nend\n",
            "C name komodo_frontend_status_name is generated by both callback"
                + " komodo_frontend_status_name and enum Status name function"),
        Arguments.of(
            ST + "callback KOMODO_FRONTEND_STATUS_OK return=void\nend\n",
            "C name KOMODO_FRONTEND_STATUS_OK is generated by both callback"
                + " KOMODO_FRONTEND_STATUS_OK and enum Status value ok"),
        Arguments.of(
            "flags F type=u8\n bit a = 0\nend\ncallback komodo_frontend_f_t return=void\nend\n",
            "C name komodo_frontend_f_t is generated by both callback komodo_frontend_f_t and"
                + " flags F typedef"),
        // Opaque and record names are struct tags, as a bits struct is.
        Arguments.of(
            "flags F type=u8 c_bitfields=true\n bit a = 0\nend\nopaque komodo_frontend_f_bits\n",
            "C name komodo_frontend_f_bits is generated by both opaque komodo_frontend_f_bits and"
                + " flags F bits struct"),
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
                + " a_b"),
        Arguments.of(
            "flags F type=u32 c_bitfields=true\n range kind = 0..3\nend\n"
                + "struct S size=8 align=4\n field f type=u32 offset=0 flags=F\n"
                + " field fKind type=u32 offset=4\nend\n",
            "struct S: accessor fKind of field f range kind collides with field fKind"),
        // Bit ranges.
        Arguments.of(
            "flags ClassFlags type=u32\n bit open = 0\n bit external = 9\n"
                + " range kind = 8..15\nend\n",
            "flags ClassFlags: range kind = 8..15 and bit external = 9 overlap"),
        Arguments.of(
            "flags F type=u32\n range a = 0..3\n range b = 3..5\nend\n",
            "flags F: ranges a = 0..3 and b = 3..5 overlap"),
        Arguments.of(
            "flags F type=u32\n range a = 8..32\nend\n",
            "flags F: range a = 8..32 is outside u32 (bits 0..31)"),
        Arguments.of(
            "flags F type=u8\n range a = 5..4\nend\n",
            "flags F: range a = 5..4 is empty (LOW > HIGH)"),
        Arguments.of(
            "flags F type=u32\n range a = 0..3 enum=Kind\nend\n",
            "flags F: range a = 0..3: undeclared enum: Kind"),
        Arguments.of(
            FL + "flags F type=u32\n range a = 0..3 enum=FunctionFlags\nend\n",
            "flags F: range a = 0..3: enum=FunctionFlags names a flag set"),
        Arguments.of(
            "enum Kind type=u8\n value a = 0\n value b = 16\nend\n"
                + "flags F type=u32\n range a = 0..3 enum=Kind\nend\n",
            "flags F: range a = 0..3: enum=Kind value b = 16 does not fit 4 bits"),
        Arguments.of(
            "enum Kind type=i8\n value a = -1\nend\n"
                + "flags F type=u32\n range a = 0..3 enum=Kind\nend\n",
            "flags F: range a = 0..3: enum=Kind value a = -1 does not fit 4 bits"),
        Arguments.of(
            "enum Kind type=i8\n value a = 0\nend\n"
                + "flags F type=u32\n range a = 0..7 enum=Kind\nend\n",
            "flags F: range a = 0..7: enum=Kind has carrier i8, too narrow for 8 bits"),
        Arguments.of(
            "flags F type=u32\n bit kind = 0\n range kind = 1..3\nend\n",
            "flags F: bit kind and range kind both use the name kind"),
        Arguments.of(
            "flags F type=u32\n bit kind_mask = 0\n range kind = 1..3\nend\n",
            "flags F: bit kind_mask and range kind both use the name kind_mask"),
        Arguments.of(
            "flags F type=u32\n range a_shift = 0..1\n range a = 2..3\nend\n",
            "flags F: range a_shift and range a both use the name a_shift"),
        Arguments.of(
            "flags F type=u32\n range none = 0..1\nend\n", "flags F: range name is reserved: none"),
        Arguments.of(
            "flags F type=u32\n range type = 0..1\nend\n", "invalid or reserved identifier: type"),
        Arguments.of(
            "flags F type=u32\n range contains = 0..1\nend\n",
            "flags F: range contains: Rust method contains collides with the flag method contains"),
        Arguments.of(
            "flags F type=u32\n range hash = 0..1\nend\n",
            "flags F: range hash: Rust method hash collides with the flag method hash"),
        Arguments.of(
            "flags F type=u32\n range has_unknown = 0..1\nend\n",
            "flags F: range has_unknown: Java method hasUnknown collides with hasUnknown"),
        Arguments.of(
            "flags F type=u32\n range a = 0..1\n range with_a = 2..3\nend\n",
            "flags F: range with_a: Java method withA collides with range a"),
        Arguments.of(
            "flags F type=u32\n range t = 0..1\nend\n",
            "C name komodo_frontend_f_t is generated by both flags F range t getter and flags F"
                + " typedef"),
        // Tagged fields.
        Arguments.of(
            TAGGED + " when tag=bogus enum=BinaryOp\nend\n",
            "field Node.operand when tag=bogus: enum NodeTag declares no value bogus"),
        Arguments.of(
            TAGGED + " when tag=binary enum=BinaryOp\n when tag=binary enum=UnaryOp\nend\n",
            "field Node.operand: two when lines for tag=binary"),
        Arguments.of(
            TAGGED + " when kind=binary enum=BinaryOp\nend\n",
            "field Node.operand when kind=binary: struct Node has no field kind"),
        Arguments.of(
            TAGGED + " when raw=binary enum=BinaryOp\nend\n",
            "field Node.operand when raw=binary: tag field raw carries no enum="),
        Arguments.of(
            TAGGED + " when operand=binary enum=BinaryOp\nend\n",
            "field Node.operand when operand=binary: tag field operand carries no enum="),
        Arguments.of(
            TAGGED + " when tag=binary enum=BinaryOp\n when zeta=unary enum=UnaryOp\nend\n",
            "field Node.operand: when lines name tag fields tag and zeta; use one"),
        Arguments.of(
            TAGGED + " when tag=binary enum=Narrow\nend\n",
            "field Node.operand when tag=binary: enum=Narrow has carrier u8, but the field's type"
                + " is u64"),
        Arguments.of(
            TAGGED + " when tag=binary enum=Missing\nend\n",
            "field Node.operand when tag=binary: undeclared enum: Missing"),
        Arguments.of(
            TAGGED + " when tag=binary flags=BinaryOp\nend\n",
            "field Node.operand when tag=binary: flags=BinaryOp names an enum; use enum=BinaryOp"),
        Arguments.of(
            TAGGED.replace("offset=8\n", "offset=8 enum=BinaryOp\n")
                + " when tag=binary enum=BinaryOp\nend\n",
            "field Node.operand: when lines and enum=/flags= are mutually exclusive"),
        Arguments.of(
            TAGGED
                + " when tag=binary enum=BinaryOp\nend\n"
                + "import operandAsBinaryOp symbol=o return=void error=no_failure\nend\n",
            "field Node.operand enum BinaryOp: Native Image accessor operandAsBinaryOp collides"
                + " with function operandAsBinaryOp"),
        Arguments.of(
            "enum AsB type=u64\n value x = 0\nend\nenum B type=u64\n value x = 0\nend\n"
                + "enum T type=u8\n value a = 0\n value b = 1\nend\n"
                + "struct S size=24 align=8\n field t type=u8 offset=0 enum=T\n"
                + " field x type=u64 offset=8\n  when t=a enum=AsB\n"
                + " field x_as type=u64 offset=16\n  when t=b enum=B\nend\n",
            "struct S: Rust accessor x_as_as_b of field x_as group B collides with the accessor"
                + " of field x group AsB"));
  }

  static final String TAGGED =
      "enum NodeTag type=u32\n value binary = 1\n value unary = 2\nend\n"
          + "enum BinaryOp type=u64\n value add = 0\nend\n"
          + "enum UnaryOp type=u64\n value negate = 0\nend\n"
          + "enum Narrow type=u8\n value a = 0\nend\n"
          + "struct Node size=24 align=8\n field tag type=u32 offset=0 enum=NodeTag\n"
          + " field raw type=u32 offset=4\n field operand type=u64 offset=8\n";

  @Test
  void whenLinesParseAndSerialize() {
    var m =
        Dsl.parse(
            HEAD
                + TAGGED
                + "  when tag=binary enum=BinaryOp # a comment\n\n"
                + " when tag=unary enum=UnaryOp\n"
                + " field after type=u64 offset=16\nend\n");
    Validator.validate(m, true);
    var fields = m.structs().get(0).fields();
    assertEquals(
        List.of(
            new FieldCase("tag", "binary", new GroupRef(GroupKind.ENUM, "BinaryOp")),
            new FieldCase("tag", "unary", new GroupRef(GroupKind.ENUM, "UnaryOp"))),
        fields.get(2).cases());
    assertNull(fields.get(2).group());
    assertTrue(fields.get(3).cases().isEmpty());
    String json = SeamJson.canonical(m);
    assertTrue(
        json.contains(
            "{\"name\":\"operand\",\"type\":\"u64\",\"offset\":8,\"cases\":[{\"tag\":\"tag\","
                + "\"value\":\"binary\",\"enum\":\"BinaryOp\"},{\"tag\":\"tag\",\"value\":\"unary\","
                + "\"enum\":\"UnaryOp\"}]}"),
        json);
    assertEquals(1, json.split("\"cases\"", -1).length - 1);
    // Cases are canonical: by the tag enum's value, whatever the order of the when lines.
    var reordered =
        Dsl.parse(
            HEAD
                + TAGGED
                + " when tag=unary enum=UnaryOp\n"
                + " when tag=binary enum=BinaryOp\n"
                + " field after type=u64 offset=16\nend\n");
    assertEquals(fields.get(2).cases(), reordered.structs().get(0).fields().get(2).cases());
    assertEquals(SeamJson.fingerprint(m), SeamJson.fingerprint(reordered));
    assertNotEquals(
        SeamJson.fingerprint(m),
        SeamJson.fingerprint(Dsl.parse(HEAD + TAGGED + " field after type=u64 offset=16\nend\n")));
    assertEquals(
        "line 3: when must directly follow a field or another when",
        parseError("struct S size=8 align=8\n when t=a enum=E\nend\n"));
    assertEquals(
        "line 2: when must directly follow a field or another when",
        parseError("when t=a enum=E\n"));
    assertEquals(
        "line 4: expected: when FIELD=VALUE enum=ENUM|flags=FLAGS",
        parseError("struct S size=8 align=8\n field f type=u64 offset=0\n when t enum=E\nend\n"));
    assertEquals(
        "line 4: when requires enum= or flags=",
        parseError("struct S size=8 align=8\n field f type=u64 offset=0\n when t=a\nend\n"));
    assertEquals(
        "line 4: enum= and flags= are mutually exclusive",
        parseError(
            "struct S size=8 align=8\n"
                + " field f type=u64 offset=0\n"
                + " when t=a enum=E flags=F\n"
                + "end\n"));
  }

  @Test
  void rangesParse() {
    var set =
        Dsl.parse(HEAD + "flags F type=u32\n range b = 4..7\n range a = 0..3 enum=E\nend\n")
            .flagSets()
            .get(0);
    assertEquals(
        List.of(new FlagRange("a", 0, 3, "E"), new FlagRange("b", 4, 7, null)), set.ranges());
    assertEquals(BigInteger.valueOf(0xf0), set.ranges().get(1).mask());
    assertEquals(BigInteger.valueOf(0xff), set.all());
    assertEquals(
        "line 3: expected: range NAME = LOW..HIGH [enum=ENUM]",
        parseError("flags F type=u8\n range a 0..3\nend\n"));
    assertEquals(
        "line 3: invalid bit range: 0-3", parseError("flags F type=u8\n range a = 0-3\nend\n"));
    assertEquals(
        "line 3: unknown options: [flags]",
        parseError("flags F type=u8\n range a = 0..3 flags=G\nend\n"));
    assertEquals(
        "line 3: range requires a flags block",
        parseError("enum E type=i32\n range a = 0..3\nend\n"));
    // A set of ranges alone is a flag set.
    Validator.validate(Dsl.parse(HEAD + "flags F type=u8\n range a = 0..7\nend\n"), true);
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
            "type collides with generated Truffle cell for status"),
        Arguments.of(
            "const Status type=i32 value=1\n" + ST,
            "enum Status: name collides with const Status in Truffle constants()"),
        Arguments.of(
            "const FunctionFlags type=u32 value=1\n" + FL,
            "flags FunctionFlags: name collides with const FunctionFlags in Truffle constants()"));
  }

  @Test
  void constAndGroupNamesShareANamespaceOnlyWithCells() {
    // Without Truffle cells nothing puts consts and groups in one namespace.
    Validator.validate(Dsl.parse(HEAD + "const Status type=i32 value=1\n" + ST), true);
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

  /** The DSL cannot spell a negative bit, but a module built in code can. */
  @Test
  void negativeFlagBitIsRejected() {
    var m = Dsl.parse(HEAD);
    var bad =
        new Module(
            m.name(),
            m.abiVersion(),
            m.targetTriple(),
            m.javaPackage(),
            m.javaClass(),
            m.opaqueTypes(),
            m.structs(),
            m.callbacks(),
            m.functions(),
            m.resources(),
            m.borrows(),
            m.nativeConfig(),
            m.truffleConfig(),
            m.constants(),
            m.enums(),
            java.util.List.of(
                new FlagSet("Perms", Scalar.U32, java.util.List.of(new FlagBit("bad", -1)))));
    assertEquals(
        "flags Perms: bit bad = -1 is outside u32 (bits 0..31)",
        assertThrows(IllegalArgumentException.class, () -> Validator.validate(bad, true))
            .getMessage());
  }

  /** The DSL cannot spell a negative range start either; a module built in code can. */
  @Test
  void negativeRangeStartIsRejected() {
    var m = Dsl.parse(HEAD);
    var bad =
        new Module(
            m.name(),
            m.abiVersion(),
            m.targetTriple(),
            m.javaPackage(),
            m.javaClass(),
            m.opaqueTypes(),
            m.structs(),
            m.callbacks(),
            m.functions(),
            m.resources(),
            m.borrows(),
            m.nativeConfig(),
            m.truffleConfig(),
            m.constants(),
            m.enums(),
            java.util.List.of(
                new FlagSet(
                    "Perms",
                    Scalar.U32,
                    java.util.List.of(),
                    false,
                    java.util.List.of(new FlagRange("code", -1, 3, null)))));
    assertEquals(
        "flags Perms: range code = -1..3 is outside u32 (bits 0..31)",
        assertThrows(IllegalArgumentException.class, () -> Validator.validate(bad, true))
            .getMessage());
  }

  /** A counted pointer's Rust view and a tagged field's case accessor share the record's impl. */
  @Test
  void countedViewCollidingWithCaseAccessorIsRejected() {
    assertEquals(
        "struct Node: Rust accessor operand_as_binary_op of field operand group BinaryOp collides"
            + " with the accessor of counted field operand_as_binary_op",
        invalid(
            HEAD,
            "enum NodeTag type=u32\n value binary = 0\nend\n"
                + "enum BinaryOp type=u32\n value add = 0\nend\n"
                + "struct Node size=24 align=8\n"
                + " field tag type=u32 offset=0 enum=NodeTag\n"
                + " field operand type=u32 offset=4\n"
                + "  when tag=binary enum=BinaryOp\n"
                + " field operand_as_binary_op type=ptr<u8> offset=8 count=n\n"
                + " field n type=u64 offset=16\n"
                + "end\n"));
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
                + " [{\"name\":\"Octet\",\"type\":\"u8\",\"bits\":[],\"ranges\":[{\"name\":\"code\",\"low\":0,\"high\":7}]},{\"name\":\"Packed\",\"type\":\"u32\",\"cBitfields\":true,\"bits\":"
                + "[{\"name\":\"open\",\"bit\":0},{\"name\":\"external\",\"bit\":2}],\"ranges\":"
                + "[{\"name\":\"kind\",\"low\":8,\"high\":15,\"enum\":\"Kind\"},"
                + "{\"name\":\"depth\",\"low\":16,\"high\":19}]},"
                + "{\"name\":\"Perms\",\"type\":\"u32\",\"bits\":[{\"name\":\"read\",\"bit\":0},"
                + "{\"name\":\"write\",\"bit\":1},{\"name\":\"sticky\",\"bit\":31}]},"
                + "{\"name\":\"Wide\",\"type\":\"u64\",\"cBitfields\":true,\"bits\":"),
        json);
    assertEquals(2, json.split("\"cBitfields\"", -1).length - 1);
    assertEquals(2, json.split("\"ranges\"", -1).length - 1);
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

  @Test
  void ownershipWrappersQualifyEnumAndFlagReturns() throws Exception {
    String source = Files.readString(Path.of("examples/constants_ownership.seam"));
    var output = Generator.generate(Dsl.parse(source), true);
    String rust = output.get("seam.rs");
    assertTrue(rust.contains("pub fn check(bytes: &[u8], mode: super::Status) -> super::Status {"));
    assertTrue(
        rust.contains("pub fn permissions(bytes: &[u8], wanted: super::Perms) -> super::Perms {"));
    for (var file : output.entrySet())
      assertEquals(
          Files.readString(Path.of("tests/golden/constants-ownership", file.getKey())),
          file.getValue(),
          file.getKey());
  }

  /**
   * {@code <Class>Constants} holds only JLS compile-time constants: every field initializer is a
   * literal, a negated literal or a cast of one; no imports, no static initializer, no nested enum.
   */
  @Test
  void javaConstantsAreLiterals() throws Exception {
    for (String example : List.of("examples/constants.seam", "examples/constants_ownership.seam")) {
      var module = Dsl.parse(Files.readString(Path.of(example)));
      String source = Generator.generate(module, true).get(module.constantsClass() + ".java");
      assertNotNull(source, example);
      var compiler = ToolProvider.getSystemJavaCompiler();
      var task =
          (JavacTask)
              compiler.getTask(
                  null, null, null, List.of("-proc:none"), null, List.of(source(module, source)));
      var unit = task.parse().iterator().next();
      assertEquals(List.of(), unit.getImports(), example);
      var fields = new ArrayList<String>();
      new TreeScanner<Void, Void>() {
        @Override
        public Void visitClass(ClassTree tree, Void unused) {
          assertNotEquals(Tree.Kind.ENUM, tree.getKind(), tree.getSimpleName().toString());
          for (Tree member : tree.getMembers())
            assertNotEquals(Tree.Kind.BLOCK, member.getKind(), "initializer block in " + example);
          return super.visitClass(tree, unused);
        }

        @Override
        public Void visitVariable(VariableTree tree, Void unused) {
          if (tree.getInitializer() == null) return null; // a parameter
          var mods = tree.getModifiers().getFlags();
          assertTrue(mods.containsAll(Set.of(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)));
          assertEquals(Tree.Kind.PRIMITIVE_TYPE, tree.getType().getKind(), tree.toString());
          ExpressionTree init = tree.getInitializer();
          if (init instanceof TypeCastTree cast) init = cast.getExpression();
          if (init instanceof UnaryTree minus && minus.getKind() == Tree.Kind.UNARY_MINUS)
            init = minus.getExpression();
          assertInstanceOf(LiteralTree.class, init, tree.toString());
          fields.add(tree.getName().toString());
          return null;
        }
      }.scan(unit, null);
      assertFalse(fields.isEmpty(), example);
    }
  }

  /** A HotSpot consumer switches over every enum constant and tests every mask predicate. */
  @Test
  void javaConstantsCompileAsCaseLabelsAndRunOnHotSpot() throws Exception {
    var module = Dsl.parse(Files.readString(Path.of("examples/constants.seam")));
    String constants = Generator.generate(module, true).get(module.constantsClass() + ".java");
    String consumer =
        """
        package fixture.constants;
        import static fixture.constants.ConstsConstants.*;
        public final class Consumer {
          static String status(int value) {
            switch (value) {
              case Status.NULL_RESULT: return "null_result";
              case Status.OK: return "ok";
              case Status.SYNTAX_ERROR: return "syntax_error";
              case Status.INTERNAL_ERROR: return "internal_error";
              default: return "?" + value;
            }
          }
          static String level(byte value) {
            return switch (value) { case Level.LOW -> "low"; case Level.HIGH -> "high"; default -> "?"; };
          }
          public static String run() {
            StringBuilder out = new StringBuilder();
            for (int v : new int[] {-1, 0, 16, 3, 7}) out.append(status(v)).append(Status.name(v)).append(' ');
            out.append(level((byte) 0xff)).append(Level.name((byte) 0)).append(' ');
            int perms = Perms.READ | Perms.STICKY;
            out.append(Perms.read(perms)).append(Perms.write(perms)).append(Perms.sticky(perms))
                .append(Perms.hasUnknown(perms)).append(Perms.hasUnknown(perms | 0x4)).append(' ');
            out.append(Wide.last(Wide.LAST)).append(Wide.first(Wide.LAST)).append(Wide.hasUnknown(2L))
                .append(' ').append(MAGIC).append(' ').append(LIMIT).append(' ').append(FLOOR);
            int packed = Packed.withDepth(Packed.withKind(Packed.OPEN, Kind.BOXED), 9);
            out.append(' ').append(Integer.toHexString(packed)).append(' ').append(Packed.kind(packed))
                .append(Packed.depth(packed)).append(Kind.name(Packed.kind(packed)));
            packed = Packed.withKind(Packed.withDepth(packed, 0x13), Kind.LAST);
            out.append(' ').append(Integer.toHexString(packed)).append(Kind.name(Packed.kind(packed)))
                .append(Packed.hasUnknown(packed)).append(Packed.hasUnknown(packed | 0x2))
                .append(Packed.hasUnknown(packed | 0x100000));
            byte octet = Octet.withCode((byte) 0x12, (byte) 0xa5);
            out.append(' ').append(octet).append(Octet.code(octet)).append(Octet.hasUnknown(octet));
            return out.toString();
          }
        }
        """;
    assertEquals(
        "null_resultnull_result okok syntax_errorsyntax_error internal_errorinternal_error"
            + " ?7null highlow truefalsetruefalsetrue truefalsetrue 826364235 -1"
            + " -9223372036854775808 90501 59boxed 3ff01lastfalsetruetrue -91-91false",
        runOnHotSpot(module, constants, consumer));
  }

  /**
   * Range edges the fixture does not reach: a {@code u64} range above bit 32 written from a
   * narrower enum (the setter widens before it shifts), a signed carrier's range over its sign bit
   * (a negative mask in every backend), and a {@code u8} range reaching bit 7 beside a bit.
   */
  @Test
  void rangeEdgesRunOnHotSpot() throws Exception {
    var module =
        Dsl.parse(
            "module edge target=x86_64-unknown-linux-gnu java_package=fixture.edge"
                + " java_class=Edge truffle_cells=true\n"
                + "enum Small type=u8\n value a = 0\n value b = 200\nend\n"
                + "flags Big type=u64\n range hi = 40..47 enum=Small\n range mid = 4..9\nend\n"
                + "flags Neg type=i32\n bit low = 0\n range top = 24..31 enum=Small\nend\n"
                + "flags Nibble type=u8\n bit low = 0\n range high = 4..7\nend\n");
    Validator.validate(module, true);
    var out = Generator.generate(module, true);
    String constants = out.get("EdgeConstants.java");
    assertTrue(
        constants.contains(
            "public static long withHi(long flags, byte value) { return (flags & ~HI_MASK) |"
                + " (((long) value << HI_SHIFT) & HI_MASK); }"),
        constants);
    assertTrue(constants.contains("public static final int TOP_MASK = 0xff000000;"), constants);
    assertTrue(
        out.get("seam.h").contains("#define EDGE_NEG_TOP_MASK (INT32_C(-16777216))"),
        out.get("seam.h"));
    assertTrue(out.get("seam.rs").contains("pub const TOP_MASK: i32 = -16777216;"));
    assertTrue(
        out.get("EdgeTruffle.java").contains("new String[] {\"low\", \"top_shift\", \"top_mask\"}"),
        out.get("EdgeTruffle.java"));
    assertTrue(out.get("EdgeTruffle.java").contains("-16777216"), out.get("EdgeTruffle.java"));
    String consumer =
        """
        package fixture.edge;
        import static fixture.edge.EdgeConstants.*;
        public final class Consumer {
          public static String run() {
            StringBuilder out = new StringBuilder();
            long big = Big.withMid(Big.withHi(-1L, Small.A), 0x3c5);
            out.append(Long.toHexString(big)).append(' ');
            big = Big.withHi(0, Small.B);
            out.append(Long.toHexString(big)).append(Big.hi(big) == Small.B).append(Big.mid(big)).append(' ');
            int neg = Neg.withTop(Neg.LOW, Small.B);
            out.append(neg).append(Neg.top(neg) == Small.B).append(Neg.hasUnknown(neg))
                .append(Neg.hasUnknown(neg | 2)).append(Neg.top(Neg.withTop(neg, Small.A))).append(' ');
            byte nibble = Nibble.withHigh((byte) 1, (byte) 0xff);
            out.append(nibble).append(Nibble.high(nibble)).append(Nibble.hasUnknown(nibble))
                .append(Nibble.hasUnknown((byte) (nibble | 2)));
            return out.toString();
          }
        }
        """;
    assertEquals(
        "ffff00fffffffc5f c80000000000true0 -939524095truefalsetrue0 -1515falsetrue",
        runOnHotSpot(module, constants, consumer));
  }

  /**
   * A full-width range into a wider enum carrier reads the unsigned number the setter stored, in
   * Java (long masking, no sign extension) and Rust (through the same-width unsigned type).
   */
  @Test
  void fullWidthRangesIntoWiderEnums() throws Exception {
    var module =
        Dsl.parse(
            "module wide target=x86_64-unknown-linux-gnu java_package=fixture.wide"
                + " java_class=Wide\n"
                + "enum Huge type=u64\n value a = 0\n value last = 4294967295\nend\n"
                + "enum Half type=i16\n value a = 0\n value last = 255\nend\n"
                + "flags Word type=u32\n range code = 0..31 enum=Huge\nend\n"
                + "flags Octet type=i8\n range code = 0..7 enum=Half\nend\n");
    Validator.validate(module, true);
    var out = Generator.generate(module, true);
    assertTrue(out.get("seam.rs").contains("Half((self.0 as u8) as i16)"), out.get("seam.rs"));
    String consumer =
        """
        package fixture.wide;
        import static fixture.wide.WideConstants.*;
        public final class Consumer {
          public static String run() {
            int word = Word.withCode(0, Huge.LAST);
            byte b = Octet.withCode((byte) 0, Half.LAST);
            return Word.code(word) + " " + (Word.code(word) == Huge.LAST) + " " + Octet.code(b)
                + " " + (Octet.code(b) == Half.LAST);
          }
        }
        """;
    assertEquals(
        "4294967295 true 255 true", runOnHotSpot(module, out.get("WideConstants.java"), consumer));
  }

  /**
   * Compiles {@code <Class>Constants} and a {@code Consumer} in the module's package with every
   * warning fatal, and returns what {@code Consumer.run()} returns on HotSpot.
   */
  static Object runOnHotSpot(Module module, String constants, String consumer) throws Exception {
    String pkg = module.javaPackageName();
    Path out = Files.createTempDirectory("myna-constants");
    try {
      var compiler = ToolProvider.getSystemJavaCompiler();
      var diagnostics = new DiagnosticCollector<JavaFileObject>();
      boolean ok =
          compiler
              .getTask(
                  null,
                  null,
                  diagnostics,
                  List.of("-proc:none", "-Werror", "-Xlint:all", "-d", out.toString()),
                  null,
                  List.of(
                      source(module, constants),
                      new SimpleJavaFileObject(
                          URI.create("string:///" + pkg.replace('.', '/') + "/Consumer.java"),
                          JavaFileObject.Kind.SOURCE) {
                        @Override
                        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                          return consumer;
                        }
                      }))
              .call();
      assertTrue(ok, diagnostics.getDiagnostics().toString());
      try (var loader = new URLClassLoader(new URL[] {out.toUri().toURL()}, null)) {
        return loader.loadClass(pkg + ".Consumer").getMethod("run").invoke(null);
      }
    } finally {
      try (var walk = Files.walk(out)) {
        walk.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
      }
    }
  }

  private static JavaFileObject source(Module module, String text) {
    return new SimpleJavaFileObject(
        URI.create(
            "string:///"
                + module.javaPackageName().replace('.', '/')
                + "/"
                + module.constantsClass()
                + ".java"),
        JavaFileObject.Kind.SOURCE) {
      @Override
      public CharSequence getCharContent(boolean ignoreEncodingErrors) {
        return text;
      }
    };
  }

  /** The Native Image class's typed forms beside the raw bindings, and opt-in @CBitfield. */
  @Test
  void nativeImageEnumsAndTypedWrappers() throws Exception {
    var module = Dsl.parse(Files.readString(Path.of("examples/constants.seam")));
    String java = Generator.generate(module, true).get("Consts.java");
    assertTrue(java.contains("    @CEnum(\"constants_status_t\")\n    public enum Status {\n"));
    assertTrue(java.contains("@CEnumConstant(\"CONSTANTS_STATUS_NULL_RESULT\") NULL_RESULT(-1),"));
    assertTrue(java.contains("@CEnumConstant(\"CONSTANTS_LEVEL_HIGH\") HIGH((byte) 255);"));
    assertTrue(java.contains("@CEnumValue public native int cValue();"));
    assertTrue(java.contains("@CEnumLookup public static native Status lookup(int value);"));
    // The plain name stays the raw leaf; the typed form calls it.
    assertTrue(java.contains("    public static int status(Record view) {"));
    assertTrue(
        java.contains(
            "    public static Status statusEnum(Record view) {\n"
                + "        return Status.of(status(view));\n"));
    assertTrue(
        java.contains(
            "    public static void setLevel(Level level, Session session) {\n"
                + "        setLevel(level.value, session);\n"));
    assertTrue(java.contains("@CBitfield(\"wideBits.last\") boolean wideLast();"));
    assertTrue(java.contains("@CBitfield(\"wideBits.last\") void wideLast(boolean value);"));
    assertTrue(
        java.contains(
            "        /** flags Perms: test with ConstsConstants.Perms.read(perms()) */\n"
                + "        @CField(\"perms\") int perms();\n"));
    // Not opted in: Perms keeps the plain field only.
    assertFalse(java.contains("permsRead"));
    // Exports and callbacks keep raw carriers.
    assertTrue(java.contains("        int invoke(IsolateThread isolate);"));
    // Not listed for build-time initialization.
    for (var output : Generator.generate(module, true).entrySet())
      if (output.getKey().endsWith(".properties"))
        assertFalse(output.getValue().contains("ConstsConstants"), output.getKey());
  }

  @Test
  void javaLiteralsKeepRawBits() throws Exception {
    String source =
        HEAD
            + "const A type=i8 value=-128\nconst B type=u16 value=65535\n"
            + "const C type=i32 value=-2147483648\nconst D type=u32 value=4294967295\n"
            + "enum E type=u32\n value big = 4294967295\n value small = 1\nend\n"
            + "enum L type=i64\n value min = -9223372036854775808\n value one = 1\nend\n";
    var module = Dsl.parse(source);
    var out = Generator.generate(module, true);
    String constants = out.get(module.constantsClass() + ".java");
    assertTrue(constants.contains("public static final byte A = (byte) -128;"), constants);
    assertTrue(constants.contains("public static final short B = (short) 0xffff;"), constants);
    assertTrue(constants.contains("public static final int C = -2147483648;"), constants);
    assertTrue(constants.contains("public static final int D = 0xffffffff;"), constants);
    assertTrue(constants.contains("public static final int BIG = 0xffffffff;"), constants);
    // Java has no switch over long.
    assertTrue(constants.contains("if (value == MIN) return \"min\";"), constants);
    String java = out.get(module.nativeClass() + ".java");
    assertTrue(java.contains("if (value == -9223372036854775808L) return MIN;"), java);
    assertTrue(java.contains("case 0xffffffff: return BIG;"), java);
  }

  private static final String ENUM_ONLY =
      "module m target=x86_64-unknown-linux-gnu java_package=fx java_class=Api truffle_cells=true\n"
          + "enum E type=i8\n value a = 1\nend\n";
  private static final String FLAGS_ONLY =
      "module m target=x86_64-unknown-linux-gnu java_package=fx java_class=Api truffle_cells=true\n"
          + "flags F type=u16\n bit a = 0\n bit b = 15\nend\n";
  private static final String CONST_ONLY =
      "module m target=x86_64-unknown-linux-gnu java_package=fx java_class=Api truffle_cells=true\n"
          + "const NARROW type=u8 value=255\nconst SMALL type=i16 value=-2\n";

  /** Enums alone, or flag sets alone, are declarations enough for every constants surface. */
  @Test
  void anyOneKindOfDeclarationBringsTheConstantsSurfaces() {
    for (String source : List.of(ENUM_ONLY, FLAGS_ONLY, CONST_ONLY)) {
      var out = Generator.generate(Dsl.parse(source), true);
      assertTrue(
          out.get("seam.h").contains("typedef") || out.get("seam.h").contains("#define M_NARROW"),
          source);
      assertNotNull(out.get("ApiConstants.java"), source);
      assertTrue(out.get("ApiTruffle.java").contains("NativeConstants"), source);
    }
    // Without any, none of them is emitted.
    var none =
        Generator.generate(
            Dsl.parse("module m target=x86_64-unknown-linux-gnu truffle_cells=true\n"), true);
    assertFalse(none.get("MynaTruffle.java").contains("NativeConstants"));
  }

  /** Name lookups switch on the signed Java type of the carrier's width, as raw bits. */
  @Test
  void truffleEnumNameLookupsUseRawBitLiteralsForNarrowCarriers() {
    String source =
        "module m target=x86_64-unknown-linux-gnu java_package=fx java_class=Api"
            + " truffle_cells=true\n"
            + "enum Small type=u8\n"
            + " value top = 255\n"
            + " value one = 1\n"
            + "end\n"
            + "enum Mid type=u16\n"
            + " value top = 65535\n"
            + " value two = 2\n"
            + "end\n";
    String java = Generator.generate(Dsl.parse(source), true).get("ApiTruffle.java");
    assertEquals(2, java.split("case -1: return TOP;", -1).length - 1, java);
    assertTrue(java.contains("case 1: return ONE;"), java);
    assertTrue(java.contains("case 2: return TWO;"), java);
  }

  /** Size-width carriers mask as 64 bits on every backend. */
  @Test
  void sizeCarriersAreSixtyFourBitsWide() {
    for (String type : List.of("usize", "isize")) {
      String source =
          "module m target=x86_64-unknown-linux-gnu\nflags F type="
              + type
              + "\n bit lo = 0\n bit hi = 63\nend\n";
      var out = Generator.generate(Dsl.parse(source), true);
      String signed =
          type.equals("isize")
              ? "(-INT64_C(9223372036854775807) - 1)"
              : "UINT64_C(0x8000000000000000)";
      assertTrue(out.get("seam.h").contains("#define M_F_HI " + signed), out.get("seam.h"));
      String rust =
          type.equals("isize") ? "Self(-9223372036854775808)" : "Self(0x8000000000000000)";
      assertTrue(
          out.get("seam.rs").contains("pub const HI: " + rust.replace("Self(", "Self = Self(")),
          out.get("seam.rs"));
    }
  }

  @Test
  void lowerCamelDropsLeadingSeparators() {
    assertEquals("analysisMagic", Names.lowerCamel("ANALYSIS_MAGIC"));
    assertEquals("magic", Names.lowerCamel("_MAGIC"));
    assertEquals("aB", Names.lowerCamel("A__B"));
  }

  @Test
  void constDeclarationsNeedAnIntegerCarrierOutsideBlocks() {
    var bad =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                Dsl.parse(
                    "module m target=x86_64-unknown-linux-gnu\nconst X type=ptr<i32> value=1\n"));
    assertTrue(bad.getMessage().contains("carrier must be an integer scalar"), bad.getMessage());
    var inBlock =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                Dsl.parse(
                    "module m target=x86_64-unknown-linux-gnu\n"
                        + "import f return=void error=no_failure\n"
                        + "const X type=u8 value=1\n"
                        + "end\n"));
    assertTrue(inBlock.getMessage().contains("const"), inBlock.getMessage());
    assertThrows(
        IllegalArgumentException.class,
        () -> Dsl.parse("module m target=x86_64-unknown-linux-gnu\nconst\n"));
  }

  /** A cell named constants clashes with the generated accessor for any declaration kind. */
  @Test
  void constantsCellClashesWithEveryKindOfDeclaration() {
    for (String source : List.of(ENUM_ONLY, FLAGS_ONLY, CONST_ONLY)) {
      var clash =
          assertThrows(
              IllegalArgumentException.class,
              () ->
                  Generator.generate(
                      Dsl.parse(source + "import constants return=i32 error=no_failure\nend\n"),
                      true),
              source);
      assertTrue(clash.getMessage().contains("collides with the generated constants()"));
    }
    // A cell opted out of Truffle does not clash.
    Generator.generate(
        Dsl.parse(
            ENUM_ONLY
                + "import constants return=i32 error=no_failure"
                + " truffle_cell=false@explicit_contract\n"
                + "end\n"),
        true);
  }

  /** Header pieces split on UTF-8 byte widths of every size and flush before an oversized line. */
  @Test
  void headerPiecesCountEveryUtf8Width() {
    String text = "ab\n" + "\u00e9".repeat(10) + "\n" + "\u20ac".repeat(10) + "\n" + "x";
    var pieces = dev.elide.myna.backend.NativeImageDirectives.pieces(text, 8);
    assertEquals(text, String.join("", pieces));
    for (String piece : pieces)
      assertTrue(piece.getBytes(java.nio.charset.StandardCharsets.UTF_8).length <= 8, piece);
    assertTrue(pieces.size() >= 4, pieces.toString());
    assertEquals("ab\n", pieces.get(0));
  }
}
