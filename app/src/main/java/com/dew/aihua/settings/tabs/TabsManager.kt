package com.dew.aihua.settings.tabs

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.widget.Toast
import com.dew.aihua.R

/**
 *  Created by Edward on 3/2/2019.
 */

class TabsManager private constructor(private val context: Context) {
    private val sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val savedTabsKey: String = context.getString(R.string.saved_tabs_key)

    fun getTabs(): List<Tab> {
        val savedJson = sharedPreferences.getString(savedTabsKey, null)
        return try {
            TabsJsonHelper.getTabsFromJson(savedJson)
        } catch (e: TabsJsonHelper.InvalidJsonException) {
            Toast.makeText(context, R.string.saved_tabs_invalid_json, Toast.LENGTH_SHORT).show()
            getDefaultTabs()
        }

    }

    private fun getDefaultTabs(): List<Tab> = TabsJsonHelper.FALLBACK_INITIAL_TABS_LIST

    private var savedTabsChangeListener: SavedTabsChangeListener? = null
    private var preferenceChangeListener: SharedPreferences.OnSharedPreferenceChangeListener? = null

    fun saveTabs(tabList: List<Tab>) {
        val jsonToSave = TabsJsonHelper.getJsonToSave(tabList)  // convert a List<Tab> to Json
        sharedPreferences.edit().putString(savedTabsKey, jsonToSave).apply()
    }

    /**
     * the working metaphor is after saveTabs removed getTabFrom SharedPreference, next time to retrieve saveTabs will get null,
     *  and to retrieve tabs with null will get the default tabs
     */
    fun resetTabs() {
        sharedPreferences.edit().remove(savedTabsKey).apply()
    }

    ///////////////////////////////////////////////////////////////////////////
    // Listener
    ///////////////////////////////////////////////////////////////////////////

    interface SavedTabsChangeListener {
        fun onTabsChanged()
    }

    fun setSavedTabsChangeListener(listener: SavedTabsChangeListener) {
        if (preferenceChangeListener != null) {
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener)
        }
        savedTabsChangeListener = listener
        preferenceChangeListener = getPreferenceChangeListener()
        sharedPreferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
    }

    fun unsetSavedTabsListener() {
        if (preferenceChangeListener != null) {
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener)
        }
        preferenceChangeListener = null
        savedTabsChangeListener = null
    }

    private fun getPreferenceChangeListener(): SharedPreferences.OnSharedPreferenceChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == savedTabsKey && savedTabsChangeListener != null) {
                savedTabsChangeListener!!.onTabsChanged()
            }
        }


    companion object {

        fun getTabsManager(context: Context): TabsManager {
            return TabsManager(context)
        }
    }

}
