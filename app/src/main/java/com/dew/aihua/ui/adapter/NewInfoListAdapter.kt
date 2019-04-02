package com.dew.aihua.ui.adapter

import android.app.Activity

import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dew.aihua.player.playqueque.holder.FallbackViewHolder
import com.dew.aihua.ui.builder.NewInfoItemBuilder
import com.dew.aihua.ui.holder.*
import com.dew.aihua.ui.infolist.holder.*
import com.dew.aihua.util.OnClickGesture
import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.channel.ChannelInfoItem
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItem
import org.schabi.newpipe.extractor.stream.StreamInfoItem

/**
 *  Created by Edward on 3/2/2019.
 */
class NewInfoListAdapter(activity: Activity) : GeneralListAdapter<InfoItem>() {

    private val infoItemBuilder: NewInfoItemBuilder =
        NewInfoItemBuilder(activity)

    override val itemsList: ArrayList<InfoItem> = arrayListOf()

    private var useGridVariant = false


    fun setOnStreamSelectedListener(listener: OnClickGesture<StreamInfoItem>) {
        infoItemBuilder.onStreamSelectedListener = listener
    }

    fun setOnChannelSelectedListener(listener: OnClickGesture<ChannelInfoItem>) {
        infoItemBuilder.onChannelSelectedListener = listener
    }

    fun setOnPlaylistSelectedListener(listener: OnClickGesture<PlaylistInfoItem>) {
        infoItemBuilder.onPlaylistSelectedListener = listener
    }


    fun addInfoItem(data: InfoItem?) {
        if (data != null) {
            Log.d(TAG, "addInfoItem() before > infoItemList.size() = ${itemsList.size}, thread = ${Thread.currentThread()}")

            val positionInserted = sizeConsideringHeader()
            itemsList.add(data)

            Log.d(TAG, "addInfoItem() after > position = $positionInserted, infoItemList.size() = ${itemsList.size}, header = $header, footer = $footer, showFooter = $showFooter")
            notifyItemInserted(positionInserted)

            if (footer != null && showFooter) {
                val footerNow = sizeConsideringHeader()
                notifyItemMoved(positionInserted, footerNow)

                Log.d(TAG, "addInfoItem() footer getTabFrom $positionInserted to $footerNow")
            }
        }
    }

    override fun viewTypeMaker(item: Any): Int {
        if (item !is InfoItem) throw IllegalArgumentException("input item isn't InfoItem")
        return when (item.infoType) {
            InfoItem.InfoType.STREAM -> when {
                useGridVariant -> GRID_STREAM_HOLDER_TYPE
                else -> STREAM_HOLDER_TYPE
            }
            InfoItem.InfoType.CHANNEL -> when {
                useGridVariant -> GRID_CHANNEL_HOLDER_TYPE
                else -> CHANNEL_HOLDER_TYPE
            }
            InfoItem.InfoType.PLAYLIST -> when {
                useGridVariant -> GRID_PLAYLIST_HOLDER_TYPE
                else -> PLAYLIST_HOLDER_TYPE
            }
            else -> {
                Log.e(TAG, "item type is invalid: item.infoType = ${item.infoType}")
                -1
            }
        }
    }

    override fun viewHolderMaker(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        Log.d(TAG, "viewHolderMaker() called with: parent = [$parent], viewType = [$viewType]")
        return when (viewType) {
            HEADER_TYPE -> HeaderFooterHolder(header!!)
            FOOTER_TYPE -> HeaderFooterHolder(footer!!)
            STREAM_HOLDER_TYPE -> NewStreamInfoItemHolder(infoItemBuilder, parent)
            GRID_STREAM_HOLDER_TYPE -> NewStreamGridInfoItemHolder(infoItemBuilder, parent)
            CHANNEL_HOLDER_TYPE -> NewChannelInfoItemHolder(infoItemBuilder, parent)
            GRID_CHANNEL_HOLDER_TYPE -> NewChannelGridInfoItemHolder(infoItemBuilder, parent)
            PLAYLIST_HOLDER_TYPE -> NewPlaylistInfoItemHolder(infoItemBuilder, parent)
            GRID_PLAYLIST_HOLDER_TYPE -> NewPlaylistGridInfoItemHolder(infoItemBuilder, parent)
            else -> {
                Log.e(TAG, "onCreateViewHolder(), viewType is invalid: viewType = $viewType")
                FallbackViewHolder(View(parent.context))
            }
        }
    }

    override fun onBindViewHolder(holder:RecyclerView.ViewHolder, position: Int) {
        var pos = position
        Log.d(TAG, "onBindViewHolder() called with: holder = [${holder.javaClass.simpleName}], position = [$pos]")
        when {
            holder is NewInfoItemHolder -> {
                // If header isn't null, offset the items by -1
                if (header != null) pos--

                holder.updateFromItem(itemsList[pos])
            }
            holder is HeaderFooterHolder && pos == 0 && header != null -> holder.view = header!!
            holder is HeaderFooterHolder && pos == sizeConsideringHeader() && footer != null && showFooter -> holder.view = footer!!
        }
    }

    companion object {
        private val TAG = NewInfoListAdapter::class.java.simpleName

        private const val STREAM_HOLDER_TYPE = 0x101
        private const val GRID_STREAM_HOLDER_TYPE = 0x102
        private const val CHANNEL_HOLDER_TYPE = 0x201
        private const val GRID_CHANNEL_HOLDER_TYPE = 0x202
        private const val PLAYLIST_HOLDER_TYPE = 0x301
        private const val GRID_PLAYLIST_HOLDER_TYPE = 0x302

    }
}
