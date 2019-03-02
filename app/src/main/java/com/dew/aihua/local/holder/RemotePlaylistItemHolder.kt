package com.dew.aihua.local.holder

import android.view.ViewGroup
import com.dew.aihua.database.LocalItem
import com.dew.aihua.database.playlist.model.PlaylistRemoteEntity
import com.dew.aihua.local.adapter.LocalItemBuilder
import com.dew.aihua.player.helper.ImageDisplayConstants
import com.dew.aihua.player.helper.Localization
import org.schabi.newpipe.extractor.NewPipe
import java.text.DateFormat

/**
 *  Created by Edward on 3/2/2019.
 */

open class RemotePlaylistItemHolder : PlaylistItemHolder {
    constructor(infoItemBuilder: LocalItemBuilder, parent: ViewGroup) : super(infoItemBuilder, parent) {}

    internal constructor(infoItemBuilder: LocalItemBuilder, layoutId: Int, parent: ViewGroup) : super(infoItemBuilder, layoutId, parent) {}

    override fun updateFromItem(item: LocalItem, dateFormat: DateFormat) {
        if (item !is PlaylistRemoteEntity) return

        itemTitleView.text = item.name
        itemStreamCountView.text = item.streamCount.toString()
        itemUploaderView.text = if (item.uploader == null) ""
        else Localization.concatenateStrings(item.uploader!!, NewPipe.getNameOfService(item.serviceId))

        itemBuilder.displayImage(item.thumbnailUrl!!, itemThumbnailView,
            ImageDisplayConstants.DISPLAY_PLAYLIST_OPTIONS)

        super.updateFromItem(item, dateFormat)
    }
}
