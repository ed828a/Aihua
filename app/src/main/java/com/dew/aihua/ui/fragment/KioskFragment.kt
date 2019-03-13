package com.dew.aihua.ui.fragment

import android.os.Bundle
import android.util.Log
import android.view.*
import com.dew.aihua.R
import com.dew.aihua.player.helper.AnimationUtils.animateView
import com.dew.aihua.player.helper.ExtractorHelper
import com.dew.aihua.report.UserAction
import com.dew.aihua.util.KioskTranslator
import icepick.State
import io.reactivex.Single
import org.schabi.newpipe.extractor.ListExtractor
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.kiosk.KioskInfo

/**
 *  Created by Edward on 3/2/2019.
 */
class KioskFragment : BaseListInfoFragment<KioskInfo>() {

    @State
    @JvmField
    var kioskId = ""
    private lateinit var kioskTranslatedName: String

    ///////////////////////////////////////////////////////////////////////////
    // LifeCycle
    ///////////////////////////////////////////////////////////////////////////

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        kioskTranslatedName = KioskTranslator.getTranslatedKioskName(kioskId, activity!!)
        name = kioskTranslatedName
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        if (useAsFrontPage && isVisibleToUser && activity != null) {
            try {
                setTitle(kioskTranslatedName)
            } catch (e: Exception) {
                onUnrecoverableError(e, UserAction.UI_ERROR,
                    "none",
                    "none", R.string.app_ui_crash)
            }

        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_kiosk, container, false)
    }

    ///////////////////////////////////////////////////////////////////////////
    // Menu
    ///////////////////////////////////////////////////////////////////////////

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        val supportActionBar = activity!!.supportActionBar
        if (supportActionBar != null && useAsFrontPage) {
            supportActionBar.setDisplayHomeAsUpEnabled(false)
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Load and handle
    ///////////////////////////////////////////////////////////////////////////

    public override fun loadResult(forceLoad: Boolean): Single<KioskInfo> {

        Log.d(TAG, "loadResult(forceLoad=$forceLoad), serviceId = $serviceId, url = $url")

        return ExtractorHelper.getKioskInfo(serviceId,
            url,
            forceLoad)
    }

    public override fun loadMoreItemsLogic(): Single<ListExtractor.InfoItemsPage<*>> {
        Log.d(TAG, "loadMoreItemsLogic(): serviceId = $serviceId, url = $url")
        return ExtractorHelper.getMoreKioskItems(serviceId,
            url,
            currentNextPageUrl)
    }

    ///////////////////////////////////////////////////////////////////////////
    // Contract
    ///////////////////////////////////////////////////////////////////////////

    override fun showLoading() {
        super.showLoading()
        animateView(itemsList!!, false, 100)
    }

    override fun handleResult(result: KioskInfo) {
        super.handleResult(result)

        name = kioskTranslatedName
        if (!useAsFrontPage) {
            setTitle(kioskTranslatedName)
        }

        if (!result.errors.isEmpty()) {
            showSnackBarError(result.errors,
                UserAction.REQUESTED_KIOSK,
                NewPipe.getNameOfService(result.serviceId), result.url, 0)
        }
    }

    override fun handleNextItems(result: ListExtractor.InfoItemsPage<*>) {
        super.handleNextItems(result)

        if (!result.errors.isEmpty()) {
            showSnackBarError(result.errors,
                UserAction.REQUESTED_PLAYLIST, NewPipe.getNameOfService(serviceId), "Get next page of: $url", 0)
        }
    }

    companion object {
        private val TAG = KioskFragment::class.java.simpleName

        @Throws(ExtractionException::class)
        @JvmOverloads
        fun getInstance(serviceId: Int, kioskId: String = NewPipe.getService(serviceId)
            .kioskList
            .defaultKioskId): KioskFragment {
            val instance = KioskFragment()
            val service = NewPipe.getService(serviceId)
            val kioskLinkHandlerFactory = service.kioskList
                .getListLinkHandlerFactoryByType(kioskId)
            instance.setInitialData(serviceId,
                kioskLinkHandlerFactory.fromId(kioskId).url, kioskId)
            instance.kioskId = kioskId
            return instance
        }
    }
}