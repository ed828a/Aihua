package com.dew.aihua.settings.preference_fragment

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.widget.Toast
import androidx.preference.Preference
import com.dew.aihua.R
import com.dew.aihua.report.ErrorActivity
import com.dew.aihua.report.ErrorInfo
import com.dew.aihua.report.UserAction
import com.dew.aihua.repository.database.AppDatabase.Companion.DATABASE_NAME
import com.dew.aihua.util.FilePickerActivityHelper
import com.dew.aihua.util.ZipHelper
import com.nononsenseapps.filepicker.FilePickerActivity
import com.nononsenseapps.filepicker.Utils
import com.nostra13.universalimageloader.core.ImageLoader
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 *  Created by Edward on 2/23/2019.
 */

class ContentSettingsFragment : BasePreferenceFragment() {
    private val TAG = this::class.java.simpleName + "@" + Integer.toHexString(hashCode())

    private lateinit var homeDir: String
    private lateinit var databasesDir: File
    private lateinit var newpipeDb: File
    private lateinit var newpipeDbJournal: File
    private lateinit var newpipeDbShm: File
    private lateinit var newpipeDbWal: File
    private lateinit var newpipeSettings: File

    private lateinit var thumbnailLoadToggleKey: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        thumbnailLoadToggleKey = getString(R.string.download_thumbnail_key)
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        if (preference.key == thumbnailLoadToggleKey) {
            val imageLoader = ImageLoader.getInstance()
            with(imageLoader) {
                stop()
                clearDiskCache()
                clearMemoryCache()
                resume()
            }
            Toast.makeText(preference.context, R.string.thumbnail_cache_wipe_complete_notice, Toast.LENGTH_SHORT).show()
        }

        return super.onPreferenceTreeClick(preference)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {

        homeDir = activity!!.applicationInfo.dataDir
        Log.d(TAG, "homeDir = $homeDir")

        databasesDir = File("$homeDir/databases")
        newpipeDb = File("$homeDir/databases/$DATABASE_NAME")
        newpipeDbJournal = File("$homeDir/databases/$DB_JOURNAL")
        newpipeDbShm = File("$homeDir/databases/$DB_SHM")
        newpipeDbWal = File("$homeDir/databases/$DB_WAL")

        newpipeSettings = File("$homeDir/databases/$SETTINGS_FILE_NAME")
        newpipeSettings.delete()

        addPreferencesFromResource(R.xml.content_settings)

        val importDataPreference = findPreference(getString(R.string.import_data))
        importDataPreference.setOnPreferenceClickListener { p: Preference ->
            val intent = Intent(activity, FilePickerActivityHelper::class.java)
                .putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false)
                .putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, false)
                .putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_FILE)
            startActivityForResult(intent, REQUEST_IMPORT_PATH)
            true
        }

        val exportDataPreference = findPreference(getString(R.string.export_data))
        exportDataPreference.setOnPreferenceClickListener { p: Preference ->
            val i = Intent(activity, FilePickerActivityHelper::class.java)
                .putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false)
                .putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, true)
                .putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_DIR)
            startActivityForResult(i, REQUEST_EXPORT_PATH)
            true
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        Log.d(TAG, "onActivityResult() called with: requestCode = [$requestCode], resultCode = [$resultCode], data = [$data]")


        if ((requestCode == REQUEST_IMPORT_PATH || requestCode == REQUEST_EXPORT_PATH)
            && resultCode == Activity.RESULT_OK && data?.data != null) {
            val path = Utils.getFileForUri(data.data!!).absolutePath
            if (requestCode == REQUEST_EXPORT_PATH) {
                val sdf = SimpleDateFormat("ddMMyyyy_HHmmss", Locale.US)
                exportDatabase("$path/ExportData-${sdf.format(Date())}.zip")
            } else {
                AlertDialog.Builder(activity)
                    .setMessage(R.string.override_current_data)
                    .setPositiveButton(android.R.string.ok) { d: DialogInterface, id: Int -> importDatabase(path) }
                    .setNegativeButton(android.R.string.cancel) { d: DialogInterface, id: Int -> d.cancel() }
                    .create()
                    .show()
            }
        }
    }

    private fun exportDatabase(path: String) {
        try {
            val outZip = ZipOutputStream(BufferedOutputStream(FileOutputStream(path)))
            ZipHelper.addFileToZip(outZip, newpipeDb.path, DATABASE_NAME)

            saveSharedPreferencesToFile(newpipeSettings)
            ZipHelper.addFileToZip(outZip, newpipeSettings.path, SETTINGS_FILE_NAME)

            outZip.close()

            Toast.makeText(context, R.string.export_complete_toast, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            onError(e)
        }

    }

    private fun saveSharedPreferencesToFile(dst: File?) {
        var output: ObjectOutputStream? = null
        try {
            output = ObjectOutputStream(FileOutputStream(dst))
            val pref = PreferenceManager.getDefaultSharedPreferences(context)
            output.writeObject(pref.all)

        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            try {
                output?.flush()
                output?.close()
            } catch (ex: IOException) {
                ex.printStackTrace()
            }

        }
    }

    private fun importDatabase(filePath: String) {
        // check if file is supported
        var zipFile: ZipFile? = null
        try {
            zipFile = ZipFile(filePath)
        } catch (ioe: IOException) {
            Toast.makeText(context, R.string.no_valid_zip_file, Toast.LENGTH_SHORT).show()
            return
        } finally {
            try {
                zipFile?.close()
            } catch (ignored: Exception) {
                Log.d(TAG, "ignored exception: ${ignored.message}")
            }
        }

        try {
            if (!databasesDir.exists() && !databasesDir.mkdir()) {
                throw Exception("Could not create databases dir")
            }

            val isDbFileExtracted = ZipHelper.extractFileFromZip(filePath, newpipeDb.path, DATABASE_NAME)
            if (isDbFileExtracted) {
                newpipeDbJournal.delete()
                newpipeDbWal.delete()
                newpipeDbShm.delete()

            } else {
                Toast.makeText(context, R.string.could_not_import_all_files, Toast.LENGTH_LONG).show()
            }

            //If settings file exist, ask if it should be imported.
            if (ZipHelper.extractFileFromZip(filePath, newpipeSettings.path, SETTINGS_FILE_NAME)) {
                AlertDialog.Builder(context)
                    .setTitle(R.string.import_settings)
                    .setNegativeButton(android.R.string.no) { dialog, which ->
                        dialog.dismiss()
                        // restart app to properly load db
                        System.exit(0)
                    }
                    .setPositiveButton(android.R.string.yes) { dialog, which ->
                        dialog.dismiss()
                        loadSharedPreferences(newpipeSettings)
                        // restart app to properly load db
                        System.exit(0)
                    }
                    .show()
            } else {
                // restart app to properly load db
                System.exit(0)
            }

        } catch (e: Exception) {
            onError(e)
        }

    }

    private fun loadSharedPreferences(src: File) {
        var input: ObjectInputStream? = null
        try {
            input = ObjectInputStream(FileInputStream(src))
            val prefEdit = PreferenceManager.getDefaultSharedPreferences(context).edit()
            prefEdit.clear()
            val entries = input.readObject() as Map<String, *>
            for ((key, value) in entries) {
                when (value) {
                    is Boolean -> prefEdit.putBoolean(key, value)
                    is Float -> prefEdit.putFloat(key, value)
                    is Int -> prefEdit.putInt(key, value)
                    is Long -> prefEdit.putLong(key, value)
                    is String -> prefEdit.putString(key, value)
                }
            }
            prefEdit.apply()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: ClassNotFoundException) {
            e.printStackTrace()
        } finally {
            try {
                input?.close()
            } catch (ex: IOException) {
                ex.printStackTrace()
            }

        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Error
    ///////////////////////////////////////////////////////////////////////////
    private fun onError(exception: Throwable): Boolean {
        val activity = activity
        activity?.let { fragmentActivity ->
            ErrorActivity.reportError(activity,
                exception,
                fragmentActivity.javaClass,
                null,
                ErrorInfo.make(UserAction.UI_ERROR, "none", "", R.string.app_ui_crash))
        }

        return true
    }

    companion object {
        //        private val TAG = ContentSettingsFragment::class.simpleName
        private const val REQUEST_IMPORT_PATH = 8945
        private const val REQUEST_EXPORT_PATH = 30945
        private const val SETTINGS_FILE_NAME = "newpipe.settings"
        private const val DB_JOURNAL = "newpipe.db-journal"
        private const val DB_SHM = "newpipe.db-shm"
        private const val DB_WAL = "newpipe.db-wal"
    }
}
