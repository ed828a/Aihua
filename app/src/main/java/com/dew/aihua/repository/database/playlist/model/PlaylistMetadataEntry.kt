package com.dew.aihua.repository.database.playlist.model

import androidx.room.ColumnInfo
import com.dew.aihua.repository.database.LocalItem
import com.dew.aihua.repository.database.playlist.PlaylistLocalItem

import com.dew.aihua.repository.database.playlist.model.PlaylistEntity.Companion.PLAYLIST_ID
import com.dew.aihua.repository.database.playlist.model.PlaylistEntity.Companion.PLAYLIST_NAME
import com.dew.aihua.repository.database.playlist.model.PlaylistEntity.Companion.PLAYLIST_THUMBNAIL_URL

/**
 *  Created by Edward on 2/23/2019.
 */


class PlaylistMetadataEntry(
    @field:ColumnInfo(name = PLAYLIST_ID) val uid: Long,
    @field:ColumnInfo(name = PLAYLIST_NAME) val name: String,
    @field:ColumnInfo(name = PLAYLIST_THUMBNAIL_URL) val thumbnailUrl: String,
    @field:ColumnInfo(name = PLAYLIST_STREAM_COUNT) val streamCount: Long) : PlaylistLocalItem {

    override val localItemType: LocalItem.LocalItemType
        get() = LocalItem.LocalItemType.PLAYLIST_LOCAL_ITEM

    override fun getOrderingName(): String = name

    companion object {
        const val PLAYLIST_STREAM_COUNT = "streamCount"
    }
}
