package bench;

/** Match generated JNI's independent runtime library initializer. */
final class ManualJNI {
  static { System.loadLibrary("boundary_jni"); }
  static native long step(long value);
}
