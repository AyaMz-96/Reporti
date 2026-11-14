package com.example.reporti.data

import com.google.gson.annotations.SerializedName

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

enum class PriorityLevel { URGENT, NORMAL, LOW }

data class CreateReportRequest(
    val title: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val categoryId: Long,
    val sectorId: Long,
    val priority: PriorityLevel = PriorityLevel.NORMAL,
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

// ----- Home (search/filter) and notifications -----

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