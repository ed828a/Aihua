package com.dew.aihua.database.playlist.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import io.reactivex.Flowable
import com.dew.aihua.database.BasicDAO
import com.dew.aihua.database.playlist.model.PlaylistEntity.Companion.PLAYLIST_ID
import com.dew.aihua.database.playlist.model.PlaylistEntity.Companion.PLAYLIST_NAME
import com.dew.aihua.database.playlist.model.PlaylistEntity.Companion.PLAYLIST_TABLE
import com.dew.aihua.database.playlist.model.PlaylistEntity.Companion.PLAYLIST_THUMBNAIL_URL
import com.dew.aihua.database.playlist.model.PlaylistMetadataEntry
import com.dew.aihua.database.playlist.model.PlaylistMetadataEntry.Companion.PLAYLIST_STREAM_COUNT
import com.dew.aihua.database.playlist.model.PlaylistStreamEntity
import com.dew.aihua.database.playlist.model.PlaylistStreamEntity.Companion.JOIN_INDEX
import com.dew.aihua.database.playlist.model.PlaylistStreamEntity.Companion.JOIN_PLAYLIST_ID
import com.dew.aihua.database.playlist.model.PlaylistStreamEntity.Companion.JOIN_STREAM_ID
import com.dew.aihua.database.playlist.model.PlaylistStreamEntity.Companion.PLAYLIST_STREAM_JOIN_TABLE
import com.dew.aihua.database.playlist.model.PlaylistStreamEntry
import com.dew.aihua.database.stream.model.StreamEntity.Companion.STREAM_ID
import com.dew.aihua.database.stream.model.StreamEntity.Companion.STREAM_TABLE

/**
 *  Created by Edward on 2/23/2019.
 */

@Dao
abstract class PlaylistStreamDAO : BasicDAO<PlaylistStreamEntity> {
    @get:Query("SELECT * FROM $PLAYLIST_STREAM_JOIN_TABLE")
    abstract override val all: Flowable<List<PlaylistStreamEntity>>

    @get:Transaction
    @get:Query("SELECT " + PLAYLIST_ID + ", " + PLAYLIST_NAME + ", " +
            PLAYLIST_THUMBNAIL_URL + ", " +
            "COALESCE(COUNT(" + JOIN_PLAYLIST_ID + "), 0) AS " + PLAYLIST_STREAM_COUNT +

            " FROM " + PLAYLIST_TABLE +
            " LEFT JOIN " + PLAYLIST_STREAM_JOIN_TABLE +
            " ON " + PLAYLIST_ID + " = " + JOIN_PLAYLIST_ID +
            " GROUP BY " + JOIN_PLAYLIST_ID +
            " ORDER BY " + PLAYLIST_NAME + " COLLATE NOCASE ASC")
    abstract val playlistMetadata: Flowable<List<PlaylistMetadataEntry>>

    @Query("DELETE FROM $PLAYLIST_STREAM_JOIN_TABLE")
    abstract override fun deleteAll(): Int

    override fun listByService(serviceId: Int): Flowable<List<PlaylistStreamEntity>> {
        throw UnsupportedOperationException()
    }

    @Query("DELETE FROM $PLAYLIST_STREAM_JOIN_TABLE WHERE $JOIN_PLAYLIST_ID = :playlistId")
    abstract fun deleteBatch(playlistId: Long)

    @Query("SELECT COALESCE(MAX($JOIN_INDEX), -1) FROM $PLAYLIST_STREAM_JOIN_TABLE WHERE $JOIN_PLAYLIST_ID = :playlistId")
    abstract fun getMaximumIndexOf(playlistId: Long): Flowable<Int>

    @Transaction
    @Query("SELECT * FROM " + STREAM_TABLE + " INNER JOIN " +
            // get ids of streams of the given playlist
            "(SELECT " + JOIN_STREAM_ID + "," + JOIN_INDEX +
            " FROM " + PLAYLIST_STREAM_JOIN_TABLE +
            " WHERE " + JOIN_PLAYLIST_ID + " = :playlistId)" +

            // then merge with the stream metadata
            " ON " + STREAM_ID + " = " + JOIN_STREAM_ID +
            " ORDER BY " + JOIN_INDEX + " ASC")
    abstract fun getOrderedStreamsOf(playlistId: Long): Flowable<List<PlaylistStreamEntry>>
}
