package com.dew.aihua.ui.local.holder

import android.view.LayoutInflater
import android.view.ViewGroup
import com.dew.aihua.ui.local.adapter.LocalItemBuilder
import com.dew.aihua.data.local.database.LocalItem
import java.text.DateFormat


/**
 *  Created by Edward on 3/2/2019.
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
