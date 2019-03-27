package com.dew.aihua.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.dew.aihua.data.local.database.downloadDB.DownloadDAO
import com.dew.aihua.data.local.database.downloadDB.MissionEntity
import com.dew.aihua.data.local.database.history.dao.SearchHistoryDAO
import com.dew.aihua.data.local.database.history.dao.StreamHistoryDAO
import com.dew.aihua.data.local.database.history.model.SearchHistoryEntry
import com.dew.aihua.data.local.database.history.model.StreamHistoryEntity
import com.dew.aihua.data.local.database.playlist.dao.PlaylistDAO
import com.dew.aihua.data.local.database.playlist.dao.PlaylistRemoteDAO
import com.dew.aihua.data.local.database.playlist.dao.PlaylistStreamDAO
import com.dew.aihua.data.local.database.playlist.model.PlaylistEntity
import com.dew.aihua.data.local.database.playlist.model.PlaylistRemoteEntity
import com.dew.aihua.data.local.database.playlist.model.PlaylistStreamEntity
import com.dew.aihua.data.local.database.stream.dao.StreamDAO
import com.dew.aihua.data.local.database.stream.dao.StreamStateDAO
import com.dew.aihua.data.local.database.stream.model.StreamEntity
import com.dew.aihua.data.local.database.stream.model.StreamStateEntity
import com.dew.aihua.data.local.database.subscription.SubscriptionDAO
import com.dew.aihua.data.local.database.subscription.SubscriptionEntity


/**
 *  Created by Edward on 3/2/2019.
 */

@TypeConverters(Converters::class)
@Database(
    entities = [
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
    version = 1,
    exportSchema = false
)
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

        const val DATABASE_NAME = "aihua.db"

        // in case Database changes later
//        private val MIGRATION_1_2: Migration = object : Migration(1, 2) {
//            override fun migrate(database: SupportSQLiteDatabase) {
//                // Since we didn't alter the table, there's nothing else to do here.
//            }
//        }


        // make this class a singleton
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // factory method
        fun getDatabase(context: Context): AppDatabase =
            INSTANCE ?: synchronized(AppDatabase::class.java) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
//                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
