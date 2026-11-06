package com.example.reporti.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.reporti.data.ReportRepository
import com.example.reporti.data.ReportResponse
import com.example.reporti.data.toUserMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val query: String = "",
    val status: String? = null,
    val priority: String? = null,
    val allReports: List<ReportResponse> = emptyList(),
    val results: List<ReportResponse> = emptyList(),
    val categories: Map<Long, String> = emptyMap(),
    val sectors: Map<Long, String> = emptyMap(),
    val total: Long = 0,
    val loading: Boolean = false,
    val error: String? = null
)

class HomeViewModel : ViewModel() {

    private val repository = ReportRepository
    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    fun onQueryChange(value: String) {
        _state.update { it.copy(query = value) }
        applyFilters()
    }

    fun onStatusChange(value: String?) {
        _state.update { it.copy(status = value) }
        applyFilters()
    }

    fun onPriorityChange(value: String?) {
        _state.update { it.copy(priority = value) }
        applyFilters()
    }

    fun reset() {
        _state.update { it.copy(query = "", status = null, priority = null) }
        applyFilters()
    }

    fun search() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                val reports = repository.getReports()
                val categories = repository.getCategories().associate { it.id to it.name }
                val sectors = repository.getSectors().associate { it.id to (it.name + (it.zone?.let { z -> " ($z)" } ?: "")) }
                _state.update {
                    it.copy(
                        allReports = reports,
                        categories = categories,
                        sectors = sectors,
                        loading = false
                    )
                }
                applyFilters()
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = e.toUserMessage()) }
            }
        }
    }

    private fun applyFilters() {
        val s = _state.value
        val q = s.query.trim().lowercase()
        val filtered = s.allReports.filter { report ->
            val matchesQuery = q.isEmpty() ||
                    report.title.lowercase().contains(q) ||
                    report.description.lowercase().contains(q)
            val matchesStatus = s.status == null || report.status.equals(s.status, ignoreCase = true)
            val matchesPriority = s.priority == null || report.priority.equals(s.priority, ignoreCase = true)

            matchesQuery && matchesStatus && matchesPriority
        }
        _state.update {
            it.copy(results = filtered, total = filtered.size.toLong())
        }
    }
}
