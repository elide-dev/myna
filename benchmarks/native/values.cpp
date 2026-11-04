#include <myna/values.hpp>
#include <cstdlib>
#include <core.h>
#include <chrono>
#include <iostream>
#include <string>
#include <unistd.h>

static volatile uint64_t observed;
template <class F>
__attribute__((noinline)) void __codspeed_root_frame__values(F &encode) {
  for (int i = 0; i < 1000; ++i) encode();
}
int main(int argc, char **argv) {
  if (argc < 2 || argc > 3) return 2;
  std::string name = argv[1];
  if (name != "inline" && name != "owned" && name != "borrowed") return 2;
  auto hooks = instrument_hooks_init();
  if (!hooks) return 4;
  bool instrumented = instrument_hooks_is_instrumented(hooks);
  if (argc == 3 && std::string(argv[2]) == "--require-instrumentation" && !instrumented) return 3;
  std::string text = name == "inline" ? "short-value" : std::string(256, 'x');
  std::pmr::monotonic_buffer_resource arena;
  myna::value_builder builder(&arena);
  auto encode = [&] {
    builder.clear();
    uint32_t region = name == "borrowed" ? builder.region(text) : 0;
    builder.begin_array();
    for (int i = 0; i < 32; ++i) {
      if (region) builder.string_ref(region, 0, text.size());
      else builder.string(text);
    }
    builder.end();
    auto view = builder.view();
    if (view.node_count != 33 || view.nodes[0].length != 32) std::abort();
    observed = view.node_count + view.nodes[32].length;
  };
  encode(); // Reserve storage before the measured region.
  if (instrumented && (instrument_hooks_set_integration(hooks, "myna-native", "1.0.0") ||
                       instrument_hooks_start_benchmark_inline(hooks))) return 4;
  auto start = std::chrono::steady_clock::now();
  __codspeed_root_frame__values(encode);
  auto elapsed = std::chrono::steady_clock::now() - start;
  if (instrumented && (instrument_hooks_stop_benchmark_inline(hooks) ||
      instrument_hooks_set_executed_benchmark(hooks, getpid(), ("benchmarks/native/values.cpp::" + name).c_str()))) return 4;
  instrument_hooks_deinit(hooks);
  if (observed != 33 + text.size()) return 5;
  std::cout << "{\"case\":\"" << name << "\",\"iterations\":1000,\"ns\":"
            << std::chrono::duration_cast<std::chrono::nanoseconds>(elapsed).count() << "}\n";
}
