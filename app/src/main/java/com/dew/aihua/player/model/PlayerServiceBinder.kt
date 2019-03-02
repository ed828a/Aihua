package com.dew.aihua.player.model

import android.os.Binder
import com.dew.aihua.player.playerUI.BasePlayer

/**
 *  Created by Edward on 3/2/2019.
 */

internal class PlayerServiceBinder(val playerInstance: BasePlayer) : Binder()
