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
import retrofit2.http.Query
import java.util.concurrent.TimeUnit


interface ReportApi {

    // ----- Report submission -----

    @GET("categories")
    suspend fun getCategories(): List<Category>

    @GET("sectors")
    suspend fun getSectors(): List<Sector>

    @POST("reports")
    suspend fun createReport(@Body request: CreateReportRequest): ReportResponse

    // Every part is named "files", matching @RequestParam("files") on the backend
    @Multipart
    @POST("reports/{id}/media")
    suspend fun uploadMedia(
        @Path("id") reportId: Long,
        @Part files: List<MultipartBody.Part>,
    ): List<String>

    // ----- Notifications -----

    @GET("notifications")
    suspend fun notifications(
        @Query("userId") userId: Long,
        @Query("unread") unread: Boolean = false,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 50
    ): PageResponse<NotificationDto>

    @GET("notifications/unread-count")
    suspend fun unreadCount(@Query("userId") userId: Long): CountResponse

    @PATCH("notifications/{id}/read")
    suspend fun markRead(
        @Path("id") id: Long,
        @Query("userId") userId: Long
    ): Response<ResponseBody>

    @PATCH("notifications/read-all")
    suspend fun markAllRead(@Query("userId") userId: Long): Map<String, Int>

    // ----- Search and filter -----

    @GET("reports/search")
    suspend fun searchReports(
        @Query("userId") userId: Long,
        @Query("q") q: String? = null,
        @Query("status") status: String? = null,
        @Query("priority") priority: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 50
    ): PageResponse<ReportDto>
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