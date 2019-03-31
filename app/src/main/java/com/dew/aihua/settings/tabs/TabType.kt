package com.dew.aihua.settings.tabs

/**
 *  Created by Edward on 3/31/2019.
 */

enum class TabType(val tab: Tab) {
    BLANK(BlankTab()),
    SUBSCRIPTIONS(SubscriptionsTab()),
    FEED(FeedTab()),
    BOOKMARKS(BookmarksTab()),
    HISTORY(HistoryTab()),
    KIOSK(KioskTab()),
    CHANNEL(ChannelTab()),
    SEARCH(SearchTab());

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
        const val SEARCH_TAB_ID = 7
    }
}