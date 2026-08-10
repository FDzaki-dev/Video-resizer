package com.example.videoresizer

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.OverlaySettings
import androidx.media3.effect.Presentation
import androidx.media3.effect.ScaleAndRotateTransformation
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import com.google.common.collect.ImmutableList
import java.io.File

/**
 * Target aspect ratio for the exported clip.
 *
 * PERF: Kotlin/Java's generated `enum.values()` allocates a brand-new array
 * on every single call — it is not cached by the compiler. `ENTRIES` below
 * computes that array exactly once (companion `object` init happens on
 * class-load) and hands out the same immutable `List` forever after.
 * `AspectRatioOption.values()` was previously called straight from
 * Composable bodies in MainActivity.kt — including ones re-evaluated on
 * every trim-handle drag frame and every Studio list-item render — so each
 * of those recompositions was paying for a fresh array + boxing into a List
 * for no reason. Use `AspectRatioOption.ENTRIES` everywhere instead of
 * `AspectRatioOption.values()`.
 */
enum class AspectRatioOption(val label: String, val widthRatio: Int, val heightRatio: Int) {
    ORIGINAL("Original", 0, 0),
    LANDSCAPE_16_9("16:9", 16, 9),
    PORTRAIT_9_16("9:16", 9, 16),
    SQUARE_1_1("1:1", 1, 1),
    STANDARD_4_3("4:3", 4, 3),
    PORTRAIT_4_5("4:5", 4, 5);

    companion object {
        val ENTRIES: List<AspectRatioOption> = values().toList()
    }
}

/** Target output resolution (height in pixels, width derived from aspect ratio). CUSTOM uses explicit width/height instead. */
enum class ResolutionOption(val label: String, val targetHeight: Int) {
    ORIGINAL("Original", 0),
    P480("480p", 480),
    P720("720p", 720),
    P1080("1080p", 1080),
    CUSTOM("Custom", -1);

    companion object {
        val ENTRIES: List<ResolutionOption> = values().toList()
    }
}

/** How the source frame is mapped into the target width/height when the aspect ratios don't match. */
enum class ResizeMode(val label: String) {
    CROP("Crop"),
    STRETCH("Stretch");

    companion object {
        val ENTRIES: List<ResizeMode> = values().toList()
    }
}

/** Rotation to apply to the output, in degrees clockwise. */
enum class RotationOption(val label: String, val degrees: Float) {
    NONE("0°", 0f),
    CW_90("90°", 90f),
    CW_180("180°", 180f),
    CW_270("270°", 270f);

    companion object {
        val ENTRIES: List<RotationOption> = values().toList()
    }
}

/**
 * Target video bitrate preset. ORIGINAL leaves the encoder's default
 * bitrate selection untouched (same behavior as before this feature
 * existed). The others request an explicit bitrate via
 * [androidx.media3.transformer.VideoEncoderSettings] so a novice user gets
 * a predictable, much smaller file at the cost of some visual quality —
 * the same "Low/Medium/High" tradeoff CapCut/InShot-style apps expose,
 * without needing to understand what a bitrate actually is.
 *
 * Values are a bits-per-pixel-per-second multiplier applied against the
 * actual output pixel count in [VideoResizer], rather than one fixed
 * number that would over-compress large frames or waste space on small
 * ones.
 */
enum class QualityOption(val label: String, val bitsPerPixelPerSecond: Double) {
    ORIGINAL("Original", 0.0),
    LOW("Rendah", 0.045),
    MEDIUM("Sedang", 0.09),
    HIGH("Tinggi", 0.18),
    CUSTOM("Custom", -1.0);

    companion object {
        val ENTRIES: List<QualityOption> = values().toList()
    }
}

/**
 * Corner (or center) anchor for a watermark/logo overlay, expressed as
 * NDC-space anchors for [androidx.media3.effect.OverlaySettings]. Pinned
 * against media3-effect 1.3.1's anchor convention specifically — 1.6.0
 * flipped the sign convention of `setOverlayFrameAnchor`, so if this
 * project's Media3 version is ever bumped past that, the anchor pairs
 * below need re-checking against the release notes for that version.
 */
enum class WatermarkPosition(val label: String) {
    TOP_LEFT("Kiri atas"),
    TOP_RIGHT("Kanan atas"),
    BOTTOM_LEFT("Kiri bawah"),
    BOTTOM_RIGHT("Kanan bawah"),
    CENTER("Tengah");

    companion object {
        val ENTRIES: List<WatermarkPosition> = values().toList()
    }
}

data class ResizeRequest(
    val sourceUri: Uri,
    val outputFile: File,
    val aspectRatio: AspectRatioOption,
    val resolution: ResolutionOption,
    val sourceWidth: Int,
    val sourceHeight: Int,
    /** Trim range, in ms, relative to the source video. Both 0 means "no trim". */
    val trimStartMs: Long = 0L,
    val trimEndMs: Long = 0L,
    val muteAudio: Boolean = false,
    val rotation: RotationOption = RotationOption.NONE,
    val resizeMode: ResizeMode = ResizeMode.CROP,
    /** Used only when resolution == CUSTOM: exact output pixel dimensions. */
    val customWidth: Int? = null,
    val customHeight: Int? = null,
    /** Bitrate/quality preset. ORIGINAL = let the encoder pick (old behavior). */
    val quality: QualityOption = QualityOption.ORIGINAL,
    /** Used only when quality == CUSTOM: exact target video bitrate in kbps. */
    val customBitrateKbps: Int? = null,
    /** Logo/watermark image to overlay on the output, or null for none. */
    val watermarkUri: Uri? = null,
    val watermarkPosition: WatermarkPosition = WatermarkPosition.BOTTOM_RIGHT,
    /** 0-100. How opaque the watermark is (100 = fully solid). */
    val watermarkOpacityPercent: Int = 70,
    /** 0-100. Watermark width as a rough percentage of the frame width. */
    val watermarkScalePercent: Int = 18
)

sealed class ResizeResult {
    data class Success(val outputFile: File) : ResizeResult()
    data class Failure(val message: String) : ResizeResult()
}

/**
 * Wraps androidx.media3 Transformer to re-encode a video into a new
 * aspect ratio/resolution, optionally trimmed, muted, and/or rotated.
 */
@UnstableApi
class VideoResizer(private val context: Context) {

    /**
     * Starts the export and returns the underlying [Transformer] so the
     * caller can cancel a running export (`transformer.cancel()`).
     *
     * UX FIX: previously `onProgress` was wired up but never actually
     * called — [androidx.media3.transformer.Transformer] doesn't push
     * progress on its own, you have to poll `getProgress()`. Without that,
     * "Processing…" was an indefinite spinner with zero feedback: a novice
     * user has no way to tell a slow export from a frozen app. This adds a
     * Main-thread poll loop (Transformer must be polled from its own
     * thread) that reports percentage until the export finishes or errors,
     * and stops cleanly either way so it doesn't keep polling forever.
     *
     * Progress is capped at 99 on purpose: media3 1.3.1's progress
     * reporting is known to be a little inaccurate near the end (it can
     * jump straight from ~70% to done), so treating "100%" as meaning
     * "finished" would be a lie the UI can't back up. The actual finish
     * signal is always `onCompleted`/`onError`, never the percentage.
     */
    fun resize(request: ResizeRequest, onProgress: (Int) -> Unit, onDone: (ResizeResult) -> Unit): Transformer? {
        // HARDENING: everything below — building effects, resolving the
        // watermark bitmap, and Transformer.start() itself — used to run
        // completely unguarded. Any synchronous exception here (revoked URI
        // permission on the watermark image, an invalid/zero target size,
        // a device rejecting the encoder config before the async pipeline
        // even begins) crashed the whole app instead of surfacing as a
        // normal failed export. Transformer.Listener.onError below only
        // catches errors *after* the pipeline is running, so it can't help
        // with this. Wrapping the setup and routing failures through the
        // exact same ResizeResult.Failure path onError already uses keeps
        // failure handling in exactly one place, with identical UX: the
        // caller just sees "export failed" instead of the app dying.
        return try {
            resizeInternal(request, onProgress, onDone)
        } catch (e: Exception) {
            onDone(ResizeResult.Failure(e.message ?: "Gagal memulai export"))
            null
        }
    }

    private fun resizeInternal(request: ResizeRequest, onProgress: (Int) -> Unit, onDone: (ResizeResult) -> Unit): Transformer {
        val (targetWidth, targetHeight) = computeTargetDimensions(request)

        val videoEffects = mutableListOf<androidx.media3.common.Effect>()

        val presentation = if (targetWidth > 0 && targetHeight > 0) {
            val layout = when (request.resizeMode) {
                ResizeMode.CROP -> Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP
                ResizeMode.STRETCH -> Presentation.LAYOUT_STRETCH_TO_FIT
            }
            Presentation.createForWidthAndHeight(targetWidth, targetHeight, layout)
        } else {
            null
        }
        presentation?.let { videoEffects.add(it) }

        if (request.rotation != RotationOption.NONE) {
            videoEffects.add(
                ScaleAndRotateTransformation.Builder()
                    .setRotationDegrees(request.rotation.degrees)
                    .build()
            )
        }

        // Watermark/logo overlay — same OverlayEffect + BitmapOverlay pipeline
        // Google's own Transformer demo app uses for picture-in-picture/logo
        // overlays. Applied AFTER presentation/rotation so the watermark sits
        // on top of the already-cropped/rotated frame, not the raw source.
        val watermarkUri = request.watermarkUri
        if (watermarkUri != null) {
            videoEffects.add(buildWatermarkOverlay(request, watermarkUri))
        }

        var mediaItemBuilder = MediaItem.Builder().setUri(request.sourceUri)
        if (request.trimEndMs > request.trimStartMs) {
            mediaItemBuilder = mediaItemBuilder.setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(request.trimStartMs)
                    .setEndPositionMs(request.trimEndMs)
                    .build()
            )
        }

        val editedMediaItemBuilder = EditedMediaItem.Builder(mediaItemBuilder.build())
            .setRemoveAudio(request.muteAudio)
        if (videoEffects.isNotEmpty()) {
            editedMediaItemBuilder.setEffects(Effects(emptyList(), videoEffects))
        }
        val editedMediaItem = editedMediaItemBuilder.build()

        val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())

        lateinit var transformer: Transformer
        val progressHolder = androidx.media3.transformer.ProgressHolder()
        val pollProgress = object : Runnable {
            override fun run() {
                val state = transformer.getProgress(progressHolder)
                if (state == Transformer.PROGRESS_STATE_AVAILABLE) {
                    onProgress(progressHolder.progress.coerceIn(0, 99))
                }
                if (state != Transformer.PROGRESS_STATE_NOT_STARTED) {
                    mainHandler.postDelayed(this, 400)
                }
            }
        }

        // Bitrate/quality: ORIGINAL leaves Transformer's default encoder
        // selection untouched (exact same behavior as before this feature
        // existed). Otherwise we hand it an explicit target bitrate via
        // DefaultEncoderFactory/VideoEncoderSettings. setEnableFallback(true)
        // is important here: not every device's hardware encoder accepts an
        // arbitrary bitrate, and without fallback enabled a device that
        // rejects the exact requested value fails the whole export instead
        // of quietly picking the closest value it can actually do.
        val targetBitrateBps = requestedBitrateBps(request, targetWidth, targetHeight)
        val transformerBuilder = Transformer.Builder(context)
        if (targetBitrateBps != null) {
            transformerBuilder.setEncoderFactory(
                androidx.media3.transformer.DefaultEncoderFactory.Builder(context)
                    .setRequestedVideoEncoderSettings(
                        androidx.media3.transformer.VideoEncoderSettings.Builder()
                            .setBitrate(targetBitrateBps)
                            .build()
                    )
                    .setEnableFallback(true)
                    .build()
            )
        }

        transformer = transformerBuilder
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: androidx.media3.transformer.Composition, exportResult: ExportResult) {
                    mainHandler.removeCallbacksAndMessages(null)
                    onDone(ResizeResult.Success(request.outputFile))
                }

                override fun onError(
                    composition: androidx.media3.transformer.Composition,
                    exportResult: ExportResult,
                    exportException: ExportException
                ) {
                    mainHandler.removeCallbacksAndMessages(null)
                    onDone(ResizeResult.Failure(exportException.message ?: "Export failed"))
                }
            })
            .build()

        transformer.start(editedMediaItem, request.outputFile.absolutePath)
        mainHandler.post(pollProgress)
        return transformer
    }

    /**
     * Builds the [OverlayEffect] that draws [ResizeRequest.watermarkUri] on
     * top of every output frame in a fixed corner, at a fixed opacity/scale,
     * for the whole clip duration (a "static" overlay — position/opacity
     * don't animate, matching what a novice user expects from a simple
     * watermark toggle).
     *
     * NDC anchor pairs below follow the pattern from Google's own Media3
     * Transformer sample code: `setOverlayFrameAnchor` picks a point *within
     * the watermark image itself*, `setBackgroundFrameAnchor` picks the
     * matching point on the video frame that the watermark's anchor point
     * gets pinned to. A small inset (0.86 instead of 1.0) keeps the
     * watermark from being flush against the very edge of the frame.
     */
    private fun buildWatermarkOverlay(request: ResizeRequest, watermarkUri: Uri): OverlayEffect {
        val inset = 0.86f
        val (overlayAnchorX, overlayAnchorY, bgAnchorX, bgAnchorY) = when (request.watermarkPosition) {
            WatermarkPosition.TOP_LEFT -> Quad(-1f, 1f, -inset, inset)
            WatermarkPosition.TOP_RIGHT -> Quad(1f, 1f, inset, inset)
            WatermarkPosition.BOTTOM_LEFT -> Quad(-1f, -1f, -inset, -inset)
            WatermarkPosition.BOTTOM_RIGHT -> Quad(1f, -1f, inset, -inset)
            WatermarkPosition.CENTER -> Quad(0f, 0f, 0f, 0f)
        }
        val scale = (request.watermarkScalePercent.coerceIn(5, 60)) / 100f
        val alpha = (request.watermarkOpacityPercent.coerceIn(5, 100)) / 100f

        val overlaySettings = OverlaySettings.Builder()
            .setOverlayFrameAnchor(overlayAnchorX, overlayAnchorY)
            .setBackgroundFrameAnchor(bgAnchorX, bgAnchorY)
            .setScale(scale, scale)
            .setAlphaScale(alpha)
            .build()

        val overlay = BitmapOverlay.createStaticBitmapOverlay(context, watermarkUri, overlaySettings)
        return OverlayEffect(ImmutableList.of(overlay))
    }

    /** Small tuple purely to keep [buildWatermarkOverlay]'s anchor math on one readable line each. */
    private data class Quad(val a: Float, val b: Float, val c: Float, val d: Float)

    /**
     * Resolves the requested video-encoder bitrate in bits per second for
     * this request, or null to leave the encoder's own default alone
     * (ORIGINAL preset — identical to pre-quality-feature behavior).
     */
    private fun requestedBitrateBps(request: ResizeRequest, targetWidth: Int, targetHeight: Int): Int? {
        if (request.quality == QualityOption.ORIGINAL) return null
        if (request.quality == QualityOption.CUSTOM) {
            val kbps = request.customBitrateKbps ?: return null
            return (kbps.coerceIn(MIN_BITRATE_KBPS, MAX_BITRATE_KBPS)) * 1000
        }
        val w = if (targetWidth > 0) targetWidth else request.sourceWidth
        val h = if (targetHeight > 0) targetHeight else request.sourceHeight
        if (w <= 0 || h <= 0) return null
        val bps = (w.toLong() * h.toLong() * request.quality.bitsPerPixelPerSecond).toInt()
        return bps.coerceIn(MIN_BITRATE_KBPS * 1000, MAX_BITRATE_KBPS * 1000)
    }

    companion object {
        /** Hard floor/ceiling so a bad custom value can't produce an unusable file or an encoder crash. */
        const val MIN_BITRATE_KBPS = 250
        const val MAX_BITRATE_KBPS = 50_000

        /** Resolves the final output width/height (0,0 means "keep original"). */
        fun computeTargetDimensions(request: ResizeRequest): Pair<Int, Int> {
            if (request.resolution == ResolutionOption.CUSTOM) {
                val w = (request.customWidth ?: request.sourceWidth).let { if (it % 2 != 0) it + 1 else it }
                val h = (request.customHeight ?: request.sourceHeight).let { if (it % 2 != 0) it + 1 else it }
                return w.coerceAtLeast(2) to h.coerceAtLeast(2)
            }

            if (request.aspectRatio == AspectRatioOption.ORIGINAL && request.resolution == ResolutionOption.ORIGINAL) {
                return 0 to 0
            }

            val ratioW: Int
            val ratioH: Int
            if (request.aspectRatio == AspectRatioOption.ORIGINAL) {
                ratioW = request.sourceWidth
                ratioH = request.sourceHeight
            } else {
                ratioW = request.aspectRatio.widthRatio
                ratioH = request.aspectRatio.heightRatio
            }

            val height = if (request.resolution == ResolutionOption.ORIGINAL) {
                request.sourceHeight
            } else {
                request.resolution.targetHeight
            }

            val width = (height * ratioW.toDouble() / ratioH.toDouble()).toInt().let {
                // Encoders generally require even dimensions.
                if (it % 2 != 0) it + 1 else it
            }
            val evenHeight = if (height % 2 != 0) height + 1 else height

            return width to evenHeight
        }

        /**
         * Rough estimated output file size in bytes, for the "Perkiraan
         * ukuran" label in the UI. This is intentionally approximate — real
         * encoders vary output somewhat around a requested bitrate — but
         * gives a novice user a ballpark figure *before* they commit to a
         * multi-minute export, which is the whole point of exposing a
         * quality control at all.
         *
         * durationMs should already reflect the trim range, not the full
         * source clip.
         */
        fun estimateOutputSizeBytes(
            request: ResizeRequest,
            durationMs: Long
        ): Long? {
            if (durationMs <= 0) return null
            val (targetWidth, targetHeight) = computeTargetDimensions(request)
            val videoBitrateBps = when {
                request.quality == QualityOption.ORIGINAL -> return null // unknown — encoder default
                request.quality == QualityOption.CUSTOM ->
                    (request.customBitrateKbps ?: return null).coerceIn(MIN_BITRATE_KBPS, MAX_BITRATE_KBPS) * 1000
                else -> {
                    val w = if (targetWidth > 0) targetWidth else request.sourceWidth
                    val h = if (targetHeight > 0) targetHeight else request.sourceHeight
                    if (w <= 0 || h <= 0) return null
                    (w.toLong() * h.toLong() * request.quality.bitsPerPixelPerSecond).toInt()
                        .coerceIn(MIN_BITRATE_KBPS * 1000, MAX_BITRATE_KBPS * 1000)
                }
            }
            // Rough constant audio bitrate assumption (AAC), 0 if muted.
            val audioBitrateBps = if (request.muteAudio) 0 else 128_000
            val totalBitsPerSecond = (videoBitrateBps + audioBitrateBps).toLong()
            val seconds = durationMs / 1000.0
            return (totalBitsPerSecond * seconds / 8.0).toLong()
        }
    }
}
