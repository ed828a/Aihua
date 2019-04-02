package com.dew.aihua.ui.holder

import android.view.ViewGroup
import android.widget.TextView
import com.dew.aihua.R
import com.dew.aihua.ui.infolist.adapter.InfoItemBuilder
import com.dew.aihua.player.helper.Localization
import com.dew.aihua.ui.builder.NewInfoItemBuilder
import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.channel.ChannelInfoItem

/**
 *  Created by Edward on 3/2/2019.
 */
class NewChannelInfoItemHolder(infoItemBuilder: NewInfoItemBuilder, parent: ViewGroup) : NewChannelMiniInfoItemHolder(infoItemBuilder, R.layout.list_channel_item_cardview, parent) {
    private val itemChannelDescriptionView: TextView = itemView.findViewById(R.id.itemChannelDescriptionView)

    override fun updateFromItem(infoItem: InfoItem) {
        super.updateFromItem(infoItem)

        if (infoItem !is ChannelInfoItem) return

        itemChannelDescriptionView.text = infoItem.description
    }

    override fun getDetailLine(item: ChannelInfoItem): String {
        var details = super.getDetailLine(item)

        if (item.streamCount >= 0) {
            val formattedVideoAmount = Localization.localizeStreamCount(itemBuilder.context, item.streamCount)

            if (!details.isEmpty()) {
                details += " • $formattedVideoAmount"
            } else {
                details = formattedVideoAmount
            }
        }
        return details
    }
}
