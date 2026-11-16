package com.lenohutapea.plantidentifier.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.lenohutapea.plantidentifier.BuildConfig
import com.lenohutapea.plantidentifier.PlantIdentifierApp
import com.lenohutapea.plantidentifier.data.Diagnosis
import com.lenohutapea.plantidentifier.data.DiagnosisRepository
import com.lenohutapea.plantidentifier.data.local.DiagnosisEntity
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
    data object Loading : AnalysisState
    data class Success(val diagnosis: Diagnosis) : AnalysisState
    data class Error(val message: String) : AnalysisState
}

class MainViewModel(private val repository: DiagnosisRepository) : ViewModel() {
    private val _state = MutableStateFlow<AnalysisState>(AnalysisState.Idle)
    val state: StateFlow<AnalysisState> = _state.asStateFlow()

    val history: StateFlow<List<DiagnosisEntity>> = repository.history
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun analyze(uri: Uri) {
        if (_state.value == AnalysisState.Loading) return
        _state.value = AnalysisState.Loading
        viewModelScope.launch {
            _state.value = try {
                AnalysisState.Success(repository.diagnose(uri))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AnalysisState.Error(e.toUserMessage())
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
                MainViewModel(app.container.repository)
            }
        }
    }
}

private fun Throwable.toUserMessage(): String = when (this) {
    is HttpException -> when (code()) {
        400 -> "The server could not read this photo. Try another image."
        413 -> "The photo is too large."
        503 -> "The server is still loading the model. Try again in a moment."
        else -> "Server error (HTTP ${code()})."
    }
    is IOException -> "Cannot reach the server at ${BuildConfig.API_BASE_URL}. " +
        "Make sure your phone is on the same network as the API."
    is IllegalArgumentException -> message ?: "Cannot read this image."
    else -> message ?: "Something went wrong."
}
