package com.dew.aihua.repository.database

import androidx.room.TypeConverter
import org.schabi.newpipe.extractor.stream.StreamType
import java.util.*

/**
 *  Created by Edward on 2/23/2019.
 */

object Converters {

    /**
     * Convert a long value to a date
     * @param value the long value
     * @return the date
     */
    @JvmStatic
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return if (value == null) null else Date(value)
    }

    /**
     * Convert a date to a long value
     * @param date the date
     * @return the long value
     */
    @JvmStatic
    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    @JvmStatic
    @TypeConverter
    fun streamTypeOf(value: String): StreamType {
        return StreamType.valueOf(value)
    }

    @JvmStatic
    @TypeConverter
    fun stringOf(streamType: StreamType): String {
        return streamType.name
    }
}
