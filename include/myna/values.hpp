// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
#ifndef MYNA_VALUES_HPP
#define MYNA_VALUES_HPP
#include "values.h"
#include <algorithm>
#include <cmath>
#include <cstring>
#include <limits>
#include <memory_resource>
#include <stdexcept>
#include <string_view>
#include <vector>

namespace myna {
struct value_limits {
  uint64_t nodes = UINT32_MAX;
  uint64_t text_bytes = UINT32_MAX;
  uint32_t depth = 128;
};

/* Direct C++17 producer with caller-supplied PMR storage. Pass a
 * monotonic_buffer_resource for bump allocation. No Rust or Serde dependency.
 * All views/region IDs are invalidated by clear or further builder mutation.
 * Borrowed region data must remain immutable and live until the consumer is done.
 * Strings are UTF-8 contracts; validated parser output can be written without a
 * redundant Unicode scan. Use simdutf to validate non-parser byte sources first.
 * Errors poison the document until clear(). Never unwind across a C ABI. */
class value_builder {
  struct frame {
    size_t index;
    uint32_t values;
  };
  std::pmr::vector<myna_value_node> nodes_;
  std::pmr::vector<uint8_t> bytes_;
  std::pmr::vector<myna_value_region> regions_;
  std::pmr::vector<frame> stack_;
  std::pmr::vector<myna_value_directory_entry> directory_;
  std::pmr::vector<uint32_t> metadata_;
  bool v4_ = false, has_metadata_ = false;
  uint32_t directory_threshold_ = 8;
  value_limits limits_;
  uint32_t flags_;
  uint64_t text_bytes_ = 0;
  bool root_ = false, failed_ = false;
  static std::pmr::memory_resource *checked(std::pmr::memory_resource *resource) {
    if (!resource)
      throw std::invalid_argument("null Myna allocator");
    return resource;
  }
  void check() const {
    if (failed_)
      throw std::logic_error("Myna builder is poisoned");
  }
  template <class F> void operation(F f) {
    check();
    try {
      f();
    } catch (...) {
      failed_ = true;
      throw;
    }
  }
  void slot(uint32_t kind) {
    if (stack_.empty()) {
      if (root_)
        throw std::logic_error("multiple Myna roots");
      root_ = true;
    } else {
      auto &f = stack_.back();
      if (nodes_[f.index].tag == MYNA_VALUE_OBJECT && f.values % 2 == 0 &&
          kind != MYNA_VALUE_STRING)
        throw std::logic_error("Myna object key must be string");
      if (f.values == UINT32_MAX)
        throw std::length_error("Myna container limit");
      ++f.values;
    }
  }
  size_t push(uint32_t tag, uint32_t len, uint64_t payload) {
    if (nodes_.size() >= limits_.nodes)
      throw std::length_error("Myna node limit");
    size_t index = nodes_.size();
    nodes_.push_back({tag, len, payload, index + 1});
    if (has_metadata_)
      metadata_.push_back(0);
    return index;
  }
  void charge(size_t len) {
    if (len > UINT32_MAX || len > limits_.text_bytes - text_bytes_)
      throw std::length_error("Myna text limit");
    text_bytes_ += len;
  }
  void scalar(uint32_t kind, uint64_t value) {
    operation([&] {
      slot(kind);
      push(kind, 0, value);
    });
  }
  void text(uint32_t kind, std::string_view value) {
    operation([&] {
      slot(kind);
      charge(value.size());
      if (value.size() <= 16) {
        size_t i = push(kind, static_cast<uint32_t>(value.size()), 0);
        auto &n = nodes_[i];
        n.next = 0;
        for (size_t j = 0; j < value.size(); ++j) {
          uint64_t b = static_cast<unsigned char>(value[j]);
          if (j < 8)
            n.payload |= b << (j * 8);
          else
            n.next |= b << ((j - 8) * 8);
        }
      } else {
        if (bytes_.size() > UINT32_MAX)
          throw std::length_error("Myna region offset limit");
        push(kind | MYNA_VALUE_REGION_TEXT, static_cast<uint32_t>(value.size()), bytes_.size());
        bytes_.insert(bytes_.end(), value.begin(), value.end());
      }
    });
  }
  void reference(uint32_t kind, uint32_t region, uint32_t offset, uint32_t len) {
    operation([&] {
      if (region == 0 || region > regions_.size() ||
          uint64_t(offset) + len > regions_[region - 1].length)
        throw std::out_of_range("Myna region extent");
      slot(kind);
      charge(len);
      push(kind | MYNA_VALUE_REGION_TEXT, len, (uint64_t(region) << 32) | offset);
    });
  }
  void begin(uint32_t kind) {
    operation([&] {
      if (stack_.size() >= limits_.depth)
        throw std::length_error("Myna depth limit");
      slot(kind);
      auto index = push(kind, 0, 0);
      stack_.push_back({index, 0});
    });
  }

public:
  explicit value_builder(std::pmr::memory_resource *allocator = std::pmr::get_default_resource(),
                         value_limits limits = {}, uint32_t flags = 0)
      : nodes_(checked(allocator)), bytes_(allocator), regions_(allocator), stack_(allocator),
        directory_(allocator), metadata_(allocator), limits_(limits), flags_(flags) {
    if (flags & ~MYNA_VALUE_KNOWN_FLAGS)
      throw std::invalid_argument("unknown Myna descriptor flags");
  }
  value_builder(const value_builder &) = delete;
  value_builder &operator=(const value_builder &) = delete;
  /* Reserve total capacities, not additional elements. Does not change limits or
   * logical contents. Allocation/limit failures poison until clear(), like writes.
   * Supplying depth avoids stack growth during the subsequent encode. */
  void reserve(size_t nodes, size_t bytes, size_t regions, size_t depth = 0) {
    operation([&] {
      if (nodes > limits_.nodes || bytes > limits_.text_bytes || regions > UINT32_MAX ||
          depth > limits_.depth)
        throw std::length_error("Myna reservation limit");
      nodes_.reserve(nodes);
      bytes_.reserve(bytes);
      regions_.reserve(regions);
      stack_.reserve(depth);
      if (v4_)
        directory_.reserve(nodes);
      if (has_metadata_)
        metadata_.reserve(nodes);
    });
  }
  /* Enable ABI 4 before writing. threshold=0 disables directories. */
  void enable_v4(uint32_t threshold = 8, bool metadata = false) {
    operation([&] {
      if (!nodes_.empty())
        throw std::logic_error("enable ABI 4 before writing");
      v4_ = true;
      directory_threshold_ = threshold;
      has_metadata_ = metadata;
    });
  }
  void metadata(size_t node, uint32_t value) {
    operation([&] {
      if (!has_metadata_ || node >= metadata_.size())
        throw std::out_of_range("Myna metadata index");
      metadata_[node] = value;
    });
  }
  void ref(size_t target) {
    operation([&] {
      if (!v4_ || target >= nodes_.size() ||
          myna_value_next(&nodes_[target], target) > nodes_.size() ||
          std::any_of(stack_.begin(), stack_.end(),
                      [=](const frame &f) { return f.index == target; }))
        throw std::invalid_argument("Myna reference requires a completed earlier value");
      slot(MYNA_VALUE_REF);
      push(MYNA_VALUE_REF, 0, target);
    });
  }
  void clear() {
    nodes_.clear();
    bytes_.clear();
    regions_.clear();
    stack_.clear();
    directory_.clear();
    metadata_.clear();
    text_bytes_ = 0;
    root_ = failed_ = false;
  }
  uint32_t region(std::string_view value) {
    uint32_t id = 0;
    operation([&] {
      if (regions_.size() == UINT32_MAX)
        throw std::length_error("Myna region count");
      // Empty std::string_view may carry nullptr; consumers never dereference it.
      regions_.push_back({reinterpret_cast<const uint8_t *>(value.data()), value.size()});
      id = static_cast<uint32_t>(regions_.size());
    });
    return id;
  }
  void null() { scalar(MYNA_VALUE_NULL, 0); }
  void boolean(bool value) { scalar(MYNA_VALUE_BOOL, value); }
  void integer(int64_t value) { scalar(MYNA_VALUE_I64, static_cast<uint64_t>(value)); }
  void unsigned_integer(uint64_t value) { scalar(MYNA_VALUE_U64, value); }
  void floating(double value) {
    operation([&] {
      if (!(flags_ & MYNA_VALUE_ALLOW_NON_FINITE) && !std::isfinite(value))
        throw std::invalid_argument("non-finite value requires MYNA_VALUE_ALLOW_NON_FINITE");
      uint64_t bits;
      std::memcpy(&bits, &value, sizeof(bits));
      slot(MYNA_VALUE_F64);
      push(MYNA_VALUE_F64, 0, bits);
    });
  }
  void string(std::string_view utf8) { text(MYNA_VALUE_STRING, utf8); }
  void string_ref(uint32_t region, uint32_t offset, uint32_t len) {
    reference(MYNA_VALUE_STRING, region, offset, len);
  }
  /* Number tokens must already have passed RFC 8259 lexical validation. This
   * named operation avoids lossy double conversion and any private parser ABI. */
  void validated_number(std::string_view token) { text(MYNA_VALUE_DECIMAL, token); }
  void validated_number_ref(uint32_t region, uint32_t offset, uint32_t len) {
    reference(MYNA_VALUE_DECIMAL, region, offset, len);
  }
  void begin_array() { begin(MYNA_VALUE_ARRAY); }
  void begin_object() { begin(MYNA_VALUE_OBJECT); }
  void end() {
    operation([&] {
      if (stack_.empty())
        throw std::logic_error("no open Myna container");
      auto f = stack_.back();
      bool object = nodes_[f.index].tag == MYNA_VALUE_OBJECT;
      if (object && f.values % 2)
        throw std::logic_error("missing Myna object value");
      nodes_[f.index].length = object ? f.values / 2 : f.values;
      nodes_[f.index].next = nodes_.size();
      stack_.pop_back();
      if (v4_ && object && directory_threshold_ && nodes_[f.index].length >= directory_threshold_) {
        size_t start = directory_.size();
        for (uint64_t key = f.index + 1; key < nodes_.size();) {
          const auto &n = nodes_[key];
          uint32_t hash = UINT32_C(2166136261);
          for (uint32_t i = 0; i < n.length; ++i) {
            uint8_t b;
            if (myna_value_inline(&n))
              b = myna_value_inline_byte(&n, i);
            else {
              auto region = myna_value_region_id(&n);
              auto data = region ? regions_[region - 1].data : bytes_.data();
              b = data[uint64_t(myna_value_offset(&n)) + i];
            }
            hash = (hash ^ b) * UINT32_C(16777619);
          }
          if (key - f.index > UINT32_MAX)
            throw std::length_error("Myna directory offset");
          directory_.push_back({hash, static_cast<uint32_t>(key - f.index)});
          key = myna_value_next(&nodes_[key + 1], key + 1);
        }
        std::sort(directory_.begin() + start, directory_.end(), [](auto a, auto b) {
          return a.hash < b.hash || (a.hash == b.hash && a.key_offset < b.key_offset);
        });
        nodes_[f.index].payload = start + 1;
      }
    });
  }
  myna_value_view view() const {
    check();
    if (!root_ || !stack_.empty())
      throw std::logic_error("incomplete Myna document");
    if (v4_)
      throw std::logic_error("use view_v4 for ABI 4");
    return {MYNA_VALUE_ABI, flags_,        nodes_.data(),   nodes_.size(),
            bytes_.data(),  bytes_.size(), regions_.data(), regions_.size()};
  }
  myna_value_view_v4 view_v4() const {
    check();
    if (!v4_ || !root_ || !stack_.empty())
      throw std::logic_error("incomplete ABI 4 document");
    return {{MYNA_VALUE_ABI_V4, flags_, nodes_.data(), nodes_.size(), bytes_.data(), bytes_.size(),
             regions_.data(), regions_.size()},
            directory_.data(),
            directory_.size(),
            metadata_.data(),
            metadata_.size()};
  }
};
} // namespace myna
#endif
