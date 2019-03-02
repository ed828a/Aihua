package com.dew.aihua.infolist.holder

import android.view.LayoutInflater
import android.view.ViewGroup
import com.dew.aihua.infolist.adapter.InfoItemBuilder
import org.schabi.newpipe.extractor.InfoItem

/**
 *  Created by Edward on 3/2/2019.
 */

abstract class InfoItemHolder(
    protected val itemBuilder: InfoItemBuilder,
    layoutId: Int,
    parent: ViewGroup
) : androidx.recyclerview.widget.RecyclerView.ViewHolder(LayoutInflater.from(itemBuilder.context).inflate(layoutId, parent, false)) {

    abstract fun updateFromItem(infoItem: InfoItem)


}
