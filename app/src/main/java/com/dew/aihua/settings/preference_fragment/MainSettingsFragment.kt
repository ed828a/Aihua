package com.dew.aihua.settings.preference_fragment

import android.os.Bundle
import com.dew.aihua.BuildConfig.DEBUG
import com.dew.aihua.R

/**
 *  Created by Edward on 2/23/2019.
 */


class MainSettingsFragment : BasePreferenceFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.main_settings)

        if (!DEBUG) {
            val debug = findPreference(getString(R.string.debug_pref_screen_key))
            preferenceScreen.removePreference(debug)
        }
    }

}
