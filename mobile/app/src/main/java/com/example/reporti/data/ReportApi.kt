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

interface ReportApi {
    @GET("categories") suspend fun getCategories(): List<Category>
    @GET("sectors") suspend fun getSectors(): List<Sector>
    @GET("reports") suspend fun getReports(): List<ReportResponse>
    @POST("reports") suspend fun createReport(@Body request: CreateReportRequest): ReportResponse
    @Multipart @POST("reports/{id}/media")
    suspend fun uploadMedia(@Path("id") reportId: Long, @Part files: List<MultipartBody.Part>): List<String>
    @GET("notifications") suspend fun notifications(): List<NotificationDto>
    @PATCH("notifications/{id}/read") suspend fun markRead(@Path("id") id: Long): Response<ResponseBody>
    @PATCH("notifications/read-all") suspend fun markAllRead(): Response<ResponseBody>
}

interface AgentApi {
    @GET("agents/me/available") suspend fun getMyAvailable(): List<ReportResponse>
    @GET("agents/me/active") suspend fun getMyActive(): List<ReportResponse>
    @POST("agents/me/claim/{reportId}") suspend fun claimReport(@Path("reportId") reportId: Long): ReportResponse
}

object ApiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val builder = chain.request().newBuilder()
                .header("X-Citizen-Id", Config.CITIZEN_ID.toString())
            AuthTokenHolder.accessToken?.let { token ->
                builder.header("Authorization", "Bearer $token")
            }
            chain.proceed(builder.build())
        }
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(Config.API_BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api: ReportApi = retrofit.create(ReportApi::class.java)
    val agentApi: AgentApi = retrofit.create(AgentApi::class.java)
    val adminApi: AdminApi = retrofit.create(AdminApi::class.java)
}
