#include <myna/values.hpp>
#include <algorithm>
#include <cstdlib>
#include <cassert>
#include <cstdint>
#include <string>

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

// Check published shape independently of the producer's mutation algorithm.
static uint64_t visit(const myna_value_view &view, uint64_t index, unsigned depth = 0) {
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
      cursor = visit(view, cursor, depth + 1);
    }
    assert(cursor == n.next);
    return cursor;
  }
  if (n.tag & MYNA_VALUE_REGION_TEXT) {
    auto region = myna_value_region_id(&n);
    auto offset = myna_value_offset(&n);
    assert(region == n.payload >> 32 && offset == (n.payload & UINT32_MAX));
    assert(region <= view.region_count);
    assert(offset + n.length <= (region ? view.regions[region - 1].length : view.byte_count));
  }
  if (myna_value_inline(&n)) {
    for (uint32_t i = 0; i < n.length; ++i)
      assert(myna_value_inline_byte(&n, i) == (kind == MYNA_VALUE_DECIMAL ? "123"[i] : 'a'));
  }
  return myna_value_next(&n, index);
}

extern "C" int LLVMFuzzerTestOneInput(const uint8_t *data, size_t size) {
  std::string backing(64, 'a');
  bounded_resource resource(256 + (size ? data[0] : 255) * 64,
      size > 1 && (data[1] & 1) ? data[1] / 2 : SIZE_MAX);
  {
  myna::value_builder builder(&resource, {64, 256, 8});
  for (size_t i = 0; i < size && i < 1024; ++i) {
    unsigned operation = data[i] % 17;
    try {
      switch (operation) {
        case 0: builder.begin_array(); break;
        case 1: builder.begin_object(); break;
        case 2: builder.end(); break;
        case 3: builder.null(); break;
        case 4: builder.integer(data[i]); break;
        case 5: builder.string(std::string_view(backing).substr(0, data[i] % 64)); break;
        case 6: builder.region(backing); break;
        case 7: builder.string_ref(data[i] % 3, data[i] % 65, data[i] % 32); break;
        case 8: builder.clear(); break;
        case 9: { auto v = builder.view(); assert(visit(v, 0) == v.node_count); break; }
        case 10: builder.validated_number("-1234567890123456789.000e-123"); break;
        case 11: builder.boolean(data[i] & 1); break;
        case 12: builder.floating(data[i] / 2.0); break;
        case 13: builder.unsigned_integer(UINT64_MAX); break;
        case 14: builder.floating(std::numeric_limits<double>::infinity()); break;
        case 15: { myna::value_builder invalid(nullptr); std::abort(); }
        case 16: builder.validated_number("123"); break;
      }
    } catch (const std::exception &) {
      // Failed mutations poison until clear; view() may reject incompleteness.
      // A failed view or independent invalid constructor does not mutate builder.
      if (operation != 9 && operation != 15) {
        bool rejected = false;
        try { (void)builder.view(); } catch (const std::logic_error &) { rejected = true; }
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
  resource.budget += sizeof(myna_value_node);
  resource.fail_after = SIZE_MAX;
  builder.clear(); builder.null();
  assert(builder.view().node_count == 1);
  assert(resource.peak <= resource.budget);
  }
  assert(resource.live == 0 && resource.allocated == resource.freed);
  return 0;
}
