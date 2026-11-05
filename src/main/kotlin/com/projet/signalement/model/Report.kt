package com.projet.signalement.model

import jakarta.persistence.*
import java.time.OffsetDateTime

@Entity
@Table(name = "reports")
class Report(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, columnDefinition = "text")
    var title: String,

    @Column(nullable = false)
    var description: String,

    @Enumerated(EnumType.STRING)
    var status: ReportStatus = ReportStatus.RECEIVED,

    @Enumerated(EnumType.STRING)
    var priority: PriorityLevel = PriorityLevel.NORMAL,

    @Column(nullable = false)
    val latitude: Double,

    @Column(nullable = false)
    val longitude: Double,

    @Column(name = "created_at", insertable = false, updatable = false)
    val createdAt: OffsetDateTime? = null,

    @Column(name = "updated_at", insertable = false, updatable = false)
    var updatedAt: OffsetDateTime? = null,

    @Column(name = "is_read")
    var isRead: Boolean = false,

    var archived: Boolean = false,

    @Column(name = "citizen_id")
    val citizenId: Long? = null,

    @Column(name = "sector_id")
    val sectorId: Long? = null,

    @Column(name = "category_id")
    val categoryId: Long? = null,

    @Column(name = "agent_id")
    val agentId: Long? = null
)