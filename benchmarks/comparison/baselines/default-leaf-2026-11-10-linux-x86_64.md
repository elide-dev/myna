# Native call floor and bounded batching

Source: `4f88a774516021f3dec587cd9ac9f8ab7ec28334`; 5 processes/case; smoke: False.

One operation is one scalar call (identity or recurrence, as labeled). Batches retain each noinline native scalar call; no control subtraction.
Native and SVM execute identical linked kernel objects. Generated leaves use binding ABI 2 defaults; custom pointers and bounded Java batches are additional internal Graal API experiments.

| Runtime | Case | Batch | Median ns/op | Process min–max | ns/boundary including work |
|---|---|---:|---:|---:|---:|
| native | batch-16 | 16 | 0.741 | 0.740–0.741 | 11.850 |
| native | batch-256 | 256 | 0.763 | 0.762–0.765 | 195.365 |
| native | batch-4 | 4 | 0.741 | 0.740–0.759 | 2.963 |
| native | batch-64 | 64 | 0.741 | 0.741–0.741 | 47.401 |
| native | control | 1 | 0.096 | 0.093–0.099 | — |
| native | direct | 1 | 0.741 | 0.740–0.743 | 0.741 |
| native | identity-direct | 1 | 0.742 | 0.740–0.792 | 0.742 |
| native | identity-direct-unrolled4 | 1 | 0.742 | 0.740–0.783 | 0.742 |
| native | identity-indirect | 1 | 0.741 | 0.740–1.110 | 0.741 |
| native | identity-indirect-unrolled4 | 1 | 0.741 | 0.740–0.787 | 0.741 |
| native | indirect | 1 | 1.111 | 1.110–1.113 | 1.111 |
| svm | batch-16 | 16 | 0.752 | 0.752–0.752 | 12.035 |
| svm | batch-256 | 256 | 0.763 | 0.763–0.765 | 195.301 |
| svm | batch-4 | 4 | 0.787 | 0.787–0.787 | 3.147 |
| svm | batch-64 | 64 | 0.743 | 0.743–0.744 | 47.583 |
| svm | control | 1 | 0.740 | 0.740–0.741 | — |
| svm | generated-leaf | 1 | 0.795 | 0.754–0.798 | 0.795 |
| svm | generated-normal | 1 | 4.812 | 4.812–4.815 | 4.812 |
| svm | identity-generated | 1 | 0.788 | 0.787–0.790 | 0.788 |
| svm | identity-inline-pointer | 1 | 0.787 | 0.745–0.787 | 0.787 |
| svm | identity-inline-pointer-u16 | 1 | 0.759 | 0.752–0.764 | 0.759 |
| svm | identity-inline-pointer-u4 | 1 | 0.787 | 0.787–0.787 | 0.787 |
| svm | identity-manual | 1 | 1.482 | 1.481–1.482 | 1.482 |
| svm | inline-pointer | 1 | 0.794 | 0.793–0.797 | 0.794 |
| svm | inline-pointer-u16 | 1 | 0.753 | 0.752–0.754 | 0.753 |
| svm | inline-pointer-u4 | 1 | 0.789 | 0.788–0.793 | 0.789 |
| svm | java-batch-16 | 16 | 0.800 | 0.799–0.803 | 12.793 |
| svm | java-batch-256 | 256 | 0.806 | 0.804–0.817 | 206.234 |
| svm | java-batch-4 | 4 | 0.974 | 0.973–0.975 | 3.896 |
| svm | java-batch-64 | 64 | 0.757 | 0.755–0.759 | 48.442 |
| svm | manual-leaf | 1 | 1.532 | 1.531–1.537 | 1.532 |
