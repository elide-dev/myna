// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package dev.elide.myna.validate;

import static org.junit.jupiter.api.Assertions.*;

import dev.elide.myna.frontend.Dsl;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;

/** Keeps the reserved Native Image type names in step with what the generator imports. */
class NativeImageNamesTest {
  /**
   * Every simple name the Native Image class imports is a reserved type name, so a declared type
   * nested in that class can never shadow it.
   */
  @Test
  void nativeImageImportsAreReservedTypeNames() throws Exception {
    var seams = new ArrayList<Path>();
    for (String root : List.of("examples", "tests", "benchmarks"))
      try (var walk = Files.walk(Path.of(root))) {
        walk.filter(p -> p.toString().endsWith(".seam")).sorted().forEach(seams::add);
      }
    assertTrue(seams.size() > 10, seams.toString());
    var imported = new TreeSet<String>();
    var pattern = java.util.regex.Pattern.compile("(?m)^import ([\\w.]+)\\.(\\w+);$");
    for (Path seam : seams) {
      var module = Dsl.parse(Files.readString(seam));
      if (module.formatsOnly()) continue; // no Native Image bindings
      var matcher =
          pattern.matcher(
              dev.elide.myna.Generator.generate(module, true).get(module.nativeClass() + ".java"));
      while (matcher.find()) imported.add(matcher.group(2));
    }
    for (String name : List.of("CConstant", "CContext", "AllowNarrowingCast", "CStruct"))
      assertTrue(imported.contains(name), name + " is not covered by a fixture");
    var missing = new TreeSet<>(imported);
    missing.removeAll(Validator.NATIVE_IMAGE_NAMES);
    assertEquals(Set.of(), missing);
  }
}
