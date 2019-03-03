package com.dew.aihua.player.mediasource

import android.util.Log
import com.dew.aihua.player.playback.PlaybackListener
import com.dew.aihua.player.helper.ServiceHelper
import com.dew.aihua.player.model.PlayQueueItem
import com.dew.aihua.player.playqueque.event.*
import com.dew.aihua.player.playqueque.queque.PlayQueue
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.internal.subscriptions.EmptySubscription
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean


/**
 *  Created by Edward on 3/2/2019.
 */
class MediaSourceManager private constructor(
    private val playbackListener: PlaybackListener,
    private val playQueue: PlayQueue,
    /**
     * Process only the last load order when receiving a stream of load orders (lessens I/O).
     * <br></br><br></br>
     * The higher it is, the less loading occurs during rapid noncritical timeline changes.
     * <br></br><br></br>
     * Not recommended to go below 100ms.
     *
     * @see .loadDebounced
     */
    private val loadDebounceMillis: Long,
    /**
     * Determines the gap time between the playback position and the playback duration which
     * the [.getEdgeIntervalSignal] begins to request loading.
     *
     * @see .progressUpdateIntervalMillis
     *
     */
    private val playbackNearEndGapMillis: Long,
    /**
     * Determines the interval which the [.getEdgeIntervalSignal] waits for between
     * each request for loading, once [.playbackNearEndGapMillis] has reached.
     */
    private val progressUpdateIntervalMillis: Long) {


    private val debouncedLoader: Disposable
    private val debouncedSignal: PublishSubject<Long> = PublishSubject.create()

    private var playQueueReactor: Subscription = EmptySubscription.INSTANCE
    private val loaderReactor: CompositeDisposable = CompositeDisposable()
    private val loadingItems: MutableSet<PlayQueueItem> = Collections.synchronizedSet(androidx.collection.ArraySet())

    private val isBlocked: AtomicBoolean = AtomicBoolean(false)

    private var playlist: ManagedMediaSourcePlaylist = ManagedMediaSourcePlaylist()

    ///////////////////////////////////////////////////////////////////////////
    // Event Reactor
    ///////////////////////////////////////////////////////////////////////////

    private val reactor: Subscriber<PlayQueueEvent>
        get() = object : Subscriber<PlayQueueEvent> {
            override fun onSubscribe(d: Subscription) {
                playQueueReactor.cancel()
                playQueueReactor = d
                playQueueReactor.request(1)
            }

            override fun onNext(playQueueMessage: PlayQueueEvent) {
                onPlayQueueChanged(playQueueMessage)
            }

            override fun onError(e: Throwable) {
                Log.d(TAG, "reactor: PlayQueueEvent error: ${e.message}")
            }

            override fun onComplete() {
                Log.d(TAG, "reactor: PlayQueueEvent onComplete()")
            }
        }

    ///////////////////////////////////////////////////////////////////////////
    // Playback Locking variables
    ///////////////////////////////////////////////////////////////////////////

    private val isPlayQueueReady: Boolean
        get() {
            val isWindowLoaded = playQueue.size() - playQueue.index > WINDOW_SIZE
            return playQueue.isComplete || isWindowLoaded
        }

    private val isPlaybackReady: Boolean
        get() {
            if (playlist.size() != playQueue.size()) return false

            val mediaSource = playlist[playQueue.index] ?: return false

            val playQueueItem = playQueue.item ?: return false
            return mediaSource.isStreamEqual(playQueueItem)
        }

    ///////////////////////////////////////////////////////////////////////////
    // MediaSource Loading variable
    ///////////////////////////////////////////////////////////////////////////

    private val edgeIntervalSignal: Observable<Long>
        get() = Observable.interval(progressUpdateIntervalMillis, TimeUnit.MILLISECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .filter {
                // this reason to move to main thread is isApproachingPlaybackEdge will access simpleExoPlayer
                playbackListener.isApproachingPlaybackEdge(playbackNearEndGapMillis)
            }
            .observeOn(Schedulers.computation())

    private val nearEndIntervalSignal: Observable<Long> = edgeIntervalSignal

    constructor(listener: PlaybackListener,
                playQueue: PlayQueue) : this(listener, playQueue, /*loadDebounceMillis=*/
        DEFAULT_LOAD_DEBOUNCE_MILLIS,
        /*playbackNearEndGapMillis=*/
        DEFAULT_PLAYBACK_NEAR_END_GAP_MILLIS,
        /*progressUpdateIntervalMillis*/
        DEFAULT_PROGRESS_UPDATE_INTERVAL_MILLIS
    )

    init {
        if (playQueue.broadcastReceiver == null) {
            throw IllegalArgumentException("Play Queue has not been initialized.")
        }
        if (playbackNearEndGapMillis < progressUpdateIntervalMillis) {
            throw IllegalArgumentException("Playback end gap=[$playbackNearEndGapMillis ms] must be longer than update interval=[ $progressUpdateIntervalMillis ms] for them to be useful.")
        }

        this.debouncedLoader = getDebouncedLoader()

        playQueue.broadcastReceiver!!
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(reactor)
    }

    ///////////////////////////////////////////////////////////////////////////
    // Exposed Methods
    ///////////////////////////////////////////////////////////////////////////
    /**
     * Dispose the manager and releases all message buses and loaders.
     */
    fun dispose() {
        Log.d(TAG, "dispose() called.")

        debouncedSignal.onComplete()
        debouncedLoader.dispose()

        playQueueReactor.cancel()
        loaderReactor.dispose()
    }

    ///////////////////////////////////////////////////////////////////////////
    // Event Reactor support function
    ///////////////////////////////////////////////////////////////////////////
    private fun onPlayQueueChanged(event: PlayQueueEvent) {
        if (playQueue.isEmpty && playQueue.isComplete) {
            playbackListener.onPlaybackShutdown()
            return
        }

        // Event specific action
        when (event.type()) {
            PlayQueueEventType.INIT,
            PlayQueueEventType.ERROR -> {
                maybeBlock()
                populateSources()
            }

            PlayQueueEventType.APPEND -> populateSources()
            PlayQueueEventType.SELECT -> maybeRenewCurrentIndex()
            PlayQueueEventType.REMOVE -> {
                val removeEvent = event as RemoveEvent
                playlist.remove(removeEvent.removeIndex)
            }
            PlayQueueEventType.MOVE -> {
                val moveEvent = event as MoveEvent
                playlist.move(moveEvent.fromIndex, moveEvent.toIndex)
            }
            PlayQueueEventType.REORDER -> {
                // Need to move to ensure the playing index getTabFrom play queue matches that of
                // the source timeline, and then window correction can take care of the rest
                val reorderEvent = event as ReorderEvent
                playlist.move(reorderEvent.fromSelectedIndex, reorderEvent.toSelectedIndex)
            }
            PlayQueueEventType.RECOVERY -> {
            }

        }

        // Loading and Syncing
        when (event.type()) {
            PlayQueueEventType.INIT,
            PlayQueueEventType.REORDER,
            PlayQueueEventType.ERROR,
            PlayQueueEventType.SELECT -> loadImmediate() // low frequency, critical events

            PlayQueueEventType.APPEND,
            PlayQueueEventType.REMOVE,
            PlayQueueEventType.MOVE,
            PlayQueueEventType.RECOVERY -> loadDebounced() // high frequency or noncritical events
        }

        if (!isPlayQueueReady) {
            maybeBlock()
            playQueue.fetch()
        }
        playQueueReactor.request(1)
    }

    ///////////////////////////////////////////////////////////////////////////
    // Playback Locking functions
    ///////////////////////////////////////////////////////////////////////////
    private fun maybeBlock() {
        Log.d(TAG, "maybeBlock() called.")

        if (isBlocked.get()) return

        playbackListener.onPlaybackBlock()
        resetSources()

        isBlocked.set(true)
    }

    private fun maybeUnblock() {
        Log.d(TAG, "maybeUnblock() called.")

        if (isBlocked.get()) {
            isBlocked.set(false)
            playbackListener.onPlaybackUnblock(playlist.internalMediaSource)
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Metadata Synchronization
    ///////////////////////////////////////////////////////////////////////////

    private fun maybeSync() {
        Log.d(TAG, "maybeSync() called.")

        val currentItem = playQueue.item
        if (isBlocked.get() || currentItem == null) return

        playbackListener.onPlaybackSynchronize(currentItem)
    }

    @Synchronized
    private fun maybeSynchronizePlayer() {
        if (isPlayQueueReady && isPlaybackReady) {
            maybeUnblock()
            maybeSync()
        }
    }

    private fun getDebouncedLoader(): Disposable {
        return debouncedSignal.mergeWith(nearEndIntervalSignal)
            .debounce(loadDebounceMillis, TimeUnit.MILLISECONDS)
            .subscribeOn(Schedulers.single())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { loadImmediate() }
    }

    private fun loadDebounced() {
        debouncedSignal.onNext(System.currentTimeMillis())
    }

    private fun loadImmediate() {
        Log.d(TAG, "MediaSource - loadImmediate() called")
        val itemsToLoad = getItemsToLoad(playQueue) ?: return

        // Evict the previous items being loaded to free up memory, before start loading new ones
        maybeClearLoaders()

        maybeLoadItem(itemsToLoad.center)
        for (item in itemsToLoad.neighbors) {
            maybeLoadItem(item)
        }
    }

    private fun maybeLoadItem(item: PlayQueueItem) {
        Log.d(TAG, "maybeLoadItem() called.")
        if (playQueue.indexOf(item) >= playlist.size()) return

        if (!loadingItems.contains(item) && isCorrectionNeeded(item)) {
            Log.d(TAG, "MediaSource: maybeLoadItem(): item.title=[${item.title}] with url=[${item.url}]")

            loadingItems.add(item)
            val loader = getLoadedMediaSource(item)
                .observeOn(AndroidSchedulers.mainThread())
                /* No exception handling since getLoadedMediaSource guarantees nonnull return */
                .subscribe { mediaSource -> onMediaSourceReceived(item, mediaSource) }
            loaderReactor.add(loader)
        }
    }

    private fun getLoadedMediaSource(stream: PlayQueueItem): Single<ManagedMediaSource> =
        stream.stream.map<ManagedMediaSource> { streamInfo ->
            val source = playbackListener.sourceOf(stream, streamInfo)
            if (source == null) {
                val message = "Unable to resolve source getTabFrom stream info." +
                        " URL: " + stream.url +
                        ", audio count: " + streamInfo.audioStreams.size +
                        ", video count: " + streamInfo.videoOnlyStreams.size +
                        streamInfo.videoStreams.size
                Log.d(TAG, "getLoadedMediaSource error: $message")
                return@map FailedMediaSource(stream, FailedMediaSource.MediaSourceResolutionException(message))
            }

            val expiration = System.currentTimeMillis() + ServiceHelper.getCacheExpirationMillis(streamInfo.serviceId)
            LoadedMediaSource(source, stream, expiration)
        }.onErrorReturn { throwable ->
            FailedMediaSource(stream, FailedMediaSource.StreamInfoLoadException(throwable))
        }


    private fun onMediaSourceReceived(item: PlayQueueItem,
                                      mediaSource: ManagedMediaSource
    ) {
        Log.d(TAG, "onMediaSourceReceived(): Loaded=[${item.title}] with url=[${item.url}]")

        loadingItems.remove(item)

        val itemIndex = playQueue.indexOf(item)
        // Only update the playlist timeline for items at the current index or after.
        if (isCorrectionNeeded(item)) {
            Log.d(TAG, "MediaSource - Updating index=[$itemIndex] with title=[${item.title}] at url=[${item.url}]")
            playlist.update(itemIndex, mediaSource, Runnable { this.maybeSynchronizePlayer() })

        }
    }

    /**
     * Checks if the corresponding MediaSource in
     * [com.google.android.exoplayer2.source.ConcatenatingMediaSource]
     * for a given [PlayQueueItem] needs replacement, either due to gapless playback
     * readiness or playlist desynchronization.
     * <br></br><br></br>
     * If the given [PlayQueueItem] is currently being played and is already loaded,
     * then correction is not only needed if the playlist is desynchronized. Otherwise, the
     * check depends on the status (e.g. expiration or placeholder) of the
     * [ManagedMediaSource].
     */
    private fun isCorrectionNeeded(item: PlayQueueItem): Boolean {
        val index = playQueue.indexOf(item)
        val mediaSource = playlist[index]
        return mediaSource != null && mediaSource.shouldBeReplacedWith(item,
            /*mightBeInProgress=*/index != playQueue.index)
    }

    /**
     * Checks if the current playing index contains an expired [ManagedMediaSource].
     * If so, the expired source is replaced by a [PlaceholderMediaSource] and
     * [.loadImmediate] is called to reload the current item.
     * <br></br><br></br>
     * If not, then the media source at the current index is ready for playback, and
     * [.maybeSynchronizePlayer] is called.
     * <br></br><br></br>
     * Under both cases, [.maybeSync] will be called to ensure the listener
     * is up-to-date.
     */
    private fun maybeRenewCurrentIndex() {
        val currentIndex = playQueue.index
        val currentSource = playlist[currentIndex] ?: return

        val currentItem = playQueue.item
        if (!currentSource.shouldBeReplacedWith(currentItem!!, /*canInterruptOnRenew=*/true)) {
            maybeSynchronizePlayer()
            return
        }

        Log.d(TAG, "MediaSource - Reloading currently playing, index=[$currentIndex], item=[${currentItem.title}]")
        playlist.invalidate(currentIndex, Runnable { this.loadImmediate() })
    }

    private fun maybeClearLoaders() {
        Log.d(TAG, "MediaSource - maybeClearLoaders() called.")
        if (!loadingItems.contains(playQueue.item) && loaderReactor.size() > MAXIMUM_LOADER_SIZE) {
            loaderReactor.clear()
            loadingItems.clear()
        }
    }
    ///////////////////////////////////////////////////////////////////////////
    // MediaSource Playlist Helpers
    ///////////////////////////////////////////////////////////////////////////

    private fun resetSources() {
        Log.d(TAG, "resetSources() called.")
        playlist = ManagedMediaSourcePlaylist()
    }

    private fun populateSources() {
        Log.d(TAG, "populateSources() called.")
        while (playlist.size() < playQueue.size()) {
            playlist.expand()
        }
    }

    private data class ItemsToLoad internal constructor(val center: PlayQueueItem,
                                                        val neighbors: Collection<PlayQueueItem>)

    companion object {
        private val TAG = "MediaSourceManager@" + hashCode()

        /**
         * Determines how many streams before and after the current stream should be loaded.
         * The default value (1) ensures seamless playback under typical network settings.
         * <br></br><br></br>
         * The streams after the current will be loaded into the playlist timeline while the
         * streams before will only be cached for future usage.
         *
         * @see .onMediaSourceReceived
         */
        private const val WINDOW_SIZE = 1

        /**
         * Determines the maximum number of disposables allowed in the [.loaderReactor].
         * Once exceeded, new calls to [.loadImmediate] will evict all disposables in the
         * [.loaderReactor] in order to load a new set of items.
         *
         * @see .loadImmediate
         * @see .maybeLoadItem
         */
        private const val MAXIMUM_LOADER_SIZE = WINDOW_SIZE * 2 + 1
        private const val DEFAULT_LOAD_DEBOUNCE_MILLIS = 400L
        private const val DEFAULT_PLAYBACK_NEAR_END_GAP_MILLIS = 30 * 1000L
        private const val DEFAULT_PROGRESS_UPDATE_INTERVAL_MILLIS = 2 * 1000L

        ///////////////////////////////////////////////////////////////////////////
        // Manager Helpers
        ///////////////////////////////////////////////////////////////////////////
        private fun getItemsToLoad(playQueue: PlayQueue): ItemsToLoad? {
            // The current item has higher priority
            val currentIndex = playQueue.index
            val currentItem = playQueue.getItem(currentIndex) ?: return null

            // The rest are just for seamless playback
            // Although timeline is not updated prior to the current index, these sources are still
            // loaded into the cache for faster retrieval at a potentially later time.
            val leftBound = Math.max(0, currentIndex - WINDOW_SIZE)
            val rightLimit = currentIndex + WINDOW_SIZE + 1
            val rightBound = Math.min(playQueue.size(), rightLimit)
            val neighbors = androidx.collection.ArraySet(
                playQueue.streams!!.subList(leftBound, rightBound))

            // Do a round robin
            val excess = rightLimit - playQueue.size()
            if (excess >= 0) {
                neighbors.addAll(playQueue.streams!!.subList(0, Math.min(playQueue.size(), excess)))
            }
            neighbors.remove(currentItem)

            return ItemsToLoad(currentItem, neighbors)
        }
    }
}
