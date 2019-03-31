package com.dew.aihua.player.model

import com.dew.aihua.data.network.api.ExtractorHelper
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.stream.StreamType

import java.io.Serializable

/**
 *  Created by Edward on 3/2/2019.
 */
data class PlayQueueItem(val title: String = EMPTY_STRING,
                         val url: String = EMPTY_STRING,
                         val serviceId: Int,
                         val duration: Long,
                         val thumbnailUrl: String = EMPTY_STRING,
                         val uploader: String = EMPTY_STRING,
                         val streamType: StreamType
) : Serializable {

    var recoveryPosition: Long = RECOVERY_UNSET
        internal set

    var isAutoQueued: Boolean = false

    var error: Throwable? = null
        private set

    val stream: Single<StreamInfo>
        get() = ExtractorHelper.getStreamInfo(this.serviceId, this.url, false)
            .subscribeOn(Schedulers.io())
            .doOnError { throwable -> error = throwable }

    internal constructor(info: StreamInfo) : this(
        info.name, info.url, info.serviceId, info.duration,
        info.thumbnailUrl, info.uploaderName, info.streamType
    ) {

        if (info.startPosition > 0)
            recoveryPosition = info.startPosition * 1000
    }

    internal constructor(item: StreamInfoItem) : this(
        item.name, item.url, item.serviceId, item.duration,
        item.thumbnailUrl, item.uploaderName, item.streamType
    )

    companion object {
        const val RECOVERY_UNSET = java.lang.Long.MIN_VALUE
        private const val EMPTY_STRING = ""
    }
}