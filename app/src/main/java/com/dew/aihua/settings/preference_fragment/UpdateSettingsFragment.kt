package com.dew.aihua.settings.preference_fragment

import android.os.Bundle
import androidx.preference.Preference
import com.dew.aihua.R


/**
 *  Created by Edward on 3/3/2019.
 */
class UpdateSettingsFragment : BasePreferenceFragment() {

    private val updatePreferenceChange =  Preference.OnPreferenceChangeListener{ _, newValue ->

        defaultPreferences.edit().putBoolean(getString(R.string.update_app_key),
            newValue as Boolean).apply()

        true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val updateToggleKey = getString(R.string.update_app_key)
        findPreference(updateToggleKey).onPreferenceChangeListener = updatePreferenceChange
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.update_settings)
    }
}
