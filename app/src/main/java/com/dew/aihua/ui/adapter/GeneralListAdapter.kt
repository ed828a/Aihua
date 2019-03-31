package com.dew.aihua.ui.adapter

import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 *  Created by Edward on 3/2/2019.
 */
abstract class GeneralListAdapter<GeneralItem: Any> : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    abstract val itemsList: ArrayList<GeneralItem>

    protected var showFooter = false
    private var useGridVariant = false
    var header: View? = null
    set(value) {
        val changed = value != field
        field = value
        if (changed) notifyDataSetChanged()
    }

    var footer: View? = null

    fun setGridItemVariants(useGridVariant: Boolean) {
        this.useGridVariant = useGridVariant
    }

    fun addItems(data: List<GeneralItem>?) {
        data?.let{items ->

//            Log.d(TAG, "addItems() before > localItems.size() = ${itemsList.size}, data.size() = ${data.size}")

            val offsetStart = sizeConsideringHeader()

            with(itemsList){
                addAll(items)
                val swapList = this.distinct()
                clear()
                addAll(swapList)
            }

            Log.d(TAG, "addItems() after > offsetStart = $offsetStart, localItems.size() = ${itemsList.size}, header = $header, footer = $footer, showFooter = $showFooter")

            notifyItemRangeInserted(offsetStart, data.size)

            if (footer != null && showFooter) {
                val footerNow = sizeConsideringHeader()
                notifyItemMoved(offsetStart, footerNow)

                Log.d(TAG, "addItems() footer getTabFrom $offsetStart to $footerNow")
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


//    fun setHeader(header: View?) {
//        val changed = header != this.header
//        this.header = header
//        if (changed) notifyDataSetChanged()
//    }

//    fun setFooter(view: View) {
//        this.footer = view
//    }

    fun showFooter(show: Boolean) {
        Log.d(TAG, "showFooter() called with: show = [$show]")
        if (show == showFooter) return

        showFooter = show
        if (show)
            notifyItemInserted(sizeConsideringHeader())
        else
            notifyItemRemoved(sizeConsideringHeader())
    }

    protected fun sizeConsideringHeader(): Int {
        val size = itemsList.size + if (header != null) 1 else 0
        Log.d(TAG, "sizeConsideringHeaderOffset() called → $size")
        return size
    }

    override fun getItemCount(): Int {
        var count = itemsList.size
        if (header != null) count++
        if (footer != null && showFooter) count++

//        Log.d(TAG, "getItemCount() called, count = $count, localItems.size() = ${itemsList.size}, header = $header, footer = $footer, showFooter = $showFooter")

        return count
    }

    override fun getItemViewType(pos: Int): Int {
        var position = pos
//        Log.d(TAG, "getItemViewType() called with: position = [$position]")

        if (header != null && position == 0) {
            return HEADER_TYPE
        } else if (header != null) {
            position--
        }
        if (footer != null && position == itemsList.size && showFooter) {
            return FOOTER_TYPE
        }
        val item = itemsList[position]

        return viewTypeMaker(item)
    }

    abstract fun viewTypeMaker(item: Any): Int

    override fun onCreateViewHolder(parent: ViewGroup, type: Int): RecyclerView.ViewHolder {
        Log.d(TAG, "onCreateViewHolder() called with: parent = [$parent], type = [$type]")

        return viewHolderMaker(parent, type)
    }

    abstract fun viewHolderMaker(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder

    fun getSpanSizeLookup(spanCount: Int): GridLayoutManager.SpanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
        override fun getSpanSize(position: Int): Int {
            val type = getItemViewType(position)
            return if (type == HEADER_TYPE || type == FOOTER_TYPE) spanCount else 1
        }
    }


    companion object {

        private val TAG = GeneralListAdapter::class.java.simpleName

        const val HEADER_TYPE = 0
        const val FOOTER_TYPE = 1

    }
}
