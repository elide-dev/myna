# Native call floor and bounded batching

Source: `20da32081f268bb3f761007e8ba6b6ce22e3347e`; 5 processes/case; smoke: False.

One operation is one scalar call (identity or recurrence, as labeled). Batches retain each noinline native scalar call; no control subtraction.
Native and SVM execute identical linked kernel objects. Generated leaves use binding ABI 2 defaults; custom pointers and bounded Java batches are additional internal Graal API experiments.

| Runtime | Case | Batch | Median ns/op | Process min–max | ns/boundary including work |
|---|---|---:|---:|---:|---:|
| native | batch-16 | 16 | 0.953 | 0.934–0.958 | 15.256 |
| native | batch-256 | 256 | 0.954 | 0.924–0.993 | 244.301 |
| native | batch-4 | 4 | 0.997 | 0.949–1.085 | 3.990 |
| native | batch-64 | 64 | 0.947 | 0.941–0.975 | 60.585 |
| native | control | 1 | 0.940 | 0.922–0.983 | — |
| native | direct | 1 | 1.063 | 1.058–1.089 | 1.063 |
| native | identity-direct | 1 | 0.577 | 0.468–0.696 | 0.577 |
| native | identity-direct-unrolled4 | 1 | 0.469 | 0.469–0.490 | 0.469 |
| native | identity-indirect | 1 | 0.643 | 0.512–0.699 | 0.643 |
| native | identity-indirect-unrolled4 | 1 | 0.512 | 0.471–0.521 | 0.512 |
| native | indirect | 1 | 1.112 | 1.061–1.163 | 1.112 |
| svm | batch-16 | 16 | 0.941 | 0.929–0.998 | 15.059 |
| svm | batch-256 | 256 | 0.942 | 0.930–0.948 | 241.036 |
| svm | batch-4 | 4 | 0.940 | 0.925–0.981 | 3.761 |
| svm | batch-64 | 64 | 0.948 | 0.933–0.999 | 60.703 |
| svm | control | 1 | 0.946 | 0.930–0.953 | — |
| svm | generated-leaf | 1 | 0.952 | 0.938–0.997 | 0.952 |
| svm | generated-normal | 1 | 3.712 | 3.678–3.855 | 3.712 |
| svm | identity-generated | 1 | 0.519 | 0.490–0.545 | 0.519 |
| svm | identity-inline-pointer | 1 | 0.530 | 0.523–0.531 | 0.530 |
| svm | identity-inline-pointer-u16 | 1 | 0.490 | 0.488–0.517 | 0.490 |
| svm | identity-inline-pointer-u4 | 1 | 0.529 | 0.526–0.530 | 0.529 |
| svm | identity-manual | 1 | 0.999 | 0.992–1.043 | 0.999 |
| svm | inline-pointer | 1 | 0.972 | 0.946–1.016 | 0.972 |
| svm | inline-pointer-u16 | 1 | 0.954 | 0.943–0.963 | 0.954 |
| svm | inline-pointer-u4 | 1 | 0.944 | 0.932–0.978 | 0.944 |
| svm | java-batch-16 | 16 | 0.946 | 0.941–0.951 | 15.129 |
| svm | java-batch-256 | 256 | 0.944 | 0.939–0.972 | 241.692 |
| svm | java-batch-4 | 4 | 0.937 | 0.927–0.958 | 3.746 |
| svm | java-batch-64 | 64 | 0.954 | 0.940–0.983 | 61.057 |
| svm | manual-leaf | 1 | 0.998 | 0.995–1.000 | 0.998 |
