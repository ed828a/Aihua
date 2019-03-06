package com.dew.aihua.database.stream.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.CASCADE
import com.dew.aihua.database.stream.model.StreamStateEntity.Companion.JOIN_STREAM_ID
import com.dew.aihua.database.stream.model.StreamStateEntity.Companion.STREAM_STATE_TABLE

/**
 *  Created by Edward on 3/2/2019.
 */
@Entity(
    tableName = STREAM_STATE_TABLE,
    primaryKeys = [JOIN_STREAM_ID],
    foreignKeys = [ForeignKey(
        entity = StreamEntity::class,
        parentColumns = [StreamEntity.STREAM_ID],
        childColumns = [JOIN_STREAM_ID],
        onDelete = CASCADE,
        onUpdate = CASCADE
    )]
)
class StreamStateEntity(
    @field:ColumnInfo(name = JOIN_STREAM_ID) var streamUid: Long,
    @field:ColumnInfo(name = STREAM_PROGRESS_TIME) var progressTime: Long
) {
    companion object {
        const val STREAM_STATE_TABLE = "stream_state"
        const val JOIN_STREAM_ID = "stream_id"
        const val STREAM_PROGRESS_TIME = "progress_time"
    }
}
