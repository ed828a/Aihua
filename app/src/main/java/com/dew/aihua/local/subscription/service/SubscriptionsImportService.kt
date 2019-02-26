package com.dew.aihua.local.subscription.service

import android.app.Service
import android.content.Intent
import android.text.TextUtils
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.dew.aihua.R
import com.dew.aihua.local.subscription.ImportExportEventListener
import com.dew.aihua.local.subscription.ImportExportJsonHelper
import com.dew.aihua.repository.database.subscription.SubscriptionEntity
import com.dew.aihua.repository.remote.helper.ExtractorHelper
import com.dew.aihua.util.Constants
import com.grack.nanojson.JsonObject
import com.grack.nanojson.JsonParser
import io.reactivex.Flowable
import io.reactivex.Notification
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Consumer
import io.reactivex.functions.Function
import io.reactivex.schedulers.Schedulers
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.channel.ChannelInfo
import org.schabi.newpipe.extractor.subscription.SubscriptionExtractor
import org.schabi.newpipe.extractor.subscription.SubscriptionItem
import java.io.*
import java.util.ArrayList

/**
 *  Created by Edward on 2/23/2019.
 */
class SubscriptionsImportService : BaseImportExportService() {

    private var subscription: Subscription? = null
    private var currentMode: Int = 0
    private var currentServiceId: Int = 0

    private var channelUrl: String? = null
    private var inputStream: InputStream? = null

    private val subscriber: Subscriber<List<SubscriptionEntity>>
        get() = object : Subscriber<List<SubscriptionEntity>> {

            override fun onSubscribe(sub: Subscription) {
                subscription = sub
                sub.request(java.lang.Long.MAX_VALUE)
            }

            override fun onNext(successfulInserted: List<SubscriptionEntity>) {
                Log.d(TAG, "startImport() ${successfulInserted.size} items successfully inserted into the database")
            }

            override fun onError(error: Throwable) {
                handleError(error)
            }

            override fun onComplete() {
                androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this@SubscriptionsImportService)
                    .sendBroadcast(
                        Intent(IMPORT_COMPLETE_ACTION)
                    )
                showToast(R.string.import_complete_toast)
                stopService()
            }
        }

    private fun getNotificationsConsumer(): Consumer<Notification<ChannelInfo>> = Consumer { notification ->
        if (notification.isOnNext) {
            val name = notification.value?.name
            eventListener.onItemCompleted(if (!TextUtils.isEmpty(name)) name!! else "")
        } else if (notification.isOnError) {
            val error = notification.error
            val cause = error?.cause
            if (error is IOException) {
                throw error
            } else if (cause != null && cause is IOException) {
                throw cause
            }

            eventListener.onItemCompleted("")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null || subscription != null) return Service.START_NOT_STICKY

        currentMode = intent.getIntExtra(KEY_MODE, -1)
        currentServiceId = intent.getIntExtra(Constants.KEY_SERVICE_ID, Constants.NO_SERVICE_ID)

        if (currentMode == CHANNEL_URL_MODE) {
            channelUrl = intent.getStringExtra(KEY_VALUE)
        } else {
            val filePath = intent.getStringExtra(KEY_VALUE)
            if (TextUtils.isEmpty(filePath)) {
                stopAndReportError(
                    IllegalStateException("Importing getTabFrom input stream, but file path is empty or null"),
                    "Importing subscriptions"
                )
                return Service.START_NOT_STICKY
            }

            try {
                inputStream = FileInputStream(File(filePath))
            } catch (e: FileNotFoundException) {
                handleError(e)
                return Service.START_NOT_STICKY
            }

        }

        if (currentMode == -1 || currentMode == CHANNEL_URL_MODE && channelUrl == null) {
            val errorDescription =
                "Some important field is null or in illegal state: currentMode=[$currentMode], channelUrl=[$channelUrl], inputStream=[$inputStream]"
            stopAndReportError(IllegalStateException(errorDescription), "Importing subscriptions")
            return Service.START_NOT_STICKY
        }

        startImport()
        return Service.START_NOT_STICKY
    }

    override fun getNotificationId(): Int {
        return IMPORT_NOTIFICATION_ID
    }

    override fun getTitle(): Int {
        return R.string.import_ongoing
    }

    override fun disposeAll() {
        super.disposeAll()
        subscription?.cancel()
    }

    private fun startImport() {
        showToast(R.string.import_ongoing)

        val flowable: Flowable<List<SubscriptionItem>>? = when (currentMode) {
            CHANNEL_URL_MODE -> importFromChannelUrl()
            INPUT_STREAM_MODE -> importFromInputStream()
//            PREVIOUS_EXPORT_MODE -> importFromPreviousExport()
            else -> null
        }

        if (flowable == null) {
            val message = "Flowable given by \"importFrom\" is null (current mode: $currentMode)"
            stopAndReportError(IllegalStateException(message), "Importing subscriptions")
            return
        }

        flowable.doOnNext { subscriptionItems -> eventListener.onSizeReceived(subscriptionItems.size) }
            .flatMap { Flowable.fromIterable(it) }
            .parallel(PARALLEL_EXTRACTIONS)
            .runOn(Schedulers.io())
            .map { subscriptionItem ->
                try {
                    return@map Notification.createOnNext(
                        ExtractorHelper.getChannelInfo(subscriptionItem.serviceId, subscriptionItem.url, true)
                            .blockingGet()
                    )
                } catch (e: Throwable) {
                    return@map Notification.createOnError<ChannelInfo>(e)
                }
            }
            .sequential()

            .observeOn(Schedulers.io())
            .doOnNext(getNotificationsConsumer())
            .buffer(BUFFER_COUNT_BEFORE_INSERT)
            .map(upsertBatch())

            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(subscriber)
    }

    private fun upsertBatch(): io.reactivex.functions.Function<List<Notification<ChannelInfo>>, List<SubscriptionEntity>> {
        return Function { notificationList ->
            val infoList = ArrayList<ChannelInfo>(notificationList.size)
            for (notification in notificationList) {
                if (notification.isOnNext) infoList.add(notification.value!!)
            }

            subscriptionService.upsertAll(infoList)
        }
    }

    private fun importFromChannelUrl(): Flowable<List<SubscriptionItem>> {
        return Flowable.fromCallable {
            NewPipe.getService(currentServiceId)
                .subscriptionExtractor
                .fromChannelUrl(channelUrl)
        }
    }

    // YouTube import goes here List<SubscriptionItem>
    private fun importFromInputStream(): Flowable<List<SubscriptionItem>> {
        return Flowable.fromCallable {
            NewPipe.getService(currentServiceId)
                .subscriptionExtractor
                .fromInputStream(inputStream)
        }
    }

    private fun importFromPreviousExport(): Flowable<List<SubscriptionItem>> {
        return Flowable.fromCallable {
            //            ImportExportJsonHelper.readFrom(inputStream, null)
            readFrom(inputStream, null)
        }
    }

    @Throws(SubscriptionExtractor.InvalidSourceException::class)
    fun readFrom(inputStream: InputStream?, eventListener: ImportExportEventListener?): List<SubscriptionItem> {
        if (inputStream == null) throw SubscriptionExtractor.InvalidSourceException("input is null")

        val channels = ArrayList<SubscriptionItem>()

        try {
            val parentObject = JsonParser.`object`().from(inputStream)
            val channelsArray = parentObject.getArray(ImportExportJsonHelper.JSON_SUBSCRIPTIONS_ARRAY_KEY)
            if (channelsArray == null || channelsArray.isEmpty()) {
                Log.e(ImportExportJsonHelper.TAG, "Error: Channels array is null/Empty")
                return channels
            }

            eventListener?.onSizeReceived(channelsArray.size)

            for (obj in channelsArray) {
                if (obj is JsonObject) {
                    val serviceId = obj.getInt(ImportExportJsonHelper.JSON_SERVICE_ID_KEY, 0)
                    val url = obj.getString(ImportExportJsonHelper.JSON_URL_KEY)
                    val name = obj.getString(ImportExportJsonHelper.JSON_NAME_KEY)

                    if (url != null && name != null && !url.isEmpty() && !name.isEmpty()) {
                        channels.add(SubscriptionItem(serviceId, url, name))
                        eventListener?.onItemCompleted(name)
                    }
                }
            }
        } catch (e: Throwable) {
            throw SubscriptionExtractor.InvalidSourceException("Couldn't parse json", e)
        }

        return channels
    }

    protected fun handleError(error: Throwable) {
        super.handleError(R.string.subscriptions_import_unsuccessful, error)
    }

    companion object {
        private val TAG = SubscriptionsImportService::class.java.simpleName

        const val CHANNEL_URL_MODE = 0
        const val INPUT_STREAM_MODE = 1
        const val PREVIOUS_EXPORT_MODE = 2
        const val KEY_MODE = "key_mode"
        const val KEY_VALUE = "key_value"

        /**
         * A [local broadcast][LocalBroadcastManager] will be made with this action when the import is successfully completed.
         */
        const val IMPORT_COMPLETE_ACTION =
            "org.schabi.newpipe.local.subscription.services.SubscriptionsImportService.IMPORT_COMPLETE"
        const val IMPORT_NOTIFICATION_ID = 4568

        ///////////////////////////////////////////////////////////////////////////
        // Imports
        ///////////////////////////////////////////////////////////////////////////

        /**
         * How many extractions running in parallel.
         */
        const val PARALLEL_EXTRACTIONS = 8

        /**
         * Number of items to buffer to mass-insert in the subscriptions table, this leads to
         * a better performance as we can then use db transactions.
         */
        const val BUFFER_COUNT_BEFORE_INSERT = 50

    }
}