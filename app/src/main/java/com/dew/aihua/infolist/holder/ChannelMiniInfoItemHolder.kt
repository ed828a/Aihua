package com.dew.aihua.infolist.holder

import android.view.ViewGroup
import android.widget.TextView
import com.dew.aihua.R
import com.dew.aihua.infolist.adapter.InfoItemBuilder
import com.dew.aihua.player.helper.ImageDisplayConstants
import com.dew.aihua.player.helper.Localization
import de.hdodenhof.circleimageview.CircleImageView
import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.channel.ChannelInfoItem

/**
 *  Created by Edward on 3/2/2019.
 */
open class ChannelMiniInfoItemHolder(infoItemBuilder: InfoItemBuilder,
                                     layoutId: Int,
                                     parent: ViewGroup
) : InfoItemHolder(infoItemBuilder, layoutId, parent) {
    val itemThumbnailView: CircleImageView = itemView.findViewById(R.id.itemThumbnailView)
    val itemTitleView: TextView = itemView.findViewById(R.id.itemTitleView)
    private val itemAdditionalDetailView: TextView = itemView.findViewById(R.id.itemAdditionalDetails)

    constructor(infoItemBuilder: InfoItemBuilder, parent: ViewGroup) : this(infoItemBuilder, R.layout.list_channel_mini_item, parent) {}

    // bindToView
    override fun updateFromItem(infoItem: InfoItem) {
        if (infoItem !is ChannelInfoItem) return

        itemTitleView.text = infoItem.name
        itemAdditionalDetailView.text = getDetailLine(infoItem)

        itemBuilder.imageLoader
            .displayImage(infoItem.thumbnailUrl,
                itemThumbnailView,
                ImageDisplayConstants.DISPLAY_THUMBNAIL_OPTIONS)

        itemView.setOnClickListener {
            if (itemBuilder.onChannelSelectedListener != null) {
                itemBuilder.onChannelSelectedListener!!.selected(infoItem)
            }
        }

        itemView.setOnLongClickListener { _ ->
            if (itemBuilder.onChannelSelectedListener != null) {
                itemBuilder.onChannelSelectedListener!!.held(infoItem)
            }
            true
        }
    }

    protected open fun getDetailLine(item: ChannelInfoItem): String {
        var details = ""
        if (item.subscriberCount >= 0) {
            details += Localization.shortSubscriberCount(itemBuilder.context,
                item.subscriberCount)
        }
        return details
    }
}
