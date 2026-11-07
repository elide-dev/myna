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
__attribute__((noinline,visibility("default"))) int64_t myna_bench_text(const char *text, int64_t salt) {
  return (int64_t)((uint64_t)salt + strlen(text));
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
