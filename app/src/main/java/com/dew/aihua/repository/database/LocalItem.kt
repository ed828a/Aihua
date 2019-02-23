package com.dew.aihua.repository.database

/**
 *  Created by Edward on 2/23/2019.
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
