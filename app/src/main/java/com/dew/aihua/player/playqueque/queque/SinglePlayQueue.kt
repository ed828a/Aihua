package com.dew.aihua.player.playqueque.queque

import com.dew.aihua.player.model.PlayQueueItem
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import java.util.ArrayList
/**
 *  Created by Edward on 3/2/2019.
 */

class SinglePlayQueue : PlayQueue {
    constructor(item: StreamInfoItem) : super(0, listOf<PlayQueueItem>(PlayQueueItem(item)))

    constructor(info: StreamInfo) : super(0, listOf<PlayQueueItem>(PlayQueueItem(info)))

    constructor(items: List<StreamInfoItem>, index: Int) : super(index, playQueueItemsOf(items))

    override val isComplete: Boolean = true

    override fun fetch() {}

    companion object {
        fun playQueueItemsOf(items: List<StreamInfoItem>): List<PlayQueueItem> {
            val playQueueItems = ArrayList<PlayQueueItem>(items.size)
            for (item in items) {
                playQueueItems.add(PlayQueueItem(item))
            }
            return playQueueItems
        }
    }


}
