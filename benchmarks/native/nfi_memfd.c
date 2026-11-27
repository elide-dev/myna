// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
#ifdef __linux__
// The pinned toolchain's sysroot predates glibc 2.27, but the Native Image libffi used by Truffle
// NFI calls memfd_create. Weak, so a newer libc's definition takes precedence at link time.
#include <unistd.h>
#include <sys/syscall.h>
__attribute__((weak)) int memfd_create(const char *name, unsigned int flags) {
  return (int)syscall(SYS_memfd_create, name, flags);
}
#endif
