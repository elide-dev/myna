// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package dev.elide.myna.backend;

import dev.elide.myna.model.Seam.Module;

/** A pure projection of validated, normalized IR. */
public interface Backend {
  String filename(Module module);

  String generate(Module module);

  /**
   * {@link #generate(Module)} given the module's ABI fingerprint, which the generator computes once
   * for all its backends. A backend that embeds the fingerprint overrides this; the rest ignore it.
   */
  default String generate(Module module, String fingerprint) {
    return generate(module);
  }
}
