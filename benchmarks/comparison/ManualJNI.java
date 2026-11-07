// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package bench;

/** Match generated JNI's independent runtime library initializer. */
final class ManualJNI {
  static { System.loadLibrary("boundary_jni"); }
  static native long step(long value);
}
