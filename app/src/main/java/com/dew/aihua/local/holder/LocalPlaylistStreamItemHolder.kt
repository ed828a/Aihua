package com.dew.aihua.local.holder

import android.annotation.SuppressLint
import android.text.TextUtils
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.dew.aihua.R
import com.dew.aihua.database.LocalItem
import com.dew.aihua.database.playlist.model.PlaylistStreamEntry
import com.dew.aihua.local.adapter.LocalItemBuilder
import com.dew.aihua.player.helper.ImageDisplayConstants
import com.dew.aihua.player.helper.Localization
import com.dew.aihua.report.ErrorActivity
import com.dew.aihua.util.NavigationHelper
import com.nostra13.universalimageloader.core.ImageLoader
import de.hdodenhof.circleimageview.CircleImageView
import org.schabi.newpipe.extractor.NewPipe
import java.text.DateFormat

/**
 *  Created by Edward on 3/2/2019.
 */

open class LocalPlaylistStreamItemHolder internal constructor(
    infoItemBuilder: LocalItemBuilder,
    layoutId: Int, parent: ViewGroup
) : LocalItemHolder(infoItemBuilder, layoutId, parent) {

    val itemThumbnailView: ImageView = itemView.findViewById(R.id.itemThumbnailView)
    val itemVideoTitleView: TextView = itemView.findViewById(R.id.itemVideoTitleView)
    val itemAdditionalDetailsView: TextView = itemView.findViewById(R.id.itemAdditionalDetails)
    val itemDurationView: TextView = itemView.findViewById(R.id.itemDurationView)
    val itemHandleView: View = itemView.findViewById(R.id.itemHandle)
    val itemUploaderThumbnail: CircleImageView = itemView.findViewById(R.id.detail_uploader_thumbnail_view)

    constructor(infoItemBuilder: LocalItemBuilder, parent: ViewGroup) : this(
        infoItemBuilder,
        R.layout.list_stream_playlist_item,
        parent
    )

    @SuppressLint("ClickableViewAccessibility")
    override fun updateFromItem(item: LocalItem, dateFormat: DateFormat) {
        if (item !is PlaylistStreamEntry) return

        itemVideoTitleView.text = item.title
        itemAdditionalDetailsView.text =
            Localization.concatenateStrings(item.uploader, NewPipe.getNameOfService(item.serviceId))

        if (!TextUtils.isEmpty(item.thumbnailUrl)) {
            itemBuilder.displayImage(
                item.thumbnailUrl, itemUploaderThumbnail,
                ImageDisplayConstants.DISPLAY_AVATAR_OPTIONS
            )
        }

        if (item.duration > 0) {
            itemDurationView.text = Localization.getDurationString(item.duration)
            itemDurationView.setBackgroundColor(
                ContextCompat.getColor(
                    itemBuilder.context!!,
                    R.color.duration_background_color
                )
            )
            itemDurationView.visibility = View.VISIBLE
        } else {
            itemDurationView.visibility = View.GONE
        }

        // Default thumbnail is shown on error, while loading and if the url is empty
        itemBuilder.displayImage(item.thumbnailUrl, itemThumbnailView, ImageDisplayConstants.DISPLAY_THUMBNAIL_OPTIONS)

        itemView.setOnClickListener {

            itemBuilder.onItemSelectedListener?.selected(item)
        }
//        itemThumbnailView.setOnClickListener { itemBuilder.onItemSelectedListener?.selected(item) }
//        itemVideoTitleView.setOnClickListener { itemBuilder.onItemSelectedListener?.selected(item) }

        itemView.isLongClickable = true
        itemView.setOnLongClickListener {
            itemBuilder.onItemSelectedListener?.held(item)
            true
        }

        itemThumbnailView.setOnTouchListener(getOnTouchListener(item))
        itemHandleView.setOnTouchListener(getOnTouchListener(item))
    }

    private fun getOnTouchListener(item: PlaylistStreamEntry): View.OnTouchListener =
        View.OnTouchListener { view, motionEvent ->
            view.performClick()
            if (motionEvent.actionMasked == MotionEvent.ACTION_DOWN) {
                itemBuilder.onItemSelectedListener?.drag(item, this@LocalPlaylistStreamItemHolder)
            }
            false
        }

    companion object {
        private const val TAG = ""
    }
}
