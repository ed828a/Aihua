package com.dew.aihua.player.resolver

import android.content.Context
import com.dew.aihua.player.helper.ListHelper
import com.dew.aihua.player.helper.PlayerDataSource
import com.dew.aihua.player.helper.PlayerHelper
import com.dew.aihua.player.model.MediaSourceTag

import com.google.android.exoplayer2.source.MediaSource
import org.schabi.newpipe.extractor.MediaFormat
import org.schabi.newpipe.extractor.stream.StreamInfo

/**
 *  Created by Edward on 3/2/2019.
 */

class AudioPlaybackResolver(private val context: Context,
                            private val dataSource: PlayerDataSource
) : PlaybackResolver {

    override fun resolve(source: StreamInfo): MediaSource? {
        val liveSource = maybeBuildLiveMediaSource(dataSource, source)
        if (liveSource != null) return liveSource

        val index = ListHelper.getDefaultAudioFormat(context, source.audioStreams)
        if (index < 0 || index >= source.audioStreams.size) return null

        val audio = source.audioStreams[index]
        val tag = MediaSourceTag(source)
        return buildMediaSource(dataSource, audio.getUrl(), PlayerHelper.cacheKeyOf(source, audio),
            MediaFormat.getSuffixById(audio.formatId), tag)
    }
}
