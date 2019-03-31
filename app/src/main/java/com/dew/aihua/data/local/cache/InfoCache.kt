package com.dew.aihua.data.local.cache

import android.util.Log
import android.util.LruCache
import com.dew.aihua.data.model.CacheData
import com.dew.aihua.player.helper.ServiceHelper
import org.schabi.newpipe.extractor.Info

/**
 *  Created by Edward on 3/2/2019.
 */

object InfoCache {
    private const val TAG = "InfoCache"

    private const val MAX_ITEMS_ON_CACHE = 60
    /**
     * Trim the cache to this size
     */
    private const val TRIM_CACHE_TO = 30

    private val lruCache = LruCache<String, CacheData>(MAX_ITEMS_ON_CACHE)

    val size: Long
        get() = synchronized(lruCache) {
            return lruCache.size().toLong()
        }

    fun getFromKey(serviceId: Int, url: String): Info? {
        Log.d(TAG, "getFromKey() called with: serviceId = [$serviceId], url = [$url]")
        synchronized(lruCache) {
            return getInfo(
                keyOf(
                    serviceId,
                    url
                )
            )
        }
    }

    fun putInfo(serviceId: Int, url: String, info: Info) {
        Log.d(TAG, "putInfo() called with: info = [$info]")

        val expirationMillis = ServiceHelper.getCacheExpirationMillis(info.serviceId)
        synchronized(lruCache) {
            val data = CacheData(info, expirationMillis)
            lruCache.put(
                keyOf(
                    serviceId,
                    url
                ), data)
        }
    }

    fun removeInfo(serviceId: Int, url: String) {
        Log.d(TAG, "removeInfo() called with: serviceId = [$serviceId], url = [$url]")
        synchronized(lruCache) {
            lruCache.remove(
                keyOf(
                    serviceId,
                    url
                )
            )
        }
    }

    fun clearCache() {
        Log.d(TAG, "clearCache() called")
        synchronized(lruCache) {
            lruCache.evictAll()
        }
    }

    fun trimCache() {
        Log.d(TAG, "trimCache() called")
        synchronized(lruCache) {
            removeStaleCache()
            lruCache.trimToSize(TRIM_CACHE_TO)
        }
    }

//    private class CacheData(val info: Info, timeoutMillis: Long) {
//        private val expireTimestamp: Long = System.currentTimeMillis() + timeoutMillis
//
//        val isExpired: Boolean
//            get() = System.currentTimeMillis() > expireTimestamp
//
//    }




    private fun keyOf(serviceId: Int, url: String): String {
        return serviceId.toString() + url
    }

    private fun removeStaleCache() {
        for ((key, data) in lruCache.snapshot()) {
            if (data != null && data.isExpired) {
                lruCache.remove(key)
            }
        }
    }

    private fun getInfo(key: String): Info? {
        val data = lruCache.get(key) ?: return null

        if (data.isExpired) {
            lruCache.remove(key)
            return null
        }

        return data.info
    }


}