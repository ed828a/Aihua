package com.dew.aihua.repository.database.playlist

import com.dew.aihua.repository.database.LocalItem

/**
 *  Created by Edward on 2/23/2019.
 */

interface PlaylistLocalItem : LocalItem {
    fun getOrderingName(): String?
}
