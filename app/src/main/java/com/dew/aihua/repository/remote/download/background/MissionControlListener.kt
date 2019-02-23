package com.dew.aihua.repository.remote.download.background

import android.os.Handler
import java.util.HashMap

/**
 *  Created by Edward on 2/23/2019.
 *
 *  DownloadMissionManagerImpl, DownloadManagerService, AllMissionsFragment use this listener.
 * all code concreting this listener will run on main thread.
 */

interface MissionControlListener {
    /**
     * @param done: the progress
     * @param total: the file length
     */
    fun onProgressUpdate(missionControl: MissionControl, done: Long, total: Long)

    fun onFinish(missionControl: MissionControl)

    fun onError(missionControl: MissionControl, errCode: Int)

    companion object {
        // to store Handler associated with UI thread
        val handlerStore = HashMap<MissionControlListener, Handler>()
    }
}