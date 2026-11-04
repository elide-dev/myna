# Symbols and Cloud context

Native builds extract symbol sidecars into `build/dist/symbols/` before stripping executables. Linux uses ELF debug files and `.gnu_debuglink`; macOS uses flat dSYM companions with matching UUIDs. Native Image does not currently emit source-level DWARF on macOS, so its sidecar preserves method symbols. The LLVM applicator retains C++ debug information where available from its inputs.

`Publish symbol sidecars` builds Linux and macOS release binaries on tags, or on explicit workflow dispatch. It uploads through the `elide` organization's managed Elide Cloud connection for project `myna`. The approved provider is **debuginfo staging**, `https://api.staging.debuginfo.dev`. Production DNS was unavailable during onboarding. Change the managed connection when moving to production; credentials remain in Cloud rather than in artifact metadata.

The repository secret `ELIDE_CLOUD_API_KEY` holds the `myna-symbols-ci` credential with only `build:read` and `build:write` scopes. Each run exchanges it for an access token and obtains a short-lived org-shared `myna` build lease. `scripts/publish-symbols.py` uses bounded, resumable parts with content hashes and immutable upload identities, then asks Cloud to forward the stored sidecars. It records provider receipts in `build/reports/symbol-publication.json`. Re-running the same upload reuses verified parts. Rotate or revoke the key through Cloud's key-management API and replace the GitHub secret; do not commit credentials.

For a local rerun with an authorized environment:

```sh
mise run build:llvm
python3 scripts/publish-symbols.py
```

The publisher accepts `ELIDE_CLOUD_ACCESS_TOKEN` for an interactive session or `ELIDE_CLOUD_API_KEY` for automation. The symbol connection must already be active; the script does not silently create or broaden access.

Cloud's members-only `myna` context project is `6a030a42-8859-45db-9919-65e021e56cf6`. Its GitHub binding tracks `elide-dev/myna` on `main` with automatic indexing. Repository instructions, including `AGENTS.md`, travel with source snapshots. Local trajectory import uses Cloud's project-scoped history adapters and immutable acceptance receipts. Acceptance is distinct from completed indexing; retain source failures and verify processing status. The adapters include visible conversation and terminal events and exclude reasoning blocks.

The current Cloud CLI's device-token poll omits the server-required `client=cli` field. A caller can use the documented Auth token endpoint with that field while retaining the same approved device code. Do not restart device approval or reuse credentials across product audiences to hide a client integration failure.


The v0.7.2 tag publication initially received transient staging HTTP 503 responses. Its original Linux and macOS CI artifacts were subsequently replayed successfully; all four sidecars have provider receipts attached to that release. The publisher now retries bounded transient failures for idempotent operations and saves receipts after each successful file. [Recovery commands](releases.md#symbols-and-recovery) preserve the original commit and platform when replaying retained artifacts.
