#ifndef MYNA_VALUES_H
#define MYNA_VALUES_H
#include <stddef.h>
#include <stdint.h>

/* Myna value ABI 3. Integer fields are native-endian, 64-bit hosts only.
 * Descriptors are borrowed: owners keep every allocation immutable and live.
 * No retain/release policy, allocator, or parser is implied by this header. */
#define MYNA_VALUE_ABI 3u
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
MYNA_STATIC_ASSERT(sizeof(myna_value_view) == 56, "Myna descriptor layout");
MYNA_STATIC_ASSERT(offsetof(myna_value_view, nodes) == 8, "Myna nodes");
MYNA_STATIC_ASSERT(offsetof(myna_value_view, node_count) == 16, "Myna node count");
MYNA_STATIC_ASSERT(offsetof(myna_value_view, bytes) == 24, "Myna bytes");
MYNA_STATIC_ASSERT(offsetof(myna_value_view, byte_count) == 32, "Myna byte count");
MYNA_STATIC_ASSERT(offsetof(myna_value_view, regions) == 40, "Myna regions");
MYNA_STATIC_ASSERT(offsetof(myna_value_view, region_count) == 48, "Myna region count");
#undef MYNA_STATIC_ASSERT
#endif
