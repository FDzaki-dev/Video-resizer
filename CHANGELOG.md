# Changelog

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
