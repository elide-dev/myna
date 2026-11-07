# Native call floor and bounded batching

Source: `2812eaf8101db8a8205577833bdb2f49f85dc9a3`; 5 processes/case; smoke: False.

One operation is one scalar call (identity or recurrence, as labeled). Batches retain each noinline native scalar call; no control subtraction.
Native and SVM execute identical linked kernel objects. Generated leaves use binding ABI 2 defaults; custom pointers and bounded Java batches are additional internal Graal API experiments.

| Runtime | Case | Batch | Median ns/op | Process min–max | ns/boundary including work |
|---|---|---:|---:|---:|---:|
| native | batch-16 | 16 | 0.605 | 0.605–0.606 | 9.682 |
| native | batch-256 | 256 | 0.611 | 0.611–0.611 | 156.370 |
| native | batch-4 | 4 | 0.607 | 0.603–0.610 | 2.427 |
| native | batch-64 | 64 | 1.943 | 1.943–1.944 | 124.378 |
| native | control | 1 | 0.589 | 0.589–0.590 | — |
| native | direct | 1 | 0.589 | 0.589–0.589 | 0.589 |
| native | identity-direct | 1 | 0.589 | 0.589–0.590 | 0.589 |
| native | identity-direct-unrolled4 | 1 | 0.516 | 0.516–0.516 | 0.516 |
| native | identity-indirect | 1 | 0.589 | 0.589–0.590 | 0.589 |
| native | identity-indirect-unrolled4 | 1 | 0.442 | 0.442–0.442 | 0.442 |
| native | indirect | 1 | 0.589 | 0.589–0.589 | 0.589 |
| svm | batch-16 | 16 | 0.637 | 0.636–0.638 | 10.186 |
| svm | batch-256 | 256 | 0.643 | 0.642–0.643 | 164.520 |
| svm | batch-4 | 4 | 0.663 | 0.663–0.663 | 2.653 |
| svm | batch-64 | 64 | 2.003 | 2.003–2.003 | 128.181 |
| svm | control | 1 | 0.589 | 0.589–0.589 | — |
| svm | generated-leaf | 1 | 0.762 | 0.762–0.762 | 0.762 |
| svm | generated-normal | 1 | 13.289 | 13.086–14.093 | 13.289 |
| svm | identity-generated | 1 | 0.527 | 0.525–0.527 | 0.527 |
| svm | identity-inline-pointer | 1 | 0.525 | 0.525–0.528 | 0.525 |
| svm | identity-inline-pointer-u16 | 1 | 0.481 | 0.479–0.482 | 0.481 |
| svm | identity-inline-pointer-u4 | 1 | 0.516 | 0.516–0.516 | 0.516 |
| svm | identity-manual | 1 | 1.509 | 1.506–1.510 | 1.509 |
| svm | inline-pointer | 1 | 0.762 | 0.761–0.762 | 0.762 |
| svm | inline-pointer-u16 | 1 | 0.610 | 0.610–0.610 | 0.610 |
| svm | inline-pointer-u4 | 1 | 0.644 | 0.643–0.645 | 0.644 |
| svm | java-batch-16 | 16 | 0.615 | 0.615–0.616 | 9.846 |
| svm | java-batch-256 | 256 | 0.709 | 0.709–0.709 | 181.473 |
| svm | java-batch-4 | 4 | 0.793 | 0.793–0.796 | 3.174 |
| svm | java-batch-64 | 64 | 0.603 | 0.603–0.603 | 38.606 |
| svm | manual-leaf | 1 | 1.532 | 1.521–1.533 | 1.532 |
