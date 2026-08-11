# Changelog

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
