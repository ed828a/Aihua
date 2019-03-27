package com.dew.aihua.ui.local.dialog

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.dew.aihua.R
import com.dew.aihua.ui.local.adapter.LocalItemListAdapter
import com.dew.aihua.ui.local.playlist.LocalPlaylistManager
import com.dew.aihua.player.model.PlayQueueItem
import com.dew.aihua.data.local.database.AppDatabase
import com.dew.aihua.data.local.database.LocalItem
import com.dew.aihua.data.local.database.playlist.model.PlaylistMetadataEntry
import com.dew.aihua.data.local.database.stream.model.StreamEntity
import com.dew.aihua.util.OnClickGesture
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import java.util.ArrayList

/**
 *  Created by Edward on 3/2/2019.
 */
class PlaylistAppendDialog : PlaylistDialog() {

    private var playlistRecyclerView: androidx.recyclerview.widget.RecyclerView? = null
    private var playlistAdapter: LocalItemListAdapter? = null

    private var playlistReactor: Disposable? = null

    private val compositeDisposable = CompositeDisposable()
    ///////////////////////////////////////////////////////////////////////////
    // LifeCycle - Creation
    ///////////////////////////////////////////////////////////////////////////

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_playlists, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//        val playlistManager = LocalPlaylistManager(NewPipeDatabase.getInstance(context!!))
        val playlistManager = LocalPlaylistManager(AppDatabase.getDatabase(context!!))
        playlistAdapter = LocalItemListAdapter(activity!!)
        playlistAdapter?.setSelectedListener(object : OnClickGesture<LocalItem>() {
            override fun selected(selectedItem: LocalItem) {
                if (selectedItem !is PlaylistMetadataEntry || streams == null)
                    return
                onPlaylistSelected(playlistManager, selectedItem, streams!!)
            }
        })

        playlistRecyclerView = view.findViewById(R.id.playlist_list)
        playlistRecyclerView?.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
        playlistRecyclerView?.adapter = playlistAdapter

        val newPlaylistButton = view.findViewById<View>(R.id.newPlaylist)
        newPlaylistButton.setOnClickListener { _ -> openCreatePlaylistDialog() }

        playlistReactor = playlistManager.playlists
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {list -> this.onPlaylistsReceived(list) }
    }

    ///////////////////////////////////////////////////////////////////////////
    // LifeCycle - Destruction
    ///////////////////////////////////////////////////////////////////////////

    override fun onDestroyView() {
        super.onDestroyView()
        playlistReactor?.dispose()
        playlistAdapter?.unsetSelectedListener()

        playlistReactor = null
        playlistRecyclerView = null
        playlistAdapter = null
        if (!compositeDisposable.isDisposed) compositeDisposable.dispose()
    }

    ///////////////////////////////////////////////////////////////////////////
    // Helper
    ///////////////////////////////////////////////////////////////////////////

    private fun openCreatePlaylistDialog() {
        if (streams == null || fragmentManager == null) return

        PlaylistCreationDialog.newInstance(streams!!).show(fragmentManager!!, TAG)
        dialog?.dismiss()
    }

    private fun onPlaylistsReceived(playlists: List<PlaylistMetadataEntry>) {
        if (playlists.isEmpty()) {
            openCreatePlaylistDialog()
            return
        }

        if (playlistAdapter != null && playlistRecyclerView != null) {
            playlistAdapter!!.clearStreamItemList()
            playlistAdapter!!.addItems(playlists)
            playlistRecyclerView!!.visibility = View.VISIBLE
        }
    }

    private fun onPlaylistSelected(manager: LocalPlaylistManager,
                                   playlist: PlaylistMetadataEntry,
                                   streams: List<StreamEntity>) {

        @SuppressLint("ShowToast")
        val successToast = Toast.makeText(context,
            R.string.playlist_add_stream_success, Toast.LENGTH_SHORT)

        val d = manager.appendToPlaylist(playlist.uid, streams)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { successToast.show() }

        compositeDisposable.add(d)
        dialog?.dismiss()
    }

    companion object {
        private val TAG = PlaylistAppendDialog::class.java.canonicalName

        fun fromStreamInfo(info: StreamInfo): PlaylistAppendDialog {
            val dialog = PlaylistAppendDialog()
            dialog.setInfo(listOf(StreamEntity(info)))
            return dialog
        }

        fun fromStreamInfoItems(items: List<StreamInfoItem>): PlaylistAppendDialog {
            val dialog = PlaylistAppendDialog()
            val entities = ArrayList<StreamEntity>(items.size)
            for (item in items) {
                entities.add(StreamEntity(item))
            }
            dialog.setInfo(entities)
            return dialog
        }

        fun fromPlayQueueItems(items: List<PlayQueueItem>): PlaylistAppendDialog {
            val dialog = PlaylistAppendDialog()
            val entities = ArrayList<StreamEntity>(items.size)
            for (item in items) {
                entities.add(StreamEntity(item))
            }
            dialog.setInfo(entities)
            return dialog
        }
    }
}
