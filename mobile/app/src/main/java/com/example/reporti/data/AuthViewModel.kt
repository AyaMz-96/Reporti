package com.example.reporti.data

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import java.io.IOException

data class AuthUiState(
    val user: AuthUser? = null,
    val ready: Boolean = false,
    val loading: Boolean = false,
    val error: String? = null,
    val message: String? = null,
)

class AuthViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = AuthRepository(app)

    var state by mutableStateOf(AuthUiState(user = repo.currentUser()))
        private set

    init {
        // Restore AuthTokenHolder from the persisted session so ApiClient can send Bearer headers
        AuthTokenHolder.accessToken = repo.accessToken
        viewModelScope.launch {
            val user = repo.restore()
            AuthTokenHolder.accessToken = repo.accessToken
            state = state.copy(user = user, ready = true)
        }
    }

    fun clearFeedback() { state = state.copy(error = null, message = null) }

    fun login(email: String, password: String) = run {
        repo.login(email, password).let { state = state.copy(user = it) }
    }

    fun register(fullName: String, email: String, phone: String?, password: String) = run {
        val user = repo.register(fullName, email, phone, password)
        state = if (user != null) state.copy(user = user)
        else state.copy(message = "Account created. Check your email to confirm it, then log in.")
    }

    fun forgotPassword(email: String) = run {
        repo.forgotPassword(email)
        state = state.copy(message = "If an account exists for this email, a reset link has been sent.")
    }

    fun updatePreferences(categoryIds: List<Long>, sectorId: Long?) = run {
        val updated = repo.updateMetadata(mapOf(
            "categoryIds" to categoryIds,
            "sectorId" to sectorId,
        ))
        state = state.copy(user = updated, message = "Preferences saved.")
    }

    fun logout() {
        viewModelScope.launch {
            repo.logout()
            state = state.copy(user = null, error = null, message = null)
        }
    }

    private fun run(block: suspend () -> Unit) {
        viewModelScope.launch {
            state = state.copy(loading = true, error = null, message = null)
            try {
                block()
            } catch (e: AuthException) {
                state = state.copy(error = e.message)
            } catch (e: IOException) {
                state = state.copy(error = "Cannot reach the server. Check your connection.")
            } catch (e: Exception) {
                state = state.copy(error = "Something went wrong. Please try again.")
            }
            state = state.copy(loading = false)
        }
    }
}
