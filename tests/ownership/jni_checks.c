// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
#include "seam-jni.h"

/* Enter generated error paths directly, including ones the Java convenience wrapper rejects. */
JNIEXPORT jboolean JNICALL Java_JniMain_nativeErrors(JNIEnv *env, jclass clazz, jobject heap, jobject direct) {
  jclass error = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
  if (error == NULL) return JNI_FALSE;
  jobject inputs[] = {NULL, heap};
  /* One buffer, then each position of a two-buffer adapter beside a valid direct buffer. */
  for (int i = 0; i < 6; i++) {
    jobject input = inputs[i % 2];
    if (i < 2) Java_fixture_owned_NativeApiJNI_checksumBuffer__Ljava_nio_ByteBuffer_2(env, clazz, input);
    else Java_fixture_owned_NativeApiJNI_transferBuffer__Ljava_nio_ByteBuffer_2Ljava_nio_ByteBuffer_2J(
        env, clazz, i < 4 ? input : direct, i < 4 ? direct : input, 0);
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
  /* A nullable buffer is the exception: NULL is an absent buffer, which reaches the native function
     as a null pointer with length zero (bit 32 of the result) and raises nothing. Any other
     invalid buffer for the same parameter is still rejected. */
  jlong absent = Java_fixture_owned_NativeApiJNI_decodeBuffer__Ljava_nio_ByteBuffer_2Ljava_nio_ByteBuffer_2(env, clazz, direct, NULL);
  if ((*env)->ExceptionCheck(env) || absent != (((jlong)1 << 32) | 1)) return JNI_FALSE;
  absent = Java_fixture_owned_NativeApiJNI_tallyBuffer__Ljava_nio_ByteBuffer_2(env, clazz, NULL);
  if ((*env)->ExceptionCheck(env) || absent != ((jlong)1 << 32)) return JNI_FALSE;
  Java_fixture_owned_NativeApiJNI_decodeBuffer__Ljava_nio_ByteBuffer_2Ljava_nio_ByteBuffer_2(env, clazz, direct, heap);
  jthrowable rejected = (*env)->ExceptionOccurred(env);
  if (rejected == NULL) return JNI_FALSE;
  (*env)->ExceptionClear(env);
  jboolean invalid = (*env)->IsInstanceOf(env, rejected, error);
  (*env)->DeleteLocalRef(env, rejected);
  if (!invalid) return JNI_FALSE;
  (*env)->DeleteLocalRef(env, error);
  return JNI_TRUE;
}
