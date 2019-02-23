package com.dew.aihua.settings.preference_fragment

import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceFragmentCompat

/**
 *  Created by Edward on 2/23/2019.
 */

abstract class BasePreferenceFragment : PreferenceFragmentCompat() {
    protected lateinit var defaultPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        defaultPreferences = PreferenceManager.getDefaultSharedPreferences(activity)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setDivider(null)
        updateTitle()
    }

    override fun onResume() {
        super.onResume()
        updateTitle()
    }

    private fun updateTitle() {
        if (activity is AppCompatActivity) {
            val actionBar = (activity as AppCompatActivity).supportActionBar
            actionBar?.title = preferenceScreen.title
        }
    }
}