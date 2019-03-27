package com.dew.aihua.data.network.download.background

import android.content.Context
import com.dew.aihua.data.local.database.AppDatabase
import com.dew.aihua.data.local.database.downloadDB.DownloadDAO
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers

/**
 *  Created by Edward on 3/2/2019.
 */

class DownloadMissionDataSourceImpl(val context: Context) : DownloadMissionDataSource {

    //    private val downloadDb: AppDatabase = NewPipeDatabase.getInstance(context)
    private val downloadDb: AppDatabase = AppDatabase.getDatabase(context)
    /**
     * Retrieve an instance getTabFrom the database, used for tests.
     */
    fun getDatabase(): AppDatabase {
        return downloadDb
    }

    override fun loadMissions(): Flowable<List<MissionControl>> {
        return Flowable.defer {
            Flowable.just(
                downloadDb.downloadDAO().loadMissions()
                    .map {
                        MissionControl(it)
                    }).subscribeOn(Schedulers.io())
        }
    }

    override fun addMission(missionControl: MissionControl): Completable {
        return Completable.defer {
            downloadDb.downloadDAO().addMission(missionControl.mission)
            Completable.complete()
                .subscribeOn(Schedulers.io())
        }
    }

    override fun updateMission(missionControl: MissionControl): Completable {
        return Completable.defer {
            DownloadDAO.updateMission(missionControl.mission, downloadDb.downloadDAO())
            Completable.complete()
                .subscribeOn(Schedulers.io())
        }
    }

    override fun deleteMission(missionControl: MissionControl): Completable {
        return Completable.defer {
            downloadDb.downloadDAO().deleteMission(missionControl.mission)
            Completable.complete()
                .subscribeOn(Schedulers.io())
        }
    }

    override fun isCached(): Single<Boolean> {
        return Single.defer {
            Single.just(downloadDb.downloadDAO().loadMissions().isNotEmpty())
                .subscribeOn(Schedulers.io())
        }
    }

}