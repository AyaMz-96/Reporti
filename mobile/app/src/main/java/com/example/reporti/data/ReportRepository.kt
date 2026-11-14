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

    suspend fun uploadMedia(
        contentResolver: ContentResolver,
        reportId: Long,
        uris: List<Uri>,
    ): List<String> = withContext(Dispatchers.IO) {
        val files = uris.mapIndexed { index, uri ->
            val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
            val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "bin"
            val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: throw UploadException("Could not read one of the selected files")
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
    is IOException -> "Cannot reach the server at ${Config.API_BASE_URL} (${javaClass.simpleName}: ${message ?: "no details"}). " +
        "Check that the backend is running and that this address matches your device."
    else -> "Something went wrong"
}

// The backend answers errors as {"message": "..."}
private fun HttpException.readServerMessage(): String? = try {
    val body = response()?.errorBody()?.string()
    JsonParser.parseString(body).asJsonObject.get("message")?.asString?.takeIf { it.isNotBlank() }
} catch (e: Exception) {
    null
}
