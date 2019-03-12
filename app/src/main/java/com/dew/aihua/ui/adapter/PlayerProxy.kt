package com.dew.aihua.ui.adapter

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

    private val relatedStreams: LinkedList<InfoItem> = LinkedList()

    private var isLoading: AtomicBoolean = AtomicBoolean()

    private fun pushToStack(serviceId: Int, videoUrl: String, name: String) {
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


    ///////////////////////////////////////////////////////////////////////////
    // Info loading and handling
    ///////////////////////////////////////////////////////////////////////////

    fun startLoading(forceLoad: Boolean) {

        pushToStack(serviceId, url!!, name!!)

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
        pushToStack(result.serviceId, result.originalUrl, result.name)

        // setup sortedVideoStreams List with result
        sortedVideoStreams = ListHelper.getSortedStreamVideosList(
            context,
            result.videoStreams,
            result.videoOnlyStreams,
            false
        )
        selectedVideoStreamIndex = ListHelper.getDefaultResolutionIndex(context, sortedVideoStreams!!)
        Log.d(TAG, "handleResult(): sortedVideoStreams = $sortedVideoStreams")

        // initialize Related Videos List with result
        relatedStreams.clear()
        relatedStreams.addAll(result.relatedStreams)

        setStackItemTitleToUrl(result.serviceId, result.url, result.name)
        setStackItemTitleToUrl(result.serviceId, result.originalUrl, result.name)

        return result.errors.isEmpty()

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
                Toast.makeText(
                    context,
                    context.getString(R.string.youtube_signature_decryption_error),
                    Toast.LENGTH_SHORT
                )
                    .show()
            }

            else -> {
                Toast.makeText(context, context.getString(R.string.general_error), Toast.LENGTH_SHORT).show()
            }
        }
        NavigationHelper.openMainActivity(context)
        return true
    }

    fun directlyPlayVideoAnchorPlayer(serviceId: Int, url: String, name: String) {
        setInitialData(serviceId, url, name)
        pushToStack(serviceId, url, name)
        currentInfo = null
        currentWorker?.dispose()

        val errorMessage = arrayListOf<Throwable>()

        currentWorker = ExtractorHelper.getStreamInfo(serviceId, url, false)
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
                { noError ->
                    Log.d(TAG, "directlyPlayVideoAnchorPlayer(): ready to play video with noError = $noError")
                    if (noError) {
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

    companion object {
        const val TAG = "PlayerProxy"
    }
}