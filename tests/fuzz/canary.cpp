#include <thread>
#include <cstdlib>
#include <climits>
#include <chrono>
#if defined(MYNA_RACE_CANARY)
volatile int shared = 0;
int main() {
  std::thread a([] { for (int i = 0; i < 10000; ++i) shared = i; });
  std::thread b([] { for (int i = 0; i < 10000; ++i) shared = i; });
  a.join(); b.join();
  return 0;
}
#elif defined(MYNA_RSS_CANARY)
int main() {
  auto data = static_cast<volatile char *>(std::malloc(64 * 1024 * 1024));
  for (size_t i = 0; i < 64 * 1024 * 1024; i += 4096) data[i] = 1;
  std::this_thread::sleep_for(std::chrono::seconds(2));
  std::free(const_cast<char *>(data));
  return 0;
}
#elif defined(MYNA_LEAK_CANARY)
void *volatile leaked;
int main(int argc, char **) {
  leaked = std::malloc(64 + argc);
  leaked = nullptr;
  return 0;
}
#elif defined(MYNA_UB_CANARY)
int main(int argc, char **) {
  volatile int largest = INT_MAX;
  return largest + argc;
}
#else
int main(int argc, char **) {
  auto data = static_cast<volatile char *>(std::malloc(8));
  data[argc + 8] = 1;
  std::free(const_cast<char *>(data));
  return 0;
}
#endif
