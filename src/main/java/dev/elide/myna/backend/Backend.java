// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package dev.elide.myna.backend;

import dev.elide.myna.model.Seam.Module;
import dev.elide.myna.serialize.Fingerprint;

/** A pure projection of validated, normalized IR. */
public interface Backend {
  String filename(Module module);

  String generate(Module module);

  /**
   * {@link #generate(Module)} given the fingerprint the generator computed once, for exactly {@code
   * module}, and shares with all its backends. A backend that embeds the fingerprint overrides this
   * and reads it with {@link Fingerprint#valueFor}; every form throws if {@code fingerprint}
   * describes another module instance.
   */
  default String generate(Module module, Fingerprint fingerprint) {
    fingerprint.requireFor(module);
    return generate(module);
  }
}
