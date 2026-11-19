// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package dev.elide.myna;

import dev.elide.myna.backend.*;
import dev.elide.myna.model.Seam;
import dev.elide.myna.model.Seam.Module;
import dev.elide.myna.serialize.Fingerprint;
import dev.elide.myna.validate.Validator;
import java.util.*;

public final class Generator {
  private Generator() {}

  public static SortedMap<String, String> generate(Module module, boolean strict) {
    return generate(module, strict, new Fingerprint(module));
  }

  /**
   * Validates and generates {@code module}, serializing and hashing it at most once: validation and
   * every backend share {@code abi}, which must have been computed for exactly {@code module}.
   */
  public static SortedMap<String, String> generate(Module module, boolean strict, Fingerprint abi) {
    Validator.validate(module, strict, abi);
    var outputs = new TreeMap<String, String>();
    outputs.put("seam.json", abi.json());
    outputs.put("seam.abi", abi.value() + "\n");
    var c = new CHeader();
    CHeader.Generated header = c.header(module, abi);
    outputs.put(c.filename(module), header.text());
    for (Backend backend : List.of(new RustFfi(), new JavaFfm(), new NativeImage()))
      outputs.put(backend.filename(module), backend.generate(module, abi));
    // The directives class embeds the seam.h just generated rather than building it again.
    var directives = new NativeImageDirectives();
    outputs.put(directives.filename(module), directives.withHeader(module, header));
    for (Backend backend :
        List.of(new NativeImageProperties(), new NativeImageForeign(), new Llvm()))
      outputs.put(backend.filename(module), backend.generate(module, abi));
    if (!module.ownedResources().isEmpty()
        || !module.callBorrows().isEmpty()
        // C string wrappers live in the ownership class too.
        || module.functions().stream()
            .anyMatch(
                f -> f.parameters().stream().anyMatch(p -> p.access() == Seam.Access.CSTRING))) {
      var ownership = new NativeOwnership();
      outputs.put(ownership.filename(module), ownership.generate(module));
    }
    if (JavaConstants.applies(module)) {
      var constants = new JavaConstants();
      outputs.put(constants.filename(module), constants.generate(module));
    }
    if (RustFormats.applies(module))
      outputs.put(RustFormats.FILENAME, new RustFormats().generate(module));
    if (JavaFormats.applies(module)) outputs.putAll(new JavaFormats().generate(module));
    if (module.truffleConfig() != null) {
      var cells = new TruffleCells();
      outputs.put(cells.filename(module), cells.generate(module, abi));
    }
    return Collections.unmodifiableSortedMap(outputs);
  }
}
