# Native call floor and bounded batching

Source: `da7b09c5827a196d6a632f20c51843ece4295294`; 5 processes/case; smoke: False.

One operation is one scalar call (identity or recurrence, as labeled). Batches retain each noinline native scalar call; no control subtraction.
Native and SVM execute identical linked kernel objects. Inlineable pointers and bounded Java batches are opt-in internal Graal API experiments.

| Runtime | Case | Batch | Median ns/op | Process min–max | ns/boundary including work |
|---|---|---:|---:|---:|---:|
| native | batch-16 | 16 | 0.606 | 0.606–0.611 | 9.694 |
| native | batch-256 | 256 | 0.611 | 0.610–0.611 | 156.417 |
| native | batch-4 | 4 | 0.604 | 0.602–0.606 | 2.418 |
| native | batch-64 | 64 | 1.943 | 1.943–1.944 | 124.369 |
| native | control | 1 | 0.589 | 0.589–0.589 | — |
| native | direct | 1 | 0.589 | 0.589–0.589 | 0.589 |
| native | identity-direct | 1 | 0.589 | 0.589–0.589 | 0.589 |
| native | identity-direct-unrolled4 | 1 | 0.516 | 0.516–0.516 | 0.516 |
| native | identity-indirect | 1 | 0.589 | 0.589–0.589 | 0.589 |
| native | identity-indirect-unrolled4 | 1 | 0.442 | 0.442–0.442 | 0.442 |
| native | indirect | 1 | 0.589 | 0.589–0.589 | 0.589 |
| svm | batch-16 | 16 | 0.659 | 0.657–0.660 | 10.542 |
| svm | batch-256 | 256 | 0.613 | 0.613–0.613 | 156.905 |
| svm | batch-4 | 4 | 0.746 | 0.741–0.748 | 2.983 |
| svm | batch-64 | 64 | 2.013 | 2.013–2.014 | 128.845 |
| svm | control | 1 | 0.589 | 0.589–0.590 | — |
| svm | generated-leaf | 1 | 1.568 | 1.549–1.571 | 1.568 |
| svm | generated-normal | 1 | 13.279 | 13.090–14.247 | 13.279 |
| svm | identity-generated | 1 | 1.503 | 1.501–1.508 | 1.503 |
| svm | identity-inline-pointer | 1 | 0.528 | 0.526–0.531 | 0.528 |
| svm | identity-inline-pointer-u16 | 1 | 0.459 | 0.459–0.460 | 0.459 |
| svm | identity-inline-pointer-u4 | 1 | 0.502 | 0.501–0.509 | 0.502 |
| svm | identity-manual | 1 | 1.505 | 1.502–1.509 | 1.505 |
| svm | inline-pointer | 1 | 0.762 | 0.761–0.762 | 0.762 |
| svm | inline-pointer-u16 | 1 | 0.610 | 0.609–0.610 | 0.610 |
| svm | inline-pointer-u4 | 1 | 0.645 | 0.644–0.647 | 0.645 |
| svm | java-batch-16 | 16 | 0.594 | 0.594–0.595 | 9.498 |
| svm | java-batch-256 | 256 | 0.709 | 0.709–0.709 | 181.397 |
| svm | java-batch-4 | 4 | 0.737 | 0.737–0.741 | 2.949 |
| svm | java-batch-64 | 64 | 0.603 | 0.603–0.603 | 38.613 |
| svm | manual-leaf | 1 | 1.532 | 1.530–1.535 | 1.532 |
