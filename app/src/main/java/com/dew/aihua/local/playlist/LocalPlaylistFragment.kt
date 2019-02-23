package com.dew.aihua.local.playlist

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.ItemTouchHelper
import com.dew.aihua.R
import com.dew.aihua.info_list.adapter.InfoItemDialog
import com.dew.aihua.local.BaseLocalListFragment
import com.dew.aihua.player.playerUI.PopupVideoPlayer.Companion.ACTION_CLOSE
import com.dew.aihua.player.playqueque.queque.PlayQueue
import com.dew.aihua.player.playqueque.queque.SinglePlayQueue
import com.dew.aihua.report.UserAction
import com.dew.aihua.repository.database.AppDatabase
import com.dew.aihua.repository.database.LocalItem
import com.dew.aihua.repository.database.playlist.model.PlaylistStreamEntry
import com.dew.aihua.util.AnimationUtils.animateView
import com.dew.aihua.util.Localization
import com.dew.aihua.util.NavigationHelper
import com.dew.aihua.util.OnClickGesture
import icepick.State
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.disposables.Disposables
import io.reactivex.subjects.PublishSubject
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import java.util.ArrayList
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 *  Created by Edward on 2/23/2019.
 */

class LocalPlaylistFragment : BaseLocalListFragment<List<PlaylistStreamEntry>, Void>() {

    private var headerRootLayout: View? = null
    private var headerTitleView: TextView? = null
    private var headerStreamCount: TextView? = null

    private var playlistControl: View? = null
    private var headerPlayAllButton: View? = null
    private var headerPopupButton: View? = null
    private var headerBackgroundButton: View? = null

    @State
    @JvmField
    var playlistId: Long? = null
    @State
    @JvmField
    var name: String = ""
    @State
    @JvmField
    var itemsListState: Parcelable? = null

    private var itemTouchHelper: ItemTouchHelper? = null

    private var playlistManager: LocalPlaylistManager? = null
    // for backpressure
    private var databaseSubscription: Subscription? = null

    private var debouncedSaveSignal: PublishSubject<Long>? = null
//    private var disposables: CompositeDisposable? = null

    /* Has the playlist been fully loaded getTabFrom db */
    private var isLoadingComplete: AtomicBoolean? = null
    /* Has the playlist been modified (e.g. items reordered or deleted) */
    private var isModified: AtomicBoolean? = null

    ///////////////////////////////////////////////////////////////////////////
    // Playlist Stream Loader
    ///////////////////////////////////////////////////////////////////////////

    private// Skip handling the result after it has been modified
    val playlistObserver: Subscriber<List<PlaylistStreamEntry>>
        get() = object : Subscriber<List<PlaylistStreamEntry>> {
            override fun onSubscribe(subscription: Subscription) {
                showLoading()
                isLoadingComplete?.set(false)

                databaseSubscription?.cancel()
                databaseSubscription = subscription
                databaseSubscription?.request(1)
            }

            override fun onNext(streams: List<PlaylistStreamEntry>) {
                if (isModified == null || !isModified!!.get()) {
                    handleResult(streams)
                    isLoadingComplete?.set(true)
                }

                databaseSubscription?.request(1)
            }

            override fun onError(exception: Throwable) {
                this@LocalPlaylistFragment.onError(exception)
            }

            override fun onComplete() {}
        }

    private val debouncedSaver: Disposable
        get() = if (debouncedSaveSignal == null) Disposables.empty()
        else debouncedSaveSignal!!
            .debounce(SAVE_DEBOUNCE_MILLIS, TimeUnit.MILLISECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ ignored -> saveImmediate() }, { this.onError(it) })


    private val itemTouchCallback: ItemTouchHelper.SimpleCallback
        get() {
            var directions = ItemTouchHelper.UP or ItemTouchHelper.DOWN
            if (isGridLayout()) {
                directions = directions or (ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT)
            }
            return object : ItemTouchHelper.SimpleCallback(directions,
                ItemTouchHelper.ACTION_STATE_IDLE) {
                override fun interpolateOutOfBoundsScroll(recyclerView: androidx.recyclerview.widget.RecyclerView,
                                                          viewSize: Int,
                                                          viewSizeOutOfBounds: Int,
                                                          totalSize: Int,
                                                          msSinceStartScroll: Long): Int {
                    val standardSpeed = super.interpolateOutOfBoundsScroll(recyclerView,
                        viewSize,
                        viewSizeOutOfBounds,
                        totalSize,
                        msSinceStartScroll)

                    val minimumAbsVelocity = Math.max(MINIMUM_INITIAL_DRAG_VELOCITY, Math.abs(standardSpeed))

                    return minimumAbsVelocity * Math.signum(viewSizeOutOfBounds.toFloat()).toInt()
                }

                override fun onMove(recyclerView: androidx.recyclerview.widget.RecyclerView,
                                    source: androidx.recyclerview.widget.RecyclerView.ViewHolder,
                                    target: androidx.recyclerview.widget.RecyclerView.ViewHolder): Boolean {
                    if (source.itemViewType != target.itemViewType || itemListAdapter == null) {
                        return false
                    }

                    val sourceIndex = source.adapterPosition
                    val targetIndex = target.adapterPosition
                    val isSwapped = itemListAdapter!!.swapItems(sourceIndex, targetIndex)
                    if (isSwapped) saveChanges()
                    return isSwapped
                }

                override fun isLongPressDragEnabled(): Boolean = true


                override fun isItemViewSwipeEnabled(): Boolean = true


                override fun onSwiped(viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder, swipeDir: Int) {}
            }
        }

    private val playQueue: PlayQueue
        get() = getPlayQueue(0)

    ///////////////////////////////////////////////////////////////////////////
    // Fragment LifeCycle - Creation
    ///////////////////////////////////////////////////////////////////////////

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        playlistManager = LocalPlaylistManager(NewPipeDatabase.getInstance(context!!))
        playlistManager = LocalPlaylistManager(AppDatabase.getDatabase(context!!))
        debouncedSaveSignal = PublishSubject.create()

        isLoadingComplete = AtomicBoolean()
        isModified = AtomicBoolean()
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_playlist, container, false)
    }

    ///////////////////////////////////////////////////////////////////////////
    // Fragment Lifecycle - Views
    ///////////////////////////////////////////////////////////////////////////

    override fun setTitle(title: String) {
        super.setTitle(title)

        headerTitleView?.text = title
    }

    override fun initViews(rootView: View, savedInstanceState: Bundle?) {
        super.initViews(rootView, savedInstanceState)
        setTitle(name)
    }

    override fun getListHeader(): View? {
        headerRootLayout = activity!!.layoutInflater.inflate(R.layout.local_playlist_header, itemsList, false)
        if (headerRootLayout == null) return null

        headerTitleView = headerRootLayout!!.findViewById(R.id.playlist_title_view)
        headerTitleView!!.isSelected = true

        headerStreamCount = headerRootLayout!!.findViewById(R.id.playlist_stream_count)

        playlistControl = headerRootLayout!!.findViewById(R.id.playlist_control)
        headerPlayAllButton = headerRootLayout!!.findViewById(R.id.playlist_ctrl_play_all_button)
        headerPopupButton = headerRootLayout!!.findViewById(R.id.playlist_ctrl_play_popup_button)
        headerBackgroundButton = headerRootLayout!!.findViewById(R.id.playlist_ctrl_play_bg_button)

        return headerRootLayout
    }

    override fun initListeners() {
        super.initListeners()

        headerTitleView?.setOnClickListener { view -> createRenameDialog() }

        itemTouchHelper = ItemTouchHelper(itemTouchCallback)
        itemTouchHelper?.attachToRecyclerView(itemsList)

        itemListAdapter?.setSelectedListener(object : OnClickGesture<LocalItem>() {
            override fun selected(selectedItem: LocalItem) {
                if (selectedItem is PlaylistStreamEntry) {
                    context?.applicationContext?.sendBroadcast(Intent(ACTION_CLOSE))
                    NavigationHelper.openVideoDetailFragment(fragmentManager,
                        selectedItem.serviceId, selectedItem.url, selectedItem.title)
                }
            }

            override fun held(selectedItem: LocalItem) {
                if (selectedItem is PlaylistStreamEntry) {
                    showStreamItemDialog(selectedItem)
                }
            }

            override fun drag(selectedItem: LocalItem, viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder) {
                itemTouchHelper?.startDrag(viewHolder)
            }
        })
    }

    ///////////////////////////////////////////////////////////////////////////
    // Fragment Lifecycle - Loading
    ///////////////////////////////////////////////////////////////////////////

    override fun showLoading() {
        super.showLoading()
        if (headerRootLayout != null) animateView(headerRootLayout!!, false, 200)
        if (playlistControl != null) animateView(playlistControl!!, false, 200)
    }

    override fun hideLoading() {
        super.hideLoading()
        if (headerRootLayout != null) animateView(headerRootLayout!!, true, 200)
        if (playlistControl != null) animateView(playlistControl!!, true, 200)
    }

    override fun startLoading(forceLoad: Boolean) {
        super.startLoading(forceLoad)

        compositeDisposable.clear()
        compositeDisposable.add(debouncedSaver)

        isLoadingComplete?.set(false)
        isModified?.set(false)

        if (playlistManager != null && playlistId != null)
            playlistManager!!.getPlaylistStreams(playlistId!!)
                .onBackpressureLatest()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(playlistObserver)
    }

    ///////////////////////////////////////////////////////////////////////////
    // Fragment Lifecycle - Destruction
    ///////////////////////////////////////////////////////////////////////////

    override fun onPause() {
        super.onPause()
        itemsListState = itemsList?.layoutManager?.onSaveInstanceState()

        // Save on exit
        saveImmediate()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        itemListAdapter?.unsetSelectedListener()
        headerBackgroundButton?.setOnClickListener(null)
        headerPlayAllButton?.setOnClickListener(null)
        headerPopupButton?.setOnClickListener(null)

        databaseSubscription?.cancel()
        compositeDisposable.clear()

        databaseSubscription = null
        itemTouchHelper = null
    }

    override fun onDestroy() {
        super.onDestroy()
        debouncedSaveSignal?.onComplete()
        if (!compositeDisposable.isDisposed) compositeDisposable.dispose()

        debouncedSaveSignal = null
        playlistManager = null

        isLoadingComplete = null
        isModified = null
    }

    override fun handleResult(result: List<PlaylistStreamEntry>) {
        super.handleResult(result)
        if (itemListAdapter == null) return

        itemListAdapter!!.clearStreamItemList()

        if (result.isEmpty()) {
            showEmptyState()
            return
        }

        itemListAdapter!!.addItems(result)
        if (itemsListState != null) {
            itemsList!!.layoutManager!!.onRestoreInstanceState(itemsListState)
            itemsListState = null
        }
        setVideoCount(itemListAdapter!!.itemsList.size.toLong())

        headerPlayAllButton!!.setOnClickListener { view -> NavigationHelper.playOnMainPlayer(activity, playQueue) }
        headerPopupButton!!.setOnClickListener { view -> NavigationHelper.playOnPopupPlayer(activity, playQueue) }
        headerBackgroundButton!!.setOnClickListener { view -> NavigationHelper.playOnBackgroundPlayer(activity, playQueue) }

        hideLoading()
    }

    ///////////////////////////////////////////////////////////////////////////
    // Fragment Error Handling
    ///////////////////////////////////////////////////////////////////////////

    override fun resetFragment() {
        super.resetFragment()
        databaseSubscription?.cancel()
    }

    override fun onError(exception: Throwable): Boolean {
        if (super.onError(exception)) return true

        onUnrecoverableError(exception, UserAction.SOMETHING_ELSE,
            "none", "Local Playlist", R.string.general_error)
        return true
    }

    ///////////////////////////////////////////////////////////////////////////
    // Playlist Metadata/Streams Manipulation
    ///////////////////////////////////////////////////////////////////////////

    private fun createRenameDialog() {
        if (playlistId == null || context == null) return

        val dialogView = View.inflate(context, R.layout.dialog_playlist_name, null)
        val nameEdit = dialogView.findViewById<EditText>(R.id.playlist_name)
        nameEdit.setText(name)
        nameEdit.setSelection(nameEdit.text.length)

        val dialogBuilder = AlertDialog.Builder(context!!)
            .setTitle(R.string.rename_playlist)
            .setView(dialogView)
            .setCancelable(true)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.rename) { dialogInterface, i -> changePlaylistName(nameEdit.text.toString()) }

        dialogBuilder.show()
    }

    private fun changePlaylistName(name: String) {
        if (playlistManager == null) return

        this.name = name
        setTitle(name)

        Log.d(TAG, "Updating playlist id=[$playlistId] with new name=[$name] items")

        val disposable = playlistManager!!.renamePlaylist(playlistId!!, name)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({/*Do nothing on success*/ longs -> }, { this.onError(it) })
        compositeDisposable.add(disposable)
    }

    private fun changeThumbnailUrl(thumbnailUrl: String) {
        if (playlistManager == null) return

        val successToast = Toast.makeText(getActivity(),
            R.string.playlist_thumbnail_change_success,
            Toast.LENGTH_SHORT)

        Log.d(TAG, "Updating playlist id=[$playlistId] with new thumbnail url=[$thumbnailUrl]")

        val disposable = playlistManager!!
            .changePlaylistThumbnail(playlistId!!, thumbnailUrl)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ ignore -> successToast.show() }, { this.onError(it) })

        compositeDisposable.add(disposable)
    }

    private fun deleteItem(item: PlaylistStreamEntry) {
        if (itemListAdapter == null) return

        itemListAdapter!!.removeItem(item)
        setVideoCount(itemListAdapter!!.itemsList.size.toLong())
        saveChanges()
    }

    private fun saveChanges() {
        if (isModified == null || debouncedSaveSignal == null) return

        isModified!!.set(true)
        debouncedSaveSignal!!.onNext(System.currentTimeMillis())
    }

    private fun saveImmediate() {
        if (playlistManager == null || itemListAdapter == null) return

        // List must be loaded and modified in order to save
        if (isLoadingComplete == null || isModified == null ||
            !isLoadingComplete!!.get() || !isModified!!.get()) {
            Log.w(TAG, "Attempting to save playlist when local playlist " +
                    "is not loaded or not modified: playlist id=[$playlistId]")
            return
        }

        val items = itemListAdapter!!.itemsList
        val streamIds = ArrayList<Long>(items.size)
        for (item in items) {
            if (item is PlaylistStreamEntry) {
                streamIds.add(item.streamId)
            }
        }

        Log.d(TAG, "Updating playlist id=[$playlistId] with [${streamIds.size}] items")

        val disposable = playlistManager!!.updateJoin(playlistId!!, streamIds)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { isModified?.set(false) },
                { this.onError(it) }
            )
        compositeDisposable.add(disposable)
    }

    ///////////////////////////////////////////////////////////////////////////
    // Utils
    ///////////////////////////////////////////////////////////////////////////

    protected fun showStreamItemDialog(item: PlaylistStreamEntry) {
        val context = context
        val activity = getActivity()
        if (context == null || context.resources == null || getActivity() == null) return

        val infoItem = item.toStreamInfoItem()

        val commands = arrayOf(context.resources.getString(R.string.enqueue_on_background), context.resources.getString(R.string.enqueue_on_popup), context.resources.getString(R.string.start_here_on_main), context.resources.getString(R.string.start_here_on_background), context.resources.getString(R.string.start_here_on_popup), context.resources.getString(R.string.set_as_playlist_thumbnail), context.resources.getString(R.string.delete), context.resources.getString(R.string.share))

        val actions = DialogInterface.OnClickListener { dialog, which ->
            val index = Math.max(itemListAdapter!!.itemsList.indexOf(item), 0)
            when (which) {
                0 -> NavigationHelper.enqueueOnBackgroundPlayer(context, SinglePlayQueue(infoItem))
                1 -> NavigationHelper.enqueueOnPopupPlayer(activity, SinglePlayQueue(infoItem))
                2 -> NavigationHelper.playOnMainPlayer(context, getPlayQueue(index))
                3 -> NavigationHelper.playOnBackgroundPlayer(context, getPlayQueue(index))
                4 -> NavigationHelper.playOnPopupPlayer(activity, getPlayQueue(index))
                5 -> changeThumbnailUrl(item.thumbnailUrl)
                6 -> deleteItem(item)
                7 -> shareUrl(item.toStreamInfoItem().name, item.toStreamInfoItem().url)
                else -> {
                }
            }
        }

        InfoItemDialog(getActivity()!!, infoItem, commands, actions).show()
    }

    private fun setInitialData(playlistId: Long, name: String) {
        this.playlistId = playlistId
        this.name = if (!TextUtils.isEmpty(name)) name else ""
    }

    private fun setVideoCount(count: Long) {
        if (activity != null && headerStreamCount != null) {
            headerStreamCount!!.text = Localization.localizeStreamCount(activity!!, count)
        }
    }

    private fun getPlayQueue(index: Int): PlayQueue {
        if (itemListAdapter == null) {
            return SinglePlayQueue(emptyList(), 0)
        }

        val infoItems = itemListAdapter!!.itemsList
        val streamInfoItems = ArrayList<StreamInfoItem>(infoItems.size)
        for (item in infoItems) {
            if (item is PlaylistStreamEntry) {
                streamInfoItems.add(item.toStreamInfoItem())
            }
        }
        return SinglePlayQueue(streamInfoItems, index)
    }

    companion object {

        // Save the list 10 seconds after the last change occurred
        private const val SAVE_DEBOUNCE_MILLIS: Long = 10000
        private const val MINIMUM_INITIAL_DRAG_VELOCITY = 12

        fun getInstance(playlistId: Long, name: String): LocalPlaylistFragment {
            val instance = LocalPlaylistFragment()
            instance.setInitialData(playlistId, name)
            return instance
        }
    }
}
