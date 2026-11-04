#include <stdint.h>
extern int64_t myna_bench_leaf(int64_t);
// Fixed bounded batches preserve every noinline scalar call. No algebraic fusion.
#define BATCH(N) \
__attribute__((noinline,visibility("default"))) int64_t myna_bench_batch##N(int64_t x) { \
  for (int i=0; i<N; i++) x=myna_bench_leaf(x); return x; \
}
BATCH(4)
BATCH(16)
BATCH(64)
BATCH(256)

int64_t (*myna_leaf_address(void))(int64_t) { return myna_bench_leaf; }
__attribute__((noinline,visibility("default"))) int64_t myna_bench_identity(int64_t x) { return x; }
int64_t (*myna_bench_identity_address(void))(int64_t) { return myna_bench_identity; }
