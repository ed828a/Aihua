package com.dew.aihua.player.model

import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.VideoStream
import java.io.Serializable

/**
 *  Created by Edward on 3/2/2019.
 */
data class MediaSourceTag (val metadata: StreamInfo,
                      val sortedAvailableVideoStreams: List<VideoStream> = emptyList(),
                      val selectedVideoStreamIndex: Int = -1 /*indexNotAvailable=*/
) : Serializable {

    val selectedVideoStream: VideoStream?
        get() =
            if (selectedVideoStreamIndex < 0 || selectedVideoStreamIndex >= sortedAvailableVideoStreams.size)
                null
            else
                sortedAvailableVideoStreams[selectedVideoStreamIndex]
}
