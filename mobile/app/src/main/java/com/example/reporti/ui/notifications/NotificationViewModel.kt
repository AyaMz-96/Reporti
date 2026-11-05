package com.example.reporti.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.reporti.data.ApiClient
import com.example.reporti.data.NotificationDto
import com.example.reporti.data.toUserMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import retrofit2.Response

data class NotificationsUiState(
    val all: List<NotificationDto> = emptyList(),
    val unreadOnly: Boolean = false,
    val loading: Boolean = false,
    val error: String? = null
) {
    // The backend returns the whole list, so the badge count and the unread filter are computed here
    val unreadCount: Long get() = all.count { !it.isRead }.toLong()
    val visible: List<NotificationDto> get() = if (unreadOnly) all.filter { !it.isRead } else all
}

class NotificationViewModel : ViewModel() {

    private val api = ApiClient.api
    private val _state = MutableStateFlow(NotificationsUiState())
    val state: StateFlow<NotificationsUiState> = _state.asStateFlow()

    init {
        // Refreshes every 30 seconds (also feeds the badge on the tab bar)
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
    }

    fun markRead(id: Long) {
        viewModelScope.launch {
            try {
                api.markRead(id).requireSuccess()
                load()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(error = e.toUserMessage()) }
            }
        }
    }

    fun markAllRead() {
        viewModelScope.launch {
            try {
                api.markAllRead().requireSuccess()
                load()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(error = e.toUserMessage()) }
            }
        }
    }

    private suspend fun load() {
        _state.update { it.copy(loading = true) }
        try {
            val items = api.notifications()
            _state.update { it.copy(all = items, loading = false, error = null) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _state.update { it.copy(loading = false, error = e.toUserMessage()) }
        }
    }
}

// Retrofit does not throw for 4xx/5xx when the return type is Response<>, so check it by hand
private fun Response<*>.requireSuccess() {
    if (!isSuccessful) throw HttpException(this)
}
