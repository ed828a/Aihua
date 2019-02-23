package com.dew.aihua.repository.database.playlist.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.dew.aihua.repository.database.playlist.model.PlaylistStreamEntity.Companion.JOIN_INDEX
import com.dew.aihua.repository.database.playlist.model.PlaylistStreamEntity.Companion.JOIN_PLAYLIST_ID
import com.dew.aihua.repository.database.playlist.model.PlaylistStreamEntity.Companion.PLAYLIST_STREAM_JOIN_TABLE
import com.dew.aihua.repository.database.playlist.model.PlaylistStreamEntity.Companion.JOIN_STREAM_ID
import com.dew.aihua.repository.database.stream.model.StreamEntity
/**
 *  Created by Edward on 2/23/2019.
 */


@Entity(tableName = PLAYLIST_STREAM_JOIN_TABLE,
    primaryKeys = [JOIN_PLAYLIST_ID, JOIN_INDEX],
    indices = [Index(value = arrayOf(JOIN_PLAYLIST_ID, JOIN_INDEX), unique = true), Index(value = arrayOf(JOIN_STREAM_ID))],
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = arrayOf(PlaylistEntity.PLAYLIST_ID),
            childColumns = arrayOf(JOIN_PLAYLIST_ID),
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
            deferred = true),

        ForeignKey(
            entity = StreamEntity::class,
            parentColumns = arrayOf(StreamEntity.STREAM_ID),
            childColumns = arrayOf(JOIN_STREAM_ID),
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
            deferred = true)
    ])
class PlaylistStreamEntity(
    @field:ColumnInfo(name = JOIN_PLAYLIST_ID) var playlistUid: Long,
    @field:ColumnInfo(name = JOIN_STREAM_ID) var streamUid: Long,
    @field:ColumnInfo(name = JOIN_INDEX) var index: Int) {

    companion object {
        const val PLAYLIST_STREAM_JOIN_TABLE = "playlist_stream_join"
        const val JOIN_PLAYLIST_ID = "playlist_id"
        const val JOIN_STREAM_ID = "stream_id"
        const val JOIN_INDEX = "join_index"
    }
}
