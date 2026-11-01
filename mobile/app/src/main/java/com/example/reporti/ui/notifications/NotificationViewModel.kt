package com.example.reporti.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.reporti.data.ApiClient
import com.example.reporti.data.NotificationDto
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

    // TODO: remplacer par l'id de l'utilisateur connecté (après login / JWT)
    private val userId = 1L

    private val api = ApiClient.api
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
                api.markRead(id, userId)
                load()
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    fun markAllRead() {
        viewModelScope.launch {
            try {
                api.markAllRead(userId)
                load()
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    private suspend fun load() {
        _state.update { it.copy(loading = true) }
        try {
            val page = api.notifications(userId, unread = _state.value.unreadOnly)
            val count = api.unreadCount(userId).count
            _state.update {
                it.copy(items = page.content, unreadCount = count, loading = false, error = null)
            }
        } catch (e: Exception) {
            _state.update { it.copy(loading = false, error = e.message ?: "Erreur réseau") }
        }
    }
}