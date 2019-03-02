package com.dew.aihua.database.stream.model

import androidx.room.*
import com.dew.aihua.database.stream.model.StreamEntity.Companion.STREAM_SERVICE_ID
import com.dew.aihua.database.stream.model.StreamEntity.Companion.STREAM_TABLE
import com.dew.aihua.database.stream.model.StreamEntity.Companion.STREAM_URL
import com.dew.aihua.player.helper.Constants
import com.dew.aihua.player.model.PlayQueueItem
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.stream.StreamType
import java.io.Serializable


/**
 *  Created by Edward on 3/2/2019.
 */

@Entity(tableName = STREAM_TABLE, indices = [Index(value = [STREAM_SERVICE_ID, STREAM_URL], unique = true)])
class StreamEntity(serviceId: Int, @field:ColumnInfo(name = STREAM_TITLE)
var title: String?, @field:ColumnInfo(name = STREAM_URL)
                   var url: String?,
                   @field:ColumnInfo(name = STREAM_TYPE)
                   var streamType: StreamType?, @field:ColumnInfo(name = STREAM_THUMBNAIL_URL)
                   var thumbnailUrl: String?, @field:ColumnInfo(name = STREAM_UPLOADER)
                   var uploader: String?,
                   @field:ColumnInfo(name = STREAM_DURATION)
                   var duration: Long?) : Serializable {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = STREAM_ID)
    var uid: Long = 0

    @ColumnInfo(name = STREAM_SERVICE_ID)
    var serviceId = Constants.NO_SERVICE_ID

    init {
        this.serviceId = serviceId
    }

    @Ignore
    constructor(item: StreamInfoItem) : this(item.serviceId, item.name, item.url, item.streamType, item.thumbnailUrl,
        item.uploaderName, item.duration)

    @Ignore
    constructor(info: StreamInfo) : this(info.serviceId, info.name, info.url, info.streamType, info.thumbnailUrl,
        info.uploaderName, info.duration)

    @Ignore
    constructor(item: PlayQueueItem) : this(item.serviceId, item.title, item.url, item.streamType,
        item.thumbnailUrl, item.uploader, item.duration)

    companion object {

        const val STREAM_TABLE = "streams"
        const val STREAM_ID = "uid"
        const val STREAM_SERVICE_ID = "service_id"
        const val STREAM_URL = "url"
        const val STREAM_TITLE = "title"
        const val STREAM_TYPE = "stream_type"
        const val STREAM_DURATION = "duration"
        const val STREAM_UPLOADER = "uploader"
        const val STREAM_THUMBNAIL_URL = "thumbnail_url"
    }
}
