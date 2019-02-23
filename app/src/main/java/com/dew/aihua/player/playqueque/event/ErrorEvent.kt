package com.dew.aihua.player.playqueque.event

/**
 *  Created by Edward on 2/23/2019.
 */

class ErrorEvent(val errorIndex: Int, val queueIndex: Int, val isSkippable: Boolean) : PlayQueueEvent {

    override fun type(): PlayQueueEventType {
        return PlayQueueEventType.ERROR
    }
}
