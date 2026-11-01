package com.example.reporti.ui.home
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.reporti.data.ApiClient
import com.example.reporti.data.ReportDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val query: String = "",
    val status: String? = null,
    val priority: String? = null,
    val results: List<ReportDto> = emptyList(),
    val total: Long = 0,
    val loading: Boolean = false,
    val error: String? = null
)

class HomeViewModel : ViewModel() {

    // TODO: remplacer par l'id de l'utilisateur connecté (après login / JWT)
    private val userId = 1L

    private val api = ApiClient.api
    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    fun onQueryChange(value: String) = _state.update { it.copy(query = value) }

    fun onStatusChange(value: String?) {
        _state.update { it.copy(status = value) }
        search()
    }

    fun onPriorityChange(value: String?) {
        _state.update { it.copy(priority = value) }
        search()
    }

    fun reset() {
        _state.update { it.copy(query = "", status = null, priority = null) }
        search()
    }

    fun search() {
        val s = _state.value
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                val page = api.searchReports(
                    userId = userId,
                    q = s.query.trim().ifEmpty { null },
                    status = s.status,
                    priority = s.priority
                )
                _state.update {
                    it.copy(results = page.content, total = page.totalElements, loading = false)
                }
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = e.message ?: "Erreur réseau") }
            }
        }
    }
}