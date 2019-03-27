package com.dew.aihua.ui.local.adapter

import android.content.Context
import android.widget.ImageView
import com.dew.aihua.data.local.database.LocalItem
import com.dew.aihua.util.OnClickGesture
import com.nostra13.universalimageloader.core.DisplayImageOptions
import com.nostra13.universalimageloader.core.ImageLoader

/**
 *  Created by Edward on 3/2/2019.
 */
class LocalItemBuilder(val context: Context?) {
    private val imageLoader = ImageLoader.getInstance()

    var onItemSelectedListener: OnClickGesture<LocalItem>? = null

    fun displayImage(url: String,
                     view: ImageView,
                     options: DisplayImageOptions
    ) {
        imageLoader.displayImage(url, view, options)
    }

}
