package com.dew.aihua.data.network.helper

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import androidx.preference.PreferenceManager
import com.dew.aihua.R
import com.nostra13.universalimageloader.core.download.BaseImageDownloader
import org.schabi.newpipe.extractor.NewPipe
import java.io.IOException
import java.io.InputStream

/**
 *  Created by Edward on 3/2/2019.
 */
class ImageDownloader(context: Context) : BaseImageDownloader(context) {
    private val resources: Resources = context.resources
    private val preferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val downloadThumbnailKey: String = context.getString(R.string.download_thumbnail_key)

    private val isDownloadingThumbnail: Boolean
        get() = preferences.getBoolean(downloadThumbnailKey, true)

    @SuppressLint("ResourceType")
    @Throws(IOException::class)
    override fun getStream(imageUri: String, extra: Any?): InputStream =
        if (isDownloadingThumbnail) {
            super.getStream(imageUri, extra)
        } else {
            resources.openRawResource(R.drawable.dummy_thumbnail_dark)
        }


    @Throws(IOException::class)
    override fun getStreamFromNetwork(imageUri: String, extra: Any?): InputStream {
        val downloader = NewPipe.getDownloader() as PageDownloader
        return downloader.stream(imageUri)
    }
}
