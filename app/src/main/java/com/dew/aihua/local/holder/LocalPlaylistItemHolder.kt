package com.dew.aihua.local.holder

import android.view.View
import android.view.ViewGroup
import com.dew.aihua.local.adapter.LocalItemBuilder
import com.dew.aihua.repository.database.LocalItem
import com.dew.aihua.repository.database.playlist.model.PlaylistMetadataEntry
import com.dew.aihua.util.ImageDisplayConstants
import java.text.DateFormat

/**
 *  Created by Edward on 2/23/2019.
 */

open class LocalPlaylistItemHolder : PlaylistItemHolder {

    constructor(infoItemBuilder: LocalItemBuilder, parent: ViewGroup) : super(infoItemBuilder, parent) {}

    internal constructor(infoItemBuilder: LocalItemBuilder, layoutId: Int, parent: ViewGroup) : super(infoItemBuilder, layoutId, parent) {}

    override fun updateFromItem(localItem: LocalItem, dateFormat: DateFormat) {
        if (localItem !is PlaylistMetadataEntry) return

        itemTitleView.text = localItem.name
        itemStreamCountView.text = localItem.streamCount.toString()
        itemUploaderView.visibility = View.INVISIBLE

        itemBuilder.displayImage(localItem.thumbnailUrl, itemThumbnailView,
            ImageDisplayConstants.DISPLAY_PLAYLIST_OPTIONS)

        super.updateFromItem(localItem, dateFormat)
    }
}
