package dev.elide.myna.bench;

import dev.elide.myna.Generator;
import dev.elide.myna.frontend.Dsl;
import dev.elide.myna.model.Seam.Module;
import dev.elide.myna.serialize.SeamJson;
import dev.elide.myna.validate.Validator;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.*;

/** End-to-end and per-stage benchmarks of the Seam IR pipeline: parse, validate, emit. */
@State(Scope.Benchmark)
@Fork(2)
@Warmup(iterations = 15, time = 1)
@Measurement(iterations = 5, time = 1)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class GeneratorBenchmark {
  @Param({"buffer", "ownership", "elide_lzma", "bemo", "synthetic"})
  public String seam;

  private String source;
  private Module module;

  @Setup
  public void setup() {
    source = Seams.load(seam);
    module = Dsl.parse(source);
    Validator.validate(module, true);
  }

  @Benchmark
  public Object parse() {
    return Dsl.parse(source);
  }

  @Benchmark
  public Module validate() {
    Validator.validate(module, true);
    return module;
  }

  @Benchmark
  public Object generate() {
    return Generator.generate(module, true);
  }

  @Benchmark
  public Object endToEnd() {
    return Generator.generate(Dsl.parse(source), true);
  }

  @Benchmark
  public String canonicalJson() {
    return SeamJson.canonical(module);
  }

  @Benchmark
  public String fingerprint() {
    return SeamJson.fingerprint(module);
  }
}
