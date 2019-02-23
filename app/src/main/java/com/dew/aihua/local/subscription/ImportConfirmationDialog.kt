package com.dew.aihua.local.subscription

import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import com.dew.aihua.R
import com.dew.aihua.util.ThemeHelper
import icepick.Icepick
import icepick.State

/**
 *  Created by Edward on 2/23/2019.
 */

class ImportConfirmationDialog : androidx.fragment.app.DialogFragment() {
    @State
    @JvmField
    var resultServiceIntent: Intent? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        AlertDialog.Builder(context, ThemeHelper.getDialogTheme(context!!))
            .setMessage(R.string.import_network_expensive_warning)
            .setCancelable(true)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(android.R.string.ok) { dialogInterface, i ->
                if (resultServiceIntent != null && context != null) {
                    context!!.startService(resultServiceIntent)
                }
                dismiss()
            }
            .create()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (resultServiceIntent == null) throw IllegalStateException("Result intent is null")

        Icepick.restoreInstanceState(this, savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Icepick.saveInstanceState(this, outState)
    }

    companion object {

        fun show(fragment: androidx.fragment.app.Fragment, resultServiceIntent: Intent) {
            if (fragment.fragmentManager == null) return

            val confirmationDialog = ImportConfirmationDialog()
            confirmationDialog.resultServiceIntent = resultServiceIntent
            confirmationDialog.show(fragment.fragmentManager!!, null)
        }
    }
}
