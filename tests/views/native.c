// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
#include "seam.h"

static struct Function functions[2] = {
    {.id = 10, .arity = 1, .entry = 0x1000},
    {.id = 11, .arity = 2, .entry = UINT64_C(0xfffffffff)},
};
static uint16_t text[3] = {'h', 'i', 0xffff};
static struct Function *roots[2] = {&functions[1], &functions[0]};
static struct Program program = {
    .functions = functions,
    .functionCount = 2,
    .text = text,
    .textLength = 3,
    .version = 1,
    .roots = roots,
    .rootCount = 2,
};
static struct Program empty = {0};

struct Program *views_program(void) { return &program; }

struct Program *views_empty(void) { return &empty; }
