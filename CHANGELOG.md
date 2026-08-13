# Changelog

## Unreleased — Batch 13: Custom in-app video picker (replaces OS Photo Picker for single-video pick)

User feedback on the existing flow: the OS Photo Picker's look (grid tiles,
OEM-themed "Video"/"Koleksi" tabs) isn't something this app controls, and
they wanted a list-style picker instead — one row per video with thumbnail
+ filename + duration + resolution + date + size, plus a Videos/Folders
split, styled after a reference file-manager-style picker they use. Also
asked for a clear cancel affordance for a wrong tap.

- **`VideoPickerScreen.kt` (new)** — full-screen composable, queries
  `MediaStore.Video.Media` directly (already covered by the existing
  `READ_MEDIA_VIDEO`/`READ_EXTERNAL_STORAGE` manifest permission, so no new
  permission was added):
  - **Videos tab**: flat list, newest-first by default, each row showing a
    lazily-loaded thumbnail (`ContentResolver.loadThumbnail` on API 29+,
    the older `MediaStore.Video.Thumbnails` table below that — minSdk is
    still 24), filename, `mm:ss • WxH`, and `date • size`.
  - **Folders tab**: videos grouped by `BUCKET_ID`/`BUCKET_DISPLAY_NAME`,
    each row showing an item/duration/size summary and the most-recent
    video's thumbnail as the cover; tapping drills into that folder's own
    video list (same row layout as the Videos tab).
  - **Sort menu** (top bar): Terbaru / Terlama / Nama (A-Z) / Ukuran
    terbesar — applies to whichever video list is currently showing
    (root Videos tab or an open folder).
  - **Cancel**: both the back arrow (steps out of an open folder first,
    only exits the picker once already at the root) and an always-visible
    "Batal" text button in the top bar — the explicit ask from this batch,
    so a wrong tap into the picker always has one unambiguous way out
    regardless of whether a folder is open.
  - **Permission handling**: if READ_MEDIA_VIDEO/READ_EXTERNAL_STORAGE
    isn't granted, shows an explanatory panel with a Grant button instead
    of silently listing nothing.
- **`ResizerScreen`/`GifScreen` integration (`MainActivity.kt`)** — the
  `ActivityResultContracts.PickVisualMedia()` launcher used for the single
  main-video pick is gone from both screens; their `VideoPickerCard` tap,
  "Ganti video", and the change-video confirm dialog now all set a local
  `showVideoPicker` flag instead, which renders `VideoPickerScreen` as a
  full-screen overlay — same "Scaffold wrapped in a Box, overlay drawn on
  top, nothing underneath loses state" pattern `VideoResizerApp` already
  uses for Studio/Batch/GIF. The old picker-launcher callback body was
  extracted into a plain `handlePickedVideo(uri)` function in each screen
  so the metadata-load/state-reset logic didn't need duplicating.
- **Explicitly out of scope this batch**: `BatchScreen`'s multi-video pick
  (`PickMultipleVisualMedia`) and both watermark-image pickers
  (`PickVisualMedia.ImageOnly`, in `ResizerScreen`/`BatchScreen`) are
  unchanged — the reported UI/reference screenshots were both single-video
  pick flows, and folding multi-select into the same custom picker is a
  larger change (selection state, a confirm/done bar, etc.) better done as
  its own batch if wanted.
- **Reported but not fixed this batch**: "UI asimetris" on the trim editor
  (`VideoEditorPreview`) — reviewed the composable's layout code closely
  (player box → time labels → filmstrip/trim-handle box → details row, all
  plain `Arrangement.spacedBy(12.dp)`, no `weight`/`fillMaxHeight` that
  could explain a large gap) and couldn't identify a concrete structural
  cause with high confidence from code review alone. Flagged back to the
  user for a pointer to the specific element rather than shipping a
  guessed fix — see PROJECT_STATE.md's pending items.
- `semanticVersionName` bumped `1.16` → `1.17` (app/build.gradle.kts).

## Unreleased — Batch 13b: Trim-editor edge-clip fix ("UI asimetris" follow-up)

User's answer to "which part looks asymmetric" was "all the parts that
don't look symmetric" — not specific enough to point at one thing, so
rather than keep asking, this batch ships the one concrete, code-level
issue Batch 13's review actually found and is confident about, instead of
guessing at layout changes with no way to verify them visually here.

- **`VideoEditorPreview`'s filmstrip/trim-handle `Box` now clips to the
  same rounded shape as the filmstrip itself** — the two `TrimHandle`s (and
  the dim overlays / selection frame) are siblings of the filmstrip `Row`
  in that `Box`, not children of it, so the filmstrip's own
  `.clip(RoundedCornerShape(10.dp))` never applied to them. Each handle's
  invisible 48dp touch target is wider than its visible 16dp bar, and at
  the two extremes (trim start at 0f / trim end at 1f) that let the
  *visible* bar hang ~8dp past the filmstrip's left/right edge — a
  lopsided sliver sitting outside the rounded corners on whichever side(s)
  the trim wasn't already pulled all the way to. Clipping the whole `Box`
  (filmstrip + overlays + frame + both handles together) to one shared
  shape makes every layer stop flush at the same edge; nothing in the
  stack can overhang anymore. Touch/drag behavior is unchanged — `clip`
  only affects drawing, not the handle's hit-test region.
- **Still open**: if this wasn't the asymmetry the user meant, the next
  step is a marked-up screenshot (arrow/circle on the actual element) —
  see PROJECT_STATE.md's pending items. Not re-guessing further without
  one, to avoid spending another batch on a possible no-op.
- `semanticVersionName` bumped `1.17` → `1.18`.

## Unreleased — Batch 12: Close out Batch 9's two scope cuts (BatchScreen controls, GIF history)

Both of Batch 9's explicitly-deferred items, done together since they're
both direct continuations of that same feature — user asked to "tuntaskan
yang pending+urgent" after confirming the Batch 11 build was green.

- **BatchScreen: Flip/Frame Rate/Target-Size (MB)** — same three controls
  ResizerScreen already had, now available for the whole queue:
  - Flip and Frame Rate: identical `OptionSection` chips to ResizerScreen's,
    new `flip`/`frameRate` state, passed straight through into every
    item's `ResizeRequest`.
  - Target-Size (MB) is **not** a single shared bitrate the way
    ResizerScreen's version is — a batch queue can hold videos of very
    different lengths, and one bitrate computed from one assumed duration
    would badly miss the target for anything shorter/longer than that.
    Instead `targetSizeMb: Double?` just stores the target itself; a new
    `BatchTargetSizeDialog` (deliberately without ResizerScreen's live
    "≈ X kbps" preview, since there's no single duration to compute it
    against here) collects it, and `startBatch()`'s per-item loop — which
    already probes each item's own duration before building its
    `ResizeRequest` — solves MB→kbps freshly for *that* item via the same
    `VideoResizer.requiredBitrateKbpsForTargetSize` ResizerScreen's dialog
    uses, falling back to `MIN_BITRATE_KBPS` (best effort, not a failure)
    for the rare item whose duration makes the target impossible.
  - The Quality chip row and the Social Preset row both now clear
    `targetSizeMb` when picked, so the three quality-selection paths
    (preset chips / custom kbps / target size) stay mutually exclusive —
    same as ResizerScreen's already-existing preset/custom-kbps exclusivity.
- **GIF exports now save to Studio history** (`VideoHistoryStore.kt` — new
  `kind`/`gifFps`/`gifWidthPx` fields, `"VIDEO"`/0/0 defaults so entries
  saved before this batch stay readable; `MainActivity.kt` — GifScreen
  writes an entry on export success):
  - `thumbnailPath` deliberately points at the GIF file itself, not a
    separately-extracted static frame — `BitmapFactory.decodeFile` (what
    `StudioEntryCard` already uses) reads a GIF's first frame as a plain
    `Bitmap` with zero extra code, unlike the video path which needs
    `extractVideoThumbnail` because `MediaMetadataRetriever` can't decode
    a *video* thumbnail without one.
  - `StudioEntryCard` now branches its detail line on `entry.kind`: GIF
    entries show "GIF • Nfps • Wpx" instead of the video-specific
    aspect/resolution/rotation/quality/watermark/caption/flip/frame-rate
    lines, since none of those fields apply to a GifExporter output.
  - "Edit ulang" on a GIF entry reopens `GifScreen` (not `ResizerScreen`)
    via a new, deliberately separate `GifPrefill` data class — a GIF
    entry carries none of `PrefillSettings`' resize-specific fields, so
    routing it through the video prefill path would've reopened the wrong
    screen with mostly-default settings.
  - Share/"Buka di Galeri" for a GIF entry now use `shareGifFile`/
    `shareGifUri` (new)/`openGifInGallery` (`image/gif` mime) instead of
    the video-mime helpers, via a `entry.kind == "GIF"` branch at the
    `StudioScreen` call site.
  - `GifScreen`'s picker-launcher metadata probe and its new prefill
    effect shared near-identical `MediaMetadataRetriever` code; factored
    into one local `loadSourceMetadata` suspend fun (same "local suspend
    fun inside the composable" convention `ResizerScreen`'s own
    `loadVideoMetadata` already uses) rather than duplicating it a third
    time.
- **Not done this batch**: the pre-Batch-5 duplicated `v1.13` GitHub
  Release still needs a manual `gh release delete v1.13 -y` — not
  something a code batch can act on, noted again here since it's still
  the one open item that isn't code.

## Unreleased — Batch 11: Debug/polish pass across GIF, target-size, and Studio history

Not a new feature — a review pass over everything Batch 9/10 touched
(user asked to "fokus debugging/polish semua fitur" after the green
build), looking for real bugs and rough edges the sandbox's structural
checks alone couldn't have caught.

- **Fixed**: GIF playback delay could drift from the actually-extracted
  frame spacing (`GifExporter.export`) — `delayCentiseconds` was computed
  from the *requested* fps, but the real spacing between frames
  (`actualIntervalMs`) only equals `1000/fps` when `MAX_FRAMES` isn't hit;
  for a clip long/fast enough to hit that cap, the GIF would've played
  back faster than the source. Now derives the delay from
  `actualIntervalMs` directly, so it's correct in both cases. (Currently
  unreachable in practice — the GIF screen's own frame-count estimate
  disables the button before this cap is hit — but `GifExporter` is a
  general-purpose object, not something that should only be correct when
  called through that one screen.)
- **Fixed**: `GifExporter` skipped a `frame.width <= 0`/`frame.height <= 0`
  guard — a corrupt/unreadable frame from `getFrameAtTime` would have
  produced `Infinity`/`NaN` math feeding `Bitmap.createScaledBitmap` and
  likely crashed the export. Now such frames are recycled and skipped,
  same as an outright-null frame already was.
- **Perf**: `GifExporter.quantizeFrame` rebuilt the palette's R/G/B split
  arrays on every single frame call, even though the palette is identical
  for the whole clip — up to `MAX_FRAMES` (200) redundant allocations of
  the same data. Split once in `export()` now, passed down instead.
- **UX fix**: `requiredBitrateKbpsForTargetSize` clamps its result to
  `MIN_BITRATE_KBPS..MAX_BITRATE_KBPS` (same floor/ceiling
  `estimateOutputSizeBytes` uses) so a bad target can't produce an
  unusable file — but an extreme target (e.g. 1MB for a 10-minute clip)
  silently returned a bitrate bigger than what the size implied, with the
  dialog showing "≈ X kbps" as if it were exact. `TargetSizeDialog` now
  detects the clamp and says so explicitly instead of quietly showing a
  number that looks precise but won't hit the target.
- **Consistency fix**: Studio history cards showed rotation/quality/
  watermark/caption but never flip or frame rate, even though both have
  been saved per-entry since Batch 9 — same blind spot as caption briefly
  was after Batch 8. Added a "Flip Horizontal • 30 fps"-style line,
  same pattern as the existing "Watermark aktif"/"Caption aktif" lines,
  only shown when either is non-default.
- **Code-hygiene fix**: `GifScreen` (new in Batch 9) had introduced three
  `!!` force-unwraps (`selectedUri!!`, `resultFile!!`, `galleryUri!!`),
  inconsistent with this file's established "local val capture instead of
  `!!`" convention (see the comments at `currentUri` in `ResizerScreen`,
  predating this batch) and with Batch 2's explicit goal of having none
  left in `MainActivity.kt`/`VideoResizer.kt`. Rewritten to match.

## Unreleased — Batch 10: Fix CI build failure (GifExporter.kt) + failure-log artifact

- **Bug fix (real compile error, not a style issue)**: `GifExporter.kt:165`
  — `sums[0] += r` where `sums` is a `LongArray` and `r`/`g`/`b` are `Int`
  failed to compile: `e: No set method providing array access`. Root
  cause: Kotlin has no implicit `Int` → `Long` widening, so the indexed
  `+=` desugaring to `sums.set(0, sums.get(0) + r)` can't resolve. Fixed
  with explicit `.toLong()` on each RHS (`r.toLong()`, `1L`, etc.). Found
  from the user-supplied Batch 9 CI failure log
  (`:app:compileReleaseKotlin FAILED`) — first real compiler signal on
  Batch 9's GIF code, exactly the kind of thing the sandbox's
  structural-checks-only verification can't catch.
- **New: failure-log artifact** (`.github/workflows/build.yml`,
  `[PROTECTED]`) — "Build release APK" now pipes its output through `tee
  build-output.log` (GitHub Actions' default bash runs with `pipefail`, so
  a failing `gradle` still fails the step through the pipe). Two new
  `if: failure()` steps follow: one computes a
  `log_fail_<version>_<run_number>` name from `semanticVersionName` +
  `$GITHUB_RUN_NUMBER` (falls back to "unknown" if the grep comes up
  empty, e.g. checkout itself failed), the other uploads
  `build-output.log` plus any Gradle/lint report files under that name via
  `actions/upload-artifact@v4`. Only runs when an earlier step in the job
  failed — a normal successful build produces no extra artifact.

## Unreleased — Batch 9 (Atomic): Video-ke-GIF, Flip/Frame Rate, Compress by Target Size

Atomic change (not split across multiple batches) because all three features
share the same export/UI surface (`VideoResizer.kt`'s pipeline,
`ResizeRequest`, and the Resizer screen's option sections) and reviewing
them together was more coherent than reviewing three partial diffs to the
same functions.

- **New: Video ke GIF** (`GifEncoder.kt`, `GifExporter.kt`, both new files;
  new `GifScreen` in `MainActivity.kt`, reachable via a new nav icon next
  to Batch) — converts a trimmed clip into an animated GIF, entirely
  independent of the Media3 Transformer pipeline the rest of the app uses,
  since GIF isn't a video codec Transformer can target:
  - `GifEncoder.kt` — a from-spec, dependency-free GIF89a + LZW encoder
    (no third-party GIF library). Single shared global color table for the
    whole clip, NETSCAPE2.0 loop extension, standard 12-bit/4096-entry LZW
    dictionary with clear-code reset.
  - `GifExporter.kt` — extracts frames via
    `MediaMetadataRetriever.getFrameAtTime` (same technique
    `FilmstripExtractor` already used, just at higher frame count/res),
    builds one 256-color palette for the whole clip with a frequency/
    popularity bucket algorithm (deliberately simpler than median-cut/
    NeuQuant — easier to verify by hand without a local compiler; a good
    target to revisit if quality matters more than this first pass),
    nearest-color-quantizes every frame against it, hands the result to
    `GifEncoder`. Hard-capped at `MAX_FRAMES = 200` as a backstop; the UI
    estimates frame count live and disables the button before hitting it.
  - Fps presets 5/10/15, width presets 240/360/480px, publishes to
    `Pictures/VideoResizer/` via a new `PublicMovieExporter.publishImage()`
    (mirrors the existing `publish()` for video, just `image/gif` mime and
    the Images MediaStore collection instead of Video).
  - **Not done this batch**: GIF results aren't written to Studio history
    (`VideoHistoryStore`) — they're share/gallery-only for now, same as a
    fresh video export before it's saved. Flagged for a future batch if
    GIF re-editing turns out to matter.
- **New: Flip / mirror** (`VideoResizer.kt` — `FlipOption` enum;
  `MainActivity.kt` — chip row in `ResizerScreen`, right under Rotasi) —
  horizontal/vertical mirror, folded into the *same*
  `ScaleAndRotateTransformation` that already handled rotation (its builder
  accepts scale and rotation together) rather than adding a second Effect
  entry, so flip and rotation compose in a single GL pass exactly like
  before this feature existed for rotation alone.
- **New: Frame rate control** (`VideoResizer.kt` — `FrameRateOption` enum,
  backed by `androidx.media3.effect.FrameDropEffect
  .createDefaultFrameDropEffect(targetFps)`; `MainActivity.kt` — chip row
  right under Flip) — Original/24/30/60fps presets. `FrameDropEffect` only
  needs the *target* rate (it decides per-frame keep/drop from presentation
  timestamps), so no "read the source's frame rate first" step was needed.
  **Required bumping `media3-transformer`/`-effect`/`-common`/`-exoplayer`/
  `-ui` from 1.3.1 → 1.4.1** (`app/build.gradle.kts`, `[PROTECTED]`,
  edited per the edit-parsial-only rule) — `FrameDropEffect` doesn't exist
  at 1.3.1. Deliberately stopped at 1.4.1, not the latest: Media3 1.6.0
  flips the `OverlaySettings` anchor-point sign convention (see the
  `WatermarkPosition` doc comment in `VideoResizer.kt`, added back when
  that was first discovered), which would silently break watermark/caption
  placement if picked up here. 1.4.1 is the earliest stable release with
  `FrameDropEffect` and predates that change.
- **New: Compress by Target File Size (MB)** (`VideoResizer.kt` — new
  `requiredBitrateKbpsForTargetSize(targetSizeMb, durationMs, muteAudio)`
  companion function, the algebraic inverse of the existing
  `estimateOutputSizeBytes`; `MainActivity.kt` — new `TargetSizeDialog` +
  an extra chip in the Quality row) — deliberately **not** a new
  `ResizeRequest` field or export-pipeline code path: the dialog solves
  MB → kbps once and writes the result into the *existing*
  `quality = CUSTOM` / `customBitrateKbps` state, the same state
  `CustomBitrateDialog` already drives. The export pipeline needed zero
  changes for this feature as a result.
- **`VideoHistoryStore.kt`**: `flipName`/`frameRateName` fields added to
  `VideoHistoryEntry` (with `FlipOption.NONE`/`FrameRateOption.ORIGINAL`
  fallbacks via `optString`, so entries saved before this batch still load
  and default to the pre-existing behavior), wired through "Edit ulang"
  the same way rotation/quality already were.
- **Not done this batch** (scope cut, noted rather than silently skipped):
  Flip/Frame Rate/Target-Size controls were added to `ResizerScreen` only,
  not `BatchScreen` — batch-export jobs currently always use
  `FlipOption.NONE`/`FrameRateOption.ORIGINAL` (safe defaults, matches old
  behavior exactly). Extending `BatchScreen` similarly is straightforward
  follow-up, deferred to keep this batch's diff reviewable.
- **Known limitation**: GIF export's "Batalkan" (cancel) button is a soft
  cancel — it hides the progress UI immediately, but `GifExporter.export`
  runs as one synchronous call on `Dispatchers.Default` with no internal
  cancellation checks, so frame extraction/quantization/encoding already in
  flight keeps running to completion in the background (result just gets
  discarded) rather than stopping instantly. Not a correctness bug — no
  wrong file is ever shown or saved — but worth knowing before assuming
  "Batalkan" frees up CPU immediately.

## Unreleased — Batch 8: Finish everything left pending (caption overlay, batch thumbnails, dynamic versionName)

Closes out every item that had been left as a "not done yet" note across
Batches 1–7, in one pass, instead of leaving them scattered:

- **New: text caption overlay** (`VideoResizer.kt`) — a short line of text
  burned into the exported video as a static overlay (white fill + black
  outline for legibility over any footage), reusing the exact same
  `OverlayEffect`/`BitmapOverlay` pipeline the watermark feature already
  uses: `buildWatermarkOverlay` is now a thin wrapper around a new shared
  `buildImageOverlay(uri, position, scale, opacity)`, and captions render
  their text to a PNG in the app's cache dir via a plain `Canvas`/`Paint`
  (`renderCaptionBitmap`) and feed that file's `Uri` through the same
  helper — so this adds zero new Media3 API surface, only reusing an
  already-working path. `ResizeRequest` gained `captionText`/
  `captionPosition` (the latter reuses `WatermarkPosition` rather than a
  new enum — same five anchor points apply). UI: both `ResizerScreen` and
  `BatchScreen` gained a "Caption" text field + position chips, directly
  below their existing watermark section. Fixed style/scale only (no
  color/size picker) for this first pass — same scope level the watermark
  feature itself started at before scale/opacity sliders were added later.
- **New: per-item thumbnail preview in the Batch Export queue**
  (`MainActivity.kt`'s `BatchScreen`) — each queued video now shows a small
  40dp preview next to its filename instead of just text. Reuses
  `FilmstripExtractor.extract(..., count = 1)` (already used for the trim
  scrubber) to grab a single frame per video on `Dispatchers.IO`, cached in
  a `Map<Uri, Bitmap>` keyed by Uri so re-picking more videos on top of an
  existing queue doesn't re-decode already-thumbnailed ones.
- **Studio history now remembers captions too** (`VideoHistoryStore.kt` +
  `MainActivity.kt`'s `PrefillSettings`) — `captionText`/
  `captionPositionName` added to `VideoHistoryEntry` (with safe
  `optString` fallbacks so older saved entries without these fields still
  load), and wired through "Edit ulang" the same way watermark settings
  already were, so re-opening a past export for editing doesn't silently
  drop its caption.
- **`versionName` is now dynamic too** (`app/build.gradle.kts` +
  `.github/workflows/build.yml`) — Batch 7 only made `versionCode`
  dynamic and deliberately left `versionName` as a static human label;
  revisited per this batch's "finish everything" scope. `versionName` now
  appends `-build<n>` using the same `VERSION_CODE_OVERRIDE` env var
  `versionCode` already reads, so a device's Settings > App info shows
  exactly which CI run produced the installed APK. The base label moved
  into a `val semanticVersionName = "1.13"` literal (still bumped
  manually per feature batch) so there's still exactly one clear place to
  change it, and `.github/workflows/build.yml`'s "Locate APK" step now
  greps that instead of the old (now-gone) plain `versionName = "..."`
  literal.

**Not respun in this batch, and why:** the already-duplicated `v1.13`
GitHub Release from before Batch 5's fix — that's a one-time manual
cleanup on the person's end (`gh release delete v1.13 -y` in Termux), not
something any code change here can reach back and undo.

## Unreleased — Batch 7: Dynamic versionCode

- **`app/build.gradle.kts`** — `versionCode` was a flat hand-maintained
  integer (`13`) that sat unchanged across Batches 1–6 even though the app
  itself changed every batch, because nobody's job in this workflow was to
  remember to bump it. Now reads `VERSION_CODE_OVERRIDE` from the
  environment, offset by a fixed `1000` so it can never dip below the old
  value regardless of exact run count (Android refuses to install an APK
  as an "update" over a higher versionCode already on the device — going
  backward would force a manual uninstall). Falls back to `1013` for any
  build that isn't running in this CI (e.g. Android Studio).
- **`.github/workflows/build.yml`** — `Build release APK` step now sets
  `VERSION_CODE_OVERRIDE: ${{ github.run_number }}`, the same run number
  already used for the release tag/APK filename since Batch 5 — so a given
  CI run's versionCode, release tag, and APK filename all derive from one
  consistent number instead of three separately-tracked ones.
- **`versionName` stays manual on purpose** — it's the human-chosen
  semantic label ("1.13"), bumped deliberately per feature batch, not per
  build. Auto-incrementing it too would mean every CI run reports a
  different marketing version even for a pure CI-config batch like this
  one, which isn't what semantic versioning is for. `versionCode` is what
  actually needed to be dynamic (it's what Android and this workflow's own
  tag/filename logic depend on), so that's the one batch this touches.

## Unreleased — Batch 6: Add missing PROJECT_STATE.md / FILE_MANIFEST.txt

Housekeeping — no app code or CI touched. The context hierarchy in user
preferences (Chat Saat Ini > PROJECT_STATE.md > FILE_MANIFEST.txt >
CHANGELOG.md > README.md) has required these two files from the start;
Batches 1–5 only ever used CHANGELOG.md/README.md, so this fills the gap.

- **New: `FILE_MANIFEST.txt`** — every file in the project, one line each,
  with its purpose and whether it's `[PROTECTED]` (edit-parsial-only per
  user preferences).
- **New: `PROJECT_STATE.md`** — current-state snapshot: version, batch
  history summary, defaults a new reader needs to know (default theme,
  minifyEnabled=false, crash-log location, CI release-tag format), pending
  items, and this sandbox's own constraints (no local compiler).

**Going forward**: both files get updated at the end of every batch instead
of drifting stale — PROJECT_STATE.md's "Batch history" section and
FILE_MANIFEST.txt's file list in particular.

## Unreleased — Batch 5: Fix duplicate/colliding GitHub Releases

- **`.github/workflows/build.yml`** — release tag and APK filename were
  built from `versionName` alone (`v${VERSION_NAME}`). `versionName` is
  bumped per feature batch, not per push, so several pushes in a row with
  no `app/build.gradle.kts` version bump (exactly what Batches 3 and 4
  were) produced the **same tag**. `softprops/action-gh-release` treats a
  repeat tag as "edit this release", not "make a new one" — hence the
  duplicated "Full Changelog" section and same-named APK asset just getting
  silently replaced each run, seen in the repo's Releases page. Fixed by
  appending `${GITHUB_RUN_NUMBER}` (a GitHub-guaranteed strictly-increasing
  per-workflow counter) to both the tag and the APK filename — e.g.
  `v1.13-build17`, `VideoResizer-v1.13-build17-release.apk` — so every CI
  run now always produces its own release and its own uniquely-named APK,
  even when versionName hasn't changed.

**Not done / out of scope for this fix:** deleting the already-duplicated
`v1.13` release on GitHub itself — that's a one-time manual cleanup on the
repo's Releases page (or `gh release delete v1.13`), not something a code
change here can retroactively undo.

## Unreleased — Batch 4: CI build-speed tuning

Config-only batch — no app code touched, purely how fast `assembleRelease`
runs in GitHub Actions (this project has no local Termux `gradle build`
step in its own workflow — the Termux commands only `git commit`/`push` —
so "compile time" here means CI time, tuned for GitHub's `ubuntu-latest`
runners specifically).

- **`gradle.properties`** — daemon heap 2048m → 4096m (`ubuntu-latest` has
  ~7GB free, and the Compose compiler plugin is memory-hungry enough that
  the old 2048m ceiling was likely forcing extra GC pauses mid-compile),
  switched to `-XX:+UseParallelGC` (throughput-oriented, better fit for a
  short-lived one-shot compile process than the default G1), and turned on
  `org.gradle.caching` (Gradle's build cache — skips re-running tasks
  entirely when their inputs haven't changed, which matters a lot for
  *this* project's actual usage pattern: small daily-update batches where
  most files are unchanged between pushes) and `org.gradle.parallel`
  (limited win with only one Gradle module, `:app`, but free to leave on).
- **`app/build.gradle.kts`** — `lint { checkReleaseBuilds = false }`.
  `assembleRelease` otherwise drags AGP's lint-vital analysis pass in as a
  dependency, which re-parses/re-analyzes the whole module every single
  build; this CI job's only job is producing a signed, installable APK, not
  gating on lint findings, so it's skipped rather than paid for on every
  push.
- **`.github/workflows/build.yml`** — `gradle assembleRelease` now passes
  `--parallel --build-cache` explicitly, as a defense-in-depth belt-and-
  suspenders alongside the `gradle.properties` flags (in case any future
  edit to `gradle.properties` accidentally drops them, the CI command
  itself still asks for both).

**Deliberately not done, and why:**
- **No AGP/Kotlin version bump.** Newer Kotlin/AGP releases do compile
  faster, but changing them is a real compatibility risk (Compose compiler
  extension version is pinned to a specific Kotlin version) that can't be
  verified without an actual build — there's no `gradle`/`kotlinc` in this
  environment to compile-check it first. Worth doing as its own batch, with
  a real CI run to confirm it, not bundled sight-unseen into a "make it
  faster" pass.
- **No `org.gradle.configuration-cache=true`.** This is the other big lever
  for repeat CI runs, but Gradle's configuration-cache entries live in a
  *project-local* `.gradle/configuration-cache` folder, not the user-home
  cache `gradle/actions/setup-gradle` already persists across runs — so it
  would need its own `actions/cache` step to actually pay off, and some
  third-party Gradle plugins still don't fully support it, which can turn
  into an outright build failure rather than a slow one. Left off until it
  can be tried and watched on a real run rather than guessed at here.
- **`isMinifyEnabled` stays `false`.** Turning R8 shrinking *on* would make
  the build slower, not faster, so it's untouched — it was already off, and
  it stays off for this specific goal too.

## Unreleased — Batch 3 (Atomic): "Midnight Blue Glass" UI/UX overhaul

Single atomic change — a full visual-identity overhaul can't be meaningfully
split into smaller batches without leaving the UI in an inconsistent
half-restyled state, so this exceeds the usual per-batch file guidance on
purpose (justification per the Atomic Change rule).

Adds a fifth selectable theme, **Midnight Blue Glass** — an iOS-style
glassmorphism look with a midnight-blue gradient backdrop — and makes it the
app's **new default** (Dark/Light/Midnight Neon/Warm Paper are all still
selectable from the theme menu; nothing existing was removed).

- **`ui/theme/Color.kt`** — new palette: `GlassGradientTop/Mid/Bottom` (the
  backdrop), translucent `GlassSurface`/`GlassSurfaceVariant` (what makes
  cards read as frosted glass), `GlassBorder` (the frosted-edge hairline),
  `GlassPrimary`/`GlassSecondary` (iOS-blue/cyan accents), plus
  `MidnightBlueGlassGradient`, the actual `Brush` painted behind every
  screen (a flat `ColorScheme.background` can't hold a gradient, hence a
  separate Brush constant).
- **`ui/theme/Type.kt`** — `GlassTypography`: tighter (slightly negative)
  letter-spacing and bolder titles for the dense, tracked-in look of iOS
  system type. Platform default sans family — no bundled font files.
- **`ui/theme/Theme.kt`** — `GlassColors` ColorScheme, `GlassShapes` (large
  iOS-"squircle" corner radii: 10/14/20/26/34dp vs. Material3's
  4/8/12/16/28dp default), `AppThemeStyle.MIDNIGHT_BLUE_GLASS` added to the
  enum, wired into every `when` in `VideoResizerTheme`, and made the default
  when following system dark mode. New `LocalIsGlassTheme` CompositionLocal
  so each screen can tell whether to paint the gradient or a plain flat
  background, without threading an extra parameter through every screen's
  function signature.
- **`MainActivity.kt`** (UI-only edits, business logic untouched):
  - `ThemePreference` enum + theme dropdown menu gained a "Midnight Blue
    Glass" entry; default `themePref` state changed to it.
  - Each of the three screens (`ResizerScreen`, `StudioScreen`,
    `BatchScreen`) now paints its own opaque background — the Midnight Blue
    Glass gradient, or the previous flat color for every other theme — via
    its `Scaffold`'s own `modifier`, with `containerColor = Color.Transparent`
    so that background shows through. This is deliberately **per-screen**,
    not a single shared background painted once at the app root: the v1.8
    fix that keeps the main screen permanently composed underneath
    Studio/Batch relies on each overlay screen's background being fully
    opaque on its own, and a single root layer would have broken that
    occlusion.
  - The four `Card(...)` composables (result card, video-editor-preview
    card, video-picker card, batch queue-item card) gained a themed
    `border` (invisible-thin on Dark/Light/Neon/Paper, a visible frosted
    hairline on Glass) and switched from hardcoded per-call
    `RoundedCornerShape(..dp)` to `MaterialTheme.shapes.large`, so they
    finally pick up each theme's shape language the way every other
    component already did (a small pre-existing inconsistency, fixed as
    part of the same pass rather than left half-done).
  - The two hardcoded `AccentPrimary`/`AccentSecondary` gradients (app-mark
    icon in the top bar, the primary "Resize video" CTA button) now read
    `MaterialTheme.colorScheme.primary`/`.secondary` instead, so they
    actually pick up each theme's accent colors — previously they always
    rendered Dark theme's purple/teal regardless of which theme was active,
    which would have undercut Glass's whole point (its accent is iOS-blue/
    cyan, not purple/teal). Zero visual change for Dark itself, since
    `DarkColors.primary`/`.secondary` already equal those same constants.

No true backdrop blur (`RenderEffect`, Android 12+/API 31 only): the
translucent glass fills + gradient backdrop + frosted border already read
clearly as glassmorphism without needing API-level gating, and true blur
would need to sample the gradient itself, not surrounding app content, so
it would add little.

## Unreleased — Batch 2: Crash logger, GitHub Release publishing, force-unwrap cleanup

Atomic batch — three independent-but-related hardening items shipped together
since none of them touch overlapping code and all were already flagged as
pending in the previous batch's notes.

- **New: built-in crash logger** (`CrashLogger.kt` + `VideoResizerApp.kt`).
  Installs a global uncaught-exception handler at app startup that writes a
  plain-text report — version, OS, device model, timestamp, thread, full
  stack trace — to `Documents/VideoResizer/logs/crash_<yyyyMMdd_HHmmss>_<UUID>.txt`
  via `MediaStore` (API 29+), with no legacy storage permission needed (same
  own-file-insert exception `PublicMovieExporter.kt` already relies on).
  FIFO retention keeps at most 50 logs, oldest deleted first. Every write is
  independently try/caught so a logging failure can never mask or add to the
  real crash. Registered via `AndroidManifest.xml`'s new
  `android:name=".VideoResizerApp"`.
- **`.github/workflows/build.yml`** — release APK is now published as an
  actual **GitHub Release** (`softprops/action-gh-release@v2`, tagged
  `v<versionName>`), not just an Actions artifact. Previously the signed APK
  only showed up inside a specific workflow run's Artifacts tab; it now
  appears directly in the repo's **Releases** sidebar, ready to download and
  install from the repo homepage. Artifact upload step kept as-is alongside
  it (harmless, some workflows still expect it).
- **Force-unwrap (`!!`) cleanup** — the five remaining call sites flagged in
  the previous batch's notes (`MainActivity.kt`'s `selectedUri`,
  `selectedSocialPreset` ×2, `watermarkUri`, `resultThumbnailBitmap`, plus
  `VideoResizer.kt`'s `watermarkUri`) are gone. All were already
  null-guarded by their enclosing `if`, so this was a style/robustness fix,
  not a live bug — but Compose mutable state (`by mutableStateOf`) can't be
  smart-cast across statement/lambda boundaries the way a plain local `val`
  can, which is why the `!!` existed in the first place. Replaced with
  either `?.let { }` (single read → non-null lambda parameter) or an
  explicit local `val` capture before the null check, both of which are
  smart-cast-safe by construction. `VideoResizer.buildWatermarkOverlay` also
  had its signature changed to take the already-checked `watermarkUri: Uri`
  directly instead of re-deriving it from `request.watermarkUri` internally.

Riwayat versi v1.0–v1.13 (fitur-fitur awal sampai before/after preview) ada
di bagian **Changelog** README.md — file ini melanjutkan dari situ, jadi
sesi AI mana pun yang mengerjakan proyek ini berikutnya bisa lihat di sini
dulu untuk konteks perubahan terbaru sebelum menyentuh kode.

## Unreleased — Batch 1: Backend hardening

Tujuan batch ini: kokohkan jalur export inti sebelum menambah fitur baru
lagi, supaya biaya debugging ke depan lebih kecil. Tidak ada perubahan
behavior yang disengaja dari sisi user — semua penambahan di bawah ini
murni menangkap kegagalan yang sebelumnya bikin app crash, dan
mengarahkannya ke jalur "export gagal" yang sudah ada di UI.

- **`VideoResizer.kt`** — `resize()` sebelumnya menjalankan seluruh setup
  pipeline (hitung dimensi, bangun efek watermark, `Transformer.start()`)
  tanpa try/catch sama sekali. Exception sinkron di sana (izin URI
  watermark dicabut, resolusi tidak valid, encoder ditolak device sebelum
  pipeline async jalan) bikin seluruh app crash. Sekarang dibungkus:
  kegagalan setup diarahkan ke `ResizeResult.Failure` yang sama persis
  dipakai `Transformer.Listener.onError` — satu jalur penanganan gagal,
  bukan crash. Signature `resize()`/`runResize()` berubah dari
  `Transformer` jadi `Transformer?` (aman — pemanggilnya sudah pakai state
  nullable `activeTransformer`).
- **`ExportForegroundService.kt`** — `startForeground()` di `onCreate()`
  sekarang dibungkus try/catch. Di Android 12+ OS bisa menolak promosi ke
  foreground priority (`ForegroundServiceStartNotAllowedException`); dulu
  ini mematikan seluruh proses app. Sekarang service berhenti sendiri
  kalau ditolak, dan export tetap jalan — cuma kehilangan proteksi
  background-kill untuk kejadian itu saja.
- **`VideoHistoryStore.kt`** — diaudit ulang, ternyata sudah aman
  (`runCatching` per-entry saat parse JSON histori). Tidak ada perubahan.
- Dibersihkan: file sampah tak terkait proyek (`signing history
  (4001440) ==="` — potongan teks bantuan `less` yang salah kesasar jadi
  nama file dari sesi lampau sebelumnya) sudah dihapus dari repo.

### Regresi yang ditemukan & diperbaiki di batch yang sama

- Perubahan awal Batch 1 sempat menambahkan `release.keystore` ke
  `.gitignore` (alasannya waktu itu: mencegah keystore ter-commit
  mentah). Ini ternyata regresi — lihat catatan **"Keystore HARUS
  ter-commit ke git"** di README.md bagian *Notes*. Build APK di GitHub
  Actions gagal di step `validateSigningRelease` karena `build.yml` tidak
  merekonstruksi `release.keystore` dari secret, ia mengandalkan file itu
  ada langsung di hasil checkout. Sudah dikembalikan: `release.keystore`
  tetap ter-track di git seperti semula.
