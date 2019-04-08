package com.dew.aihua.util

import android.util.Log
import com.dew.aihua.ui.model.DrawerMenuItem
import com.grack.nanojson.*
import java.util.*

/**
 *  Created by Edward on 3/2/2019.
 ***
 * Class to get a JSON representation of a list of tabs, and the other way around.
 *
 * JsonParser/JsonWriter: refer to https://github.com/mmastrac/nanojson
 */
object DrawerJsonHelper {
    private const val TAG = "DrawerJsonHelper"
    private const val JSON_DRAWER_EXTRA_MENU_ARRAY_KEY = "tabs"


    /**
     * Try to reads the passed JSON and returns the list of tabs if no error were encountered.
     *
     *
     * If the JSON is null or empty, or the list of tabs that it represents is empty, the
     * [fallback list][.FALLBACK_INITIAL_TABS_LIST] will be returned.
     *
     *
     * Tabs with invalid ids (i.e. not in the TabType enum) will be ignored.
     *
     * @param tabsJson a JSON string got getTabFrom [.getJsonToSave].
     * @return a list of [tabs][Tab].
     * @throws InvalidJsonException if the JSON string is not valid
     */
//    @Throws(InvalidJsonException::class)
//    fun getTabsFromJson(tabsJson: String?): List<DrawerMenuItem> {
//        if (tabsJson == null || tabsJson.isEmpty()) {
//            return emptyList()
//        }
//
//        val returnTabs = ArrayList<DrawerMenuItem>()
//
////        val outerJsonObject: JsonObject
//        try {
//            val outerJsonObject = JsonParser.`object`().from(tabsJson)
//                ?: throw InvalidJsonException("JSON doesn't contain Json Object")
//            val tabsArray = outerJsonObject.getArray(JSON_DRAWER_EXTRA_MENU_ARRAY_KEY)
//                ?: throw InvalidJsonException("JSON doesn't contain \"$JSON_DRAWER_EXTRA_MENU_ARRAY_KEY\" array")
//            Log.d(TAG, "tabsArray: $tabsArray")
//
//            for (obj in tabsArray) {
//                if (obj is JsonObject) {
//                    val tab = .getTabFrom(obj)
//                    if (tab != null) {
//                        returnTabs.add(tab)
//                    }
//                }
//            }
//        } catch (e: JsonParserException) {
//            throw InvalidJsonException(e)
//        }
//
//        return if (returnTabs.isEmpty()) {
//
//        } else returnTabs
//
//    }

    /**
     * Get a JSON representation getTabFrom a list of tabs.
     * convert a List<TAB> to Json
     * @param tabList a list of [tabs][Tab].
     * @return a JSON string representing the list of tabs
     */
//    fun getJsonToSave(tabList: List<DrawerMenuItem>?): String {
//        val jsonWriter = JsonWriter.string()
//        jsonWriter.`object`().array(JSON_DRAWER_EXTRA_MENU_ARRAY_KEY)
//
//        if (tabList != null) {
//            for (tab in tabList) {
//                tab.writeJsonOn(jsonWriter)
//            }
//            jsonWriter.end()   // close array
//        }
//
//        jsonWriter.end() // close object
//        return jsonWriter.done()
//    }

//    fun writeJsonOn(jsonSink: JsonSink<*>) {
//        jsonSink.`object`()
//
//        jsonSink.value(Tab.JSON_TAB_ID_KEY, tabId)
//        writeDataToJson(jsonSink)
//
//        jsonSink.end()
//    }
}