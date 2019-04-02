package com.dew.aihua.util


import android.text.TextUtils
import okhttp3.*
import org.schabi.newpipe.extractor.DownloadRequest
import org.schabi.newpipe.extractor.DownloadResponse
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import org.schabi.newpipe.extractor.utils.Localization

import java.io.IOException
import java.io.InputStream
import java.util.HashMap
import java.util.concurrent.TimeUnit


/*
 * Created by Christian Schabesberger on 28.01.16.
 *
 * Copyright (C) Christian Schabesberger 2016 <chris.schabesberger@mailbox.org>
 * PageDownloader.java is part of NewPipe.
 *
 * NewPipe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * NewPipe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with NewPipe.  If not, see <http://www.gnu.org/licenses/>.
 */

class PageDownloader private constructor(builder: OkHttpClient.Builder) : org.schabi.newpipe.extractor.Downloader {
    var cookies: String? = null
    private val client: OkHttpClient = builder
        .readTimeout(30, TimeUnit.SECONDS)
        //.cache(new Cache(new File(context.getExternalCacheDir(), "okhttp"), 16 * 1024 * 1024))
        .build()

    /**
     * Get the size of the content that the url is pointing by firing a HEAD request.
     *
     * @param url an url pointing to the content
     * @return the size of the content, in bytes
     */
    @Throws(IOException::class)
    fun getContentLength(url: String): Long {
        var response: Response? = null
        try {
            val request = Request.Builder()
                .head().url(url)
                .addHeader("User-Agent", USER_AGENT)
                .build()
            response = client.newCall(request).execute()

            val contentLength = response!!.header("Content-Length")
            return if (contentLength == null) -1 else java.lang.Long.parseLong(contentLength)
        } catch (e: NumberFormatException) {
            throw IOException("Invalid content length", e)
        } finally {
            response?.close()
        }
    }

    /**
     * Download the text file at the supplied URL as in download(String),
     * but set the HTTP header field "Accept-Language" to the supplied string.
     *
     * @param siteUrl  the URL of the text file to return the contents of
     * @param localization the language and country (usually a 2-character code) to set
     * @return the contents of the specified text file
     */
    @Throws(IOException::class, ReCaptchaException::class)
    override fun download(siteUrl: String, localization: Localization): String {
        val requestProperties = HashMap<String, String>()
        requestProperties["Accept-Language"] = localization.language
        return download(siteUrl, requestProperties)
    }

    /**
     * Download the text file at the supplied URL as in download(String),
     * but set the HTTP headers included in the customProperties map.
     *
     * @param siteUrl          the URL of the text file to return the contents of
     * @param customProperties set request header properties
     * @return the contents of the specified text file
     * @throws IOException
     */
    @Throws(IOException::class, ReCaptchaException::class)
    override fun download(siteUrl: String, customProperties: Map<String, String>): String {
        return getBody(siteUrl, customProperties)!!.string()
    }

    @Throws(IOException::class)
    fun stream(siteUrl: String): InputStream {
        try {
            return getBody(siteUrl, emptyMap())!!.byteStream()
        } catch (e: ReCaptchaException) {
            throw IOException(e.message, e.cause)
        }

    }

    @Throws(IOException::class, ReCaptchaException::class)
    private fun getBody(siteUrl: String, customProperties: Map<String, String>): ResponseBody? {
        val requestBuilder = Request.Builder()
            .method("GET", null).url(siteUrl)

        for ((key, value) in customProperties) {
            requestBuilder.addHeader(key, value)
        }

        if (!customProperties.containsKey("User-Agent")) {
            requestBuilder.header("User-Agent", USER_AGENT)
        }

        if (!TextUtils.isEmpty(cookies)) {
            requestBuilder.addHeader("Cookie", cookies!!)
        }

        val request = requestBuilder.build()
        val response = client.newCall(request).execute()
        val body = response.body()

        if (response.code() == 429) {
            throw ReCaptchaException("reCaptcha Challenge requested")
        }

        if (body == null) {
            response.close()
            return null
        }

        return body
    }

    /**
     * Download (via HTTP) the text file located at the supplied URL, and return its contents.
     * Primarily intended for downloading web pages.
     *
     * @param siteUrl the URL of the text file to download
     * @return the contents of the specified text file
     */
    @Throws(IOException::class, ReCaptchaException::class)
    override fun download(siteUrl: String): String {
        return download(siteUrl, emptyMap())
    }


    @Throws(IOException::class, ReCaptchaException::class)
    override fun get(siteUrl: String, request: DownloadRequest): DownloadResponse? {
        val requestBuilder = Request.Builder()
            .method("GET", null).url(siteUrl)

        val requestHeaders = request.requestHeaders
        // set custom headers in request
        for ((key, value1) in requestHeaders) {
            for (value in value1) {
                requestBuilder.addHeader(key, value)
            }
        }

        if (!requestHeaders.containsKey("User-Agent")) {
            requestBuilder.header("User-Agent", USER_AGENT)
        }

        if (!TextUtils.isEmpty(cookies)) {
            requestBuilder.addHeader("Cookie", cookies!!)
        }

        val okRequest = requestBuilder.build()
        val response = client.newCall(okRequest).execute()
        val body = response.body()

        if (response.code() == 429) {
            throw ReCaptchaException("reCaptcha Challenge requested")
        }

        if (body == null) {
            response.close()
            return null
        }

        return DownloadResponse(body.string(), response.headers().toMultimap())
    }

    @Throws(IOException::class, ReCaptchaException::class)
    override fun get(siteUrl: String): DownloadResponse? {
        return get(siteUrl, DownloadRequest.emptyRequest)
    }

    @Throws(IOException::class, ReCaptchaException::class)
    override fun post(siteUrl: String, request: DownloadRequest): DownloadResponse? {

        val requestHeaders = request.requestHeaders
        if (null == requestHeaders["Content-Type"] || requestHeaders["Content-Type"]!!.isEmpty()) {
            // content type header is required. maybe throw an exception here
            return null
        }

        val contentType = requestHeaders["Content-Type"]!![0]

        var okRequestBody: RequestBody? = null
        if (null != request.requestBody) {
            okRequestBody = RequestBody.create(MediaType.parse(contentType), request.requestBody)
        }
        val requestBuilder = Request.Builder()
            .method("POST", okRequestBody).url(siteUrl)

        // set custom headers in request
        for ((key, value1) in requestHeaders) {
            for (value in value1) {
                requestBuilder.addHeader(key, value)
            }
        }

        if (!requestHeaders.containsKey("User-Agent")) {
            requestBuilder.header("User-Agent", USER_AGENT)
        }

        if (!TextUtils.isEmpty(cookies)) {
            requestBuilder.addHeader("Cookie", cookies!!)
        }

        val okRequest = requestBuilder.build()
        val response = client.newCall(okRequest).execute()
        val body = response.body()

        if (response.code() == 429) {
            throw ReCaptchaException("reCaptcha Challenge requested")
        }

        if (body == null) {
            response.close()
            return null
        }

        return DownloadResponse(body.string(), response.headers().toMultimap())
    }

    companion object {
        const val USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:43.0) Gecko/20100101 Firefox/43.0"

        var instance: PageDownloader? = null
            private set

        /**
         * It's recommended to call exactly once in the entire lifetime of the application.
         *
         * @param builder if null, default builder will be used
         */
        fun init(builder: OkHttpClient.Builder?): PageDownloader =
            instance ?: synchronized(PageDownloader::class.java){
                instance ?: PageDownloader(builder ?: OkHttpClient.Builder()).also { instance = it }
        }
    }
}