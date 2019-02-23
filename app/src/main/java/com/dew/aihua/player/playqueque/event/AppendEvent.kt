package com.dew.aihua.player.playqueque.event

/**
 *  Created by Edward on 2/23/2019.
 */

class AppendEvent(val amount: Int) : PlayQueueEvent {

    override fun type(): PlayQueueEventType {
        return PlayQueueEventType.APPEND
    }
}
