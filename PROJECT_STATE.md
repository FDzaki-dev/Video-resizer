# PROJECT_STATE.md — Video Resizer

Snapshot as of **Batch 24**. This is the first-read file per the context
hierarchy (Chat Saat Ini > this file > FILE_MANIFEST.txt > CHANGELOG.md >
README.md) — update it at the end of every batch rather than making it
stale. Full detail for anything summarized here lives in CHANGELOG.md;
architecture/quirk notes live in README.md.

## Standing rules (permanent — read first, applies to every future session without exception)

- **Automated versioning rule (added Batch 22).** `versionCode` AND
  `versionName` are BOTH derived automatically from CI (`$GITHUB_RUN_NUMBER`
  via `VERSION_CODE_OVERRIDE`) in `app/build.gradle.kts` — see "Current
  version" below for the exact formula. **No session may ever reintroduce a
  hand-bumped per-batch version literal** (the old `semanticVersionName =
  "1.19"`-style pattern, bumped manually batch-to-batch, is retired for
  good — it was the direct cause of the version-history gap noted in the
  old Batch 12/20 entries below). The only manual version constant that
  still exists anywhere is `appVersionMajor` in `app/build.gradle.kts`
  (currently `1`) — touch it ONLY for a deliberate breaking/milestone
  release, never as routine batch work, and if it IS deliberately changed,
  the matching literal `"1"` in `.github/workflows/build.yml`'s two
  `VERSION_NAME="1.${GITHUB_RUN_NUMBER}"` lines must be updated to match in
  the same batch (both sites carry a cross-reference comment pointing at
  each other and at this rule). This rule itself is not a batch-history
  item and must not be treated as "resolved and removable" — it's a
  permanent policy.

## Current version
- **Fully automated since Batch 22 — see "Standing rules" above.**
  `versionCode = (VERSION_CODE_OVERRIDE ?: 13) + 1000`; `versionName =
  "$appVersionMajor.$VERSION_CODE_OVERRIDE"` (e.g. `"1.47"`) when running in
  CI, else `"$appVersionMajor.0-dev"` for any local/non-CI build.
  `appVersionMajor` is currently `1`. Nothing here requires — or should
  ever again receive — a manual per-batch bump.
- Historical note (pre-Batch-22, kept for context only — NOT an active
  issue): versionName used to be a hand-maintained `semanticVersionName`
  literal bumped once per batch (last manual value was `"1.19"`, Batch 20).
  A version-history gap was noticed in Batch 12 (uploaded project was a
  full minor ahead of what PROJECT_STATE.md recorded) — this whole class of
  bug is what the Batch 22 automation rule exists to permanently prevent.
- Package: `com.example.videoresizer`, minSdk 24, targetSdk/compileSdk 34
- AGP 8.4.0, Kotlin 1.9.24, Gradle 8.7 (no wrapper jar — see FILE_MANIFEST.txt)
- **Media3: 1.4.1** (bumped from 1.3.1 this batch, see Batch 9 below).
  Deliberately not bumped further — see "Defaults a new reader should know".

## Pending Queue (not done this batch — do next, in this order)
1. **Dokumentasi repository wajib Bahasa Indonesia saja**: README.md dan
   CHANGELOG.md saat ini campur Bahasa Inggris (ditulis dari sesi-sesi
   sebelumnya). Perlu diterjemahkan penuh ke Bahasa Indonesia. Ini
   pekerjaan besar (README.md ~36KB, CHANGELOG.md ~52KB) — akan dikerjakan
   bertahap per section di batch-batch berikutnya, bukan sekaligus, sesuai
   Strict Micro-Batching Rule (menghindari ZIP terpotong).

## Batch history (newest first — full detail in CHANGELOG.md)
- **Batch 26** — Closed Pending Queue item 1 (feedback audit). Added
  `LocalHapticFeedback` (`HapticFeedbackType.LongPress`) to the 5 highest-
  value action points that had zero tactile feedback before: Resize CTA
  (`ResizerScreen`), Batch start (`BatchScreen.startBatch()`), GIF convert
  start (`GifScreen`), Compress start (`CompressorScreen`), and the
  destructive delete-confirm in `StudioScreen`'s history list. Loading-
  state coverage was already complete (all 4 processing screens already
  had `isProcessing` + progress UI pre-existing) so no changes needed
  there; snackbar/toast coverage for errors was also already present.
  1 file touched (`MainActivity.kt`), within cap.
- **Batch 25** — User request: fold every top-bar feature except Studio
  into the "More" overflow menu. `Compress`/`Batch export`/`Video ke GIF`
  IconButtons removed from `ResizerScreen`'s `TopAppBar` `actions` and
  re-added as `DropdownMenuItem`s at the top of the existing `showMoreMenu`
  `DropdownMenu` (above the "Cek update" divider, in Compress→Batch→GIF
  order), calling the same `onOpenCompressor`/`onOpenBatch`/`onOpenGif`
  lambdas. Studio (`onOpenStudio`) intentionally left as the sole
  standalone icon per explicit instruction. Top bar is now just
  `[Studio, More]` (2 icons) — title has more room than at any point since
  Batch 20. 1 file touched (`MainActivity.kt`), within cap.
- **Batch 24** — Follow-up fix on Batch 23's title-truncation fix (user:
  "distorsi sih nggak, tapi gak gini juga kalik" — no longer garbled, but
  "Vi…" was still unacceptable). Root cause was the bar carrying 6 action
  icons since Batch 21 (Update/Compress/Batch/GIF/Studio/Theme), squeezing
  the title slot too narrow even with maxLines=1+Ellipsis. Real fix:
  folded Update + Theme (both settings-like, not core editing actions)
  into one "More" (`MoreVert`) overflow `DropdownMenu` — restores the
  original 5-icon-wide bar (Compress/Batch/GIF/Studio/More) the title had
  before Batch 21, so "Video Resizer" fits without truncation on typical
  screen widths again. `showThemeMenu` state renamed `showMoreMenu` (now
  covers both). Update-check menu item shows "Mengecek update…" + disables
  itself while in flight — small incidental improvement toward Pending
  Queue item 1, not a full fix (see below). 1 file touched
  (`MainActivity.kt`).
- **Batch 23** — Fixed real UI bug reported via screenshot: `ResizerScreen`'s
  `TopAppBar` title ("Video Resizer") had no `maxLines`/`overflow`, and with
  6 action icons now crammed into the bar since Batch 21's update button,
  the shrunk title slot caused the text to wrap onto a second line and get
  clipped by the bar's fixed height — rendered as garbled fragments
  ("eo"/"Res" per the screenshot). Fixed: `Modifier.weight(1f, fill=false)`
  + `maxLines = 1` + `TextOverflow.Ellipsis` on the title `Text`, so it now
  always renders as one clean line (ellipsized on very narrow screens
  instead of wrapping-then-clipping). 1 file touched (`MainActivity.kt`) —
  the single most critical item from this batch's 3-item request; items 2
  and 3 recorded above in Pending Queue per Strict Micro-Batching Rule.
- **Batch 22** — Automated versioning (standing rule, see above): removed
  the hand-maintained `semanticVersionName` literal from
  `app/build.gradle.kts` entirely. `versionName` is now
  `"$appVersionMajor.$VERSION_CODE_OVERRIDE"`, fully derived from CI same
  as `versionCode` already was. `.github/workflows/build.yml`'s two steps
  that used to `grep` that literal out of the gradle file now compute
  `VERSION_NAME="1.${GITHUB_RUN_NUMBER}"` directly in bash instead (kept
  cross-referenced with the gradle constant via comments on both sides).
  Release tag format unchanged (`v<versionName>-build<n>`, slightly
  redundant now but kept byte-for-byte compatible with `AppUpdater.kt`'s
  `Regex("build(\\d+)")` parser from Batch 21 — so that file needed zero
  changes this batch). 2 protected files touched
  (`app/build.gradle.kts`, `.github/workflows/build.yml`) — within cap.
- **Batch 21** — New **in-app updater**: top bar gains a `SystemUpdate` icon
  button (`ResizerScreen`, next to Compress/Batch/GIF/Studio) that hits
  GitHub's Releases API (`GET /repos/FDzaki-dev/Video-resizer/releases/latest`)
  via a new `AppUpdater.kt` — no new Gradle dependency, uses
  `HttpURLConnection` + platform `org.json` instead of OkHttp/Retrofit for
  one endpoint + one download. Compares the release tag's `-build<N>`
  suffix against the installed `versionCode` (both already `1000+N` per
  `app/build.gradle.kts`/`build.yml`'s existing scheme — no separate
  "latest version" field needed). Shows an `AlertDialog` with release notes
  + "Unduh & Pasang"; download streams chunk-by-chunk straight to
  `cacheDir/updates/update.apk` (never `readBytes()` into RAM), explicit
  15s connect / 20s read timeouts, `followRedirects(true)` for the
  asset's S3/CDN 302, optional `Authorization: Bearer` header (blank/unused
  today — public repo). Install hands off to the system installer via the
  existing `FileProvider` authority (`file_paths.xml`'s `cache-path`
  already covers `cacheDir`, no change needed) with an
  `ACTION_VIEW`/`application/vnd.android.package-archive` intent; if
  `canRequestPackageInstalls()` is false, opens
  `ACTION_MANAGE_UNKNOWN_APP_SOURCES` first. New manifest permissions
  `INTERNET` + `REQUEST_INSTALL_PACKAGES` (edit-parsial on the protected
  `AndroidManifest.xml`). 3 files touched (`AppUpdater.kt` new,
  `MainActivity.kt`, `AndroidManifest.xml`) — at the Strict Micro-Batching
  cap.
- **Batch 20** — Fixed real CI compile failure from Batch 19 (run confirmed
  failed, `compileReleaseKotlin FAILED`, uploaded failure-log artifact
  analyzed directly per this project's own "crash/failure log first"
  convention): `CompressorScreen` in `MainActivity.kt` used `TopAppBar`/
  `TopAppBarDefaults.topAppBarColors` (both experimental Material3 APIs)
  without `@OptIn(ExperimentalMaterial3Api::class)` — same exact class of
  gap Batch 16 fixed for `VideoPickerScreen`, just missed on this new
  function. `GifScreen`, right above it in the same file, already carries
  this annotation; `CompressorScreen` simply didn't get it copied over.
  Fixed by adding `@OptIn(ExperimentalMaterial3Api::class)` directly above
  `private fun CompressorScreen(...)`. No other code touched — semantic
  version stays `1.19`, this is a pure compile fix.
- **Batch 19** — New **Compressor tab**: `Screen.COMPRESSOR` + a
  `CompressorScreen` composable (own top-bar icon, `Icons.Filled.Compress`,
  next to Batch/GIF/Studio), backed by a new `VideoResizer.compress()` path
  that is entirely additive — `ResizeRequest`/`resize()` untouched. Picks a
  video (in-app `VideoPickerScreen`, same as Resizer/GIF), lets the user
  trim, pick **Rekomendasi** or **Maksimal** compression, shows a live
  before/after size estimate, then re-encodes at the *same resolution* as
  **H.265/HEVC** at a much lower bitrate than the source's own codec needs
  for the same perceived quality — that's the actual mechanism behind
  "smaller file, same visual quality": HEVC efficiency, not literally
  lossless re-encoding (no re-encode of an already-lossy video can be
  truly lossless — see `CompressionLevel`'s doc comment in
  `VideoResizer.kt`). A safety cap (`estimateSourceBitrateBps`) means the
  target bitrate is never set *above* ~85% of the source's own estimated
  bitrate, so an already-efficient source is left alone instead of being
  re-encoded larger. Saves to Studio history with `kind = "COMPRESS"`
  (no `VideoHistoryEntry` schema change — reuses existing fields). Not
  done this batch, see Known pending items below: "Edit ulang" on a
  COMPRESS history entry currently reopens Resizer with default settings
  rather than reopening Compressor.
- **Batch 18** — Fixed real CI compile failure from Batch 17 (run confirmed
  failed, `compileReleaseKotlin FAILED`): `AnimatedVisibility(...)` at
  MainActivity.kt:2356 resolved to the `ColumnScope.AnimatedVisibility`
  extension (found via implicit-receiver search up through the enclosing
  Card's Column, even though the call site is inside a nested Box) instead
  of the intended top-level `androidx.compose.animation.AnimatedVisibility`
  composable — Kotlin's error: "can't be called in this context by
  implicit receiver". Fixed by fully-qualifying the call
  (`androidx.compose.animation.AnimatedVisibility(...)`) and dropping the
  now-unused bare import. Batch 17's other 2 changes (TrimHandle clamp,
  controlsVisible state/timer) were untouched — only this one call site.
- **Batch 17** — 3 UI bugs reported against `VideoEditorPreview` (screenshot,
  currently-installed APK — **note**: no APK has been released since before
  Batch 16's compile fix, so this screenshot is almost certainly an OLD
  build; re-verify all 3 once the new release installs):
  1. Trim handle "gets thinner toward the ends" — real bug, confirmed in
     current source: the visible 16dp bar was centered exactly on
     `fraction * trackWidthPx` with no clamp of its own, so near fraction
     0f/1f up to half the bar sat outside `[0, trackWidthPx]` and got
     sliced off by the shared rounded-corner `.clip()` on the parent Box.
     Fixed: added `barCorrectionPx` that clamps the bar's own left edge to
     `[0, trackWidthPx - handleWidthPx]` and nudges it inward by exactly
     that amount, so the full bar is always inside the clip — no more
     shrinking, and it now sits flush (not overhanging) at the extremes.
  2. "Big empty gaps" around the `720×720 • Potong: ...` details row —
     **could not reproduce in current source**: that Card uses
     `spacedBy(12.dp)` internally and the outer Column uses
     `spacedBy(20.dp)`, nowhere near the ~130dp/~110dp gaps described. No
     `weight()`, no reserved-height placeholder, nothing else in that
     Composable that could produce this. Left AS-IS pending a fresh
     screenshot from the Batch 16+ build — likely explained by #2's note
     above (stale APK).
  3. Play/pause button never fades (during playback or while paused) — real
     bug, confirmed: there was no auto-hide logic at all, `IconButton` was
     permanently opaque. Added `controlsVisible` state + `AnimatedVisibility`
     (fadeIn/fadeOut): auto-hides 2.5s into playback, reappears on tap
     (button or video area), stays visible whenever paused/static.
- **Batch 16** — Fixed real CI compile failure (run #46, `assembleRelease`
  exit 1): `VideoPickerScreen.kt`'s `VideoPickerScreen` composable (uses
  `TopAppBar`/`DropdownMenu`/`DropdownMenuItem`, all experimental Material3
  APIs) was missing `@OptIn(ExperimentalMaterial3Api::class)` — every other
  Composable using these APIs project-wide already carries this annotation
  (see `MainActivity.kt`), this one function was the sole gap. Added the
  annotation directly above `internal fun VideoPickerScreen(...)`. Also
  confirms the `log_fail_<version>_<run-number>` failure-artifact step
  (`Prepare failure log name` / `Upload failure logs` in `build.yml`,
  added Batch-unknown before this file's tracking) was simply absent from
  *that specific* failed run's log bundle because it predates this fix —
  `build.yml` already has both steps present and correctly gated on
  `if: failure()`; no workflow change needed, only the compile fix.
- **Batch 15** — Added Stale Run Guard to `build.yml`: new first step
  (right after checkout, before JDK setup/build/sign/release) compares
  `$GITHUB_SHA` against `origin/main`'s current tip via `git ls-remote`.
  Mismatch (e.g. a GitHub "Re-run jobs" click on an old run after main has
  since moved forward) aborts with `exit 1` before any build or publish
  step runs — prevents a stale re-run from silently publishing an
  outdated APK as a fresh GitHub Release. No app code touched.
- **Batch 14** — Added `.gitattributes` (was missing since project start
  despite being a [PROTECTED] asset category in user preferences): forces
  LF line endings on all source/text files and marks `release.keystore`/
  `*.apk`/`*.aab`/images explicitly `binary -diff -merge` so git never
  attempts text-mode EOL conversion or diff/patch on them. No app code
  touched.
- **Batch 13b** — Follow-up to Batch 13's flagged "UI asimetris" item:
  user's clarification ("semua bagian yang gak kelihatan simetris") wasn't
  specific enough to target, so rather than keep asking, shipped the one
  concrete issue found on review — `VideoEditorPreview`'s trim handles
  visually overhanging the filmstrip's rounded edges by ~8dp at the two
  trim extremes, now fixed by clipping the whole filmstrip+handles Box to
  one shared shape. Still open if this isn't what the user meant — see
  pending items.
- **Batch 13** — New `VideoPickerScreen.kt`: full-screen in-app video
  picker (MediaStore-backed, Videos/Folders tabs, list rows with
  thumbnail/name/duration/resolution/date/size, sort menu, explicit
  "Batal" cancel), replacing the OS Photo Picker for `ResizerScreen`'s and
  `GifScreen`'s single-video pick only — `BatchScreen`'s multi-pick and
  both watermark-image pickers are unchanged (see CHANGELOG's "out of
  scope" note). "UI asimetris" on the trim editor was investigated but not
  fixed — see "Known pending items" below.
- **Batch 12** — Closed both of Batch 9's explicit scope cuts: BatchScreen
  now has Flip/Frame Rate/Target-Size (MB) — the last one resolved
  per-item against each queued video's own duration, not one shared
  bitrate — and GIF exports now save to Studio history with their own
  `kind`/"Edit ulang" (`GifPrefill`)/share-mime handling.
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
- **Single-video pick goes through `VideoPickerScreen.kt`, not the OS
  Photo Picker, as of Batch 13** — `ResizerScreen`/`GifScreen` only.
  `BatchScreen`'s multi-pick and the two watermark-image pickers still use
  `ActivityResultContracts.PickVisualMedia`/`PickMultipleVisualMedia`
  unchanged. If multi-select ever needs the same list-style treatment,
  that's new scope, not an extension of the existing single-pick screen.

## Defaults a new reader should know (cont'd — Compressor, Batch 19)
- **Compressor is a separate pipeline call, not a mode of `resize()`** —
  `VideoResizer.compress()`/`CompressRequest`/`CompressionLevel` are all
  new, additive members; nothing about `ResizeRequest`/`resize()`/
  `QualityOption` changed.
- **Always forces H.265/HEVC** via `TransformationRequest.setVideoMimeType`
  + `DefaultEncoderFactory.setEnableFallback(true)` — falls back to
  whatever the device's encoder supports if no HEVC hardware encoder is
  present, same fallback pattern `resize()`'s bitrate path already uses.
- **No aspect/resolution/watermark/caption controls** — same resolution as
  source in, source (or trimmed clip) out, just re-encoded smaller.

## Known pending items (not yet actioned)
- 🟡 **Compressor "Edit ulang" not wired (Batch 19)** — a `kind =
  "COMPRESS"` Studio history entry falls into `onEditAgain`'s default
  (non-GIF) branch today, which reopens **Resizer** with mostly-default
  settings rather than reopening **Compressor** with its trim/level
  restored. Needs a `CompressPrefill` data class + a third branch in
  `VideoResizerApp`'s `onEditAgain`, mirroring how `GifPrefill` already
  works — deferred to keep Batch 19 to its one task (add the tab) per the
  micro-batching rule.
- 🟡 **"UI asimetris" on the trim editor — one concrete cause fixed
  (Batch 13b), may not be the whole story.** User's clarification was
  "semua bagian yang gak kelihatan simetris" (not specific enough to
  target further), so rather than keep asking, Batch 13b shipped the one
  issue actually found on review: the trim handles overhanging the
  filmstrip's rounded edges by ~8dp at the trim extremes — now fixed via
  a shared clip on the filmstrip+handles Box. No other structural cause
  was found in `VideoEditorPreview` (player box/time-labels/details row
  all plain `spacedBy(12.dp)`, no `weight`/`fillMaxHeight`). **If this
  wasn't it: next useful input is a marked-up screenshot** (arrow/circle
  on the actual element) rather than another verbal description — visual
  layout bugs are hard to pin down blind, and two rounds of guessing
  without one risks burning batches on a no-op.
- 🟡 **Batch 13/13b changes not yet CI-verified** — same caveat as every
  batch: structural checks (brace/paren balance) + manual review only,
  push and confirm the next Actions run is green.
- 🟡 Manual-only cleanup: the pre-Batch-5 duplicated `v1.13` GitHub Release
  (old tag collision) — needs `gh release delete v1.13 -y` on the user's
  end; not something a code batch can retroactively fix.
- ⚪ Not done, flagged as risky-without-a-real-build (see CHANGELOG.md
  Batch 4 "Deliberately not done" section): AGP/Kotlin version bump,
  `org.gradle.configuration-cache=true`.

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

