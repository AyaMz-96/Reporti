package com.projet.signalement.service

import com.projet.signalement.dto.NotificationDto
import com.projet.signalement.dto.toDto
import com.projet.signalement.model.Notification
import com.projet.signalement.repository.NotificationRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

@Service
class NotificationService(
    private val repo: NotificationRepository
) {

    @Transactional
    fun send(userId: Long, message: String) {
        repo.save(
            Notification(
                message = message,
                userId = userId
            )
        )
    }

    @Transactional(readOnly = true)
    fun list(
        userId: Long,
        unreadOnly: Boolean,
        page: Pageable
    ): Page<NotificationDto> =
        (if (unreadOnly) {
            repo.findUnreadFor(userId, page)
        } else {
            repo.findAllFor(userId, page)
        }).map { it.toDto() }

    @Transactional(readOnly = true)
    fun unreadCount(userId: Long): Long =
        repo.countUnread(userId)

    @Transactional
    fun markRead(id: Long, userId: Long) {
        val notification = repo.findById(id).orElseThrow {
            ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Notification introuvable"
            )
        }

        if (notification.userId != userId) {
            throw ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Accès refusé"
            )
        }

        notification.isRead = true
    }

    @Transactional
    fun markAllRead(userId: Long): Int =
        repo.markAllRead(userId)
}