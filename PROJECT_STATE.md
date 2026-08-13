# PROJECT_STATE.md — Video Resizer

Snapshot as of **Batch 11**. This is the first-read file per the context
hierarchy (Chat Saat Ini > this file > FILE_MANIFEST.txt > CHANGELOG.md >
README.md) — update it at the end of every batch rather than making it
stale. Full detail for anything summarized here lives in CHANGELOG.md;
architecture/quirk notes live in README.md.

## Current version
- `versionName`/`versionCode` both dynamic since Batch 8: base semantic
  label **`"1.14"`** (bumped from `1.13` this batch — `val
  semanticVersionName` in `app/build.gradle.kts`, bump manually per feature
  batch) with `-build<n>` appended in CI; `versionCode` =
  `1000 + $GITHUB_RUN_NUMBER`. Both fall back to plain `1.14` / `1014` for
  any non-CI build.
- Package: `com.example.videoresizer`, minSdk 24, targetSdk/compileSdk 34
- AGP 8.4.0, Kotlin 1.9.24, Gradle 8.7 (no wrapper jar — see FILE_MANIFEST.txt)
- **Media3: 1.4.1** (bumped from 1.3.1 this batch, see Batch 9 below).
  Deliberately not bumped further — see "Defaults a new reader should know".

## Batch history (newest first — full detail in CHANGELOG.md)
- **Batch 11** — Debug/polish pass over Batch 9/10's GIF + target-size +
  Studio history code (no new features): GIF playback-delay drift fix,
  corrupt-frame guard, a perf fix in GIF quantization, a target-size-clamp
  warning in the dialog, flip/frame-rate now shown in Studio history
  cards, and three `!!` force-unwraps in `GifScreen` rewritten to match
  the file's existing convention.
- **Batch 10** — Fixed the real `:app:compileReleaseKotlin` failure Batch 9
  shipped (`GifExporter.kt` LongArray `+=` Int type mismatch — see
  CHANGELOG), plus a `log_fail_<version>_<run_number>` GitHub Actions
  artifact uploaded automatically whenever a build fails (captured Gradle
  output + reports), so future failures don't need a manual "download log
  archive" round-trip to diagnose.
- **Batch 9 (Atomic)** — Video ke GIF (`GifEncoder.kt`/`GifExporter.kt`,
  new files, own from-spec GIF89a/LZW encoder, no Transformer involved),
  Flip/mirror + Frame Rate control in the main Resizer screen (Media3
  bumped 1.3.1→1.4.1 for `FrameDropEffect`), and Compress-by-Target-Size
  (MB) reusing the existing CUSTOM-bitrate plumbing rather than adding a
  new pipeline field. BatchScreen intentionally not extended with the two
  new resize controls this round — see CHANGELOG's "Not done this batch".
- **Batch 8** — Caption text overlay (Resizer + Batch screens, reuses the
  watermark's overlay pipeline), per-item thumbnails in the Batch Export
  queue, caption fields added to Studio history/"Edit ulang", and
  `versionName` made dynamic (`-build<n>` suffix, same as `versionCode`).
  Closes out every previously-pending item at once.
- **Batch 7** — `versionCode` is now dynamic: `1000 + $GITHUB_RUN_NUMBER`
  (was a static `13` since before Batch 1). `versionName` stayed manual in
  this batch specifically — see Batch 8, which revisited that.
- **Batch 6** — Added this file and FILE_MANIFEST.txt (were missing since
  Batch 1 despite being required by the context hierarchy).
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
- **Media3 pinned at 1.4.1, do not bump past 1.5.x without care** (Batch
  9) — 1.6.0 flips the `OverlaySettings` anchor-point sign convention used
  by watermark/caption placement (`ScaleAndRotateTransformation`/
  `OverlayEffect` usage in `VideoResizer.kt`). Any future media3 bump needs
  that anchor math re-verified against whatever version is being moved to.
- **GIF export is a separate pipeline**, not part of `VideoResizer`/
  Transformer — `GifExporter.kt` decodes/quantizes/encodes everything
  itself. If GIF output quality ever needs to improve, the palette
  algorithm (`GifExporter.buildPalette`, a frequency-bucket approach) is
  the place to swap in something like median-cut/NeuQuant.

## Known pending items (not yet actioned)
- 🟡 **Batch 11 fixes not yet CI-verified** — same caveat as every batch:
  structural checks + manual review only, push and confirm the next
  Actions run is green.
- 🟡 **Batch 10 fix not yet CI-verified**: the `GifExporter.kt` type-mismatch
  fix and the new failure-log workflow step are both structurally checked
  only (brace/paren balance + YAML parse), same as every batch — push and
  check the next Actions run is genuinely green before treating Batch 9's
  GIF feature as confirmed-working.
- 🟡 Manual-only cleanup: the pre-Batch-5 duplicated `v1.13` GitHub Release
  (old tag collision) — needs `gh release delete v1.13 -y` on the user's
  end; not something a code batch can retroactively fix.
- ⚪ Not done, flagged as risky-without-a-real-build (see CHANGELOG.md
  Batch 4 "Deliberately not done" section): AGP/Kotlin version bump,
  `org.gradle.configuration-cache=true`.
- ⚪ **Batch 9 scope cut**: `BatchScreen` doesn't yet expose Flip/Frame
  Rate/Target-Size — batch jobs use safe defaults (NONE/ORIGINAL) so
  nothing broke, but the controls only exist in the single-video Resizer
  screen for now.
- ⚪ **Batch 9 scope cut**: GIF exports aren't written to `VideoHistoryStore`
  — no "Edit ulang" for a past GIF yet, share/gallery-save only.

## Known constraints on this side (Claude's sandbox)
- No `gradle`/`kotlinc`/`gh` available here — every batch is verified by
  structural checks (brace/paren balance, XML/YAML parse) and careful
  manual review, not an actual compile. First real compile signal is
  always the next GitHub Actions run after push.
- Media3 API surface used in Batch 9 (`FrameDropEffect
  .createDefaultFrameDropEffect`, `ScaleAndRotateTransformation
  .Builder().setScale(x, y)`) was verified against public docs/real-world
  usage examples via web search rather than a local compile — same
  "structural checks + review, not a real compile" caveat as always
  applies; treat the next CI run as the actual first signal.

