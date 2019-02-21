package com.dew.aihua



import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.multidex.MultiDex
import androidx.preference.PreferenceManager
import com.dew.aihua.report.AcraReportSenderFactory
import com.dew.aihua.report.ErrorActivity
import com.dew.aihua.report.ErrorInfo
import com.dew.aihua.report.UserAction
import com.facebook.stetho.Stetho
import com.facebook.stetho.okhttp3.StethoInterceptor
import com.nostra13.universalimageloader.core.ImageLoader
import com.squareup.leakcanary.*
import okhttp3.OkHttpClient
import org.acra.ACRA
import org.acra.config.ACRAConfigurationException
import org.acra.config.ConfigurationBuilder
import org.acra.sender.ReportSenderFactory
import java.io.File
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
//
//        // Initialize settings first because others inits can use its values
//        SettingsActivity.initSettings(this)
//
//        NewPipe.init(downloader, org.schabi.newpipe.util.Localization.getPreferredExtractorLocal(this))
//        StateSaver.init(this)
//        initNotificationChannel()
//
//        // Initialize image loader
//        ImageLoader.getInstance().init(getImageLoaderConfigurations(10, 50))
//
//        configureRxJavaErrorHandler()

    }

    private fun initACRA() {
        try {
            val acraConfig = ConfigurationBuilder(this)
                .setReportSenderFactoryClasses(*reportSenderFactoryClasses as Array<Class<out ReportSenderFactory>>)
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