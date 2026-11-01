package com.example.reporti.data
import com.google.gson.annotations.SerializedName
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path
import retrofit2.http.Query

data class PageResponse<T>(val content: List<T>, val totalElements: Long)

data class NotificationDto(
    val id: Long,
    val message: String,
    val type: String,
    @SerializedName(value = "isRead", alternate = ["read"]) val isRead: Boolean,
    val createdAt: String?,
    val reportId: Long?
)

data class ReportDto(
    val id: Long,
    val description: String,
    val status: String,
    val priority: String,
    val category: String,
    val sector: String,
    val createdAt: String?
)

data class CountResponse(val count: Long)

interface ApiService {

    // ----- Notifications -----
    @GET("api/notifications")
    suspend fun notifications(
        @Query("userId") userId: Long,
        @Query("unread") unread: Boolean = false,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 50
    ): PageResponse<NotificationDto>

    @GET("api/notifications/unread-count")
    suspend fun unreadCount(@Query("userId") userId: Long): CountResponse

    @PATCH("api/notifications/{id}/read")
    suspend fun markRead(
        @Path("id") id: Long,
        @Query("userId") userId: Long
    ): Response<ResponseBody>

    @PATCH("api/notifications/read-all")
    suspend fun markAllRead(@Query("userId") userId: Long): Map<String, Int>

    // ----- Recherche et filtre -----
    @GET("api/reports/search")
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
    // 10.0.2.2 = ton PC vu depuis l'émulateur.
    // Téléphone réel : l'IP de ton PC, ex. "http://192.168.1.20:8080/"
    private const val BASE_URL = "http://10.0.2.2:8080/"

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}