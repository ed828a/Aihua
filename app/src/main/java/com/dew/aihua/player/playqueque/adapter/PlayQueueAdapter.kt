package com.dew.aihua.player.playqueque.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.dew.aihua.R
import com.dew.aihua.player.model.PlayQueueItem
import com.dew.aihua.player.playqueque.event.*
import com.dew.aihua.player.playqueque.queque.PlayQueue
import io.reactivex.Observer
import io.reactivex.disposables.Disposable


/**
 *  Created by Edward on 3/2/2019.
 */

class PlayQueueAdapter(private val playQueue: PlayQueue) : androidx.recyclerview.widget.RecyclerView.Adapter<androidx.recyclerview.widget.RecyclerView.ViewHolder>() {

    private val playQueueItemBuilder: PlayQueueItemBuilder = PlayQueueItemBuilder()
    private var showFooter = false
    private var footer: View? = null

    private var playQueueReactor: Disposable? = null

    private val reactor: Observer<PlayQueueEvent>
        get() = object : Observer<PlayQueueEvent> {
            override fun onSubscribe(d: Disposable) {
                playQueueReactor?.dispose()
                playQueueReactor = d
            }

            override fun onNext(playQueueMessage: PlayQueueEvent) {
                if (playQueueReactor != null) onPlayQueueChanged(playQueueMessage)
            }

            override fun onError(e: Throwable) {
                Log.d(TAG, "onError(): ${e.message}")
            }

            override fun onComplete() {
                dispose()
            }
        }

    val items: List<PlayQueueItem>
        get() = playQueue.streams!!

    inner class HFHolder(var view: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(view)

    init {
        if (playQueue.broadcastReceiver == null) {
            throw IllegalStateException("Play Queue has not been initialized.")
        }

        playQueue.broadcastReceiver!!.toObservable().subscribe(reactor)
    }

    private fun onPlayQueueChanged(message: PlayQueueEvent) {
        when (message.type()) {
            PlayQueueEventType.RECOVERY -> { /* no-op */ }

            PlayQueueEventType.SELECT -> {
                val selectEvent = message as SelectEvent
                notifyItemChanged(selectEvent.oldIndex)
                notifyItemChanged(selectEvent.newIndex)
            }

            PlayQueueEventType.APPEND -> {
                val appendEvent = message as AppendEvent
                notifyItemRangeInserted(playQueue.size(), appendEvent.amount)
            }

            PlayQueueEventType.ERROR -> {
                val errorEvent = message as ErrorEvent
                if (!errorEvent.isSkippable) {
                    notifyItemRemoved(errorEvent.errorIndex)
                }
                notifyItemChanged(errorEvent.errorIndex)
                notifyItemChanged(errorEvent.queueIndex)
            }

            PlayQueueEventType.REMOVE -> {
                val removeEvent = message as RemoveEvent
                notifyItemRemoved(removeEvent.removeIndex)
                notifyItemChanged(removeEvent.queueIndex)
            }

            PlayQueueEventType.MOVE -> {
                val moveEvent = message as MoveEvent
                notifyItemMoved(moveEvent.fromIndex, moveEvent.toIndex)
            }

            PlayQueueEventType.INIT,
            PlayQueueEventType.REORDER -> notifyDataSetChanged()
        }
    }

    fun dispose() {
        playQueueReactor?.dispose()
        playQueueReactor = null
    }

    fun setSelectedListener(listener: PlayQueueItemBuilder.OnSelectedListener) {
        playQueueItemBuilder.setOnSelectedListener(listener)
    }

    fun unsetSelectedListener() {
        playQueueItemBuilder.setOnSelectedListener(null)
    }

    fun setFooter(footer: View) {
        this.footer = footer
        notifyItemChanged(playQueue.size())
    }

    fun showFooter(show: Boolean) {
        showFooter = show
        notifyItemChanged(playQueue.size())
    }

    override fun getItemCount(): Int {
        var count = playQueue.streams!!.size
        if (footer != null && showFooter) count++
        return count
    }

    override fun getItemViewType(position: Int): Int {
        return if (footer != null && position == playQueue.streams!!.size && showFooter) {
            FOOTER_VIEW_TYPE_ID
        } else ITEM_VIEW_TYPE_ID

    }

    override fun onCreateViewHolder(parent: ViewGroup, type: Int): androidx.recyclerview.widget.RecyclerView.ViewHolder {
        return when (type) {
            FOOTER_VIEW_TYPE_ID -> HFHolder(footer!!)
            ITEM_VIEW_TYPE_ID -> PlayQueueItemHolder(LayoutInflater.from(parent.context).inflate(R.layout.play_queue_item, parent, false))
            else -> {
                Log.e(TAG, "Attempting to create view holder with undefined type: $type")
                FallbackViewHolder(View(parent.context))
            }
        }
    }

    override fun onBindViewHolder(holder: androidx.recyclerview.widget.RecyclerView.ViewHolder, position: Int) {
        if (holder is PlayQueueItemHolder) {

            // Build the list item
            playQueueItemBuilder.buildStreamInfoItem(holder, playQueue.streams!![position])

            // Check if the current item should be selected/highlighted
            val isSelected = playQueue.index == position
            holder.itemSelected.visibility = if (isSelected) View.VISIBLE else View.INVISIBLE
            holder.itemView.isSelected = isSelected
        } else if (holder is HFHolder && position == playQueue.streams!!.size && footer != null && showFooter) {
            holder.view = footer!!
        }
    }

    companion object {
        private val TAG = PlayQueueAdapter::class.java.toString()

        private const val ITEM_VIEW_TYPE_ID = 0
        private const val FOOTER_VIEW_TYPE_ID = 1
    }
}
