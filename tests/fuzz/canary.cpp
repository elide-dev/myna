#include <thread>
#include <cstdlib>
#if defined(MYNA_RACE_CANARY)
volatile int shared = 0;
int main() {
  std::thread a([] { for (int i = 0; i < 10000; ++i) shared = i; });
  std::thread b([] { for (int i = 0; i < 10000; ++i) shared = i; });
  a.join(); b.join();
  return 0;
}
#else
int main(int argc, char **) {
  auto data = static_cast<volatile char *>(std::malloc(8));
  data[argc + 8] = 1;
  std::free(const_cast<char *>(data));
  return 0;
}
#endif
