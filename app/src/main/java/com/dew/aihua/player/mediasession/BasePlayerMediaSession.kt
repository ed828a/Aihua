package com.dew.aihua.player.mediasession

import android.net.Uri
import android.support.v4.media.MediaDescriptionCompat
import com.dew.aihua.player.playerUI.BasePlayer

/**
 *  Created by Edward on 3/2/2019.
 */class BasePlayerMediaSession(private val player: BasePlayer) : MediaSessionCallback {

    override fun onPlay() {
        player.onPlay()
    }

    override fun onPause() {
        player.onPause()
    }

    override fun onSkipToPrevious() {
        player.onPlayPrevious()
    }

    override fun onSkipToNext() {
        player.onPlayNext()
    }

    override fun onSkipToIndex(index: Int) {
        if (player.playQueue == null) return
        player.onSelected(player.playQueue!!.getItem(index)!!)
    }

    override fun getCurrentPlayingIndex(): Int =
        if (player.playQueue == null) -1 else player.playQueue!!.index


    override fun getQueueSize(): Int =
        if (player.playQueue == null) -1 else player.playQueue!!.size()


    override fun getQueueMetadata(index: Int): MediaDescriptionCompat? {
        if (player.playQueue == null || player.playQueue!!.getItem(index) == null) {
            return null
        }

        val item = player.playQueue!!.getItem(index)
        val descriptionBuilder = MediaDescriptionCompat.Builder()
            .setMediaId(index.toString())
            .setTitle(item!!.title)
            .setSubtitle(item.uploader)

        val thumbnailUri = Uri.parse(item.thumbnailUrl)
        if (thumbnailUri != null) descriptionBuilder.setIconUri(thumbnailUri)

        return descriptionBuilder.build()
    }


}
