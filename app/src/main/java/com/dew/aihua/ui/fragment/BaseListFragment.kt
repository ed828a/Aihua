package com.dew.aihua.ui.fragment


import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.dew.aihua.R
import com.dew.aihua.infolist.adapter.InfoItemDialog
import com.dew.aihua.infolist.adapter.InfoListAdapter
import com.dew.aihua.local.dialog.PlaylistAppendDialog
import com.dew.aihua.player.helper.AnimationUtils.animateView
import com.dew.aihua.player.helper.ExtractorHelper
import com.dew.aihua.player.playqueque.queque.SinglePlayQueue
import com.dew.aihua.report.ErrorActivity
import com.dew.aihua.ui.contract.ListViewContract
import com.dew.aihua.ui.contract.OnScrollBelowItemsListener
import com.dew.aihua.util.NavigationHelper
import com.dew.aihua.util.OnClickGesture
import com.dew.aihua.util.StateSaver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.channel.ChannelInfoItem
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItem
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import java.util.*

/**
 *  Created by Edward on 3/2/2019.
 */
abstract class BaseListFragment<I, N> : BaseStateFragment<I>(), ListViewContract<I, N>, StateSaver.WriteRead, SharedPreferences.OnSharedPreferenceChangeListener {

    ///////////////////////////////////////////////////////////////////////////
    // Views
    ///////////////////////////////////////////////////////////////////////////

    protected var infoListAdapter: InfoListAdapter? = null
    protected var itemsList: androidx.recyclerview.widget.RecyclerView? = null
    private var updateFlags = FLAG_NO_UPDATE

    ///////////////////////////////////////////////////////////////////////////
    // State Saving
    ///////////////////////////////////////////////////////////////////////////

    protected var savedState: StateSaver.SavedState? = null

    ///////////////////////////////////////////////////////////////////////////
    // Init
    ///////////////////////////////////////////////////////////////////////////

    protected open fun getListHeader(): View? = null

    protected open fun getListFooter(): View = activity!!.layoutInflater.inflate(R.layout.pignate_footer, itemsList, false)

    private fun getListLayoutManager(): androidx.recyclerview.widget.RecyclerView.LayoutManager = androidx.recyclerview.widget.LinearLayoutManager(activity)

    private fun getGridLayoutManager(): androidx.recyclerview.widget.RecyclerView.LayoutManager {
        val resources = activity!!.resources
        var width = resources.getDimensionPixelSize(R.dimen.video_item_grid_thumbnail_image_width)
        width += (24 * resources.displayMetrics.density).toInt()
        val spanCount = Math.floor(resources.displayMetrics.widthPixels / width.toDouble()).toInt()
        val layoutManager = androidx.recyclerview.widget.GridLayoutManager(activity, spanCount)
        layoutManager.spanSizeLookup = infoListAdapter!!.getSpanSizeLookup(spanCount)
        return layoutManager
    }


    ///////////////////////////////////////////////////////////////////////////
    // LifeCycle
    ///////////////////////////////////////////////////////////////////////////

    override fun onAttach(context: Context) {
        super.onAttach(context)
        infoListAdapter = InfoListAdapter(activity!!)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        PreferenceManager.getDefaultSharedPreferences(activity)
            .registerOnSharedPreferenceChangeListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        StateSaver.onDestroy(savedState)
        PreferenceManager.getDefaultSharedPreferences(activity)
            .unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onResume() {
        super.onResume()

        if (updateFlags != FLAG_NO_UPDATE) {
            if (updateFlags and LIST_MODE_UPDATE_FLAG != FLAG_NO_UPDATE) {
                val useGrid = isGridLayout
                Log.d(TAG, "onResume(): useGrid = $useGrid ")
                itemsList?.layoutManager = if (useGrid) getGridLayoutManager() else getListLayoutManager()
                infoListAdapter?.setGridItemVariants(useGrid)
                infoListAdapter?.notifyDataSetChanged()
            }
            updateFlags = FLAG_NO_UPDATE
        }
    }

    override fun generateSuffix(): String {
        // Naive solution, but it's good for now (the items don't change)
        return ".${infoListAdapter!!.itemsList.size}.list"
    }

    override fun writeTo(objectsToSave: Queue<Any>) {
        objectsToSave.add(infoListAdapter!!.itemsList)
    }

    @Throws(Exception::class)
    override fun readFrom(savedObjects: Queue<Any>) {
        infoListAdapter!!.itemsList.clear()

        @Suppress("UNCHECKED_CAST")
        infoListAdapter!!.itemsList.addAll(savedObjects.poll() as List<InfoItem>)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        savedState = StateSaver.tryToSave(activity!!.isChangingConfigurations, savedState, outState, this)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        savedState = StateSaver.tryToRestore(savedInstanceState, this)
    }

    override fun initViews(rootView: View, savedInstanceState: Bundle?) {
        super.initViews(rootView, savedInstanceState)

        val useGrid = isGridLayout
        itemsList = rootView.findViewById(R.id.items_list)
        itemsList!!.layoutManager = if (useGrid) getGridLayoutManager() else getListLayoutManager()

        infoListAdapter!!.setGridItemVariants(useGrid)
        infoListAdapter!!.setFooter(getListFooter())
        infoListAdapter!!.setHeader(getListHeader())

        itemsList!!.adapter = infoListAdapter
    }

    protected open fun onItemSelected(selectedItem: InfoItem) {
        Log.d(TAG, "onItemSelected() called with: selectedItem = [$selectedItem]")
    }

    override fun initListeners() {
        super.initListeners()
        infoListAdapter!!.setOnStreamSelectedListener(object : OnClickGesture<StreamInfoItem>() {
            override fun selected(selectedItem: StreamInfoItem) {
                onStreamSelected(selectedItem)
            }

            override fun held(selectedItem: StreamInfoItem) {
                showStreamDialog(selectedItem)
            }
        })

        infoListAdapter!!.setOnChannelSelectedListener(object : OnClickGesture<ChannelInfoItem>() {
            override fun selected(selectedItem: ChannelInfoItem) {
                try {
                    onItemSelected(selectedItem)
                    NavigationHelper.openChannelFragment(getFM(),
                        selectedItem.serviceId,
                        selectedItem.url,
                        selectedItem.name)
                } catch (e: Exception) {
                    val context = getActivity()
                    context?.let {
                        ErrorActivity.reportUiError(it as AppCompatActivity, e)
                    }
                }

            }
        })

        infoListAdapter!!.setOnPlaylistSelectedListener(object : OnClickGesture<PlaylistInfoItem>() {
            override fun selected(selectedItem: PlaylistInfoItem) {
                try {
                    onItemSelected(selectedItem)
                    NavigationHelper.openPlaylistFragment(getFM(),
                        selectedItem.serviceId,
                        selectedItem.url,
                        selectedItem.name)
                } catch (e: Exception) {
                    val context = getActivity()
                    context?.let{
                        ErrorActivity.reportUiError(it as AppCompatActivity, e)
                    }

                }

            }
        })

        itemsList!!.clearOnScrollListeners()
        itemsList!!.addOnScrollListener(object : OnScrollBelowItemsListener() {
            override fun onScrolledDown(recyclerView: androidx.recyclerview.widget.RecyclerView) {
                onScrollToBottom()
            }
        })
    }

    private fun onStreamSelected(selectedItem: StreamInfoItem) {
        Log.d(TAG, "onStreamSelected() called: autoPlay = true")
        onItemSelected(selectedItem)
        // no last parameter: true before
//        context?.sendBroadcast(Intent(PopupVideoPlayer.ACTION_CLOSE))
        // Todo: inset directly play and store the related-videos list.
        NavigationHelper.openVideoDetailFragment(getFM(), selectedItem.serviceId, selectedItem.url, selectedItem.name)
    }

    private fun directlyPlayAndStoreRelatedVideos(selectedItem: StreamInfoItem){
        var currentInfo: StreamInfo? = null
        val d = ExtractorHelper.getStreamInfo(selectedItem.serviceId, selectedItem.url, false)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { result ->
                    isLoading.set(false)
                    currentInfo = result
//                    showContentWithAnimation(120, 0, 0f)
//                    handleResult(result)
                },
                { throwable: Throwable ->
                    isLoading.set(false)
                    onError(throwable)
                })
    }

    protected fun onScrollToBottom() {
        if (hasMoreItems() && !isLoading.get()) {
            loadMoreItems()
        }
    }

    protected open fun showStreamDialog(item: StreamInfoItem) {
        val context = context
        val activity = getActivity()
        if (context == null || context.resources == null || getActivity() == null) return

        val commands = arrayOf(context.resources.getString(R.string.enqueue_on_background), context.resources.getString(R.string.enqueue_on_popup), context.resources.getString(R.string.append_playlist), context.resources.getString(R.string.share))

        val actions = DialogInterface.OnClickListener { _, which ->
            when (which) {
                0 -> NavigationHelper.enqueueOnBackgroundPlayer(context, SinglePlayQueue(item))
                1 -> NavigationHelper.enqueueOnPopupPlayer(activity, SinglePlayQueue(item))
                2 -> if (fragmentManager != null) {
                    PlaylistAppendDialog.fromStreamInfoItems(listOf(item))
                        .show(fragmentManager!!, TAG)
                }
                3 -> shareUrl(item.name, item.url)
                else -> {
                }
            }
        }

        InfoItemDialog(getActivity()!!, item, commands, actions).show()
    }

    ///////////////////////////////////////////////////////////////////////////
    // Menu
    ///////////////////////////////////////////////////////////////////////////

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        Log.d(TAG, "onCreateOptionsMenu() called with: menu = [$menu], inflater = [$inflater]")
        super.onCreateOptionsMenu(menu, inflater)
        val supportActionBar = activity!!.supportActionBar
        if (supportActionBar != null) {
            supportActionBar.setDisplayShowTitleEnabled(true)
            if (useAsFrontPage) {
                supportActionBar.setDisplayHomeAsUpEnabled(false)
            } else {
                supportActionBar.setDisplayHomeAsUpEnabled(true)
            }
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Load and handle
    ///////////////////////////////////////////////////////////////////////////

    protected abstract fun loadMoreItems()

    protected abstract fun hasMoreItems(): Boolean

    ///////////////////////////////////////////////////////////////////////////
    // Contract
    ///////////////////////////////////////////////////////////////////////////

    override fun showLoading() {
        super.showLoading()
        // animateView(itemsList, false, 400);
    }

    override fun hideLoading() {
        super.hideLoading()
        animateView(itemsList!!, true, 300)
    }

    override fun showError(message: String, showRetryButton: Boolean) {
        super.showError(message, showRetryButton)
        showListFooter(false)
        animateView(itemsList!!, false, 200)
    }

    override fun showEmptyState() {
        super.showEmptyState()
        showListFooter(false)
    }

    override fun showListFooter(show: Boolean) {
        itemsList!!.post {
            if (infoListAdapter != null && itemsList != null) {
                infoListAdapter!!.showFooter(show)
            }
        }
    }

    override fun handleNextItems(result: N) {
        isLoading.set(false)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        if (key == getString(R.string.list_view_mode_key)) {
            updateFlags = updateFlags or LIST_MODE_UPDATE_FLAG
        }
    }

    companion object {
        private const val FLAG_NO_UPDATE = 0
        private const val LIST_MODE_UPDATE_FLAG = 0x32
    }
}
