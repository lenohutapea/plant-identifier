package com.lenohutapea.plantidentifier.data.remote

import com.lenohutapea.plantidentifier.BuildConfig
import com.lenohutapea.plantidentifier.data.ServerSettings
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Retrofit is built once with the default base URL. This interceptor rewrites every request to the address
 * currently saved in Settings, so changing the server takes effect immediately without restarting the app.
 */
class BaseUrlInterceptor(private val settings: ServerSettings) : Interceptor {
    private val defaultBase = BuildConfig.API_BASE_URL.toHttpUrl()

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val target = settings.baseUrl.toHttpUrlOrNull() ?: return chain.proceed(request)
        val original = request.url
        val relativePath = original.encodedPath.removePrefix(defaultBase.encodedPath).trimStart('/')
        val newUrl = original.newBuilder()
            .scheme(target.scheme)
            .host(target.host)
            .port(target.port)
            .encodedPath(target.encodedPath.trimEnd('/') + "/" + relativePath)
            .build()
        return chain.proceed(request.newBuilder().url(newUrl).build())
    }
}
