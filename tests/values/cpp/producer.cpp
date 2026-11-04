#include <myna/values.hpp>
#include <simdjson.h>
#include <cassert>
#include <regex>
#include <iostream>

using consumer = int64_t (*)(void *, const myna_value_view *, int32_t);
namespace {
void append(simdjson::ondemand::value value, myna::value_builder &builder);
void object(simdjson::ondemand::object obj, myna::value_builder &builder) {
  builder.begin_object();
  for (auto field : obj) {
    auto key = field.unescaped_key().value();
    builder.string(key); // Small keys inline; long keys copy under the builder's PMR.
    append(field.value(), builder);
  }
  builder.end();
}
void append(simdjson::ondemand::value value, myna::value_builder &builder) {
  using simdjson::ondemand::json_type;
  switch (value.type().value()) {
    case json_type::object: object(value.get_object().value(), builder); break;
    case json_type::array:
      builder.begin_array();
      for (auto item : value.get_array()) append(item.value(), builder);
      builder.end(); break;
    case json_type::string: {
      auto text = value.get_string().value();
      // get_string may point into parser-owned unescape storage. Keep the
      // parser live and unreused until the entire synchronous consumer returns.
      auto region = builder.region(text);
      builder.string_ref(region, 0, static_cast<uint32_t>(text.size()));
      break;
    }
    case json_type::number: {
      auto token = value.raw_json_token();
      while (!token.empty() && (token.back() == ' ' || token.back() == '\n' || token.back() == '\r' || token.back() == '\t')) token.remove_suffix(1);
      // Conformance fixture only: raw token access does not validate number
      // grammar. Preserve arbitrary precision using independent lexical checking
      // instead of converting through simdjson's bounded machine number API.
      static const std::regex number(R"(-?(0|[1-9][0-9]*)(\.[0-9]+)?([eE][+-]?[0-9]+)?)");
      if (!std::regex_match(token.begin(), token.end(), number)) throw std::invalid_argument("invalid number");
      auto region = builder.region(token);
      builder.validated_number_ref(region, 0, static_cast<uint32_t>(token.size()));
      break;
    }
    case json_type::boolean: builder.boolean(value.get_bool().value()); break;
    case json_type::null:
      if (!value.is_null().value()) throw std::invalid_argument("invalid null");
      builder.null(); break;
    default: throw std::invalid_argument("unknown JSON type");
  }
}
}
extern "C" int64_t myna_cpp_values(consumer call, void *isolate) noexcept {
  try {
    simdjson::ondemand::parser parser;
    simdjson::padded_string input(std::string_view(R"({"name":"C++ 🦀","escaped":"line\nend","count":18446744073709551615,"exact":1.234567890123456789012345678901,"tiny":1e-999999999999,"nested":[null,true,-0]})"));
    std::pmr::monotonic_buffer_resource arena;
    myna::value_builder builder(&arena);
    auto doc = parser.iterate(input);
    object(doc.get_object().value(), builder);
    auto view = builder.view();
    assert(view.region_count >= 5);
    assert(view.byte_count == 0); // All keys inline; all text/number values borrowed.
    auto result = call(isolate, &view, 100);
    return result;
  } catch (...) { return -800; }
}

#ifdef MYNA_STANDALONE_TEST
struct counting_resource : std::pmr::memory_resource {
  size_t live = 0;
  void *do_allocate(size_t n, size_t alignment) override { auto p = std::pmr::new_delete_resource()->allocate(n, alignment); ++live; return p; }
  void do_deallocate(void *p, size_t n, size_t alignment) override { --live; std::pmr::new_delete_resource()->deallocate(p, n, alignment); }
  bool do_is_equal(const std::pmr::memory_resource &other) const noexcept override { return &other == this; }
};
int main() {
  counting_resource resource;
  {
    myna::value_builder builder(&resource);
    std::string backing = "borrowed";
    auto region = builder.region(backing);
    builder.begin_array(); builder.string_ref(region, 0, 8); builder.validated_number("123.4500e+7"); builder.end();
    auto view = builder.view();
    assert(view.byte_count == 0 && view.region_count == 1);
    assert(view.regions[0].data == reinterpret_cast<const uint8_t *>(backing.data()));
    assert(view.nodes[1].tag == (MYNA_VALUE_STRING | MYNA_VALUE_REGION_TEXT));
    assert(myna_value_inline(&view.nodes[2]));
    assert(myna_value_next(&view.nodes[2], 2) == 3);
    builder.clear();
    bool failed = false;
    try { builder.string_ref(1, 0, 8); } catch (const std::out_of_range &) { failed = true; }
    assert(failed);
    failed = false; try { (void)builder.view(); } catch (const std::logic_error &) { failed = true; } assert(failed);
    builder.clear(); builder.begin_object(); builder.string("key");
    failed = false; try { builder.end(); } catch (const std::logic_error &) { failed = true; } assert(failed);
    builder.clear(); builder.floating(1.5); assert(builder.view().nodes[0].tag == MYNA_VALUE_F64);
  }
  assert(resource.live == 0);
  myna::value_builder failed(std::pmr::null_memory_resource());
  bool oom = false; try { failed.null(); } catch (const std::bad_alloc &) { oom = true; } assert(oom);
  auto callback = +[](void *, const myna_value_view *v, int32_t mode) -> int64_t {
    assert(mode == 100 && v->version == 3 && v->nodes[0].length == 6);
    for (uint64_t i = 0; i < v->node_count; ++i) assert(myna_value_next(&v->nodes[i], i) <= v->node_count);
    return 100;
  };
  assert(myna_cpp_values(callback, nullptr) == 100);
  std::cout << "C++ direct producer and simdjson conformance passed\n";
}
#endif
