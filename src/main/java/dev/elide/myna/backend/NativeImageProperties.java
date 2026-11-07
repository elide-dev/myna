// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package dev.elide.myna.backend;

import dev.elide.myna.model.Seam;
import dev.elide.myna.model.Seam.Module;
import java.util.stream.Collectors;

/** Automatically consumed when the generated META-INF tree accompanies compiled bindings. */
public final class NativeImageProperties implements Backend {
  public String filename(Module module) {
    return "META-INF/native-image/dev.elide.myna/"
        + module.javaPackageName()
        + "."
        + module.nativeClass()
        + "/native-image.properties";
  }

  public String generate(Module module) {
    String type = module.javaPackageName() + "." + module.nativeClass();
    var leaves = module.functions().stream().filter(Seam::nativeLeaf).toList();
    String result =
        "# Myna binding ABI 2: preserve symbolic metadata / runtime-address separation.\n";
    if (leaves.isEmpty()) return result;
    return result
        + "JavaArgs ="
        + " --add-exports=org.graalvm.nativeimage.guest.staging/com.oracle.svm.guest.staging.c=ALL-UNNAMED\n"
        + "Args = --initialize-at-build-time="
        + type
        + ","
        + type
        + "$MynaSymbols"
        + " --initialize-at-run-time="
        + leaves.stream()
            .map(f -> type + "$" + f.name() + "_Binding")
            .collect(Collectors.joining(","))
        + "\n";
  }
}
