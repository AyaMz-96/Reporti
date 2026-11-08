package com.example.reporti.data

import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

/**
 * Mirrors the Spring controllers in com.example.Reporti.controller exactly.
 * The citizen is identified by the X-Citizen-Id header (added in ApiClient), not by a userId query parameter.
 */
interface ReportApi {

    // ----- Reference data -----

    @GET("categories")
    suspend fun getCategories(): List<Category>

    @GET("sectors")
    suspend fun getSectors(): List<Sector>

    // ----- Reports -----

    @GET("reports")
    suspend fun getReports(): List<ReportResponse>

    @POST("reports")
    suspend fun createReport(@Body request: CreateReportRequest): ReportResponse

    // Every part is named "files", matching @RequestParam("files") on the backend
    @Multipart
    @POST("reports/{id}/media")
    suspend fun uploadMedia(
        @Path("id") reportId: Long,
        @Part files: List<MultipartBody.Part>,
    ): List<String>

    // ----- Notifications (plain list, no paging) -----

    @GET("notifications")
    suspend fun notifications(): List<NotificationDto>

    // The backend answers 204 No Content, so these return Response<> to avoid parsing an empty body
    @PATCH("notifications/{id}/read")
    suspend fun markRead(@Path("id") id: Long): Response<ResponseBody>

    @PATCH("notifications/read-all")
    suspend fun markAllRead(): Response<ResponseBody>
}

object ApiClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        // The backend uploads to Supabase before answering, so big files need more than the 10 s default
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        // TEMPORARY: the backend identifies the citizen from this header until real authentication exists
        .addInterceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .header("X-Citizen-Id", Config.CITIZEN_ID.toString())
                    .build()
            )
        }
        .build()

    val api: ReportApi = Retrofit.Builder()
        .baseUrl(Config.API_BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ReportApi::class.java)
}
