// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package dev.elide.myna.bench;

import dev.elide.myna.backend.*;
import dev.elide.myna.frontend.Dsl;
import dev.elide.myna.model.Seam.Module;
import dev.elide.myna.validate.Validator;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.*;

/** Each code generation backend in isolation, on a validated module. */
@State(Scope.Benchmark)
@Fork(2)
@Warmup(iterations = 15, time = 1)
@Measurement(iterations = 5, time = 1)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class BackendBenchmark {
  @Param({"buffer", "synthetic"})
  public String seam;

  private Module module;

  @Setup
  public void setup() {
    module = Dsl.parse(Seams.load(seam));
    Validator.validate(module, true);
  }

  @Benchmark
  public String cHeader() {
    return new CHeader().generate(module);
  }

  @Benchmark
  public String rustFfi() {
    return new RustFfi().generate(module);
  }

  @Benchmark
  public String javaFfm() {
    return new JavaFfm().generate(module);
  }

  @Benchmark
  public String nativeImage() {
    return new NativeImage().generate(module);
  }

  @Benchmark
  public String nativeImageForeign() {
    return new NativeImageForeign().generate(module);
  }

  @Benchmark
  public String llvm() {
    return new Llvm().generate(module);
  }
}
