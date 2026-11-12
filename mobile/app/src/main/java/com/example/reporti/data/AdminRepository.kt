package com.example.reporti.data

import retrofit2.HttpException
import retrofit2.Response

object AdminRepository {
    private val api = ApiClient.adminApi

    private fun Response<*>.requireSuccess() {
        if (!isSuccessful) throw HttpException(this)
    }

    suspend fun listUsers(): List<AdminUser> =
        api.listUsers().users.map(AdminUser::from)

    suspend fun createUser(body: CreateAdminUserBody) {
        api.createUser(body).requireSuccess()
    }

    suspend fun updateUser(id: String, body: UpdateAdminUserBody) {
        api.updateUser(id, body).requireSuccess()
    }

    suspend fun deleteUser(id: String) {
        api.deleteUser(id).requireSuccess()
    }

    suspend fun createCategory(name: String, description: String?): Category =
        api.createCategory(CategoryBody(name, description))

    suspend fun updateCategory(id: Long, name: String, description: String?): Category =
        api.updateCategory(id, CategoryBody(name, description))

    suspend fun deleteCategory(id: Long) {
        api.deleteCategory(id).requireSuccess()
    }

    suspend fun getAvailableAgents(categoryId: Long?, sectorId: Long?): List<AdminUser> =
        api.getAvailableAgents(categoryId, sectorId).map(AdminUser::from)

    suspend fun assignReport(reportId: Long, agentId: String): ReportResponse =
        api.assignReport(reportId, AssignReportBody(agentId))
}
