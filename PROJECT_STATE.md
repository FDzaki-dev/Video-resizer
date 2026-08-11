# PROJECT_STATE.md — Video Resizer

Snapshot as of **Batch 6**. This is the first-read file per the context
hierarchy (Chat Saat Ini > this file > FILE_MANIFEST.txt > CHANGELOG.md >
README.md) — update it at the end of every batch rather than making it
stale. Full detail for anything summarized here lives in CHANGELOG.md;
architecture/quirk notes live in README.md.

## Current version
- `versionName` "1.13" / `versionCode` 13 (unchanged since before Batch 1 —
  none of Batches 1–5 bumped it, since none were user-facing feature
  changes to the resize/export pipeline itself. Bump it whenever a batch
  changes what the app actually *does*, not for CI/theming/housekeeping.)
- Package: `com.example.videoresizer`, minSdk 24, targetSdk/compileSdk 34
- AGP 8.4.0, Kotlin 1.9.24, Gradle 8.7 (no wrapper jar — see FILE_MANIFEST.txt)

## Batch history (newest first — full detail in CHANGELOG.md)
- **Batch 5** — Fixed GitHub Release tag/APK-name collisions: tag now
  includes `$GITHUB_RUN_NUMBER` so repeat pushes without a versionName
  bump stop overwriting/duplicating the same release.
- **Batch 4** — CI build-speed tuning (gradle.properties heap/cache flags,
  `lint.checkReleaseBuilds = false`, explicit `--parallel --build-cache`).
  Config-only, zero app-code files touched.
- **Batch 3 (Atomic)** — "Midnight Blue Glass" theme (iOS glassmorphism +
  midnight-blue gradient), now the app's default. 4 other themes still
  selectable. Touched `ui/theme/*` + UI-only parts of `MainActivity.kt`.
- **Batch 2** — Built-in crash logger (`CrashLogger.kt`,
  `VideoResizerApp.kt`, MediaStore-based, FIFO 50), GitHub Release
  publishing (was Actions-artifact-only before), cleaned all remaining
  force-unwrap (`!!`) in `MainActivity.kt`/`VideoResizer.kt`.
- **Batch 1** — (pre-dates this file; see CHANGELOG.md/README.md) core
  export-path crash hardening, `VideoHistoryStore.kt` audit, keystore
  tracking regression fix.

## Defaults a new reader should know
- **Default theme**: `MIDNIGHT_BLUE_GLASS` (Batch 3) — not `DARK`. Theme
  picker in the top bar still offers Light/Dark/Midnight Neon/Warm
  Paper/Midnight Blue Glass, all fully working.
- **`isMinifyEnabled = false`** for release builds, deliberately — R8 risk
  with Media3 Transformer's reflection use hasn't been verified with a real
  device install. Do not flip this on without a real test pass.
- **CI publishes to GitHub Releases**, not just Actions artifacts, tagged
  `v<versionName>-build<run_number>` (Batch 5). Needs repo secret
  `RELEASE_KEYSTORE_PASSWORD` to sign correctly — verify it's still set if
  a release APK won't install over a previous one.
- **Crash logs** land in `Documents/VideoResizer/logs/` on-device via
  MediaStore, FIFO-capped at 50 files — check there first before asking
  for Logcat/ADB on any crash report.

## Known pending items (not yet actioned)
- 🔵 Optional, not yet built: text caption overlay, per-batch-item preview
  thumbnails in the Batch Export queue (from README's original "next
  steps" notes, still true).
- 🟡 Manual-only cleanup: the pre-Batch-5 duplicated `v1.13` GitHub Release
  (old tag collision) — needs `gh release delete v1.13` on the user's end;
  not something a code batch can retroactively fix.
- ⚪ Not done, flagged as risky-without-a-real-build (see CHANGELOG.md
  Batch 4 "Deliberately not done" section): AGP/Kotlin version bump,
  `org.gradle.configuration-cache=true`.

## Known constraints on this side (Claude's sandbox)
- No `gradle`/`kotlinc`/`gh` available here — every batch is verified by
  structural checks (brace/paren balance, XML/YAML parse) and careful
  manual review, not an actual compile. First real compile signal is
  always the next GitHub Actions run after push.
