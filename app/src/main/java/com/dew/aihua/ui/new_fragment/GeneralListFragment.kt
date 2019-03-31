package com.dew.aihua.ui.new_fragment

import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dew.aihua.R
import com.dew.aihua.player.helper.AnimationUtils
import com.dew.aihua.ui.adapter.GeneralListAdapter


/**
 *  Created by Edward on 3/22/2019.
 */
abstract class GeneralListFragment<I, N, ItemType: Any, AdapterType: GeneralListAdapter<ItemType>>: NewBaseStateFragment<I>(),
    SharedPreferences.OnSharedPreferenceChangeListener {
    ///////////////////////////////////////////////////////////////////////////
    // Views
    ///////////////////////////////////////////////////////////////////////////
    protected var itemsList: RecyclerView? = null
    private var updateFlags = FLAG_NO_UPDATE

    abstract var listAdapter: AdapterType?

    ///////////////////////////////////////////////////////////////////////////
    // Init
    ///////////////////////////////////////////////////////////////////////////

    protected open fun getListHeader(): View? = null   // no ListHeaderView

    protected open fun getListFooter(): View =
        activity!!.layoutInflater.inflate(R.layout.pignate_footer, itemsList, false)

    private fun getListLayoutManager(): RecyclerView.LayoutManager = LinearLayoutManager(activity) as RecyclerView.LayoutManager

    private fun getGridLayoutManager(): RecyclerView.LayoutManager {
        val resources = activity!!.resources
        val width = if (resources.displayMetrics.widthPixels > resources.displayMetrics.heightPixels) {
            if (resources.getBoolean(R.bool.isTablet))
                resources.displayMetrics.widthPixels / 3
            else
                resources.displayMetrics.widthPixels / 2
        } else {
            if (resources.getBoolean(R.bool.isTablet))
                resources.displayMetrics.widthPixels / 2
            else
                resources.displayMetrics.widthPixels
        }
        Log.d(TAG, "resources.displayMetrics.widthPixels = ${resources.displayMetrics.widthPixels}")

        val spanCount = Math.floor(resources.displayMetrics.widthPixels / width.toDouble()).toInt()
        val layoutManager = GridLayoutManager(activity, spanCount)
        if (listAdapter != null) {
            layoutManager.spanSizeLookup = listAdapter!!.getSpanSizeLookup(spanCount)
        } else {
            layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return spanCount
                }
            }
        }

        return layoutManager
    }


    ///////////////////////////////////////////////////////////////////////////
    // LifeCycle
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

        hideLoading()

        if (updateFlags != FLAG_NO_UPDATE) {
            if (updateFlags and LIST_MODE_UPDATE_FLAG != FLAG_NO_UPDATE) {
                val useGrid = isGridLayout
                Log.d(TAG, "onResume(): useGrid = $useGrid ")
                itemsList?.layoutManager = if (useGrid) getGridLayoutManager() else getListLayoutManager()
                listAdapter?.setGridItemVariants(useGrid)
                listAdapter?.notifyDataSetChanged()
            }
            updateFlags = FLAG_NO_UPDATE
        }
    }


    override fun initViews(rootView: View, savedInstanceState: Bundle?) {
        super.initViews(rootView, savedInstanceState)

        val useGrid = isGridLayout
        listAdapter?.let {
            it.setGridItemVariants(useGrid)
            it.footer = getListFooter()
            it.header = getListHeader()
        }

        itemsList = rootView.findViewById(R.id.items_list)
        itemsList?.let {
            it.layoutManager = if (useGrid) getGridLayoutManager() else getListLayoutManager()
            it.adapter = listAdapter
        }
    }


    ///////////////////////////////////////////////////////////////////////////
    // Menu
    ///////////////////////////////////////////////////////////////////////////

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        Log.d(TAG, "onCreateOptionsMenu() called with: menu = [$menu], inflater = [$inflater]")
        super.onCreateOptionsMenu(menu, inflater)
        val supportActionBar = activity!!.supportActionBar ?: return
        supportActionBar.setDisplayShowTitleEnabled(true)

        // Todo 6: this need to reconsider
        if (isUsedAsFrontPage) {
            supportActionBar.setDisplayHomeAsUpEnabled(false)
        } else {
            supportActionBar.setDisplayHomeAsUpEnabled(true)
        }
    }


    ///////////////////////////////////////////////////////////////////////////
    // Lifecycle - Destruction
    ///////////////////////////////////////////////////////////////////////////

    override fun onDestroyView() {
        super.onDestroyView()
        itemsList = null
        listAdapter = null
    }

    ///////////////////////////////////////////////////////////////////////////
    // Contract
    ///////////////////////////////////////////////////////////////////////////

    override fun showLoading() {
        super.showLoading()
        AnimationUtils.animateView(itemsList!!, false, 400)
    }

    override fun hideLoading() {
        super.hideLoading()
        AnimationUtils.animateView(itemsList!!, true, 300)
    }

    override fun showError(message: String, showRetryButton: Boolean) {
        super.showError(message, showRetryButton)
        showListFooter(false)
        AnimationUtils.animateView(itemsList!!, false, 200)
    }

    override fun showEmptyState() {
        super.showEmptyState()
        showListFooter(false)
    }

    protected open fun showListFooter(show: Boolean) {
        itemsList?.post {
                listAdapter?.showFooter(show)
        }
    }

    protected open fun handleNextItems(result: N) {
        isLoading.set(false)
    }

    ///////////////////////////////////////////////////////////////////////////
    // Error handling
    ///////////////////////////////////////////////////////////////////////////
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        if (key == getString(R.string.list_view_mode_key)) {
            updateFlags = updateFlags or LIST_MODE_UPDATE_FLAG
        }
    }

    override fun startLoading(forceLoad: Boolean) {
        super.startLoading(forceLoad)
        resetFragment()
    }

    private fun resetFragment() {
        listAdapter?.itemsList?.clear()
    }

    protected val isGridLayout: Boolean
        get() {
            val listMode = PreferenceManager.getDefaultSharedPreferences(activity).getString(
                getString(R.string.list_view_mode_key), getString(
                    R.string.list_view_mode_value
                )
            )
            return when (listMode) {
                "list" -> {
                    val configuration = resources.configuration
                    configuration.orientation == Configuration.ORIENTATION_LANDSCAPE &&
                            configuration.isLayoutSizeAtLeast(Configuration.SCREENLAYOUT_SIZE_LARGE)
                }

                "auto",
                "grid" -> true
                else -> false
            }
        }

    companion object {
        private const val TAG = "BaseListFragment"
        private const val FLAG_NO_UPDATE = 0
        private const val LIST_MODE_UPDATE_FLAG = 0x32
    }
}