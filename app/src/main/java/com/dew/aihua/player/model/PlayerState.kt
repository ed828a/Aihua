package com.dew.aihua.player.model

import com.dew.aihua.player.playqueque.queque.PlayQueue
import java.io.Serializable

/**
 *  Created by Edward on 2/23/2019.
 */

class PlayerState(
    val playQueue: PlayQueue,
    val repeatMode: Int,
    val playbackSpeed: Float,
    val playbackPitch: Float,
    val playbackQuality: String?,
    val isPlaybackSkipSilence: Boolean,
    val wasPlaying: Boolean) : Serializable {

    internal constructor(playQueue: PlayQueue,
                         repeatMode: Int,
                         playbackSpeed: Float,
                         playbackPitch: Float,
                         playbackSkipSilence: Boolean,
                         wasPlaying: Boolean
    ) : this(playQueue, repeatMode, playbackSpeed, playbackPitch, null, playbackSkipSilence, wasPlaying)
}
