// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
#include "seam.h"
#include <pthread.h>
#include <time.h>
#include <stdlib.h>

static int64_t now(void) {
  struct timespec ts;
  if (clock_gettime(CLOCK_MONOTONIC, &ts)) abort();
  return (int64_t)ts.tv_sec * 1000000000 + ts.tv_nsec;
}

static int64_t control(int64_t previous, int64_t increment) {
  return previous + increment;
}

struct sample { Reducer target; int32_t count; int64_t elapsed; };

static void *loop(void *arg) {
  struct sample *s = arg;
  int64_t result = 0;
  int64_t start = now();
  for (int32_t i = 0; i < s->count; ++i) result = s->target(result, 1);
  s->elapsed = now() - start;
  if (result != s->count) abort();
  return NULL;
}

static void run_round(void *(*task)(void *), void *args, bool worker) {
  if (worker) {
    pthread_t thread;
    if (pthread_create(&thread, NULL, task, args) || pthread_join(thread, NULL)) abort();
  } else {
    task(args);
  }
}

int64_t myna_measure(Reducer target, bool worker, int32_t count) {
  struct sample s = {target ? target : control, count, 0};
  run_round(loop, &s, worker);
  return s.elapsed;
}

int64_t myna_measure_isolate(IsolateReducer target, void *thread, int32_t count) {
  int64_t result = 0;
  int64_t start = now();
  for (int32_t i = 0; i < count; ++i) result = target(thread, result, 1);
  int64_t elapsed = now() - start;
  if (result != count) abort();
  return elapsed;
}

struct hook_sample { Hook target; int32_t count; int64_t elapsed; };

static void *hook_loop(void *arg) {
  struct hook_sample *s = arg;
  int64_t start = now();
  for (int32_t i = 0; i < s->count; ++i) s->target();
  s->elapsed = now() - start;
  return NULL;
}

int64_t myna_measure_hook(Hook target, bool worker, int32_t count) {
  struct hook_sample s = {target, count, 0};
  run_round(hook_loop, &s, worker);
  return s.elapsed;
}

struct guest_sample { struct hook_sample hook; EnterGuest enter; LeaveGuest leave; };

static void *guest_loop(void *arg) {
  struct guest_sample *s = arg;
  if (s->enter) s->enter();
  hook_loop(&s->hook);
  if (s->leave) s->leave();
  return NULL;
}

int64_t myna_measure_guest(GuestHook target, EnterGuest enter, LeaveGuest leave,
                           bool worker, int32_t count) {
  struct guest_sample s = {{target, count, 0}, enter, leave};
  run_round(guest_loop, &s, worker);
  return s.hook.elapsed;
}

struct reducer_sample { Reducer target; EnterGuest enter; LeaveGuest leave; int32_t count; int64_t elapsed; };

static void *guest_reducer_loop(void *arg) {
  struct reducer_sample *s = arg;
  int64_t result = INT64_C(0x100000031);
  s->enter();
  int64_t start = now();
  for (int32_t i = 0; i < s->count; ++i) {
    result = s->target(result, 1 + (i & 7));
  }
  s->elapsed = now() - start;
  s->leave();
  // Check the value C actually received, including the high 32 bits of the carrier.
  int64_t tail = s->count % 8;
  int64_t expected = INT64_C(0x100000031) + INT64_C(36) * (s->count / 8) + tail * (tail + 1) / 2;
  if (result != expected) s->elapsed = -1;
  return NULL;
}

int64_t myna_measure_guest_reducer(Reducer target, EnterGuest enter, LeaveGuest leave,
                                   bool worker, int32_t count) {
  struct reducer_sample s = {target, enter, leave, count, 0};
  run_round(guest_reducer_loop, &s, worker);
  return s.elapsed;
}

struct reducer_check { Reducer target; int64_t previous; int64_t increment; int64_t result; };

static void *check_reducer(void *arg) {
  struct reducer_check *s = arg;
  s->result = s->target(s->previous, s->increment);
  return NULL;
}

int64_t myna_check_reducer(Reducer target, int64_t previous, int64_t increment, bool worker) {
  struct reducer_check s = {target, previous, increment, 0};
  run_round(check_reducer, &s, worker);
  return s.result;
}
