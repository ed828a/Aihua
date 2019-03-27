package com.dew.aihua.data.local.database.stream.dao

import androidx.room.*
import com.dew.aihua.data.local.database.BasicDAO
import io.reactivex.Flowable
import java.util.ArrayList
import com.dew.aihua.data.local.database.history.model.StreamHistoryEntity
import com.dew.aihua.data.local.database.history.model.StreamHistoryEntity.Companion.STREAM_HISTORY_TABLE
import com.dew.aihua.data.local.database.playlist.model.PlaylistStreamEntity
import com.dew.aihua.data.local.database.playlist.model.PlaylistStreamEntity.Companion.PLAYLIST_STREAM_JOIN_TABLE
import com.dew.aihua.data.local.database.stream.model.StreamEntity
import com.dew.aihua.data.local.database.stream.model.StreamEntity.Companion.STREAM_ID
import com.dew.aihua.data.local.database.stream.model.StreamEntity.Companion.STREAM_SERVICE_ID
import com.dew.aihua.data.local.database.stream.model.StreamEntity.Companion.STREAM_TABLE
import com.dew.aihua.data.local.database.stream.model.StreamEntity.Companion.STREAM_URL

/**
 *  Created by Edward on 3/2/2019.
 */


@Dao
abstract class StreamDAO : BasicDAO<StreamEntity> {
    @get:Query("SELECT * FROM $STREAM_TABLE")
    abstract override val all: Flowable<List<StreamEntity>>

    @Query("DELETE FROM $STREAM_TABLE")
    abstract override fun deleteAll(): Int

    @Query("SELECT * FROM $STREAM_TABLE WHERE $STREAM_SERVICE_ID = :serviceId")
    abstract override fun listByService(serviceId: Int): Flowable<List<StreamEntity>>

    @Query("SELECT * FROM " + STREAM_TABLE + " WHERE " +
            STREAM_URL + " = :url AND " +
            STREAM_SERVICE_ID + " = :serviceId")
    abstract fun getStream(serviceId: Long, url: String): Flowable<List<StreamEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    internal abstract fun silentInsertAllInternal(streams: List<StreamEntity>)

    @Query("SELECT " + STREAM_ID + " FROM " + STREAM_TABLE + " WHERE " +
            STREAM_URL + " = :url AND " +
            STREAM_SERVICE_ID + " = :serviceId")
    internal abstract fun getStreamIdInternal(serviceId: Long, url: String?): Long?

    @Transaction
    open fun upsert(stream: StreamEntity): Long {
        val streamIdCandidate = getStreamIdInternal(stream.serviceId.toLong(), stream.url)

        return if (streamIdCandidate == null) {
            insert(stream)
        } else {
            stream.uid = streamIdCandidate
            update(stream)
            streamIdCandidate
        }
    }

    @Transaction
    open fun upsertAll(streams: List<StreamEntity>): List<Long> {
        silentInsertAllInternal(streams)

        val streamIds = ArrayList<Long>(streams.size)
        for (stream in streams) {
            val streamId = getStreamIdInternal(stream.serviceId.toLong(), stream.url)
                ?: throw IllegalStateException("StreamID cannot be null just after insertion.")

            streamIds.add(streamId)
            stream.uid = streamId
        }

        update(streams)
        return streamIds
    }

    @Query("DELETE FROM " + STREAM_TABLE + " WHERE " + STREAM_ID +
            " NOT IN " +
            "(SELECT DISTINCT " + STREAM_ID + " FROM " + STREAM_TABLE +

            " LEFT JOIN " + STREAM_HISTORY_TABLE +
            " ON " + STREAM_ID + " = " +
            StreamHistoryEntity.STREAM_HISTORY_TABLE + "." + StreamHistoryEntity.JOIN_STREAM_ID +

            " LEFT JOIN " + PLAYLIST_STREAM_JOIN_TABLE +
            " ON " + STREAM_ID + " = " +
            PlaylistStreamEntity.PLAYLIST_STREAM_JOIN_TABLE + "." + PlaylistStreamEntity.JOIN_STREAM_ID +
            ")")
    abstract fun deleteOrphans(): Int
}
