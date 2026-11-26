// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
#include "seam.h"

int64_t myna_deliver_records(Span span, int64_t records, int64_t width) {
  if (width <= 0 || records < 0)
    return -1;
  int64_t first = 0;
  while (first < records) {
    int64_t count = records - first < width ? records - first : width;
    if (span(first, count) != count)
      return -1;
    first += count;
  }
  return first;
}
