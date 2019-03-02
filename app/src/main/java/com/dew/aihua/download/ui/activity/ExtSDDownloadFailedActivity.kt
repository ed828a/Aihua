package com.dew.aihua.download.ui.activity

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.dew.aihua.R
import com.dew.aihua.player.helper.ServiceHelper
import com.dew.aihua.player.helper.ThemeHelper
import com.dew.aihua.settings.AppSettings

/**
 *  Created by Edward on 3/2/2019.
 */
class ExtSDDownloadFailedActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeHelper.setTheme(this, ServiceHelper.getSelectedServiceId(this))
    }

    override fun onStart() {
        super.onStart()
        AlertDialog.Builder(this)
            .setTitle(R.string.download_to_sdcard_error_title)
            .setMessage(R.string.download_to_sdcard_error_message)
            .setPositiveButton(R.string.yes) { _: DialogInterface, _: Int ->
                AppSettings.resetDownloadFolders(this)
                finish()
            }
            .setNegativeButton(R.string.cancel) { dialogInterface: DialogInterface, _: Int ->
                dialogInterface.dismiss()
                finish()
            }
            .create()
            .show()
    }
}
