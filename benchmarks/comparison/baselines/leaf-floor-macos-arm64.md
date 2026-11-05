# Native call floor and bounded batching

Source: `125ef636e5bc42482ac83f0b5a0e0996f98bf403`; 5 processes/case; smoke: False.

One operation is one scalar call (identity or recurrence, as labeled). Batches retain each noinline native scalar call; no control subtraction.
Native and SVM execute identical linked kernel objects. Inlineable pointers and bounded Java batches are opt-in internal Graal API experiments.

| Runtime | Case | Batch | Median ns/op | Process min–max | ns/boundary including work |
|---|---|---:|---:|---:|---:|
| native | batch-16 | 16 | 0.949 | 0.944–0.954 | 15.183 |
| native | batch-256 | 256 | 0.940 | 0.937–0.963 | 240.669 |
| native | batch-4 | 4 | 1.000 | 0.997–1.011 | 4.000 |
| native | batch-64 | 64 | 0.954 | 0.942–0.981 | 61.028 |
| native | control | 1 | 0.944 | 0.937–0.951 | — |
| native | direct | 1 | 1.122 | 1.053–1.155 | 1.122 |
| native | identity-direct | 1 | 0.475 | 0.468–0.694 | 0.475 |
| native | identity-direct-unrolled4 | 1 | 0.471 | 0.467–0.550 | 0.471 |
| native | identity-indirect | 1 | 0.570 | 0.499–0.660 | 0.570 |
| native | identity-indirect-unrolled4 | 1 | 0.481 | 0.471–0.525 | 0.481 |
| native | indirect | 1 | 1.070 | 1.054–1.116 | 1.070 |
| svm | batch-16 | 16 | 0.954 | 0.946–0.965 | 15.269 |
| svm | batch-256 | 256 | 0.945 | 0.937–0.952 | 241.818 |
| svm | batch-4 | 4 | 0.946 | 0.935–0.965 | 3.785 |
| svm | batch-64 | 64 | 0.952 | 0.937–0.973 | 60.898 |
| svm | control | 1 | 0.944 | 0.936–0.977 | — |
| svm | generated-leaf | 1 | 0.998 | 0.992–1.010 | 0.998 |
| svm | generated-normal | 1 | 3.685 | 3.656–3.841 | 3.685 |
| svm | identity-generated | 1 | 1.001 | 0.997–1.095 | 1.001 |
| svm | identity-inline-pointer | 1 | 0.525 | 0.509–0.555 | 0.525 |
| svm | identity-inline-pointer-u16 | 1 | 0.485 | 0.476–0.518 | 0.485 |
| svm | identity-inline-pointer-u4 | 1 | 0.526 | 0.520–0.531 | 0.526 |
| svm | identity-manual | 1 | 1.006 | 0.995–1.185 | 1.006 |
| svm | inline-pointer | 1 | 0.951 | 0.945–0.963 | 0.951 |
| svm | inline-pointer-u16 | 1 | 0.954 | 0.952–0.967 | 0.954 |
| svm | inline-pointer-u4 | 1 | 0.939 | 0.937–1.005 | 0.939 |
| svm | java-batch-16 | 16 | 0.946 | 0.939–1.003 | 15.139 |
| svm | java-batch-256 | 256 | 0.940 | 0.938–0.980 | 240.739 |
| svm | java-batch-4 | 4 | 0.938 | 0.937–0.944 | 3.752 |
| svm | java-batch-64 | 64 | 0.948 | 0.944–0.987 | 60.660 |
| svm | manual-leaf | 1 | 0.993 | 0.988–1.188 | 0.993 |
