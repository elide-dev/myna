# Native call floor and bounded batching

Source: `0e81866b8f665cca704732551f99cee246cba7c2`; 5 processes/case; smoke: False.

One operation is one scalar call (identity or recurrence, as labeled). Batches retain each noinline native scalar call; no control subtraction.
Native and SVM execute identical linked kernel objects. Generated leaves use binding ABI 2 defaults; custom pointers and bounded Java batches are additional internal Graal API experiments.

| Runtime | Case | Batch | Median ns/op | Process min–max | ns/boundary including work |
|---|---|---:|---:|---:|---:|
| native | batch-16 | 16 | 0.741 | 0.740–0.741 | 11.849 |
| native | batch-256 | 256 | 0.762 | 0.762–0.814 | 195.090 |
| native | batch-4 | 4 | 0.740 | 0.740–0.758 | 2.962 |
| native | batch-64 | 64 | 0.740 | 0.740–0.740 | 47.386 |
| native | control | 1 | 0.099 | 0.093–0.099 | — |
| native | direct | 1 | 0.740 | 0.740–0.741 | 0.740 |
| native | identity-direct | 1 | 0.743 | 0.740–0.794 | 0.743 |
| native | identity-direct-unrolled4 | 1 | 0.740 | 0.740–0.760 | 0.740 |
| native | identity-indirect | 1 | 1.110 | 0.900–1.110 | 1.110 |
| native | identity-indirect-unrolled4 | 1 | 0.740 | 0.740–0.757 | 0.740 |
| native | indirect | 1 | 1.110 | 0.909–1.115 | 1.110 |
| svm | batch-16 | 16 | 0.752 | 0.752–0.752 | 12.032 |
| svm | batch-256 | 256 | 0.763 | 0.763–0.763 | 195.277 |
| svm | batch-4 | 4 | 0.787 | 0.787–0.799 | 3.150 |
| svm | batch-64 | 64 | 0.743 | 0.743–0.756 | 47.579 |
| svm | control | 1 | 0.740 | 0.740–0.740 | — |
| svm | generated-leaf | 1 | 0.792 | 0.750–0.793 | 0.792 |
| svm | generated-normal | 1 | 4.813 | 4.811–4.826 | 4.813 |
| svm | identity-generated | 1 | 0.792 | 0.787–0.844 | 0.792 |
| svm | identity-inline-pointer | 1 | 0.787 | 0.741–0.843 | 0.787 |
| svm | identity-inline-pointer-u16 | 1 | 0.763 | 0.752–0.764 | 0.763 |
| svm | identity-inline-pointer-u4 | 1 | 0.787 | 0.787–0.787 | 0.787 |
| svm | identity-manual | 1 | 1.482 | 1.481–1.484 | 1.482 |
| svm | inline-pointer | 1 | 0.792 | 0.791–0.797 | 0.792 |
| svm | inline-pointer-u16 | 1 | 0.752 | 0.752–0.753 | 0.752 |
| svm | inline-pointer-u4 | 1 | 0.788 | 0.787–0.790 | 0.788 |
| svm | java-batch-16 | 16 | 0.799 | 0.798–0.799 | 12.777 |
| svm | java-batch-256 | 256 | 0.815 | 0.787–0.823 | 208.551 |
| svm | java-batch-4 | 4 | 0.972 | 0.972–0.973 | 3.889 |
| svm | java-batch-64 | 64 | 0.756 | 0.755–0.756 | 48.356 |
| svm | manual-leaf | 1 | 1.529 | 1.529–1.530 | 1.529 |
