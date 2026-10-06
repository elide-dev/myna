package dev.elide.seam;

import static org.junit.jupiter.api.Assertions.*;

import dev.elide.seam.nativeimage.SeamFeature;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FeatureTest {
  @TempDir Path temp;

  @Test
  void exportsDescriptorsFromExplicitInput() throws Exception {
    String oldInput = System.getProperty("svmgen.input"),
        oldOutput = System.getProperty("svmgen.output");
    try {
      System.clearProperty("svmgen.input");
      System.clearProperty("svmgen.output");
      var feature = new SeamFeature();
      assertThrows(IllegalArgumentException.class, () -> feature.beforeAnalysis(null));
      System.setProperty("svmgen.input", "examples/buffer.seam");
      System.setProperty("svmgen.output", temp.toString());
      feature.beforeAnalysis(null);
      assertTrue(Files.readString(temp.resolve("seam.ll")).contains("!svmgen.contract"));
      System.setProperty("svmgen.input", temp.resolve("missing").toString());
      assertThrows(IllegalStateException.class, () -> feature.beforeAnalysis(null));
    } finally {
      if (oldInput == null) System.clearProperty("svmgen.input");
      else System.setProperty("svmgen.input", oldInput);
      if (oldOutput == null) System.clearProperty("svmgen.output");
      else System.setProperty("svmgen.output", oldOutput);
    }
  }
}
