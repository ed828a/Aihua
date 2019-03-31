package com.dew.aihua.ui.builder

import android.content.Context
import android.widget.ImageView
import com.nostra13.universalimageloader.core.DisplayImageOptions
import com.nostra13.universalimageloader.core.ImageLoader

/**
 *  Created by Edward on 3/23/2019.
 */
abstract class GeneralItemBuilder(val context: Context) {
    private val imageLoader: ImageLoader = ImageLoader.getInstance()

    fun displayImage(
        url: String,
        view: ImageView,
        options: DisplayImageOptions
    ) {

        imageLoader.displayImage(url, view, options)
    }
}