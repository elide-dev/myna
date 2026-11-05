# Native call floor and bounded batching

Source: `e09430fb999be082102c2eef404e6650bf430c91`; 5 processes/case; smoke: False.

One operation is one scalar call (identity or recurrence, as labeled). Batches retain each noinline native scalar call; no control subtraction.
Native and SVM execute identical linked kernel objects. Generated leaves use binding ABI 2 defaults; custom pointers and bounded Java batches are additional internal Graal API experiments.

| Runtime | Case | Batch | Median ns/op | Process min–max | ns/boundary including work |
|---|---|---:|---:|---:|---:|
| native | batch-16 | 16 | 0.606 | 0.603–0.609 | 9.692 |
| native | batch-256 | 256 | 0.611 | 0.610–0.611 | 156.359 |
| native | batch-4 | 4 | 0.604 | 0.602–0.611 | 2.416 |
| native | batch-64 | 64 | 1.944 | 1.943–1.944 | 124.395 |
| native | control | 1 | 0.589 | 0.589–0.590 | — |
| native | direct | 1 | 0.589 | 0.589–0.589 | 0.589 |
| native | identity-direct | 1 | 0.589 | 0.589–0.590 | 0.589 |
| native | identity-direct-unrolled4 | 1 | 0.516 | 0.516–0.516 | 0.516 |
| native | identity-indirect | 1 | 0.589 | 0.589–0.590 | 0.589 |
| native | identity-indirect-unrolled4 | 1 | 0.442 | 0.442–0.442 | 0.442 |
| native | indirect | 1 | 0.589 | 0.589–0.589 | 0.589 |
| svm | batch-16 | 16 | 0.637 | 0.636–0.638 | 10.189 |
| svm | batch-256 | 256 | 0.626 | 0.626–0.626 | 160.276 |
| svm | batch-4 | 4 | 0.663 | 0.663–0.663 | 2.652 |
| svm | batch-64 | 64 | 2.003 | 2.003–2.003 | 128.193 |
| svm | control | 1 | 0.589 | 0.589–0.589 | — |
| svm | generated-leaf | 1 | 0.762 | 0.762–0.762 | 0.762 |
| svm | generated-normal | 1 | 14.086 | 13.095–14.261 | 14.086 |
| svm | identity-generated | 1 | 0.526 | 0.525–0.528 | 0.526 |
| svm | identity-inline-pointer | 1 | 0.527 | 0.526–0.528 | 0.527 |
| svm | identity-inline-pointer-u16 | 1 | 0.467 | 0.466–0.468 | 0.467 |
| svm | identity-inline-pointer-u4 | 1 | 0.516 | 0.516–0.516 | 0.516 |
| svm | identity-manual | 1 | 1.510 | 1.507–1.512 | 1.510 |
| svm | inline-pointer | 1 | 0.762 | 0.762–0.762 | 0.762 |
| svm | inline-pointer-u16 | 1 | 0.610 | 0.610–0.610 | 0.610 |
| svm | inline-pointer-u4 | 1 | 0.645 | 0.644–0.645 | 0.645 |
| svm | java-batch-16 | 16 | 0.616 | 0.616–0.616 | 9.849 |
| svm | java-batch-256 | 256 | 0.709 | 0.709–0.709 | 181.552 |
| svm | java-batch-4 | 4 | 0.795 | 0.791–0.796 | 3.180 |
| svm | java-batch-64 | 64 | 0.606 | 0.605–0.606 | 38.753 |
| svm | manual-leaf | 1 | 1.535 | 1.520–1.538 | 1.535 |
