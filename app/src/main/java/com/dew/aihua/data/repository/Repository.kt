package com.dew.aihua.data.repository

import android.content.Context
import android.util.Log
import com.dew.aihua.data.local.database.AppDatabase
import com.dew.aihua.data.local.manoeuvre.HistoryRecordManager
import com.dew.aihua.data.model.SuggestionItem
import com.dew.aihua.data.network.api.ExtractorHelper
import com.dew.aihua.player.helper.ServiceHelper
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import org.schabi.newpipe.extractor.ListExtractor
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.search.SearchInfo
import org.schabi.newpipe.extractor.stream.StreamInfo

/**
 *  Created by Edward on 3/24/2019.
 */
class Repository(
    context: Context,
    private val historyRecordManager: HistoryRecordManager,
    private val database: AppDatabase
) {

    val serviceId: Int = ServiceHelper.getSelectedServiceId(context)
//    val historyRecordManager = HistoryRecordManager(context)

    fun getStreamInfoFromNetwork(url: String): Single<StreamInfo> =
        ExtractorHelper.getStreamInfo(this.serviceId, url, false)
            .subscribeOn(Schedulers.io())

    fun search(
        serviceId: Int,
        searchString: String,
        contentFilter: List<String>,
        sortFilter: String
    ): Single<SearchInfo> = ExtractorHelper.searchFor(serviceId, searchString, contentFilter, sortFilter)
        .subscribeOn(Schedulers.io())

    fun loadMoreSearchItems(
        serviceId: Int,
        searchString: String,
        contentFilter: List<String>,
        sortFilter: String,
        pageUrl: String
    ): Single<ListExtractor.InfoItemsPage<*>> =
        ExtractorHelper.getMoreSearchItems(serviceId, searchString, contentFilter, sortFilter, pageUrl)
            .subscribeOn(Schedulers.io())

    fun loadSuggestions(queryString: String) =
        Observable.just(queryString)
            .subscribeOn(Schedulers.io())
            .switchMap { query ->
                val local =
                    historyRecordManager.getRelatedSearches(query, 5, 25)
                        .toObservable()
                        .map { searchHistoryEntries ->
                            val suggestionItems = arrayListOf<SuggestionItem>()
                            searchHistoryEntries.forEach {
                                suggestionItems.add(SuggestionItem(true, it.search))
                            }
                            suggestionItems
                        }
                Log.d(TAG, "loadSuggestions(): serviceId = $serviceId, local = $local")
                if (query.length < THRESHOLD_NETWORK_SUGGESTION) return@switchMap local.materialize()

                val network = ExtractorHelper.suggestionsFor(serviceId, query)
                    .toObservable()
                    .map { suggestions ->
                        val results = arrayListOf<SuggestionItem>()
                        suggestions.forEach {
                            results.add(SuggestionItem(false, it))
                        }
                        results
                    }

                Observable.zip<List<SuggestionItem>, List<SuggestionItem>, List<SuggestionItem>>(
                    local, network,
                    BiFunction { localResult, networkResult ->
                        val result = arrayListOf<SuggestionItem>()
                        if (localResult.isNotEmpty()) result.addAll(localResult)
                        result.addAll(networkResult)
                        result.distinct()
                    }
                ).materialize()
            }


    fun saveSearchStringToDb(serviceId: Int, searchString: String) =
        historyRecordManager.onSearched(serviceId, searchString)
            .observeOn(Schedulers.computation())

    fun getStreamingService(serviceId: Int): StreamingService =
        NewPipe.getService(serviceId)

    fun deleteSuggestionItem(item: SuggestionItem) =
        historyRecordManager.deleteSearchHistory(item.query!!)
            .observeOn(Schedulers.computation())


    companion object {
        const val TAG = "Repository"
        private const val THRESHOLD_NETWORK_SUGGESTION = 2

        @Volatile
        var INSTANCE: Repository? = null

        fun getInstance(
            context: Context,
            historyRecordManager: HistoryRecordManager,
            database: AppDatabase
        ): Repository =
            INSTANCE ?: synchronized(Repository::class.java) {
                INSTANCE ?: Repository(context, historyRecordManager, database).also { INSTANCE = it }
            }

        fun getStreamInfoFromNetwork(serviceId: Int, url: String): Single<StreamInfo> =
            ExtractorHelper.getStreamInfo(serviceId, url, false)
                .subscribeOn(Schedulers.io())

        fun getServiceId(context: Context): Int = ServiceHelper.getSelectedServiceId(context)
    }


}