package com.dew.aihua.data.local.database.history.dao

import com.dew.aihua.data.local.database.BasicDAO

/**
 *  Created by Edward on 3/2/2019.
 */
interface HistoryDAO<T> : BasicDAO<T> {
    fun getLatestEntry(): T?
}
