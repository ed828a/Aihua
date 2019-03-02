package com.dew.aihua.player.playback


import com.dew.aihua.player.mediasession.MediaSessionCallback
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.mediasession.DefaultPlaybackController

/**
 *  Created by Edward on 3/2/2019.
 */

class PlayQueuePlaybackController(private val callback: MediaSessionCallback) : DefaultPlaybackController() {

    override fun onPlay(player: Player) {
        callback.onPlay()
    }

    override fun onPause(player: Player) {
        callback.onPause()
    }
}
