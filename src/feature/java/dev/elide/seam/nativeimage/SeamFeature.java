package dev.elide.seam.nativeimage;

import dev.elide.seam.Generator;
import dev.elide.seam.cli.Main;
import dev.elide.seam.frontend.Dsl;
import java.io.IOException;
import java.nio.file.*;
import org.graalvm.nativeimage.hosted.Feature;

/** Explicit descriptor export during image building; no inferred compiler facts in v1. */
public final class SeamFeature implements Feature {
  @Override
  public void beforeAnalysis(BeforeAnalysisAccess access) {
    String input = System.getProperty("svmgen.input");
    String output = System.getProperty("svmgen.output");
    if (input == null || output == null)
      throw new IllegalArgumentException(
          "SeamFeature requires -Dsvmgen.input=FILE -Dsvmgen.output=DIR");
    try {
      Main.write(
          Generator.generate(Dsl.parse(Files.readString(Path.of(input))), true), Path.of(output));
    } catch (IOException e) {
      throw new IllegalStateException("cannot export seam descriptors", e);
    }
  }
}
