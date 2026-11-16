// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
#include "seam.h"

int32_t records_exchange(struct Rec *rec) {
  int32_t mask = 0;
  int bit = 0;
#define CHECK(condition) mask |= (int32_t)((condition) ? 1 : 0) << bit++
  CHECK(rec->signed64 == INT64_MIN);
  CHECK(rec->unsigned64 == UINT64_MAX);
  CHECK(rec->size == SIZE_MAX);
  CHECK(rec->offset == PTRDIFF_MIN);
  CHECK(rec->ratio == 1.5);
  CHECK(rec->next == rec);
  CHECK(rec->handle == (struct Handle *)(uintptr_t)0x1234);
  CHECK(rec->signed32 == INT32_MIN);
  CHECK(rec->unsigned32 == UINT32_C(0x80000000));
  CHECK(rec->scale == -2.5f);
  CHECK(rec->signed16 == INT16_MIN);
  CHECK(rec->unsigned16 == UINT16_MAX);
  CHECK(*(const unsigned char *)&rec->flag == 1);
  CHECK(rec->letter == 'z');
  CHECK(rec->signed8 == INT8_MIN);
  CHECK(rec->unsigned8 == UINT8_MAX);
#undef CHECK
  rec->signed64 = INT64_MAX;
  rec->unsigned64 = UINT64_C(1) << 63;
  rec->size = 1;
  rec->offset = -1;
  rec->ratio = -0.25;
  rec->next = NULL;
  rec->handle = (struct Handle *)(uintptr_t)0x5678;
  rec->signed32 = INT32_MAX;
  rec->unsigned32 = UINT32_MAX;
  rec->scale = 0.5f;
  rec->signed16 = INT16_MAX;
  rec->unsigned16 = UINT16_C(0x8000);
  rec->flag = false;
  rec->letter = 'a';
  rec->signed8 = INT8_MAX;
  rec->unsigned8 = UINT8_C(0x80);
  return mask;
}

struct Handle *records_handle_of(struct Rec const *rec) { return rec->handle; }
