package dev.elide.myna.backend;

import dev.elide.myna.model.Seam.Module;

/** A pure projection of validated, normalized IR. */
public interface Backend {
  String filename(Module module);

  String generate(Module module);
}
