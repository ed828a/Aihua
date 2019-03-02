package com.dew.aihua.player.playqueque.event

/**
 *  Created by Edward on 2/23/2019.
 */

class ReorderEvent(val fromSelectedIndex: Int, val toSelectedIndex: Int) : PlayQueueEvent {

    override fun type(): PlayQueueEventType {
        return PlayQueueEventType.REORDER
    }
}
