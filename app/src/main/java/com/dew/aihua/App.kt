package com.dew.aihua


import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.multidex.MultiDex
import androidx.preference.PreferenceManager
import com.dew.aihua.report.AcraReportSenderFactory
import com.dew.aihua.report.ErrorActivity
import com.dew.aihua.report.ErrorInfo
import com.dew.aihua.report.UserAction
import com.dew.aihua.repository.remote.helper.ExtractorHelper
import com.dew.aihua.repository.remote.helper.PageDownloader
import com.dew.aihua.settings.SettingsActivity
import com.dew.aihua.util.ImageDownloader
import com.dew.aihua.util.Localization
import com.dew.aihua.util.StateSaver
import com.facebook.stetho.Stetho
import com.facebook.stetho.okhttp3.StethoInterceptor
import com.nostra13.universalimageloader.cache.memory.impl.LRULimitedMemoryCache
import com.nostra13.universalimageloader.core.ImageLoader
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration

import com.squareup.leakcanary.*
import io.reactivex.annotations.NonNull
import io.reactivex.exceptions.CompositeException
import io.reactivex.exceptions.MissingBackpressureException
import io.reactivex.exceptions.OnErrorNotImplementedException
import io.reactivex.exceptions.UndeliverableException
import io.reactivex.functions.Consumer
import io.reactivex.plugins.RxJavaPlugins
import okhttp3.OkHttpClient
import org.acra.ACRA
import org.acra.config.ACRAConfigurationException
import org.acra.config.ConfigurationBuilder
import org.acra.sender.ReportSenderFactory
import org.schabi.newpipe.extractor.NewPipe
import java.io.File
import java.io.IOException
import java.io.InterruptedIOException
import java.net.SocketException
import java.util.concurrent.TimeUnit

class App : Application() {
    private var refWatcher: RefWatcher? = null

    protected val downloader: PageDownloader
        get() = if (BuildConfig.DEBUG) {
            PageDownloader.getInstance(
                OkHttpClient.Builder()
                    .addNetworkInterceptor(StethoInterceptor())
            )
        } else {
            PageDownloader.getInstance(null)
        }

    protected open val isDisposedRxExceptionsReported: Boolean
        get() = if (BuildConfig.DEBUG){
            PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean(getString(R.string.allow_disposed_exceptions_key), false)
        } else false

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)

        MultiDex.install(this)
        initACRA()
    }


    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            initStetho()
        }


        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not initialize your app in this process.
            return
        }
        refWatcher = installLeakCanary()

        // Initialize settings first because others inits can use its values
        SettingsActivity.initSettings(this)

        NewPipe.init(downloader, Localization.getPreferredExtractorLocal(this))
        StateSaver.init(this)
        initNotificationChannel()

        // Initialize image loader
        ImageLoader.getInstance().init(getImageLoaderConfigurations(10, 50))

        configureRxJavaErrorHandler()

    }

    private fun initNotificationChannel() {
        if (Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) {
            return
        }

        val id = getString(R.string.notification_channel_id)
        val name = getString(R.string.notification_channel_name)
        val description = getString(R.string.notification_channel_description)

        // Keep this below DEFAULT to avoid making noise on every notification update
        val importance = NotificationManager.IMPORTANCE_LOW

        val mChannel = NotificationChannel(id, name, importance)
        mChannel.description = description

        val mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mNotificationManager.createNotificationChannel(mChannel)
    }

    private fun configureRxJavaErrorHandler() {
        // https://github.com/ReactiveX/RxJava/wiki/What's-different-in-2.0#error-handling
        RxJavaPlugins.setErrorHandler(object : Consumer<Throwable> {
            @Throws(Exception::class)
            override fun accept(@NonNull throwable: Throwable) {
                var throwable = throwable
                Log.e(TAG, "RxJavaPlugins.ErrorHandler called with -> : throwable = [${throwable.javaClass.name}]")

                if (throwable is UndeliverableException) {
                    // As UndeliverableException is a wrapper, get the cause of it to get the "real" exception
                    throwable = throwable.cause!!
                }

                val errors: List<Throwable>
                if (throwable is CompositeException) {
                    errors = throwable.exceptions
                } else {
                    errors = listOf(throwable)
                }

                for (error in errors) {
                    if (isThrowableIgnored(error)) return
                    if (isThrowableCritical(error)) {
                        reportException(error)
                        return
                    }
                }

                // Out-of-lifecycle exceptions should only be reported if a debug user wishes so,
                // When exception is not reported, log it
                if (isDisposedRxExceptionsReported) {
                    reportException(throwable)
                } else {
                    Log.e(TAG, "RxJavaPlugin: Undeliverable Exception received: ", throwable)
                }
            }

            private fun isThrowableIgnored(@NonNull throwable: Throwable): Boolean {
                // Don't crash the application over a simple network problem
                return ExtractorHelper.hasAssignableCauseThrowable(
                    throwable,
                    IOException::class.java,
                    SocketException::class.java, // network api cancellation
                    InterruptedException::class.java,
                    InterruptedIOException::class.java) // blocking code disposed
            }

            private fun isThrowableCritical(@NonNull throwable: Throwable): Boolean {
                // Though these exceptions cannot be ignored
                return ExtractorHelper.hasAssignableCauseThrowable(
                    throwable,
                    NullPointerException::class.java,
                    IllegalArgumentException::class.java, // bug in app
                    OnErrorNotImplementedException::class.java,
                    MissingBackpressureException::class.java,
                    IllegalStateException::class.java) // bug in operator
            }

            private fun reportException(@NonNull throwable: Throwable) {
                // Throw uncaught exception that will trigger the report system
                Thread.currentThread().uncaughtExceptionHandler
                    .uncaughtException(Thread.currentThread(), throwable)
            }
        })
    }

    private fun getImageLoaderConfigurations(
        memoryCacheSizeMb: Int,
        diskCacheSizeMb: Int
    ): ImageLoaderConfiguration =
        ImageLoaderConfiguration.Builder(this)
            .memoryCache(LRULimitedMemoryCache(memoryCacheSizeMb * 1024 * 1024))
            .diskCacheSize(diskCacheSizeMb * 1024 * 1024)
            .imageDownloader(ImageDownloader(applicationContext))
            .build()

    private fun initACRA() {
        try {
            val acraConfig = ConfigurationBuilder(this)
                .setReportSenderFactoryClasses(*reportSenderFactoryClasses as Array<Class<out ReportSenderFactory>>)
                .setBuildConfigClass(BuildConfig::class.java)
                .build()
            ACRA.init(this, acraConfig)
        } catch (ace: ACRAConfigurationException) {
            ace.printStackTrace()
            ErrorActivity.reportError(
                this, ace, null, null, ErrorInfo.make(
                    UserAction.SOMETHING_ELSE, "none",
                    "Could not initialize ACRA crash report", R.string.app_ui_crash
                )
            )
        }

    }

    private fun initStetho() {

        val initializer = Stetho.newInitializerBuilder(this)        // Create an InitializerBuilder
            .enableWebKitInspector(Stetho.defaultInspectorModulesProvider(this))  // Enable Chrome DevTools
            .enableDumpapp(Stetho.defaultDumperPluginsProvider(this))             // Enable command line interface
            .build()                                                                     //  generate an Initializer

        // Initialize Stetho with the Initializer
        Stetho.initialize(initializer)
    }

    private fun installLeakCanary(): RefWatcher {

        return if (BuildConfig.DEBUG) {
            LeakCanary.refWatcher(this)
                .heapDumper(ToggleableHeapDumper(this))
                // give each object 10 seconds to be gc'ed, before leak canary gets nosy on it
                .watchDelay(10, TimeUnit.SECONDS)
                .buildAndInstall()
        } else {
            RefWatcher.DISABLED
        }

    }

    class ToggleableHeapDumper internal constructor(context: Context) : HeapDumper {
        private val dumper: HeapDumper
        private val preferences: SharedPreferences
        private val dumpingAllowanceKey: String

        private val isDumpingAllowed: Boolean
            get() = preferences.getBoolean(dumpingAllowanceKey, false)

        init {
            val leakDirectoryProvider = DefaultLeakDirectoryProvider(context)
            this.dumper = AndroidHeapDumper(context, leakDirectoryProvider)
            this.preferences = android.preference.PreferenceManager.getDefaultSharedPreferences(context)
            this.dumpingAllowanceKey = context.getString(R.string.allow_heap_dumping_key)
        }

        override fun dumpHeap(): File? {
            return if (isDumpingAllowed) dumper.dumpHeap() else HeapDumper.RETRY_LATER
        }
    }

    companion object {
        private val TAG = App::class.java.toString()

        private val reportSenderFactoryClasses = arrayOf<Class<*>>(AcraReportSenderFactory::class.java)

        fun getRefWatcher(context: Context): RefWatcher? {
            val application = context.applicationContext as App
            return application.refWatcher
        }

        @Volatile
        private var INSTANCE: App? = null

        fun getApp(): App =
            INSTANCE ?: synchronized(App::class.java) {
                INSTANCE ?: App().also { INSTANCE = it }
            }
    }
}