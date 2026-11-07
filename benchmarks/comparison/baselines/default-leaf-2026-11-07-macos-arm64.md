# Native call floor and bounded batching

Source: `83229a24c813e1e3f7ea197539402754ad4511f9`; 5 processes/case; smoke: False.

One operation is one scalar call (identity or recurrence, as labeled). Batches retain each noinline native scalar call; no control subtraction.
Native and SVM execute identical linked kernel objects. Generated leaves use binding ABI 2 defaults; custom pointers and bounded Java batches are additional internal Graal API experiments.

| Runtime | Case | Batch | Median ns/op | Process min–max | ns/boundary including work |
|---|---|---:|---:|---:|---:|
| native | batch-16 | 16 | 0.905 | 0.894–0.952 | 14.486 |
| native | batch-256 | 256 | 0.886 | 0.882–0.929 | 226.730 |
| native | batch-4 | 4 | 0.950 | 0.942–1.002 | 3.798 |
| native | batch-64 | 64 | 0.898 | 0.885–0.904 | 57.465 |
| native | control | 1 | 0.895 | 0.878–0.900 | — |
| native | direct | 1 | 1.010 | 0.995–1.065 | 1.010 |
| native | identity-direct | 1 | 0.455 | 0.442–0.467 | 0.455 |
| native | identity-direct-unrolled4 | 1 | 0.441 | 0.441–0.493 | 0.441 |
| native | identity-indirect | 1 | 0.659 | 0.508–0.677 | 0.659 |
| native | identity-indirect-unrolled4 | 1 | 0.494 | 0.443–0.515 | 0.494 |
| native | indirect | 1 | 1.013 | 0.996–1.046 | 1.013 |
| svm | batch-16 | 16 | 0.902 | 0.884–0.954 | 14.431 |
| svm | batch-256 | 256 | 0.898 | 0.887–0.944 | 229.782 |
| svm | batch-4 | 4 | 0.900 | 0.883–0.950 | 3.599 |
| svm | batch-64 | 64 | 0.900 | 0.884–0.938 | 57.581 |
| svm | control | 1 | 0.899 | 0.890–0.902 | — |
| svm | generated-leaf | 1 | 0.915 | 0.891–0.936 | 0.915 |
| svm | generated-normal | 1 | 3.592 | 3.490–3.685 | 3.592 |
| svm | identity-generated | 1 | 0.506 | 0.466–0.528 | 0.506 |
| svm | identity-inline-pointer | 1 | 0.522 | 0.458–0.531 | 0.522 |
| svm | identity-inline-pointer-u16 | 1 | 0.465 | 0.455–0.486 | 0.465 |
| svm | identity-inline-pointer-u4 | 1 | 0.515 | 0.496–0.524 | 0.515 |
| svm | identity-manual | 1 | 0.973 | 0.965–1.032 | 0.973 |
| svm | inline-pointer | 1 | 0.929 | 0.901–0.947 | 0.929 |
| svm | inline-pointer-u16 | 1 | 0.919 | 0.900–0.954 | 0.919 |
| svm | inline-pointer-u4 | 1 | 0.909 | 0.888–0.970 | 0.909 |
| svm | java-batch-16 | 16 | 0.903 | 0.886–0.937 | 14.446 |
| svm | java-batch-256 | 256 | 0.904 | 0.894–0.949 | 231.478 |
| svm | java-batch-4 | 4 | 0.929 | 0.889–0.950 | 3.715 |
| svm | java-batch-64 | 64 | 0.909 | 0.886–0.913 | 58.191 |
| svm | manual-leaf | 1 | 0.974 | 0.946–1.039 | 0.974 |
