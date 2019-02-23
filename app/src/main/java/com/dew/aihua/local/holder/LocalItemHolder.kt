package com.dew.aihua.local.holder

import android.view.LayoutInflater
import android.view.ViewGroup
import com.dew.aihua.local.adapter.LocalItemBuilder
import com.dew.aihua.repository.database.LocalItem
import java.text.DateFormat

/**
 *  Created by Edward on 2/23/2019.
 */


abstract class LocalItemHolder(
    protected val itemBuilder: LocalItemBuilder,
    layoutId: Int,
    parent: ViewGroup
)
    : androidx.recyclerview.widget.RecyclerView.ViewHolder(
    LayoutInflater.from(itemBuilder.context).inflate(layoutId, parent, false)
) {
    // bind-To-View
    abstract fun updateFromItem(item: LocalItem, dateFormat: DateFormat)
}
