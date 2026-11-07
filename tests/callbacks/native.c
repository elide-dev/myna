// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
#include "seam.h"
#include <pthread.h>
#include <stdlib.h>

struct invocation { Reducer callback; int64_t left, right, result; };
static void *invoke_on_thread(void *arg) {
  struct invocation *call = arg;
  call->result = call->callback(call->left, call->right);
  return NULL;
}
int64_t myna_invoke_callback(Reducer callback, bool threaded, int64_t left, int64_t right) {
  struct invocation call = {callback, left, right, 0};
  if (threaded) {
    pthread_t thread;
    if (pthread_create(&thread, NULL, invoke_on_thread, &call) != 0) abort();
    if (pthread_join(thread, NULL) != 0) abort();
  } else {
    invoke_on_thread(&call);
  }
  return call.result;
}

// Widen in C: examining only the returned low byte/short on the Java side would conceal
// a missing zero-extension in the upcall adapter on Apple ARM64.
int32_t myna_invoke_byte(ByteResult callback, bool fail) { return callback(fail); }
int32_t myna_invoke_short(ShortResult callback, bool fail) { return callback(fail); }
void myna_notify(Notify callback, int32_t value) { callback(value); }
uint8_t *myna_invoke_pointer(PointerResult callback, uint8_t *value) { return callback(value); }
float myna_invoke_float(FloatResult callback, float value) { return callback(value); }
double myna_invoke_double(DoubleResult callback, double value) { return callback(value); }
