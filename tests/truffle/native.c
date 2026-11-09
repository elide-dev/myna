// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
#include <stdbool.h>
#include <stddef.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>

int32_t cells_add(int32_t left, int32_t right) { return left + right; }
int64_t cells_widen(uint8_t small, uint16_t medium, uint32_t wide, bool flag) {
  return (int64_t)small | ((int64_t)medium << 8) | ((int64_t)wide << 24) | ((int64_t)(flag ? 1 : 0) << 56);
}
double cells_scale(float value, double factor) { return (double)value * factor; }
uint8_t cells_low_byte(int64_t value) { return (uint8_t)(value & 0xff); }
int64_t cells_peek(const int64_t *cell) { return *cell; }
void cells_poke(int64_t *cell, int64_t value) { *cell = value; }
size_t cells_raw_length(void *text) { return text ? strlen((const char *)text) : 0; }
size_t cells_strlen(const char *text) { return strlen(text); }
uint64_t cells_checksum(const uint8_t *bytes, uint64_t count) {
  uint64_t sum = 0;
  for (uint64_t i = 0; i < count; i++) sum += bytes[i];
  return sum;
}
void cells_fill(uint8_t *bytes, uint32_t count, uint8_t value) { memset(bytes, value, count); }

typedef struct Counter { int64_t next; } Counter;
static int64_t live = 0;
Counter *cells_counter_create(int64_t start) {
  if (start < 0) return NULL;
  Counter *counter = malloc(sizeof *counter);
  if (!counter) return NULL;
  counter->next = start;
  live++;
  return counter;
}
void cells_counter_destroy(Counter *counter) { free(counter); live--; }
int64_t cells_counter_next(Counter *counter) { return counter->next++; }
int64_t cells_counter_live(void) { return live; }
int64_t cells_sleepy_add(int64_t left, int64_t right) { return left + right; }
/* Two C strings borrowed by one call: lengths through each terminator, plus each first byte. */
int64_t cells_pair(const char *left, const char *right) {
  return ((int64_t)strlen(left) << 32) | ((int64_t)strlen(right) << 16) | ((int64_t)(uint8_t)left[0] << 8) | (uint8_t)right[0];
}
