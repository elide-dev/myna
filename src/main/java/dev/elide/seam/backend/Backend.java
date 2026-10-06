package dev.elide.seam.backend;

import dev.elide.seam.model.Seam.Module;

/** A pure projection of validated, normalized IR. */
public interface Backend {
  String filename(Module module);

  String generate(Module module);
}
