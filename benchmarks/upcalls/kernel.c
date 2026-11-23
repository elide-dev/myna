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
