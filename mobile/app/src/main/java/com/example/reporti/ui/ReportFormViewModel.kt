package com.example.reporti.ui

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.reporti.data.Category
import com.example.reporti.data.CreateReportRequest
import com.example.reporti.data.ReportRepository
import com.example.reporti.data.Sector
import com.example.reporti.data.toUserMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.util.Locale
import com.example.reporti.data.PriorityLevel

class ReportFormViewModel(application: Application) : AndroidViewModel(application) {

    // What the user types or picks
    var title by mutableStateOf("")
    var description by mutableStateOf("")
    var categoryId by mutableStateOf<Long?>(null)
    var sectorId by mutableStateOf<Long?>(null)
    var latitude by mutableStateOf("")
    var longitude by mutableStateOf("")
    var photos by mutableStateOf<List<Uri>>(emptyList())

    var priority by mutableStateOf(PriorityLevel.NORMAL)

    // Loaded from the backend
    var categories by mutableStateOf<List<Category>>(emptyList())
        private set
    var sectors by mutableStateOf<List<Sector>>(emptyList())
        private set

    // Screen state
    var loading by mutableStateOf(true)
        private set
    var submitting by mutableStateOf(false)
        private set
    var locating by mutableStateOf(false)
        private set
    var showErrors by mutableStateOf(false)
        private set
    var loadError by mutableStateOf<String?>(null)
        private set
    var locationError by mutableStateOf<String?>(null)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var successMessage by mutableStateOf<String?>(null)
        private set

    /** Set when the report was created but its photos failed, so a retry never creates a duplicate report. */
    var pendingUploadId by mutableStateOf<Long?>(null)
        private set

    val fieldsLocked: Boolean get() = submitting || pendingUploadId != null

    val titleError: String?
        get() = when {
            title.isBlank() -> "Title is required"
            title.length > 150 -> "At most 150 characters"
            else -> null
        }

    val descriptionError: String?
        get() = when {
            description.isBlank() -> "Description is required"
            description.length > 2000 -> "At most 2000 characters"
            else -> null
        }

    val categoryError: String? get() = if (categoryId == null) "Choose a category" else null
    val sectorError: String? get() = if (sectorId == null) "Choose a sector" else null
    val latitudeError: String? get() = coordinateError(latitude, 90)
    val longitudeError: String? get() = coordinateError(longitude, 180)

    private val hasErrors: Boolean
        get() = listOf(titleError, descriptionError, categoryError, sectorError, latitudeError, longitudeError)
            .any { it != null }

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            loading = true
            loadError = null
            try {
                categories = ReportRepository.getCategories()
                sectors = ReportRepository.getSectors()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                loadError = "Could not load categories and sectors: ${e.toUserMessage()}"
            } finally {
                loading = false
            }
        }
    }

    fun startLocating() {
        locating = true
        locationError = null
    }

    fun onLocationFound(lat: Double, lng: Double) {
        latitude = String.format(Locale.US, "%.6f", lat)
        longitude = String.format(Locale.US, "%.6f", lng)
        locating = false
    }

    fun onLocationFailed(message: String) {
        locating = false
        locationError = message
    }

    fun submit() {
        if (submitting) return
        errorMessage = null
        successMessage = null

        // The report already exists, only the photos are left to send
        val pendingId = pendingUploadId
        if (pendingId != null) {
            viewModelScope.launch {
                submitting = true
                uploadAndFinish(pendingId)
            }
            return
        }

        showErrors = true
        if (hasErrors) return

        submitting = true
        viewModelScope.launch {
            try {
                val report = ReportRepository.createReport(
                    CreateReportRequest(
                        title = title,
                        description = description,
                        latitude = latitude.replace(',', '.').toDouble(),
                        longitude = longitude.replace(',', '.').toDouble(),
                        categoryId = categoryId!!,
                        sectorId = sectorId!!,
                        priority = priority,
                    )
                )
                uploadAndFinish(report.id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                submitting = false
                errorMessage = e.toUserMessage()
            }
        }
    }

    private suspend fun uploadAndFinish(reportId: Long) {
        if (photos.isNotEmpty()) {
            try {
                ReportRepository.uploadMedia(getApplication<Application>().contentResolver, reportId, photos)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                submitting = false
                pendingUploadId = reportId
                errorMessage = "Report #$reportId was created, but the photo upload failed: ${e.toUserMessage()}"
                return
            }
        }
        finish(reportId)
    }

    private fun finish(reportId: Long) {
        title = ""
        description = ""
        categoryId = null
        sectorId = null
        latitude = ""
        longitude = ""
        photos = emptyList()
        showErrors = false
        pendingUploadId = null
        submitting = false
        successMessage = "Report #$reportId submitted. Thank you!"
        priority = PriorityLevel.NORMAL
    }

    private fun coordinateError(text: String, limit: Int): String? {
        val value = text.replace(',', '.').toDoubleOrNull()
            ?: return if (text.isBlank()) "Required" else "Must be a number"
        return if (value < -limit || value > limit) "Must be between -$limit and $limit" else null
    }
}