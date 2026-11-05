package com.example.reporti.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.reporti.data.ApiClient
import com.example.reporti.data.ReportResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MapUiState(
    val reports: List<ReportResponse> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
)

class MapViewModel : ViewModel() {
    private val api = ApiClient.api
    private val _state = MutableStateFlow(MapUiState())
    val state: StateFlow<MapUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                val reports = api.getReports()
                _state.update { it.copy(reports = reports, loading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = e.message ?: "Erreur réseau") }
            }
        }
    }
}