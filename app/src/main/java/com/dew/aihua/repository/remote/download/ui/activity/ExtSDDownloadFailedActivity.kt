package com.dew.aihua.repository.remote.download.ui.activity

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.dew.aihua.R
import com.dew.aihua.repository.remote.helper.ServiceHelper
import com.dew.aihua.settings.NewPipeSettings
import com.dew.aihua.util.ThemeHelper

/**
 *  Created by Edward on 2/23/2019.
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
            .setPositiveButton(R.string.yes) { dialogInterface: DialogInterface, i: Int ->
                NewPipeSettings.resetDownloadFolders(this)
                finish()
            }
            .setNegativeButton(R.string.cancel) { dialogInterface: DialogInterface, i: Int ->
                dialogInterface.dismiss()
                finish()
            }
            .create()
            .show()
    }
}
