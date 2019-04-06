package com.dew.aihua.ui.new_fragment

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.webkit.URLUtil
import android.widget.EditText
import android.widget.ImageView
import android.widget.SearchView
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import com.dew.aihua.R
import com.dew.aihua.data.local.manoeuvre.HistoryRecordManager
import com.dew.aihua.data.model.SuggestionItem
import com.dew.aihua.player.helper.AnimationUtils
import com.dew.aihua.player.helper.AnimationUtils.animateView
import com.dew.aihua.util.Constants
import com.dew.aihua.player.helper.ServiceHelper
import com.dew.aihua.report.ErrorActivity
import com.dew.aihua.report.ErrorInfo
import com.dew.aihua.report.UserAction
import com.dew.aihua.ui.activity.ReCaptchaActivity
import com.dew.aihua.ui.adapter.SuggestionListAdapter
import com.dew.aihua.ui.contract.BackPressable
import com.dew.aihua.ui.viewmodel.ViewModelFactory
import com.dew.aihua.util.LayoutManagerSmoothScroller
import com.dew.aihua.util.NavigationHelper
import com.dew.aihua.ui.viewmodel.SearchFragmentViewModel
import com.dew.aihua.util.Constants.ACTION_ADD_TAB_MESSAGE
import com.dew.aihua.util.Constants.KEY_SEARCH_STRING
import com.dew.aihua.util.Constants.KEY_TAB_TITLE
import com.jakewharton.rxbinding2.view.RxView
import com.jakewharton.rxbinding2.widget.RxSearchView
import icepick.State
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.closestKodein
import org.kodein.di.generic.instance
import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.ListExtractor
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.search.SearchExtractor
import org.schabi.newpipe.extractor.search.SearchInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import java.util.*
import java.util.concurrent.TimeUnit

/**
 *  Created by Edward on 2/23/2019.
 *
 *  ListExtractor: Base class to extractors something who has a list (e.g. playlists, users).
 */

class NewSearchFragment : NewBaseListFragment<SearchInfo, ListExtractor.InfoItemsPage<*>>(), BackPressable,
    KodeinAware {
    override val kodein: Kodein by closestKodein()
    private val viewModelFactory: ViewModelFactory by instance()

    private lateinit var viewModel: SearchFragmentViewModel

    @State
    @JvmField
    protected var filterItemCheckedId = -1

    @State
    @JvmField
    protected var serviceId = Constants.NO_SERVICE_ID

    // these three re-preset the current search query
    @State
    @JvmField
    protected var searchString: String? = null
    @State
    @JvmField
    protected var contentFilter: Array<String> = emptyArray()
    @State
    @JvmField
    protected var sortFilter: String? = null

    // these two re-prestent the last search
    @State
    @JvmField
    protected var lastSearchedString: String? = null

    @State
    @JvmField
    protected var isSuggestionPanelShowing = false

    @State
    @JvmField
    protected var currentPageUrl: String? = null   // StateSaver takes care it

    @State
    @JvmField
    protected var nextPageUrl: String? = null      // SateSave takes care it

    @State
    @JvmField
    protected var searchFragmentTitle: String? = null

    private val menuItemToFilterName: MutableMap<Int, String> = HashMap()
    private var service: StreamingService? = null

    private lateinit var contentCountry: String
    private var isSuggestionsEnabled = true
    private var isSearchHistoryEnabled = true

    private val suggestionPublisher = PublishSubject.create<String>()
    private var searchDisposable: Disposable? = null
    private var suggestionDisposable: Disposable? = null

    private lateinit var suggestionListAdapter: SuggestionListAdapter
    private lateinit var historyRecordManager: HistoryRecordManager

    private lateinit var preferences: SharedPreferences

    ///////////////////////////////////////////////////////////////////////////
    // Views
    ///////////////////////////////////////////////////////////////////////////
    private lateinit var suggestionsPanel: View
    private lateinit var suggestionsRecyclerView: RecyclerView

    ///////////////////////////////////////////////////////////////////////////
    // Search
    ///////////////////////////////////////////////////////////////////////////

    private lateinit var searchView: SearchView
    private lateinit var fragmentView: View
    private val disposables = CompositeDisposable()

    /**
     * Set wasLoading to true so when the fragment onResume is called, the initial search is done.
     */
    private fun setSearchOnResume() {
        wasLoading.set(true)
    }

    ///////////////////////////////////////////////////////////////////////////
    // Fragment's LifeCycle
    ///////////////////////////////////////////////////////////////////////////

    override fun onAttach(context: Context) {
        super.onAttach(context)

        viewModel = ViewModelProviders.of(this, viewModelFactory).get(SearchFragmentViewModel::class.java)

        suggestionListAdapter = SuggestionListAdapter(activity!!)
        preferences = PreferenceManager.getDefaultSharedPreferences(activity)
        isSearchHistoryEnabled = preferences.getBoolean(getString(R.string.enable_search_history_key), true)
        suggestionListAdapter.setShowSuggestionHistory(isSearchHistoryEnabled)

        historyRecordManager = HistoryRecordManager(context)

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        isSuggestionsEnabled = preferences.getBoolean(getString(R.string.show_search_suggestions_key), true)
        contentCountry =
            preferences.getString(getString(R.string.content_country_key), getString(R.string.default_country_value))
                ?: getString(R.string.default_country_value)
        Log.d(TAG, "onCreate(): contentCountry = $contentCountry")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        fragmentView = inflater.inflate(R.layout.fragment_search, container, false)
        return fragmentView
    }

    override fun onViewCreated(rootView: View, savedInstanceState: Bundle?) {
        Log.d(TAG, "onViewCreated() called")
        super.onViewCreated(rootView, savedInstanceState)
        showSearchOnStart()
        initSearchListeners()
    }

    override fun onPause() {
        super.onPause()

        searchDisposable?.dispose()
        suggestionDisposable?.dispose()
        hideKeyboardSearch()
    }

    private fun setObservers(viewModel: SearchFragmentViewModel) {

        with(viewModel) {
            onEvent.observe(this@NewSearchFragment, Observer { resultArrived ->
                Log.d(TAG, "onEvent.observe() resultArrived = $resultArrived")
                if (resultArrived != null && resultArrived) {
                    isLoading.set(false)
                    hideLoading()
                    onEvent.postValue(false)
                }
            })
            currentPageUrl.observe(this@NewSearchFragment, Observer {
                this@NewSearchFragment.currentPageUrl = it
                Log.d(TAG, "this@SearchFragment.currentPageUrl = ${this@NewSearchFragment.currentPageUrl}")
            })
            nextPageUrl.observe(this@NewSearchFragment, Observer {
                this@NewSearchFragment.nextPageUrl = it
                showListFooter(false)
                Log.d(TAG, "this@SearchFragment.nextPageUrl = ${this@NewSearchFragment.nextPageUrl}")
            })
            lastSearchString.observe(this@NewSearchFragment, Observer {
                this@NewSearchFragment.lastSearchedString = it
                Log.d(TAG, "this@SearchFragment.lastSearchedString = ${this@NewSearchFragment.lastSearchedString}")
            })

            itemList.observe(this@NewSearchFragment, Observer { list ->
                if (list != null && list.isNotEmpty()) {
                    listAdapter?.addItems(list)
                } else if (listAdapter!!.itemsList.isEmpty()) {
                    showEmptyState()
                }
                Log.d(TAG, "list = $list")
            })
            snackbarError.observe(this@NewSearchFragment, Observer { snackBarError ->
                snackBarError?.let {
                    showSnackBarError(it.exception, it.userAction, it.serviceName, it.request, it.errorId)
                }
            })

            networkError.observe(this@NewSearchFragment, Observer { error ->
                if (error != null) this@NewSearchFragment.onError(error)
            })

            suggestions.observe(this@NewSearchFragment, Observer { list ->
                Log.d(TAG, "suggestions Observing: thread = ${Thread.currentThread().name}" )
                if (list != null) handleSuggestions(list)
            })

            suggestionError.observe(this@NewSearchFragment, Observer { error ->
                if (error != null) onSuggestionError(error)
            })
        }
    }

    override fun onResume() {
        Log.d(TAG, "onResume() called, searchString = $searchString")
        super.onResume()

        setObservers(viewModel)

        try {
            service = viewModel.getStreamingService(serviceId)
        } catch (exception: Exception) {
            ErrorActivity.reportError(
                activity!!, exception, activity!!.javaClass,
                activity!!.findViewById(android.R.id.content),
                ErrorInfo.make(
                    UserAction.UI_ERROR,
                    "",
                    "",
                    R.string.general_error
                )
            )
        }

        if (!TextUtils.isEmpty(searchString)) {
            if (wasLoading.getAndSet(false)) {
                search(searchString, contentFilter, sortFilter!!)
            } else if (listAdapter!!.itemsList.isEmpty()) {
                if (currentPageUrl == null) {
                    search(searchString, contentFilter, sortFilter!!)
                } else if (!isLoading.get()) {
                    listAdapter!!.clearStreamItemList()
                    showEmptyState()
                }
            }
        }

        hideSuggestionsPanel()

        if (suggestionDisposable == null || suggestionDisposable!!.isDisposed) initSuggestionObserver()

    }

    override fun onDestroyView() {
        Log.d(TAG, "onDestroyView() called")
        unsetSearchListeners()
        super.onDestroyView()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (searchDisposable != null) searchDisposable!!.dispose()
        if (suggestionDisposable != null) suggestionDisposable!!.dispose()
        if (!compositeDisposable.isDisposed) compositeDisposable.dispose()
        disposables.clear()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            ReCaptchaActivity.RECAPTCHA_REQUEST ->
                if (resultCode == Activity.RESULT_OK && !TextUtils.isEmpty(searchString)) {
                    search(searchString, contentFilter, sortFilter!!)
                } else
                    Log.e(TAG, "ReCaptcha failed")

            else -> Log.e(TAG, "Request code getTabFrom activity not supported [$requestCode]")
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Init
    ///////////////////////////////////////////////////////////////////////////

    override fun initViews(rootView: View, savedInstanceState: Bundle?) {
        super.initViews(rootView, savedInstanceState)
        suggestionsPanel = rootView.findViewById(R.id.suggestions_panel)
        suggestionsRecyclerView = rootView.findViewById(R.id.suggestions_list)
        suggestionsRecyclerView.adapter = suggestionListAdapter
        suggestionsRecyclerView.layoutManager = LayoutManagerSmoothScroller(activity!!)

    }


    override fun reloadContent() {
        if (!TextUtils.isEmpty(searchString)) search(searchString, arrayOf(), "")
    }

    ///////////////////////////////////////////////////////////////////////////
    // Menu
    ///////////////////////////////////////////////////////////////////////////

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        Log.d(TAG, "onCreateOptionsMenu() called")
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()
        inflater.inflate(R.menu.menu_search, menu)

        val supportActionBar = activity!!.supportActionBar?.apply {
            setDisplayShowTitleEnabled(true)
            setDisplayHomeAsUpEnabled(true)
            title = searchFragmentTitle
        }

        val view = menu.findItem(R.id.action_search)?.actionView ?: throw Exception("searchView is null")

        searchView = view as SearchView
        with(searchView) {
            setIconifiedByDefault(true)
            requestFocus()
        }
        if (TextUtils.isEmpty(searchString))
            (activity!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).toggleSoftInput(
                InputMethodManager.SHOW_FORCED,
                InputMethodManager.HIDE_IMPLICIT_ONLY
            )


        val d = RxSearchView.queryTextChangeEvents(searchView)
            .doOnNext {
                Log.d(
                    TAG,
                    "SearchViewQueryTextEvent: OnEvent: isSubmitted = ${it.isSubmitted}, text = ${it.queryText()}"
                )
            }
            .skip(1)
            .throttleFirst(100, TimeUnit.MILLISECONDS)
            .filter { queryEvent -> !TextUtils.isEmpty(queryEvent.queryText()) }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { queryEvent ->
                    if (queryEvent.isSubmitted) {
                        searchString = queryEvent.queryText().toString()
                        searchView.setQuery("", false)
                        searchView.clearFocus()
                        hideSuggestionsPanel()
                        search(searchString, this.contentFilter, sortFilter!!)
                    } else {
                        searchString = queryEvent.queryText().toString()
                        Log.d(TAG, "SearchViewQueryTextEvent: searchString = $searchString")
                        suggestionPublisher.onNext(searchString!!)
                        if (!isSuggestionPanelShowing)
                            showSuggestionsPanel()
                    }
                },

                {}
            )

        disposables.add(d)

//        val searchCloseButtonId = searchView.context.resources
//            .getIdentifier("android:id/search_close_btn", null, null)
//        val closeButton = searchView.findViewById<ImageView>(searchCloseButtonId)
//        val searchTextViewId = searchView.context.resources
//            .getIdentifier("android:id/search_src_text", null, null)
//        val searchTextView = searchView.findViewById<EditText>(searchTextViewId)
//
//        if (closeButton != null) {
//            val d1 = RxView.clicks(closeButton)
//                .subscribe {
//                    searchTextView.text.clear()
//                    suggestionListAdapter.setItems(emptyList())
//                    hideSuggestionsPanel()
//                }
//            compositeDisposable.add(d1)
//        }

        searchView.setOnCloseListener {
            Log.d(TAG, "onCloseListener called")
            hideSuggestionsPanel()
//            if (searchView.query.isEmpty()) {
//                // go back to MainFragment
//                false
//            } else {
//                // no working
//                // clear the list, and hide suggestion panel
//                suggestionListAdapter.setItems(arrayListOf())
//                false
//            }
            false

        }

        var itemId = 0
        var isFirstItem = true
        val context = context
        if (context != null) {
            for (filter in service!!.searchQHFactory.availableContentFilter) {
                menuItemToFilterName[itemId] = filter
                val item = menu.add(
                    1,
                    itemId++,
                    0,
                    ServiceHelper.getTranslatedFilterString(filter, context)
                )
                if (isFirstItem) {
                    item.isChecked = true
                    isFirstItem = false
                }
            }
            menu.setGroupCheckable(1, true, true)
            restoreFilterChecked(menu, filterItemCheckedId)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
            R.id.action_search -> {
            }
            R.id.action_add_tab -> {
                Log.d(TAG, "onOptionsItemSelected(): searchString = $searchString")
                val editView = LayoutInflater.from(activity)
                    .inflate(R.layout.dialog_input_prompts, null)

                android.app.AlertDialog.Builder(activity)
                    .setTitle("Tab Title")
                    .setView(editView)
                    .setPositiveButton("OK") { _, _ ->
                        searchFragmentTitle = editView.findViewById<EditText>(R.id.editUserInput).text.toString()
                        Log.d(
                            TAG,
                            "onOptionsItemSelected(): searchFragmentTitle = $searchFragmentTitle, sendingMessage"
                        )

                        // addSearchTab(searchFragmentTitle!!)
                        val intent = Intent(ACTION_ADD_TAB_MESSAGE)
                        // You can also include some extra data.
                        intent.putExtra(KEY_SEARCH_STRING, searchString)
                        intent.putExtra(KEY_TAB_TITLE, searchFragmentTitle)
                        LocalBroadcastManager.getInstance(activity!!).sendBroadcast(intent)

                    }
                    .setNegativeButton("Cancel") { dialog, which ->

                    }
                    .create()
                    .show()

            }
            else -> {
                if (item.groupId == 1) {
                    val contentFilter = ArrayList<String>(1)
                    contentFilter.add(menuItemToFilterName[item.itemId]!!)
                    changeContentFilter(item, contentFilter)
                }
            }
        }

        return true
    }

    private fun restoreFilterChecked(menu: Menu, itemId: Int) {
        if (itemId != -1) {
            val item = menu.findItem(itemId) ?: return

            item.isChecked = true
        }
    }

    private fun showSearchOnStart() {
        Log.d(TAG, "showSearchOnStart(): searchQuery = $searchString, lastSearchedQuery = $lastSearchedString")
//        searchEditText.setText(searchString)
//
//        if (TextUtils.isEmpty(searchString) || TextUtils.isEmpty(searchEditText.text)) {
//            searchToolbarContainer.apply {
//                translationX = 100f
//                alpha = 0f
//                visibility = View.VISIBLE
//                animate()
//                    .translationX(0f)
//                    .alpha(1f)
//                    .setDuration(200)
//                    .setInterpolator(DecelerateInterpolator())
//                    .start()
//            }
//        } else {
//            searchToolbarContainer.apply {
//                translationX = 0f
//                alpha = 1f
//                visibility = View.VISIBLE
//            }
//        }
    }

    private fun initSearchListeners() {
        Log.d(TAG, "initSearchListeners() called")


        suggestionListAdapter.setListener(object : SuggestionListAdapter.OnSuggestionItemSelected {
            override fun onSuggestionItemSelected(item: SuggestionItem) {
                search(item.query, arrayOf(), "")
                searchView.setQuery(item.query, true)
            }

            override fun onSuggestionItemInserted(item: SuggestionItem) {
                searchView.setQuery(item.query, false)
            }

            override fun onSuggestionItemLongClick(item: SuggestionItem) {
                if (item.fromHistory) showDeleteSuggestionDialog(item)
            }
        })

        if (suggestionDisposable == null || suggestionDisposable!!.isDisposed)
            initSuggestionObserver()
    }

    private fun unsetSearchListeners() {
        Log.d(TAG, "SearchFragment.unsetSearchListeners() called")
    }

    private fun showSuggestionsPanel() {
        Log.d(TAG, "showSuggestionsPanel() called")
        isSuggestionPanelShowing = true
        animateView(suggestionsPanel, true, 200, animationType = AnimationUtils.Type.LIGHT_SLIDE_AND_ALPHA)
    }

    private fun hideSuggestionsPanel() {
        Log.d(TAG, "hideSuggestionsPanel() called")
        isSuggestionPanelShowing = false
        animateView(suggestionsPanel, false, 200, animationType = AnimationUtils.Type.LIGHT_SLIDE_AND_ALPHA)
    }

    private fun showKeyboardSearch() {
        Log.d(TAG, "showKeyboardSearch() called")

        val inputMethodManager = activity!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.showSoftInput(fragmentView, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun hideKeyboardSearch() {
        Log.d(TAG, "hideKeyboardSearch() called")

        val inputMethodManager = activity!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(fragmentView.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)

    }

    private fun showDeleteSuggestionDialog(item: SuggestionItem) {
        if (activity == null) return

        val query = item.query
        AlertDialog.Builder(activity!!)
            .setTitle(query)
            .setMessage(R.string.delete_item_search_history)
            .setCancelable(true)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete) { _, _ ->
                val onDelete = historyRecordManager.deleteSearchHistory(query!!)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        {
                            viewModel.deleteSuggestionItem(item)
                        },
                        { throwable ->
                            showSnackBarError(
                                throwable,
                                UserAction.DELETE_FROM_HISTORY, "none",
                                "Deleting item failed", R.string.general_error
                            )
                        })
                compositeDisposable.add(onDelete)
            }
            .show()
    }

    override fun onBackPressed(): Boolean {
        if (suggestionsPanel.visibility == View.VISIBLE
            && listAdapter!!.itemsList.size > 0
            && !isLoading.get()
        ) {
            hideSuggestionsPanel()
            hideKeyboardSearch()
            searchView.setQuery(lastSearchedString, false)
            return true
        }
        return false
    }

    fun giveSearchEditTextFocus() {
        showKeyboardSearch()
    }

    // Todo: this should be down in repository
    private fun initSuggestionObserver() {
        Log.d(TAG, "initSuggestionObserver() called")
        if (suggestionDisposable != null && !suggestionDisposable!!.isDisposed) suggestionDisposable!!.dispose()

        val d = suggestionPublisher
            .subscribeOn(Schedulers.io())
            .debounce(SUGGESTIONS_DEBOUNCE.toLong(), TimeUnit.MILLISECONDS)
            .startWith(
                if (searchString != null)
                    searchString
                else
                    ""
            )
            .filter { isSuggestionsEnabled }
            .subscribe { queryString ->
                Log.d(TAG, "initSuggestionObserver() subscribe(): serviceId = $serviceId, searchString = $searchString")
                if (queryString != null) {
                    suggestionDisposable = viewModel.loadSuggestions(queryString)
                }
            }

        disposables.add(d)

    }

    override fun doInitialLoadLogic() {
        // no-op
    }

    private fun search(searchString: String?, contentFilter: Array<String>, sortFilter: String) {
        Log.d(
            TAG,
            "search() called with: query = [$searchString], contentFilter = $contentFilter, sortFilter = $sortFilter"
        )
        if (TextUtils.isEmpty(searchString)) return

//        compositeDisposable.clear()

        if (URLUtil.isValidUrl(searchString)) {
            try {
                val service = NewPipe.getServiceByUrl(searchString)
                val context = activity ?: return
                if (service != null) {
                    showLoading()
                    val d = Observable.fromCallable { NavigationHelper.getIntentByLink(context, service, searchString) }
                        .subscribeOn(Schedulers.computation())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                            { intent ->
                                fragmentManager?.popBackStackImmediate()
                                activity?.startActivity(intent)
                            },
                            {
                                showError(getString(R.string.url_not_supported_toast), false)
                            })

                    compositeDisposable.add(d)
                    return
                }
            } catch (e: Exception) {
                // Exception occurred, it's not a url -- Of course, because searchString is just earch text.
                Log.d(TAG, "Exception occurred, because searchString is not a url")
            }
        }

        lastSearchedString = this.searchString
        this.searchString = searchString
        listAdapter?.clearStreamItemList()
        hideSuggestionsPanel()
        hideKeyboardSearch()

        val d2 = viewModel.saveSearchString(serviceId, searchString!!)

        suggestionPublisher.onNext(searchString) // start suggestion
        startLoading(false)   // start loading search result
        compositeDisposable.add(d2)
    }

    public override fun startLoading(forceLoad: Boolean) {
        super.startLoading(forceLoad)
//        if (searchDisposable != null && !searchDisposable!!.isDisposed) searchDisposable!!.dispose()
        compositeDisposable.clear()
        searchDisposable = viewModel.search(serviceId, searchString!!, Arrays.asList(*contentFilter), sortFilter!!)
//        searchDisposable = ExtractorHelper.searchFor(serviceId, searchString!!, Arrays.asList(*contentFilter), sortFilter!!)
//            .subscribeOn(Schedulers.io())
//            .observeOn(AndroidSchedulers.mainThread())
//            .doOnEvent { _, _ -> isLoading.set(false) }
//            .subscribe({ result -> this.handleResult(result) },
//                { error -> this.onError(error) })
    }

    override fun loadMoreItems() {
        isLoading.set(true)
        showListFooter(true)
        if (searchDisposable != null && !searchDisposable!!.isDisposed) searchDisposable!!.dispose()

        searchDisposable = viewModel.loadMoreSearchResults(
            serviceId, searchString!!,
            Arrays.asList(*contentFilter), sortFilter!!, nextPageUrl!!
        )
    }

    override fun hasMoreItems(): Boolean {
        // TODO: No way to tell if search has more items in the moment
        return true
    }

    override fun onItemSelected(selectedItem: InfoItem) {
        super.onItemSelected(selectedItem)
        hideKeyboardSearch()
    }

    ///////////////////////////////////////////////////////////////////////////
    // Utils
    ///////////////////////////////////////////////////////////////////////////

    private fun changeContentFilter(item: MenuItem, contentFilter: List<String>) {
        this.filterItemCheckedId = item.itemId
        item.isChecked = true

        this.contentFilter = arrayOf(contentFilter[0])

        if (!TextUtils.isEmpty(searchString)) {
            search(searchString, this.contentFilter, sortFilter!!)
        }
    }

    private fun setQuery(serviceId: Int, searchString: String, contentfilter: Array<String>, sortFilter: String) {
        this.serviceId = serviceId
        this.searchString = searchString
        this.contentFilter = contentfilter
        this.sortFilter = sortFilter
        this.searchFragmentTitle = searchString

    }

    ///////////////////////////////////////////////////////////////////////////
    // Suggestion Results
    ///////////////////////////////////////////////////////////////////////////

    private fun handleSuggestions(suggestions: List<SuggestionItem>) {
        Log.d(TAG, "handleSuggestions(): suggestions = $suggestions")
        suggestionsRecyclerView.smoothScrollToPosition(0)
        suggestionsRecyclerView.post { suggestionListAdapter.setItems(suggestions) }

        if (errorPanelRoot.visibility == View.VISIBLE) {
            hideLoading()
        }
    }

    private fun onSuggestionError(exception: Throwable) {
        Log.d(TAG, "onSuggestionError(): exception = [$exception]")
        if (super.onError(exception)) return

        val errorId = if (exception is ParsingException)
            R.string.parsing_error
        else
            R.string.general_error
        onUnrecoverableError(
            exception, UserAction.GET_SUGGESTIONS,
            NewPipe.getNameOfService(serviceId), searchString!!, errorId
        )
    }

    ///////////////////////////////////////////////////////////////////////////
    // Contract
    ///////////////////////////////////////////////////////////////////////////

    override fun hideLoading() {
        super.hideLoading()
        showListFooter(false)
    }

    override fun showError(message: String, showRetryButton: Boolean) {
        super.showError(message, showRetryButton)
        hideSuggestionsPanel()
        hideKeyboardSearch()
    }

    ///////////////////////////////////////////////////////////////////////////
    // Search Results
    ///////////////////////////////////////////////////////////////////////////

    override fun handleResult(result: SearchInfo) {
        Log.d(
            TAG,
            "handleResult(result = $result): result.url = ${result.url}, result.nextPageUrl = ${result.nextPageUrl},  result.relatedItems = ${result.relatedItems}"
        )
        val exceptions = result.errors
        if (exceptions != null && !exceptions.isEmpty() && !(exceptions.size == 1 && exceptions[0] is SearchExtractor.NothingFoundException)) {
            showSnackBarError(
                result.errors, UserAction.SEARCHED,
                NewPipe.getNameOfService(serviceId), searchString!!, 0
            )
        }

        lastSearchedString = searchString
        nextPageUrl = result.nextPageUrl
        currentPageUrl = result.url

        if (listAdapter != null && listAdapter!!.itemsList.size == 0) {
            if (!result.relatedItems.isEmpty()) {
                listAdapter!!.addItems(result.relatedItems)
            } else {
                listAdapter!!.clearStreamItemList()
                showEmptyState()
                return
            }
        }

        super.handleResult(result)
    }

    override fun handleNextItems(result: ListExtractor.InfoItemsPage<*>) {
        showListFooter(false)
        currentPageUrl = result.nextPageUrl  // wrong!
        listAdapter!!.addItems(result.items)
        nextPageUrl = result.nextPageUrl

        if (!result.errors.isEmpty()) {
            showSnackBarError(
                result.errors, UserAction.SEARCHED,
                NewPipe.getNameOfService(serviceId), "\"$searchString\" → page: $nextPageUrl", 0
            )
        }
        super.handleNextItems(result)
    }

    override fun onError(exception: Throwable): Boolean {
        if (super.onError(exception)) return true

        if (exception is SearchExtractor.NothingFoundException) {
            listAdapter!!.clearStreamItemList()
            showEmptyState()
        } else {
            val errorId = if (exception is ParsingException)
                R.string.parsing_error
            else
                R.string.general_error
            onUnrecoverableError(
                exception, UserAction.SEARCHED,
                NewPipe.getNameOfService(serviceId), searchString!!, errorId
            )
        }

        return true
    }

    override fun actionOnSelectedValidStream(selectedItem: StreamInfoItem) {
        Log.d(TAG, "actionOnSelectedValidStream() selectedItem = $selectedItem")

        NavigationHelper.openAnchorPlayer(activity!!, selectedItem.serviceId, selectedItem.url, selectedItem.name)
//        NavigationHelper.openVideoDetailFragment(getFM(), selectedItem.serviceId, selectedItem.url, selectedItem.name)

    }


    companion object {
        private const val TAG = "SearchFragment"
        ///////////////////////////////////////////////////////////////////////////
        // Search
        ///////////////////////////////////////////////////////////////////////////

        /**
         * The suggestions will only be fetched getTabFrom network if the query meet this threshold (>=).
         * (local ones will be fetched regardless of the length)
         */
        private const val THRESHOLD_NETWORK_SUGGESTION = 1

        /**
         * How much time have to pass without emitting a item (i.e. the user stop typing) to fetch/show the suggestions, in milliseconds.
         */
        private const val SUGGESTIONS_DEBOUNCE = 120 //ms

        //////////////////////////////////////////////////////////////////////////

        fun getInstance(serviceId: Int, searchString: String): NewSearchFragment {
            val searchFragment = NewSearchFragment()
            searchFragment.setQuery(serviceId, searchString, arrayOf(), "")

            if (!TextUtils.isEmpty(searchString)) {
                searchFragment.setSearchOnResume()
            }

            return searchFragment
        }
    }
}

