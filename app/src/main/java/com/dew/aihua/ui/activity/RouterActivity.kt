package com.dew.aihua.ui.activity

import android.annotation.SuppressLint
import android.app.IntentService
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.preference.PreferenceManager
import android.text.TextUtils
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import com.dew.aihua.R
import com.dew.aihua.player.helper.PlayerHelper
import com.dew.aihua.player.playqueque.queque.ChannelPlayQueue
import com.dew.aihua.player.playqueque.queque.PlayQueue
import com.dew.aihua.player.playqueque.queque.PlaylistPlayQueue
import com.dew.aihua.player.playqueque.queque.SinglePlayQueue
import com.dew.aihua.report.UserAction
import com.dew.aihua.repository.remote.download.ui.dialog.DownloadDialog
import com.dew.aihua.repository.remote.helper.ExtractorHelper
import com.dew.aihua.util.ListHelper
import com.dew.aihua.util.NavigationHelper
import com.dew.aihua.util.PermissionHelper
import com.dew.aihua.util.ThemeHelper
import com.dew.aihua.util.ThemeHelper.resolveResourceIdFromAttr
import icepick.Icepick
import icepick.State
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import org.schabi.newpipe.extractor.Info
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.channel.ChannelInfo
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.playlist.PlaylistInfo
import org.schabi.newpipe.extractor.stream.StreamInfo
import java.io.Serializable
import java.util.*

/**
 *  Created by Edward on 2/23/2019.
 ***
 * Get the url getTabFrom the intent and open it in the chosen preferred simpleExoPlayer
 */

class RouterActivity : AppCompatActivity() {

    @State
    @JvmField
    protected var currentServiceId = -1

    @State
    @JvmField
    protected var currentLinkType: StreamingService.LinkType? = null

    @State
    @JvmField
    protected var selectedRadioPosition = -1

    private var currentService: StreamingService? = null
    private var selectedPreviously = -1

    private var currentUrl: String? = null
    private var disposables = CompositeDisposable()

    private var selectionIsDownload = false

    // ContextThemeWrapper: A context wrapper that allows you to modify or replace the theme of the wrapped context
    private val themeWrapperContext: Context
        get() = ContextThemeWrapper(this,
            if (ThemeHelper.isLightThemeSelected(this)) R.style.LightTheme else R.style.DarkTheme)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Icepick.restoreInstanceState(this, savedInstanceState)

        if (TextUtils.isEmpty(currentUrl)) {
            currentUrl = getUrl(intent)

            if (TextUtils.isEmpty(currentUrl)) {
                Toast.makeText(this, R.string.invalid_url_toast, Toast.LENGTH_LONG).show()
                finish()
            }
        }

        setTheme(if (ThemeHelper.isLightThemeSelected(this))
            R.style.RouterActivityThemeLight
        else
            R.style.RouterActivityThemeDark)

        Log.d(TAG, "RouterActivity: onCreate() called.")
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Icepick.saveInstanceState(this, outState)
    }

    override fun onStart() {
        super.onStart()

        handleUrl(currentUrl!!)
    }

    override fun onDestroy() {
        super.onDestroy()

        disposables.dispose()
    }

    private fun handleUrl(url: String) {
        val d = Observable.fromCallable {
            if (currentServiceId == -1) {
                currentService = NewPipe.getServiceByUrl(url)
                currentServiceId = currentService!!.serviceId
                currentLinkType = currentService!!.getLinkTypeByUrl(url)
                currentUrl = url
            } else {
                currentService = NewPipe.getService(currentServiceId)
            }

            StreamingService.LinkType.NONE != currentLinkType
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { result ->
                    if (result!!) {
                        onSuccess()
                    } else {
                        onError()
                    }
                },
                { this.handleError(it) })

        disposables.add(d)
    }

    private fun handleError(error: Throwable) {
        error.printStackTrace()

        if (error is ExtractionException) {
            Toast.makeText(this, R.string.url_not_supported_toast, Toast.LENGTH_LONG).show()
        } else {
            ExtractorHelper.handleGeneralException(this, -1, null!!, error, UserAction.SOMETHING_ELSE, null)
        }

        finish()
    }

    private fun onError() {
        Toast.makeText(this, R.string.url_not_supported_toast, Toast.LENGTH_LONG).show()
        finish()
    }

    private fun onSuccess() {
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        val selectedChoiceKey = preferences.getString(getString(R.string.preferred_open_action_key), getString(R.string.preferred_open_action_default))

        val showInfoKey = getString(R.string.show_info_key)
        val videoPlayerKey = getString(R.string.video_player_key)
        val backgroundPlayerKey = getString(R.string.background_player_key)
        val popupPlayerKey = getString(R.string.popup_player_key)
        val downloadKey = getString(R.string.download_key)
        val alwaysAskKey = getString(R.string.always_ask_open_action_key)

        when (selectedChoiceKey) {
            alwaysAskKey -> {
                val choices = getChoicesForService(currentService!!, currentLinkType!!)

                when {
                    choices.size == 1 -> handleChoice(choices[0].key)
                    choices.isEmpty() -> handleChoice(showInfoKey)
                    else -> showDialog(choices)
                }

            }

            showInfoKey -> handleChoice(showInfoKey)
            downloadKey -> handleChoice(downloadKey)

            else -> {
                val isExtVideoEnabled = preferences.getBoolean(getString(R.string.use_external_video_player_key), false)
                val isExtAudioEnabled = preferences.getBoolean(getString(R.string.use_external_audio_player_key), false)
                val isVideoPlayerSelected = selectedChoiceKey == videoPlayerKey || selectedChoiceKey == popupPlayerKey
                val isAudioPlayerSelected = selectedChoiceKey == backgroundPlayerKey

                if (StreamingService.LinkType.STREAM != currentLinkType && (isExtAudioEnabled && isAudioPlayerSelected || isExtVideoEnabled && isVideoPlayerSelected)) {

                    Toast.makeText(this, R.string.external_player_unsupported_link_type, Toast.LENGTH_LONG).show()
                    handleChoice(showInfoKey)
                    return
                }

                val capabilities = currentService!!.serviceInfo.mediaCapabilities

                val serviceSupportsChoice =
                    when {
                        isVideoPlayerSelected -> capabilities.contains(StreamingService.ServiceInfo.MediaCapability.VIDEO)
                        selectedChoiceKey == backgroundPlayerKey -> capabilities.contains(StreamingService.ServiceInfo.MediaCapability.AUDIO)
                        else -> false
                    }

                if (serviceSupportsChoice) {
                    handleChoice(selectedChoiceKey)
                } else {
                    handleChoice(showInfoKey)
                }
            }
        }
    }

    private fun showDialog(choices: List<AdapterChoiceItem>) {
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        val themeWrapperContext = themeWrapperContext

        val inflater = LayoutInflater.from(themeWrapperContext)  // the view this inflater inflates comes with the theme.
        val rootLayout = inflater.inflate(R.layout.preferred_player_dialog_view, null, false) as LinearLayout
        val radioGroup = rootLayout.findViewById<RadioGroup>(android.R.id.list)

        val dialogButtonsClickListener = DialogInterface.OnClickListener { dialog, which ->
            val indexOfChild = radioGroup.indexOfChild(
                radioGroup.findViewById(radioGroup.checkedRadioButtonId))
            val choice = choices[indexOfChild]

            handleChoice(choice.key)

            if (which == DialogInterface.BUTTON_POSITIVE) {
                preferences.edit().putString(getString(R.string.preferred_open_action_key), choice.key).apply()
            }
        }

        val alertDialog = AlertDialog.Builder(themeWrapperContext)
            .setTitle(R.string.preferred_open_action_share_menu_title)
            .setView(radioGroup)
            .setCancelable(true)
            .setNegativeButton(R.string.just_once, dialogButtonsClickListener)
            .setPositiveButton(R.string.always, dialogButtonsClickListener)
            .setOnDismissListener { dialog -> if (!selectionIsDownload) finish() }
            .create()


        alertDialog.setOnShowListener { dialog -> setDialogButtonsState(alertDialog, radioGroup.checkedRadioButtonId != -1) }

        radioGroup.setOnCheckedChangeListener { group, checkedId -> setDialogButtonsState(alertDialog, true) }

        val radioButtonsClickListener = View.OnClickListener { view ->
            val indexOfChild = radioGroup.indexOfChild(view)
            if (indexOfChild == -1) return@OnClickListener

            selectedPreviously = selectedRadioPosition
            selectedRadioPosition = indexOfChild

            if (selectedPreviously == selectedRadioPosition) {
                handleChoice(choices[selectedRadioPosition].key)
            }
        }

        var id = 12345
        for (item in choices) {
            val radioButton = inflater.inflate(R.layout.list_radio_icon_item, null) as RadioButton
            radioButton.text = item.description
            radioButton.setCompoundDrawablesWithIntrinsicBounds(item.icon, 0, 0, 0)
            radioButton.isChecked = false
            radioButton.id = id++
            radioButton.layoutParams = RadioGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            radioButton.setOnClickListener(radioButtonsClickListener)
            radioGroup.addView(radioButton)
        }

        if (selectedRadioPosition == -1) {
            val lastSelectedPlayer = preferences.getString(getString(R.string.preferred_open_action_last_selected_key), null)
            if (!TextUtils.isEmpty(lastSelectedPlayer)) {
                for (i in choices.indices) {
                    val c = choices[i]
                    if (lastSelectedPlayer == c.key) {
                        selectedRadioPosition = i
                        break
                    }
                }
            }
        }

        selectedRadioPosition = Math.min(Math.max(-1, selectedRadioPosition), choices.size - 1)
        if (selectedRadioPosition != -1) {
            (radioGroup.getChildAt(selectedRadioPosition) as RadioButton).isChecked = true
        }
        selectedPreviously = selectedRadioPosition

        alertDialog.show()
    }

    private fun getChoicesForService(service: StreamingService, linkType: StreamingService.LinkType): List<AdapterChoiceItem> {
        val context = themeWrapperContext

        val returnList = ArrayList<AdapterChoiceItem>()
        val capabilities = service.serviceInfo.mediaCapabilities

        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        val isExtVideoEnabled = preferences.getBoolean(getString(R.string.use_external_video_player_key), false)
        val isExtAudioEnabled = preferences.getBoolean(getString(R.string.use_external_audio_player_key), false)

        returnList.add(AdapterChoiceItem(getString(R.string.show_info_key), getString(R.string.show_info),
            resolveResourceIdFromAttr(context, R.attr.info)))

        if (capabilities.contains(StreamingService.ServiceInfo.MediaCapability.VIDEO) && !(isExtVideoEnabled && linkType != StreamingService.LinkType.STREAM)) {
            returnList.add(AdapterChoiceItem(getString(R.string.video_player_key), getString(R.string.video_player),
                resolveResourceIdFromAttr(context, R.attr.play)))
            returnList.add(AdapterChoiceItem(getString(R.string.popup_player_key), getString(R.string.popup_player),
                resolveResourceIdFromAttr(context, R.attr.popup)))
        }

        if (capabilities.contains(StreamingService.ServiceInfo.MediaCapability.AUDIO) && !(isExtAudioEnabled && linkType != StreamingService.LinkType.STREAM)) {
            returnList.add(AdapterChoiceItem(getString(R.string.background_player_key), getString(R.string.background_player),
                resolveResourceIdFromAttr(context, R.attr.audio)))
        }

        returnList.add(AdapterChoiceItem(getString(R.string.download_key), getString(R.string.download),
            resolveResourceIdFromAttr(context, R.attr.download)))

        return returnList
    }

    private fun setDialogButtonsState(dialog: AlertDialog, state: Boolean) {
        val negativeButton = dialog.getButton(DialogInterface.BUTTON_NEGATIVE)
        val positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
        if (negativeButton == null || positiveButton == null) return

        negativeButton.isEnabled = state
        positiveButton.isEnabled = state
    }

    private fun handleChoice(selectedChoiceKey: String?) {
        val validChoicesList = Arrays.asList(*resources.getStringArray(R.array.preferred_open_action_values_list))
        if (validChoicesList.contains(selectedChoiceKey)) {
            PreferenceManager.getDefaultSharedPreferences(this).edit()
                .putString(getString(R.string.preferred_open_action_last_selected_key), selectedChoiceKey)
                .apply()
        }

        if (selectedChoiceKey == getString(R.string.popup_player_key) && !PermissionHelper.isPopupEnabled(this)) {
            PermissionHelper.showPopupEnablementToast(this)
            finish()
            return
        }

        if (selectedChoiceKey == getString(R.string.download_key)) {
            if (PermissionHelper.checkStoragePermissions(this, PermissionHelper.DOWNLOAD_DIALOG_REQUEST_CODE)) {
                selectionIsDownload = true
                openDownloadDialog()
            }
            return
        }

        // stop and bypass FetcherService if InfoScreen was selected since
        // StreamDetailFragment can fetch data itself
        if (selectedChoiceKey == getString(R.string.show_info_key)) {
            disposables.add(Observable
                .fromCallable { NavigationHelper.getIntentByLink(this, currentUrl) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ intent ->
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    startActivity(intent)

                    finish()
                }, { this.handleError(it) })
            )
            return
        }

        val intent = Intent(this, FetcherService::class.java)
        val choice = Choice(currentService!!.serviceId, currentLinkType!!, currentUrl!!, selectedChoiceKey!!)
        intent.putExtra(FetcherService.KEY_CHOICE, choice)
        startService(intent)

        finish()
    }

    @SuppressLint("CheckResult")
    private fun openDownloadDialog() {
        ExtractorHelper.getStreamInfo(currentServiceId, currentUrl!!, true)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { result: StreamInfo ->
                    val sortedVideoStreams = ListHelper.getSortedStreamVideosList(this,
                        result.videoStreams,
                        result.videoOnlyStreams,
                        false)
                    val selectedVideoStreamIndex = ListHelper.getDefaultResolutionIndex(this,
                        sortedVideoStreams)

                    val fm = supportFragmentManager
                    val downloadDialog = DownloadDialog.newInstance(result)
                    downloadDialog.setVideoStreams(sortedVideoStreams)
                    downloadDialog.setAudioStreams(result.audioStreams)
                    downloadDialog.setSelectedVideoStream(selectedVideoStreamIndex)
                    downloadDialog.show(fm, "downloadDialog")
                    fm.executePendingTransactions()
                    downloadDialog.dialog?.setOnDismissListener { dialog -> finish() }
                },
                { throwable: Throwable -> onError() })
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        for (i in grantResults) {
            if (i == PackageManager.PERMISSION_DENIED) {
                finish()
                return
            }
        }
        if (requestCode == PermissionHelper.DOWNLOAD_DIALOG_REQUEST_CODE) {
            openDownloadDialog()
        }
    }

    private class AdapterChoiceItem internal constructor(internal val key: String, internal val description: String, @field:DrawableRes internal val icon: Int)

    class Choice internal constructor(internal val serviceId: Int, internal val linkType: StreamingService.LinkType, internal val url: String, internal val playerChoice: String) :
        Serializable {

        override fun toString(): String {
            return "serviceId:$serviceId, url:$url, linkType:$linkType, playerChoice:$playerChoice"
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Service Fetcher
    ///////////////////////////////////////////////////////////////////////////

    class FetcherService : IntentService(FetcherService::class.java.simpleName) {
        private var fetcher: Disposable? = null

        override fun onCreate() {
            super.onCreate()
            startForeground(FETCH_NOTIFICATION_ID, createNotification().build())
        }

        override fun onHandleIntent(intent: Intent?) {
            if (intent == null) return

            val serializable = intent.getSerializableExtra(KEY_CHOICE) as? Choice
                ?: return
            handleChoice(serializable)
        }

        private fun handleChoice(choice: Choice) {
            var single: Single<out Info>? = null
            var userAction = UserAction.SOMETHING_ELSE

            when (choice.linkType) {
                StreamingService.LinkType.STREAM -> {
                    single = ExtractorHelper.getStreamInfo(choice.serviceId, choice.url, false)
                    userAction = UserAction.REQUESTED_STREAM
                }
                StreamingService.LinkType.CHANNEL -> {
                    single = ExtractorHelper.getChannelInfo(choice.serviceId, choice.url, false)
                    userAction = UserAction.REQUESTED_CHANNEL
                }
                StreamingService.LinkType.PLAYLIST -> {
                    single = ExtractorHelper.getPlaylistInfo(choice.serviceId, choice.url, false)
                    userAction = UserAction.REQUESTED_PLAYLIST
                }
                StreamingService.LinkType.NONE -> {
                    Log.d(TAG, "handleChoice(): LinkType error: LinkType.NONE")
                }
            }


            if (single != null) {
                val finalUserAction = userAction
                val resultHandler = getResultHandler(choice)
                fetcher = single
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ info ->
                        resultHandler.accept(info)
                        if (fetcher != null) fetcher!!.dispose()
                    }, { throwable ->
                        ExtractorHelper.handleGeneralException(this,
                            choice.serviceId, choice.url, throwable, finalUserAction, ", opened with " + choice.playerChoice)
                    })
            }
        }

        private fun getResultHandler(choice: Choice): Consumer<Info> =
            Consumer { info ->
                val videoPlayerKey = getString(R.string.video_player_key)
                val backgroundPlayerKey = getString(R.string.background_player_key)
                val popupPlayerKey = getString(R.string.popup_player_key)

                val preferences = PreferenceManager.getDefaultSharedPreferences(this)
                val isExtVideoEnabled = preferences.getBoolean(getString(R.string.use_external_video_player_key), false)
                val isExtAudioEnabled = preferences.getBoolean(getString(R.string.use_external_audio_player_key), false)
                val useOldVideoPlayer = PlayerHelper.isUsingOldPlayer(this)

                val playQueue: PlayQueue
                val playerChoice = choice.playerChoice
                when (info) {
                    is StreamInfo -> {
                        when {
                            playerChoice == backgroundPlayerKey && isExtAudioEnabled -> NavigationHelper.playOnExternalAudioPlayer(this, info as StreamInfo)
                            playerChoice == videoPlayerKey && isExtVideoEnabled -> NavigationHelper.playOnExternalVideoPlayer(this, info as StreamInfo)
                            else -> {
                                playQueue = SinglePlayQueue(info as StreamInfo)

                                when (playerChoice) {
                                    videoPlayerKey -> NavigationHelper.playOnMainPlayer(this, playQueue)
                                    backgroundPlayerKey -> NavigationHelper.enqueueOnBackgroundPlayer(this, playQueue, true)
                                    popupPlayerKey -> NavigationHelper.enqueueOnPopupPlayer(this, playQueue, true)
                                }
                            }
                        }
                    }

                    is ChannelInfo -> {
                        playQueue = ChannelPlayQueue(info)

                        when (playerChoice) {
                            videoPlayerKey -> NavigationHelper.playOnMainPlayer(this, playQueue)
                            backgroundPlayerKey -> NavigationHelper.enqueueOnBackgroundPlayer(this, playQueue, true)
                            popupPlayerKey -> NavigationHelper.enqueueOnPopupPlayer(this, playQueue, true)
                        }
                    }

                    is PlaylistInfo -> {
                        playQueue = PlaylistPlayQueue(info)

                        when (playerChoice) {
                            videoPlayerKey -> NavigationHelper.playOnMainPlayer(this, playQueue)
                            backgroundPlayerKey -> NavigationHelper.enqueueOnBackgroundPlayer(this, playQueue, true)
                            popupPlayerKey -> NavigationHelper.enqueueOnPopupPlayer(this, playQueue, true)
                        }
                    }
                }
            }


        override fun onDestroy() {
            super.onDestroy()
            stopForeground(true)
            if (fetcher != null) fetcher!!.dispose()
        }

        private fun createNotification(): NotificationCompat.Builder =
            NotificationCompat.Builder(this, getString(R.string.notification_channel_id))
                .setOngoing(true)
                .setSmallIcon(R.drawable.ic_newpipe_triangle_white)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentTitle(getString(R.string.preferred_player_fetcher_notification_title))
                .setContentText(getString(R.string.preferred_player_fetcher_notification_message))


        companion object {

            private const val FETCH_NOTIFICATION_ID = 456
            const val KEY_CHOICE = "key_choice"
        }
    }


    ///////////////////////////////////////////////////////////////////////////
    // Utils
    ///////////////////////////////////////////////////////////////////////////

    private fun getUrl(intent: Intent): String? {
        // first gather data and find service
        var videoUrl: String? = null
        if (intent.data != null) {
            // this means the video was called though another app
            videoUrl = intent.data!!.toString()
        } else if (intent.getStringExtra(Intent.EXTRA_TEXT) != null) {
            //this means that video was called through share menu
            val extraText = intent.getStringExtra(Intent.EXTRA_TEXT)
            val uris = getUris(extraText)
            videoUrl = if (uris.isNotEmpty()) uris[0] else null
        }

        return videoUrl
    }

    private fun removeHeadingGibberish(input: String): String {
        var start = 0
        for (i in input.indexOf("://") - 1 downTo 0) {
            if (!input.substring(i, i + 1).matches("\\p{L}".toRegex())) {
                start = i + 1
                break
            }
        }
        return input.substring(start, input.length)
    }

    private fun trim(input: String?): String? {
        if (input == null || input.isEmpty()) {
            return input
        } else {
            var output: String = input
            while (output.isNotEmpty() && output.substring(0, 1).matches(REGEX_REMOVE_FROM_URL.toRegex())) {
                output = output.substring(1)
            }
            while (output.isNotEmpty() && output.substring(output.length - 1, output.length).matches(REGEX_REMOVE_FROM_URL.toRegex())) {
                output = output.substring(0, output.length - 1)
            }
            return output
        }
    }

    /**
     * Retrieves all Strings which look remotely like URLs getTabFrom a text.
     * Used if NewPipe was called through share menu.
     *
     * @param sharedText text to scan for URLs.
     * @return potential URLs
     */
    private fun getUris(sharedText: String?): Array<String> {
        val result = HashSet<String>()
        if (sharedText != null) {
            val array = sharedText.split("\\p{Space}".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            for (s in array) {
                val string = trim(s)!!
                if (string.isNotEmpty()) {
                    when {
                        string.matches(".+://.+".toRegex()) -> result.add(removeHeadingGibberish(string))
                        string.matches(".+\\..+".toRegex()) -> result.add("http://$string")
                    }
                }
            }
        }
        return result.toTypedArray()
    }

    companion object {
        private val TAG = this::class.java.simpleName
        /**
         * Removes invisible separators (\p{Z}) and punctuation characters including
         * brackets (\p{P}). See http://www.regular-expressions.info/unicode.html for
         * more details.
         */
        private const val REGEX_REMOVE_FROM_URL = "[\\p{Z}\\p{P}]"
    }
}
