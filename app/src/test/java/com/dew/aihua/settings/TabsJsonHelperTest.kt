package com.dew.aihua.settings

import com.dew.aihua.settings.tabs.Tab
import com.dew.aihua.settings.tabs.TabsJsonHelper
import com.grack.nanojson.JsonObject
import com.grack.nanojson.JsonParser
import com.grack.nanojson.JsonParserException
import org.junit.Assert
import org.junit.Test
import java.util.*

/**
 *  Created by Edward on 2/23/2019.
 */

class TabsJsonHelperTest {

    @Test
    @Throws(TabsJsonHelper.InvalidJsonException::class)
    fun testEmptyAndNullRead() {
        val emptyTabsJson = "{\"$JSON_TABS_ARRAY_KEY\":[]}"
        var items = TabsJsonHelper.getTabsFromJson(emptyTabsJson)
        // Check if instance is the same
        Assert.assertTrue(items === TabsJsonHelper.FALLBACK_INITIAL_TABS_LIST)

        val nullSource: String? = null
        items = TabsJsonHelper.getTabsFromJson(nullSource)
        Assert.assertTrue(items === TabsJsonHelper.FALLBACK_INITIAL_TABS_LIST)
    }

    @Test
    @Throws(TabsJsonHelper.InvalidJsonException::class)
    fun testInvalidIdRead() {
        val blankTabId = Tab.Type.BLANK.tabId
        val emptyTabsJson = "{\"" + JSON_TABS_ARRAY_KEY + "\":[" +
                "{\"" + JSON_TAB_ID_KEY + "\":" + blankTabId + "}," +
                "{\"" + JSON_TAB_ID_KEY + "\":" + 12345678 + "}" +
                "]}"
        val items = TabsJsonHelper.getTabsFromJson(emptyTabsJson)

        Assert.assertEquals("Should ignore the tab with invalid id", 1, items.size.toLong())
        Assert.assertEquals(blankTabId.toLong(), items[0].tabId.toLong())
    }

    @Test
    fun testInvalidRead() {
        val invalidList = Arrays.asList(
            "{\"notTabsArray\":[]}",
            "{invalidJSON]}",
            "{}"
        )

        for (invalidContent in invalidList) {
            try {
                TabsJsonHelper.getTabsFromJson(invalidContent)

                Assert.fail("didn't throw exception")
            } catch (e: Exception) {
                val isExpectedException = e is TabsJsonHelper.InvalidJsonException
                Assert.assertTrue(
                    "\"" + e.javaClass.simpleName + "\" is not the expected exception",
                    isExpectedException
                )
            }

        }
    }

    @Test
    @Throws(JsonParserException::class)
    fun testEmptyAndNullSave() {
        val emptyList = emptyList<Tab>()
        var returnedJson = TabsJsonHelper.getJsonToSave(emptyList)
        Assert.assertTrue(isTabsArrayEmpty(returnedJson))

        val nullList: List<Tab>? = null
        returnedJson = TabsJsonHelper.getJsonToSave(nullList)
        Assert.assertTrue(isTabsArrayEmpty(returnedJson))
    }

    @Throws(JsonParserException::class)
    private fun isTabsArrayEmpty(returnedJson: String): Boolean {
        val jsonObject = JsonParser.`object`().from(returnedJson)
        Assert.assertTrue(jsonObject.containsKey(JSON_TABS_ARRAY_KEY))
        return jsonObject.getArray(JSON_TABS_ARRAY_KEY).size == 0
    }

    @Test
    @Throws(JsonParserException::class)
    fun testSaveAndReading() {
        // Saving
        val blankTab = Tab.BlankTab()
        val subscriptionsTab = Tab.SubscriptionsTab()
        val channelTab = Tab.ChannelTab(666, "https://example.org", "testName")
        val kioskTab = Tab.KioskTab(123, "trending_key")

        val tabs = Arrays.asList(blankTab, subscriptionsTab, channelTab, kioskTab)
        val returnedJson = TabsJsonHelper.getJsonToSave(tabs)

        // Reading
        val jsonObject = JsonParser.`object`().from(returnedJson)
        Assert.assertTrue(jsonObject.containsKey(JSON_TABS_ARRAY_KEY))
        val tabsFromArray = jsonObject.getArray(JSON_TABS_ARRAY_KEY)

        Assert.assertEquals(tabs.size.toLong(), tabsFromArray.size.toLong())

        val blankTabFromReturnedJson =
            Objects.requireNonNull<Tab.BlankTab>(Tab.getTabFrom(tabsFromArray[0] as JsonObject) as Tab.BlankTab?)
        Assert.assertEquals(blankTab.tabId.toLong(), blankTabFromReturnedJson.tabId.toLong())

        val subscriptionsTabFromReturnedJson =
            Objects.requireNonNull<Tab.SubscriptionsTab>(Tab.getTabFrom(tabsFromArray[1] as JsonObject) as Tab.SubscriptionsTab?)
        Assert.assertEquals(subscriptionsTab.tabId.toLong(), subscriptionsTabFromReturnedJson.tabId.toLong())

        val channelTabFromReturnedJson =
            Objects.requireNonNull<Tab.ChannelTab>(Tab.getTabFrom(tabsFromArray[2] as JsonObject) as Tab.ChannelTab?)
        Assert.assertEquals(channelTab.tabId.toLong(), channelTabFromReturnedJson.tabId.toLong())
        Assert.assertEquals(channelTab.channelServiceId.toLong(), channelTabFromReturnedJson.channelServiceId.toLong())
        Assert.assertEquals(channelTab.channelUrl, channelTabFromReturnedJson.channelUrl)
        Assert.assertEquals(channelTab.channelName, channelTabFromReturnedJson.channelName)

        val kioskTabFromReturnedJson =
            Objects.requireNonNull<Tab.KioskTab>(Tab.getTabFrom(tabsFromArray[3] as JsonObject) as Tab.KioskTab?)
        Assert.assertEquals(kioskTab.tabId.toLong(), kioskTabFromReturnedJson.tabId.toLong())
        Assert.assertEquals(kioskTab.kioskServiceId.toLong(), kioskTabFromReturnedJson.kioskServiceId.toLong())
        Assert.assertEquals(kioskTab.kioskId, kioskTabFromReturnedJson.kioskId)
    }

    companion object {
        private const val JSON_TABS_ARRAY_KEY = "tabs"
        private const val JSON_TAB_ID_KEY = "tab_id"
    }
}