package com.dew.aihua.player.playqueque.holder

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.dew.aihua.R

/**
 *  Created by Edward on 3/2/2019.
 */

class PlayQueueItemHolder(view: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {

    val itemVideoTitleView: TextView = view.findViewById(R.id.itemVideoTitleView)
    val itemDurationView: TextView = view.findViewById(R.id.itemDurationView)
    val itemAdditionalDetailsView: TextView = view.findViewById(R.id.itemAdditionalDetails)
    val itemSelected: ImageView = view.findViewById(R.id.itemSelected)
    val itemThumbnailView: ImageView = view.findViewById(R.id.itemThumbnailView)
    val itemHandle: ImageView = view.findViewById(R.id.itemHandle)

    val itemRoot: View = view.findViewById(R.id.itemRoot)

}
