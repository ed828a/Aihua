package com.dew.aihua.player.helper

import com.google.android.exoplayer2.DefaultLoadControl
import com.google.android.exoplayer2.LoadControl
import com.google.android.exoplayer2.Renderer
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.upstream.Allocator

/**
 *  Created by Edward on 3/2/2019.
 */

class LoadController private constructor(initialPlaybackBufferMs: Int,
                                         minimumPlaybackbufferMs: Int,
                                         optimalPlaybackBufferMs: Int) : LoadControl {

    private val initialPlaybackBufferUs: Long = (initialPlaybackBufferMs * 1000).toLong()
    private val internalLoadControl: LoadControl

    ///////////////////////////////////////////////////////////////////////////
    // Default Load Control
    ///////////////////////////////////////////////////////////////////////////

    constructor() : this(PlayerHelper.getPlaybackStartBufferMs(),
        PlayerHelper.getPlaybackMinimumBufferMs(),
        PlayerHelper.getPlaybackOptimalBufferMs())

    init {
        // this is a default one in DefaultLoadControl
        internalLoadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /*minBufferMs=*/minimumPlaybackbufferMs,
                /*maxBufferMs=*/optimalPlaybackBufferMs,
                /*bufferForPlaybackMs=*/initialPlaybackBufferMs,
                /*bufferForPlaybackAfterRebufferMs=*/initialPlaybackBufferMs
            )
            .createDefaultLoadControl()
    }

    ///////////////////////////////////////////////////////////////////////////
    // Custom behaviours
    ///////////////////////////////////////////////////////////////////////////

    override fun onPrepared() {
        internalLoadControl.onPrepared()
    }

    override fun onTracksSelected(renderers: Array<Renderer>,
                                  trackGroupArray: TrackGroupArray,
                                  trackSelectionArray: TrackSelectionArray
    ) {
        internalLoadControl.onTracksSelected(renderers, trackGroupArray, trackSelectionArray)
    }

    override fun onStopped() {
        internalLoadControl.onStopped()
    }

    override fun onReleased() {
        internalLoadControl.onReleased()
    }

    override fun getAllocator(): Allocator = internalLoadControl.allocator


    override fun getBackBufferDurationUs(): Long = internalLoadControl.backBufferDurationUs

    override fun retainBackBufferFromKeyframe(): Boolean = internalLoadControl.retainBackBufferFromKeyframe()

    override fun shouldContinueLoading(bufferedDurationUs: Long, playbackSpeed: Float): Boolean =
        internalLoadControl.shouldContinueLoading(bufferedDurationUs, playbackSpeed)

    override fun shouldStartPlayback(bufferedDurationUs: Long, playbackSpeed: Float, rebuffering: Boolean): Boolean {
        val isInitialPlaybackBufferFilled = bufferedDurationUs >= this.initialPlaybackBufferUs * playbackSpeed
        val isInternalStartingPlayback = internalLoadControl.shouldStartPlayback(bufferedDurationUs, playbackSpeed, rebuffering)

        return isInitialPlaybackBufferFilled || isInternalStartingPlayback
    }

    companion object {

        const val TAG = "LoadController"
    }
}
