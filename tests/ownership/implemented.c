// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
// The C caller side of the ownership fixture, linked against the Rust implementation written only
// against the generated trait (implementation.rs).
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>

#include "seam.h"

#define ABSENT (UINT64_C(1) << 32)
#define CHECK(condition)                                     \
  do {                                                       \
    if (!(condition)) {                                      \
      fprintf(stderr, "failed: %s:%d %s\n", __FILE__, __LINE__, #condition); \
      return 1;                                              \
    }                                                        \
  } while (0)

#include <string.h>

/*
 * Contract violations, one per run: a debug build of the implementation asserts (and aborts, since
 * a panic cannot leave an extern "C" function); a release build avoids undefined behavior instead,
 * by reading an empty slice or ignoring a null destroy. A null query aborts in both.
 */
static int violate(const char *which) {
  uint8_t bytes[3] = {1, 2, 3};
  if (strcmp(which, "length") == 0) {
    // A u64 length past isize::MAX, as a raw Java caller's -1 would be.
    printf("checksum %llu\n", (unsigned long long)buffer_checksum(bytes, UINT64_MAX));
  } else if (strcmp(which, "null-buffer") == 0) {
    printf("checksum %llu\n", (unsigned long long)buffer_checksum(NULL, 0));
  } else if (strcmp(which, "destroy") == 0) {
    ticket_destroy(NULL);
    puts("destroy returned");
  } else if (strcmp(which, "err-success") == 0) {
    // The implementation returns Err with a success status: no handle to report it with.
    struct Surface *slot = (struct Surface *)0xdeadbeef;
    printf("status %d\n", surface_open(-7, &slot, NULL, 0));
  } else if (strcmp(which, "query") == 0) {
    printf("value %llu\n", (unsigned long long)parsed_value(NULL));
  } else {
    return 2;
  }
  return 0;
}

static uint64_t mix(uint64_t hash, uint64_t value) { return (hash ^ value) * UINT64_C(0x100000001b3); }
static uint64_t fold(uint64_t hash, const uint8_t *bytes, uint64_t length) {
  hash = mix(hash, length);
  for (uint64_t i = 0; i < length; i++) hash = mix(hash, bytes[i]);
  return hash;
}

int main(int argc, char **argv) {
  if (argc > 1) return violate(argv[1]);
  CHECK(ticket_live() == 0);
  struct Ticket *ticket = ticket_create();
  CHECK(ticket != NULL && ticket_live() == 1);
  ticket_destroy(ticket);
  CHECK(ticket_live() == 0);
  ticket_fail_next();
  CHECK(ticket_create() == NULL);

  uint8_t bytes[3] = {1, 2, 255};
  CHECK(buffer_checksum(bytes, 3) == 258);
  buffer_increment(bytes, 3);
  CHECK(bytes[0] == 2 && bytes[1] == 3 && bytes[2] == 0);
  CHECK(buffer_checksum(bytes, 0) == 0);
  buffer_increment(bytes, 0);
  uint8_t output[2] = {0, 0};
  uint64_t copied = UINT64_MAX;
  CHECK(buffer_transfer(bytes, 3, output, 2, &copied) == 5 && copied == 2);
  CHECK(output[0] == 2 && output[1] == 3);
  CHECK(buffer_transfer(bytes, 3, output, 0, NULL) == 0);
  // A nullable buffer: null is None, anything else is a slice, empty or not.
  uint8_t text[2] = {0, 0};
  CHECK(buffer_decode(bytes, 3, text, 2) == 3 && text[0] == 3 && text[1] == 4);
  CHECK(buffer_decode(bytes, 3, NULL, 0) == (3 | ABSENT));
  CHECK(buffer_decode(bytes, 3, text, 0) == 3);
  CHECK(buffer_tally(bytes, 3) == 5);
  CHECK(buffer_tally(NULL, 0) == ABSENT);

  // Two constructors of one resource, each reading its buffer during the call only.
  uint8_t source[3] = {1, 2, 3};
  struct Parsed *parsed = parsed_parse(source, 3);
  CHECK(parsed != NULL);
  source[0] = 100;
  CHECK(parsed_value(parsed) == 6);
  CHECK(parsed_parse(NULL, 0) == NULL);
  struct Parsed *analyzed = parsed_analyze(source, 3, 2);
  CHECK(analyzed != NULL && parsed_value(analyzed) == 210);
  CHECK(ticket_live() == 2);
  ticket_fail_next();
  CHECK(parsed_analyze(source, 3, 1) == NULL);
  parsed_release(parsed);
  parsed_release(analyzed);
  // The release accepts null as a no-op.
  parsed_release(NULL);
  CHECK(ticket_live() == 0);
  // Status plus out-pointer stays raw in the trait; the C caller sees the same ABI.
  char error[32] = {0};
  struct Surface *surface = NULL;
  CHECK(surface_open(7, &surface, (uint8_t *)error, sizeof error) == 0 && surface_width(surface) == 7);
  CHECK(surface_open(-1, &surface, (uint8_t *)error, sizeof error) == 3);
  CHECK(error[0] == 'n' && error[13] == 'h');
  surface_close(surface);
  // A null slot (the parameter is nullable): the implementation's box is dropped, nothing leaks.
  CHECK(surface_open(5, NULL, NULL, 0) == 0);
  CHECK(ticket_live() == 0);
  uint8_t pair[2] = {2, 3};
  CHECK(surface_from(pair, 2, &surface, NULL, 0) == 0 && surface_width(surface) == 5);
  surface_close(surface);
  CHECK(surface_titled((const uint8_t *)"abcd", &surface, NULL, 0) == 0 && surface_width(surface) == 4);
  surface_close(surface);
  CHECK(title_length((const uint8_t *)"hello") == 5);
  // Batches reach the implementation as BufferBatch views of exactly the bytes lent.
  uint8_t first[2] = {1, 2}, second[1] = {3};
  uint8_t *parts[2] = {first, second};
  uint64_t lengths[2] = {2, 1};
  uint64_t expected = fold(fold(mix(UINT64_C(0xcbf29ce484222325), 2), first, 2), second, 1);
  CHECK(buffer_batch_digest(parts, lengths, 2) == expected);
  CHECK(buffer_batch_digest(NULL, NULL, 0) == mix(UINT64_C(0xcbf29ce484222325), 0));
  int32_t narrow[2] = {2, 1};
  struct Bundle *bundle = bundle_create(parts, narrow, 2);
  CHECK(bundle != NULL && bundle_value(bundle) == expected && ticket_live() == 1);
  bundle_release(bundle);
  CHECK(ticket_live() == 0);
  puts("Rust implementation trait passed");
  return 0;
}
