// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package dev.elide.myna.truffle;

/**
 * How integer arguments outside a carrier's range are treated. {@link #STRICT} raises a {@link
 * NativeCellException}; {@link #WRAP} truncates to the carrier width in two's complement, the way
 * JavaScript {@code ToInt32} does, and maps non-finite doubles to zero.
 */
public enum CoercionPolicy {
  STRICT,
  WRAP
}
