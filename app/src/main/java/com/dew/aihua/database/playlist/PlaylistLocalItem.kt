package com.dew.aihua.database.playlist

import com.dew.aihua.database.LocalItem

/**
 *  Created by Edward on 3/2/2019.
 */
interface PlaylistLocalItem : LocalItem {
    fun getOrderingName(): String?
}
