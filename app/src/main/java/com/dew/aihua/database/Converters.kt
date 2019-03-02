package com.dew.aihua.database

import androidx.room.TypeConverter
import org.schabi.newpipe.extractor.stream.StreamType
import java.util.*

/**
 *  Created by Edward on 3/2/2019.
 */

object Converters {

    /**
     * Convert a long value to a date
     * @param value the long value
     * @return the date
     */
    @TypeConverter
    @JvmStatic
    fun fromTimestamp(value: Long?): Date? {
        return if (value == null) null else Date(value)
    }

    /**
     * Convert a date to a long value
     * @param date the date
     * @return the long value
     */
    @TypeConverter
    @JvmStatic
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    @JvmStatic
    fun streamTypeOf(value: String): StreamType {
        return StreamType.valueOf(value)
    }

    @TypeConverter
    @JvmStatic
    fun stringOf(streamType: StreamType): String {
        return streamType.name
    }
}

