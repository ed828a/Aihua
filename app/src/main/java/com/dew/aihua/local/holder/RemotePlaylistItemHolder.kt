package com.dew.aihua.local.holder

import android.view.ViewGroup
import com.dew.aihua.local.adapter.LocalItemBuilder
import com.dew.aihua.repository.database.LocalItem
import com.dew.aihua.repository.database.playlist.model.PlaylistRemoteEntity
import com.dew.aihua.util.ImageDisplayConstants
import com.dew.aihua.util.Localization
import org.schabi.newpipe.extractor.NewPipe
import java.text.DateFormat

/**
 *  Created by Edward on 2/23/2019.
 */

open class RemotePlaylistItemHolder : PlaylistItemHolder {
    constructor(infoItemBuilder: LocalItemBuilder, parent: ViewGroup) : super(infoItemBuilder, parent) {}

    internal constructor(infoItemBuilder: LocalItemBuilder, layoutId: Int, parent: ViewGroup) : super(infoItemBuilder, layoutId, parent) {}

    override fun updateFromItem(localItem: LocalItem, dateFormat: DateFormat) {
        if (localItem !is PlaylistRemoteEntity) return

        itemTitleView.text = localItem.name
        itemStreamCountView.text = localItem.streamCount.toString()
        itemUploaderView.text = if (localItem.uploader == null) ""
        else Localization.concatenateStrings(localItem.uploader!!, NewPipe.getNameOfService(localItem.serviceId))

        itemBuilder.displayImage(localItem.thumbnailUrl!!, itemThumbnailView,
            ImageDisplayConstants.DISPLAY_PLAYLIST_OPTIONS)

        super.updateFromItem(localItem, dateFormat)
    }
}
