package com.dew.aihua.ui.fragment

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.preference.PreferenceManager
import com.dew.aihua.R
import com.dew.aihua.player.helper.AnimationUtils.animateView
import com.dew.aihua.data.network.api.ExtractorHelper
import com.dew.aihua.data.local.cache.InfoCache
import com.dew.aihua.report.ErrorActivity
import com.dew.aihua.report.ErrorInfo
import com.dew.aihua.report.UserAction
import com.dew.aihua.ui.activity.MainActivity
import com.dew.aihua.ui.activity.ReCaptchaActivity
import com.dew.aihua.ui.contract.ViewContract
import com.jakewharton.rxbinding2.view.RxView
import icepick.State
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 *  Created by Edward on 3/2/2019.
 */
abstract class BaseStateFragment<I> : BaseFragment(), ViewContract<I> {

    @State
    @JvmField
    var wasLoading = AtomicBoolean()
    protected var isLoading = AtomicBoolean()

    protected var emptyStateView: View? = null
    protected var loadingProgressBar: ProgressBar? = null

    protected lateinit var errorPanelRoot: View
    protected lateinit var errorButtonRetry: Button
    protected lateinit var errorTextView: TextView

    protected val compositeDisposable = CompositeDisposable()

    override fun onViewCreated(rootView: View, savedInstanceState: Bundle?) {
        super.onViewCreated(rootView, savedInstanceState)
        doInitialLoadLogic()
    }

    override fun onPause() {
        super.onPause()
        wasLoading.set(isLoading.get())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        compositeDisposable.clear()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!compositeDisposable.isDisposed) compositeDisposable.dispose()
    }

    ///////////////////////////////////////////////////////////////////////////
    // Init
    ///////////////////////////////////////////////////////////////////////////


    override fun initViews(rootView: View, savedInstanceState: Bundle?) {
        super.initViews(rootView, savedInstanceState)

        emptyStateView = rootView.findViewById(R.id.empty_state_view)
        loadingProgressBar = rootView.findViewById(R.id.loading_progress_bar)

        errorPanelRoot = rootView.findViewById(R.id.error_panel)
        errorButtonRetry = rootView.findViewById(R.id.error_button_retry)
        errorTextView = rootView.findViewById(R.id.error_message_view)
    }

    override fun initListeners() {
        super.initListeners()
        val d = RxView.clicks(errorButtonRetry)
            .debounce(300, TimeUnit.MILLISECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { onRetryButtonClicked() }

        compositeDisposable.add(d)
    }

    protected fun onRetryButtonClicked() {
        reloadContent()
    }

    open fun reloadContent() {
        startLoading(true)
    }

    ///////////////////////////////////////////////////////////////////////////
    // Load
    ///////////////////////////////////////////////////////////////////////////

    protected open fun doInitialLoadLogic() {
        startLoading(true)
    }

    protected open fun startLoading(forceLoad: Boolean) {
        Log.d(TAG, "startLoading() called with: forceLoad = [$forceLoad]")
        showLoading()
        isLoading.set(true)
    }

    ///////////////////////////////////////////////////////////////////////////
    // Contract
    ///////////////////////////////////////////////////////////////////////////

    override fun showLoading() {
        if (emptyStateView != null) animateView(emptyStateView!!, false, 150)
        if (loadingProgressBar != null) animateView(loadingProgressBar!!, true, 400)
        animateView(errorPanelRoot, false, 150)
    }

    override fun hideLoading() {
        if (emptyStateView != null) animateView(emptyStateView!!, false, 150)
        if (loadingProgressBar != null) animateView(loadingProgressBar!!, false, 0)
        animateView(errorPanelRoot, false, 150)
    }

    override fun showEmptyState() {
        isLoading.set(false)
        if (emptyStateView != null) animateView(emptyStateView!!, true, 200)
        if (loadingProgressBar != null) animateView(loadingProgressBar!!, false, 0)
        animateView(errorPanelRoot, false, 150)
    }

    override fun showError(message: String, showRetryButton: Boolean) {
        Log.d(TAG, "showError() called with: message = [$message], showRetryButton = [$showRetryButton]")
        isLoading.set(false)
        InfoCache.clearCache()
        hideLoading()

        errorTextView.text = message
        if (showRetryButton)
            animateView(errorButtonRetry, true, 600)
        else
            animateView(errorButtonRetry, false, 0)
        animateView(errorPanelRoot, true, 300)
    }

    override fun handleResult(result: I) {
        Log.d(TAG, "handleResult() called with: result = [$result]")
        hideLoading()
    }

    ///////////////////////////////////////////////////////////////////////////
    // Error handling
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Default implementation handles some general exceptions
     *
     * @return if the exception was handled
     */
    protected open fun onError(exception: Throwable): Boolean {
        Log.d(TAG, "onError() called with: exception = [$exception]")
        isLoading.set(false)

        if (isDetached || isRemoving) {
            Log.w(TAG, "onError() is detached or removing = [$exception]")
            return true
        }

        if (ExtractorHelper.isInterruptedCaused(exception)) {
            Log.w(TAG, "onError() isInterruptedCaused! = [$exception]")
            return true
        }

        if (exception is ReCaptchaException) {
            onReCaptchaException()
            return true
        } else if (exception is IOException) {
            showError(getString(R.string.network_error), true)
            return true
        }

        return false
    }

    private fun onReCaptchaException() {
        Log.d(TAG, "onReCaptchaException() called")
        Toast.makeText(activity, R.string.recaptcha_request_toast, Toast.LENGTH_LONG).show()
        // Starting ReCaptcha Challenge Activity
        startActivityForResult(Intent(activity, ReCaptchaActivity::class.java), ReCaptchaActivity.RECAPTCHA_REQUEST)

        showError(getString(R.string.recaptcha_request_toast), false)
    }

    fun onUnrecoverableError(exception: Throwable, userAction: UserAction, serviceName: String, request: String, @StringRes errorId: Int) {
        // if the exception like "ParsingException: Malformed unacceptable url: https://www.youtube.com/channel/UC4ZLnCS3X8CI4RppOXTmT4g"
        // the link is a channel, still recoverable.
        // todo: recover this kind of parsing exceptions.
        onUnrecoverableError(listOf(exception), userAction, serviceName, request, errorId)
    }

    fun onUnrecoverableError(exception: List<Throwable>, userAction: UserAction, serviceNameP: String?, requestP: String?, @StringRes errorId: Int) {
        var serviceName = serviceNameP
        var request = requestP
        Log.d(TAG, "onUnrecoverableError() called with: exception = [$exception]")

        if (serviceName == null) serviceName = "none"
        if (request == null) request = "none"

        ErrorActivity.reportError(context!!, exception, MainActivity::class.java, null, ErrorInfo.make(userAction, serviceName, request, errorId))
    }

    fun showSnackBarError(exception: Throwable, userAction: UserAction, serviceName: String, request: String, @StringRes errorId: Int) {
        showSnackBarError(listOf(exception), userAction, serviceName, request, errorId)
    }

    /**
     * Show a SnackBar and only call ErrorActivity#reportError IF we a find a valid view (otherwise the error screen appears)
     */
    fun showSnackBarError(exception: List<Throwable>, userAction: UserAction, serviceName: String, request: String, @StringRes errorId: Int) {
        Log.d(TAG, "showSnackBarError() called with: exception = [$exception], userAction = [$userAction], request = [$request], errorId = [$errorId]")

        var rootView: View? = if (activity != null) activity!!.findViewById(android.R.id.content) else null
        if (rootView == null && view != null) rootView = view
        if (rootView == null) return

        ErrorActivity.reportError(context!!, exception, MainActivity::class.java, rootView,
            ErrorInfo.make(userAction, serviceName, request, errorId))
    }

    ///////////////////////////////////////////////////////////////////////////
    // Utils
    ///////////////////////////////////////////////////////////////////////////

    protected fun openUrlInBrowser(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(Intent.createChooser(intent, activity!!.getString(R.string.share_dialog_title)))
    }

    protected fun shareUrl(subject: String, url: String) {
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/plain"
        intent.putExtra(Intent.EXTRA_SUBJECT, subject)
        intent.putExtra(Intent.EXTRA_TEXT, url)
        startActivity(Intent.createChooser(intent, getString(R.string.share_dialog_title)))
    }

    protected val isGridLayout: Boolean
        get() {
            val listMode = PreferenceManager.getDefaultSharedPreferences(activity).getString(getString(R.string.list_view_mode_key), getString(R.string.list_view_mode_value))
            return when (listMode) {
                "list" -> {
                    val configuration = resources.configuration
                    configuration.orientation == Configuration.ORIENTATION_LANDSCAPE &&
                            configuration.isLayoutSizeAtLeast(Configuration.SCREENLAYOUT_SIZE_LARGE)
                }

                "auto",
                "grid" -> true
                else -> false
            }
        }

    companion object {
        private val TAG = BaseStateFragment::class.java.simpleName
    }
}
