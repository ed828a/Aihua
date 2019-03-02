package com.dew.aihua.settings.preference_fragment

import android.os.Bundle
import com.dew.aihua.R

/**
 *  Created by Edward on 3/2/2019.
 */
class DebugSettingsFragment : BasePreferenceFragment() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.debug_settings)
    }
}
