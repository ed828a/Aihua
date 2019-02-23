package com.dew.aihua.repository.database.stream.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import com.dew.aihua.repository.database.stream.model.StreamStateEntity.Companion.STREAM_STATE_TABLE
import com.dew.aihua.repository.database.stream.model.StreamStateEntity.Companion.JOIN_STREAM_ID

/**
 *  Created by Edward on 2/23/2019.
 */

@Entity(tableName = STREAM_STATE_TABLE,
    primaryKeys = [JOIN_STREAM_ID],
    foreignKeys = [ForeignKey(entity = StreamEntity::class,
        parentColumns = arrayOf(StreamEntity.STREAM_ID),
        childColumns = arrayOf(JOIN_STREAM_ID),
        onDelete = ForeignKey.CASCADE,
        onUpdate = ForeignKey.CASCADE
    )])
class StreamStateEntity(@field:ColumnInfo(name = JOIN_STREAM_ID) var streamUid: Long,
                        @field:ColumnInfo(name = STREAM_PROGRESS_TIME) var progressTime: Long) {
    companion object {
        const val STREAM_STATE_TABLE = "stream_state"
        const val JOIN_STREAM_ID = "stream_id"
        const val STREAM_PROGRESS_TIME = "progress_time"
    }
}
