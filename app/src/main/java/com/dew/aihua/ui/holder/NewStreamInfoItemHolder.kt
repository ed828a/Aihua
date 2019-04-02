package com.dew.aihua.ui.holder

import android.text.TextUtils
import android.view.ViewGroup
import android.widget.TextView
import com.dew.aihua.R
import com.dew.aihua.player.helper.Localization
import com.dew.aihua.ui.builder.NewInfoItemBuilder
import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.stream.StreamInfoItem


/**
 *  Created by Edward on 3/2/2019.
 */
class NewStreamInfoItemHolder(infoItemBuilder: NewInfoItemBuilder, parent: ViewGroup
) : NewStreamMiniInfoItemHolder(infoItemBuilder, R.layout.list_stream_grid_item_cardview, parent) {

    val itemAdditionalDetails: TextView = itemView.findViewById(R.id.itemAdditionalDetails)

    override fun updateFromItem(infoItem: InfoItem) {
        super.updateFromItem(infoItem)

        if (infoItem !is StreamInfoItem) return

        itemAdditionalDetails.text = getStreamInfoDetailLine(infoItem)
    }

    private fun getStreamInfoDetailLine(infoItem: StreamInfoItem): String {
        var viewsAndDate = ""
        if (infoItem.viewCount >= 0) {
            viewsAndDate = Localization.shortViewCount(itemBuilder.context, infoItem.viewCount)
        }
        if (!TextUtils.isEmpty(infoItem.uploadDate)) {
            if (viewsAndDate.isEmpty()) {
                viewsAndDate = infoItem.uploadDate
            } else {
                viewsAndDate += " • " + infoItem.uploadDate
            }
        }
        return viewsAndDate
    }


}
