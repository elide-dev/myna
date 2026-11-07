// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
#ifndef MYNA_VALUES_H
#define MYNA_VALUES_H
#include <stddef.h>
#include <stdint.h>

/* Myna value ABI 3. Integer fields are native-endian, 64-bit hosts only.
 * Descriptors are borrowed: owners keep every allocation immutable and live.
 * No retain/release policy, allocator, or parser is implied by this header. */
#define MYNA_VALUE_ABI 3u
#define MYNA_VALUE_ABI_V4 4u
/* Opt-in descriptor capability; flags=0 retains finite-only F64 semantics. */
#define MYNA_VALUE_ALLOW_NON_FINITE 1u
#define MYNA_VALUE_KNOWN_FLAGS MYNA_VALUE_ALLOW_NON_FINITE
#define MYNA_VALUE_NULL 0u
#define MYNA_VALUE_BOOL 1u
#define MYNA_VALUE_I64 2u
#define MYNA_VALUE_U64 3u
#define MYNA_VALUE_F64 4u
#define MYNA_VALUE_STRING 5u
#define MYNA_VALUE_ARRAY 6u
#define MYNA_VALUE_OBJECT 7u
#define MYNA_VALUE_DECIMAL 8u
#define MYNA_VALUE_REF 9u
#define MYNA_VALUE_KIND_MASK 255u
#define MYNA_VALUE_REGION_TEXT 256u
#define MYNA_VALUE_INLINE_BYTES 16u

typedef struct myna_value_node {
  uint32_t tag;
  uint32_t length;
  uint64_t payload;
  uint64_t next;
} myna_value_node;
typedef struct myna_value_region {
  const uint8_t *data;
  uint64_t length;
} myna_value_region;
typedef struct myna_value_view {
  uint32_t version;
  uint32_t flags;
  const myna_value_node *nodes;
  uint64_t node_count;
  const uint8_t *bytes; /* Region 0: owned text. */
  uint64_t byte_count;
  const myna_value_region *regions; /* Additional regions, IDs 1..region_count. */
  uint64_t region_count;
} myna_value_view;

/* ABI 4 extends the unchanged v3 prefix. Object payload is zero (linear) or
 * directory start + 1. Exactly object.length entries, sorted by unsigned hash
 * then relative key-node offset; duplicates retain first-match semantics.
 * Hash is FNV-1a over decoded UTF-8 key bytes. Metadata is absent (count=0)
 * or exactly one opaque uint32 per node. REF payload names an earlier, fully
 * completed subtree (target.next <= reference index), never an open ancestor. */
typedef struct myna_value_directory_entry {
  uint32_t hash;
  uint32_t key_offset;
} myna_value_directory_entry;
typedef struct myna_value_view_v4 {
  myna_value_view base;
  const myna_value_directory_entry *directory;
  uint64_t directory_count;
  const uint32_t *metadata;
  uint64_t metadata_count;
} myna_value_view_v4;

static inline uint32_t myna_value_hash(const uint8_t *bytes, size_t length) {
  uint32_t hash = UINT32_C(2166136261);
  for (size_t i = 0; i < length; ++i)
    hash = (hash ^ bytes[i]) * UINT32_C(16777619);
  return hash;
}

static inline uint32_t myna_value_kind(const myna_value_node *n) {
  return n->tag & MYNA_VALUE_KIND_MASK;
}
static inline int myna_value_inline(const myna_value_node *n) {
  return (n->tag == MYNA_VALUE_STRING || n->tag == MYNA_VALUE_DECIMAL) && n->length <= 16;
}
static inline uint64_t myna_value_next(const myna_value_node *n, uint64_t index) {
  return myna_value_inline(n) ? index + 1 : n->next;
}
static inline uint32_t myna_value_region_id(const myna_value_node *n) {
  return (uint32_t)(n->payload >> 32);
}
static inline uint32_t myna_value_offset(const myna_value_node *n) { return (uint32_t)n->payload; }
/* Inline text bytes are packed into numeric words in little-endian order,
 * regardless of host endianness. This accessor does not check bounds. */
static inline uint8_t myna_value_inline_byte(const myna_value_node *n, uint32_t index) {
  return (uint8_t)((index < 8 ? n->payload : n->next) >> ((index & 7u) * 8u));
}
typedef struct myna_text_entry {
  uint64_t offset;   /* Byte offset in caller output. */
  uint64_t length;   /* Code units: bytes for Latin-1, u16 units for UTF-16LE. */
  uint32_t encoding; /* 0 non-text, 1 Latin-1, 2 UTF-16LE. */
  uint32_t reserved;
} myna_text_entry;
/* Caller supplies live, disjoint input/output allocations. All strings/keys and
 * decimal tokens are converted, without expanding references. No allocation.
 * Null output/table with zero capacities queries required bytes. Returns bytes,
 * or -1 invalid UTF-8, -2 invalid extents, -3 insufficient capacity. Errors do
 * not modify output. Table capacity is in entries, output capacity in bytes. */
#ifdef __cplusplus
extern "C" {
#endif
int64_t myna_tape_text_v1(const myna_value_view *descriptor, uint8_t *output, uint64_t capacity,
                          myna_text_entry *table, uint64_t table_capacity);
#ifdef __cplusplus
}
#endif
#if defined(__cplusplus)
#define MYNA_STATIC_ASSERT static_assert
#else
#define MYNA_STATIC_ASSERT _Static_assert
#endif
MYNA_STATIC_ASSERT(sizeof(void *) == 8, "Myna values require 64-bit pointers");
MYNA_STATIC_ASSERT(sizeof(myna_value_node) == 24, "Myna node layout");
MYNA_STATIC_ASSERT(offsetof(myna_value_node, payload) == 8, "Myna node payload");
MYNA_STATIC_ASSERT(offsetof(myna_value_node, next) == 16, "Myna node next");
MYNA_STATIC_ASSERT(sizeof(myna_value_region) == 16, "Myna region layout");
MYNA_STATIC_ASSERT(sizeof(myna_value_view_v4) == 88, "Myna v4 descriptor layout");
MYNA_STATIC_ASSERT(offsetof(myna_value_view_v4, directory) == 56, "Myna directory pointer");
MYNA_STATIC_ASSERT(offsetof(myna_value_view_v4, metadata) == 72, "Myna metadata pointer");
MYNA_STATIC_ASSERT(sizeof(myna_text_entry) == 24, "Myna text entry layout");
MYNA_STATIC_ASSERT(sizeof(myna_value_directory_entry) == 8, "Myna directory layout");
MYNA_STATIC_ASSERT(sizeof(myna_value_view) == 56, "Myna descriptor layout");
MYNA_STATIC_ASSERT(offsetof(myna_value_view, nodes) == 8, "Myna nodes");
MYNA_STATIC_ASSERT(offsetof(myna_value_view, node_count) == 16, "Myna node count");
MYNA_STATIC_ASSERT(offsetof(myna_value_view, bytes) == 24, "Myna bytes");
MYNA_STATIC_ASSERT(offsetof(myna_value_view, byte_count) == 32, "Myna byte count");
MYNA_STATIC_ASSERT(offsetof(myna_value_view, regions) == 40, "Myna regions");
MYNA_STATIC_ASSERT(offsetof(myna_value_view, region_count) == 48, "Myna region count");
#undef MYNA_STATIC_ASSERT
#endif
