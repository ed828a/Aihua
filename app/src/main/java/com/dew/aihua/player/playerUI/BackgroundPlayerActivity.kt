package com.dew.aihua.player.playerUI

import android.content.Intent
import android.view.MenuItem
import com.dew.aihua.R
import com.dew.aihua.player.helper.PermissionHelper
import com.dew.aihua.player.playerUI.BackgroundPlayer.Companion.ACTION_CLOSE


/**
 *  Created by Edward on 3/2/2019.
 */
class BackgroundPlayerActivity : ServicePlayerActivity() {

    override fun getTag(): String {
        return TAG
    }

    override fun getSupportActionTitle(): String {
        return resources.getString(R.string.title_activity_background_player)
    }

    override fun getBindIntent(): Intent {
        return Intent(this, BackgroundPlayer::class.java)
    }

    override fun startPlayerListener() {
        if (player != null && player is BackgroundPlayer.BasePlayerImpl) {
            (player as BackgroundPlayer.BasePlayerImpl).setActivityListener(this)
        }
    }

    override fun stopPlayerListener() {
        if (player != null && player is BackgroundPlayer.BasePlayerImpl) {
            (player as BackgroundPlayer.BasePlayerImpl).removeActivityListener(this)
        }
    }

    override fun getPlayerOptionMenuResource(): Int {
        return R.menu.menu_play_queue_bg
    }

    override fun onPlayerOptionSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_switch_popup) {

            if (!PermissionHelper.isPopupEnabled(this)) {
                PermissionHelper.showPopupEnablementToast(this)
                return true
            }

            this.player!!.setRecovery()
            applicationContext.sendBroadcast(getPlayerShutdownIntent())
            applicationContext.startService(getSwitchIntent(PopupVideoPlayer::class.java))
            return true
        }
        return false
    }

    override fun getPlayerShutdownIntent(): Intent {
        return Intent(ACTION_CLOSE)
    }

    companion object {

        private const val TAG = "BackgroundPlayerActivity"
    }
}
