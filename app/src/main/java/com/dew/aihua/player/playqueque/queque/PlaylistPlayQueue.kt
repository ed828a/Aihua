package com.dew.aihua.player.playqueque.queque

import com.dew.aihua.repository.remote.helper.ExtractorHelper
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.schabi.newpipe.extractor.playlist.PlaylistInfo
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItem
import org.schabi.newpipe.extractor.stream.StreamInfoItem

/**
 *  Created by Edward on 2/23/2019.
 */

class PlaylistPlayQueue : AbstractInfoPlayQueue<PlaylistInfo, PlaylistInfoItem> {

    override val tag: String
        get() = "PlaylistPlayQueue@" + Integer.toHexString(hashCode())

    constructor(item: PlaylistInfoItem) : super(item) {}

    constructor(info: PlaylistInfo) : this(info.serviceId, info.url, info.nextPageUrl, info.relatedItems, 0) {}

    constructor(serviceId: Int,
                url: String,
                nextPageUrl: String,
                streams: List<StreamInfoItem>,
                index: Int) : super(serviceId, url, nextPageUrl, streams, index) {
    }

    override fun fetch() {
        if (this.isInitial) {
            ExtractorHelper.getPlaylistInfo(this.serviceId, this.baseUrl, false)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(headListObserver)
        } else {
            ExtractorHelper.getMorePlaylistItems(this.serviceId, this.baseUrl, this.nextUrl!!)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(nextPageObserver)
        }
    }
}
