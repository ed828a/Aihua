package com.dew.aihua.player.playqueque.event

/**
 *  Created by Edward on 2/23/2019.
 */

class InitEvent : PlayQueueEvent {
    override fun type(): PlayQueueEventType {
        return PlayQueueEventType.INIT
    }
}
