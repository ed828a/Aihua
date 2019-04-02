package com.dew.aihua.ui.holder

import android.view.ViewGroup
import android.widget.TextView
import com.dew.aihua.R
import com.dew.aihua.player.helper.ImageDisplayConstants
import com.dew.aihua.player.helper.Localization
import com.dew.aihua.ui.builder.NewInfoItemBuilder
import de.hdodenhof.circleimageview.CircleImageView
import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.channel.ChannelInfoItem

/**
 *  Created by Edward on 3/2/2019.
 */
open class NewChannelMiniInfoItemHolder(
    infoItemBuilder: NewInfoItemBuilder,
    layoutId: Int,
    parent: ViewGroup
) : NewInfoItemHolder(infoItemBuilder, layoutId, parent) {
    val itemThumbnailView: CircleImageView = itemView.findViewById(R.id.itemThumbnailView)
    val itemTitleView: TextView = itemView.findViewById(R.id.itemTitleView)
    private val itemAdditionalDetailView: TextView = itemView.findViewById(R.id.itemAdditionalDetails)

    constructor(infoItemBuilder: NewInfoItemBuilder, parent: ViewGroup) : this(
        infoItemBuilder,
        R.layout.list_channel_mini_item,
        parent
    )

    // bindToView
    override fun updateFromItem(infoItem: InfoItem) {
        if (infoItem !is ChannelInfoItem) return

        itemTitleView.text = infoItem.name
        itemAdditionalDetailView.text = getDetailLine(infoItem)

        itemBuilder.displayImage(
            infoItem.thumbnailUrl,
            itemThumbnailView,
            ImageDisplayConstants.DISPLAY_THUMBNAIL_OPTIONS
        )

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
            details += Localization.shortSubscriberCount(
                itemBuilder.context,
                item.subscriberCount
            )
        }
        return details
    }
}
