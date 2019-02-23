package com.dew.aihua.info_list.holder

import android.view.LayoutInflater
import android.view.ViewGroup
import com.dew.aihua.info_list.adapter.InfoItemBuilder
import org.schabi.newpipe.extractor.InfoItem

/**
 *  Created by Edward on 2/23/2019.
 */

abstract class InfoItemHolder(
    protected val itemBuilder: InfoItemBuilder,
    layoutId: Int,
    parent: ViewGroup
) : androidx.recyclerview.widget.RecyclerView.ViewHolder(LayoutInflater.from(itemBuilder.context).inflate(layoutId, parent, false)) {

    abstract fun updateFromItem(infoItem: InfoItem)


}
