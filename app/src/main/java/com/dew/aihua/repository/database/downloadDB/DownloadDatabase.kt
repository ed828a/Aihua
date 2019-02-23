package com.dew.aihua.repository.database.downloadDB

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 *  Created by Edward on 2/23/2019.
 */

@Database(entities = [MissionEntity::class], version = 1, exportSchema = false)
abstract class DownloadDatabase : RoomDatabase(){

    abstract fun downloadDao(): DownloadDAO

    companion object {

        // make this class a singleton
        @Volatile
        private var INSTANCE: DownloadDatabase? = null

        // factory method
        fun getDatabase(context: Context): DownloadDatabase =
            INSTANCE ?: synchronized(DownloadDatabase::class.java) {
                INSTANCE ?: Room
                    .databaseBuilder(context.applicationContext,
                        DownloadDatabase::class.java,
                        "download_database.db")
                    .build()
                    .also { INSTANCE = it }
            }

    }
}