package dev.elide.myna;

import dev.elide.myna.backend.*;
import dev.elide.myna.model.Seam.Module;
import dev.elide.myna.serialize.SeamJson;
import dev.elide.myna.validate.Validator;
import java.util.*;

public final class Generator {
  private Generator() {}

  public static SortedMap<String, String> generate(Module module, boolean strict) {
    Validator.validate(module, strict);
    var outputs = new TreeMap<String, String>();
    outputs.put("seam.json", SeamJson.canonical(module));
    outputs.put("seam.abi", SeamJson.fingerprint(module) + "\n");
    for (Backend backend :
        List.of(
            new CHeader(),
            new RustFfi(),
            new JavaFfm(),
            new NativeImage(),
            new NativeImageForeign(),
            new Llvm())) outputs.put(backend.filename(module), backend.generate(module));
    if (!module.resources().isEmpty() || !module.borrows().isEmpty()) {
      var ownership = new NativeOwnership();
      outputs.put(ownership.filename(module), ownership.generate(module));
    }
    return Collections.unmodifiableSortedMap(outputs);
  }
}
