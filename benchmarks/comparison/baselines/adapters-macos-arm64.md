# Scalar call boundary comparison

Revision: `6377cbdcafc89ea2ae86306f3d51e73e43ed0b7e`.
Fresh processes per case: 5. Smoke run: False.

Each row summarizes process medians. Modes and transition policies are separate comparisons.
Control is shown without subtraction. This is a dependent scalar latency workload.

Allocated bytes measure the current Java thread's heap only; native allocations are excluded.

| Mode | Case | Median ns/op | Min–max process median | Bootstrap 95% CI | Java heap B/op median / max sample |
|---|---|---:|---:|---:|---:|
| native | control | 0.995 | 0.936–1.551 | 0.936–1.551 | 0 / 0 |
| native | generated-leaf | 1.032 | 0.939–1.366 | 0.939–1.366 | 0 / 0 |
| native | generated-normal | 3.879 | 3.642–5.811 | 3.642–5.811 | 0 / 0 |
| native | generated-static-jni | 14.036 | 13.825–23.914 | 13.825–23.914 | 0 / 0 |
| native | manual-leaf | 1.009 | 0.947–1.680 | 0.947–1.680 | 0 / 0 |
| native | manual-normal | 3.938 | 3.795–4.552 | 3.795–4.552 | 0 / 0 |
| native | manual-static-jni | 14.293 | 13.412–21.615 | 13.412–21.615 | 0 / 0 |
| ffm-aot | control | 1.036 | 0.935–1.205 | 0.935–1.205 | 0 / 0 |
| ffm-aot | generated-critical-ffm | 1923.313 | 1828.315–2764.103 | 1828.315–2764.103 | 3416.01 / 3416.02 |
| ffm-aot | generated-ffm | 5.003 | 4.840–7.859 | 4.840–7.859 | 0 / 0 |
| ffm-aot | generated-library-ffm | 12.404 | 11.866–17.727 | 11.866–17.727 | 0 / 0 |
| ffm-aot | generated-typed-critical-ffm | 2.507 | 2.120–3.215 | 2.120–3.215 | 0 / 0 |
| ffm-aot | manual-bound-critical-ffm | 2176.468 | 1847.140–3918.942 | 1847.140–3918.942 | 3416.01 / 3416.02 |
| ffm-aot | manual-bound-ffm | 2011.782 | 1850.468–4479.851 | 1850.468–4479.851 | 3416.01 / 3416.02 |
| ffm-aot | manual-ffm | 4.659 | 4.603–7.509 | 4.603–7.509 | 0 / 0 |
| ffm-aot | manual-library-ffm | 11.965 | 11.654–19.267 | 11.654–19.267 | 0 / 0 |
| ffm-aot | manual-unbound-critical-ffm | 2.118 | 1.993–2.908 | 1.993–2.908 | 0 / 0 |
| jvm | control | 1.002 | 0.936–1.551 | 0.936–1.551 | 0 / 0 |
| jvm | generated-critical-ffm | 1.068 | 1.049–1.746 | 1.049–1.746 | 0 / 0 |
| jvm | generated-ffm | 2.918 | 2.750–3.466 | 2.750–3.466 | 0 / 0 |
| jvm | generated-library-ffm | 8.950 | 8.736–12.936 | 8.736–12.936 | 0 / 0 |
| jvm | generated-typed-critical-ffm | 1.113 | 0.956–1.739 | 0.956–1.739 | 0 / 0 |
| jvm | jni | 3.175 | 2.828–4.359 | 2.828–4.359 | 0 / 0 |
| jvm | manual-bound-critical-ffm | 1.240 | 1.061–1.754 | 1.061–1.754 | 0 / 0 |
| jvm | manual-bound-ffm | 2.741 | 2.660–4.891 | 2.660–4.891 | 0 / 0 |
| jvm | manual-ffm | 2.780 | 2.649–3.753 | 2.649–3.753 | 0 / 0 |
| jvm | manual-library-ffm | 9.191 | 8.856–24.541 | 8.856–24.541 | 0 / 0 |
| jvm | manual-unbound-critical-ffm | 1.190 | 1.103–1.745 | 1.103–1.745 | 0 / 0 |
