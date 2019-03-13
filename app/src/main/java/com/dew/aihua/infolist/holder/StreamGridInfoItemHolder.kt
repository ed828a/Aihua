package com.dew.aihua.infolist.holder

import android.util.Log
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dew.aihua.R
import com.dew.aihua.infolist.adapter.InfoItemBuilder

/**
 *  Created by Edward on 3/2/2019.
 */
class StreamGridInfoItemHolder(infoItemBuilder: InfoItemBuilder, parent: ViewGroup) : StreamMiniInfoItemHolder(infoItemBuilder, R.layout.list_stream_grid_item_cardview, parent){
    init {
        Log.d(TAG, "StreamGridInfoItemHolder, init() parent = $parent")
//        val params = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
//        parent.layoutParams = params
    }
    companion object {
        val TAG = StreamGridInfoItemHolder::class.java.simpleName
    }
}
