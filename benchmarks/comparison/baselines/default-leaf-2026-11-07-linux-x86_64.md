# Native call floor and bounded batching

Source: `89bcf2a607c338d3a53efa2bded90f0bb8fcdebf`; 5 processes/case; smoke: False.

One operation is one scalar call (identity or recurrence, as labeled). Batches retain each noinline native scalar call; no control subtraction.
Native and SVM execute identical linked kernel objects. Generated leaves use binding ABI 2 defaults; custom pointers and bounded Java batches are additional internal Graal API experiments.

| Runtime | Case | Batch | Median ns/op | Process min–max | ns/boundary including work |
|---|---|---:|---:|---:|---:|
| native | batch-16 | 16 | 0.740 | 0.740–0.741 | 11.845 |
| native | batch-256 | 256 | 0.762 | 0.762–0.762 | 195.021 |
| native | batch-4 | 4 | 0.740 | 0.740–0.741 | 2.961 |
| native | batch-64 | 64 | 0.740 | 0.740–0.743 | 47.377 |
| native | control | 1 | 0.099 | 0.093–0.099 | — |
| native | direct | 1 | 0.740 | 0.740–0.740 | 0.740 |
| native | identity-direct | 1 | 0.740 | 0.740–0.755 | 0.740 |
| native | identity-direct-unrolled4 | 1 | 0.740 | 0.740–0.740 | 0.740 |
| native | identity-indirect | 1 | 1.110 | 0.740–1.110 | 1.110 |
| native | identity-indirect-unrolled4 | 1 | 0.740 | 0.740–1.156 | 0.740 |
| native | indirect | 1 | 1.110 | 0.740–1.110 | 1.110 |
| svm | batch-16 | 16 | 0.752 | 0.752–0.752 | 12.028 |
| svm | batch-256 | 256 | 0.763 | 0.762–0.765 | 195.310 |
| svm | batch-4 | 4 | 0.786 | 0.786–0.787 | 3.146 |
| svm | batch-64 | 64 | 0.743 | 0.743–0.743 | 47.560 |
| svm | control | 1 | 0.740 | 0.740–0.741 | — |
| svm | generated-leaf | 1 | 0.791 | 0.746–0.793 | 0.791 |
| svm | generated-normal | 1 | 4.810 | 4.809–4.810 | 4.810 |
| svm | identity-generated | 1 | 0.787 | 0.787–0.843 | 0.787 |
| svm | identity-inline-pointer | 1 | 0.741 | 0.741–0.787 | 0.741 |
| svm | identity-inline-pointer-u16 | 1 | 0.763 | 0.752–0.764 | 0.763 |
| svm | identity-inline-pointer-u4 | 1 | 0.787 | 0.787–0.842 | 0.787 |
| svm | identity-manual | 1 | 1.482 | 1.481–1.486 | 1.482 |
| svm | inline-pointer | 1 | 0.791 | 0.790–0.793 | 0.791 |
| svm | inline-pointer-u16 | 1 | 0.752 | 0.752–0.752 | 0.752 |
| svm | inline-pointer-u4 | 1 | 0.787 | 0.787–0.788 | 0.787 |
| svm | java-batch-16 | 16 | 0.798 | 0.798–0.799 | 12.771 |
| svm | java-batch-256 | 256 | 0.809 | 0.791–0.826 | 207.166 |
| svm | java-batch-4 | 4 | 0.972 | 0.972–0.974 | 3.888 |
| svm | java-batch-64 | 64 | 0.755 | 0.755–0.755 | 48.312 |
| svm | manual-leaf | 1 | 1.529 | 1.528–1.534 | 1.529 |
