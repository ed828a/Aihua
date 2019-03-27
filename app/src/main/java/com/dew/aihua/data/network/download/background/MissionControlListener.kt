package com.dew.aihua.data.network.download.background

import android.os.Handler
import java.util.HashMap

/**
 *  Created by Edward on 3/2/2019.
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