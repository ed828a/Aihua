package com.dew.aihua.ui.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.core.app.NavUtils
import com.dew.aihua.R
import com.dew.aihua.data.network.helper.PageDownloader

class ReCaptchaActivity : AppCompatActivity() {

    @SuppressLint("SetJavaScriptEnabled")
    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recaptcha)

        // Set return to Cancel by default
        setResult(Activity.RESULT_CANCELED)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setTitle(R.string.reCaptcha_title)
            actionBar.setDisplayShowTitleEnabled(true)
        }

        val myWebView = findViewById<WebView>(R.id.reCaptchaWebView)

        // Enable Javascript
        val webSettings = myWebView.settings
        webSettings.javaScriptEnabled = true

        val webClient = ReCaptchaWebViewClient(this)
        myWebView.webViewClient = webClient

        // Cleaning cache, history and cookies from webView
        myWebView.clearCache(true)
        myWebView.clearHistory()
        val cookieManager = CookieManager.getInstance()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.removeAllCookies { }
        } else {
            cookieManager.removeAllCookie()
        }

        myWebView.loadUrl(YT_URL)
    }

    private inner class ReCaptchaWebViewClient internal constructor(private val context: Activity) : WebViewClient() {
        private var mCookies: String? = null


        override fun onPageStarted(view: WebView, url: String, favicon: Bitmap) {
            // Start Loader
            super.onPageStarted(view, url, favicon)
            Log.d(TAG, "onPageStarted(): url = $url")
            Toast.makeText(context, "ReCaptcha is doing.", Toast.LENGTH_SHORT).show()
        }

        override fun onPageFinished(view: WebView, url: String) {
            val cookies = CookieManager.getInstance().getCookie(url)

            // Stop Loader

            // find cookies : s_gl & goojf and Add cookies to Downloader
            if (findAccessCookies(cookies)) {
                // Give cookies to Downloader class
                PageDownloader.instance!!.cookies = mCookies

                // Closing activity and return to parent
                setResult(Activity.RESULT_OK)
                finish()
            }
        }

        private fun findAccessCookies(cookies: String): Boolean {
            var ret = false
            var cSGL = ""
            var cGoojf = ""

            val parts = cookies.split("; ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            for (part in parts) {
                if (part.trim { it <= ' ' }.startsWith("s_gl")) {
                    cSGL = part.trim { it <= ' ' }
                }
                if (part.trim { it <= ' ' }.startsWith("goojf")) {
                    cGoojf = part.trim { it <= ' ' }
                }
            }
            if (cSGL.isNotEmpty() && cGoojf.isNotEmpty()) {
                ret = true
                //mCookies = c_s_gl + "; " + c_goojf;
                // Youtube seems to also need the other cookies:
                mCookies = cookies
            }

            return ret
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        return when (id) {
            android.R.id.home -> {
                val intent = Intent(this, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                NavUtils.navigateUpTo(this, intent)
                true
            }
            else -> false
        }
    }

    companion object {
        const val RECAPTCHA_REQUEST = 10

        val TAG = ReCaptchaActivity::class.java.toString()
        const val YT_URL = "https://www.youtube.com"
    }
}
