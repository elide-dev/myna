// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
#include <stdint.h>
#include <string.h>
// Borrow and string targets: each call depends on the previous result through salt.
__attribute__((noinline,visibility("default"))) int64_t myna_bench_sum(const uint8_t *bytes, uint64_t count, int64_t salt) {
  uint64_t sum = (uint64_t)salt;
  for (uint64_t i = 0; i < count; i++) sum += bytes[i];
  return (int64_t)sum;
}
// The same sum behind a transitioning import, for pinning across a transition.
__attribute__((noinline,visibility("default"))) int64_t myna_bench_sum_step(const uint8_t *bytes, uint64_t count, int64_t salt) {
  return myna_bench_sum(bytes, count, salt);
}
__attribute__((noinline,visibility("default"))) int64_t myna_bench_text(const char *text, int64_t salt) {
  return (int64_t)((uint64_t)salt + strlen(text));
}
// A decode-entry shape: two byte buffers, two opaque pointers and five integers; every argument
// reaches the result so none can be dropped. Exported once as a leaf and once as a transition.
static inline int64_t slab(const uint8_t *data, uint64_t count, const uint8_t *aux, void *context, void *output,
                           int32_t a, int32_t b, uint32_t c, int64_t d, int64_t salt) {
  return (int64_t)((uint64_t)salt + count + data[0] + aux[0] + (uint64_t)(int64_t)a + (uint64_t)(int64_t)b + c +
                   (uint64_t)d + (context != 0) + (output != 0));
}
__attribute__((noinline,visibility("default"))) int64_t myna_bench_slab(const uint8_t *data, uint64_t count, const uint8_t *aux, void *context, void *output,
                                                                        int32_t a, int32_t b, uint32_t c, int64_t d, int64_t salt) {
  return slab(data, count, aux, context, output, a, b, c, d, salt);
}
__attribute__((noinline,visibility("default"))) int64_t myna_bench_slab_step(const uint8_t *data, uint64_t count, const uint8_t *aux, void *context, void *output,
                                                                             int32_t a, int32_t b, uint32_t c, int64_t d, int64_t salt) {
  return slab(data, count, aux, context, output, a, b, c, d, salt);
}
// The same entry for a seam that declares its byte buffers truffle_buffer (raw bytes).
__attribute__((noinline,visibility("default"))) int64_t myna_bench_slab_bytes(const uint8_t *data, uint64_t count, const uint8_t *aux, void *context, void *output,
                                                                              int32_t a, int32_t b, uint32_t c, int64_t d, int64_t salt) {
  return slab(data, count, aux, context, output, a, b, c, d, salt);
}
// Creates that initialize the caller's struct: adopt records where its input is and reads it on
// release, after the call that lent it; scan sums its input during the call and keeps only the sum.
// Each release adds its sum to a total that myna_bench_created_total returns and clears.
typedef struct MynaBenchCreated { const uint8_t *data; uint64_t length; } MynaBenchCreated;
static uint64_t created_total;
__attribute__((noinline,visibility("default"))) int32_t myna_bench_adopt(const uint8_t *data, uint64_t length, MynaBenchCreated *adopted) {
  adopted->data = data;
  adopted->length = length;
  return 0;
}
__attribute__((noinline,visibility("default"))) void myna_bench_adopted_release(MynaBenchCreated *adopted) {
  created_total += (uint64_t)myna_bench_sum(adopted->data, adopted->length, 0);
  adopted->data = 0;
}
__attribute__((noinline,visibility("default"))) int32_t myna_bench_scan(const uint8_t *data, uint64_t length, MynaBenchCreated *scanned) {
  scanned->data = 0;
  scanned->length = (uint64_t)myna_bench_sum(data, length, 0);
  return 0;
}
__attribute__((noinline,visibility("default"))) void myna_bench_scanned_release(MynaBenchCreated *scanned) {
  created_total += scanned->length;
}
__attribute__((noinline,visibility("default"))) int64_t myna_bench_created_total(void) {
  uint64_t total = created_total;
  created_total = 0;
  return (int64_t)total;
}
#ifdef __linux__
// The pinned toolchain's sysroot predates glibc 2.27, but the Native Image libffi used by Truffle
// NFI calls memfd_create. Weak, so a newer libc's definition takes precedence at link time.
#include <unistd.h>
#include <sys/syscall.h>
__attribute__((weak)) int memfd_create(const char *name, unsigned int flags) {
  return (int)syscall(SYS_memfd_create, name, flags);
}
#endif
