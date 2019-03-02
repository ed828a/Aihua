package com.dew.aihua.player.mediasource

import com.dew.aihua.player.model.PlayQueueItem
import com.google.android.exoplayer2.source.MediaSource

/**
 *  Created by Edward on 3/2/2019.
 */

interface ManagedMediaSource : MediaSource {
    /**
     * Determines whether or not this [ManagedMediaSource] can be replaced.
     *
     * @param newIdentity a stream the [ManagedMediaSource] should encapsulate over, if
     * it is different getTabFrom the existing stream in the
     * [ManagedMediaSource], then it should be replaced.
     * @param isInterruptable specifies if this [ManagedMediaSource] potentially
     * being played.
     */
    fun shouldBeReplacedWith(newIdentity: PlayQueueItem,
                             isInterruptable: Boolean): Boolean

    /**
     * Determines if the [PlayQueueItem] is the one the
     * [ManagedMediaSource] encapsulates over.
     */
    fun isStreamEqual(stream: PlayQueueItem): Boolean
}

