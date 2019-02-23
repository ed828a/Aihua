package com.dew.aihua.repository.database.playlist.model

import androidx.room.ColumnInfo
import com.dew.aihua.repository.database.LocalItem
import com.dew.aihua.repository.database.stream.model.StreamEntity
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.stream.StreamType

/**
 *  Created by Edward on 2/23/2019.
 */

class PlaylistStreamEntry(
    @field:ColumnInfo(name = StreamEntity.STREAM_ID) val uid: Long,
    @field:ColumnInfo(name = StreamEntity.STREAM_SERVICE_ID) val serviceId: Int,
    @field:ColumnInfo(name = StreamEntity.STREAM_URL) val url: String,
    @field:ColumnInfo(name = StreamEntity.STREAM_TITLE) val title: String,
    @field:ColumnInfo(name = StreamEntity.STREAM_TYPE) val streamType: StreamType,
    @field:ColumnInfo(name = StreamEntity.STREAM_DURATION) val duration: Long,
    @field:ColumnInfo(name = StreamEntity.STREAM_UPLOADER) val uploader: String,
    @field:ColumnInfo(name = StreamEntity.STREAM_THUMBNAIL_URL) val thumbnailUrl: String,
    @field:ColumnInfo(name = PlaylistStreamEntity.JOIN_STREAM_ID) val streamId: Long,
    @field:ColumnInfo(name = PlaylistStreamEntity.JOIN_INDEX) val joinIndex: Int) : LocalItem {

    override val localItemType: LocalItem.LocalItemType
        get() = LocalItem.LocalItemType.PLAYLIST_STREAM_ITEM

    @Throws(IllegalArgumentException::class)
    fun toStreamInfoItem(): StreamInfoItem {
        val item = StreamInfoItem(serviceId, url, title, streamType)
        item.thumbnailUrl = thumbnailUrl
        item.uploaderName = uploader
        item.duration = duration
        return item
    }
}
