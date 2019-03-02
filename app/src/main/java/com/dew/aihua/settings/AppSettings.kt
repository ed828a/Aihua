package com.dew.aihua.settings

import android.content.Context
import android.content.SharedPreferences
import android.os.Environment
import android.preference.PreferenceManager
import com.dew.aihua.R
import java.io.File

/**
 *  Created by Edward on 3/2/2019.
 */
object AppSettings {

    fun initSettings(context: Context) {
        PreferenceManager.setDefaultValues(context, R.xml.appearance_settings, true)
        PreferenceManager.setDefaultValues(context, R.xml.content_settings, true)
        PreferenceManager.setDefaultValues(context, R.xml.download_settings, true)
        PreferenceManager.setDefaultValues(context, R.xml.history_settings, true)
        PreferenceManager.setDefaultValues(context, R.xml.main_settings, true)
        PreferenceManager.setDefaultValues(context, R.xml.video_audio_settings, true)
        PreferenceManager.setDefaultValues(context, R.xml.debug_settings, true)

        getVideoDownloadFolder(context)
        getAudioDownloadFolder(context)
    }

    private fun getVideoDownloadFolder(context: Context): File = getDir(context, R.string.download_path_key, Environment.DIRECTORY_MOVIES)

    fun getVideoDownloadPath(context: Context): String {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val key = context.getString(R.string.download_path_key)
        return prefs.getString(key, Environment.DIRECTORY_MOVIES) ?: ""
    }

    private fun getAudioDownloadFolder(context: Context): File = getDir(context, R.string.download_path_audio_key, Environment.DIRECTORY_MUSIC)

    fun getAudioDownloadPath(context: Context): String {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val key = context.getString(R.string.download_path_audio_key)
        return prefs.getString(key, Environment.DIRECTORY_MUSIC) ?: ""
    }

    private fun getDir(context: Context, keyID: Int, defaultDirectoryName: String): File {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val key = context.getString(keyID)
        val downloadPath = prefs.getString(key, null)
        if (downloadPath != null && !downloadPath.isEmpty()) return File(downloadPath.trim { it <= ' ' })

        val dir = getDir(defaultDirectoryName)
        prefs.edit()
            .putString(key, getNewPipeChildFolderPathForDir(dir))
            .apply()

        return dir
    }

    private fun getDir(defaultDirectoryName: String): File = File(Environment.getExternalStorageDirectory(), defaultDirectoryName)


    fun resetDownloadFolders(context: Context) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        resetDownloadFolder(prefs, context.getString(R.string.download_path_audio_key), Environment.DIRECTORY_MUSIC)
        resetDownloadFolder(prefs, context.getString(R.string.download_path_key), Environment.DIRECTORY_MOVIES)
    }

    private fun resetDownloadFolder(prefs: SharedPreferences, key: String, defaultDirectoryName: String) =
        prefs.edit()
            .putString(key, getNewPipeChildFolderPathForDir(getDir(defaultDirectoryName)))
            .apply()


    private fun getNewPipeChildFolderPathForDir(dir: File): String = File(dir, "DewTube").absolutePath

}
