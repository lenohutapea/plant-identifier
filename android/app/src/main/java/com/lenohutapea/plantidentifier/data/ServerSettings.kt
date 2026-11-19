package com.lenohutapea.plantidentifier.data

import android.content.Context
import com.lenohutapea.plantidentifier.BuildConfig
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/** The API address chosen in the app's Settings screen; falls back to the address the APK was built with. */
class ServerSettings(context: Context) {
    private val prefs = context.getSharedPreferences("server_settings", Context.MODE_PRIVATE)

    var baseUrl: String
        get() = prefs.getString(KEY, null) ?: BuildConfig.API_BASE_URL
        set(value) {
            prefs.edit().putString(KEY, value).apply()
        }

    val isDefault: Boolean
        get() = !prefs.contains(KEY)

    fun reset() {
        prefs.edit().remove(KEY).apply()
    }

    private companion object {
        const val KEY = "api_base_url"
    }
}

/**
 * Turns what the user typed into a base URL Retrofit accepts: adds http:// when no scheme is given and a
 * trailing slash. Returns null when it is not a valid http(s) URL.
 */
fun normalizeBaseUrl(input: String): String? {
    var text = input.trim()
    if (text.isEmpty()) return null
    if (!text.startsWith("http://", ignoreCase = true) && !text.startsWith("https://", ignoreCase = true)) {
        text = "http://$text"
    }
    val url = text.toHttpUrlOrNull() ?: return null
    val asString = url.toString()
    return if (asString.endsWith("/")) asString else "$asString/"
}
