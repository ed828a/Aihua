package com.dew.aihua.data.local.database.playlist

import com.dew.aihua.data.local.database.LocalItem

/**
 *  Created by Edward on 3/2/2019.
 */
interface PlaylistLocalItem : LocalItem {
    fun getOrderingName(): String?
}
