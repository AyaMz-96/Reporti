package com.example.reporti.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.reporti.data.NotificationDto
import com.example.reporti.data.ReportRepository
import com.example.reporti.data.toUserMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotificationsUiState(
    val items: List<NotificationDto> = emptyList(),
    val unreadCount: Long = 0,
    val unreadOnly: Boolean = false,
    val loading: Boolean = false,
    val error: String? = null
)

class NotificationViewModel : ViewModel() {

    private val repository = ReportRepository
    private val _state = MutableStateFlow(NotificationsUiState())
    val state: StateFlow<NotificationsUiState> = _state.asStateFlow()

    init {
        // Rafraîchit automatiquement toutes les 30 secondes (alimente aussi le badge)
        viewModelScope.launch {
            while (true) {
                load()
                delay(30_000)
            }
        }
    }

    fun refresh() {
        viewModelScope.launch { load() }
    }

    fun setUnreadOnly(value: Boolean) {
        _state.update { it.copy(unreadOnly = value) }
        refresh()
    }

    fun markRead(id: Long) {
        viewModelScope.launch {
            try {
                repository.markNotificationRead(id)
                load()
            } catch (e: Exception) {
                _state.update { it.copy(error = e.toUserMessage()) }
            }
        }
    }

    fun markAllRead() {
        viewModelScope.launch {
            try {
                repository.markAllNotificationsRead()
                load()
            } catch (e: Exception) {
                _state.update { it.copy(error = e.toUserMessage()) }
            }
        }
    }

    private suspend fun load() {
        _state.update { it.copy(loading = true) }
        try {
            val allNotifications = repository.getNotifications()
            val totalUnread = allNotifications.count { !it.isRead }.toLong()
            val filtered = if (_state.value.unreadOnly) {
                allNotifications.filter { !it.isRead }
            } else {
                allNotifications
            }
            _state.update {
                it.copy(items = filtered, unreadCount = totalUnread, loading = false, error = null)
            }
        } catch (e: Exception) {
            _state.update { it.copy(loading = false, error = e.toUserMessage()) }
        }
    }
}
