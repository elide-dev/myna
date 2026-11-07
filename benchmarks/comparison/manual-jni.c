// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
#include <jni.h>
#include <stdint.h>

extern int64_t myna_bench_step(int64_t value);

/* Match the generated adapter's pending-exception contract and raw target. */
JNIEXPORT jlong JNICALL Java_bench_ManualJNI_step(JNIEnv *env, jclass cls, jlong value) {
  (void)cls;
  if ((*env)->ExceptionCheck(env)) return 0;
  return (jlong)myna_bench_step((int64_t)value);
}
