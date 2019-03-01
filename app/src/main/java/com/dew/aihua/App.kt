package com.dew.aihua



import android.annotation.TargetApi
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.multidex.MultiDex
import com.dew.aihua.report.AcraReportSenderFactory
import com.dew.aihua.report.ErrorActivity
import com.dew.aihua.report.ErrorInfo
import com.dew.aihua.report.UserAction
import com.facebook.stetho.Stetho
import com.squareup.leakcanary.*
import io.reactivex.exceptions.CompositeException
import io.reactivex.exceptions.MissingBackpressureException
import io.reactivex.exceptions.OnErrorNotImplementedException
import io.reactivex.exceptions.UndeliverableException
import io.reactivex.functions.Consumer
import io.reactivex.plugins.RxJavaPlugins
import org.acra.ACRA
import org.acra.config.ACRAConfigurationException
import org.acra.config.ConfigurationBuilder
import java.io.File
import java.io.IOException
import java.io.InterruptedIOException
import java.net.SocketException
import java.util.concurrent.TimeUnit

class App: Application() {
    private var refWatcher: RefWatcher? = null

//    protected open val downloader: Downloader
//        get() = if (BuildConfig.DEBUG){
//    org.schabi.newpipe.util.Downloader.getInstance(
//                                OkHttpClient.Builder()
//                                .addNetworkInterceptor(StethoInterceptor()))
//    } else {
//    org.schabi.newpipe.util.Downloader.getInstance(null)
//}

//    protected open val isDisposedRxExceptionsReported: Boolean
//        get() = if (BuildConfig.DEBUG){
//            PreferenceManager.getDefaultSharedPreferences(this)
//                .getBoolean(getString(R.string.allow_disposed_exceptions_key), false)
//        } else false

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)

        MultiDex.install(this)
        initACRA()
    }



    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG){
            initStetho()
        }


        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not initialize your app in this process.
            return
        }
        refWatcher = installLeakCanary()

        INSTANCE = this
//
//        // Initialize settings first because others inits can use its values
//        SettingsActivity.initSettings(this)
//
//        NewPipe.init(downloader, org.schabi.newpipe.util.Localization.getPreferredExtractorLocal(this))
//        StateSaver.init(this)
        initNotificationChannel()
//
//        // Initialize image loader
//        ImageLoader.getInstance().init(getImageLoaderConfigurations(10, 50))
//
//        configureRxJavaErrorHandler()

    }

//    private fun configureRxJavaErrorHandler() {
//        // https://github.com/ReactiveX/RxJava/wiki/What's-different-in-2.0#error-handling
//        RxJavaPlugins.setErrorHandler(object : Consumer<Throwable> {
//            override fun accept(throwable: Throwable) {
//                var throwable1 = throwable
//                Log.e(TAG, "RxJavaPlugins.ErrorHandler called with -> : " +
//                        "throwable = [" + throwable1.javaClass.name + "]")
//
//                if (throwable1 is UndeliverableException && throwable1.cause != null) {
//                    // As UndeliverableException is a wrapper, get the cause of it to get the "real" exception
//                    throwable1 = throwable1.cause!!
//                }
//
//                val errors: List<Throwable>
//                if (throwable1 is CompositeException) {
//                    errors = throwable1.exceptions
//                } else {
//                    errors = listOf(throwable1)
//                }
//
//                for (error in errors) {
//                    if (isThrowableIgnored(error)) return
//                    if (isThrowableCritical(error)) {
//                        reportException(error)
//                        return
//                    }
//                }
//
//                // Out-of-lifecycle exceptions should only be reported if a debug user wishes so,
//                // When exception is not reported, log it
//                if (isDisposedRxExceptionsReported) {
//                    reportException(throwable1)
//                } else {
//                    Log.e(TAG, "RxJavaPlugin: Undeliverable Exception received: ", throwable1)
//                }
//            }
//
//            private fun isThrowableIgnored(throwable: Throwable): Boolean {
//                // Don't crash the application over a simple network problem
//                return ExtractorHelper.hasAssignableCauseThrowable(throwable,
//                    IOException::class.java, SocketException::class.java, // network api cancellation
//                    InterruptedException::class.java, InterruptedIOException::class.java) // blocking code disposed
//            }
//
//            private fun isThrowableCritical(throwable: Throwable): Boolean {
//                // Though these exceptions cannot be ignored
//                return ExtractorHelper.hasAssignableCauseThrowable(throwable,
//                    NullPointerException::class.java, IllegalArgumentException::class.java, // bug in app
//                    OnErrorNotImplementedException::class.java, MissingBackpressureException::class.java,
//                    IllegalStateException::class.java) // bug in operator
//            }
//
//            private fun reportException(throwable: Throwable) {
//                // Throw uncaught exception that will trigger the report system
//                Thread.currentThread().uncaughtExceptionHandler
//                    .uncaughtException(Thread.currentThread(), throwable)
//            }
//        })
//    }


    @TargetApi(Build.VERSION_CODES.O)
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


    private fun initACRA() {
        try {
            val acraConfig = ConfigurationBuilder(this)
                .setReportSenderFactoryClasses(*reportSenderFactoryClasses)
                .setBuildConfigClass(BuildConfig::class.java)
                .build()
            ACRA.init(this, acraConfig)
        } catch (ace: ACRAConfigurationException) {
            ace.printStackTrace()
            ErrorActivity.reportError(this, ace, null, null, ErrorInfo.make(
                UserAction.SOMETHING_ELSE, "none",
                "Could not initialize ACRA crash report", R.string.app_ui_crash))
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

        return if (BuildConfig.DEBUG){
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

        private val reportSenderFactoryClasses = arrayOf(AcraReportSenderFactory::class.java)

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