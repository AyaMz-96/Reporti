package com.example.reporti.data

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface AdminApi {
    @GET("admin/users")
    suspend fun listUsers(): AdminUsersResponse

    @POST("admin/users")
    suspend fun createUser(@Body body: CreateAdminUserBody): Response<ResponseBody>

    @PUT("admin/users/{id}")
    suspend fun updateUser(@Path("id") id: String, @Body body: UpdateAdminUserBody): Response<ResponseBody>

    @DELETE("admin/users/{id}")
    suspend fun deleteUser(@Path("id") id: String): Response<ResponseBody>

    @POST("categories")
    suspend fun createCategory(@Body body: CategoryBody): Category

    @PUT("categories/{id}")
    suspend fun updateCategory(@Path("id") id: Long, @Body body: CategoryBody): Category

    @DELETE("categories/{id}")
    suspend fun deleteCategory(@Path("id") id: Long): Response<ResponseBody>

    @GET("agents/available")
    suspend fun getAvailableAgents(
        @Query("categoryId") categoryId: Long?,
        @Query("sectorId") sectorId: Long?,
    ): List<AdminUserDto>

    @PATCH("reports/{id}/assign")
    suspend fun assignReport(@Path("id") reportId: Long, @Body body: AssignReportBody): ReportResponse
}
