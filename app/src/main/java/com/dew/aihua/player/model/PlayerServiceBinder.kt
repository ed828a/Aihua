package com.dew.aihua.player.model

import android.os.Binder
import com.dew.aihua.player.playerUI.BasePlayer

/**
 *  Created by Edward on 2/23/2019.
 */

internal class PlayerServiceBinder(val playerInstance: BasePlayer) : Binder()
