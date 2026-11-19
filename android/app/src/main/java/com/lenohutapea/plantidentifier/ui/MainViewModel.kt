package com.lenohutapea.plantidentifier.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.lenohutapea.plantidentifier.PlantIdentifierApp
import com.lenohutapea.plantidentifier.data.Diagnosis
import com.lenohutapea.plantidentifier.data.DiagnosisRepository
import com.lenohutapea.plantidentifier.data.ServerSettings
import com.lenohutapea.plantidentifier.data.local.DiagnosisEntity
import com.lenohutapea.plantidentifier.data.remote.PlantSummary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

sealed interface AnalysisState {
    data object Idle : AnalysisState
    data class Loading(val photo: Uri) : AnalysisState
    data class Success(val diagnosis: Diagnosis) : AnalysisState
    data class Error(val message: String) : AnalysisState
}

class MainViewModel(
    private val repository: DiagnosisRepository,
    private val settings: ServerSettings,
) : ViewModel() {
    private val _state = MutableStateFlow<AnalysisState>(AnalysisState.Idle)
    val state: StateFlow<AnalysisState> = _state.asStateFlow()

    val history: StateFlow<List<DiagnosisEntity>> = repository.history
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _plants = MutableStateFlow<List<PlantSummary>>(emptyList())
    val plants: StateFlow<List<PlantSummary>> = _plants.asStateFlow()

    /** Fetch the plant list shown on the home screen; failures just leave it empty. */
    fun refreshPlants() {
        viewModelScope.launch {
            runCatching { repository.supportedPlants() }.onSuccess { _plants.value = it }
        }
    }

    fun analyze(uri: Uri) {
        if (_state.value is AnalysisState.Loading) return
        _state.value = AnalysisState.Loading(uri)
        viewModelScope.launch {
            _state.value = try {
                AnalysisState.Success(repository.diagnose(uri))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AnalysisState.Error(e.toUserMessage(settings.baseUrl))
            }
        }
    }

    fun openHistory(id: Long) {
        viewModelScope.launch {
            repository.load(id)?.let { _state.value = AnalysisState.Success(it) }
        }
    }

    fun delete(entity: DiagnosisEntity) {
        viewModelScope.launch { repository.delete(entity) }
    }

    fun reset() {
        _state.value = AnalysisState.Idle
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as PlantIdentifierApp
                MainViewModel(app.container.repository, app.container.settings)
            }
        }
    }
}

private fun Throwable.toUserMessage(serverUrl: String): String = when (this) {
    is HttpException -> when (code()) {
        400 -> "The server could not read this photo. Try another image."
        413 -> "The photo is too large."
        503 -> "The server is still loading the model. Try again in a moment."
        else -> "Server error (HTTP ${code()})."
    }
    is IOException -> "Cannot reach the server at $serverUrl. Make sure your phone is on the same network " +
        "as the API, or change the address in Settings."
    is IllegalArgumentException -> message ?: "Cannot read this image."
    else -> message ?: "Something went wrong."
}
