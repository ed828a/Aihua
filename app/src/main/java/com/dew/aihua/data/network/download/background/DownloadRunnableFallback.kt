package com.dew.aihua.data.network.download.background

import android.util.Log
import java.io.BufferedInputStream
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL

/**
 *  Created by Edward on 3/2/2019.
 */


class DownloadRunnableFallback(private val missionControl: MissionControl) : Runnable {

    override fun run() {
        Log.d(
            TAG,
            "DownloadFallback run() called, file located: ${missionControl.mission.location}/${missionControl.mission.name}, Thread name: ${Thread.currentThread().name}"
        )
        try {
            val url = URL(missionControl.mission.url)
            val conn = url.openConnection() as HttpURLConnection

            if (conn.responseCode != 200 && conn.responseCode != 206) {
                notifyError(MissionControl.ERROR_SERVER_UNSUPPORTED)
            } else {
                val file = RandomAccessFile("${missionControl.mission.location}/${missionControl.mission.name}", "rw")
                file.seek(0)
                val bufferedInputStream = BufferedInputStream(conn.inputStream)
                val buf = ByteArray(512)
                var len = bufferedInputStream.read(buf, 0, 512)
                while (len != -1 && missionControl.running) {
                    file.write(buf, 0, len)
                    notifyProgress(len.toLong())
                    len = bufferedInputStream.read(buf, 0, 512)

                    if (Thread.interrupted()) {
                        break
                    }
                }

                file.close()
                bufferedInputStream.close()
            }
        } catch (e: Exception) {
            notifyError(MissionControl.ERROR_UNKNOWN)
        }

        if (missionControl.errCode == -1 && missionControl.running) {
            notifyFinished()
        }
    }

    private fun notifyProgress(len: Long) {
        synchronized(missionControl) {
            missionControl.notifyProgress(len)
        }
    }

    private fun notifyError(err: Int) {
        synchronized(missionControl) {
            missionControl.notifyError(err)
            missionControl.pause()
        }
    }

    private fun notifyFinished() {
        synchronized(missionControl) {
            missionControl.notifyFinished()
        }
    }

    companion object {
        private val TAG = DownloadRunnableFallback::class.java.simpleName
    }
}
