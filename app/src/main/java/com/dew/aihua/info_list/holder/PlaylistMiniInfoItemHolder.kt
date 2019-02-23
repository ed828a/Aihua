package com.dew.aihua.info_list.holder

import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.dew.aihua.R
import com.dew.aihua.info_list.adapter.InfoItemBuilder
import com.dew.aihua.util.ImageDisplayConstants
import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItem

/**
 *  Created by Edward on 2/23/2019.
 */

open class PlaylistMiniInfoItemHolder(infoItemBuilder: InfoItemBuilder, layoutId: Int, parent: ViewGroup) : InfoItemHolder(infoItemBuilder, layoutId, parent) {
    val itemThumbnailView: ImageView = itemView.findViewById(R.id.itemThumbnailView)
    val itemTitleView: TextView = itemView.findViewById(R.id.itemTitleView)
    val itemUploaderView: TextView = itemView.findViewById(R.id.itemUploaderView)
    val itemStreamCountView: TextView = itemView.findViewById(R.id.itemStreamCountView)

    constructor(infoItemBuilder: InfoItemBuilder, parent: ViewGroup) : this(infoItemBuilder, R.layout.list_playlist_mini_item, parent)

    override fun updateFromItem(infoItem: InfoItem) {
        if (infoItem !is PlaylistInfoItem) return

        itemTitleView.text = infoItem.name
        itemStreamCountView.text = infoItem.streamCount.toString()
        itemUploaderView.text = infoItem.uploaderName

        itemBuilder.imageLoader
            .displayImage(infoItem.thumbnailUrl, itemThumbnailView, ImageDisplayConstants.DISPLAY_THUMBNAIL_OPTIONS)

        itemView.setOnClickListener { view ->
            itemBuilder.onPlaylistSelectedListener?.selected(infoItem)
        }

        itemView.isLongClickable = true
        itemView.setOnLongClickListener { view ->
            itemBuilder.onPlaylistSelectedListener?.held(infoItem)
            true
        }
    }
}
