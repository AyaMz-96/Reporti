package com.example.reporti.data

import android.content.ContentResolver
import android.net.Uri
import android.webkit.MimeTypeMap
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import java.io.IOException

// Same limits as spring.servlet.multipart.* in application.properties
private const val MAX_FILE_BYTES = 10 * 1024 * 1024
private const val MAX_TOTAL_BYTES = 30 * 1024 * 1024

/** A problem found on the phone before anything was sent, so it must not be shown as a network error. */
class UploadException(message: String) : Exception(message)

object ReportRepository {

    private val api = ApiClient.api

    suspend fun getCategories(): List<Category> = api.getCategories()

    suspend fun getSectors(): List<Sector> = api.getSectors()

    suspend fun createReport(request: CreateReportRequest): ReportResponse = api.createReport(request)

    suspend fun getReports(): List<ReportResponse> = api.getReports()

    suspend fun getNotifications(): List<NotificationDto> = api.getNotifications()

    suspend fun markNotificationRead(id: Long) {
        api.markRead(id)
    }

    suspend fun markAllNotificationsRead() {
        api.markAllRead()
    }

    suspend fun uploadMedia(
        contentResolver: ContentResolver,
        reportId: Long,
        uris: List<Uri>,
    ): List<String> = withContext(Dispatchers.IO) {
        val files = uris.mapIndexed { index, uri ->
            val rawType = contentResolver.getType(uri)
            val mimeType = when {
                rawType != null && (rawType.startsWith("image/") || rawType.startsWith("video/")) -> rawType
                uri.toString().endsWith(".png", true) -> "image/png"
                uri.toString().endsWith(".webp", true) -> "image/webp"
                uri.toString().endsWith(".gif", true) -> "image/gif"
                uri.toString().endsWith(".mp4", true) -> "video/mp4"
                uri.toString().endsWith(".mov", true) -> "video/quicktime"
                else -> "image/jpeg"
            }
            val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
                ?: when {
                    mimeType.startsWith("image/") -> "jpg"
                    mimeType.startsWith("video/") -> "mp4"
                    else -> "jpg"
                }

            val bytes = try {
                contentResolver.openInputStream(uri)?.use { it.readBytes() }
            } catch (e: Exception) {
                null
            } ?: throw UploadException("Could not read one of the selected files")

            if (bytes.size > MAX_FILE_BYTES) {
                throw UploadException("A selected file is larger than 10 MB")
            }
            Triple("file$index.$extension", mimeType, bytes)
        }

        if (files.sumOf { it.third.size.toLong() } > MAX_TOTAL_BYTES) {
            throw UploadException("The selected files add up to more than 30 MB")
        }

        val parts = files.map { (name, mimeType, bytes) ->
            MultipartBody.Part.createFormData("files", name, bytes.toRequestBody(mimeType.toMediaType()))
        }
        api.uploadMedia(reportId, parts)
    }
}

/** Turns any failure into a message that is safe to show the user. */
fun Throwable.toUserMessage(): String = when (this) {
    is HttpException -> readServerMessage() ?: "Request failed (${code()})"
    is UploadException -> message ?: "Could not prepare the files"
    is IOException -> "Cannot reach the server. Is the backend running?"
    else -> message ?: "Something went wrong"
}

// The backend answers errors as {"message": "..."}
private fun HttpException.readServerMessage(): String? = try {
    val body = response()?.errorBody()?.string()
    JsonParser.parseString(body).asJsonObject.get("message")?.asString?.takeIf { it.isNotBlank() }
} catch (e: Exception) {
    null
}
