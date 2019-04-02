package com.dew.aihua.ui.new_fragment

import android.content.Context
import android.content.DialogInterface
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.dew.aihua.R
import com.dew.aihua.player.playqueque.queque.SinglePlayQueue
import com.dew.aihua.report.ErrorActivity
import com.dew.aihua.ui.adapter.NewInfoListAdapter
import com.dew.aihua.ui.contract.OnScrollBelowItemsListener
import com.dew.aihua.ui.dialog.InfoItemDialog
import com.dew.aihua.ui.local.dialog.PlaylistAppendDialog
import com.dew.aihua.util.NavigationHelper
import com.dew.aihua.util.OnClickGesture
import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.channel.ChannelInfoItem
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItem
import org.schabi.newpipe.extractor.stream.StreamInfoItem


/**
 *  Created by Edward on 3/2/2019.
 */
abstract class NewBaseListFragment<I, N> : GeneralListFragment<I, N, InfoItem, NewInfoListAdapter>() {

    override var listAdapter: NewInfoListAdapter? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listAdapter = NewInfoListAdapter(activity!!)
    }

    protected open fun onItemSelected(selectedItem: InfoItem) {
        Log.d(TAG, "onItemSelected() called with: selectedItem = [$selectedItem]")
        showLoading()
    }

    override fun initListeners() {
        super.initListeners()
        if (listAdapter == null) listAdapter = NewInfoListAdapter(activity!!)
        listAdapter!!.setOnStreamSelectedListener(object : OnClickGesture<StreamInfoItem>() {
            override fun selected(selectedItem: StreamInfoItem) {
                onStreamSelected(selectedItem)
            }

            override fun held(selectedItem: StreamInfoItem) {
                showStreamDialog(selectedItem)
            }
        })

        listAdapter!!.setOnChannelSelectedListener(object : OnClickGesture<ChannelInfoItem>() {
            override fun selected(selectedItem: ChannelInfoItem) {
                try {
                    onItemSelected(selectedItem)
                    NavigationHelper.openChannelFragment(
                        getFM(),
                        selectedItem.serviceId,
                        selectedItem.url,
                        selectedItem.name
                    )
                } catch (e: Exception) {
                    val context = getActivity()
                    context?.let {
                        ErrorActivity.reportUiError(it as AppCompatActivity, e)
                    }
                }

            }
        })

        listAdapter!!.setOnPlaylistSelectedListener(object : OnClickGesture<PlaylistInfoItem>() {
            override fun selected(selectedItem: PlaylistInfoItem) {
                try {
                    onItemSelected(selectedItem)
                    NavigationHelper.openPlaylistFragment(
                        getFM(),
                        selectedItem.serviceId,
                        selectedItem.url,
                        selectedItem.name
                    )
                } catch (e: Exception) {
                    val context = getActivity()
                    context?.let {
                        ErrorActivity.reportUiError(it as AppCompatActivity, e)
                    }
                }
            }
        })

        itemsList!!.clearOnScrollListeners()
        itemsList!!.addOnScrollListener(object : OnScrollBelowItemsListener() {
            override fun onScrolledDown(recyclerView: RecyclerView) {
                onScrollToBottom()
            }
        })
    }

    private fun onStreamSelected(selectedItem: StreamInfoItem) {
        Log.d(TAG, "onStreamSelected() called: autoPlay = true")
//        onItemSelected(selectedItem)
        // no last parameter: true before
//        context?.sendBroadcast(Intent(PopupVideoPlayer.ACTION_CLOSE))
        // Todo: insert directly play and store the related-videos list.
        if (selectedItem.url != null && selectedItem.name != null) {
            showLoading()
            actionOnSelectedValidStream(selectedItem)

        } else {
            Log.d(
                TAG,
                "onStreamSelected() Error: selectedItem.url = ${selectedItem.url}, selectedItem.name = ${selectedItem.name} "
            )
        }
    }

    abstract fun actionOnSelectedValidStream(selectedItem: StreamInfoItem)
    // Todo 4: on the concrete class, this function can do either play selected stream directly or show the details of the select stream and its related videos
//    NavigationHelper.openAnchorPlayer(activity!!, selectedItem.serviceId, selectedItem.url, selectedItem.name)
//    NavigationHelper.openVideoDetailFragment(getFM(), selectedItem.serviceId, selectedItem.url, selectedItem.name)

    protected fun onScrollToBottom() {
        if (hasMoreItems() && !isLoading.get()) {
            loadMoreItems()
        }
    }

    protected open fun showStreamDialog(item: StreamInfoItem) {
        val context = context
        val activity = getActivity()
        if (context == null || context.resources == null || getActivity() == null) return

        val commands = arrayOf(
            context.resources.getString(R.string.enqueue_on_background),
            context.resources.getString(R.string.enqueue_on_popup),
            context.resources.getString(R.string.append_playlist),
            context.resources.getString(R.string.share)
        )

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
    // Load and handle
    ///////////////////////////////////////////////////////////////////////////

    protected abstract fun loadMoreItems()

    protected abstract fun hasMoreItems(): Boolean

    companion object {
        private const val TAG = "BaseListFragment"
    }
}
