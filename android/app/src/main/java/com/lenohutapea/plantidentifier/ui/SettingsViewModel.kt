package com.lenohutapea.plantidentifier.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.lenohutapea.plantidentifier.PlantIdentifierApp
import com.lenohutapea.plantidentifier.data.ServerSettings
import com.lenohutapea.plantidentifier.data.normalizeBaseUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

data class SettingsUiState(
    val input: String,
    val saved: String,
    val isDefault: Boolean,
    val testing: Boolean = false,
    val message: String? = null,
    val success: Boolean? = null,
)

class SettingsViewModel(
    private val settings: ServerSettings,
    private val client: OkHttpClient,
) : ViewModel() {
    private val _state = MutableStateFlow(SettingsUiState(settings.baseUrl, settings.baseUrl, settings.isDefault))
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    fun onInputChange(value: String) {
        _state.update { it.copy(input = value, message = null, success = null) }
    }

    /** Calls GET /readyz on the typed address without saving it. */
    fun testConnection() {
        val url = normalizeBaseUrl(_state.value.input)
        if (url == null) {
            _state.update { it.copy(message = "Not a valid address.", success = false) }
            return
        }
        _state.update { it.copy(testing = true, message = null, success = null) }
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    client.newCall(Request.Builder().url(url + "readyz").build()).execute().use { response ->
                        response.code to response.body?.string().orEmpty().take(120)
                    }
                }
            }
            _state.update { current ->
                result.fold(
                    onSuccess = { (code, body) ->
                        if (code == 200) current.copy(testing = false, success = true, message = "Connected: $body")
                        else current.copy(testing = false, success = false, message = "The server answered HTTP $code.")
                    },
                    onFailure = { e ->
                        current.copy(
                            testing = false, success = false,
                            message = "Cannot reach $url (${e.javaClass.simpleName}). Check the address and that " +
                                "the phone is on the same network.",
                        )
                    },
                )
            }
        }
    }

    fun save() {
        val url = normalizeBaseUrl(_state.value.input)
        if (url == null) {
            _state.update { it.copy(message = "Not a valid address.", success = false) }
            return
        }
        settings.baseUrl = url
        _state.update { it.copy(input = url, saved = url, isDefault = false, message = "Saved.", success = true) }
    }

    fun resetToDefault() {
        settings.reset()
        val url = settings.baseUrl
        _state.update { SettingsUiState(url, url, isDefault = true, message = "Reset to the default address.", success = true) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as PlantIdentifierApp
                SettingsViewModel(app.container.settings, app.container.httpClient)
            }
        }
    }
}
