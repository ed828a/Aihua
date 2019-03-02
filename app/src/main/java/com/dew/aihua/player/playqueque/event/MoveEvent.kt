package com.dew.aihua.player.playqueque.event

/**
 *  Created by Edward on 2/23/2019.
 */

class MoveEvent(val fromIndex: Int, val toIndex: Int) : PlayQueueEvent {

    override fun type(): PlayQueueEventType {
        return PlayQueueEventType.MOVE
    }
}
