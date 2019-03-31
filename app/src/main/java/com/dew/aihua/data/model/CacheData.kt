package com.dew.aihua.data.model

import org.schabi.newpipe.extractor.Info

class CacheData(val info: Info, timeoutMillis: Long) {
    private val expireTimestamp: Long = System.currentTimeMillis() + timeoutMillis

    val isExpired: Boolean
        get() = System.currentTimeMillis() > expireTimestamp

}
