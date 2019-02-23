package com.dew.aihua.player.resolver

import android.content.Context
import android.net.Uri
import com.dew.aihua.player.helper.PlayerDataSource
import com.dew.aihua.player.helper.PlayerHelper
import com.dew.aihua.player.model.MediaSourceTag
import com.dew.aihua.util.ListHelper
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.Format
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.MergingMediaSource
import org.schabi.newpipe.extractor.MediaFormat
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.VideoStream
import java.util.ArrayList

/**
 *  Created by Edward on 2/23/2019.
 */

class VideoPlaybackResolver(private val context: Context,
                            private val dataSource: PlayerDataSource,
                            private val qualityResolver: QualityResolver) : PlaybackResolver {

    var playbackQuality: String? = null

    interface QualityResolver {
        fun getDefaultResolutionIndex(sortedVideos: List<VideoStream>): Int
        fun getOverrideResolutionIndex(sortedVideos: List<VideoStream>,
                                       playbackQuality: String?): Int
    }

    override fun resolve(info: StreamInfo): MediaSource? {
        val liveSource = maybeBuildLiveMediaSource(dataSource, info)
        if (liveSource != null) return liveSource

        val mediaSources = ArrayList<MediaSource>()

        // Create video stream source
        val videos = ListHelper.getSortedStreamVideosList(context,
            info.videoStreams, info.videoOnlyStreams, false)

        val index: Int  = when {
            videos.isEmpty() -> -1
            playbackQuality == null -> qualityResolver.getDefaultResolutionIndex(videos)
            else -> qualityResolver.getOverrideResolutionIndex(videos, playbackQuality)
        }
        val tag = MediaSourceTag(info, videos, index)
        val video = tag.selectedVideoStream

        if (video != null) {
            val streamSource = buildMediaSource(dataSource, video.getUrl(),
                PlayerHelper.cacheKeyOf(info, video),
                MediaFormat.getSuffixById(video.formatId), tag)
            mediaSources.add(streamSource)
        }

        // Create optional audio stream source
        val audioStreams = info.audioStreams
        val audio = if (audioStreams.isEmpty())
            null
        else
            audioStreams[ListHelper.getDefaultAudioFormat(context, audioStreams)]

        // Use the audio stream if there is no video stream, or
        // Merge with audio stream in case if video does not contain audio
        if (audio != null && (video != null && video.isVideoOnly || video == null)) {
            val audioSource = buildMediaSource(dataSource, audio.getUrl(),
                PlayerHelper.cacheKeyOf(info, audio),
                MediaFormat.getSuffixById(audio.formatId), tag)
            mediaSources.add(audioSource)
        }

        // If there is no audio or video sources, then this media source cannot be played back
        if (mediaSources.isEmpty()) return null
        // Below are auxiliary media sources

        // Create subtitle sources
        for (subtitle in info.subtitles) {
            val mimeType = PlayerHelper.subtitleMimeTypesOf(subtitle.format)

            val textFormat = Format.createTextSampleFormat(null, mimeType,
                C.SELECTION_FLAG_AUTOSELECT, PlayerHelper.captionLanguageOf(context, subtitle))
            val textSource = dataSource.sampleMediaSourceFactory
                .createMediaSource(Uri.parse(subtitle.getURL()), textFormat, C.TIME_UNSET)
            mediaSources.add(textSource)
        }

        return if (mediaSources.size == 1) {
            mediaSources[0]
        } else {
            MergingMediaSource(*mediaSources.toTypedArray())
        }
    }
}
