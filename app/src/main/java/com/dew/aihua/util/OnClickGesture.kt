package com.dew.aihua.util

/**
 *  Created by Edward on 2/23/2019.
 */


abstract class OnClickGesture<T> {

    abstract fun selected(selectedItem: T)

    open fun held(selectedItem: T) {
        // Optional gesture
    }

    open fun drag(selectedItem: T, viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder) {
        // Optional gesture
    }
}
