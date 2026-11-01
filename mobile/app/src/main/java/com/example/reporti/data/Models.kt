package com.example.reporti.data

data class Category(
    val id: Long,
    val name: String,
    val description: String?,
)

data class Sector(
    val id: Long,
    val name: String,
    val zone: String?,
)

data class CreateReportRequest(
    val title: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val categoryId: Long,
    val sectorId: Long,
)

data class ReportResponse(
    val id: Long,
    val title: String,
    val description: String,
    val status: String,
    val priority: String,
    val latitude: Double,
    val longitude: Double,
    val categoryId: Long,
    val sectorId: Long,
    val createdAt: String,
    val mediaUrls: List<String>,
)