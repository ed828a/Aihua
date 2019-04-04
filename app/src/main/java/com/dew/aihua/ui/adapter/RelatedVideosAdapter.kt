package com.dew.aihua.ui.adapter

import android.app.Activity
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dew.aihua.ui.infolist.adapter.InfoItemBuilder
import com.dew.aihua.ui.infolist.adapter.InfoListAdapter
import com.dew.aihua.ui.infolist.holder.*
import com.dew.aihua.util.OnClickGesture
import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import java.util.ArrayList

/**
 *  Created by Edward on 3/5/2019.
 */

class RelatedVideosAdapter(activity: Activity) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val infoItemBuilder: InfoItemBuilder = InfoItemBuilder(activity)
    val itemsList: ArrayList<InfoItem> = ArrayList()
    private var useGridVariant = false
    private var showFooter = false
    private var header: View? = null
    private var footer: View? = null

    fun setOnStreamSelectedListener(listener: OnClickGesture<StreamInfoItem>) {
        infoItemBuilder.onStreamSelectedListener = listener
    }

    fun setGridItemVariants(useGridVariant: Boolean) {
        this.useGridVariant = useGridVariant
    }

    fun addInfoItemList(data: List<InfoItem>?) {
        data?.let { dataList ->
            Log.d(TAG, "addInfoItemList() before > infoItemList.size() = ${itemsList.size}, data.size() = ${dataList.size}")

            val offsetStart = sizeConsideringHeaderOffset()
            itemsList.addAll(dataList)

            Log.d(TAG, "addInfoItemList() after > offsetStart = $offsetStart, infoItemList.size() = ${itemsList.size}, header = $header, footer = $footer, showFooter = $showFooter")

            notifyItemRangeInserted(offsetStart, dataList.size)

            if (footer != null && showFooter) {
                val footerNow = sizeConsideringHeaderOffset()
                notifyItemMoved(offsetStart, footerNow)

                Log.d(TAG, "addInfoItemList() footer getTabFrom $offsetStart to $footerNow")
            }
        }
    }

    fun clearStreamItemList() {
        if (itemsList.isEmpty()) {
            return
        }
        itemsList.clear()
        notifyDataSetChanged()
    }

    private fun sizeConsideringHeaderOffset(): Int {
        val size = itemsList.size + if (header != null) 1 else 0
        Log.d(TAG, "sizeConsideringHeaderOffset() called → $size")
        return size
    }

    override fun getItemCount(): Int {
        val count = itemsList.size

        Log.d(TAG, "getItemCount() called, count = $count, infoItemList.size() = ${itemsList.size}")
        return count
    }

    override fun getItemViewType(pos: Int): Int {
        Log.d(TAG, "getItemViewType() called with: pos = [$pos]")
        return GRID_STREAM_HOLDER_TYPE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        Log.d(TAG, "onCreateViewHolder() called with: parent = [$parent], viewType = [$viewType]")
        return StreamGridInfoItemHolder(infoItemBuilder, parent)

    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        var pos = position
        Log.d(TAG, "onBindViewHolder() called with: holder = [${holder.javaClass.simpleName}], position = [$pos]")
        when {
            holder is InfoItemHolder -> {
                // If header isn't null, offset the items by -1
                if (header != null) pos--

                holder.updateFromItem(itemsList[pos])
            }
            holder is HeaderFooterHolder && pos == 0 && header != null -> holder.view = header!!
            holder is HeaderFooterHolder && pos == sizeConsideringHeaderOffset() && footer != null && showFooter -> holder.view = footer!!
        }
    }

    fun getSpanSizeLookup(spanCount: Int): androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup {
        return object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                val type = getItemViewType(position)
                return if (type == HEADER_TYPE || type == FOOTER_TYPE) spanCount else 1
            }
        }
    }


    companion object {
        private val TAG = InfoListAdapter::class.java.simpleName

        private const val HEADER_TYPE = 0
        private const val FOOTER_TYPE = 1
        private const val GRID_STREAM_HOLDER_TYPE = 0x102

    }
}
