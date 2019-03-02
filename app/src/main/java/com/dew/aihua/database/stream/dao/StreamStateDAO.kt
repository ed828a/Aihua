package com.dew.aihua.database.stream.dao

import androidx.room.*
import com.dew.aihua.database.BasicDAO
import com.dew.aihua.database.stream.model.StreamStateEntity
import com.dew.aihua.database.stream.model.StreamStateEntity.Companion.JOIN_STREAM_ID
import com.dew.aihua.database.stream.model.StreamStateEntity.Companion.STREAM_STATE_TABLE
import io.reactivex.Flowable

/**
 *  Created by Edward on 3/2/2019.
 */
@Dao
abstract class StreamStateDAO : BasicDAO<StreamStateEntity> {
    @get:Query("SELECT * FROM $STREAM_STATE_TABLE")
    abstract override val all: Flowable<List<StreamStateEntity>>

    @Query("DELETE FROM $STREAM_STATE_TABLE")
    abstract override fun deleteAll(): Int

    override fun listByService(serviceId: Int): Flowable<List<StreamStateEntity>> {
        throw UnsupportedOperationException()
    }

    @Query("SELECT * FROM $STREAM_STATE_TABLE WHERE $JOIN_STREAM_ID = :streamId")
    abstract fun getState(streamId: Long): Flowable<List<StreamStateEntity>>

    @Query("DELETE FROM $STREAM_STATE_TABLE WHERE $JOIN_STREAM_ID = :streamId")
    abstract fun deleteState(streamId: Long): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    internal abstract fun silentInsertInternal(streamState: StreamStateEntity)

    @Transaction
    open fun upsert(stream: StreamStateEntity): Long {
        silentInsertInternal(stream)
        return update(stream).toLong()
    }
}
