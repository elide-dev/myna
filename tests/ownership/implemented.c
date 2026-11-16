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

int main(void) {
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
  // A null buffer with length zero is an empty slice, not undefined behavior.
  CHECK(buffer_checksum(NULL, 0) == 0);
  buffer_increment(NULL, 0);
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
  puts("Rust implementation trait passed");
  return 0;
}
