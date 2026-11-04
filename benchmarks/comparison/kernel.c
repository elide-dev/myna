#include <stdint.h>
#include <jni.h>
// Identical dependency chain for each projection. Unsigned arithmetic wraps.
__attribute__((noinline,visibility("default"))) int64_t myna_bench_step(int64_t value) {
  return (int64_t)((uint64_t)value * UINT64_C(1664525) + UINT64_C(1013904223));
}
JNIEXPORT jlong JNICALL Java_bench_Jvm_step(JNIEnv *env, jclass cls, jlong value) {
  (void)env; (void)cls;
  return myna_bench_step(value);
}

__attribute__((noinline,visibility("default"))) int64_t myna_bench_leaf(int64_t value) {
  return (int64_t)((uint64_t)value * UINT64_C(1664525) + UINT64_C(1013904223));
}
