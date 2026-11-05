package com.projet.signalement.model

enum class ReportStatus {
    RECEIVED,
    IN_PROGRESS,
    RESOLVED,
    REJECTED
}

enum class PriorityLevel {
    URGENT,
    NORMAL,
    LOW
}

enum class CommentType {
    MAIN,
    REPLY
}

enum class MediaType {
    PHOTO,
    VIDEO
}

enum class UserRole {
    CITIZEN,
    ADMINISTRATOR,
    SECTOR_MANAGER,
    AGENT
}

enum class UserStatus {
    ACTIVE,
    SUSPENDED
}