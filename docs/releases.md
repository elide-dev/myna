# Release artifacts and verification

Myna's own code is available under **Apache-2.0 OR MIT**, at the recipient's option. New source archives, portable SDK builds, JAR resources, and container images carry the license texts. Third-party packages retain their own terms; see [THIRD_PARTY.md](../THIRD_PARTY.md).

## Release process

1. Update the version and regenerate versioned golden outputs. Refresh every lockfile that records the workspace crates, including the fuzz crate's own (`cargo update --offline -w` at the root and `cargo update --offline -p myna-values --manifest-path crates/myna-values/fuzz/Cargo.toml`); CI fetches both with `--locked`. Merge the reviewed change after required checks pass.
2. Create a signed `v<version>` tag on the release commit. Version tags are immutable under repository rules; correct an unsuccessful candidate with a new tag rather than moving an existing one.
3. The artifact workflow builds the portable SDK and an exact `git archive` source archive, generates checksums and a source SBOM, and attests the bytes produced in that workflow. Tag runs create or populate a **draft** release and refuse to replace existing assets or write into an already published release. Manual branch runs retain verification artifacts without modifying a release.
4. Check Linux/macOS validation, container publication, and Linux symbol receipts. Review the draft notes and assets, then publish the draft. Do not publish before artifact jobs have attached their outputs.

The Docker release job smoke-tests each image before publication, records BuildKit provenance, scans the immutable pushed digest, and attaches signed provenance and SBOM attestations to that digest. Source and image inventories are distinct: the source SBOM covers first-party declared dependencies, including development/build dependencies; vendored benchmark submodules and generated build trees are excluded. Inherited workspace license declarations are resolved from Cargo metadata; this does not alter dependency licenses or claim license conclusions. An image SBOM inventories detected packages in that image and cannot prove that every statically linked component was identified.

The release downloads also retain the original Sigstore provenance bundle
(`myna-provenance.sigstore.json`) and its signed in-toto DSSE envelope as a single
JSON line (`myna-provenance.intoto.jsonl`). The latter makes provenance discoverable
to clients such as Scorecard; use the full bundle for offline signature verification.
These describe only the archives built by that release workflow.

## Consumer verification

Check checksums before using downloaded files. For artifacts produced by the provenance workflow:

```sh
gh attestation verify myna-values-VERSION.tar.gz --repo elide-dev/myna
gh attestation verify myna-source-VERSION.tar.gz --repo elide-dev/myna
gh attestation verify myna-values-VERSION.tar.gz --repo elide-dev/myna --bundle myna-provenance.sigstore.json
gh attestation verify oci://ghcr.io/elide-dev/myna@sha256:DIGEST --repo elide-dev/myna
```

Verify the attestation's source revision, workflow identity, and artifact digest against the intended release. A signature alone is not an assertion that a build was audited or vulnerability-free. Do not substitute a similarly named manual-run artifact for the published release asset.

The current release tags use X.509 signatures. The public certificate and chain travel with the signed tag; verify them with a trusted S/MIME verifier such as [smimesign](https://github.com/github/smimesign):

```sh
git fetch origin tag v0.9.1
git -c gpg.format=x509 -c gpg.x509.program=smimesign verify-tag v0.9.1
```

For v0.9.1 the signer certificate ID is `8db2725ec24ba177cd2adef718a9da68f0785972`. From v0.12.0 through v0.13.0, tags are signed with the OpenPGP key `15FAB722A29D5AD20917A0C50A11B04C1623040F` (see [0.12.0](releases/0.12.0.md)). From v0.14.0, tags are signed with the OpenPGP key `A166AC62F6DE8C63732277731D5903AF4582BCF4` (Sam Gammon, registered on the maintainer's GitHub account, so GitHub marks the tags Verified). Verify with `git verify-tag` against the fingerprint for the tag's version. Validate both certificate trust and the intended release identity; simply extracting a certificate from an untrusted tag does not establish trust. Later signer changes must be documented in a reviewed release change. Signing private keys stay with the authorized signer, never on the public distribution service. CI artifact attestations instead use short-lived Sigstore signing identities: `gh attestation verify` obtains the public verification material and checks the signed bundle against the named repository. The source archive and SDK have their own signed artifact digests; a tag signature alone does not authenticate an arbitrary binary download.

The GitHub-generated source archive and the workflow's attested `git archive` asset are separate byte streams with separate checksums. Existing v0.7.2 SDK bytes and hashes remain unchanged; CI provenance is not retroactively claimed for that locally packaged artifact. Its license texts and recovered Linux/macOS symbol receipts are attached to the release. Later CI-built releases use the provenance workflow above.

## Symbols and recovery

See [operations](operations.md) for the managed staging connection. Build IDs/UUIDs must be nonempty and match the executable and sidecar. One identity must also name exactly one sidecar: the macOS LLVM applicator binds its UUID to its debug information (see [operations](operations.md)), so a `debuginfo_conflict` (HTTP 409) means different bytes were built under an already published identity and must not be retried or overridden. Native Image on macOS supplies method symbols rather than full source DWARF.

The publisher retries transient failures only for replayable reads, content-addressed uploads, and symbol publication. It retains each successful receipt immediately. To recover a failed publication without rebuilding, download the original workflow's artifact, verify its source commit and platform, and run:

```sh
python3 scripts/publish-symbols.py --revision FULL_SOURCE_COMMIT --target linux-x86_64 path/to/symbols/*.debug
```

Use `darwin-arm64` and `.dsym` files for macOS. Preserve the original artifact bytes; never relabel a newly built binary as an earlier release's symbols. Credentials come from the authorized environment and must not appear in receipts, logs, or source control.
