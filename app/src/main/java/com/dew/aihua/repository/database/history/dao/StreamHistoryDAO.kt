package com.dew.aihua.repository.database.history.dao

import androidx.room.Dao
import androidx.room.Query
import com.dew.aihua.repository.database.history.model.StreamHistoryEntity
import com.dew.aihua.repository.database.history.model.StreamHistoryEntity.Companion.JOIN_STREAM_ID
import com.dew.aihua.repository.database.history.model.StreamHistoryEntity.Companion.STREAM_ACCESS_DATE
import com.dew.aihua.repository.database.history.model.StreamHistoryEntity.Companion.STREAM_HISTORY_TABLE
import com.dew.aihua.repository.database.history.model.StreamHistoryEntity.Companion.STREAM_REPEAT_COUNT
import com.dew.aihua.repository.database.history.model.StreamHistoryEntry
import com.dew.aihua.repository.database.stream.model.StreamEntity.Companion.STREAM_ID
import com.dew.aihua.repository.database.stream.model.StreamEntity.Companion.STREAM_TABLE
import com.dew.aihua.repository.database.stream.model.StreamStatisticsEntry
import com.dew.aihua.repository.database.stream.model.StreamStatisticsEntry.Companion.STREAM_LATEST_DATE
import com.dew.aihua.repository.database.stream.model.StreamStatisticsEntry.Companion.STREAM_WATCH_COUNT

import io.reactivex.Flowable

/**
 *  Created by Edward on 2/23/2019.
 */

@Dao
abstract class StreamHistoryDAO : HistoryDAO<StreamHistoryEntity> {

    @get:Query("SELECT * FROM $STREAM_HISTORY_TABLE")
    abstract override val all: Flowable<List<StreamHistoryEntity>>

    @get:Query("SELECT * FROM $STREAM_TABLE INNER JOIN $STREAM_HISTORY_TABLE ON $STREAM_ID = $JOIN_STREAM_ID ORDER BY $STREAM_ACCESS_DATE DESC")
    abstract val history: Flowable<List<StreamHistoryEntry>>

    @get:Query("SELECT * FROM " + STREAM_TABLE +

            // Select the latest entry and watch count for each stream id on history table
            " INNER JOIN " +
            "(SELECT " + JOIN_STREAM_ID + ", " +
            "  MAX(" + STREAM_ACCESS_DATE + ") AS " + STREAM_LATEST_DATE + ", " +
            "  SUM(" + STREAM_REPEAT_COUNT + ") AS " + STREAM_WATCH_COUNT +
            " FROM " + STREAM_HISTORY_TABLE + " GROUP BY " + JOIN_STREAM_ID + ")" +

            " ON " + STREAM_ID + " = " + JOIN_STREAM_ID)
    abstract val statistics: Flowable<List<StreamStatisticsEntry>>

    @Query("SELECT * FROM $STREAM_HISTORY_TABLE WHERE $STREAM_ACCESS_DATE = (SELECT MAX($STREAM_ACCESS_DATE) FROM $STREAM_HISTORY_TABLE)")
    abstract override fun getLatestEntry(): StreamHistoryEntity?

    @Query("DELETE FROM $STREAM_HISTORY_TABLE")
    abstract override fun deleteAll(): Int

    override fun listByService(serviceId: Int): Flowable<List<StreamHistoryEntity>> {
        throw UnsupportedOperationException()
    }

    @Query("DELETE FROM $STREAM_HISTORY_TABLE WHERE $JOIN_STREAM_ID = :streamId")
    abstract fun deleteStreamHistory(streamId: Long): Int
}
