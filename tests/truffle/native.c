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
size_t cells_title_length(const uint8_t *text) { return strlen((const char *)text); }
// A nullable C string: null has length zero.
size_t cells_title_or_empty(const uint8_t *text) { return text ? strlen((const char *)text) : 0; }
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
// What TruffleData's session decode checks of its byte arguments: 2-byte alignment of UTF-16
// input and of text_out, and an even UTF-16 length. 0 accepts; 1 is its argument status.
int32_t cells_utf16_probe(const uint8_t *data, uint64_t length, uint32_t flags, uint8_t *text_out, uint64_t text_capacity) {
  (void)text_capacity;
  if (text_out && ((uintptr_t)text_out % 2)) return 1;
  if ((flags & 1) && (length % 2 || (uintptr_t)data % 2)) return 1;
  return 0;
}
// The address of a buffer, to observe the alignment of what a cell lends.
uint64_t cells_address_of(const uint8_t *bytes, uint64_t count) { (void)count; return (uint64_t)(uintptr_t)bytes; }
uint64_t cells_address_of_borrow(const uint8_t *bytes, uint64_t count) { (void)count; return (uint64_t)(uintptr_t)bytes; }
uint64_t cells_address_of_wide(const uint8_t *bytes, uint64_t count) { (void)count; return (uint64_t)(uintptr_t)bytes; }
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

/* hold for a writable buffer: waits on the gate, then fills what it was lent. */
uint64_t cells_hold_fill(uint8_t *bytes, uint64_t count, int32_t *gate) {
  struct timespec pause = {0, 1000000};
  __atomic_store_n(&gate[0], 1, __ATOMIC_SEQ_CST);
  while (!__atomic_load_n(&gate[1], __ATOMIC_SEQ_CST)) nanosleep(&pause, NULL);
  for (uint64_t i = 0; i < count; i++) bytes[i] = 0x5a;
  return count;
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
// Two constructors of one resource, each reading its buffer during the call only: the resource
// keeps the sum it computed. parse fails on an absent source, analyze on a scale of zero. The
// release accepts null as a no-op, as free does.
typedef struct Parsed { uint64_t value; } Parsed;
static Parsed *parsed_new(uint64_t value) {
  Parsed *parsed = malloc(sizeof *parsed);
  if (!parsed) return NULL;
  parsed->value = value;
  live++;
  return parsed;
}
Parsed *cells_parse_text(const uint8_t *source, uint64_t length) {
  if (!source) return NULL;
  uint64_t sum = 0;
  for (uint64_t i = 0; i < length; i++) sum += source[i];
  return parsed_new(sum);
}
Parsed *cells_analyze_text(const uint8_t *request, uint32_t length, uint8_t scale) {
  if (scale == 0) return NULL;
  uint64_t sum = 0;
  for (uint32_t i = 0; i < length; i++) sum += request[i];
  return parsed_new(sum * scale);
}
// Waits at its gate as cells_hold_fill does, fills its report, and makes a resource that holds the
// report's length.
Parsed *cells_fill_text(uint8_t *report, uint64_t capacity, int32_t *gate) {
  struct timespec pause = {0, 1000000};
  __atomic_store_n(&gate[0], 1, __ATOMIC_SEQ_CST);
  while (!__atomic_load_n(&gate[1], __ATOMIC_SEQ_CST)) nanosleep(&pause, NULL);
  for (uint64_t i = 0; i < capacity; i++) report[i] = 0x5a;
  return parsed_new(capacity);
}
typedef struct Window { int64_t width; } Window;
// A negative width fails with status 3, junk in the out slot and a message in the error buffer.
int32_t cells_window_open(int64_t width, Window **out, uint8_t *error, size_t capacity) {
  if (width < 0) {
    *out = (Window *)0xdead;
    static const char message[] = "negative width";
    for (size_t i = 0; i < capacity && i < sizeof message - 1; i++) error[i] = (uint8_t)message[i];
    return 3;
  }
  Window *window = malloc(sizeof *window);
  if (!window) return 4;
  window->width = width;
  live++;
  *out = window;
  return 0;
}
void cells_window_close(Window *window) {
  free(window);
  live--;
}
int64_t cells_window_width(const Window *window) { return window->width; }
void cells_parsed_release(Parsed *parsed) {
  if (!parsed) return;
  free(parsed);
  live--;
}
uint64_t cells_parsed_value(const Parsed *parsed) { return parsed->value; }
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
// A create that keeps its input until release: the struct records where the adopted bytes are, and
// the sum is recomputed from them on every read and once more on release, so bytes that were freed
// or reused after the call show up as a wrong sum. A key, borrowed for the call only, is added to
// the recorded sum. An empty input fails with status 3.
typedef struct Adopted { const uint8_t *data; uint64_t length; int64_t sum; } Adopted;
static int64_t adopted_last = -1;
static int64_t adopted_sum(const uint8_t *data, uint64_t length) {
  int64_t sum = 0;
  for (uint64_t i = 0; i < length; i++) sum += data[i];
  return sum;
}
int32_t cells_adopt(const uint8_t *data, uint64_t length, const uint8_t *key, uint32_t key_length, Adopted *adopted) {
  if (length == 0) return 3;
  adopted->data = data;
  adopted->length = length;
  adopted->sum = adopted_sum(data, length) + adopted_sum(key, key_length);
  live++;
  return 0;
}
int64_t cells_adopted_sum(const Adopted *adopted) { return adopted_sum(adopted->data, adopted->length); }
void cells_adopted_release(Adopted *adopted) {
  adopted_last = adopted_sum(adopted->data, adopted->length);
  adopted->data = NULL;
  live--;
}
int64_t cells_adopted_last(void) { return adopted_last; }

// TruffleData's json/native/api.cc, reduced to its argument checks (#120). The result is 88 bytes
// whose owner word is set on success; every refusal is TD_JSON_ARGUMENT (3).
typedef struct { void *owner; void *view; uint64_t words[9]; } TdResult;
static int td_aligned(const void *p) { return p && ((uintptr_t)p % 8) == 0; }
void *td_session_create(void) { return malloc(8); }
void td_session_release(void *session) { free(session); }
int32_t td_session_decode(void *session, const uint8_t *data, uint64_t length, uint32_t flags, uint8_t *text_out, uint64_t text_capacity, void *output) {
  (void)session;
  if (!td_aligned(output)) return 3;
  TdResult *result = output;
  memset(result, 0, sizeof *result);
  if (!data || (flags & ~15u)) return 3;
  int text = flags & 1, validate_only = flags & 8;
  if (validate_only && text) return 3;
  if (text ? !text_out || ((uintptr_t)text_out % 2) || text_capacity > INT32_MAX : (text_out || text_capacity)) return 3;
  int utf16 = flags & 2, latin1 = flags & 4;
  if (utf16 && (latin1 || length % 2 || ((uintptr_t)data % 2))) return 3;
  result->owner = (void *)1;
  result->words[0] = length;
  return 0;
}
int32_t td_decode_adopted(const uint8_t *data, uint64_t length, uint64_t capacity, uint64_t max_input_bytes, void *output) {
  if (!td_aligned(output)) return 3;
  TdResult *result = output;
  if (result->owner || result->view) return 3;
  memset(result, 0, sizeof *result);
  if (!data || capacity < length) return 3;
  if (length > max_input_bytes) return 4;
  result->owner = (void *)data;
  result->words[0] = length;
  return 0;
}
void td_release(void *output) { if (td_aligned(output)) memset(output, 0, sizeof(TdResult)); }
// Batches: each digest folds the count, then every element's length and bytes (or NULL_ELEMENT for
// an absent one), so the caller's expected value proves the native side saw exactly the bytes it
// lent. Null arrays with count zero are the empty batch, folded as ABSENT; any other null is an
// error, reported as UINT64_MAX.
#define BATCH_OFFSET 0xcbf29ce484222325ULL
#define BATCH_ABSENT (1ULL << 32)
#define BATCH_NULL_ELEMENT (UINT64_MAX - 1)
static uint64_t batch_mix(uint64_t hash, uint64_t value) { return (hash ^ value) * 0x100000001b3ULL; }
static int batch_fold(uint64_t *hash, uint8_t *const *sources, const void *lengths, int width, uint64_t count, int nullable) {
  if (!sources || !lengths) {
    if (sources || lengths || count) return 0;
    *hash = batch_mix(*hash, BATCH_ABSENT);
    return 1;
  }
  *hash = batch_mix(*hash, count);
  for (uint64_t i = 0; i < count; i++) {
    uint64_t length = width == 8 ? ((const uint64_t *) lengths)[i] : ((const uint32_t *) lengths)[i];
    if (!sources[i]) {
      if (!nullable || length) return 0;
      *hash = batch_mix(*hash, BATCH_NULL_ELEMENT);
      continue;
    }
    *hash = batch_mix(*hash, length);
    for (uint64_t k = 0; k < length; k++) *hash = batch_mix(*hash, sources[i][k]);
  }
  return 1;
}
uint64_t cells_batch_digest(uint8_t *const *sources, const uint64_t *lengths, uint64_t count) {
  uint64_t hash = BATCH_OFFSET;
  return batch_fold(&hash, sources, lengths, 8, count, 0) ? hash : UINT64_MAX;
}
uint64_t cells_batch_merge(uint8_t *const *inputs, const uint32_t *input_lengths, uint32_t input_count,
                           uint8_t *const *deps, const int64_t *dep_lengths, int32_t dep_count) {
  uint64_t hash = BATCH_OFFSET;
  if (dep_count < 0) return UINT64_MAX;
  if (!batch_fold(&hash, inputs, input_lengths, 4, input_count, 0)) return UINT64_MAX;
  if (!batch_fold(&hash, deps, dep_lengths, 8, (uint64_t) dep_count, 1)) return UINT64_MAX;
  return hash;
}
