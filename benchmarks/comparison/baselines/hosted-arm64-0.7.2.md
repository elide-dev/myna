# Scalar call boundary comparison

Revision: `d3237d94c84e7ea4532164b0b58cfbb99212cef0`.
Fresh processes per case: 5. Smoke run: False.

Each row summarizes process medians. Modes and transition policies are separate comparisons.
Control is shown without subtraction. This is a dependent scalar latency workload.

| Mode | Case | Median ns/op | Min–max process median | Bootstrap 95% CI |
|---|---|---:|---:|---:|
| native | control | 0.590 | 0.589–0.590 | 0.589–0.590 |
| native | generated-leaf | 1.537 | 1.522–1.541 | 1.522–1.541 |
| native | generated-normal | 13.298 | 13.109–14.118 | 13.109–14.118 |
| native | manual-leaf | 1.559 | 1.558–1.563 | 1.558–1.563 |
| native | manual-normal | 13.291 | 13.125–14.074 | 13.125–14.074 |
| ffm-aot | control | 0.590 | 0.589–0.591 | 0.589–0.591 |
| ffm-aot | generated-ffm | 15.771 | 15.631–16.089 | 15.631–16.089 |
| ffm-aot | manual-bound-ffm | 4962.359 | 4896.420–5024.732 | 4896.420–5024.732 |
| ffm-aot | manual-ffm | 16.100 | 15.631–16.482 | 15.631–16.482 |
| jvm | control | 0.593 | 0.592–0.593 | 0.592–0.593 |
| jvm | generated-ffm | 7.323 | 7.321–7.332 | 7.321–7.332 |
| jvm | jni | 6.727 | 6.724–6.729 | 6.724–6.729 |
| jvm | manual-bound-ffm | 7.324 | 7.321–7.328 | 7.321–7.328 |
| jvm | manual-ffm | 7.519 | 7.517–7.522 | 7.517–7.522 |
