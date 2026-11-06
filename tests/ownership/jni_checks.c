#include "seam-jni.h"

/* Enter generated error paths directly, including ones the Java convenience wrapper rejects. */
JNIEXPORT jboolean JNICALL Java_JniMain_nativeErrors(JNIEnv *env, jclass clazz, jobject heap) {
  jclass error = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
  if (error == NULL) return JNI_FALSE;
  jobject inputs[] = {NULL, heap};
  for (int i = 0; i < 2; i++) {
    Java_fixture_owned_NativeApiJNI_checksumBuffer__Ljava_nio_ByteBuffer_2(env, clazz, inputs[i]);
    jthrowable before = (*env)->ExceptionOccurred(env);
    if (before == NULL) return JNI_FALSE;
    /* A pending exception must survive entry to another generated wrapper unchanged. */
    Java_fixture_owned_NativeApiJNI_liveCount__(env, clazz);
    jthrowable after = (*env)->ExceptionOccurred(env);
    (*env)->ExceptionClear(env);
    jboolean same = (*env)->IsSameObject(env, before, after);
    jboolean expected = (*env)->IsInstanceOf(env, before, error);
    (*env)->DeleteLocalRef(env, before);
    (*env)->DeleteLocalRef(env, after);
    if (!same || !expected) return JNI_FALSE;
  }
  (*env)->DeleteLocalRef(env, error);
  return JNI_TRUE;
}
