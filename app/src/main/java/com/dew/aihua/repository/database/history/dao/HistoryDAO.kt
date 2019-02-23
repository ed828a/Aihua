package com.dew.aihua.repository.database.history.dao

import com.dew.aihua.repository.database.BasicDAO

/**
 *  Created by Edward on 2/23/2019.
 */


interface HistoryDAO<T> : BasicDAO<T> {
    fun getLatestEntry(): T?
}
