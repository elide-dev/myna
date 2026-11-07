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
