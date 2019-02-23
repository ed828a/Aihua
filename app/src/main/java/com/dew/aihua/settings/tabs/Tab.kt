package com.dew.aihua.settings.tabs

import android.content.Context
import android.util.Log
import androidx.annotation.DrawableRes
import com.dew.aihua.R
import com.dew.aihua.local.bookmark.BookmarkFragment
import com.dew.aihua.local.feed.FeedFragment
import com.dew.aihua.local.history.StatisticsPlaylistFragment
import com.dew.aihua.local.subscription.SubscriptionFragment
import com.dew.aihua.settings.tabs.Tab.Type.Companion.BLANK_TAB_ID
import com.dew.aihua.settings.tabs.Tab.Type.Companion.BOOKMARK_TAB_ID
import com.dew.aihua.settings.tabs.Tab.Type.Companion.CHANNEL_TAB_ID
import com.dew.aihua.settings.tabs.Tab.Type.Companion.FEED_TAB_ID
import com.dew.aihua.settings.tabs.Tab.Type.Companion.HISTORY_TAB_ID
import com.dew.aihua.settings.tabs.Tab.Type.Companion.KIOSK_TAB_ID
import com.dew.aihua.settings.tabs.Tab.Type.Companion.SUBSCRIPTION_TAB_ID
import com.dew.aihua.ui.fragment.BlankFragment
import com.dew.aihua.ui.fragment.ChannelFragment
import com.dew.aihua.ui.fragment.KioskFragment
import com.dew.aihua.util.Constants.NO_ID_STRING
import com.dew.aihua.util.Constants.NO_NAME_STRING
import com.dew.aihua.util.Constants.NO_SERVICE_ID
import com.dew.aihua.util.Constants.NO_URL_STRING
import com.dew.aihua.util.KioskTranslator
import com.dew.aihua.util.ThemeHelper
import com.grack.nanojson.JsonObject
import com.grack.nanojson.JsonSink
import org.schabi.newpipe.extractor.exceptions.ExtractionException

/**
 *  Created by Edward on 2/23/2019.
 */

abstract class Tab(jsonObject: JsonObject? = null) {
    init {
        jsonObject?.let {
            readDataFromJson(it)
        }
    }

    abstract val tabId: Int

    /**
     * Return a instance of the fragment that this tab represent.
     */
    abstract val fragment: androidx.fragment.app.Fragment

    abstract fun getTabName(context: Context): String
    @DrawableRes
    abstract fun getTabIconRes(context: Context): Int

    override fun equals(other: Any?): Boolean =
        (other is Tab) && other.javaClass == this.javaClass && other.tabId == this.tabId


    fun writeJsonOn(jsonSink: JsonSink<*>) {
        jsonSink.`object`()

        jsonSink.value(JSON_TAB_ID_KEY, tabId)
        writeDataToJson(jsonSink)

        jsonSink.end()
    }

    protected open fun writeDataToJson(writerSink: JsonSink<*>) {
        // No-op
    }

    protected open fun readDataFromJson(jsonObject: JsonObject) {
        // No-op
    }

    ///////////////////////////////////////////////////////////////////////////
    // Implementations
    ///////////////////////////////////////////////////////////////////////////

    enum class Type(val tab: Tab) {
        BLANK(BlankTab()),
        SUBSCRIPTIONS(SubscriptionsTab()),
        FEED(FeedTab()),
        BOOKMARKS(BookmarksTab()),
        HISTORY(HistoryTab()),
        KIOSK(KioskTab()),
        CHANNEL(ChannelTab());

        val tabId: Int
            get() = tab.tabId

        companion object {
            const val BLANK_TAB_ID = 0
            const val SUBSCRIPTION_TAB_ID = 1
            const val FEED_TAB_ID = 2
            const val BOOKMARK_TAB_ID = 3
            const val HISTORY_TAB_ID = 4
            const val KIOSK_TAB_ID = 5
            const val CHANNEL_TAB_ID = 6
        }
    }

    class BlankTab : Tab() {

        override val tabId: Int
            get() = BLANK_TAB_ID

        override val fragment: BlankFragment
            get() = BlankFragment()

        override fun getTabName(context: Context): String {
            return context.getString(R.string.blank_page_summary)
        }

        @DrawableRes
        override fun getTabIconRes(context: Context): Int {
            return ThemeHelper.resolveResourceIdFromAttr(context, R.attr.ic_blank_page)
        }

    }

    class SubscriptionsTab : Tab() {

        override val tabId: Int
            get() = SUBSCRIPTION_TAB_ID

        override val fragment: SubscriptionFragment
            get() = SubscriptionFragment()

        override fun getTabName(context: Context): String {
            return context.getString(R.string.tab_subscriptions)
        }

        @DrawableRes
        override fun getTabIconRes(context: Context): Int {
            return ThemeHelper.resolveResourceIdFromAttr(context, R.attr.ic_channel)
        }
    }

    class FeedTab : Tab() {

        override val tabId: Int
            get() = FEED_TAB_ID

        override val fragment: FeedFragment
            get() = FeedFragment()

        override fun getTabName(context: Context): String {
            return context.getString(R.string.fragment_whats_new)
        }

        @DrawableRes
        override fun getTabIconRes(context: Context): Int {
            return ThemeHelper.resolveResourceIdFromAttr(context, R.attr.rss)
        }
    }

    class BookmarksTab : Tab() {

        override val tabId: Int
            get() = BOOKMARK_TAB_ID

        override val fragment: BookmarkFragment
            get() = BookmarkFragment()

        override fun getTabName(context: Context): String {
            return context.getString(R.string.tab_bookmarks)
        }

        @DrawableRes
        override fun getTabIconRes(context: Context): Int {
            return ThemeHelper.resolveResourceIdFromAttr(context, R.attr.ic_bookmark)
        }
    }

    class HistoryTab : Tab() {

        override val tabId: Int
            get() = HISTORY_TAB_ID

        override val fragment: StatisticsPlaylistFragment
            get() = StatisticsPlaylistFragment()

        override fun getTabName(context: Context): String {
            return context.getString(R.string.title_activity_history)
        }

        @DrawableRes
        override fun getTabIconRes(context: Context): Int {
            return ThemeHelper.resolveResourceIdFromAttr(context, R.attr.history)
        }
    }

    class KioskTab : Tab {

        var kioskServiceId: Int = 0
            private set
        var kioskId: String? = null
            private set

        override val tabId: Int
            get() = KIOSK_TAB_ID

        override val fragment: KioskFragment
            @Throws(ExtractionException::class)
            get() = KioskFragment.getInstance(kioskServiceId, kioskId!!)

//        constructor() : this(-1, "<no-id>") {}

        constructor(kioskServiceId: Int = NO_SERVICE_ID, kioskId: String = "<no-id>") {
            this.kioskServiceId = kioskServiceId
            this.kioskId = kioskId
        }

        constructor(jsonObject: JsonObject) : super(jsonObject) {}

        override fun getTabName(context: Context): String {
            val id = kioskId ?: ""
            return KioskTranslator.getTranslatedKioskName(id, context)
        }

        @DrawableRes
        override fun getTabIconRes(context: Context): Int {
            val id = kioskId ?: ""
            val kioskIcon = KioskTranslator.getKioskIcons(id, context)

            if (kioskIcon <= 0) {
                throw IllegalStateException("Kiosk ID is not valid: \"$kioskId\"")
            }

            return kioskIcon
        }

        override fun writeDataToJson(writerSink: JsonSink<*>) {
            writerSink.value(JSON_KIOSK_SERVICE_ID_KEY, kioskServiceId)
                .value(JSON_KIOSK_ID_KEY, kioskId)
        }

        override fun readDataFromJson(jsonObject: JsonObject) {
            kioskServiceId = jsonObject.getInt(JSON_KIOSK_SERVICE_ID_KEY, NO_SERVICE_ID)
            kioskId = jsonObject.getString(JSON_KIOSK_ID_KEY, NO_ID_STRING)
        }

        companion object {
            private const val JSON_KIOSK_SERVICE_ID_KEY = "service_id"
            private const val JSON_KIOSK_ID_KEY = "kiosk_id"
        }
    }

    class ChannelTab : Tab {

        var channelServiceId: Int = 0
            private set
        var channelUrl: String? = null
            private set
        var channelName: String? = null
            private set

        override val tabId: Int
            get() = CHANNEL_TAB_ID

        override val fragment: ChannelFragment
            get() {
                Log.d(TAG, "ChannelTab::getFragment called")
                return ChannelFragment.getInstance(channelServiceId, channelUrl!!, channelName!!)
            }

        constructor() : this(NO_SERVICE_ID, NO_URL_STRING, NO_NAME_STRING) {
            Log.d(TAG, "ChannelTab() called with No_SERVICE_ID")
        }

        constructor(channelServiceId: Int, channelUrl: String, channelName: String) {
            this.channelServiceId = channelServiceId
            this.channelUrl = channelUrl
            this.channelName = channelName
        }

        constructor(jsonObject: JsonObject) : super(jsonObject) {}

        override fun getTabName(context: Context): String {
            return channelName!!
        }

        @DrawableRes
        override fun getTabIconRes(context: Context): Int {
            return ThemeHelper.resolveResourceIdFromAttr(context, R.attr.ic_channel)
        }

        override fun writeDataToJson(writerSink: JsonSink<*>) {
            writerSink.value(JSON_CHANNEL_SERVICE_ID_KEY, channelServiceId)
                .value(JSON_CHANNEL_URL_KEY, channelUrl)
                .value(JSON_CHANNEL_NAME_KEY, channelName)
        }

        override fun readDataFromJson(jsonObject: JsonObject) {
            channelServiceId = jsonObject.getInt(JSON_CHANNEL_SERVICE_ID_KEY, NO_SERVICE_ID)
            channelUrl = jsonObject.getString(JSON_CHANNEL_URL_KEY, NO_URL_STRING)
            channelName = jsonObject.getString(JSON_CHANNEL_NAME_KEY, NO_NAME_STRING)
        }

        companion object {
            private const val JSON_CHANNEL_SERVICE_ID_KEY = "channel_service_id"
            private const val JSON_CHANNEL_URL_KEY = "channel_url"
            private const val JSON_CHANNEL_NAME_KEY = "channel_name"
        }
    }

    companion object {
        private val TAG = Tab::class.simpleName

        ///////////////////////////////////////////////////////////////////////////
        // JSON Handling
        ///////////////////////////////////////////////////////////////////////////

        private const val JSON_TAB_ID_KEY = "tab_id"

        ///////////////////////////////////////////////////////////////////////////
        // Tab Handling
        ///////////////////////////////////////////////////////////////////////////

        fun getTabFrom(jsonObject: JsonObject): Tab? {
            Log.d(TAG, "getTabFrom(jsonObject: JsonObject): $jsonObject")
            val tabId = jsonObject.getInt(Tab.JSON_TAB_ID_KEY, NO_SERVICE_ID)

            return if (tabId == NO_SERVICE_ID) {
                null
            } else getTabFrom(tabId, jsonObject)

        }

        fun getTabFrom(tabId: Int): Tab? {
            Log.d(TAG, " getTabFrom(tabId: Int): tabId = $tabId")
            return getTabFrom(tabId, null)
        }

        fun getTypeFrom(tabId: Int): Type? {
            for (available in Type.values()) {
                if (available.tabId == tabId) {
                    Log.d(TAG, "getType: Type.value = $available")
                    return available
                }
            }
            return null
        }

        private fun getTabFrom(tabId: Int, jsonObject: JsonObject?): Tab? {
            val type = getTypeFrom(tabId) ?: return null

            if (jsonObject != null) {
                when (tabId) {
                    KIOSK_TAB_ID -> {
                        Log.d(TAG, "getTabFrom(tabId, jsonObject): tabId = KIOSK_TAB_ID")
                        return KioskTab(jsonObject)
                    }
                    CHANNEL_TAB_ID -> {
                        Log.d(TAG, "getTabFrom(tabId, jsonObject): tabId = CHANNEL_TAB_ID")
                        return ChannelTab(jsonObject)
                    }
                }
            }

            return type.tab
        }
    }
}
