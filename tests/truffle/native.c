// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
#include <stdbool.h>
#include <stddef.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>

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
// An optional output buffer: text may be NULL with capacity zero. Writes each input byte plus one
// into what fits of a buffer that is present and returns the bytes the whole output needs, with
// bit 32 set when there is no buffer. A buffer that is present never has the null address, even
// when it is empty; NULL with a capacity breaks the contract and returns UINT64_MAX.
uint64_t cells_decode(const uint8_t *data, uint64_t length, uint8_t *text, uint32_t capacity) {
  if (!text) return capacity == 0 ? length | (UINT64_C(1) << 32) : UINT64_MAX;
  uint64_t fits = length < capacity ? length : capacity;
  for (uint64_t i = 0; i < fits; i++) text[i] = (uint8_t)(data[i] + 1);
  return length;
}
// A byte buffer whose count stays an explicit argument, beside a second pointer.
uint64_t cells_sum_bytes(const uint8_t *bytes, uint64_t count, const int64_t *bias) {
  uint64_t sum = bias ? (uint64_t)*bias : 0;
  for (uint64_t i = 0; i < count; i++) sum += bytes[i];
  return sum;
}
// Sums a buffer for as long as the caller wants the call to last: gate[0] is set once the call
// is in native code, and the sum starts when the caller sets gate[1].
uint64_t cells_hold(const uint8_t *bytes, uint64_t count, int32_t *gate) {
  struct timespec pause = {0, 1000000};
  __atomic_store_n(&gate[0], 1, __ATOMIC_SEQ_CST);
  while (!__atomic_load_n(&gate[1], __ATOMIC_SEQ_CST)) nanosleep(&pause, NULL);
  uint64_t sum = 0;
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
// A second resource created from another resource's handle and a pointer to its step: the
// stepper advances its counter, so it must not outlive it, which is the consumer's to ensure.
typedef struct Stepper { Counter *counter; int64_t step; } Stepper;
Stepper *cells_stepper_create(Counter *counter, const int64_t *step) {
  if (!counter || !step || *step == 0) return NULL;
  Stepper *stepper = malloc(sizeof *stepper);
  if (!stepper) return NULL;
  stepper->counter = counter;
  stepper->step = *step;
  live++;
  return stepper;
}
void cells_stepper_destroy(Stepper *stepper) { free(stepper); live--; }
int64_t cells_stepper_next(Stepper *stepper) {
  int64_t value = stepper->counter->next;
  stepper->counter->next += stepper->step;
  return value;
}
int64_t cells_counter_live(void) { return live; }
// A create that reports a status and writes its handle into a field of the caller's struct.
// Statuses 0 and 2 mean the layout exists; 7 means it does not; a first byte of 0xff reports
// success without writing a handle.
typedef struct Layout { int64_t width; } Layout;
typedef struct LayoutResult { uint64_t count; Layout *data; uint64_t capacity; } LayoutResult;
int32_t cells_layout_compile(const uint8_t *schema, uint64_t length, LayoutResult *result) {
  result->count = length;
  result->data = NULL;
  result->capacity = 0;
  if (length == 0) return 7;
  if (schema[0] == 0xff) return 0;
  Layout *layout = malloc(sizeof *layout);
  if (!layout) return 12;
  layout->width = schema[0];
  result->data = layout;
  result->capacity = 2 * length;
  live++;
  return schema[0] == 2 ? 2 : 0;
}
void cells_layout_release(Layout *layout) { free(layout); live--; }
int64_t cells_layout_width(const Layout *layout) { return layout->width; }
// A result initialized in place: the caller's struct is the resource, and release takes it back.
typedef struct View { int64_t sum; uint8_t *scratch; } View;
int32_t cells_view_init(const uint8_t *data, uint64_t length, View *view) {
  if (length == 0) return -1;
  view->scratch = malloc(16);
  if (!view->scratch) return -2;
  view->sum = 0;
  for (uint64_t i = 0; i < length; i++) view->sum += data[i];
  live++;
  return 0;
}
void cells_view_release(View *view) { free(view->scratch); view->scratch = NULL; live--; }
int64_t cells_view_sum(const View *view) { return view->sum; }
int64_t cells_sleepy_add(int64_t left, int64_t right) { return left + right; }
/* Two C strings borrowed by one call: each length up to its terminator, plus each first byte
   (an empty string's first byte is its terminator, 0). */
int64_t cells_pair(const char *left, const char *right) {
  return ((int64_t)strlen(left) << 32) | ((int64_t)strlen(right) << 16) | ((int64_t)(uint8_t)left[0] << 8) | (uint8_t)right[0];
}
