package com.dew.aihua.repository.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.dew.aihua.repository.database.downloadDB.DownloadDAO
import com.dew.aihua.repository.database.downloadDB.MissionEntity
import com.dew.aihua.repository.database.history.dao.SearchHistoryDAO
import com.dew.aihua.repository.database.history.dao.StreamHistoryDAO
import com.dew.aihua.repository.database.history.model.SearchHistoryEntry
import com.dew.aihua.repository.database.history.model.StreamHistoryEntity
import com.dew.aihua.repository.database.playlist.dao.PlaylistDAO
import com.dew.aihua.repository.database.playlist.dao.PlaylistRemoteDAO
import com.dew.aihua.repository.database.playlist.dao.PlaylistStreamDAO
import com.dew.aihua.repository.database.playlist.model.PlaylistEntity
import com.dew.aihua.repository.database.playlist.model.PlaylistRemoteEntity
import com.dew.aihua.repository.database.playlist.model.PlaylistStreamEntity
import com.dew.aihua.repository.database.stream.dao.StreamDAO
import com.dew.aihua.repository.database.stream.dao.StreamStateDAO
import com.dew.aihua.repository.database.stream.model.StreamEntity
import com.dew.aihua.repository.database.stream.model.StreamStateEntity
import com.dew.aihua.repository.database.subscription.SubscriptionDAO
import com.dew.aihua.repository.database.subscription.SubscriptionEntity

/**
 *  Created by Edward on 2/23/2019.
 */


@TypeConverters(Converters::class)
@Database(entities = [
    SubscriptionEntity::class,
    SearchHistoryEntry::class,
    StreamEntity::class,
    StreamHistoryEntity::class,
    StreamStateEntity::class,
    PlaylistEntity::class,
    PlaylistStreamEntity::class,
    PlaylistRemoteEntity::class,
    MissionEntity::class
],
//        version = DB_VER_12_0,
    version = 1,
    exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun subscriptionDAO(): SubscriptionDAO

    abstract fun searchHistoryDAO(): SearchHistoryDAO

    abstract fun streamDAO(): StreamDAO

    abstract fun streamHistoryDAO(): StreamHistoryDAO

    abstract fun streamStateDAO(): StreamStateDAO

    abstract fun playlistDAO(): PlaylistDAO

    abstract fun playlistStreamDAO(): PlaylistStreamDAO

    abstract fun playlistRemoteDAO(): PlaylistRemoteDAO

    abstract fun downloadDAO(): DownloadDAO

    companion object {

        const val DATABASE_NAME = "newpipe.db"

        // make this class a singleton
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // factory method
        fun getDatabase(context: Context): AppDatabase =
            INSTANCE ?: synchronized(AppDatabase::class.java) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
