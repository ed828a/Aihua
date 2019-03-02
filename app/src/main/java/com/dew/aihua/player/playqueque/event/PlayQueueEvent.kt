package com.dew.aihua.player.playqueque.event

import java.io.Serializable

/**
 *  Created by Edward on 2/23/2019.
 */

interface PlayQueueEvent : Serializable {
    fun type(): PlayQueueEventType
}
