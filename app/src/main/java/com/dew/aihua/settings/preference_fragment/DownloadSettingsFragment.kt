package com.dew.aihua.settings.preference_fragment

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.preference.Preference
import com.dew.aihua.R
import com.dew.aihua.util.FilePickerActivityHelper
import com.nononsenseapps.filepicker.Utils

/**
 *  Created by Edward on 3/2/2019.
 */
class DownloadSettingsFragment : BasePreferenceFragment() {

    private lateinit var downloadPathPreference: String
    private lateinit var downloadPathAudioPreference: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initKeys()
        updatePreferencesSummary()
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.download_settings)
    }

    private fun initKeys() {
        downloadPathPreference = getString(R.string.download_path_key)
        downloadPathAudioPreference = getString(R.string.download_path_audio_key)
    }

    private fun updatePreferencesSummary() {
        findPreference(downloadPathPreference).summary = defaultPreferences.getString(downloadPathPreference, getString(R.string.download_path_summary))
        findPreference(downloadPathAudioPreference).summary = defaultPreferences.getString(downloadPathAudioPreference, getString(R.string.download_path_audio_summary))
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {

        Log.d(TAG, "onPreferenceTreeClick() called with: preference = [$preference]")

        if (preference.key == downloadPathPreference || preference.key == downloadPathAudioPreference) {
            val intent = Intent(activity, FilePickerActivityHelper::class.java)
                .putExtra(com.nononsenseapps.filepicker.FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false)
                .putExtra(com.nononsenseapps.filepicker.FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, true)
                .putExtra(com.nononsenseapps.filepicker.FilePickerActivity.EXTRA_MODE, com.nononsenseapps.filepicker.FilePickerActivity.MODE_DIR)

            if (preference.key == downloadPathPreference) {
                startActivityForResult(intent, REQUEST_DOWNLOAD_PATH)
            } else if (preference.key == downloadPathAudioPreference) {
                startActivityForResult(intent, REQUEST_DOWNLOAD_AUDIO_PATH)
            }
        }

        return super.onPreferenceTreeClick(preference)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        Log.d(TAG, "onActivityResult() called with: requestCode = [$requestCode], resultCode = [$resultCode], data = [$data]")

        if ((requestCode == REQUEST_DOWNLOAD_PATH || requestCode == REQUEST_DOWNLOAD_AUDIO_PATH)
            && resultCode == Activity.RESULT_OK && data!!.data != null) {
            val key = getString(if (requestCode == REQUEST_DOWNLOAD_PATH) R.string.download_path_key else R.string.download_path_audio_key)
            val path = Utils.getFileForUri(data.data!!).absolutePath

            defaultPreferences.edit().putString(key, path).apply()
            updatePreferencesSummary()
        }
    }

    companion object {
        private val TAG = DownloadSettingsFragment::class.simpleName
        private const val REQUEST_DOWNLOAD_PATH = 0x1235
        private const val REQUEST_DOWNLOAD_AUDIO_PATH = 0x1236
    }
}
