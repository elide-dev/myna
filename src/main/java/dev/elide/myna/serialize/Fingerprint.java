// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package dev.elide.myna.serialize;

import dev.elide.myna.model.Seam.Module;

/**
 * One generation's canonical {@code seam.json} and ABI fingerprint, each computed at most once and
 * only when first asked for. A generation creates one, lends it to validation and hands its value
 * to every backend, so the canonical JSON is serialized and hashed once per generation. It belongs
 * to that generation alone: it is neither shared nor thread-safe.
 */
public final class Fingerprint {
  private final Module module;
  private String json;
  private String value;

  public Fingerprint(Module module) {
    this.module = module;
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
}
