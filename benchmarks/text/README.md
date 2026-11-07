# SVM text crossover

Build with `mise run test:native-image`, then run:

```
mise exec -- python3 benchmarks/text/run.py --output build/reports/text-crossover.json
```

Seven independent process samples cover 16–4096 UTF-8 bytes of ASCII, non-BMP
Unicode, and decoded escape characters. Each path uses the same borrowed Rust
storage, native callback, reusable reader, descriptor rebinding, and volatile
string sink. Java and simdutf are forced independently of the default threshold.
The output records raw timings, source/binary SHA-256, hardware, toolchain,
Native Image flags, and iterations. CI retains results on Linux x86_64 and Apple
arm64; inspect Linux CPU flags before describing a host as AVX-512 capable.

These measure end-to-end string materialization including callback overhead,
not isolated transcoder throughput. Shared-host timing noise is not proof of a
win. Compare repeated distributions per fixture and host. The compatibility
default stays at 256 bytes; applications can select their own crossover without
rebuilding the SDK. No universal crossover or cross-machine speedup is claimed.
