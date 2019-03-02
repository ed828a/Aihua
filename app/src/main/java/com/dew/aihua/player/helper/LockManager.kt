package com.dew.aihua.player.helper

import android.content.Context
import android.net.wifi.WifiManager
import android.os.PowerManager
import android.util.Log

/**
 *  Created by Edward on 3/2/2019.
 */
class LockManager(context: Context) {
    private val TAG = "LockManager@" + hashCode()

    private val powerManager: PowerManager = context.applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
    private val wifiManager: WifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null

    fun acquireWifiAndCpu() {
        Log.d(TAG, "acquireWifiAndCpu() called")
        if (wakeLock != null && wakeLock!!.isHeld && wifiLock != null && wifiLock!!.isHeld) return

        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG)
        wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL, TAG)

        if (wakeLock != null) wakeLock?.acquire(2 * 60 * 60 * 1000) // 2 hours
        if (wifiLock != null) wifiLock!!.acquire()
    }

    fun releaseWifiAndCpu() {
        Log.d(TAG, "releaseWifiAndCpu() called")
        if (wakeLock != null && wakeLock!!.isHeld) wakeLock!!.release()
        if (wifiLock != null && wifiLock!!.isHeld) wifiLock!!.release()

        wakeLock = null
        wifiLock = null
    }
}
