# Scalar call boundary comparison

Revision: `ad9019ceba5916e3c7ac11bdcbfdd7974491273c`.
Fresh processes per case: 5. Smoke run: False.

Each row summarizes process medians. Modes and transition policies are separate comparisons.
Control is shown without subtraction. This is a dependent scalar latency workload.

| Mode | Case | Median ns/op | Min–max process median | Bootstrap 95% CI |
|---|---|---:|---:|---:|
| ffm-aot | control | 0.986 | 0.931–0.996 | 0.931–0.996 |
| ffm-aot | generated-ffm | 2056.035 | 1821.396–2920.903 | 1821.396–2920.903 |
| ffm-aot | manual-ffm | 1968.965 | 1854.229–2563.814 | 1854.229–2563.814 |
| jvm | control | 0.932 | 0.915–1.065 | 0.915–1.065 |
| jvm | generated-ffm | 2.635 | 2.577–2.968 | 2.577–2.968 |
| jvm | jni | 2.984 | 2.894–3.564 | 2.894–3.564 |
| jvm | manual-ffm | 2.747 | 2.630–3.086 | 2.630–3.086 |
| native | control | 0.934 | 0.915–1.054 | 0.915–1.054 |
| native | generated-leaf | 0.992 | 0.941–1.061 | 0.941–1.061 |
| native | generated-normal | 4.526 | 3.993–5.453 | 3.993–5.453 |
| native | manual-leaf | 1.001 | 0.944–1.047 | 0.944–1.047 |
| native | manual-normal | 4.293 | 4.037–4.762 | 4.037–4.762 |
