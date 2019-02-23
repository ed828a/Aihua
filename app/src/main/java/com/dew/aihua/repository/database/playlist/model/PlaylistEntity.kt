package com.dew.aihua.repository.database.playlist.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.dew.aihua.repository.database.playlist.model.PlaylistEntity.Companion.PLAYLIST_NAME
import com.dew.aihua.repository.database.playlist.model.PlaylistEntity.Companion.PLAYLIST_TABLE

/**
 *  Created by Edward on 2/23/2019.
 */


@Entity(tableName = PLAYLIST_TABLE, indices = [Index(value = arrayOf(PLAYLIST_NAME))])
class PlaylistEntity(
    @field:ColumnInfo(name = PLAYLIST_NAME) var name: String?,
    @field:ColumnInfo(name = PLAYLIST_THUMBNAIL_URL) var thumbnailUrl: String?) {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = PLAYLIST_ID)
    var uid: Long = 0

    companion object {
        const val PLAYLIST_TABLE = "playlists"
        const val PLAYLIST_ID = "uid"
        const val PLAYLIST_NAME = "name"
        const val PLAYLIST_THUMBNAIL_URL = "thumbnail_url"
    }
}
