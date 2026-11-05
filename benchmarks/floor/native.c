#define _POSIX_C_SOURCE 200809L
#include <inttypes.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
extern int64_t myna_bench_leaf(int64_t);
#define DECLARE(N) extern int64_t myna_bench_batch##N(int64_t);
DECLARE(4) DECLARE(16) DECLARE(64) DECLARE(256)
static int64_t (*volatile target)(int64_t) = myna_bench_leaf;
extern int64_t myna_bench_identity(int64_t);
static int64_t (*volatile identity_target)(int64_t)=myna_bench_identity;
static volatile int64_t sink;
static uint64_t now(void) {
  struct timespec t;
  if (clock_gettime(CLOCK_MONOTONIC, &t)) abort();
  return (uint64_t)t.tv_sec*UINT64_C(1000000000)+(uint64_t)t.tv_nsec;
}
static int64_t reference(int n, int64_t seed) {
  uint64_t x=(uint64_t)seed;
  for(int i=0;i<n;i++) x=x*UINT64_C(1664525)+UINT64_C(1013904223);
  return (int64_t)x;
}
// Separate translation units and no LTO preserve calls to the same kernel.
__attribute__((noinline)) static int64_t direct(int n,int64_t x) {
  for(int i=0;i<n;i++) x=myna_bench_leaf(x); return x;
}
__attribute__((noinline)) static int64_t indirect(int n,int64_t x) {
  int64_t (*fn)(int64_t)=target;
  for(int i=0;i<n;i++) x=fn(x); return x;
}
__attribute__((noinline)) static int64_t identity_direct(int n,int64_t x) {
  for(int i=0;i<n;i++) x=myna_bench_identity(x); return x;
}
__attribute__((noinline)) static int64_t identity_indirect(int n,int64_t x) {
  int64_t (*fn)(int64_t)=identity_target;
  for(int i=0;i<n;i++) x=fn(x); return x;
}
__attribute__((noinline)) static int64_t identity_direct_unrolled4(int n,int64_t x) {
  for(int i=0;i<n;i+=4) { x=myna_bench_identity(x); x=myna_bench_identity(x); x=myna_bench_identity(x); x=myna_bench_identity(x); } return x;
}
__attribute__((noinline)) static int64_t identity_indirect_unrolled4(int n,int64_t x) {
  int64_t (*fn)(int64_t)=identity_target;
  for(int i=0;i<n;i+=4) { x=fn(x); x=fn(x); x=fn(x); x=fn(x); } return x;
}
#define BATCH(N) \
__attribute__((noinline)) static int64_t batch##N(int n,int64_t x) { \
  for(int i=0;i<n;i+=N) x=myna_bench_batch##N(x); return x; \
}
BATCH(4) BATCH(16) BATCH(64) BATCH(256)
int main(int argc,char**argv) {
  if(argc!=4) return 2;
  const char *name=argv[1]; int n=atoi(argv[2]);
  if(n<=0 || n%256) return 2;
  int64_t (*loop)(int,int64_t)=NULL;
  if(!strcmp(name,"control")) loop=reference;
  if(!strcmp(name,"direct")) loop=direct;
  if(!strcmp(name,"indirect")) loop=indirect;
#define SELECT(N) if(!strcmp(name,"batch-" #N)) loop=batch##N;
  SELECT(4) SELECT(16) SELECT(64) SELECT(256)
  if(!strcmp(name,"identity-direct")) loop=identity_direct;
  if(!strcmp(name,"identity-indirect")) loop=identity_indirect;
  if(!strcmp(name,"identity-direct-unrolled4")) loop=identity_direct_unrolled4;
  if(!strcmp(name,"identity-indirect-unrolled4")) loop=identity_indirect_unrolled4;
  if(!loop) return 2;
  FILE *out=fopen(argv[3],"w"); if(!out) return 2;
  for(int sample=-10;sample<10;sample++) {
    int64_t seed=123456789+sample, expected=!strncmp(name,"identity-",9) ? seed : reference(n,seed);
    uint64_t start=now(); int64_t result=loop(n,seed); uint64_t elapsed=now()-start;
    sink=result; if(result!=expected) return 3;
    if(sample>=0) fprintf(out,"{\"case\":\"%s\",\"sample\":%d,\"iterations\":%d,\"ns\":%" PRIu64 ",\"checksum\":%" PRId64 "}\n", name,sample,n,elapsed,result);
  }
  return fclose(out)!=0;
}
