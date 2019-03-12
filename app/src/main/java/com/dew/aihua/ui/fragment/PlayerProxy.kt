package com.dew.aihua.ui.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import com.dew.aihua.R
import com.dew.aihua.infolist.adapter.InfoItemBuilder
import com.dew.aihua.infolist.adapter.InfoListAdapter
import com.dew.aihua.player.helper.Constants
import com.dew.aihua.player.helper.ExtractorHelper
import com.dew.aihua.player.helper.ListHelper
import com.dew.aihua.player.helper.PermissionHelper
import com.dew.aihua.player.playerUI.MainVideoPlayer
import com.dew.aihua.player.playerUI.PopupVideoPlayer
import com.dew.aihua.player.playqueque.queque.SinglePlayQueue
import com.dew.aihua.ui.model.StackItem
import com.dew.aihua.util.NavigationHelper
import com.dew.aihua.util.OnClickGesture
import icepick.State
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.exceptions.ContentNotAvailableException
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.services.youtube.extractors.YoutubeStreamExtractor
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.stream.VideoStream
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 *  Created by Edward on 3/12/2019.
 */
class PlayerProxy(val context: Context) {

    private lateinit var infoItemBuilder: InfoItemBuilder

    private var updateFlags = 0

    private var showRelatedStreams: Boolean = false

    @State
    @JvmField
    var serviceId = Constants.NO_SERVICE_ID
    @State
    @JvmField
    var name: String? = null
    @State
    @JvmField
    var url: String? = null

    private var currentInfo: StreamInfo? = null
    private var currentWorker: Disposable? = null

    private var sortedVideoStreams: List<VideoStream>? = null
    private var selectedVideoStreamIndex = -1


    ///////////////////////////////////////////////////////////////////////////
    // OwnStack
    ///////////////////////////////////////////////////////////////////////////

    /**
     *  Stack that contains the "navigation history"
     *  The peek is the current video. */
    private val stack = LinkedList<StackItem>()


    private val selectedVideoStream: VideoStream?
        get() = if (sortedVideoStreams != null) sortedVideoStreams!![selectedVideoStreamIndex] else null

    private var infoListAdapter: InfoListAdapter? = null
    private val relatedStreams: LinkedList<InfoItem> = LinkedList()

    private var isLoading: AtomicBoolean = AtomicBoolean()
    private var wasLoading = AtomicBoolean()
    private val compositeDisposable = CompositeDisposable()
    ///////////////////////////////////////////////////////////////////////////
    // Fragment's Lifecycle
    ///////////////////////////////////////////////////////////////////////////

    fun initialize() {

        if (updateFlags != 0) {
            if (!isLoading.get() && currentInfo != null) {
                if (updateFlags and RELATED_STREAMS_UPDATE_FLAG != 0) initRelatedVideos(currentInfo!!)
                if (updateFlags and RESOLUTIONS_MENU_UPDATE_FLAG != 0) setupActionBar(currentInfo!!)
            }

            updateFlags = 0
        }
        if (wasLoading.getAndSet(false)) {
            selectAndLoadVideo(serviceId, url!!, name!!)
        }
        currentWorker?.dispose()
        if (!compositeDisposable.isDisposed) compositeDisposable.dispose()
        currentWorker = null
    }


    ///////////////////////////////////////////////////////////////////////////
    // OnClick
    ///////////////////////////////////////////////////////////////////////////
//    override fun onClick(view: View) {
//        if (isLoading.get() || currentInfo == null) return
//
//        when (view.id) {
//            R.id.detail_controls_background -> openBackgroundPlayer(false)
//            R.id.detail_controls_popup -> openPopupPlayer(false)
//            R.id.detail_controls_playlist_append -> if (fragmentManager != null && currentInfo != null) {
//                PlaylistAppendDialog.fromStreamInfo(currentInfo!!)
//                    .show(fragmentManager!!, TAG)
//            }
//            R.id.detail_controls_download -> if (PermissionHelper.checkStoragePermissions(
//                    activity!!,
//                    PermissionHelper.DOWNLOAD_DIALOG_REQUEST_CODE
//                )
//            ) {
//                this.openDownloadDialog()
//            }
//            R.id.detail_uploader_root_layout -> if (TextUtils.isEmpty(currentInfo!!.uploaderUrl)) {
//                Log.w(TAG, "Can't open channel because we got no channel URL")
//            } else {
//                try {
//                    NavigationHelper.openChannelFragment(
//                        fragmentManager,
//                        currentInfo!!.serviceId,
//                        currentInfo!!.uploaderUrl,
//                        currentInfo!!.uploaderName
//                    )
//                } catch (e: Exception) {
//                    val context = getActivity()
//                    context.let {
//                        ErrorActivity.reportUiError(it as AppCompatActivity, e)
//                    }
//
//                }
//
//            }
//            R.id.detail_thumbnail_root_layout -> {
//                view.context.applicationContext.sendBroadcast(Intent(PopupVideoPlayer.ACTION_CLOSE))
//                if (currentInfo!!.videoStreams.isEmpty() && currentInfo!!.videoOnlyStreams.isEmpty()) {
//                    openBackgroundPlayer(false)
//                } else {
//                    openVideoPlayer()
//                }
//            }
//            R.id.detail_title_root_layout -> toggleTitleAndDescription()
//        }
//    }

//    override fun onLongClick(view: View): Boolean {
//        if (isLoading.get() || currentInfo == null) return false
//
//        when (view.id) {
//            R.id.detail_controls_background -> openBackgroundPlayer(true)
//            R.id.detail_controls_popup -> openPopupPlayer(true)
//            R.id.detail_controls_download -> {
//                val activity = getActivity()
//                activity?.let {
//                    NavigationHelper.openDownloads(it)
//                }
//            }
//        }
//
//        return true
//    }

    ///////////////////////////////////////////////////////////////////////////
    // Init
    ///////////////////////////////////////////////////////////////////////////

//     fun initViews(rootView: View, savedInstanceState: Bundle?) {
    // setup recyclerView
//        infoListAdapter = InfoListAdapter(context)
//        infoListAdapter?.setGridItemVariants(isGridLayout)
//        infoItemBuilder = InfoItemBuilder(activity!!)
//    }

    @SuppressLint("ClickableViewAccessibility")  // for blind people
    fun initListeners() {
        infoItemBuilder.onStreamSelectedListener = object : OnClickGesture<StreamInfoItem>() {
            override fun selected(selectedItem: StreamInfoItem) {
                Log.d(TAG, "initListeners(): selected() called")
                selectAndLoadVideo(selectedItem.serviceId, selectedItem.url, selectedItem.name)
            }

            override fun held(selectedItem: StreamInfoItem) {
                showStreamDialog(selectedItem)
            }
        }

        infoListAdapter?.setOnStreamSelectedListener(object : OnClickGesture<StreamInfoItem>() {
            override fun selected(selectedItem: StreamInfoItem) {
//                onStreamSelected(selectedItem)
                directlyPlayVideoAnchorPlayer(selectedItem)
            }

            override fun held(selectedItem: StreamInfoItem) {
                showStreamDialog(selectedItem)
            }
        })
    }

    private fun showStreamDialog(item: StreamInfoItem) {
        val context = context
//        if (context == null || context.resources == null || getActivity() == null) return

        val commands = arrayOf(
            context.resources.getString(R.string.enqueue_on_background),
            context.resources.getString(R.string.enqueue_on_popup),
            context.resources.getString(R.string.append_playlist),
            context.resources.getString(R.string.share)
        )

//        val actions = DialogInterface.OnClickListener { _, which: Int ->
//            when (which) {
//                0 -> NavigationHelper.enqueueOnBackgroundPlayer(context, SinglePlayQueue(item))
//                1 -> NavigationHelper.enqueueOnPopupPlayer(getActivity(), SinglePlayQueue(item))
//                2 -> if (fragmentManager != null) {
//                    PlaylistAppendDialog.fromStreamInfoItems(listOf(item))
//                        .show(fragmentManager!!, TAG)
//                }
//                3 -> shareUrl(item.name, item.url)
//                else -> {
//                }
//            }
//        }
//
//        InfoItemDialog(getActivity()!!, item, commands, actions).show()
    }


    private fun initRelatedVideos(info: StreamInfo) {
        relatedStreams.clear()
        relatedStreams.addAll(info.relatedStreams)
        Log.d(TAG, "initRelatedVideos(): relatedStreams.size = ${relatedStreams.size}")
    }

    ///////////////////////////////////////////////////////////////////////////
    // Menu
    ///////////////////////////////////////////////////////////////////////////

    private fun setupActionBar(info: StreamInfo) {
        Log.d(TAG, "setupActionBarHandler(): info = [$info]")

        sortedVideoStreams =
            ListHelper.getSortedStreamVideosList(context, info.videoStreams, info.videoOnlyStreams, false)
        selectedVideoStreamIndex = ListHelper.getDefaultResolutionIndex(context, sortedVideoStreams!!)

    }

    private fun pushToStack(serviceId: Int, videoUrl: String, name: String?) {
        Log.d(TAG, "pushToStack() called with: serviceId = [$serviceId], videoUrl = [$videoUrl], name = [$name]")

        if (stack.size > 0 && stack.peek().serviceId == serviceId && stack.peek().url == videoUrl) {
            Log.d(
                TAG,
                "pushToStack() called with: serviceId == peek.serviceId = [$serviceId], videoUrl == peek.getUrl = [$videoUrl]"
            )
        } else {
            Log.d(TAG, "pushToStack() when no stackItem has equal serviceId and videoUrl")
            stack.push(StackItem(serviceId, videoUrl, name))
        }
    }

    private fun setStackItemTitleToUrl(serviceId: Int, videoUrl: String, name: String?) {
        if (name != null && !name.isEmpty()) {
            for (stackItem in stack) {
                if (stack.peek().serviceId == serviceId && stackItem.url == videoUrl) {
                    stackItem.title = name
                }
            }
        }
    }

    fun onBackPressed(): Boolean {
        Log.d(TAG, "onBackPressed() called")
        // That means that we are on the start of the stack,
        // return false to let the MainActivity handle the onBack
        if (stack.size <= 1) return false
        // Remove top
        stack.pop()
        // Get stack item getTabFrom the new top
        val peek = stack.peek()

        selectAndLoadVideo(peek.serviceId, peek.url, if (!TextUtils.isEmpty(peek.title)) peek.title!! else "")
        return true
    }

    ///////////////////////////////////////////////////////////////////////////
    // Info loading and handling
    ///////////////////////////////////////////////////////////////////////////
    fun doInitialLoadLogic() {
        if (currentInfo == null)
            prepareAndLoadInfo()
        else
            prepareAndHandleInfo(currentInfo!!, false)
    }

    fun selectAndLoadVideo(serviceId: Int, videoUrl: String?, name: String) {
        setInitialData(serviceId, videoUrl, name)
        prepareAndLoadInfo()
    }

    private fun prepareAndHandleInfo(info: StreamInfo, scrollToTop: Boolean) {
        Log.d(TAG, "prepareAndHandleInfo() called with: info = [$info], scrollToTop = [$scrollToTop]")

        setInitialData(info.serviceId, info.originalUrl, info.name)
        pushToStack(serviceId, url!!, name)

        handleResult(info)
    }


    private fun prepareAndLoadInfo() {
//        pushToStack(serviceId, url!!, name)
        startLoading(false)
    }

    fun startLoading(forceLoad: Boolean) {

        pushToStack(serviceId, url!!, name)

        currentInfo = null
        currentWorker?.dispose()

        url?.let { url ->
            currentWorker = ExtractorHelper.getStreamInfo(serviceId, url, forceLoad)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { result: StreamInfo ->
                        isLoading.set(false)
                        currentInfo = result
                        handleResult(result)
                    },
                    { throwable: Throwable ->
                        isLoading.set(false)
                        onError(throwable)
                    })
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Play Utils
    ///////////////////////////////////////////////////////////////////////////

    private fun openBackgroundPlayer(append: Boolean) {
        openNormalBackgroundPlayer(append)
    }

    private fun openPopupPlayer(append: Boolean) {
        if (!PermissionHelper.isPopupEnabled(context)) {
            PermissionHelper.showPopupEnablementToast(context)
            return
        }

        val itemQueue = SinglePlayQueue(currentInfo!!)
        if (append) {
            NavigationHelper.enqueueOnPopupPlayer(context, itemQueue)
        } else {
            Toast.makeText(context, R.string.popup_playing_toast, Toast.LENGTH_SHORT).show()
            val intent = NavigationHelper.getPlayerIntent(
                context, PopupVideoPlayer::class.java, itemQueue, selectedVideoStream!!.resolution
            )
            context.startService(intent)
        }
    }

    private fun openVideoPlayer() {
        context.sendBroadcast(Intent(PopupVideoPlayer.ACTION_CLOSE))
        val selectedVideoStream = selectedVideoStream

        openNormalPlayer(selectedVideoStream)

    }

    private fun openNormalPlayer(selectedVideoStream: VideoStream?) {
        // using ExoPlayer
        val playQueue = SinglePlayQueue(currentInfo!!)
        val intent: Intent = NavigationHelper.getPlayerIntent(
            context,
            MainVideoPlayer::class.java,
            playQueue,
            selectedVideoStream!!.getResolution()
        )

        context.startActivity(intent)
    }

    private fun openNormalBackgroundPlayer(append: Boolean) {
        val itemQueue = SinglePlayQueue(currentInfo!!)
        if (append) {
            NavigationHelper.enqueueOnBackgroundPlayer(context, itemQueue)
        } else {
            NavigationHelper.playOnBackgroundPlayer(context, itemQueue)
        }
    }


    ///////////////////////////////////////////////////////////////////////////
    // Utils
    ///////////////////////////////////////////////////////////////////////////


    private fun setInitialData(serviceId: Int, url: String?, name: String) {
        this.serviceId = serviceId
        this.url = url
        this.name = if (!TextUtils.isEmpty(name)) name else ""
    }


    ///////////////////////////////////////////////////////////////////////////
    // Contract
    ///////////////////////////////////////////////////////////////////////////


    fun handleResult(result: StreamInfo): Boolean {
        Log.d(TAG, "handleResult() called: result = $result")

        setInitialData(result.serviceId, result.originalUrl, result.name)
        pushToStack(serviceId, url!!, name)

//        setupActionBar(result)
        sortedVideoStreams = ListHelper.getSortedStreamVideosList(
            context,
            result.videoStreams,
            result.videoOnlyStreams,
            false
        )
        selectedVideoStreamIndex = ListHelper.getDefaultResolutionIndex(context, sortedVideoStreams!!)


//        initRelatedVideos(result)
        relatedStreams.clear()
        relatedStreams.addAll(result.relatedStreams)

        setStackItemTitleToUrl(result.serviceId, result.url, result.name)
        setStackItemTitleToUrl(result.serviceId, result.originalUrl, result.name)

        return result.errors.isEmpty()
//        if (!result.errors.isEmpty()) {
//            Log.d(TAG, "Result Error: ${result.errors}, UserAction.REQUESTED_STREAM, result.url = ${result.url}")
//        }
//
//        openVideoPlayer()
    }

    ///////////////////////////////////////////////////////////////////////////
    // Stream Results
    ///////////////////////////////////////////////////////////////////////////

    fun onError(exception: Throwable): Boolean {

        when (exception) {
            is YoutubeStreamExtractor.GemaException -> {
                Toast.makeText(context, context.getString(R.string.blocked_by_gema), Toast.LENGTH_SHORT).show()
            }

            is ContentNotAvailableException -> {
                Toast.makeText(context, context.getString(R.string.content_not_available), Toast.LENGTH_SHORT).show()
            }

            is ParsingException -> {
                Toast.makeText(context, context.getString(R.string.parsing_error), Toast.LENGTH_SHORT).show()
            }

            is YoutubeStreamExtractor.DecryptException -> {
                Toast.makeText(context, context.getString(R.string.youtube_signature_decryption_error), Toast.LENGTH_SHORT)
                    .show()
            }

            else -> {
                Toast.makeText(context, context.getString(R.string.general_error), Toast.LENGTH_SHORT).show()
            }
        }
        NavigationHelper.openMainActivity(context)
        return true
    }


    fun directlyPlayVideoAnchorPlayer(selectedItem: StreamInfoItem) {
        setInitialData(selectedItem.serviceId, selectedItem.url, selectedItem.name)
        pushToStack(selectedItem.serviceId, selectedItem.url, selectedItem.name)
        currentInfo = null
        currentWorker?.dispose()

        val errorMessage = arrayListOf<Throwable>()
        url?.let { url ->
            currentWorker = ExtractorHelper.getStreamInfo(selectedItem.serviceId, selectedItem.url, false)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.computation())
                .flatMap { result: StreamInfo ->
                    isLoading.set(false)
                    currentInfo = result
                    errorMessage.addAll(result.errors)
                    Single.fromCallable {
                        Log.d(TAG, "directlyPlayVideoAnchorPlayer(): Single.fromCallable called")
                        handleResult(result)
                    }
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    {noError ->
                        Log.d(TAG, "directlyPlayVideoAnchorPlayer(): ready to play video with noError = $noError")
                        if (noError){
                            openVideoPlayer()
                        } else {
                            Log.d(TAG, "Result Error: $errorMessage, UserAction.REQUESTED_STREAM, result.url = $url")
                        }

                    },
                    { throwable: Throwable ->
                        isLoading.set(false)
                        onError(throwable)
                    })
        }
    }

    companion object {
        const val TAG = "PlayerProxy"
        // Amount of videos to show on start
        private const val RELATED_STREAMS_UPDATE_FLAG = 0x1
        private const val RESOLUTIONS_MENU_UPDATE_FLAG = 0x2

    }
}