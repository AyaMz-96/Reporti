package com.example.reporti.data

import okhttp3.MultipartBody
import okhttp3.OkHttpClient
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

interface ReportApi {

    // ----- Categories and Sectors -----

    @GET("categories")
    suspend fun getCategories(): List<Category>

    @GET("sectors")
    suspend fun getSectors(): List<Sector>

    // ----- Reports -----

    @POST("reports")
    suspend fun createReport(@Body request: CreateReportRequest): ReportResponse

    @Multipart
    @POST("reports/{id}/media")
    suspend fun uploadMedia(
        @Path("id") reportId: Long,
        @Part files: List<MultipartBody.Part>,
    ): List<String>

    @GET("reports")
    suspend fun getReports(): List<ReportResponse>

    @GET("reports/{id}")
    suspend fun getReport(@Path("id") reportId: Long): ReportResponse

    // ----- Notifications -----

    @GET("notifications")
    suspend fun getNotifications(): List<NotificationDto>

    @PATCH("notifications/{id}/read")
    suspend fun markRead(@Path("id") id: Long): Response<Unit>

    @PATCH("notifications/read-all")
    suspend fun markAllRead(): Response<Unit>
}

object ApiClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        // The backend uploads to Supabase before answering, so big files need more than default
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
