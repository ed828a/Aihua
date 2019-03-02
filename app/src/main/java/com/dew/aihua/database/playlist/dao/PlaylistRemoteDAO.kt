package com.dew.aihua.database.playlist.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import io.reactivex.Flowable

import com.dew.aihua.database.BasicDAO
import com.dew.aihua.database.playlist.model.PlaylistRemoteEntity
import com.dew.aihua.database.playlist.model.PlaylistRemoteEntity.Companion.REMOTE_PLAYLIST_ID
import com.dew.aihua.database.playlist.model.PlaylistRemoteEntity.Companion.REMOTE_PLAYLIST_SERVICE_ID
import com.dew.aihua.database.playlist.model.PlaylistRemoteEntity.Companion.REMOTE_PLAYLIST_TABLE
import com.dew.aihua.database.playlist.model.PlaylistRemoteEntity.Companion.REMOTE_PLAYLIST_URL


/**
 *  Created by Edward on 2/23/2019.
 */

@Dao
abstract class PlaylistRemoteDAO : BasicDAO<PlaylistRemoteEntity> {
    @get:Query("SELECT * FROM $REMOTE_PLAYLIST_TABLE")
    abstract override val all: Flowable<List<PlaylistRemoteEntity>>

    @Query("DELETE FROM $REMOTE_PLAYLIST_TABLE")
    abstract override fun deleteAll(): Int

    @Query("SELECT * FROM $REMOTE_PLAYLIST_TABLE WHERE $REMOTE_PLAYLIST_SERVICE_ID = :serviceId")
    abstract override fun listByService(serviceId: Int): Flowable<List<PlaylistRemoteEntity>>

    @Query("SELECT * FROM $REMOTE_PLAYLIST_TABLE WHERE $REMOTE_PLAYLIST_URL = :url AND $REMOTE_PLAYLIST_SERVICE_ID = :serviceId")
    abstract fun getPlaylist(serviceId: Long, url: String): Flowable<List<PlaylistRemoteEntity>>

    @Query("SELECT $REMOTE_PLAYLIST_ID FROM $REMOTE_PLAYLIST_TABLE WHERE $REMOTE_PLAYLIST_URL = :url AND $REMOTE_PLAYLIST_SERVICE_ID = :serviceId")
    abstract fun getPlaylistIdInternal(serviceId: Long, url: String): Long?

    @Transaction
    open fun upsert(playlist: PlaylistRemoteEntity): Long {
        val playlistId = getPlaylistIdInternal(playlist.serviceId.toLong(), playlist.url!!)

        return if (playlistId == null) {
            insert(playlist)
        } else {
            playlist.uid = playlistId
            update(playlist)
            playlistId
        }
    }

    @Query("DELETE FROM $REMOTE_PLAYLIST_TABLE WHERE $REMOTE_PLAYLIST_ID = :playlistId")
    abstract fun deletePlaylist(playlistId: Long): Int
}
