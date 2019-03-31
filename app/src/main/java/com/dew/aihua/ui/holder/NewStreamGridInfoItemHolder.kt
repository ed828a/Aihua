package com.dew.aihua.ui.holder

import android.util.Log
import android.view.ViewGroup
import com.dew.aihua.R
import com.dew.aihua.ui.builder.NewInfoItemBuilder

/**
 *  Created by Edward on 3/2/2019.
 */
class NewStreamGridInfoItemHolder(
    infoItemBuilder: NewInfoItemBuilder,
    parent: ViewGroup
) : NewStreamMiniInfoItemHolder(infoItemBuilder, R.layout.list_stream_grid_item_cardview, parent) {
    init {
        Log.d(TAG, "StreamGridInfoItemHolder, init() parent = $parent")
//        val params = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
//        parent.layoutParams = params
    }

    companion object {
        val TAG = NewStreamGridInfoItemHolder::class.java.simpleName
    }
}
