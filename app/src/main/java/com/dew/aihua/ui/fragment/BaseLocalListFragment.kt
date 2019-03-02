package com.dew.aihua.ui.fragment

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import androidx.preference.PreferenceManager
import com.dew.aihua.R
import com.dew.aihua.local.adapter.LocalItemListAdapter
import com.dew.aihua.player.helper.AnimationUtils.animateView
import com.dew.aihua.ui.contract.ListViewContract

/**
 *  Created by Edward on 3/2/2019.
 *
 * This fragment is design to be used with persistent data such as
 * [org.schabi.newpipe.database.LocalItem], and does not cache the data contained
 * in the list adapter to avoid extra writes when the it exits or re-enters its lifecycle.
 *
 * This fragment destroys its adapter and views when [Fragment.onDestroyView] is
 * called and is memory efficient when in backstack.
 */
abstract class BaseLocalListFragment<I, N> : BaseStateFragment<I>(), ListViewContract<I, N>, SharedPreferences.OnSharedPreferenceChangeListener {

    ///////////////////////////////////////////////////////////////////////////
    // Views
    ///////////////////////////////////////////////////////////////////////////

    protected var headerRootView: View? = null
    protected var footerRootView: View? = null

    protected var itemListAdapter: LocalItemListAdapter? = null
    protected var itemsList: androidx.recyclerview.widget.RecyclerView? = null
    private var updateFlags = FLAG_NO_UPDATE

    ///////////////////////////////////////////////////////////////////////////
    // Lifecycle - View
    ///////////////////////////////////////////////////////////////////////////

    protected open fun getListHeader(): View? = null

    private fun getListFooter(): View = activity!!.layoutInflater.inflate(R.layout.pignate_footer, itemsList, false)

    private fun getGridLayoutManager(): androidx.recyclerview.widget.RecyclerView.LayoutManager {
        val resources = activity!!.resources
        var width = resources.getDimensionPixelSize(R.dimen.video_item_grid_thumbnail_image_width)
        width += (24 * resources.displayMetrics.density).toInt()
        val spanCount = Math.floor(resources.displayMetrics.widthPixels / width.toDouble()).toInt()
        val layoutManager = androidx.recyclerview.widget.GridLayoutManager(activity, spanCount)
        layoutManager.spanSizeLookup = itemListAdapter!!.getSpanSizeLookup(spanCount)
        return layoutManager
    }

    private fun getListLayoutManager(): androidx.recyclerview.widget.RecyclerView.LayoutManager = androidx.recyclerview.widget.LinearLayoutManager(activity)

    /**
     * when screen size is big enough and Landscaped, it's grid layout. or listMode is set to grid.
     */
//    protected fun isGridLayout(): Boolean {
//        val listMode = PreferenceManager.getDefaultSharedPreferences(activity).getString(getString(R.string.list_view_mode_key), getString(R.string.list_view_mode_value))
//
//        return when (listMode){
//            "list" -> {
//                val configuration = resources.configuration
//                configuration.orientation == Configuration.ORIENTATION_LANDSCAPE &&
//                        configuration.isLayoutSizeAtLeast(Configuration.SCREENLAYOUT_SIZE_LARGE)
//            }
//
//            "auto",
//            "grid"-> true
//            else -> false
//        }
//    }

    ///////////////////////////////////////////////////////////////////////////
    // Lifecycle - Creation
    ///////////////////////////////////////////////////////////////////////////

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        PreferenceManager.getDefaultSharedPreferences(activity)
            .registerOnSharedPreferenceChangeListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        PreferenceManager.getDefaultSharedPreferences(activity)
            .unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onResume() {
        super.onResume()
        if (updateFlags != FLAG_NO_UPDATE) {
            if ((updateFlags and LIST_MODE_UPDATE_FLAG) != FLAG_NO_UPDATE) {
                val useGrid = isGridLayout
                Log.d(TAG, "onResume(): isGridLayout() = $useGrid")
                itemsList?.layoutManager = if (useGrid) getGridLayoutManager() else getListLayoutManager()
                itemListAdapter?.setGridItemVariants(useGrid)
                itemListAdapter?.notifyDataSetChanged()
            }
            updateFlags = FLAG_NO_UPDATE
        }
    }

    override fun initViews(rootView: View, savedInstanceState: Bundle?) {
        super.initViews(rootView, savedInstanceState)

        itemListAdapter = LocalItemListAdapter(activity)

        val useGrid = isGridLayout
        Log.d(TAG, "initViews(): isGridLayout() = $useGrid")
        itemsList = rootView.findViewById(R.id.items_list)
        itemsList?.layoutManager = if (useGrid) getGridLayoutManager() else getListLayoutManager()

        itemListAdapter?.setGridItemVariants(useGrid)
        headerRootView = getListHeader()
        headerRootView?.let {
            itemListAdapter?.setHeader(it)
        }

        footerRootView = getListFooter()
        itemListAdapter?.setFooter(footerRootView!!)

        itemsList?.adapter = itemListAdapter
    }

    override fun initListeners() {
        super.initListeners()
    }

    ///////////////////////////////////////////////////////////////////////////
    // Lifecycle - Menu
    ///////////////////////////////////////////////////////////////////////////

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        Log.d(TAG, "onCreateOptionsMenu() called with: menu = [$menu], inflater = [$inflater]")

        val supportActionBar = activity?.supportActionBar ?: return

        supportActionBar.setDisplayShowTitleEnabled(true)
    }

    ///////////////////////////////////////////////////////////////////////////
    // Lifecycle - Destruction
    ///////////////////////////////////////////////////////////////////////////

    override fun onDestroyView() {
        super.onDestroyView()
        itemsList = null
        itemListAdapter = null
    }

    ///////////////////////////////////////////////////////////////////////////
    // Contract
    ///////////////////////////////////////////////////////////////////////////

    public override fun startLoading(forceLoad: Boolean) {
        super.startLoading(forceLoad)
        resetFragment()
    }

    override fun showLoading() {
        super.showLoading()
        if (itemsList != null) animateView(itemsList!!, false, 200)
        if (headerRootView != null) animateView(headerRootView!!, false, 200)
    }

    override fun hideLoading() {
        super.hideLoading()
        if (itemsList != null) animateView(itemsList!!, true, 200)
        if (headerRootView != null) animateView(headerRootView!!, true, 200)
    }

    override fun showError(message: String, showRetryButton: Boolean) {
        super.showError(message, showRetryButton)
        showListFooter(false)

        if (itemsList != null) animateView(itemsList!!, false, 200)
        if (headerRootView != null) animateView(headerRootView!!, false, 200)
    }

    override fun showEmptyState() {
        super.showEmptyState()
        showListFooter(false)
    }

    override fun showListFooter(show: Boolean) {
//        if (itemsList == null) return
        itemsList?.post { itemListAdapter?.showFooter(show) }
    }

    override fun handleNextItems(result: N) {
        isLoading.set(false)
    }

    ///////////////////////////////////////////////////////////////////////////
    // Error handling
    ///////////////////////////////////////////////////////////////////////////

    protected open fun resetFragment() {
        itemListAdapter?.clearStreamItemList()
    }

    override fun onError(exception: Throwable): Boolean {
        resetFragment()
        return super.onError(exception)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        if (key == getString(R.string.list_view_mode_key)) {
            updateFlags = updateFlags or LIST_MODE_UPDATE_FLAG
        }
    }

    companion object {
        const val FLAG_NO_UPDATE = 0
        private const val LIST_MODE_UPDATE_FLAG = 0x32
    }
}
