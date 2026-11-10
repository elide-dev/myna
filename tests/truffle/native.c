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
// Two borrowed buffers with lengths of their own beside a pointer that is not a buffer: copies what
// fits, stores how many bytes that was through copied when it is non-null, and returns their sum.
uint64_t cells_transfer(const uint8_t *input, uint64_t count, uint8_t *output, uint32_t capacity, int64_t *copied) {
  uint64_t fits = count < capacity ? count : capacity;
  uint64_t sum = 0;
  for (uint64_t i = 0; i < fits; i++) { output[i] = input[i]; sum += input[i]; }
  if (copied) *copied = (int64_t)fits;
  return sum;
}
// A byte buffer whose count stays an explicit argument, beside a second pointer.
uint64_t cells_sum_bytes(const uint8_t *bytes, uint64_t count, const int64_t *bias) {
  uint64_t sum = bias ? (uint64_t)*bias : 0;
  for (uint64_t i = 0; i < count; i++) sum += bytes[i];
  return sum;
}

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
/* Two C strings borrowed by one call: each length up to its terminator, plus each first byte
   (an empty string's first byte is its terminator, 0). */
int64_t cells_pair(const char *left, const char *right) {
  return ((int64_t)strlen(left) << 32) | ((int64_t)strlen(right) << 16) | ((int64_t)(uint8_t)left[0] << 8) | (uint8_t)right[0];
}
