package com.projet.signalement.repository

import com.projet.signalement.model.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface UserRepository : JpaRepository<User, Long> {

    fun findByRole(role: String): List<User>
}

interface ReportRepository :
    JpaRepository<Report, Long>,
    JpaSpecificationExecutor<Report>

interface NotificationRepository : JpaRepository<Notification, Long> {

    @Query("""
        select n from Notification n
        where n.userId = :userId
        order by n.createdAt desc
    """)
    fun findAllFor(
        @Param("userId") userId: Long,
        pageable: Pageable
    ): Page<Notification>

    @Query("""
        select n from Notification n
        where n.userId = :userId and n.isRead = false
        order by n.createdAt desc
    """)
    fun findUnreadFor(
        @Param("userId") userId: Long,
        pageable: Pageable
    ): Page<Notification>

    @Query("""
        select count(n) from Notification n
        where n.userId = :userId and n.isRead = false
    """)
    fun countUnread(@Param("userId") userId: Long): Long

    @Modifying
    @Query("""
        update Notification n
        set n.isRead = true
        where n.userId = :userId and n.isRead = false
    """)
    fun markAllRead(@Param("userId") userId: Long): Int
}