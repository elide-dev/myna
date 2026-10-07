// Clang relies on callee extension of narrow returns on x86-64 and Apple arm64; Rust does not.
#include <stdio.h>
#include "seam.h"
int graal_create_isolate(void *params, void **isolate, void **thread);
int graal_tear_down_isolate(void *thread);
int main(void) {
  void *isolate = NULL, *thread = NULL;
  if (graal_create_isolate(NULL, &isolate, &thread) != 0) return 2;
  volatile int16_t negative = -300;
  volatile uint8_t high = 200;
  int signed_result = seam_classify(thread, negative, false);
  int bool_result = seam_is_high(thread, high);
  int unsigned_result = seam_echo(thread, high);
  if (signed_result != -1 || bool_result != 1 || unsigned_result != 200) {
    printf("narrow returns: classify=%d is_high=%d echo=%d\n", signed_result, bool_result, unsigned_result);
    return 1;
  }
  struct Pair scaled = seam_scale(thread, (struct Pair){.left = 3, .right = -4}, 10);
  struct Triple spread = seam_spread(thread, (struct Triple){.a = 100, .b = 2, .c = 3});
  if (scaled.left != 30 || scaled.right != -40 || spread.b != 102 || spread.c != 103) {
    printf("by-value exports: scaled=%lld,%d spread=%lld,%lld\n", (long long)scaled.left, scaled.right,
           (long long)spread.b, (long long)spread.c);
    return 1;
  }
  graal_tear_down_isolate(thread);
  puts("C -> Native Image narrow returns passed");
  return 0;
}
