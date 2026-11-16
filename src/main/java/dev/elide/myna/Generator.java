// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package dev.elide.myna;

import dev.elide.myna.backend.*;
import dev.elide.myna.model.Seam.Module;
import dev.elide.myna.serialize.Fingerprint;
import dev.elide.myna.validate.Validator;
import java.util.*;

public final class Generator {
  private Generator() {}

  public static SortedMap<String, String> generate(Module module, boolean strict) {
    // Serialized and hashed at most once: validation and every backend share the one fingerprint.
    var abi = new Fingerprint(module);
    Validator.validate(module, strict, abi);
    String fingerprint = abi.value();
    var outputs = new TreeMap<String, String>();
    outputs.put("seam.json", abi.json());
    outputs.put("seam.abi", fingerprint + "\n");
    var header = new CHeader();
    String seamH = header.generate(module, fingerprint);
    outputs.put(header.filename(module), seamH);
    // The directives class embeds the seam.h just generated rather than building it again.
    var directives = new NativeImageDirectives();
    outputs.put(directives.filename(module), directives.withHeader(module, seamH));
    for (Backend backend :
        List.of(
            new RustFfi(),
            new JavaFfm(),
            new NativeImage(),
            new NativeImageProperties(),
            new NativeImageForeign(),
            new Llvm()))
      outputs.put(backend.filename(module), backend.generate(module, fingerprint));
    if (!module.returnedResources().isEmpty() || !module.callBorrows().isEmpty()) {
      var ownership = new NativeOwnership();
      outputs.put(ownership.filename(module), ownership.generate(module));
    }
    if (JavaConstants.applies(module)) {
      var constants = new JavaConstants();
      outputs.put(constants.filename(module), constants.generate(module));
    }
    if (module.truffleConfig() != null) {
      var cells = new TruffleCells();
      outputs.put(cells.filename(module), cells.generate(module, fingerprint));
    }
    return Collections.unmodifiableSortedMap(outputs);
  }
}
