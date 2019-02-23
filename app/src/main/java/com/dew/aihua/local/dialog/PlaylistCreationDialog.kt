package com.dew.aihua.local.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import com.dew.aihua.R
import com.dew.aihua.local.playlist.LocalPlaylistManager
import com.dew.aihua.repository.database.AppDatabase
import com.dew.aihua.repository.database.stream.model.StreamEntity
import io.reactivex.android.schedulers.AndroidSchedulers

/**
 *  Created by Edward on 2/23/2019.
 */

class PlaylistCreationDialog : PlaylistDialog() {

    ///////////////////////////////////////////////////////////////////////////
    // Dialog
    ///////////////////////////////////////////////////////////////////////////

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        if (streams == null) return super.onCreateDialog(savedInstanceState)

        val dialogView = View.inflate(context, R.layout.dialog_playlist_name, null)
        val nameInput = dialogView.findViewById<EditText>(R.id.playlist_name)

        val dialogBuilder = AlertDialog.Builder(context)
            .setTitle(R.string.create_playlist)
            .setView(dialogView)
            .setCancelable(true)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.create) { dialogInterface, i ->
                val name = nameInput.text.toString()
                val playlistManager = LocalPlaylistManager(AppDatabase.getDatabase(context!!))
                val successToast = Toast.makeText(activity,
                    R.string.playlist_creation_success,
                    Toast.LENGTH_SHORT)

                playlistManager.createPlaylist(name, streams!!)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { longs -> successToast.show() }
            }

        return dialogBuilder.create()
    }

    companion object {
        private val TAG = PlaylistCreationDialog::class.java.canonicalName

        fun newInstance(streams: List<StreamEntity>): PlaylistCreationDialog {
            val dialog = PlaylistCreationDialog()
            dialog.setInfo(streams)
            return dialog
        }
    }
}
