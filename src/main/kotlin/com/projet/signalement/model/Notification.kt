package com.projet.signalement.model

import jakarta.persistence.*
import java.time.OffsetDateTime

@Entity
@Table(name = "notifications")
class Notification(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, columnDefinition = "text")
    val message: String,

    @Column(name = "created_at", insertable = false, updatable = false)
    val createdAt: OffsetDateTime? = null,

    @Column(name = "is_read")
    var isRead: Boolean = false,

    @Column(name = "user_id")
    val userId: Long? = null
)