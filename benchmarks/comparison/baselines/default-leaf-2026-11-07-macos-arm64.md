# Native call floor and bounded batching

Source: `89bcf2a607c338d3a53efa2bded90f0bb8fcdebf`; 5 processes/case; smoke: False.

One operation is one scalar call (identity or recurrence, as labeled). Batches retain each noinline native scalar call; no control subtraction.
Native and SVM execute identical linked kernel objects. Generated leaves use binding ABI 2 defaults; custom pointers and bounded Java batches are additional internal Graal API experiments.

| Runtime | Case | Batch | Median ns/op | Process min–max | ns/boundary including work |
|---|---|---:|---:|---:|---:|
| native | batch-16 | 16 | 0.908 | 0.890–0.982 | 14.530 |
| native | batch-256 | 256 | 0.972 | 0.882–1.000 | 248.940 |
| native | batch-4 | 4 | 0.965 | 0.939–1.013 | 3.860 |
| native | batch-64 | 64 | 0.900 | 0.882–0.937 | 57.589 |
| native | control | 1 | 0.897 | 0.880–0.943 | — |
| native | direct | 1 | 1.007 | 1.000–1.068 | 1.007 |
| native | identity-direct | 1 | 0.449 | 0.439–0.472 | 0.449 |
| native | identity-direct-unrolled4 | 1 | 0.458 | 0.443–0.465 | 0.458 |
| native | identity-indirect | 1 | 0.660 | 0.533–0.700 | 0.660 |
| native | identity-indirect-unrolled4 | 1 | 0.488 | 0.475–0.508 | 0.488 |
| native | indirect | 1 | 1.011 | 0.991–1.060 | 1.011 |
| svm | batch-16 | 16 | 0.902 | 0.880–0.945 | 14.429 |
| svm | batch-256 | 256 | 0.895 | 0.882–0.922 | 229.078 |
| svm | batch-4 | 4 | 0.935 | 0.896–0.976 | 3.739 |
| svm | batch-64 | 64 | 0.902 | 0.893–0.982 | 57.748 |
| svm | control | 1 | 0.908 | 0.881–0.928 | — |
| svm | generated-leaf | 1 | 0.908 | 0.890–0.947 | 0.908 |
| svm | generated-normal | 1 | 3.572 | 3.489–3.811 | 3.572 |
| svm | identity-generated | 1 | 0.516 | 0.487–0.546 | 0.516 |
| svm | identity-inline-pointer | 1 | 0.516 | 0.508–0.535 | 0.516 |
| svm | identity-inline-pointer-u16 | 1 | 0.483 | 0.460–0.502 | 0.483 |
| svm | identity-inline-pointer-u4 | 1 | 0.510 | 0.495–0.529 | 0.510 |
| svm | identity-manual | 1 | 0.997 | 0.961–1.065 | 0.997 |
| svm | inline-pointer | 1 | 0.922 | 0.886–1.000 | 0.922 |
| svm | inline-pointer-u16 | 1 | 0.922 | 0.897–0.962 | 0.922 |
| svm | inline-pointer-u4 | 1 | 0.922 | 0.886–0.942 | 0.922 |
| svm | java-batch-16 | 16 | 0.941 | 0.889–1.007 | 15.055 |
| svm | java-batch-256 | 256 | 0.896 | 0.894–0.955 | 229.303 |
| svm | java-batch-4 | 4 | 0.920 | 0.881–0.947 | 3.681 |
| svm | java-batch-64 | 64 | 0.898 | 0.881–0.953 | 57.488 |
| svm | manual-leaf | 1 | 0.986 | 0.952–1.027 | 0.986 |
