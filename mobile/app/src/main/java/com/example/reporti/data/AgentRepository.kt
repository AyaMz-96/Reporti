package com.example.reporti.data

/**
 * Thin wrapper over AgentApi so screens don't touch Retrofit directly,
 * mirroring the shape of ReportRepository.
 */
object AgentRepository {
    suspend fun getMyAvailable(): List<ReportResponse> = ApiClient.agentApi.getMyAvailable()
    suspend fun getMyActive(): List<ReportResponse> = ApiClient.agentApi.getMyActive()
    suspend fun claimReport(id: Long): ReportResponse = ApiClient.agentApi.claimReport(id)
}
