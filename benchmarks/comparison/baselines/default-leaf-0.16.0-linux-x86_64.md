# Native call floor and bounded batching

Source: `c8802c553835cb48b4d3f764b90dadfb7aebfee5`; 5 processes/case; smoke: False.

One operation is one scalar call (identity or recurrence, as labeled). Batches retain each noinline native scalar call; no control subtraction.
Native and SVM execute identical linked kernel objects. Generated leaves use binding ABI 2 defaults; custom pointers and bounded Java batches are additional internal Graal API experiments.

| Runtime | Case | Batch | Median ns/op | Process min–max | ns/boundary including work |
|---|---|---:|---:|---:|---:|
| native | batch-16 | 16 | 0.741 | 0.740–0.793 | 11.858 |
| native | batch-256 | 256 | 0.762 | 0.762–0.947 | 195.120 |
| native | batch-4 | 4 | 0.740 | 0.740–0.741 | 2.961 |
| native | batch-64 | 64 | 0.740 | 0.740–0.789 | 47.383 |
| native | control | 1 | 0.099 | 0.093–0.099 | — |
| native | direct | 1 | 0.741 | 0.740–0.743 | 0.741 |
| native | identity-direct | 1 | 0.740 | 0.740–0.752 | 0.740 |
| native | identity-direct-unrolled4 | 1 | 0.740 | 0.740–0.740 | 0.740 |
| native | identity-indirect | 1 | 0.740 | 0.740–1.110 | 0.740 |
| native | identity-indirect-unrolled4 | 1 | 0.740 | 0.740–0.740 | 0.740 |
| native | indirect | 1 | 1.110 | 0.740–1.110 | 1.110 |
| svm | batch-16 | 16 | 0.752 | 0.752–0.752 | 12.033 |
| svm | batch-256 | 256 | 0.763 | 0.763–0.772 | 195.247 |
| svm | batch-4 | 4 | 0.787 | 0.786–0.787 | 3.146 |
| svm | batch-64 | 64 | 0.743 | 0.743–0.743 | 47.574 |
| svm | control | 1 | 0.740 | 0.740–0.740 | — |
| svm | generated-leaf | 1 | 0.747 | 0.745–0.790 | 0.747 |
| svm | generated-normal | 1 | 4.812 | 4.810–4.815 | 4.812 |
| svm | identity-generated | 1 | 0.787 | 0.787–0.843 | 0.787 |
| svm | identity-inline-pointer | 1 | 0.787 | 0.741–0.790 | 0.787 |
| svm | identity-inline-pointer-u16 | 1 | 0.764 | 0.752–0.764 | 0.764 |
| svm | identity-inline-pointer-u4 | 1 | 0.787 | 0.787–0.787 | 0.787 |
| svm | identity-manual | 1 | 1.481 | 1.481–1.482 | 1.481 |
| svm | inline-pointer | 1 | 0.791 | 0.788–0.794 | 0.791 |
| svm | inline-pointer-u16 | 1 | 0.752 | 0.752–0.752 | 0.752 |
| svm | inline-pointer-u4 | 1 | 0.787 | 0.787–0.792 | 0.787 |
| svm | java-batch-16 | 16 | 0.799 | 0.798–0.801 | 12.777 |
| svm | java-batch-256 | 256 | 0.807 | 0.798–0.811 | 206.592 |
| svm | java-batch-4 | 4 | 0.972 | 0.972–0.972 | 3.887 |
| svm | java-batch-64 | 64 | 0.755 | 0.755–0.756 | 48.331 |
| svm | manual-leaf | 1 | 1.529 | 1.528–1.529 | 1.529 |
