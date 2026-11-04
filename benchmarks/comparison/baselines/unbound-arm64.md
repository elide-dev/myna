# Scalar call boundary comparison

Revision: `55d1ddd01ab9a14e712b0a6266e686ca75fc0994`.
Fresh processes per case: 5. Smoke run: False.

Each row summarizes process medians. Modes and transition policies are separate comparisons.
Control is shown without subtraction. This is a dependent scalar latency workload.

| Mode | Case | Median ns/op | Min–max process median | Bootstrap 95% CI |
|---|---|---:|---:|---:|
| ffm-aot | control | 1.483 | 0.935–6.364 | 0.935–6.364 |
| ffm-aot | generated-ffm | 4.671 | 4.199–8.792 | 4.199–8.792 |
| ffm-aot | manual-bound-ffm | 2665.845 | 2152.113–4315.639 | 2152.113–4315.639 |
| ffm-aot | manual-ffm | 6.078 | 4.406–9.395 | 4.406–9.395 |
| jvm | control | 1.187 | 0.952–1.274 | 0.952–1.274 |
| jvm | generated-ffm | 4.589 | 2.668–7.342 | 2.668–7.342 |
| jvm | jni | 3.331 | 2.812–6.239 | 2.812–6.239 |
| jvm | manual-bound-ffm | 3.939 | 2.733–8.370 | 2.733–8.370 |
| jvm | manual-ffm | 3.480 | 2.647–4.977 | 2.647–4.977 |
| native | control | 1.211 | 0.935–1.684 | 0.935–1.684 |
| native | generated-leaf | 1.059 | 0.994–1.623 | 0.994–1.623 |
| native | generated-normal | 5.889 | 4.019–12.861 | 4.019–12.861 |
| native | manual-leaf | 1.260 | 1.001–2.615 | 1.001–2.615 |
| native | manual-normal | 7.393 | 4.775–31.205 | 4.775–31.205 |
