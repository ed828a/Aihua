package com.dew.aihua.local.holder

import android.view.View
import android.view.ViewGroup
import com.dew.aihua.local.adapter.LocalItemBuilder
import com.dew.aihua.database.LocalItem
import com.dew.aihua.database.playlist.model.PlaylistMetadataEntry
import com.dew.aihua.player.helper.ImageDisplayConstants
import java.text.DateFormat

/**
 *  Created by Edward on 3/2/2019.
 */

open class LocalPlaylistItemHolder : PlaylistItemHolder {

    constructor(infoItemBuilder: LocalItemBuilder, parent: ViewGroup) : super(infoItemBuilder, parent) {}

    internal constructor(infoItemBuilder: LocalItemBuilder, layoutId: Int, parent: ViewGroup) : super(infoItemBuilder, layoutId, parent) {}

    override fun updateFromItem(item: LocalItem, dateFormat: DateFormat) {
        if (item !is PlaylistMetadataEntry) return

        itemTitleView.text = item.name
        itemStreamCountView.text = item.streamCount.toString()
        itemUploaderView.visibility = View.INVISIBLE

        itemBuilder.displayImage(item.thumbnailUrl, itemThumbnailView,
            ImageDisplayConstants.DISPLAY_PLAYLIST_OPTIONS)

        super.updateFromItem(item, dateFormat)
    }
}
