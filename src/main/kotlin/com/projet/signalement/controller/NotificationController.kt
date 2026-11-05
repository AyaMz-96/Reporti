package com.projet.signalement.controller
import com.projet.signalement.service.NotificationService
import org.springframework.data.domain.PageRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/notifications")
class NotificationController(private val service: NotificationService) {

    // TODO: remplacer userId par l'utilisateur authentifié (JWT)
    @GetMapping
    fun list(
        @RequestParam userId: Long,
        @RequestParam(defaultValue = "false") unread: Boolean,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ) = service.list(userId, unread, PageRequest.of(page, size))

    @GetMapping("/unread-count")
    fun count(@RequestParam userId: Long) = mapOf("count" to service.unreadCount(userId))

    @PatchMapping("/{id}/read")
    fun read(@PathVariable id: Long, @RequestParam userId: Long): ResponseEntity<Void> {
        service.markRead(id, userId)
        return ResponseEntity.noContent().build()
    }

    @PatchMapping("/read-all")
    fun readAll(@RequestParam userId: Long) = mapOf("updated" to service.markAllRead(userId))
}