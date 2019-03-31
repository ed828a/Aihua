package com.dew.aihua.ui.fragment


import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.dew.aihua.R
import com.dew.aihua.data.local.database.LocalItem
import com.dew.aihua.data.local.database.stream.model.StreamStatisticsEntry
import com.dew.aihua.ui.dialog.InfoItemDialog
import com.dew.aihua.data.local.manoeuvre.HistoryRecordManager
import com.dew.aihua.player.helper.ThemeHelper
import com.dew.aihua.player.playerUI.PopupVideoPlayer.Companion.ACTION_CLOSE
import com.dew.aihua.player.playqueque.queque.PlayQueue
import com.dew.aihua.player.playqueque.queque.SinglePlayQueue
import com.dew.aihua.report.UserAction
import com.dew.aihua.util.NavigationHelper
import com.dew.aihua.util.OnClickGesture
import com.google.android.material.snackbar.Snackbar
import icepick.State
import io.reactivex.android.schedulers.AndroidSchedulers
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import java.util.ArrayList

/**
 *  Created by Edward on 2/23/2019.
 */

class StatisticsPlaylistFragment : BaseLocalListFragment<List<StreamStatisticsEntry>, Void>() {

    private var headerPlayAllButton: View? = null
    private var headerPopupButton: View? = null
    private var headerBackgroundButton: View? = null
    private var playlistCtrl: View? = null
    private var sortButton: View? = null
    private var sortButtonIcon: ImageView? = null
    private var sortButtonText: TextView? = null

    @State
    @JvmField
    var itemsListState: Parcelable? = null

    /* Used for independent events */
    private var databaseSubscription: Subscription? = null
    private var recordManager: HistoryRecordManager? = null
//    private val disposables = CompositeDisposable()

    private var sortMode = StatisticSortMode.LAST_PLAYED

    private val playQueue: PlayQueue
        get() = getPlayQueue(0)

    enum class StatisticSortMode {
        LAST_PLAYED,
        MOST_PLAYED
    }

    private fun processResult(results: List<StreamStatisticsEntry>): List<StreamStatisticsEntry>? {
        val convertList: MutableList<StreamStatisticsEntry> = arrayListOf<StreamStatisticsEntry>()
        convertList.addAll(results)
        when (sortMode) {
            StatisticSortMode.LAST_PLAYED -> {
                convertList.sortWith(Comparator { left, right ->
                    right.latestAccessDate.compareTo(left.latestAccessDate)
                })
            }

            StatisticSortMode.MOST_PLAYED -> {
                convertList.sortWith(Comparator { left, right ->
                    java.lang.Long.compare(right.watchCount, left.watchCount)
                })
            }
        }

        return convertList.toList()
    }

    ///////////////////////////////////////////////////////////////////////////
    // Fragment LifeCycle - Creation
    ///////////////////////////////////////////////////////////////////////////

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        context?.let {
            recordManager = HistoryRecordManager(it)
        }

    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_playlist, container, false)

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        if (activity != null && isVisibleToUser) {
            setTitle(activity!!.getString(R.string.title_activity_history))
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Fragment LifeCycle - Views
    ///////////////////////////////////////////////////////////////////////////

    override fun initViews(rootView: View, savedInstanceState: Bundle?) {
        super.initViews(rootView, savedInstanceState)
        if (!isUsedAsFrontPage) {
            setTitle(getString(R.string.title_last_played))
        }
    }

    override fun getListHeader(): View? {
        val headerRootLayout = activity!!.layoutInflater.inflate(R.layout.statistic_playlist_control,
            itemsList, false)
        playlistCtrl = headerRootLayout.findViewById(R.id.playlist_control)
        headerPlayAllButton = headerRootLayout.findViewById(R.id.playlist_ctrl_play_all_button)
        headerPopupButton = headerRootLayout.findViewById(R.id.playlist_ctrl_play_popup_button)
        headerBackgroundButton = headerRootLayout.findViewById(R.id.playlist_ctrl_play_bg_button)
        sortButton = headerRootLayout.findViewById(R.id.sortButton)
        sortButtonIcon = headerRootLayout.findViewById(R.id.sortButtonIcon)
        sortButtonText = headerRootLayout.findViewById(R.id.sortButtonText)
        return headerRootLayout
    }

    override fun initListeners() {
        super.initListeners()

        itemListAdapter?.setSelectedListener(object : OnClickGesture<LocalItem>() {
            override fun selected(selectedItem: LocalItem) {
                if (selectedItem is StreamStatisticsEntry) {
                    context?.applicationContext?.sendBroadcast(Intent(ACTION_CLOSE))
                    showLoading()
                    NavigationHelper.openAnchorPlayer(activity!!, selectedItem.serviceId, selectedItem.url, selectedItem.title)

//                    NavigationHelper.openVideoDetailFragment(getFM(), selectedItem.serviceId, selectedItem.url, selectedItem.title)
                }
            }

            override fun held(selectedItem: LocalItem) {
                if (selectedItem is StreamStatisticsEntry) {
                    showStreamDialog(selectedItem)
                }
            }
        })
    }

    ///////////////////////////////////////////////////////////////////////////
    // Fragment LifeCycle - Loading
    ///////////////////////////////////////////////////////////////////////////

    override fun startLoading(forceLoad: Boolean) {
        super.startLoading(forceLoad)
        recordManager!!.streamStatistics
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(getHistoryObserver())
    }

    ///////////////////////////////////////////////////////////////////////////
    // Fragment LifeCycle - Destruction
    ///////////////////////////////////////////////////////////////////////////

    override fun onResume() {
        super.onResume()
        hideLoading()
    }

    override fun onPause() {
        super.onPause()
        itemsListState = itemsList!!.layoutManager!!.onSaveInstanceState()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        itemListAdapter?.unsetSelectedListener()
        headerBackgroundButton?.setOnClickListener(null)
        headerPlayAllButton?.setOnClickListener(null)
        headerPopupButton?.setOnClickListener(null)

        databaseSubscription?.cancel()
        databaseSubscription = null
    }

    override fun onDestroy() {
        super.onDestroy()
        recordManager = null
        itemsListState = null
    }

    ///////////////////////////////////////////////////////////////////////////
    // Statistics Loader
    ///////////////////////////////////////////////////////////////////////////

    private fun getHistoryObserver(): Subscriber<List<StreamStatisticsEntry>> =
        object : Subscriber<List<StreamStatisticsEntry>> {
            override fun onSubscribe(subscription: Subscription) {
                showLoading()

                databaseSubscription?.cancel()
                databaseSubscription = subscription
                databaseSubscription?.request(1)
            }

            override fun onNext(streams: List<StreamStatisticsEntry>) {
                handleResult(streams)
                databaseSubscription?.request(1)
            }

            override fun onError(exception: Throwable) {
                this@StatisticsPlaylistFragment.onError(exception)
            }

            override fun onComplete() {}
        }


    override fun handleResult(result: List<StreamStatisticsEntry>) {
        super.handleResult(result)
        if (itemListAdapter == null) return

        playlistCtrl?.visibility = View.VISIBLE

        itemListAdapter?.clearStreamItemList()

        if (result.isEmpty()) {
            showEmptyState()
            return
        }

        // display history
        itemListAdapter?.addItems(processResult(result))
        if (itemsListState != null) {
            itemsList?.layoutManager?.onRestoreInstanceState(itemsListState)
            itemsListState = null
        }

        headerPlayAllButton?.setOnClickListener { NavigationHelper.playOnMainPlayer(activity, playQueue) }
        headerPopupButton?.setOnClickListener { NavigationHelper.playOnPopupPlayer(activity, playQueue) }
        headerBackgroundButton?.setOnClickListener { NavigationHelper.playOnBackgroundPlayer(activity, playQueue) }
        sortButton?.setOnClickListener { toggleSortMode() }

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
            "none", "History Statistics", R.string.general_error)
        return true
    }

    ///////////////////////////////////////////////////////////////////////////
    // Utils
    ///////////////////////////////////////////////////////////////////////////

    private fun toggleSortMode() {
        when (sortMode) {
            StatisticSortMode.LAST_PLAYED -> {
                sortMode = StatisticSortMode.MOST_PLAYED
                setTitle(getString(R.string.title_most_played))
                sortButtonIcon?.setImageResource(ThemeHelper.getIconByAttr(R.attr.history, context!!))
                sortButtonText?.setText(R.string.title_last_played)
            }
            else -> {
                sortMode = StatisticSortMode.LAST_PLAYED
                setTitle(getString(R.string.title_last_played))
                sortButtonIcon?.setImageResource(ThemeHelper.getIconByAttr(R.attr.filter, context!!))
                sortButtonText?.setText(R.string.title_most_played)
            }
        }
        startLoading(true)
    }

    private fun showStreamDialog(item: StreamStatisticsEntry) {
        val context = context
        val activity = getActivity()
        if (context == null || context.resources == null || activity == null) return
        val infoItem = item.toStreamInfoItem()

        val commands = arrayOf(context.resources.getString(R.string.enqueue_on_background), context.resources.getString(R.string.enqueue_on_popup), context.resources.getString(R.string.start_here_on_main), context.resources.getString(R.string.start_here_on_background), context.resources.getString(R.string.start_here_on_popup), context.resources.getString(R.string.delete), context.resources.getString(R.string.share))

        val actions = DialogInterface.OnClickListener { _, which ->
            val index = Math.max(itemListAdapter!!.itemsList.indexOf(item), 0)
            when (which) {
                0 -> NavigationHelper.enqueueOnBackgroundPlayer(context, SinglePlayQueue(infoItem))
                1 -> NavigationHelper.enqueueOnPopupPlayer(activity, SinglePlayQueue(infoItem))
                2 -> NavigationHelper.playOnMainPlayer(context, getPlayQueue(index))
                3 -> NavigationHelper.playOnBackgroundPlayer(context, getPlayQueue(index))
                4 -> NavigationHelper.playOnPopupPlayer(activity, getPlayQueue(index))
                5 -> deleteEntry(index)
                6 -> shareUrl(item.toStreamInfoItem().name, item.toStreamInfoItem().url)
                else -> { }
            }
        }

        InfoItemDialog(activity, infoItem, commands, actions).show()
    }

    private fun deleteEntry(index: Int) {
        if (itemListAdapter == null) return
        val infoItem = itemListAdapter!!.itemsList[index]
        if (infoItem is StreamStatisticsEntry) {
            val onDelete = recordManager!!.deleteStreamHistory(infoItem.streamId)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { howManyDeleted ->
                        if (view != null) {
                            Snackbar.make(view!!, "$howManyDeleted item deleted.",
                                Snackbar.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context,
                                R.string.one_item_deleted,
                                Toast.LENGTH_SHORT).show()
                        }
                    },
                    { throwable ->
                        showSnackBarError(throwable,
                            UserAction.DELETE_FROM_HISTORY, "none",
                            "Deleting item failed", R.string.general_error)
                    })

            compositeDisposable.add(onDelete)
        }
    }

    private fun getPlayQueue(index: Int): PlayQueue {
        if (itemListAdapter == null) {
            return SinglePlayQueue(emptyList(), 0)
        }

        val infoItems = itemListAdapter!!.itemsList
        val streamInfoItems = ArrayList<StreamInfoItem>(infoItems.size)
        for (item in infoItems) {
            if (item is StreamStatisticsEntry) {
                streamInfoItems.add(item.toStreamInfoItem())
            }
        }

        return SinglePlayQueue(streamInfoItems, index)
    }
}
