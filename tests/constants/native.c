// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
#include "seam.h"

/* Not in the seam: the target of a consumer-written @CFunction typed with the @CEnum. */
int32_t consts_echo_status(int32_t value);

static constants_level_t last_level;

constants_status_t consts_status(struct Record const *view) { return view->status; }

void consts_set_level(constants_level_t level, struct Session const *session) {
  (void)session;
  last_level = level;
}

constants_status_t consts_blocking_status(struct Session const *session) {
  (void)session;
  return (constants_status_t)last_level;
}

int32_t consts_echo_status(int32_t value) { return value; }
