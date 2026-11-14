package com.example.reporti.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.reporti.data.ApiClient
import com.example.reporti.data.Category
import com.example.reporti.data.ReportResponse
import com.example.reporti.data.toUserMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MapUiState(
    val reports: List<ReportResponse> = emptyList(),
    val categories: List<Category> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
) {
    fun categoryName(id: Long): String = categories.firstOrNull { it.id == id }?.name ?: "#$id"
}

class MapViewModel : ViewModel() {
    private val api = ApiClient.api
    private val _state = MutableStateFlow(MapUiState())
    val state: StateFlow<MapUiState> = _state.asStateFlow()

    // Loaded by MapScreen each time the tab opens, so new reports always appear

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                val reports = api.getReports()
                val categories = api.getCategories()
                _state.update { it.copy(reports = reports, categories = categories, loading = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = e.toUserMessage()) }
            }
        }
    }
}
