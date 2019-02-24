package com.dew.aihua.local.holder

import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.dew.aihua.R
import com.dew.aihua.local.adapter.LocalItemBuilder
import com.dew.aihua.repository.database.LocalItem
import java.text.DateFormat

/**
 *  Created by Edward on 2/23/2019.
 */


abstract class PlaylistItemHolder(infoItemBuilder: LocalItemBuilder,
                                  layoutId: Int,
                                  parent: ViewGroup
) : LocalItemHolder(infoItemBuilder, layoutId, parent) {

    val itemThumbnailView: ImageView = itemView.findViewById(R.id.itemThumbnailView)
    val itemTitleView: TextView = itemView.findViewById(R.id.itemTitleView)
    val itemUploaderView: TextView = itemView.findViewById(R.id.itemUploaderView)
    val itemStreamCountView: TextView = itemView.findViewById(R.id.itemStreamCountView)

    constructor(infoItemBuilder: LocalItemBuilder, parent: ViewGroup) : this(infoItemBuilder, R.layout.list_playlist_item, parent)

    override fun updateFromItem(item: LocalItem, dateFormat: DateFormat) {
        itemView.setOnClickListener { view ->
            itemBuilder.onItemSelectedListener?.selected(item)
        }

        itemView.isLongClickable = true
        itemView.setOnLongClickListener { view ->
            itemBuilder.onItemSelectedListener?.held(item)
            true
        }
    }
}
