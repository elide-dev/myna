// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package dev.elide.myna.serialize;

import dev.elide.myna.model.Seam.Module;

/**
 * One generation's canonical {@code seam.json} and ABI fingerprint, each computed at most once and
 * only when first asked for. A generation creates one, lends it to validation and hands it to every
 * backend, so the canonical JSON is serialized and hashed once per generation.
 *
 * <p>It records the exact {@link Module} instance it was computed for, and everything that accepts
 * one checks that instance with {@link #valueFor} or {@link #requireFor}: a fingerprint is never
 * applied to another module, including the same module {@link Module#withTarget retargeted}. It
 * belongs to one generation: it is not thread-safe and must not be shared between modules.
 */
public final class Fingerprint {
  private final Module module;
  private String json;
  private String value;

  public Fingerprint(Module module) {
    this.module = java.util.Objects.requireNonNull(module);
  }

  /** The module instance this fingerprint describes. */
  public Module module() {
    return module;
  }

  /** The module's canonical {@code seam.json}. */
  public String json() {
    if (json == null) json = SeamJson.canonical(module);
    return json;
  }

  /** The SHA-256 of {@link #json()}, in lowercase hex. */
  public String value() {
    if (value == null) value = SeamJson.fingerprintOf(json());
    return value;
  }

  /** {@link #value()}, after {@link #requireFor checking} that it describes {@code m}. */
  public String valueFor(Module m) {
    requireFor(m);
    return value();
  }

  /**
   * Throws unless this fingerprint was computed for exactly {@code m}. The check compares
   * references, so it costs nothing; any other instance needs a fingerprint of its own.
   */
  public void requireFor(Module m) {
    if (m != module)
      throw new IllegalArgumentException(
          "fingerprint of module "
              + module.name()
              + " ("
              + module.targetTriple()
              + ") used for another module instance; compute a Fingerprint for that module");
  }
}
