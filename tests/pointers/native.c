// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
#include <stddef.h>
#include "seam.h"

static char handle;
static char name[] = "head";
static struct Node second;
static struct Leaf leaf;
static struct Node head = {
    .value = 1,
    .next = &second,
    .leaf = &leaf,
    .handle = (struct Handle *)&handle,
    .name = name,
};
static struct Node second = {.value = 2};
static struct Leaf leaf = {.weight = 2.5, .owner = &head};
static struct Anchor anchor = {.first = &head};

struct Node *pointers_head(void) { return &head; }

struct Node *pointers_none(void) { return NULL; }

double pointers_weigh(struct Leaf const *l) { return l->weight * (double)l->owner->value; }

struct Anchor *pointers_anchor(void) { return &anchor; }

int64_t pointers_visit(Visit callback) { return callback(&head); }

int64_t pointers_sum(struct Pair pair) { return pair.left + pair.right; }
