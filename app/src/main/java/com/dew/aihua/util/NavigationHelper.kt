package com.dew.aihua.util

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.preference.PreferenceManager
import com.dew.aihua.R
import com.dew.aihua.data.network.download.ui.activity.DownloadActivity
import com.dew.aihua.player.helper.*
import com.dew.aihua.player.playerUI.*
import com.dew.aihua.player.playerUI.BasePlayer.Companion.PLAYBACK_QUALITY
import com.dew.aihua.player.playerUI.BasePlayer.Companion.PLAY_QUEUE_KEY
import com.dew.aihua.player.playqueque.queque.PlayQueue
import com.dew.aihua.settings.SettingsActivity
import com.dew.aihua.settings.tabs.ChooseTabsFragment
import com.dew.aihua.ui.activity.MainActivity
import com.dew.aihua.ui.adapter.PlayerProxy
import com.dew.aihua.ui.fragment.*
import com.nostra13.universalimageloader.core.ImageLoader
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.stream.Stream
import org.schabi.newpipe.extractor.stream.StreamInfo
import java.util.ArrayList

/**
 *  Created by Edward on 3/2/2019.
 */
object NavigationHelper {
    const val TAG = "NavigationHelper"
    private const val MAIN_FRAGMENT_TAG = "main_fragment_tag"
    private const val SEARCH_FRAGMENT_TAG = "search_fragment_tag"

    ///////////////////////////////////////////////////////////////////////////
    // Players
    ///////////////////////////////////////////////////////////////////////////

    @JvmOverloads
    fun getPlayerIntent(context: Context?,
                        targetClazz: Class<*>,
                        playQueue: PlayQueue,
                        quality: String? = null
    ): Intent {

        val cacheKey = SerializedCache.put(playQueue, PlayQueue::class.java)
        val intent = Intent(context, targetClazz)
        if (cacheKey != null) intent.putExtra(PLAY_QUEUE_KEY, cacheKey)
        if (quality != null) intent.putExtra(PLAYBACK_QUALITY, quality)

        return intent
    }

    private fun getPlayerEnqueueIntent(context: Context?,
                                       targetClazz: Class<*>,
                                       playQueue: PlayQueue,
                                       selectOnAppend: Boolean): Intent =
        getPlayerIntent(context, targetClazz, playQueue)
            .putExtra(BasePlayer.APPEND_ONLY, true)
            .putExtra(BasePlayer.SELECT_ON_APPEND, selectOnAppend)


    fun getPlayerIntent(context: Context,
                        targetClazz: Class<*>,
                        playQueue: PlayQueue,
                        repeatMode: Int,
                        playbackSpeed: Float,
                        playbackPitch: Float,
                        playbackSkipSilence: Boolean,
                        playbackQuality: String?): Intent =
        getPlayerIntent(context, targetClazz, playQueue, playbackQuality)
            .putExtra(BasePlayer.REPEAT_MODE, repeatMode)
            .putExtra(BasePlayer.PLAYBACK_SPEED, playbackSpeed)
            .putExtra(BasePlayer.PLAYBACK_PITCH, playbackPitch)
            .putExtra(BasePlayer.PLAYBACK_SKIP_SILENCE, playbackSkipSilence)


    // main video simpleExoPlayer is ExoPlayer
    fun playOnMainPlayer(context: Context?, queue: PlayQueue) {
        val playerIntent = getPlayerIntent(context, MainVideoPlayer::class.java, queue)
        playerIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context?.startActivity(playerIntent)
    }

    fun playOnPopupPlayer(context: Context?, queue: PlayQueue) {
        if (!PermissionHelper.isPopupEnabled(context)) {
            PermissionHelper.showPopupEnablementToast(context)
            return
        }

        Toast.makeText(context, R.string.popup_playing_toast, Toast.LENGTH_SHORT).show()
        startService(context, getPlayerIntent(context, PopupVideoPlayer::class.java, queue))
    }

    fun playOnBackgroundPlayer(context: Context?, queue: PlayQueue) {
        Toast.makeText(context, R.string.background_player_playing_toast, Toast.LENGTH_SHORT).show()
        startService(context, getPlayerIntent(context, BackgroundPlayer::class.java, queue))
    }

    @JvmOverloads
    fun enqueueOnPopupPlayer(context: Context?, queue: PlayQueue, selectOnAppend: Boolean = false) {
        if (!PermissionHelper.isPopupEnabled(context)) {
            PermissionHelper.showPopupEnablementToast(context)
            return
        }

        Toast.makeText(context, R.string.popup_playing_append, Toast.LENGTH_SHORT).show()
        startService(context,
            getPlayerEnqueueIntent(context, PopupVideoPlayer::class.java, queue, selectOnAppend))
    }

    @JvmOverloads
    fun enqueueOnBackgroundPlayer(context: Context?, queue: PlayQueue, selectOnAppend: Boolean = false) {
        Toast.makeText(context, R.string.background_player_append, Toast.LENGTH_SHORT).show()
        startService(context,
            getPlayerEnqueueIntent(context, BackgroundPlayer::class.java, queue, selectOnAppend))
    }

    private fun startService(context: Context?, intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context?.startForegroundService(intent)
        } else {
            context?.startService(intent)
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // External Players
    ///////////////////////////////////////////////////////////////////////////

    fun playOnExternalAudioPlayer(context: Context, info: StreamInfo) {
        val index = ListHelper.getDefaultAudioFormat(context, info.audioStreams)

        if (index == -1) {
            Toast.makeText(context, R.string.audio_streams_empty, Toast.LENGTH_SHORT).show()
            return
        }

        val audioStream = info.audioStreams[index]
        playOnExternalPlayer(context, info.name, info.uploaderName, audioStream)
    }

    fun playOnExternalVideoPlayer(context: Context, info: StreamInfo) {
        val list = ListHelper.getSortedStreamVideosList(context, info.videoStreams, null, false)
        val videoStreamsList = ArrayList(list)
        val index = ListHelper.getDefaultResolutionIndex(context, videoStreamsList)

        if (index == -1) {
            Toast.makeText(context, R.string.video_streams_empty, Toast.LENGTH_SHORT).show()
            return
        }

        val videoStream = videoStreamsList[index]
        playOnExternalPlayer(context, info.name, info.uploaderName, videoStream)
    }

    fun playOnExternalPlayer(context: Context, name: String, artist: String, stream: Stream) {
        val intent = Intent().apply {
            action = Intent.ACTION_VIEW
            setDataAndType(Uri.parse(stream.getUrl()), stream.getFormat().getMimeType())
            putExtra(Intent.EXTRA_TITLE, name)
            putExtra("title", name)
            putExtra("artist", artist)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        resolveActivityOrAskToInstall(context, intent)
    }

    private fun resolveActivityOrAskToInstall(context: Context, intent: Intent) {
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            if (context is Activity) {
                AlertDialog.Builder(context)
                    .setMessage(R.string.no_player_found)
                    .setPositiveButton(R.string.install) { _, _ ->
                        val intent1 = Intent()
                        intent1.action = Intent.ACTION_VIEW
                        intent1.data = Uri.parse(context.getString(R.string.fdroid_vlc_url))
                        context.startActivity(intent1)
                    }
                    .setNegativeButton(R.string.cancel) { _, _ -> Log.i("NavigationHelper", "You unlocked a secret unicorn.") }
                    .show()
            } else {
                Toast.makeText(context, R.string.no_player_found_toast, Toast.LENGTH_LONG).show()
            }
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Through FragmentManager
    ///////////////////////////////////////////////////////////////////////////

    @SuppressLint("CommitTransaction")
    private fun defaultTransaction(fragmentManager: androidx.fragment.app.FragmentManager): androidx.fragment.app.FragmentTransaction {
        return fragmentManager.beginTransaction()
            .setCustomAnimations(R.animator.custom_fade_in, R.animator.custom_fade_out, R.animator.custom_fade_in, R.animator.custom_fade_out)
    }

    fun gotoMainFragment(fragmentManager: androidx.fragment.app.FragmentManager) {
        ImageLoader.getInstance().clearMemoryCache()
        // get MainFragment back out of the BackStack
        // if no MainFragment in BackStack, open a new one
        val popped = fragmentManager.popBackStackImmediate(MAIN_FRAGMENT_TAG, 0)
        if (!popped) openMainFragment(fragmentManager)
    }

    private fun openMainFragment(fragmentManager: androidx.fragment.app.FragmentManager) {
        InfoCache.trimCache()

        fragmentManager.popBackStackImmediate(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
        defaultTransaction(fragmentManager)
            .replace(R.id.fragment_holder, MainFragment())
            .addToBackStack(MAIN_FRAGMENT_TAG)
            .commit()
    }

    fun tryGotoSearchFragment(fragmentManager: androidx.fragment.app.FragmentManager): Boolean {
        for (i in 0 until fragmentManager.backStackEntryCount) {
            Log.d("NavigationHelper", "tryGoToSearchFragment(): fragmentManager.BackStackEntryAt[$i] = [${fragmentManager.getBackStackEntryAt(i)}]")
        }

        return fragmentManager.popBackStackImmediate(SEARCH_FRAGMENT_TAG, 0)
    }

    fun openSearchFragment(fragmentManager: androidx.fragment.app.FragmentManager?,
                           serviceId: Int,
                           searchString: String) {
        fragmentManager?.let {
            defaultTransaction(it)
                .replace(R.id.fragment_holder, SearchFragment.getInstance(serviceId, searchString))
                .addToBackStack(SEARCH_FRAGMENT_TAG)
                .commit()
        }

    }

    @JvmOverloads
    fun openVideoDetailFragment(
        fragmentManager: androidx.fragment.app.FragmentManager?,
        serviceId: Int,
        url: String?,
        title: String?,
        autoPlay: Boolean = false  // it was false
    ) {
        if (fragmentManager == null) {
            Log.d(TAG, "openVideoDetailFragment() with fragmentManager = null")
            return
        }

        Log.d(TAG, "openVideoDetailFragment() with fragmentManager != null")

        val fragment = fragmentManager.findFragmentById(R.id.fragment_holder)
        val locTitle = title ?: ""

        if (fragment != null && fragment is VideoDetailFragment && fragment.isVisible) {
            Log.d(TAG, "openVideoDetailFragment() with autoPlay = $autoPlay")
            fragment.setAutoplay(autoPlay)
            fragment.selectAndLoadVideo(serviceId, url, locTitle)
            return
        }

        val instance = VideoDetailFragment.getInstance(serviceId, url, locTitle)
        instance.setAutoplay(autoPlay)

        defaultTransaction(fragmentManager)
            .replace(R.id.fragment_holder, instance)
            .addToBackStack(null)
            .commit()
    }

    fun openAnchorPlayer(context: Context, serviceId: Int, url: String, name: String){
        Log.d(TAG, "openAnchorPlayer(): url = $url, name = $name")
        val playerProxy = PlayerProxy(context)
        playerProxy.directlyPlayVideoAnchorPlayer(serviceId, url, name)
    }

    fun openChannelFragment(fragmentManager: androidx.fragment.app.FragmentManager?,
                            serviceId: Int,
                            url: String,
                            name: String?) {
        val locName = name ?: ""
        if (fragmentManager != null)
            defaultTransaction(fragmentManager)
                .replace(R.id.fragment_holder, ChannelFragment.getInstance(serviceId, url, locName))
                .addToBackStack(null)
                .commit()
    }

    fun openPlaylistFragment(fragmentManager: androidx.fragment.app.FragmentManager?,
                             serviceId: Int,
                             url: String,
                             name: String?) {
        val locName = name ?: ""
        if (fragmentManager != null)
            defaultTransaction(fragmentManager)
                .replace(R.id.fragment_holder, PlaylistFragment.getInstance(serviceId, url, locName))
                .addToBackStack(null)
                .commit()
    }

    fun openWhatsNewFragment(fragmentManager: androidx.fragment.app.FragmentManager?) {
        if (fragmentManager != null)
            defaultTransaction(fragmentManager)
                .replace(R.id.fragment_holder, FeedFragment())
                .addToBackStack(null)
                .commit()
    }

    fun openBookmarksFragment(fragmentManager: androidx.fragment.app.FragmentManager?) {
        if (fragmentManager != null)
            defaultTransaction(fragmentManager)
                .replace(R.id.fragment_holder, BookmarkFragment())
                .addToBackStack(null)
                .commit()
    }

    fun openChooseTabsFragment(fragmentManager: androidx.fragment.app.FragmentManager?) {
        if (fragmentManager != null)
            defaultTransaction(fragmentManager)
                .replace(R.id.fragment_holder, ChooseTabsFragment())
                .addToBackStack(null)
                .commit()
    }


    fun openSubscriptionFragment(fragmentManager: androidx.fragment.app.FragmentManager?) {
        if (fragmentManager != null)
            defaultTransaction(fragmentManager)
                .replace(R.id.fragment_holder, SubscriptionFragment())
                .addToBackStack(null)
                .commit()
    }

    @Throws(ExtractionException::class)
    fun openKioskFragment(fragmentManager: androidx.fragment.app.FragmentManager?, serviceId: Int, kioskId: String) {
        if (fragmentManager != null)
            defaultTransaction(fragmentManager)
                .replace(R.id.fragment_holder, KioskFragment.getInstance(serviceId, kioskId))
                .addToBackStack(null)
                .commit()
    }

    fun openLocalPlaylistFragment(fragmentManager: androidx.fragment.app.FragmentManager?, playlistId: Long, name: String?) {
        val name1 = name ?: ""
        if (fragmentManager != null)
            defaultTransaction(fragmentManager)
                .replace(R.id.fragment_holder, LocalPlaylistFragment.getInstance(playlistId, name1))
                .addToBackStack(null)
                .commit()
    }

    fun openStatisticFragment(fragmentManager: androidx.fragment.app.FragmentManager?) {
        if (fragmentManager != null)
            defaultTransaction(fragmentManager)
                .replace(R.id.fragment_holder, StatisticsPlaylistFragment())
                .addToBackStack(null)
                .commit()
    }

    fun openSubscriptionsImportFragment(fragmentManager: androidx.fragment.app.FragmentManager?, serviceId: Int) {
        if (fragmentManager != null)
            defaultTransaction(fragmentManager)
                .replace(R.id.fragment_holder, SubscriptionsImportFragment.getInstance(serviceId))
                .addToBackStack(null)
                .commit()
    }

    ///////////////////////////////////////////////////////////////////////////
    // Through Intents
    ///////////////////////////////////////////////////////////////////////////

    fun openSearch(context: Context, serviceId: Int, searchString: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(Constants.KEY_SERVICE_ID, serviceId)
            putExtra(Constants.KEY_SEARCH_STRING, searchString)
            putExtra(Constants.KEY_OPEN_SEARCH, true)
        }

        context.startActivity(intent)
    }

    @JvmOverloads
    fun openChannel(context: Context, serviceId: Int, url: String, name: String? = null) {
        val openIntent = getOpenIntent(context, url, serviceId, StreamingService.LinkType.CHANNEL)
        if (name != null && !name.isEmpty()) openIntent.putExtra(Constants.KEY_TITLE, name)
        context.startActivity(openIntent)
    }

    @JvmOverloads
    fun openVideoDetail(context: Context, serviceId: Int, url: String, title: String? = null) {
        // getOpenIntent is to launch MainActivity
        val openIntent = getOpenIntent(context, url, serviceId, StreamingService.LinkType.STREAM)
        if (title != null && !title.isEmpty()) openIntent.putExtra(Constants.KEY_TITLE, title)
        context.startActivity(openIntent)
    }

    fun openMainActivity(context: Context) {
        val intent = Intent(context, MainActivity::class.java)
            .apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
        context.startActivity(intent)
    }

    fun openSettings(context: Context) {
        val intent = Intent(context, SettingsActivity::class.java)
        context.startActivity(intent)
    }

    fun openDownloads(activity: Activity): Boolean {
        if (!PermissionHelper.checkStoragePermissions(activity, PermissionHelper.DOWNLOADS_REQUEST_CODE)) {
            return false
        }
        val intent = Intent(activity, DownloadActivity::class.java)
        activity.startActivity(intent)
        return true
    }

    fun getBackgroundPlayerActivityIntent(context: Context): Intent =
        getServicePlayerActivityIntent(context, BackgroundPlayerActivity::class.java)


    fun getPopupPlayerActivityIntent(context: Context): Intent =
        getServicePlayerActivityIntent(context, PopupVideoPlayerActivity::class.java)


    private fun getServicePlayerActivityIntent(context: Context,
                                               activityClass: Class<*>): Intent {
        val intent = Intent(context, activityClass)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return intent
    }
    ///////////////////////////////////////////////////////////////////////////
    // Link handling
    ///////////////////////////////////////////////////////////////////////////

    // launching MainActivity
    private fun getOpenIntent(context: Context, url: String?, serviceId: Int, type: StreamingService.LinkType): Intent =
        Intent(context, MainActivity::class.java)
            .apply {
                putExtra(Constants.KEY_SERVICE_ID, serviceId)
                putExtra(Constants.KEY_URL, url)
                putExtra(Constants.KEY_LINK_TYPE, type)
            }


    @Throws(ExtractionException::class)
    fun getIntentByLink(context: Context, url: String?): Intent =
        getIntentByLink(context, NewPipe.getServiceByUrl(url), url)

    @Throws(ExtractionException::class)
    fun getIntentByLink(context: Context, service: StreamingService, url: String?): Intent {
        Log.d(TAG, "getIntentByLink(): url = $url")

        val linkType = service.getLinkTypeByUrl(url)
        if (linkType == StreamingService.LinkType.NONE) {
            throw ExtractionException("Url not known to service. service=$service url=$url")
        }

        val intent = getOpenIntent(context, url, service.serviceId, linkType)

        if (linkType == StreamingService.LinkType.STREAM) {
            val autoPlay = PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(context.getString(R.string.autoplay_through_intent_key), false)
            intent.putExtra(VideoDetailFragment.AUTO_PLAY, autoPlay)
        }

        return intent
    }

    private fun openMarketUrl(packageName: String): Uri =
        Uri.parse("market://details")
            .buildUpon()
            .appendQueryParameter("id", packageName)
            .build()


    private fun getGooglePlayUrl(packageName: String): Uri {
        return Uri.parse("https://play.google.com/store/apps/details")
            .buildUpon()
            .appendQueryParameter("id", packageName)
            .build()
    }

    private fun installApp(context: Context, packageName: String) {
        try {
            // Try market:// scheme
            context.startActivity(Intent(Intent.ACTION_VIEW, openMarketUrl(packageName)))
        } catch (e: ActivityNotFoundException) {
            // Fall back to google play URL (don't worry F-Droid can handle it :)
            context.startActivity(Intent(Intent.ACTION_VIEW, getGooglePlayUrl(packageName)))
        }

    }

    /**
     * Start an activity to install Kore
     * @param context the context
     */
    fun installKore(context: Context) {
        installApp(context, context.getString(R.string.kore_package))
    }

    /**
     * Start Kore app to show a video on Kodi
     *
     * For a list of supported urls see the
     * <a href="https://github.com/xbmc/Kore/blob/master/app/src/main/AndroidManifest.xml">
     *     Kore source code
     * </a>.
     * @param context the context to use
     * @param videoURL the url to the video
     */
    fun playWithKore(context: Context?, videoURL: Uri) {
        val intent = Intent(Intent.ACTION_VIEW)
            .apply {
                setPackage(context?.getString(R.string.kore_package))
                data = videoURL
            }

        context?.startActivity(intent)
    }
}
