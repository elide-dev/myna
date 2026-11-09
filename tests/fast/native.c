// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
#include "seam.h"
int64_t myna_fast_increment(int64_t value) { return value + 1; }

struct Pair myna_fast_flip(struct Pair value) {
  return (struct Pair) {value.y, value.x};
}
void myna_fast_flip_myna_ref(struct Pair const *value, struct Pair *result) {
  *result = myna_fast_flip(*value);
}
int32_t myna_fast_total(struct Pair value) { return value.x + value.y; }
int32_t myna_fast_total_myna_ref(struct Pair const *value) {
  return myna_fast_total(*value);
}
