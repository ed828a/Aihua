package com.dew.aihua.database.playlist.dao

import androidx.room.Dao
import androidx.room.Query
import io.reactivex.Flowable
import com.dew.aihua.database.BasicDAO
import com.dew.aihua.database.playlist.model.PlaylistEntity
import com.dew.aihua.database.playlist.model.PlaylistEntity.Companion.PLAYLIST_ID
import com.dew.aihua.database.playlist.model.PlaylistEntity.Companion.PLAYLIST_TABLE


/**
 *  Created by Edward on 3/2/2019.
 */

@Dao
abstract class PlaylistDAO : BasicDAO<PlaylistEntity> {
    @get:Query("SELECT * FROM $PLAYLIST_TABLE")
    abstract override val all: Flowable<List<PlaylistEntity>>

    @Query("DELETE FROM $PLAYLIST_TABLE")
    abstract override fun deleteAll(): Int

    override fun listByService(serviceId: Int): Flowable<List<PlaylistEntity>> {
        throw UnsupportedOperationException()
    }

    @Query("SELECT * FROM $PLAYLIST_TABLE WHERE $PLAYLIST_ID = :playlistId")
    abstract fun getPlaylist(playlistId: Long): Flowable<List<PlaylistEntity>>

    @Query("DELETE FROM $PLAYLIST_TABLE WHERE $PLAYLIST_ID = :playlistId")
    abstract fun deletePlaylist(playlistId: Long): Int
}
