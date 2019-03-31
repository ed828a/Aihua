package com.dew.aihua.ui.viewmodel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.dew.aihua.R
import com.dew.aihua.data.model.SnackbarErrorParameters
import com.dew.aihua.data.model.SuggestionItem
import com.dew.aihua.data.network.api.ExtractorHelper
import com.dew.aihua.data.repository.Repository
import com.dew.aihua.report.UserAction
import icepick.State
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.search.SearchExtractor
import java.io.IOException
import java.io.InterruptedIOException
import java.net.SocketException

class SearchFragmentViewModel(private val repository: Repository) : ViewModel() {

    val itemList: MutableLiveData<List<InfoItem>> = MutableLiveData()
    val networkError: MutableLiveData<Throwable> = MutableLiveData()
    val lastSearchString: MutableLiveData<String> = MutableLiveData()
    val nextPageUrl: MutableLiveData<String> = MutableLiveData()
    val currentPageUrl: MutableLiveData<String> = MutableLiveData()
    val snackbarError: MutableLiveData<SnackbarErrorParameters> = MutableLiveData()
    val onEvent: MutableLiveData<Boolean> = MutableLiveData()
    val suggestions: MutableLiveData<List<SuggestionItem>> = MutableLiveData()
    val suggestionError: MutableLiveData<Throwable> = MutableLiveData()

    @State
    @JvmField
    var wholeInfoItemList = arrayListOf<InfoItem>()

    private val compositeDisposable = CompositeDisposable()

    fun search(serviceId: Int,
               searchString: String,
               contentFilter: List<String>,
               sortFilter: String
    ): Disposable = repository.search(serviceId, searchString, contentFilter, sortFilter)
        .observeOn(Schedulers.computation())
        .subscribe(
            { result ->
                Log.d(TAG, "search() result = $result")
                onEvent.postValue(true)
                lastSearchString.postValue(searchString)
                nextPageUrl.postValue(result.nextPageUrl)
                currentPageUrl.postValue(result.url)
                itemList.postValue(result.relatedItems)
                val exceptions = result.errors
                if (exceptions != null && !exceptions.isEmpty() && !(exceptions.size == 1 && exceptions[0] is SearchExtractor.NothingFoundException)) {
                    val errorParameters = SnackbarErrorParameters(
                        result.errors, UserAction.SEARCHED,
                        NewPipe.getNameOfService(serviceId), searchString, 0
                    )
                    snackbarError.postValue(errorParameters)
                }
            },
            { error ->
                networkError.postValue(error)
            }
        )

    fun loadMoreSearchResults(serviceId: Int,
                              searchString: String,
                              contentFilter: List<String>,
                              sortFilter: String,
                              nextUrl: String
    ): Disposable =
        repository.loadMoreSearchItems(serviceId, searchString, contentFilter, sortFilter, nextUrl)
            .observeOn(Schedulers.computation())
            .subscribe { result ->
                onEvent.postValue(true)
                currentPageUrl.postValue(nextUrl)
                nextPageUrl.postValue(result.nextPageUrl)
                val list = itemList.value

                if (list != null) {
                    wholeInfoItemList.addAll(list)
                }
                wholeInfoItemList.addAll(result.items)
                itemList.postValue(result.items)
                if (result.errors.isNotEmpty()) {
                    val errorParameters = SnackbarErrorParameters(
                        result.errors, UserAction.SEARCHED,
                        NewPipe.getNameOfService(serviceId), searchString, 0
                    )
                    snackbarError.postValue(errorParameters)
                }
            }


    fun loadSuggestions(query: String): Disposable {
        Log.d(TAG, "loadSuggestions(): query = $query")
        return repository.loadSuggestions(query)
            .observeOn(Schedulers.computation())
            .subscribe { listNotification ->
                when {
                    listNotification.isOnNext -> {
                        suggestions.postValue(listNotification.value)
                    }

                    listNotification.isOnError -> {
                        val error = listNotification.error
                        error?.let { throwable ->
                            if (!ExtractorHelper.hasAssignableCauseThrowable(
                                    throwable,
                                    IOException::class.java,
                                    SocketException::class.java,
                                    InterruptedException::class.java,
                                    InterruptedIOException::class.java
                                )
                            ) {
                                suggestionError.postValue(throwable)
                            }
                        }
                    }
                }
            }
    }

    fun saveSearchString(serviceId: Int, searchString: String): Disposable =
        repository.saveSearchStringToDb(serviceId, searchString)
            .subscribe(
                {},
                { error ->
                    val errorParameters = SnackbarErrorParameters(
                        arrayListOf(error), UserAction.SEARCHED,
                        NewPipe.getNameOfService(serviceId), searchString, 0
                    )
                    snackbarError.postValue(errorParameters)
                }
            )

    fun getStreamingService(serviceId: Int): StreamingService =
        repository.getStreamingService(serviceId)

    fun deleteSuggestionItem(item: SuggestionItem) {
        val suggestionItemList = suggestions.value
        if (suggestionItemList != null) {
            val newList = suggestionItemList.filter { it != item }
            suggestions.postValue(newList)
            val d = repository.deleteSuggestionItem(item)
                .subscribe(
                    {},
                    { error ->
                        val snackbarErrorParameters = SnackbarErrorParameters(
                            arrayListOf(error),
                            UserAction.DELETE_FROM_HISTORY, "none",
                            "Deleting item failed", R.string.general_error
                        )
                        snackbarError.postValue(snackbarErrorParameters)
                    }
                )
            compositeDisposable.add(d)
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (!compositeDisposable.isDisposed) compositeDisposable.dispose()
    }

    companion object {
        private val TAG = SearchFragmentViewModel::class.java.simpleName
    }
}
