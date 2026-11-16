// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
#include <stddef.h>
#include <stdint.h>
#include <stdlib.h>
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

// A negative width fails with status 3 and leaves junk, misaligned for a Box, in the slot.
int32_t pointers_box_open(int64_t width, struct Box **out) {
    if (width < 0) {
        if (out != NULL) *out = (struct Box *)(uintptr_t)0xdead;
        return 3;
    }
    struct Box *box = malloc(sizeof *box);
    if (box == NULL) return 1;
    box->width = width;
    *out = box;
    return 0;
}

void pointers_box_close(struct Box *target) { free(target); }
