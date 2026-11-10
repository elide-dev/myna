# Symbols and Cloud context

Native builds extract symbol sidecars into `build/dist/symbols/` before stripping executables. Linux uses ELF debug files and `.gnu_debuglink`; macOS uses flat dSYM companions with matching UUIDs. Native Image does not currently emit source-level DWARF on macOS, so its sidecar preserves method symbols. The LLVM applicator retains C++ debug information where available from its inputs.

A Mach-O UUID identifies the linked image, and a linked image holds no DWARF: it stays in the object files until `dsymutil` collects it. `ld64.lld` derives `LC_UUID` from the image, so a source change that moves lines without changing machine code keeps the UUID while changing the dSYM. The v0.9.1 and v0.10.0 applicators were byte-identical after stripping (SHA-256 `61c25b9e…`, UUID `4C4C4476-5555-3144-A1D3-F7633821EC98`) because the only change to `llvm/main.cpp` between them was reformatting, yet their dSYMs differed in line tables and `DW_AT_decl_line`/`DW_AT_call_line` values (7,062,800 and 7,062,920 bytes). The symbol server correctly refused the second sidecar under the first one's identity; v0.11.0 and v0.12.0 repeated the pattern after a license header was added. Both dSYMs were valid for the same machine code; they disagreed only about source positions.

`llvm/debug-identity.cmake` therefore links a `__TEXT,__myna_debug_id` section holding a SHA-256 over the applicator's object files, which do contain the DWARF. The image, and so its UUID, now changes whenever the debug content can, while identical inputs still reproduce identical executables and sidecars. `scripts/build.sh` refuses a macOS applicator without the section, and `tests/symbols.py` builds a line-shifted fixture both ways: the unbound canary must still share one UUID across two different dSYMs, and the bound build must not. ELF build IDs already hash the linked `.debug_*` sections. The Native Image dSYM's compile units come from the pinned GraalVM distribution's static libraries, not from Myna sources. Sidecars already published under a shared UUID are immutable: leave the stored bytes in place and do not republish a different sidecar under that identity.

`Publish symbol sidecars` builds Linux release binaries on tags, or on explicit workflow dispatch. macOS sidecars are no longer published automatically; publish them manually with the [recovery commands](releases.md#symbols-and-recovery) when needed. It uploads through the `elide` organization's managed Elide Cloud connection for project `myna`. The approved provider is **debuginfo staging**, `https://api.staging.debuginfo.dev`. Production DNS was unavailable during onboarding. Change the managed connection when moving to production; credentials remain in Cloud rather than in artifact metadata.

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
