package com.dew.aihua.player.playqueque.queque

import android.util.Log
import com.dew.aihua.player.model.PlayQueueItem
import io.reactivex.SingleObserver
import io.reactivex.disposables.Disposable
import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.ListExtractor
import org.schabi.newpipe.extractor.ListInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem

/**
 *  Created by Edward on 3/2/2019.
 */

abstract class AbstractInfoPlayQueue<T : ListInfo<*>, U : InfoItem>(
    val serviceId: Int,
    val baseUrl: String,
    var nextUrl: String?,
    streams: List<StreamInfoItem>,
    index: Int) : PlayQueue(index,
    extractListItems(streams)
) {

    var isInitial: Boolean = streams.isEmpty()

    override var isComplete: Boolean = !isInitial && (nextUrl == null || nextUrl!!.isEmpty())

    @Transient
    var fetchReactor: Disposable? = null

    protected abstract val tag: String

    // Notify change
    val headListObserver: SingleObserver<T>
        get() = object : SingleObserver<T> {
            override fun onSubscribe(d: Disposable) {
                if (isComplete || !isInitial || fetchReactor != null && !fetchReactor!!.isDisposed) {
                    d.dispose()
                } else {
                    fetchReactor = d
                }
            }

            override fun onSuccess(result: T) {
                isInitial = false
                if (!result.hasNextPage()) isComplete = true
                nextUrl = result.nextPageUrl

                append(extractListItems(result.relatedItems))

                fetchReactor!!.dispose()
                fetchReactor = null
            }

            override fun onError(e: Throwable) {
                Log.e(tag, "Error fetching more playlist, marking playlist as complete.", e)
                isComplete = true
                append()
            }
        }

    // Notify change
    val nextPageObserver: SingleObserver<ListExtractor.InfoItemsPage<*>>
        get() = object : SingleObserver<ListExtractor.InfoItemsPage<*>> {
            override fun onSubscribe(d: Disposable) {
                if (isComplete || isInitial || fetchReactor != null && !fetchReactor!!.isDisposed) {
                    d.dispose()
                } else {
                    fetchReactor = d
                }
            }

            override fun onSuccess( result: ListExtractor.InfoItemsPage<*>) {
                if (!result.hasNextPage()) isComplete = true
                nextUrl = result.nextPageUrl

                append(extractListItems(result.items))

                fetchReactor!!.dispose()
                fetchReactor = null
            }

            override fun onError(e: Throwable) {
                Log.e(tag, "Error fetching more playlist, marking playlist as complete.", e)
                isComplete = true
                append()
            }
        }

    constructor(item: U) : this(item.serviceId, item.url, null, emptyList<StreamInfoItem>(), 0)

    override fun dispose() {
        super.dispose()
        fetchReactor?.dispose()
        fetchReactor = null
    }

    companion object {

        private fun extractListItems(infos: List<InfoItem>): List<PlayQueueItem> {
            val result = ArrayList<PlayQueueItem>()
            for (stream in infos) {
                if (stream is StreamInfoItem) {
                    result.add(PlayQueueItem(stream))
                }
            }
            return result
        }
    }


}
