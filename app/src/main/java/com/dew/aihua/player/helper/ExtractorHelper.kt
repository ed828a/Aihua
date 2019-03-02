package com.dew.aihua.player.helper

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.util.Log
import android.widget.Toast
import com.dew.aihua.R
import com.dew.aihua.report.ErrorActivity
import com.dew.aihua.report.ErrorInfo
import com.dew.aihua.report.UserAction
import com.dew.aihua.ui.activity.MainActivity
import io.reactivex.Maybe
import io.reactivex.Single
import org.schabi.newpipe.extractor.Info
import org.schabi.newpipe.extractor.ListExtractor
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.channel.ChannelInfo
import org.schabi.newpipe.extractor.exceptions.ContentNotAvailableException
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import org.schabi.newpipe.extractor.kiosk.KioskInfo
import org.schabi.newpipe.extractor.playlist.PlaylistInfo
import org.schabi.newpipe.extractor.search.SearchInfo
import org.schabi.newpipe.extractor.services.youtube.extractors.YoutubeStreamExtractor
import org.schabi.newpipe.extractor.stream.StreamInfo
import java.io.IOException

/**
 *  Created by Edward on 3/2/2019.
 */

object ExtractorHelper {
    private val TAG = ExtractorHelper::class.java.simpleName

    private fun checkServiceId(serviceId: Int) {
        if (serviceId == NO_SERVICE_ID) {
            throw IllegalArgumentException("serviceId is NO_SERVICE_ID")
        }
    }

    fun searchFor(serviceId: Int,
                  searchString: String,
                  contentFilter: List<String>,
                  sortFilter: String
    ): Single<SearchInfo> {
        checkServiceId(serviceId)
        val searchQueryHandler = NewPipe.getService(serviceId)
            .searchQHFactory
            .fromQuery(searchString, contentFilter, sortFilter)

        return Single.fromCallable {
            SearchInfo.getInfo(NewPipe.getService(serviceId), searchQueryHandler)
        }
    }

    fun getMoreSearchItems(serviceId: Int,
                           searchString: String,
                           contentFilter: List<String>,
                           sortFilter: String,
                           pageUrl: String): Single<ListExtractor.InfoItemsPage<*>> {
        Log.d(TAG, "getMoreSearchItems(): serviceId = $serviceId, searchString = $searchString, pageUrl = $pageUrl")
        checkServiceId(serviceId)
        return Single.fromCallable {
            SearchInfo.getMoreItems(
                NewPipe.getService(serviceId),
                NewPipe.getService(serviceId)
                    .searchQHFactory
                    .fromQuery(searchString, contentFilter, sortFilter),
                pageUrl)
        }

    }

    fun suggestionsFor(serviceId: Int,
                       query: String): Single<List<String>> {
        Log.d(TAG, "suggestionsFor(serviceId = $serviceId, query = $query")
        checkServiceId(serviceId)
        return Single.fromCallable {
            NewPipe.getService(serviceId)
                .suggestionExtractor
                .suggestionList(query)
        }
    }

    fun getStreamInfo(serviceId: Int,
                      url: String,
                      forceLoad: Boolean): Single<StreamInfo> {
        checkServiceId(serviceId)
        return checkCache(forceLoad, serviceId, url,
            Single.fromCallable {
                StreamInfo.getInfo(NewPipe.getService(serviceId), url)
            })
    }

    // YouTube Channel goes here
    fun getChannelInfo(serviceId: Int,
                       url: String,
                       forceLoad: Boolean): Single<ChannelInfo> {
        checkServiceId(serviceId)
        return checkCache(forceLoad, serviceId, url,
            Single.fromCallable {
                ChannelInfo.getInfo(NewPipe.getService(serviceId), url)
            })
    }

    fun getMoreChannelItems(serviceId: Int,
                            url: String,
                            nextStreamsUrl: String): Single<ListExtractor.InfoItemsPage<*>> {
        checkServiceId(serviceId)
        return Single.fromCallable { ChannelInfo.getMoreItems(NewPipe.getService(serviceId), url, nextStreamsUrl) }
    }

    fun getPlaylistInfo(serviceId: Int,
                        url: String,
                        forceLoad: Boolean): Single<PlaylistInfo> {
        checkServiceId(serviceId)
        return checkCache(forceLoad, serviceId, url,
            Single.fromCallable {
                PlaylistInfo.getInfo(NewPipe.getService(serviceId), url)
            })
    }

    fun getMorePlaylistItems(serviceId: Int,
                             url: String,
                             nextStreamsUrl: String): Single<ListExtractor.InfoItemsPage<*>> {
        checkServiceId(serviceId)
        return Single.fromCallable { PlaylistInfo.getMoreItems(NewPipe.getService(serviceId), url, nextStreamsUrl) }
    }

    fun getKioskInfo(serviceId: Int,
                     url: String,
                     forceLoad: Boolean): Single<KioskInfo> {
        return checkCache(forceLoad, serviceId, url,
            Single.fromCallable {
                KioskInfo.getInfo(NewPipe.getService(serviceId), url)
            })
    }

    fun getMoreKioskItems(serviceId: Int,
                          url: String,
                          nextStreamsUrl: String): Single<ListExtractor.InfoItemsPage<*>> {
        return Single.fromCallable { KioskInfo.getMoreItems(NewPipe.getService(serviceId), url, nextStreamsUrl) }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Utils
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Check if we can load it getTabFrom the cache (forceLoad parameter), if we can't,
     * load getTabFrom the network (Single loadFromNetwork)
     * and put the results in the cache.
     */
    private fun <I : Info> checkCache(forceLoad: Boolean,
                                      serviceId: Int,
                                      url: String,
                                      loadFromNetwork: Single<I>
    ): Single<I> {
//        var loadFromNetwork = loadFromNetwork
        checkServiceId(serviceId)
        val loadFromNetwork1 = loadFromNetwork.doOnSuccess { info -> InfoCache.putInfo(serviceId, url, info) }

        return if (forceLoad) {
            InfoCache.removeInfo(serviceId, url)
            loadFromNetwork1
        } else {
            Maybe.concat(loadFromCache(serviceId, url), loadFromNetwork1.toMaybe())
                .firstElement() //Take the first valid
                .toSingle()
        }
    }

    /**
     * Default implementation uses the [InfoCache] to get cached results
     */
    private fun <I : Info> loadFromCache(serviceId: Int, url: String): Maybe<I> {
        checkServiceId(serviceId)
        return Maybe.defer {
            val info = InfoCache.getFromKey(serviceId, url)
            Log.d(TAG, "loadFromCache() called, info > $info")

            // Only return info if it's not null (which means that it is cached)
            if (info != null) {
                @Suppress("UNCHECKED_CAST")
                Maybe.just<I>(info as I)
            } else {
                Maybe.empty<I>()
            }
        }
    }

    /**
     * A simple and general error handler that show a Toast for known exceptions, and for others, opens the report error activity with the (optional) error message.
     */
    fun handleGeneralException(context: Context, serviceId: Int, url: String, exception: Throwable, userAction: UserAction, optionalErrorMessage: String?) {
        val handler = Handler(context.mainLooper)

        handler.post {
            when (exception) {
                is ReCaptchaException -> {
                    Toast.makeText(context, R.string.recaptcha_request_toast, Toast.LENGTH_LONG).show()
                    // Starting ReCaptcha Challenge Activity
                    val intent = Intent(context, ReCaptchaActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                }
                is IOException -> Toast.makeText(context, R.string.network_error, Toast.LENGTH_LONG).show()
                is YoutubeStreamExtractor.GemaException -> Toast.makeText(context, R.string.blocked_by_gema, Toast.LENGTH_LONG).show()
                is ContentNotAvailableException -> Toast.makeText(context, R.string.content_not_available, Toast.LENGTH_LONG).show()
                else -> {
                    val errorId = when (exception) {
                        is YoutubeStreamExtractor.DecryptException -> R.string.youtube_signature_decryption_error
                        is ParsingException -> R.string.parsing_error
                        else -> R.string.general_error
                    }

                    ErrorActivity.reportError(handler, context, exception, MainActivity::class.java, null,
                        ErrorInfo.make(userAction,
                            if (serviceId == -1) "none" else NewPipe.getNameOfService(serviceId),
                            url + (optionalErrorMessage ?: ""),
                            errorId)
                    )
                }
            }
        }
    }

    /**
     * Check if throwable have the cause that can be assignable getTabFrom the causes to check.
     *
     * @see Class.isAssignableFrom
     */
    fun hasAssignableCauseThrowable(throwable: Throwable,
                                    vararg causesToCheck: Class<*>): Boolean {
        // Check if throwable is a subclass of any of the filtered classes
        val throwableClass = throwable.javaClass
        for (causesEl in causesToCheck) {
            if (causesEl.isAssignableFrom(throwableClass)) {
                return true
            }
        }

        // Iteratively checks if the root cause of the throwable is a subclass of the filtered class
        val cause: Throwable? = throwable.cause
        if (cause != null) {
            val causeClass = cause.javaClass
            for (causesEl in causesToCheck) {
                if (causesEl.isAssignableFrom(causeClass)) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * Check if throwable have the exact cause getTabFrom one of the causes to check.
     */
    private fun hasExactCauseThrowable(throwable: Throwable, vararg causesToCheck: Class<*>): Boolean {
        for (causesEl in causesToCheck) {
            if (throwable.javaClass == causesEl) {
                return true
            }
        }

        val cause: Throwable? = throwable.cause
        if (cause != null) {
            for (causesEl in causesToCheck) {
                if (cause.javaClass == causesEl) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * Check if throwable have Interrupted* exception as one of its causes.
     */
    fun isInterruptedCaused(throwable: Throwable): Boolean {
        return ExtractorHelper.hasExactCauseThrowable(throwable,
            InterruptedIOException::class.java,
            InterruptedException::class.java)
    }
}