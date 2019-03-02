package com.dew.aihua.player.helper

import android.content.Context
import android.content.Intent
import android.os.Build
import com.dew.aihua.player.playerUI.BackgroundPlayerActivity
import com.dew.aihua.player.playerUI.BasePlayer
import com.dew.aihua.player.playerUI.BasePlayer.Companion.PLAYBACK_QUALITY
import com.dew.aihua.player.playerUI.BasePlayer.Companion.PLAY_QUEUE_KEY
import com.dew.aihua.player.playerUI.PopupVideoPlayerActivity
import com.dew.aihua.player.playqueque.queque.PlayQueue

/**
 *  Created by Edward on 3/2/2019.
 */
object PlayerNavigationHelper {

    private fun getServicePlayerActivityIntent(context: Context,
                                               activityClass: Class<*>): Intent {
        val intent = Intent(context, activityClass)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return intent
    }

    fun getBackgroundPlayerActivityIntent(context: Context): Intent =
        getServicePlayerActivityIntent(context, BackgroundPlayerActivity::class.java)

    fun getPopupPlayerActivityIntent(context: Context): Intent =
        getServicePlayerActivityIntent(context, PopupVideoPlayerActivity::class.java)

    private fun getPlayerIntent(context: Context?,
                                targetClazz: Class<*>,
                                playQueue: PlayQueue,
                                quality: String? = null
    ): Intent {

        val cacheKey = SerializedCache.put(playQueue, PlayQueue::class.java)
        val intent = Intent(context, targetClazz)
        if (cacheKey != null) intent.putExtra(PLAY_QUEUE_KEY, cacheKey)
        if (quality != null) intent.putExtra(PLAYBACK_QUALITY, quality)

        return intent
    }

    fun getPlayerIntent(context: Context,
                        targetClazz: Class<*>,
                        playQueue: PlayQueue,
                        repeatMode: Int,
                        playbackSpeed: Float,
                        playbackPitch: Float,
                        playbackSkipSilence: Boolean,
                        playbackQuality: String?): Intent =
        getPlayerIntent(context, targetClazz, playQueue, playbackQuality)
            .putExtra(BasePlayer.REPEAT_MODE, repeatMode)
            .putExtra(BasePlayer.PLAYBACK_SPEED, playbackSpeed)
            .putExtra(BasePlayer.PLAYBACK_PITCH, playbackPitch)
            .putExtra(BasePlayer.PLAYBACK_SKIP_SILENCE, playbackSkipSilence)
}