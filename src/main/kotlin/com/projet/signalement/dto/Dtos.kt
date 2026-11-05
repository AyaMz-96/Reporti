package com.projet.signalement.dto

import com.fasterxml.jackson.annotation.JsonProperty
import com.projet.signalement.model.*
import java.time.OffsetDateTime

data class NotificationDto(
    val id: Long,
    val message: String,

    @get:JsonProperty("isRead")
    val isRead: Boolean,

    val createdAt: OffsetDateTime?,
    val userId: Long?
)

fun Notification.toDto() = NotificationDto(
    id = id!!,
    message = message,
    isRead = isRead,
    createdAt = createdAt,
    userId = userId
)

data class ReportSummaryDto(
    val id: Long,
    val title: String,
    val description: String,
    val status: ReportStatus,
    val priority: PriorityLevel,
    val createdAt: OffsetDateTime?
)