package com.dew.aihua.player.mediasession

import android.support.v4.media.MediaDescriptionCompat

/**
 *  Created by Edward on 2/23/2019.
 */

interface MediaSessionCallback {

    fun onPlay()
    fun onPause()
    fun onSkipToPrevious()
    fun onSkipToNext()
    fun onSkipToIndex(index: Int)

    fun getCurrentPlayingIndex(): Int
    fun getQueueSize(): Int
    fun getQueueMetadata(index: Int): MediaDescriptionCompat?
}
