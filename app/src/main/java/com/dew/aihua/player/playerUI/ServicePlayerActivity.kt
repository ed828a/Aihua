package com.dew.aihua.player.playerUI

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.ItemTouchHelper
import com.dew.aihua.R
import com.dew.aihua.player.dialog.PlaybackParameterDialog
import com.dew.aihua.player.helper.Localization
import com.dew.aihua.player.helper.PlayerHelper.formatPitch
import com.dew.aihua.player.helper.PlayerHelper.formatSpeed
import com.dew.aihua.player.helper.PlayerNavigationHelper
import com.dew.aihua.player.helper.ThemeHelper
import com.dew.aihua.player.model.PlayQueueItem
import com.dew.aihua.player.model.PlayerServiceBinder
import com.dew.aihua.player.playback.PlayerEventListener
import com.dew.aihua.player.playqueque.adapter.PlayQueueItemBuilder
import com.dew.aihua.player.playqueque.adapter.PlayQueueItemTouchCallback
import com.dew.aihua.player.playqueque.holder.PlayQueueItemHolder
import com.dew.aihua.ui.contract.OnScrollBelowItemsListener
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.Player
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamType

/**
 *  Created by Edward on 3/2/2019.
 */
abstract class ServicePlayerActivity : AppCompatActivity(),
    PlayerEventListener,
    SeekBar.OnSeekBarChangeListener,
    View.OnClickListener,
    PlaybackParameterDialog.Callback {

    private var serviceBound: Boolean = false
    private var serviceConnection: ServiceConnection? = null

    protected var player: BasePlayer? = null

    private var seeking: Boolean = false
    private var redraw: Boolean = false

    private var rootView: View? = null

    private var itemsList: androidx.recyclerview.widget.RecyclerView? = null
    private var itemTouchHelper: ItemTouchHelper? = null

    private var metadata: LinearLayout? = null
    private var metadataTitle: TextView? = null
    private var metadataArtist: TextView? = null

    private var progressSeekBar: SeekBar? = null
    private var progressCurrentTime: TextView? = null
    private var progressEndTime: TextView? = null
    private var progressLiveSync: TextView? = null
    private var seekDisplay: TextView? = null

    private var repeatButton: ImageButton? = null
    private var backwardButton: ImageButton? = null
    private var playPauseButton: ImageButton? = null
    private var forwardButton: ImageButton? = null
    private var shuffleButton: ImageButton? = null
    private var progressBar: ProgressBar? = null

    private var playbackSpeedButton: TextView? = null
    private var playbackPitchButton: TextView? = null

    ////////////////////////////////////////////////////////////////////////////
    // Abstracts
    ////////////////////////////////////////////////////////////////////////////

    abstract fun getTag(): String

    abstract fun getSupportActionTitle(): String

    abstract fun getBindIntent(): Intent

    abstract fun getPlayerOptionMenuResource(): Int

    abstract fun getPlayerShutdownIntent(): Intent

    abstract fun startPlayerListener()

    abstract fun stopPlayerListener()

    abstract fun onPlayerOptionSelected(item: MenuItem): Boolean

    ////////////////////////////////////////////////////////////////////////////
    // Component Helpers
    ////////////////////////////////////////////////////////////////////////////

    private fun getQueueScrollListener(): OnScrollBelowItemsListener =
        object : OnScrollBelowItemsListener() {
            override fun onScrolledDown(recyclerView: androidx.recyclerview.widget.RecyclerView) {
                if (player != null && player!!.playQueue != null && !player!!.playQueue!!.isComplete) {
                    player!!.playQueue!!.fetch()
                } else if (itemsList != null) {
                    itemsList!!.clearOnScrollListeners()
                }
            }
        }


    private fun getItemTouchCallback(): ItemTouchHelper.SimpleCallback =
        object : PlayQueueItemTouchCallback() {
            override fun onMove(sourceIndex: Int, targetIndex: Int) {
                player?.playQueue!!.move(sourceIndex, targetIndex)
            }
        }

    private fun getOnSelectedListener(): PlayQueueItemBuilder.OnSelectedListener =
        object : PlayQueueItemBuilder.OnSelectedListener {
            override fun selected(item: PlayQueueItem, view: View) {
                player?.onSelected(item)
            }

            override fun held(item: PlayQueueItem, view: View) {
                if (player == null) return

                val index = player!!.playQueue!!.indexOf(item)
                if (index != -1) buildItemPopupMenu(item, view)
            }

            override fun onStartDrag(viewHolder: PlayQueueItemHolder) {
                itemTouchHelper?.startDrag(viewHolder)
            }
        }

    ////////////////////////////////////////////////////////////////////////////
    // Activity Lifecycle
    ////////////////////////////////////////////////////////////////////////////

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ThemeHelper.setTheme(this)
        setContentView(R.layout.activity_player_queue_control)
        rootView = findViewById(R.id.main_content)

        val toolbar = rootView!!.findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.title = getSupportActionTitle()
        }

        serviceConnection = getServiceConnection()
        bind()
    }

    override fun onResume() {
        super.onResume()
        if (redraw) {
            recreate()
            redraw = false
        }
        // add it for testing
        appendAllToPlaylist()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_play_queue, menu)
        menuInflater.inflate(getPlayerOptionMenuResource(), menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
            R.id.action_append_playlist -> {
                appendAllToPlaylist()
                return true
            }
            R.id.action_settings -> {
                // TODO: open later
//                NavigationHelper.openSettings(this)
                redraw = true
                return true
            }
            R.id.action_system_audio -> {
                startActivity(Intent(Settings.ACTION_SOUND_SETTINGS))
                return true
            }
            R.id.action_switch_main -> {
                this.player!!.setRecovery()
                applicationContext.sendBroadcast(getPlayerShutdownIntent())
                applicationContext.startActivity(getSwitchIntent(MainVideoPlayer::class.java))
                return true
            }
        }


        return onPlayerOptionSelected(item) || super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()
        unbind()
    }

    protected fun getSwitchIntent(clazz: Class<*>): Intent =
        PlayerNavigationHelper.getPlayerIntent(
            applicationContext,
            clazz,
            this.player!!.playQueue!!,
            this.player!!.repeatMode,
            this.player!!.playbackSpeed,
            this.player!!.playbackPitch,
            this.player!!.playbackSkipSilence, null
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)


    ////////////////////////////////////////////////////////////////////////////
    // Service Connection
    ////////////////////////////////////////////////////////////////////////////

    private fun bind() {
        val success = bindService(getBindIntent(), serviceConnection!!, BIND_AUTO_CREATE)
        if (!success) {
            unbindService(serviceConnection!!)
        }
        serviceBound = success
    }

    private fun unbind() {
        if (serviceBound) {
            unbindService(serviceConnection!!)
            serviceBound = false
            stopPlayerListener()

            if (player != null && player!!.playQueueAdapter != null) {
                player!!.playQueueAdapter!!.unsetSelectedListener()
            }
            if (itemsList != null) itemsList!!.adapter = null
            if (itemTouchHelper != null) itemTouchHelper!!.attachToRecyclerView(null)

            itemsList = null
            itemTouchHelper = null
            player = null
        }
    }

    private fun getServiceConnection(): ServiceConnection {
        return object : ServiceConnection {
            override fun onServiceDisconnected(name: ComponentName) {
                Log.d(getTag(), "Player service is disconnected")
            }

            override fun onServiceConnected(name: ComponentName, service: IBinder) {
                Log.d(getTag(), "Player service is connected")

                if (service is PlayerServiceBinder) {
                    player = service.playerInstance
                }

                if (player == null || player!!.playQueue == null ||
                    player!!.playQueueAdapter == null || player!!.simpleExoPlayer == null
                ) {
                    unbind()
                    finish()
                } else {
                    buildComponents()
                    startPlayerListener()
                }
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    // Component Building
    ////////////////////////////////////////////////////////////////////////////

    private fun buildComponents() {
        buildQueue()
        buildMetadata()
        buildSeekBar()
        buildControls()
    }

    private fun buildQueue() {
        itemsList = findViewById(R.id.play_queue)
        itemsList!!.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        itemsList!!.adapter = player!!.playQueueAdapter
        itemsList!!.isClickable = true
        itemsList!!.isLongClickable = true
        itemsList!!.clearOnScrollListeners()
        itemsList!!.addOnScrollListener(getQueueScrollListener())

        itemTouchHelper = ItemTouchHelper(getItemTouchCallback())
        itemTouchHelper!!.attachToRecyclerView(itemsList)

        player!!.playQueueAdapter!!.setSelectedListener(getOnSelectedListener())
    }

    private fun buildMetadata() {
        metadata = rootView!!.findViewById(R.id.metadata)
        metadataTitle = rootView!!.findViewById(R.id.song_name)
        metadataArtist = rootView!!.findViewById(R.id.artist_name)

        metadata!!.setOnClickListener(this)
        metadataTitle!!.isSelected = true
        metadataArtist!!.isSelected = true
    }

    private fun buildSeekBar() {
        progressCurrentTime = rootView!!.findViewById(R.id.current_time)
        progressSeekBar = rootView!!.findViewById(R.id.seek_bar)
        progressEndTime = rootView!!.findViewById(R.id.end_time)
        progressLiveSync = rootView!!.findViewById(R.id.live_sync)
        seekDisplay = rootView!!.findViewById(R.id.seek_display)

        progressSeekBar!!.setOnSeekBarChangeListener(this)
        progressLiveSync!!.setOnClickListener(this)
    }

    private fun buildControls() {
        repeatButton = rootView!!.findViewById(R.id.control_repeat)
        backwardButton = rootView!!.findViewById(R.id.control_backward)
        playPauseButton = rootView!!.findViewById(R.id.control_play_pause)
        forwardButton = rootView!!.findViewById(R.id.control_forward)
        shuffleButton = rootView!!.findViewById(R.id.control_shuffle)
        playbackSpeedButton = rootView!!.findViewById(R.id.control_playback_speed)
        playbackPitchButton = rootView!!.findViewById(R.id.control_playback_pitch)
        progressBar = rootView!!.findViewById(R.id.control_progress_bar)

        repeatButton!!.setOnClickListener(this)
        backwardButton!!.setOnClickListener(this)
        playPauseButton!!.setOnClickListener(this)
        forwardButton!!.setOnClickListener(this)
        shuffleButton!!.setOnClickListener(this)
        playbackSpeedButton!!.setOnClickListener(this)
        playbackPitchButton!!.setOnClickListener(this)
    }

    private fun buildItemPopupMenu(item: PlayQueueItem, view: View) {
        val menu = PopupMenu(this, view)
        val remove = menu.menu.add(
            RECYCLER_ITEM_POPUP_MENU_GROUP_ID, /*pos=*/0,
            Menu.NONE, R.string.play_queue_remove
        )
        remove.setOnMenuItemClickListener { _ ->
            if (player == null) false
            else {
                val index = player!!.playQueue!!.indexOf(item)
                if (index != -1) player!!.playQueue!!.remove(index)
                true
            }
        }

        val detail = menu.menu.add(
            RECYCLER_ITEM_POPUP_MENU_GROUP_ID, /*pos=*/1,
            Menu.NONE, R.string.play_queue_stream_detail
        )
        detail.setOnMenuItemClickListener { _ ->
            onOpenDetail(item.serviceId, item.url, item.title)
            true
        }

        val append = menu.menu.add(
            RECYCLER_ITEM_POPUP_MENU_GROUP_ID, /*pos=*/2,
            Menu.NONE, R.string.append_playlist
        )
        append.setOnMenuItemClickListener { _ ->
            openPlaylistAppendDialog(listOf(item))
            true
        }

        val share = menu.menu.add(
            RECYCLER_ITEM_POPUP_MENU_GROUP_ID, /*pos=*/3,
            Menu.NONE, R.string.share
        )
        share.setOnMenuItemClickListener { _ ->
            shareUrl(item.title, item.url)
            true
        }

        menu.show()
    }

    private fun onOpenDetail(serviceId: Int, videoUrl: String, videoTitle: String) {
        // TODO: open later
//        NavigationHelper.openVideoDetail(this, serviceId, videoUrl, videoTitle)
    }

    private fun scrollToSelected() {
        if (player == null) return

        val currentPlayingIndex = player!!.playQueue!!.index
        val currentVisibleIndex: Int
        currentVisibleIndex = if (itemsList!!.layoutManager is androidx.recyclerview.widget.LinearLayoutManager) {
            val layout = itemsList!!.layoutManager as androidx.recyclerview.widget.LinearLayoutManager?
            layout!!.findFirstVisibleItemPosition()
        } else {
            0
        }

        val distance = Math.abs(currentPlayingIndex - currentVisibleIndex)
        if (distance < SMOOTH_SCROLL_MAXIMUM_DISTANCE) {
            itemsList!!.smoothScrollToPosition(currentPlayingIndex)
        } else {
            itemsList!!.scrollToPosition(currentPlayingIndex)
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    // Component On-Click Listener
    ////////////////////////////////////////////////////////////////////////////

    override fun onClick(view: View) {
        if (player == null) return

        when (view.id) {
            repeatButton!!.id -> player!!.onRepeatClicked()
            backwardButton!!.id -> player!!.onPlayPrevious()
            playPauseButton!!.id -> player!!.onPlayPause()
            forwardButton!!.id -> player!!.onPlayNext()
            shuffleButton!!.id -> player!!.onShuffleClicked()
            playbackSpeedButton!!.id,
            playbackPitchButton!!.id -> openPlaybackParameterDialog()
            metadata!!.id -> scrollToSelected()
            progressLiveSync!!.id -> player!!.seekToDefault()
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    // Playback Parameters
    ////////////////////////////////////////////////////////////////////////////

    private fun openPlaybackParameterDialog() {
        if (player == null) return
        PlaybackParameterDialog.newInstance(
            player!!.playbackSpeed.toDouble(), player!!.playbackPitch.toDouble(),
            player!!.playbackSkipSilence
        ).show(supportFragmentManager, getTag())
    }

    override fun onPlaybackParameterChanged(
        playbackTempo: Float, playbackPitch: Float,
        playbackSkipSilence: Boolean
    ) {
        if (player != null) {
            player!!.setPlaybackParameters(playbackTempo, playbackPitch, playbackSkipSilence)
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    // Seekbar Listener
    ////////////////////////////////////////////////////////////////////////////

    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        if (fromUser) {
            val seekTime = Localization.getDurationString((progress / 1000).toLong())
            progressCurrentTime!!.text = seekTime
            seekDisplay!!.text = seekTime
        }
    }

    override fun onStartTrackingTouch(seekBar: SeekBar) {
        seeking = true
        seekDisplay!!.visibility = View.VISIBLE
    }

    override fun onStopTrackingTouch(seekBar: SeekBar) {
        if (player != null) player!!.seekTo(seekBar.progress.toLong())
        seekDisplay!!.visibility = View.GONE
        seeking = false
    }

    ////////////////////////////////////////////////////////////////////////////
    // Playlist append
    ////////////////////////////////////////////////////////////////////////////

    private fun appendAllToPlaylist() {
        if (player != null && player!!.playQueue != null) {
            openPlaylistAppendDialog(player!!.playQueue!!.streams!!)
        }
    }

    private fun openPlaylistAppendDialog(playlist: List<PlayQueueItem>) {
        Log.d(getTag(), "openPlaylistAppendDialog(playlist = $playlist)")
        // TODO: open later
//        PlaylistAppendDialog.fromPlayQueueItems(playlist)
//            .show(supportFragmentManager, getTag())
    }

    ////////////////////////////////////////////////////////////////////////////
    // Share
    ////////////////////////////////////////////////////////////////////////////

    private fun shareUrl(subject: String, url: String) {
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/plain"
        intent.putExtra(Intent.EXTRA_SUBJECT, subject)
        intent.putExtra(Intent.EXTRA_TEXT, url)
        startActivity(Intent.createChooser(intent, getString(R.string.share_dialog_title)))
    }

    ////////////////////////////////////////////////////////////////////////////
    // Binding Service Listener
    ////////////////////////////////////////////////////////////////////////////

    override fun onPlaybackUpdate(state: Int, repeatMode: Int, shuffled: Boolean, parameters: PlaybackParameters) {
        onStateChanged(state)
        onPlayModeChanged(repeatMode, shuffled)
        onPlaybackParameterChanged(parameters)
        onMaybePlaybackAdapterChanged()
    }

    override fun onProgressUpdate(currentProgress: Int, duration: Int, bufferPercent: Int) {
        // Set buffer progress
        progressSeekBar!!.secondaryProgress = (progressSeekBar!!.max * (bufferPercent.toFloat() / 100)).toInt()

        // Set Duration
        progressSeekBar!!.max = duration
        progressEndTime!!.text = Localization.getDurationString((duration / 1000).toLong())

        // Set current time if not seeking
        if (!seeking) {
            progressSeekBar!!.progress = currentProgress
            progressCurrentTime!!.text = Localization.getDurationString((currentProgress / 1000).toLong())
        }

        if (player != null) {
            progressLiveSync!!.isClickable = !player!!.isLiveEdge
        }

        // this will make sure progressCurrentTime has the same width as progressEndTime
        val currentTimeParams = progressCurrentTime!!.layoutParams
        currentTimeParams.width = progressEndTime!!.width
        progressCurrentTime!!.layoutParams = currentTimeParams
    }

    override fun onMetadataUpdate(info: StreamInfo?) {
        if (info != null) {
            metadataTitle!!.text = info.name
            metadataArtist!!.text = info.uploaderName

            progressEndTime!!.visibility = View.GONE
            progressLiveSync!!.visibility = View.GONE
            when (info.streamType) {
                StreamType.LIVE_STREAM, StreamType.AUDIO_LIVE_STREAM -> progressLiveSync!!.visibility = View.VISIBLE
                else -> progressEndTime!!.visibility = View.VISIBLE
            }

            scrollToSelected()
        }
    }

    override fun onServiceStopped() {
        unbind()
        finish()
    }

    ////////////////////////////////////////////////////////////////////////////
    // Binding Service Helper
    ////////////////////////////////////////////////////////////////////////////

    private fun onStateChanged(state: Int) {
        when (state) {
            BasePlayer.STATE_PAUSED -> playPauseButton!!.setImageResource(R.drawable.ic_play_arrow_white)
            BasePlayer.STATE_PLAYING -> playPauseButton!!.setImageResource(R.drawable.ic_pause_white)
            BasePlayer.STATE_COMPLETED -> playPauseButton!!.setImageResource(R.drawable.ic_replay_white)
            else -> {
            }
        }

        when (state) {
            BasePlayer.STATE_PAUSED, BasePlayer.STATE_PLAYING, BasePlayer.STATE_COMPLETED -> {
                playPauseButton!!.isClickable = true
                playPauseButton!!.visibility = View.VISIBLE
                progressBar!!.visibility = View.GONE
            }
            else -> {
                playPauseButton!!.isClickable = false
                playPauseButton!!.visibility = View.INVISIBLE
                progressBar!!.visibility = View.VISIBLE
            }
        }
    }

    private fun onPlayModeChanged(repeatMode: Int, shuffled: Boolean) {
        when (repeatMode) {
            Player.REPEAT_MODE_OFF -> repeatButton!!.setImageResource(R.drawable.exo_controls_repeat_off)
            Player.REPEAT_MODE_ONE -> repeatButton!!.setImageResource(R.drawable.exo_controls_repeat_one)
            Player.REPEAT_MODE_ALL -> repeatButton!!.setImageResource(R.drawable.exo_controls_repeat_all)
        }

        val shuffleAlpha = if (shuffled) 255 else 77
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            shuffleButton!!.imageAlpha = shuffleAlpha
        } else {
            @Suppress("DEPRECATION")
            shuffleButton!!.setAlpha(shuffleAlpha)
        }
    }

    private fun onPlaybackParameterChanged(parameters: PlaybackParameters?) {
        if (parameters != null) {
            playbackSpeedButton!!.text = formatSpeed(parameters.speed.toDouble())
            playbackPitchButton!!.text = formatPitch(parameters.pitch.toDouble())
        }
    }

    private fun onMaybePlaybackAdapterChanged() {
        if (itemsList == null || player == null) return
        val maybeNewAdapter = player!!.playQueueAdapter
        if (maybeNewAdapter != null && itemsList!!.adapter !== maybeNewAdapter) {
            itemsList!!.adapter = maybeNewAdapter
        }
    }

    companion object {
        ////////////////////////////////////////////////////////////////////////////
        // Views
        ////////////////////////////////////////////////////////////////////////////

        private const val RECYCLER_ITEM_POPUP_MENU_GROUP_ID = 47

        private const val SMOOTH_SCROLL_MAXIMUM_DISTANCE = 80
    }
}
