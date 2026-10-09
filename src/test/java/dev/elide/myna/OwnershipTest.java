package dev.elide.myna;

import static org.junit.jupiter.api.Assertions.*;

import dev.elide.myna.frontend.Dsl;
import dev.elide.myna.serialize.SeamJson;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
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
    assertTrue(rust.contains("unsafe impl Send for Ticket"));
    assertTrue(rust.contains("impl Drop for Ticket"));
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
    assertFalse(confined.get("seam.rs").contains("unsafe impl Send"));
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
                              + " error=no_failure nounwind=true@explicit_contract\n"
                              + "end",
                          "import ticketCreate symbol=ticket_create return=ptr<Ticket>"
                              + " error=no_failure nounwind=true@explicit_contract\n"
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
        "readonly=true@explicit_contract|readonly=true@explicit_contract"
            + " noalias=true@explicit_contract",
        "param count type=u64|param count type=u64\nparam extra type=ptr<u8>",
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

  @ParameterizedTest
  @ValueSource(
      strings = {
        "NativeBuffer",
        "NativeDestructor",
        "ResourceFactory",
        "java",
        "org",
        "core",
        "Option"
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
}
