package com.dew.aihua.player.playerUI

import android.content.Intent
import android.view.MenuItem
import com.dew.aihua.R
import com.dew.aihua.player.playerUI.PopupVideoPlayer.Companion.ACTION_CLOSE

/**
 *  Created by Edward on 2/23/2019.
 */

class PopupVideoPlayerActivity : ServicePlayerActivity() {

    override fun getTag(): String {
        return TAG
    }

    override fun getSupportActionTitle(): String {
        return resources.getString(R.string.title_activity_popup_player)
    }

    override fun getBindIntent(): Intent {
        return Intent(this, PopupVideoPlayer::class.java)
    }

    override fun startPlayerListener() {
        if (player != null && player is PopupVideoPlayer.VideoPlayerImpl) {
            (player as PopupVideoPlayer.VideoPlayerImpl).setActivityListener(this)
        }
    }

    override fun stopPlayerListener() {
        if (player != null && player is PopupVideoPlayer.VideoPlayerImpl) {
            (player as PopupVideoPlayer.VideoPlayerImpl).removeActivityListener(this)
        }
    }

    override fun getPlayerOptionMenuResource(): Int {
        return R.menu.menu_play_queue_popup
    }

    override fun onPlayerOptionSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_switch_background) {
            this.player!!.setRecovery()
            applicationContext.sendBroadcast(getPlayerShutdownIntent())
            applicationContext.startService(getSwitchIntent(BackgroundPlayer::class.java))
            return true
        }
        return false
    }

    override fun getPlayerShutdownIntent(): Intent {
        return Intent(ACTION_CLOSE)
    }

    companion object {

        private const val TAG = "PopupVideoPlayerActivity"
    }
}
