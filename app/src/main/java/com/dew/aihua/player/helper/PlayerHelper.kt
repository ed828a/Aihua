package com.dew.aihua.player.helper

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.view.accessibility.CaptioningManager
import androidx.annotation.IntDef
import com.dew.aihua.R
import com.dew.aihua.player.helper.PlayerHelper.MinimizeMode.Companion.MINIMIZE_ON_EXIT_MODE_BACKGROUND
import com.dew.aihua.player.helper.PlayerHelper.MinimizeMode.Companion.MINIMIZE_ON_EXIT_MODE_NONE
import com.dew.aihua.player.helper.PlayerHelper.MinimizeMode.Companion.MINIMIZE_ON_EXIT_MODE_POPUP
import com.dew.aihua.player.model.PlayQueueItem
import com.dew.aihua.player.playqueque.queque.PlayQueue
import com.dew.aihua.player.playqueque.queque.SinglePlayQueue
import com.google.android.exoplayer2.SeekParameters
import com.google.android.exoplayer2.text.CaptionStyleCompat
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.TrackSelection
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.upstream.BandwidthMeter
import com.google.android.exoplayer2.util.MimeTypes
import org.schabi.newpipe.extractor.MediaFormat
import org.schabi.newpipe.extractor.stream.*
import java.text.DecimalFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 *  Created by Edward on 3/2/2019.
 */

object PlayerHelper {

    private val stringBuilder = StringBuilder()
    private val stringFormatter = Formatter(stringBuilder, Locale.getDefault())
    private val speedFormatter = DecimalFormat("0.##x")
    private val pitchFormatter = DecimalFormat("##%")

    @kotlin.annotation.Retention(AnnotationRetention.SOURCE)
    @IntDef(MINIMIZE_ON_EXIT_MODE_NONE, MINIMIZE_ON_EXIT_MODE_BACKGROUND, MINIMIZE_ON_EXIT_MODE_POPUP)
    annotation class MinimizeMode {
        companion object {
            const val MINIMIZE_ON_EXIT_MODE_NONE = 0
            const val MINIMIZE_ON_EXIT_MODE_BACKGROUND = 1
            const val MINIMIZE_ON_EXIT_MODE_POPUP = 2
        }
    }
    ////////////////////////////////////////////////////////////////////////////
    // Exposed helpers
    ////////////////////////////////////////////////////////////////////////////

    fun getTimeString(milliSeconds: Int): String {
        val seconds = milliSeconds % (60 * 1000L) / 1000L                    // rest of minute
        val minutes = milliSeconds % (60 * 60 * 1000L) / (60 * 1000L)        // rest of hour
        val hours = milliSeconds % 86400000L / 3600000L                      // rest of day
        val days = milliSeconds % (86400000L * 7L) / 86400000L               // rest of week

        stringBuilder.setLength(0)
        return when {
            days > 0 -> stringFormatter.format("%d:%02d:%02d:%02d", days, hours, minutes, seconds).toString()
            hours > 0 -> stringFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString()
            else -> stringFormatter.format("%02d:%02d", minutes, seconds).toString()
        }
    }

    fun formatSpeed(speed: Double): String = speedFormatter.format(speed)

    fun formatPitch(pitch: Double): String = pitchFormatter.format(pitch)

    fun subtitleMimeTypesOf(format: MediaFormat): String {
        when (format) {
            MediaFormat.VTT -> return MimeTypes.TEXT_VTT
            MediaFormat.TTML -> return MimeTypes.APPLICATION_TTML
            else -> throw IllegalArgumentException("Unrecognized mime type: ${format.getName()}")
        }
    }

    fun captionLanguageOf(
        context: Context,
        subtitles: SubtitlesStream
    ): String {
        val displayName = subtitles.locale.getDisplayName(subtitles.locale)

        return displayName + if (subtitles.isAutoGenerated) " (" + context.getString(R.string.caption_auto_generated) + ")" else ""
    }

    fun resizeTypeOf(
        context: Context,
        @AspectRatioFrameLayout.ResizeMode resizeMode: Int
    ): String {
        return when (resizeMode) {
            AspectRatioFrameLayout.RESIZE_MODE_FIT -> context.resources.getString(R.string.resize_fit)
            AspectRatioFrameLayout.RESIZE_MODE_FILL -> context.resources.getString(R.string.resize_fill)
            AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> context.resources.getString(R.string.resize_zoom)
            else -> throw IllegalArgumentException("Unrecognized resize mode: $resizeMode")
        }
    }

    fun cacheKeyOf(info: StreamInfo, video: VideoStream): String {
        return info.url + video.getResolution() + video.getFormat().getName()
    }

    fun cacheKeyOf(info: StreamInfo, audio: AudioStream): String {
        return info.url + audio.averageBitrate + audio.getFormat().getName()
    }

    /**
     * Given a [StreamInfo] and the existing queue items, provide the
     * [SinglePlayQueue] consisting of the next video for auto queuing.
     * <br></br><br></br>
     * This method detects and prevents cycle by naively checking if a
     * candidate next video's url already exists in the existing items.
     * <br></br><br></br>
     * To select the next video, [StreamInfo.getNextVideo] is first
     * checked. If it is nonnull and is not part of the existing items, then
     * it will be used as the next video. Otherwise, an random item with
     * non-repeating url will be selected getTabFrom the [StreamInfo.getRelatedStreams].
     */
    fun autoQueueOf(
        info: StreamInfo,
        existingItems: List<PlayQueueItem>
    ): PlayQueue? {
        val urls = HashSet<String>(existingItems.size)
        for (item in existingItems) {
            urls.add(item.url)
        }

        val nextVideo = info.nextVideo
        if (nextVideo != null && !urls.contains(nextVideo.url)) {
            return getAutoQueuedSinglePlayQueue(nextVideo)
        }

        val relatedItems = info.relatedStreams ?: return null
        val autoQueueItems = ArrayList<StreamInfoItem>()
        relatedItems.forEach { item ->
            if (item is StreamInfoItem && !urls.contains(item.getUrl())) {
                autoQueueItems.add(item)
            }
        }
        autoQueueItems.shuffle()
        return if (autoQueueItems.isEmpty()) null else getAutoQueuedSinglePlayQueue(autoQueueItems[0])
    }

    ////////////////////////////////////////////////////////////////////////////
    // Settings Resolution
    ////////////////////////////////////////////////////////////////////////////

    fun isResumeAfterAudioFocusGain(context: Context): Boolean {
        return isResumeAfterAudioFocusGain(context, false)
    }

    fun isVolumeGestureEnabled(context: Context): Boolean {
        return isVolumeGestureEnabled(context, true)
    }

    fun isBrightnessGestureEnabled(context: Context): Boolean {
        return isBrightnessGestureEnabled(context, true)
    }

    fun isUsingOldPlayer(context: Context): Boolean {
        return isUsingOldPlayer(context, false)
    }

    fun isRememberingPopupDimensions(context: Context): Boolean {
        return isRememberingPopupDimensions(context, true)
    }

    fun isAutoQueueEnabled(context: Context): Boolean {
        return isAutoQueueEnabled(context, true)
    }

    @MinimizeMode
    fun getMinimizeOnExitAction(context: Context): Int {
        val defaultAction = context.getString(R.string.minimize_on_exit_none_key)
        val popupAction = context.getString(R.string.minimize_on_exit_popup_key)
        val backgroundAction = context.getString(R.string.minimize_on_exit_background_key)

        val action = getMinimizeOnExitAction(context, defaultAction)
        return when (action) {
            popupAction -> MINIMIZE_ON_EXIT_MODE_POPUP
            backgroundAction -> MINIMIZE_ON_EXIT_MODE_BACKGROUND
            else -> MINIMIZE_ON_EXIT_MODE_NONE
        }
    }

    fun getSeekParameters(context: Context): SeekParameters =
        if (isUsingInexactSeek(context))
            SeekParameters.CLOSEST_SYNC
        else
            SeekParameters.EXACT


    fun getPreferredCacheSize(): Long {
        return 64 * 1024 * 1024L    // 64MB
    }

    fun getPreferredFileSize(): Long {
        return 512 * 1024L        // 0.5MB
    }

    /**
     * Returns the number of milliseconds the simpleExoPlayer buffers for before starting playback.
     */
    fun getPlaybackStartBufferMs(): Int {
        return 500
    }

    /**
     * Returns the minimum number of milliseconds the simpleExoPlayer always buffers to after starting
     * playback.
     */
    fun getPlaybackMinimumBufferMs(): Int {
        return 25000
    }

    /**
     * Returns the maximum/optimal number of milliseconds the simpleExoPlayer will buffer to once the buffer
     * hits the point of [.getPlaybackMinimumBufferMs].
     */
    fun getPlaybackOptimalBufferMs(): Int {
        return 60000
    }

    @Suppress("DEPRECATION")
    fun getQualitySelector(meter: BandwidthMeter): TrackSelection.Factory =
        AdaptiveTrackSelection.Factory(
            meter,
            /*bufferDurationRequiredForQualityIncrease=*/1000,
            AdaptiveTrackSelection.DEFAULT_MAX_DURATION_FOR_QUALITY_DECREASE_MS,
            AdaptiveTrackSelection.DEFAULT_MIN_DURATION_TO_RETAIN_AFTER_DISCARD_MS,
            AdaptiveTrackSelection.DEFAULT_BANDWIDTH_FRACTION
        )


    fun isUsingDSP(): Boolean {
        return true
    }

    fun getTossFlingVelocity(): Int {
        return 2500
    }

    fun getCaptionStyle(context: Context): CaptionStyleCompat {
        val captioningManager = context.getSystemService(Context.CAPTIONING_SERVICE) as CaptioningManager
        return if (captioningManager.isEnabled) CaptionStyleCompat.createFromCaptionStyle(captioningManager.userStyle) else CaptionStyleCompat.DEFAULT
    }

    /**
     * System font scaling:
     * Very small - 0.25f, Small - 0.5f, Normal - 1.0f, Large - 1.5f, Very Large - 2.0f
     */
    fun getCaptionScale(context: Context): Float {
        val captioningManager = context.getSystemService(Context.CAPTIONING_SERVICE) as CaptioningManager
        return if (captioningManager.isEnabled) captioningManager.fontScale else 1f
    }

    fun getScreenBrightness(context: Context): Float {
        //a value of less than 0, the default, means to use the preferred screen brightness
        return getScreenBrightness(context, -1f)
    }

    fun setScreenBrightness(context: Context, setScreenBrightness: Float) {
        setScreenBrightness(context, setScreenBrightness, System.currentTimeMillis())
    }

    ////////////////////////////////////////////////////////////////////////////
    // Private helpers
    ////////////////////////////////////////////////////////////////////////////

    private fun getPreferences(context: Context): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(context)
    }

    private fun isResumeAfterAudioFocusGain(context: Context, b: Boolean): Boolean {
        return getPreferences(context).getBoolean(context.getString(R.string.resume_on_audio_focus_gain_key), b)
    }

    private fun isVolumeGestureEnabled(context: Context, b: Boolean): Boolean {
        return getPreferences(context).getBoolean(context.getString(R.string.volume_gesture_control_key), b)
    }

    private fun isBrightnessGestureEnabled(context: Context, b: Boolean): Boolean {
        return getPreferences(context).getBoolean(context.getString(R.string.brightness_gesture_control_key), b)
    }

    private fun isUsingOldPlayer(context: Context, b: Boolean): Boolean {
        return getPreferences(context).getBoolean(context.getString(R.string.use_old_player_key), b)
    }

    private fun isRememberingPopupDimensions(context: Context, b: Boolean): Boolean {
        return getPreferences(context).getBoolean(context.getString(R.string.popup_remember_size_pos_key), b)
    }

    private fun isUsingInexactSeek(context: Context): Boolean {
        return getPreferences(context).getBoolean(context.getString(R.string.use_inexact_seek_key), false)
    }

    private fun isAutoQueueEnabled(context: Context, b: Boolean): Boolean {
        return getPreferences(context).getBoolean(context.getString(R.string.auto_queue_key), b)
    }

    private fun setScreenBrightness(context: Context, screenBrightness: Float, timestamp: Long) {
        val editor = getPreferences(context).edit()
        editor.putFloat(context.getString(R.string.screen_brightness_key), screenBrightness)
        editor.putLong(context.getString(R.string.screen_brightness_timestamp_key), timestamp)
        editor.apply()
    }

    private fun getScreenBrightness(context: Context, screenBrightness: Float): Float {
        val sp = getPreferences(context)
        val timestamp = sp.getLong(context.getString(R.string.screen_brightness_timestamp_key), 0)
        // hypothesis: 4h covers a viewing block, eg evening. External lightning conditions will change in the next
        // viewing block so we fall back to the default brightness
        return if (System.currentTimeMillis() - timestamp > TimeUnit.HOURS.toMillis(4)) {
            screenBrightness
        } else {
            sp.getFloat(context.getString(R.string.screen_brightness_key), screenBrightness)
        }
    }

    private fun getMinimizeOnExitAction(
        context: Context,
        key: String
    ): String? {

        return getPreferences(context).getString(context.getString(R.string.minimize_on_exit_key), key)
    }

    private fun getAutoQueuedSinglePlayQueue(streamInfoItem: StreamInfoItem): SinglePlayQueue {
        val singlePlayQueue = SinglePlayQueue(streamInfoItem)
        singlePlayQueue.item!!.isAutoQueued = true
        return singlePlayQueue
    }

}
