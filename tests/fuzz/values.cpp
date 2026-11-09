// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
#include <myna/values.hpp>
#include <algorithm>
#include <cstdlib>
#include <cassert>
#include <cstdint>
#include <string>
#include <string_view>
#include <vector>

// Bound actual retained PMR storage, not only logical node/text counts. Inject
// failures after successful allocations so cleanup exercises partial documents.
struct bounded_resource final : std::pmr::memory_resource {
  size_t live = 0, peak = 0, allocated = 0, freed = 0;
  size_t budget, fail_after;
  bounded_resource(size_t limit, size_t fail) : budget(limit), fail_after(fail) {}
  void *do_allocate(size_t size, size_t alignment) override {
    if (allocated >= fail_after || size > budget - live) throw std::bad_alloc();
    void *pointer = std::pmr::new_delete_resource()->allocate(size, alignment);
    live += size; peak = std::max(peak, live); ++allocated;
    return pointer;
  }
  void do_deallocate(void *pointer, size_t size, size_t alignment) override {
    assert(size <= live);
    live -= size; ++freed;
    std::pmr::new_delete_resource()->deallocate(pointer, size, alignment);
  }
  bool do_is_equal(const std::pmr::memory_resource &other) const noexcept override { return this == &other; }
};

// Independent FNV-1a 32 reference, so a change to myna_value_hash (which the
// builder now uses for directories) cannot drift unnoticed. 64-bit arithmetic
// reduced mod 2^32 never wraps, so it needs no sanitizer exemption.
static uint32_t reference_fnv1a(std::string_view bytes) {
  uint64_t hash = UINT64_C(2166136261);
  for (char c : bytes) hash = ((hash ^ static_cast<unsigned char>(c)) * UINT64_C(16777619)) & UINT32_MAX;
  return static_cast<uint32_t>(hash);
}

// Text bytes as published, read through the ABI rather than builder internals.
static std::string_view text(const myna_value_view &view, const myna_value_node &n, char (&scratch)[16]) {
  if (myna_value_inline(&n)) {
    assert(n.length <= MYNA_VALUE_INLINE_BYTES);
    for (uint32_t i = 0; i < MYNA_VALUE_INLINE_BYTES; ++i) {
      auto b = static_cast<char>(myna_value_inline_byte(&n, i));
      if (i < n.length) scratch[i] = b;
      else assert(b == 0); // Unused inline bytes are zero.
    }
    return {scratch, n.length};
  }
  assert(n.tag & MYNA_VALUE_REGION_TEXT);
  auto region = myna_value_region_id(&n);
  auto offset = myna_value_offset(&n);
  assert(region == n.payload >> 32 && offset == (n.payload & UINT32_MAX));
  assert(region <= view.region_count);
  auto extent = region ? view.regions[region - 1].length : view.byte_count;
  assert(offset <= extent && n.length <= extent - offset);
  if (!n.length) return {};
  auto base = region ? view.regions[region - 1].data : view.bytes;
  return {reinterpret_cast<const char *>(base) + offset, n.length};
}

// Check published shape independently of the producer's mutation algorithm.
// `texts` is the caller's record of every accepted STRING/DECIMAL write, in
// append order; text nodes are visited in that same pre-order.
struct expected_text {
  const std::vector<std::string_view> *texts = nullptr;
  size_t cursor = 0;
};
static uint64_t visit(const myna_value_view &view, uint64_t index, expected_text &expected, unsigned depth = 0) {
  assert(index < view.node_count && depth <= 8);
  const auto &n = view.nodes[index];
  auto kind = myna_value_kind(&n);
  assert(kind == (n.tag & MYNA_VALUE_KIND_MASK));
  if (kind == MYNA_VALUE_ARRAY || kind == MYNA_VALUE_OBJECT) {
    uint64_t cursor = index + 1;
    for (uint64_t i = 0; i < uint64_t(n.length) * (kind == MYNA_VALUE_OBJECT ? 2 : 1); ++i) {
      assert(cursor < view.node_count);
      if (kind == MYNA_VALUE_OBJECT && i % 2 == 0)
        assert((view.nodes[cursor].tag & MYNA_VALUE_KIND_MASK) == MYNA_VALUE_STRING);
      cursor = visit(view, cursor, expected, depth + 1);
    }
    assert(cursor == n.next);
    return cursor;
  }
  if (kind == MYNA_VALUE_REF) {
    assert(view.version == 4 && n.payload < index);
    assert(myna_value_next(&view.nodes[n.payload], n.payload) <= index);
  }
  if (kind == MYNA_VALUE_STRING || kind == MYNA_VALUE_DECIMAL) {
    // The producer is byte-transparent: arbitrary (including invalid UTF-8 and
    // malformed number) bytes must be published exactly as written.
    char scratch[16];
    auto published = text(view, n, scratch);
    if (expected.texts) {
      assert(expected.cursor < expected.texts->size());
      assert(published == (*expected.texts)[expected.cursor]);
      ++expected.cursor;
    }
  }
  return myna_value_next(&n, index);
}
extern "C" int LLVMFuzzerInitialize(int *, char ***) {
  bool unknown_rejected = false;
  try { myna::value_builder invalid(std::pmr::get_default_resource(), {}, 2); }
  catch (const std::invalid_argument &) { unknown_rejected = true; }
  assert(unknown_rejected);
  myna::value_builder yaml(std::pmr::get_default_resource(), {}, MYNA_VALUE_ALLOW_NON_FINITE);
  for (uint64_t bits : {UINT64_C(0x7ff0000000000000), UINT64_C(0xfff0000000000000),
                        UINT64_C(0x7ff8000000000042), UINT64_C(0x8000000000000000)}) {
    double value;
    std::memcpy(&value, &bits, sizeof(value));
    yaml.clear();
    yaml.floating(value);
    auto view = yaml.view();
    assert(view.flags == MYNA_VALUE_ALLOW_NON_FINITE && view.nodes[0].payload == bits);
  }
  // Deterministic boundaries that random valid documents rarely reach.
  myna::value_builder empty(std::pmr::get_default_resource(), {0, 256, 8});
  bool rejected = false;
  try { empty.null(); } catch (const std::length_error &) { rejected = true; }
  assert(rejected);
  rejected = false;
  try { empty.view(); } catch (const std::logic_error &) { rejected = true; }
  assert(rejected);

  const std::string token = "-123456789.000e-12";
  myna::value_builder decimal;
  auto region = decimal.region(token);
  decimal.validated_number_ref(region, 0, token.size());
  auto view = decimal.view();
  assert(view.node_count == 1 && myna_value_kind(view.nodes) == MYNA_VALUE_DECIMAL);
  assert(myna_value_region_id(view.nodes) == region);
  assert(view.nodes[0].length == token.size());
  assert(std::memcmp(view.regions[region - 1].data, token.data(), token.size()) == 0);
  // Deterministic extension cases complement mutation of malformed protocols.
  myna::value_builder indexed;
  indexed.enable_v4(1, true);
  indexed.reserve(32, 128, 2, 4);
  auto borrowed = indexed.region("borrowed long object key");
  indexed.begin_array(); indexed.begin_object();
  indexed.string("costarring"); indexed.integer(1);
  indexed.string("liquid"); indexed.null();
  indexed.string("owned long object key"); indexed.boolean(true);
  indexed.string_ref(borrowed, 0, 24); indexed.null();
  indexed.end(); indexed.metadata(1, 42); indexed.ref(1); indexed.end();
  auto extended = indexed.view_v4();
  assert(extended.directory_count == 4 && extended.metadata_count == 11);
  assert(extended.metadata[1] == 42 && extended.base.nodes[10].payload == 1);
  // Published FNV-1a 32 vectors, shared with the Rust hash_utf8 test.
  for (auto [text, expected] : {std::pair<std::string_view, uint32_t>{"", UINT32_C(0x811c9dc5)},
                                {"a", UINT32_C(0xe40c292c)}, {"foobar", UINT32_C(0xbf9cf968)},
                                {"hello", UINT32_C(0x4f9f2cab)}}) {
    assert(myna_value_hash(reinterpret_cast<const uint8_t *>(text.data()), text.size()) == expected);
    assert(reference_fnv1a(text) == expected);
  }
  rejected = false; try { indexed.view(); } catch (const std::logic_error &) { rejected = true; } assert(rejected);
  rejected = false; try { decimal.view_v4(); } catch (const std::logic_error &) { rejected = true; } assert(rejected);
  rejected = false; try { indexed.enable_v4(); } catch (const std::logic_error &) { rejected = true; } assert(rejected);
  indexed.clear(); indexed.begin_array();
  rejected = false; try { indexed.ref(0); } catch (const std::invalid_argument &) { rejected = true; } assert(rejected);
  indexed.clear(); indexed.enable_v4(0, false); indexed.null();
  assert(indexed.view_v4().metadata_count == 0);
  return 0;
}

extern "C" int LLVMFuzzerTestOneInput(const uint8_t *data, size_t size) {
  // Text and region contents come from the input that follows each operation
  // byte, so arbitrary bytes (invalid UTF-8, malformed or oversized number
  // tokens, NULs) reach string(), validated_number() and borrowed regions. The
  // C++ producer documents both as caller-validated contracts and performs no
  // Unicode or lexical scan: it must store such bytes exactly, and reject only
  // extent, limit, allocation and protocol violations (poisoning until clear).
  // UTF-8 and number rejection live in the Rust producer and in consumers
  // (myna_tape_text_v1, the SVM reader); see crates/myna-values/fuzz.
  auto payload = [&](size_t i, size_t length) {
    size_t start = std::min(size, i + 1);
    return std::string_view(reinterpret_cast<const char *>(data) + start, std::min(length, size - start));
  };
  // Every accepted text write and region, in append order. The fuzz input
  // outlives the builder, so regions may borrow it directly.
  std::vector<std::string_view> texts, regions;
  auto accept = [&](std::string_view value) { texts.push_back(value); };
  auto borrowed = [&](uint32_t region, uint32_t offset, uint32_t length) {
    assert(region >= 1 && region <= regions.size());
    return regions[region - 1].substr(offset, length);
  };
  bounded_resource resource(256 + (size ? data[0] : 255) * 64,
      size > 1 && (data[1] & 1) ? data[1] / 2 : SIZE_MAX);
  {
  myna::value_builder builder(&resource, {64, 256, 8},
      size > 2 && (data[2] & 1) ? MYNA_VALUE_ALLOW_NON_FINITE : 0);
  bool v4 = size > 3 && (data[3] & 1);
  if (v4) builder.enable_v4(2, true);
  auto view = [&]() {
    if (!v4) return builder.view();
    auto extended = builder.view_v4();
    assert(extended.metadata_count == extended.base.node_count);
    for (uint64_t i = 0; i < extended.base.node_count; ++i) {
      const auto &node = extended.base.nodes[i];
      if (node.tag != MYNA_VALUE_OBJECT || !node.payload) continue;
      uint64_t start = node.payload - 1;
      assert(start <= extended.directory_count && node.length <= extended.directory_count - start);
      for (uint64_t j = start; j < start + node.length; ++j) {
        const auto &entry = extended.directory[j];
        assert(entry.key_offset > 0 && entry.key_offset < node.next - i);
        const auto &key = extended.base.nodes[i + entry.key_offset];
        assert(myna_value_kind(&key) == MYNA_VALUE_STRING);
        char scratch[16];
        auto bytes = text(extended.base, key, scratch);
        assert(entry.hash == reference_fnv1a(bytes));
        if (j > start) {
          const auto &prior = extended.directory[j-1];
          assert(prior.hash < entry.hash || (prior.hash == entry.hash && prior.key_offset < entry.key_offset));
        }
      }
    }
    return extended.base;
  };
  for (size_t i = 0; i < size && i < 1024; ++i) {
    unsigned operation = data[i] % 20;
    try {
      switch (operation) {
        case 0: builder.begin_array(); break;
        case 1: builder.begin_object(); break;
        case 2: builder.end(); break;
        case 3: builder.null(); break;
        case 4: builder.integer(data[i]); break;
        case 5: { auto value = payload(i, data[i] % 64); builder.string(value); accept(value); break; }
        case 6: { auto value = payload(i, data[i] % 65); builder.region(value); regions.push_back(value); break; }
        case 7: {
          uint32_t region = data[i] % 3, offset = data[i] % 65, length = data[i] % 32;
          builder.string_ref(region, offset, length);
          accept(borrowed(region, offset, length));
          break;
        }
        case 8: builder.clear(); texts.clear(); regions.clear(); break;
        case 9: {
          auto v = view();
          expected_text expected{&texts};
          assert(visit(v, 0, expected) == v.node_count && expected.cursor == texts.size());
          break;
        }
        case 10: { auto value = payload(i, data[i] % 40); builder.validated_number(value); accept(value); break; }
        case 11: builder.boolean(data[i] & 1); break;
        case 12: builder.floating(data[i] / 2.0); break;
        case 13: builder.unsigned_integer(UINT64_MAX); break;
        case 14: builder.floating(std::numeric_limits<double>::infinity()); break;
        case 15: { myna::value_builder invalid(nullptr); std::abort(); }
        case 16: {
          uint32_t region = data[i] % 3, offset = data[i] % 17, length = data[i] % 24;
          builder.validated_number_ref(region, offset, length);
          accept(borrowed(region, offset, length));
          break;
        }
        case 17: builder.reserve(data[i] % 70, data[i], data[i] % 8, data[i] % 10); break;
        case 18: builder.ref(data[i] % 64); break;
        case 19: builder.metadata(data[i] % 64, data[i]); break;
      }
    } catch (const std::exception &) {
      // Failed mutations poison until clear; view() may reject incompleteness.
      // A failed view or independent invalid constructor does not mutate builder.
      if (operation != 9 && operation != 15) {
        bool rejected = false;
        try { (void)view(); } catch (const std::logic_error &) { rejected = true; }
        assert(rejected);
        rejected = false;
        try { builder.null(); } catch (const std::logic_error &) { rejected = true; }
        assert(rejected);
      }
    }
    assert(resource.live <= resource.budget);
  }
  // Removing failure injection and clearing must restore a usable document.
  // Retained capacity stays bounded; allow one final node allocation if needed.
  resource.budget += sizeof(myna_value_node) + sizeof(uint32_t);
  resource.fail_after = SIZE_MAX;
  builder.clear(); texts.clear(); regions.clear();
  builder.null();
  assert(view().node_count == 1);
  assert(resource.peak <= resource.budget);
  }
  assert(resource.live == 0 && resource.allocated == resource.freed);
  return 0;
}
