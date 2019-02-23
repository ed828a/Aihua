package com.dew.aihua.info_list.holder

import android.content.Intent
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.dew.aihua.R
import com.dew.aihua.info_list.adapter.InfoItemBuilder
import com.dew.aihua.player.playerUI.PopupVideoPlayer.Companion.ACTION_CLOSE
import com.dew.aihua.ui.fragment.VideoDetailFragment
import com.dew.aihua.util.ImageDisplayConstants
import com.dew.aihua.util.Localization
import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.stream.StreamType

/**
 *  Created by Edward on 2/23/2019.
 */

open class StreamMiniInfoItemHolder (infoItemBuilder: InfoItemBuilder, layoutId: Int, parent: ViewGroup) : InfoItemHolder(infoItemBuilder, layoutId, parent) {

    val itemThumbnailView: ImageView = itemView.findViewById(R.id.itemThumbnailView)
    val itemVideoTitleView: TextView = itemView.findViewById(R.id.itemVideoTitleView)
    val itemUploaderView: TextView = itemView.findViewById(R.id.itemUploaderView)
    val itemDurationView: TextView = itemView.findViewById(R.id.itemDurationView)

    constructor(infoItemBuilder: InfoItemBuilder, parent: ViewGroup) : this(infoItemBuilder, R.layout.list_stream_mini_item, parent) {}

    override fun updateFromItem(infoItem: InfoItem) {
        if (infoItem !is StreamInfoItem) return
        Log.d(TAG, "updateFromItem() called: infoItem = $infoItem")

        itemVideoTitleView.text = infoItem.name
        itemUploaderView.text = infoItem.uploaderName

        when {
            infoItem.duration > 0 -> {
                itemDurationView.text = Localization.getDurationString(infoItem.duration)
                itemDurationView.setBackgroundColor(
                    ContextCompat.getColor(itemBuilder.context,
                        R.color.duration_background_color))
                itemDurationView.visibility = View.VISIBLE
            }
            infoItem.streamType == StreamType.LIVE_STREAM -> {
                itemDurationView.setText(R.string.duration_live)
                itemDurationView.setBackgroundColor(
                    ContextCompat.getColor(itemBuilder.context,
                        R.color.live_duration_background_color))
                itemDurationView.visibility = View.VISIBLE
            }
            else -> itemDurationView.visibility = View.GONE
        }

        // Default thumbnail is shown on error, while loading and if the url is empty
        itemBuilder.imageLoader
            .displayImage(infoItem.thumbnailUrl,
                itemThumbnailView,
                ImageDisplayConstants.DISPLAY_THUMBNAIL_OPTIONS)

        itemView.setOnClickListener { view ->
            val fragment = (view.context as androidx.fragment.app.FragmentActivity).supportFragmentManager.findFragmentById(R.id.fragment_holder)
            if (fragment is VideoDetailFragment){
                Log.d(TAG, "itemView onClick(): fragment is  VideoDetailFragment")
            } else {
                view.context.applicationContext.sendBroadcast(Intent(ACTION_CLOSE))
            }

            Log.d(TAG, "itemView onClicked() called: itemBuilder.onStreamSelectedListener  = ${itemBuilder.onStreamSelectedListener }")
            if (itemBuilder.onStreamSelectedListener != null) {
                itemBuilder.onStreamSelectedListener?.selected(infoItem)
            }
        }

        when (infoItem.streamType) {
            StreamType.AUDIO_STREAM, StreamType.VIDEO_STREAM, StreamType.LIVE_STREAM, StreamType.AUDIO_LIVE_STREAM -> enableLongClick(infoItem)
            StreamType.FILE, StreamType.NONE -> disableLongClick()
            else -> disableLongClick()
        }
    }

    private fun enableLongClick(item: StreamInfoItem) {
        itemView.isLongClickable = true
        itemView.setOnLongClickListener { view ->
            if (itemBuilder.onStreamSelectedListener != null) {
                itemBuilder.onStreamSelectedListener?.held(item)
            }
            true
        }
    }

    private fun disableLongClick() {
        itemView.isLongClickable = false
        itemView.setOnLongClickListener(null)
    }


    companion object {
        const val TAG = "InfoItemHolder"
    }

}
