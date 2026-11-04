package dev.elide.myna;

import static org.junit.jupiter.api.Assertions.*;
import dev.elide.myna.frontend.Dsl;
import dev.elide.myna.model.Seam;
import dev.elide.myna.serialize.SeamJson;
import java.util.List;
import org.junit.jupiter.api.Test;

class CompatibilityTest {
  @Test
  void legacyModelConstructorsPreserveCanonicalIdentity() {
    var parsed = Dsl.parse("module sample target=aarch64-apple-darwin\n");
    var legacy = new Seam.Module("sample", 1, "aarch64-apple-darwin", null, null,
        List.of(), List.of(), List.of());
    var callbacks = new Seam.Module("sample", 1, "aarch64-apple-darwin", null, null,
        List.of(), List.of(), List.of(), List.of());
    var ownership = new Seam.Module("sample", 1, "aarch64-apple-darwin", null, null,
        List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
    assertEquals(SeamJson.canonical(parsed), SeamJson.canonical(legacy));
    assertEquals(parsed, callbacks);
    assertEquals(parsed, ownership);
    assertEquals(new Seam.Signature("callback", Seam.Scalar.I64, List.of(), null),
        new Seam.Signature("callback", Seam.Scalar.I64, List.of()));
  }

  @Test
  void narrowAndFloatingPointersAndBooleanFieldHaveTypedCarriers() {
    var output = Generator.generate(Dsl.parse("""
        module carriers target=aarch64-apple-darwin
        struct Flag size=1 align=1
          field value type=bool offset=0
        end
        import inspect return=void error=no_failure
          param shortValue type=ptr<i16> nullable=false ownership=borrowed access=read
          param unsignedValue type=ptr<u16> nullable=false ownership=borrowed access=read
          param floatValue type=ptr<f32> nullable=false ownership=borrowed access=read
          param doubleValue type=ptr<f64> nullable=false ownership=borrowed access=read
        end
        """), true);
    var java = output.get("MynaNative.java");
    assertTrue(java.contains("CShortPointer shortValue"));
    assertTrue(java.contains("CShortPointer unsignedValue"));
    assertTrue(java.contains("CFloatPointer floatValue"));
    assertTrue(java.contains("CDoublePointer doubleValue"));
    assertTrue(java.contains("readByte(0) != 0"));
    assertTrue(java.contains("(byte) (value ? 1 : 0)"));
    assertTrue(output.get("MynaFFM.java").contains("FunctionDescriptor.ofVoid"));
  }
}
