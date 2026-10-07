package dev.elide.myna;

import static org.junit.jupiter.api.Assertions.*;

import dev.elide.myna.nativeimage.MynaFeature;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FeatureTest {
  @TempDir Path temp;

  @Test
  void exportsDescriptorsFromExplicitInput() throws Exception {
    String oldInput = System.getProperty("myna.input"),
        oldOutput = System.getProperty("myna.output");
    try {
      System.clearProperty("myna.input");
      System.clearProperty("myna.output");
      var feature = new MynaFeature();
      assertThrows(IllegalArgumentException.class, () -> feature.beforeAnalysis(null));
      System.setProperty("myna.input", "examples/buffer.seam");
      System.setProperty("myna.output", temp.toString());
      feature.beforeAnalysis(null);
      assertTrue(Files.readString(temp.resolve("seam.ll")).contains("!myna.contract"));
      System.setProperty("myna.input", temp.resolve("missing").toString());
      assertThrows(IllegalStateException.class, () -> feature.beforeAnalysis(null));
    } finally {
      if (oldInput == null) System.clearProperty("myna.input");
      else System.setProperty("myna.input", oldInput);
      if (oldOutput == null) System.clearProperty("myna.output");
      else System.setProperty("myna.output", oldOutput);
    }
  }
}
