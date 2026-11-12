package com.example.reporti.ui.assignments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.reporti.data.AgentRepository
import com.example.reporti.data.ReportResponse
import com.example.reporti.data.toUserMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AssignmentUiState(
    val available: List<ReportResponse> = emptyList(),
    val active: List<ReportResponse> = emptyList(),
    val availableLoading: Boolean = false,
    val activeLoading: Boolean = false,
    val claiming: Boolean = false,
    val error: String? = null,
    val message: String? = null,
)

class AssignmentViewModel : ViewModel() {

    private val _state = MutableStateFlow(AssignmentUiState())
    val state: StateFlow<AssignmentUiState> = _state.asStateFlow()

    fun load() {
        _state.update { it.copy(availableLoading = true, activeLoading = true, error = null) }

        viewModelScope.launch {
            try {
                val available = AgentRepository.getMyAvailable()
                _state.update { it.copy(available = available, availableLoading = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(availableLoading = false, error = e.toUserMessage()) }
            }
        }
        viewModelScope.launch {
            try {
                val active = AgentRepository.getMyActive()
                _state.update { it.copy(active = active, activeLoading = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(activeLoading = false, error = e.toUserMessage()) }
            }
        }
    }

    fun claim(id: Long) {
        if (_state.value.claiming) return
        viewModelScope.launch {
            _state.update { it.copy(claiming = true, error = null, message = null) }
            try {
                val report = AgentRepository.claimReport(id)
                _state.update {
                    it.copy(
                        claiming = false,
                        message = "You took report #${report.id}. It's now IN PROGRESS.",
                    )
                }
                // Refresh both lists: the claimed item moves from Available to Active
                load()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(claiming = false, error = e.toUserMessage()) }
            }
        }
    }

    fun clearFeedback() = _state.update { it.copy(error = null, message = null) }
}
