package com.dew.aihua.infolist.adapter

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.view.View
import android.widget.TextView
import com.dew.aihua.R
import org.schabi.newpipe.extractor.stream.StreamInfoItem

/**
 *  Created by Edward on 3/2/2019.
 */
class InfoItemDialog(activity: Activity,
                     commands: Array<String>,
                     actions: DialogInterface.OnClickListener,
                     title: String,
                     additionalDetails: String?) {
    private val dialog: AlertDialog

    constructor(activity: Activity,
                info: StreamInfoItem,
                commands: Array<String>,
                actions: DialogInterface.OnClickListener) : this(activity, commands, actions, info.name, info.uploaderName)

    init {

        val bannerView = View.inflate(activity, R.layout.dialog_title, null)
        bannerView.isSelected = true

        val titleView = bannerView.findViewById<TextView>(R.id.itemTitleView)
        titleView.text = title

        val detailsView = bannerView.findViewById<TextView>(R.id.itemAdditionalDetails)
        if (additionalDetails != null) {
            detailsView.text = additionalDetails
            detailsView.visibility = View.VISIBLE
        } else {
            detailsView.visibility = View.GONE
        }

        dialog = AlertDialog.Builder(activity)
            .setCustomTitle(bannerView)
            .setItems(commands, actions)
            .create()
    }

    fun show() {
        dialog.show()
    }
}
