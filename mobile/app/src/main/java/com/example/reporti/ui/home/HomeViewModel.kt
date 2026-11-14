package com.example.reporti.ui.home

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

data class HomeUiState(
    val reports: List<ReportResponse> = emptyList(),
    val categories: List<Category> = emptyList(),
    val query: String = "",
    val categoryId: Long? = null,
    val status: String? = null,
    val priority: String? = null,
    val loading: Boolean = false,
    val error: String? = null,
) {
    /** Same filtering as the web list: search in title/description + category + status + priority. */
    val filtered: List<ReportResponse>
        get() {
            val term = query.trim().lowercase()
            return reports.filter { r ->
                (term.isEmpty() ||
                    r.title.lowercase().contains(term) ||
                    r.description.lowercase().contains(term)) &&
                    (categoryId == null || r.categoryId == categoryId) &&
                    (status == null || r.status == status) &&
                    (priority == null || r.priority == priority)
            }
        }

    fun categoryName(id: Long): String = categories.firstOrNull { it.id == id }?.name ?: "#$id"
}

class HomeViewModel : ViewModel() {

    private val api = ApiClient.api
    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    fun onQueryChange(value: String) = _state.update { it.copy(query = value) }
    fun onCategoryChange(value: Long?) = _state.update { it.copy(categoryId = value) }
    fun onStatusChange(value: String?) = _state.update { it.copy(status = value) }
    fun onPriorityChange(value: String?) = _state.update { it.copy(priority = value) }

    fun reset() = _state.update { it.copy(query = "", categoryId = null, status = null, priority = null) }

    /** Loads everything once (like the web list), then filters on the phone. */
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
