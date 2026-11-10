# Scalar call boundary comparison

Revision: `c8802c553835cb48b4d3f764b90dadfb7aebfee5`.
Fresh processes per case: 5. Smoke run: False.

Each row summarizes process medians. Modes and transition policies are separate comparisons.
Control is shown without subtraction. This is a dependent scalar latency workload.

Allocated bytes measure the current Java thread's heap only; native allocations are excluded.

| Mode | Case | Median ns/op | Min–max process median | Bootstrap 95% CI | Java heap B/op median / max sample |
|---|---|---:|---:|---:|---:|
| native | control | 0.793 | 0.740–0.796 | 0.740–0.796 | 0 / 0 |
| native | generated-leaf | 0.844 | 0.789–0.846 | 0.789–0.846 | 0 / 0 |
| native | generated-normal | 4.813 | 4.811–5.167 | 4.811–5.167 | 0 / 0 |
| native | generated-static-jni | 17.455 | 17.446–18.679 | 17.446–18.679 | 0 / 0 |
| native | manual-leaf | 1.545 | 1.482–1.592 | 1.482–1.592 | 0 / 0 |
| native | manual-normal | 4.815 | 4.810–4.981 | 4.810–4.981 | 0 / 0 |
| native | manual-static-jni | 17.444 | 17.437–17.452 | 17.437–17.452 | 0 / 0 |
| ffm-aot | control | 0.742 | 0.740–0.793 | 0.740–0.793 | 0 / 0 |
| ffm-aot | generated-critical-ffm | 1489.590 | 1479.546–1496.240 | 1479.546–1496.240 | 3416.01 / 3416.01 |
| ffm-aot | generated-ffm | 5.738 | 5.737–5.741 | 5.737–5.741 | 0 / 0 |
| ffm-aot | generated-library-ffm | 14.251 | 14.247–14.257 | 14.247–14.257 | 0 / 0 |
| ffm-aot | generated-typed-critical-ffm | 1.601 | 1.595–1.646 | 1.595–1.646 | 0 / 0 |
| ffm-aot | manual-bound-critical-ffm | 1479.180 | 1476.180–1521.690 | 1476.180–1521.690 | 3416.01 / 3416.01 |
| ffm-aot | manual-bound-ffm | 1482.795 | 1477.830–1510.885 | 1477.830–1510.885 | 3416.01 / 3416.01 |
| ffm-aot | manual-ffm | 5.552 | 5.551–5.948 | 5.551–5.948 | 0 / 0 |
| ffm-aot | manual-library-ffm | 14.065 | 14.061–14.069 | 14.061–14.069 | 0 / 0 |
| ffm-aot | manual-unbound-critical-ffm | 1.393 | 1.393–1.397 | 1.393–1.397 | 0 / 0 |
| jvm | control | 0.740 | 0.740–0.742 | 0.740–0.742 | 0 / 0 |
| jvm | generated-critical-ffm | 1.667 | 1.666–1.670 | 1.666–1.670 | 0 / 0 |
| jvm | generated-ffm | 4.999 | 4.996–5.003 | 4.996–5.003 | 0 / 0 |
| jvm | generated-library-ffm | 12.677 | 12.677–12.680 | 12.677–12.680 | 0 / 0 |
| jvm | generated-typed-critical-ffm | 1.667 | 1.666–1.784 | 1.666–1.784 | 0 / 0 |
| jvm | jni | 5.833 | 5.831–6.246 | 5.831–6.246 | 0 / 0 |
| jvm | manual-bound-critical-ffm | 1.669 | 1.666–1.852 | 1.666–1.852 | 0 / 0 |
| jvm | manual-bound-ffm | 4.997 | 4.995–5.096 | 4.995–5.096 | 0 / 0 |
| jvm | manual-ffm | 4.998 | 4.997–5.001 | 4.997–5.001 | 0 / 0 |
| jvm | manual-library-ffm | 12.677 | 12.672–12.680 | 12.672–12.680 | 0 / 0 |
| jvm | manual-unbound-critical-ffm | 1.667 | 1.666–1.785 | 1.666–1.785 | 0 / 0 |
