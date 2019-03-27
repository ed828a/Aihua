package com.dew.aihua.data.local.database.history.model

import androidx.room.*
import androidx.room.ForeignKey.CASCADE
import com.dew.aihua.data.local.database.history.model.StreamHistoryEntity.Companion.JOIN_STREAM_ID
import com.dew.aihua.data.local.database.history.model.StreamHistoryEntity.Companion.STREAM_ACCESS_DATE
import com.dew.aihua.data.local.database.history.model.StreamHistoryEntity.Companion.STREAM_HISTORY_TABLE
import com.dew.aihua.data.local.database.stream.model.StreamEntity
import java.util.*

/**
 *  Created by Edward on 3/2/2019.
 */
@Entity(
    tableName = STREAM_HISTORY_TABLE,
    primaryKeys = [JOIN_STREAM_ID, STREAM_ACCESS_DATE],
    indices = [Index(value = [JOIN_STREAM_ID])],
    foreignKeys = [ForeignKey(
        entity = StreamEntity::class,
        parentColumns = [StreamEntity.STREAM_ID],
        childColumns = [JOIN_STREAM_ID],
        onDelete = CASCADE,
        onUpdate = CASCADE
    )]
)// No need to index for timestamp as they will almost always be unique
class StreamHistoryEntity(
    @field:ColumnInfo(name = JOIN_STREAM_ID) var streamUid: Long,
    @field:ColumnInfo(name = STREAM_ACCESS_DATE) var accessDate: Date,
    @field:ColumnInfo(name = STREAM_REPEAT_COUNT) var repeatCount: Long
) {

    @Ignore
    constructor(streamUid: Long, accessDate: Date) : this(streamUid, accessDate, 1)

    companion object {
        const val STREAM_HISTORY_TABLE = "stream_history"
        const val JOIN_STREAM_ID = "stream_id"
        const val STREAM_ACCESS_DATE = "access_date"
        const val STREAM_REPEAT_COUNT = "repeat_count"
    }
}
