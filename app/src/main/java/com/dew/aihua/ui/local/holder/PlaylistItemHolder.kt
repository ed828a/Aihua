package com.dew.aihua.ui.local.holder

import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.dew.aihua.R
import com.dew.aihua.data.local.database.LocalItem
import com.dew.aihua.ui.local.adapter.LocalItemBuilder
import de.hdodenhof.circleimageview.CircleImageView
import java.text.DateFormat

/**
 *  Created by Edward on 3/2/2019.
 */
abstract class PlaylistItemHolder(infoItemBuilder: LocalItemBuilder,
                                  layoutId: Int,
                                  parent: ViewGroup
) : LocalItemHolder(infoItemBuilder, layoutId, parent) {

    val itemThumbnailView: ImageView = itemView.findViewById(R.id.itemThumbnailView)
    val itemTitleView: TextView = itemView.findViewById(R.id.itemTitleView)
    val itemUploaderView: TextView = itemView.findViewById(R.id.itemUploaderView)
    val itemStreamCountView: TextView = itemView.findViewById(R.id.itemStreamCountView)
    val itemUploaderThumbnail: CircleImageView? = itemView.findViewById(R.id.detail_uploader_thumbnail_view)

    constructor(infoItemBuilder: LocalItemBuilder, parent: ViewGroup) : this(infoItemBuilder, R.layout.list_playlist_item, parent)

    override fun updateFromItem(item: LocalItem, dateFormat: DateFormat) {
        itemView.setOnClickListener {
            itemBuilder.onItemSelectedListener?.selected(item)
        }

        itemView.isLongClickable = true
        itemView.setOnLongClickListener {
            itemBuilder.onItemSelectedListener?.held(item)
            true
        }
    }
}
