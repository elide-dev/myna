// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
#include <myna/values.hpp>
#include <cassert>
#include <thread>
#include <vector>
int main() {
  myna::value_builder published;
  published.begin_array(); published.integer(42); published.end();
  const auto view = published.view();
  std::vector<std::thread> threads;
  for (int t = 0; t < 4; ++t) threads.emplace_back([view] {
    std::pmr::monotonic_buffer_resource allocator;
    myna::value_builder local(&allocator);
    for (int i = 0; i < 10000; ++i) {
      assert(view.nodes[1].payload == 42);
      local.clear(); local.begin_array(); local.integer(i); local.string("thread-local"); local.end();
      assert(local.view().node_count == 3);
    }
  });
  for (auto &thread : threads) thread.join();
}
