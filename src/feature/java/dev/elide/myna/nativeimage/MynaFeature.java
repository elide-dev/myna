package dev.elide.myna.nativeimage;

import dev.elide.myna.Generator;
import dev.elide.myna.cli.Main;
import dev.elide.myna.frontend.Dsl;
import java.io.IOException;
import java.nio.file.*;
import org.graalvm.nativeimage.hosted.Feature;

/** Explicit descriptor export during image building; no inferred compiler facts in v1. */
public final class MynaFeature implements Feature {
  @Override
  public void beforeAnalysis(BeforeAnalysisAccess access) {
    String input = System.getProperty("myna.input");
    String output = System.getProperty("myna.output");
    if (input == null || output == null)
      throw new IllegalArgumentException(
          "MynaFeature requires -Dmyna.input=FILE -Dmyna.output=DIR");
    try {
      Main.write(
          Generator.generate(Dsl.parse(Files.readString(Path.of(input))), true), Path.of(output));
    } catch (IOException e) {
      throw new IllegalStateException("cannot export seam descriptors", e);
    }
  }
}
