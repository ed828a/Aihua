package com.dew.aihua.settings.preference_fragment

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.preference.Preference
import com.dew.aihua.R
import com.dew.aihua.data.local.manoeuvre.HistoryRecordManager
import com.dew.aihua.data.local.cache.InfoCache
import com.dew.aihua.report.ErrorActivity
import com.dew.aihua.report.ErrorInfo
import com.dew.aihua.report.UserAction
import com.dew.aihua.settings.SettingsActivity
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable

/**
 *  Created by Edward on 2/23/2019.
 */

class HistorySettingsFragment : BasePreferenceFragment() {
    private lateinit var cacheWipeKey: String
    private lateinit var viewsHistoryClearKey: String
    private lateinit var searchHistoryClearKey: String
    private var recordManager: HistoryRecordManager? = null
    private var disposables: CompositeDisposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        cacheWipeKey = getString(R.string.metadata_cache_wipe_key)
        viewsHistoryClearKey = getString(R.string.clear_views_history_key)
        searchHistoryClearKey = getString(R.string.clear_search_history_key)
        activity?.let {
            recordManager = HistoryRecordManager(it)
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.history_settings)
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {

        when(preference.key){
            cacheWipeKey -> {
                InfoCache.clearCache()
                Toast.makeText(preference.context, R.string.metadata_cache_wipe_complete_notice,
                    Toast.LENGTH_SHORT).show()
            }

            viewsHistoryClearKey -> {
                activity?.let { fragmentActivity ->
                    AlertDialog.Builder(fragmentActivity)
                        .setTitle(R.string.delete_view_history_alert)
                        .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
                        .setPositiveButton(R.string.delete) { _, _ ->
                            recordManager?.let {historyRecordManager ->
                                val onDelete = historyRecordManager.deleteWholeStreamHistory()
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(
                                        { howManyDeleted ->
                                            Toast.makeText(activity,
                                                "${resources.getString(R.string.view_history_deleted)} deleted: $howManyDeleted",
                                                Toast.LENGTH_SHORT).show()
                                        },
                                        { throwable ->
                                            ErrorActivity.reportError(context!!,
                                                throwable,
                                                SettingsActivity::class.java,
                                                null,
                                                ErrorInfo.make(
                                                    UserAction.DELETE_FROM_HISTORY,
                                                    "none",
                                                    "Delete view history",
                                                    R.string.general_error))
                                        })

                                val onClearOrphans = historyRecordManager.removeOrphanedRecords()
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(
                                        { howManyDeleted ->
                                            Toast.makeText(activity, "Deleted $howManyDeleted Orphan records", Toast.LENGTH_SHORT).show()
                                        },
                                        { throwable ->
                                            ErrorActivity.reportError(context!!,
                                                throwable,
                                                SettingsActivity::class.java,
                                                null,
                                                ErrorInfo.make(
                                                    UserAction.DELETE_FROM_HISTORY,
                                                    "none",
                                                    "Delete search history",
                                                    R.string.general_error))
                                        })

                                disposables.addAll(onClearOrphans, onDelete)
                            }
                        }
                        .create()
                        .show()
                }
            }

            searchHistoryClearKey -> {
                activity?.let { fragmentActivity ->
                    AlertDialog.Builder(fragmentActivity)
                        .setTitle(R.string.delete_search_history_alert)
                        .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
                        .setPositiveButton(R.string.delete) { _, _ ->
                            recordManager?.let {historyRecordManager ->
                                val onDelete = historyRecordManager.deleteWholeSearchHistory()
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(
                                        { howManyDeleted ->
                                            Toast.makeText(activity,
                                                "${getString(R.string.search_history_deleted)} : howManyDeleted = $howManyDeleted",
                                                Toast.LENGTH_SHORT).show()
                                        },
                                        { throwable ->
                                            ErrorActivity.reportError(context!!,
                                                throwable,
                                                SettingsActivity::class.java,
                                                null,
                                                ErrorInfo.make(
                                                    UserAction.DELETE_FROM_HISTORY,
                                                    "none",
                                                    "Delete search history",
                                                    R.string.general_error))
                                        })
                                disposables.add(onDelete)
                            }
                        }
                        .create()
                        .show()
                }
            }
        }

        return super.onPreferenceTreeClick(preference)
    }

    override fun onDestroy() {
        disposables.dispose()
        super.onDestroy()
    }
}
