// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
#include <stdint.h>

#include "seam.h"

// The declared by-value functions, and the reference shims Native Image binds.
struct Pair addr_swap_pair(struct Pair value) {
  struct Pair swapped = {value.b + 1, value.a + 1};
  return swapped;
}
struct Pair addr_swap_pair_slow(struct Pair value) {
  struct Pair swapped = {value.b + 2, value.a + 2};
  return swapped;
}
void addr_swap_pair_myna_ref(struct Pair const *value, struct Pair *result) { *result = addr_swap_pair(*value); }
void addr_swap_pair_slow_myna_ref(struct Pair const *value, struct Pair *result) {
  *result = addr_swap_pair_slow(*value);
}

// A native caller that only knows the declared signature.
int64_t addr_call_pair(uint64_t target, int64_t a, int64_t b) {
  struct Pair (*call)(struct Pair) = (struct Pair (*)(struct Pair))(uintptr_t)target;
  struct Pair argument = {a, b};
  struct Pair result = call(argument);
  return result.a * 1000 + result.b;
}
