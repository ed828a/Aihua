package com.dew.aihua.database

/**
 *  Created by Edward on 3/2/2019.
 */
interface LocalItem {

    val localItemType: LocalItemType

    enum class LocalItemType {
        PLAYLIST_LOCAL_ITEM,
        PLAYLIST_REMOTE_ITEM,

        PLAYLIST_STREAM_ITEM,
        STATISTIC_STREAM_ITEM
    }
}
