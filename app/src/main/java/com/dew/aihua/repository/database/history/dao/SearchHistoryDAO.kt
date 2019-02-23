package com.dew.aihua.repository.database.history.dao

import androidx.room.Dao
import androidx.room.Query
import com.dew.aihua.repository.database.history.model.SearchHistoryEntry
import com.dew.aihua.repository.database.history.model.SearchHistoryEntry.Companion.CREATION_DATE
import com.dew.aihua.repository.database.history.model.SearchHistoryEntry.Companion.ID
import com.dew.aihua.repository.database.history.model.SearchHistoryEntry.Companion.SEARCH
import com.dew.aihua.repository.database.history.model.SearchHistoryEntry.Companion.SERVICE_ID
import com.dew.aihua.repository.database.history.model.SearchHistoryEntry.Companion.TABLE_NAME
import io.reactivex.Flowable

/**
 *  Created by Edward on 2/23/2019.
 */

@Dao
interface SearchHistoryDAO : HistoryDAO<SearchHistoryEntry> {

    @get:Query("SELECT * FROM $TABLE_NAME$ORDER_BY_CREATION_DATE")
    override val all: Flowable<List<SearchHistoryEntry>>

    @Query("SELECT * FROM $TABLE_NAME WHERE $ID = (SELECT MAX($ID) FROM $TABLE_NAME)")
    override fun getLatestEntry(): SearchHistoryEntry?

    @Query("DELETE FROM $TABLE_NAME")
    override fun deleteAll(): Int

    @Query("DELETE FROM $TABLE_NAME WHERE $SEARCH = :query")
    fun deleteAllWhereQuery(query: String): Int

    @Query("SELECT * FROM $TABLE_NAME GROUP BY $SEARCH$ORDER_BY_CREATION_DATE LIMIT :limit")
    fun getUniqueEntries(limit: Int): Flowable<List<SearchHistoryEntry>>

    @Query("SELECT * FROM $TABLE_NAME WHERE $SERVICE_ID = :serviceId $ORDER_BY_CREATION_DATE")
    override fun listByService(serviceId: Int): Flowable<List<SearchHistoryEntry>>

    @Query("SELECT * FROM $TABLE_NAME WHERE $SEARCH LIKE :query || '%' GROUP BY $SEARCH LIMIT :limit")
    fun getSimilarEntries(query: String, limit: Int): Flowable<List<SearchHistoryEntry>>

    companion object {
        const val ORDER_BY_CREATION_DATE = " ORDER BY $CREATION_DATE DESC"
    }
}
